package jeb

import jeb.ddsl.dir
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class JebSpec {

    val now: LocalDateTime = LocalDateTime.now()
    val nowStr: String = now.format(DateTimeFormatter.BASIC_ISO_DATE)
    val tomorrow: LocalDateTime = now.plusDays(1)
    val tomorrowStr: String = tomorrow.format(DateTimeFormatter.BASIC_ISO_DATE)
    val twoDaysLater: LocalDateTime = now.plusDays(2)
    val twoDaysLaterStr: String = twoDaysLater.format(DateTimeFormatter.BASIC_ISO_DATE)
    val threeDaysLater: LocalDateTime = now.plusDays(3)
    val threeDaysLaterStr: String = threeDaysLater.format(DateTimeFormatter.BASIC_ISO_DATE)
    val baseDir = createTempDir("jeb-", "-funct-test")
    val srcDir1 = File(baseDir, "source1")
    val srcDir2 = File(baseDir, "source2")
    val backupsDir = File(baseDir, "backups")
    val backup1 = File(backupsDir, "$nowStr-1")
    val backup1_2 = File(backupsDir, "$nowStr-2")
    val backup2 = File(backupsDir, "$tomorrowStr-2")
    val backup3 = File(backupsDir, "$twoDaysLaterStr-1")
    val backup3_10 = File(backupsDir, "$twoDaysLaterStr-10")
    val backup4 = File(backupsDir, "$threeDaysLaterStr-3")
    val stateFile = File(backupsDir, "jeb.json")

    @Test
    fun jebFunctionalTest() {

        val userInput = "${srcDir1.absolutePath}/\n" +
                "no\n" +
                "${backupsDir.absolutePath}\n" +
                "10\n"
        System.setIn(ByteArrayInputStream(userInput.toByteArray()))
        jeb.inReader = null
        main(arrayOf("init"))

        val state = State.loadState(stateFile).result
        assertEquals(Source(srcDir1.absolutePath + "/"), state.source.first())
        assertEquals(backupsDir.absolutePath, state.backupsDir)
        assertEquals(10, state.lastTapeNumber)

        val srcContent = dir(srcDir1) {
            file("file1") {
                "content1"
            }
            dir("dir1") {
                file("file2") {
                    "content2"
                }
            }
        }
        srcContent.create()

        main(arrayOf("backup", backupsDir.absolutePath))
        assertTrue(srcContent.contentEqualTo(backup1))
        forSameFiles(srcDir1, backup1) { file1, file2 -> assertNotEquals(file1.inode, file2.inode) }

        File(srcDir1, "file3").writeText("content3")

        main(arrayOf("backup", backupsDir.absolutePath))
        MatcherAssert.assertThat(backupsDir.listFiles().map { it.name }, Matchers.containsInAnyOrder("$nowStr-1", "jeb.json"))
        with (State.loadState(stateFile).result) {
            assertEquals(2, this.selectTape().second)
        }

        val secondState = State.loadState(stateFile).result
        val thirdState = Backuper(Storage(), tomorrow).doBackup(secondState, false)
        State.saveState(stateFile, thirdState)
        forSameFiles(backup1, backup2, ::inodesShouldBeEqual)

        val forthState = Backuper(Storage(), twoDaysLater).doBackup(thirdState, false)
        State.saveState(stateFile, forthState)
        forSameFiles(backup2, backup3, ::inodesShouldBeEqual)
        assertEquals(true, backup3_10.exists())
        assertEquals(false, backup1.exists())
        forSameFiles(backup3, backup3_10, ::inodesShouldBeEqual)

        val fifthState = Backuper(Storage(), threeDaysLater).doBackup(forthState, false)
        State.saveState(stateFile, fifthState)
        forSameFiles(backup3, backup4, ::inodesShouldBeEqual)
    }

    @Test
    fun multipleSourcesFunctionalTest() {
        val userInput = "${srcDir1.absolutePath}\n" +
                "yes\n" +
                "${srcDir2.absolutePath}/\n" +
                "\n" +
                "${backupsDir.absolutePath}\n" +
                "10\n"
        System.setIn(ByteArrayInputStream(userInput.toByteArray()))
        jeb.inReader = null
        main(arrayOf("init"))

        val srcContent1 = dir(srcDir1) {
            file("file11") {
                "content11"
            }
            dir("dir11") {
                file("file12") {
                    "content12"
                }
            }
        }
        srcContent1.create()
        val srcContent2 = dir(srcDir2) {
            file("file21") {
                "content21"
            }
            dir("dir21") {
                file("file22") {
                    "content22"
                }
            }
        }
        srcContent2.create()
        val backupContent = dir(backup1) {
            dir("source1") {
                file("file11") {
                    "content11"
                }
                dir("dir11") {
                    file("file12") {
                        "content12"
                    }
                }
            }
            file("file21") {
                "content21"
            }
            dir("dir21") {
                file("file22") {
                    "content22"
                }
            }
        }

        main(arrayOf("backup", backupsDir.absolutePath))
        assertTrue(backupContent.contentEqualTo(backup1))
        forSameFiles(srcDir1, backup1) { file1, file2 -> assertNotEquals(file1.inode, file2.inode) }

        val secondState = State.loadState(stateFile).result
        Backuper(Storage(), tomorrow).doBackup(secondState, false)
        forSameFiles(backup1, backup2, ::inodesShouldBeEqual)
    }

    @Test
    fun notExistingConfig() {
        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))
        val configPath = "${baseDir.absolutePath}/not-existing"
        main(arrayOf("backup", configPath))
        assertEquals("jeb-k config is not found at $configPath/jeb.json", String(out.toByteArray(), 0, out.size()).trim())
    }

    @Test
    fun malformedConfig() {
        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))
        val configPath = "${baseDir.absolutePath}/malformed"
        val configFile = File(configPath, "jeb.json")
        with(configFile) {
            parentFile.mkdirs()
            writeText("{}")
        }
        main(arrayOf("backup", configPath))
        assertEquals("Invalid config file: \"{}\" ($configFile)", String(out.toByteArray(), 0, out.size()).trim())
    }

    @Test
    fun useDefaultSourceDir() {
        val testBaseDir = File(baseDir, "default-source-dir")
        val testBackupsDir = File(testBaseDir, "backups")
        testBackupsDir.mkdirs()
        System.setProperty("user.dir", File(testBaseDir, "source").absolutePath)
        val userInput = "\n" +
                "no\n" +
                "${testBackupsDir.absolutePath}\n" +
                "10\n"
        System.setIn(ByteArrayInputStream(userInput.toByteArray()))
        jeb.inReader = null
        main(arrayOf("init"))
        val state = State.loadState(File(testBackupsDir, "jeb.json")).result
        assertEquals(Source(File(testBaseDir, "source").absolutePath + "/"), state.source.first())
    }

    @Test
    fun useDefaultBackupsDir() {
        val testBaseDir = File(baseDir, "default-backups-dir")
        val testBackupsDir = File(testBaseDir, "backups")
        testBackupsDir.mkdirs()
        System.setProperty("user.dir", testBackupsDir.absolutePath)
        val userInput = "${File(testBaseDir, "sources")}/\n" +
                "no\n" +
                "\n" +
                "10\n"
        System.setIn(ByteArrayInputStream(userInput.toByteArray()))
        jeb.inReader = null
        main(arrayOf("init"))
        val state = State.loadState(File(testBackupsDir, "jeb.json")).result
        assertEquals(testBackupsDir.absolutePath + "/", state.backupsDir)
    }

    @Test
    fun useDefaultBackupsCount() {
        val testBaseDir = File(baseDir, "default-backups-dir")
        val testBackupsDir = File(testBaseDir, "backups")
        testBackupsDir.mkdirs()
        val userInput = "${File(testBaseDir, "sources")}\n" +
                "no\n" +
                "$testBackupsDir\n" +
                "\n"
        System.setIn(ByteArrayInputStream(userInput.toByteArray()))
        jeb.inReader = null
        main(arrayOf("init"))
        val state = State.loadState(File(testBackupsDir, "jeb.json")).result
        assertEquals(10, state.lastTapeNumber)
    }

    @Test
    fun useMultipleSources() {
        val testBaseDir = File(baseDir, "default-backups-dir")
        val testBackupsDir = File(testBaseDir, "backups")
        testBackupsDir.mkdirs()
        val userInput = "${File(testBaseDir, "source1")}\n" +
                "yes\n" +
                "${File(testBaseDir, "source2")}/\n" +
                "\n" +
                "$testBackupsDir\n" +
                "\n"
        System.setIn(ByteArrayInputStream(userInput.toByteArray()))
        jeb.inReader = null
        main(arrayOf("init"))
        val state = State.loadState(File(testBackupsDir, "jeb.json")).result
        assertEquals(Source(File(testBaseDir, "source1").absolutePath), state.source[0])
        assertEquals(Source(File(testBaseDir, "source2").absolutePath + "/"), state.source[1])
    }

    @Test
    fun forceBackup() {
        val userInput = "${srcDir1.absolutePath}/\n" +
                "\n" +
                "${backupsDir.absolutePath}\n" +
                "10\n"
        System.setIn(ByteArrayInputStream(userInput.toByteArray()))
        jeb.inReader = null
        main(arrayOf("init"))

        val srcContent = dir(srcDir1) {
            file("file1") {
                "content1"
            }
            dir("dir1") {
                file("file2") {
                    "content2"
                }
            }
        }
        srcContent.create()

        main(arrayOf("backup", backupsDir.absolutePath))
        main(arrayOf("backup", "--force", backupsDir.absolutePath))

        assertTrue(srcContent.contentEqualTo(backup1_2))
        forSameFiles(backup1, backup1_2, ::inodesShouldBeEqual)
    }

    @After
    fun tearDown() {
        baseDir.deleteRecursively()
    }
}

private fun inodesShouldBeEqual(file1: File, file2: File) = assertEquals(file1.inode, file2.inode, "Files $file1 & $file2 should be same")
