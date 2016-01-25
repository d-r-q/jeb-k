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

    val now = LocalDateTime.now()
    val nowStr = now.format(DateTimeFormatter.BASIC_ISO_DATE)
    val tomorrow = now.plusDays(1)
    val tomorrowStr = tomorrow.format(DateTimeFormatter.BASIC_ISO_DATE)
    val twoDaysLater = now.plusDays(2)
    val twoDaysLaterStr = twoDaysLater.format(DateTimeFormatter.BASIC_ISO_DATE)
    val threeDaysLater = now.plusDays(3)
    val threeDaysLaterStr = threeDaysLater.format(DateTimeFormatter.BASIC_ISO_DATE)
    val baseDir = createTempDir("jeb-", "-funct-test")
    val srcDir = File(baseDir, "source")
    val backupsDir = File(baseDir, "backups")
    val backup1 = File(backupsDir, "$nowStr-1")
    val backup2 = File(backupsDir, "$tomorrowStr-2")
    val backup3 = File(backupsDir, "$twoDaysLaterStr-1")
    val backup3_10 = File(backupsDir, "$twoDaysLaterStr-10")
    val backup4 = File(backupsDir, "$threeDaysLaterStr-3")
    val stateFile = File(backupsDir, "jeb.json")

    @Test
    fun jebFunctionalTest() {

        val userInput = "${srcDir.absolutePath}\r\n" +
                "${backupsDir.absolutePath}\n" +
                "10\n"
        System.setIn(ByteArrayInputStream(userInput.toByteArray()))
        jeb.inReader = null
        main(arrayOf("init"))

        val state = State.loadState(stateFile).result
        assertEquals(srcDir.absolutePath + "/", state.source)
        assertEquals(backupsDir.absolutePath, state.backupsDir)
        assertEquals(10, state.lastTapeNumber)

        val srcContent = dir(srcDir) {
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
        forSameFiles(srcDir, backup1) { file1, file2 -> assertNotEquals(file1.inode, file2.inode) }

        File(srcDir, "file3").writeText("content3")

        main(arrayOf("backup", backupsDir.absolutePath))
        MatcherAssert.assertThat(backupsDir.listFiles().map { it.name }, Matchers.containsInAnyOrder("$nowStr-1", "jeb.json"))
        with (State.loadState(stateFile).result) {
            assertEquals(2, this.selectTape().second)
        }

        val secondState = State.loadState(stateFile).result
        val thirdState = Backuper(Storage(), tomorrow).doBackup(secondState)
        State.saveState(stateFile, thirdState)
        forSameFiles(backup1, backup2, ::inodesShouldBeEqual)

        val forthState = Backuper(Storage(), twoDaysLater).doBackup(thirdState)
        State.saveState(stateFile, forthState)
        forSameFiles(backup2, backup3, ::inodesShouldBeEqual)
        assertEquals(true, backup3_10.exists())
        assertEquals(false, backup1.exists())
        forSameFiles(backup3, backup3_10, ::inodesShouldBeEqual)

        val fifthState = Backuper(Storage(), threeDaysLater).doBackup(forthState)
        State.saveState(stateFile, fifthState)
        forSameFiles(backup3, backup4, ::inodesShouldBeEqual)
    }

    @Test
    fun notExistingConfig() {
        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))
        val configPath = "${baseDir.absolutePath}/not-existing"
        main(arrayOf("backup", configPath))
        assertEquals("jeb-k config is not found at $configPath/jeb.json", String(out.toByteArray(), 0, out.size()))
    }

    @Test
    fun malformedConfig() {
        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))
        val configPath = "${baseDir.absolutePath}/malformed"
        with(File(configPath, "jeb.json")) {
            parentFile.mkdirs()
            writeText("{}")
        }
        main(arrayOf("backup", configPath))
        assertEquals("jeb-k config at $configPath/jeb.json is malformed", String(out.toByteArray(), 0, out.size()))
    }

    @Test
    fun useDefaultSourceDir() {
        val testBaseDir = File(baseDir, "default-source-dir")
        val testBackupsDir = File(testBaseDir, "backups")
        testBackupsDir.mkdirs()
        System.setProperty("user.dir", File(testBaseDir, "source").absolutePath)
        val userInput = "\n" +
                "${testBackupsDir.absolutePath}\n" +
                "10\n"
        System.setIn(ByteArrayInputStream(userInput.toByteArray()))
        jeb.inReader = null
        main(arrayOf("init"))
        val state = State.loadState(File(testBackupsDir, "jeb.json")).result
        assertEquals(File(testBaseDir, "source").absolutePath + "/", state.source)
    }

    @Test
    fun useDefaultBackupsDir() {
        val testBaseDir = File(baseDir, "default-backups-dir")
        val testBackupsDir = File(testBaseDir, "backups")
        testBackupsDir.mkdirs()
        System.setProperty("user.dir", testBackupsDir.absolutePath)
        val userInput = "${File(testBaseDir, "sources")}\n" +
                "\n" +
                "10\n"
        System.setIn(ByteArrayInputStream(userInput.toByteArray()))
        jeb.inReader = null
        main(arrayOf("init"))
        val state = State.loadState(File(testBackupsDir, "jeb.json")).result
        assertEquals(testBackupsDir.absolutePath + "/", state.backupsDir)
    }

    @Test
    fun useDefaultBackupsCountDir() {
        val testBaseDir = File(baseDir, "default-backups-dir")
        val testBackupsDir = File(testBaseDir, "backups")
        testBackupsDir.mkdirs()
        val userInput = "${File(testBaseDir, "sources")}\n" +
                "$testBackupsDir\n" +
                "\n"
        System.setIn(ByteArrayInputStream(userInput.toByteArray()))
        jeb.inReader = null
        main(arrayOf("init"))
        val state = State.loadState(File(testBackupsDir, "jeb.json")).result
        assertEquals(10, state.lastTapeNumber)
    }

    @After
    fun tearDown() {
        baseDir.deleteRecursively()
    }
}

private fun inodesShouldBeEqual(file1: File, file2: File) = assertEquals(file1.inode, file2.inode, "Files $file1 & $file2 should be same")
