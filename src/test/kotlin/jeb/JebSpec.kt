package jeb

import jeb.ddsl.dir
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
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
        main(arrayOf("init"))

        val state = State.loadState(stateFile)
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
        with (State.loadState(stateFile)) {
            assertEquals(2, this.selectTape().second)
        }

        val secondState = State.loadState(stateFile)
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

    @After
    fun tearDown() {
        //baseDir.deleteRecursively()
    }
}

private fun inodesShouldBeEqual(file1: File, file2: File) = assertEquals(file1.inode, file2.inode, "Files $file1 & $file2 should be same")
