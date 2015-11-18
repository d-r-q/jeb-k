package jeb

import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.text.Regex

class Backuper(private val storage: Storage) {

    private val log = LoggerFactory.getLogger(javaClass)

    public fun doBackup(state: State): State {
        if (storage.fileExists(File(state.backupsDir), { isTape(it, state) && modifiedToday(it) })) {
            log.info("Tape modified today exists, so halt backup")
            return state
        }

        val (newState, nextTapeNum) = state.selectTape()
        log.info("Next tape number = $nextTapeNum")

        val lastBackup = storage.lastModified(File(state.backupsDir), { isTape(it, state) })
        val fromDir = File(state.source)
        val lastTape = File(state.backupsDir, toFileName(state.lastTapeNumber))
        val tape = File(state.backupsDir, toFileName(nextTapeNum))
        val tmpTape = File(tape.parentFile, "${tape.name}-${System.currentTimeMillis()}")
        log.debug("""
            lastBackup=$lastBackup
            fromDir=$fromDir
            lastTape=$lastTape
            tape=$tape
            tmpTape=$tmpTape
        """.trimIndent())

        createBackup(from = fromDir, base = lastBackup, to = tmpTape)
        if (storage.fileExists(tape)) {
            prepareTape(tape = tape, lastTape = lastTape)
        }
        log.info("Moving backup from $tmpTape to $tape")
        storage.move(from = tmpTape, to = tape)

        return newState
    }

    private fun createBackup(from: File, base: File?, to: File) {
        if (base == null) {
            log.info("Base backup not found, creating original backup")
            storage.copy(from, to)
        } else {
            log.info("Base backup found at $base")
            storage.sync(from, base, to)
        }
    }

    private fun prepareTape(tape: File, lastTape: File) {
        if (storage.fileExists(lastTape)) {
            log.info("Last tape taken, so cleaning $tape")
            storage.remove(tape)
        } else {
            log.info("Last tape free, so moving $tape to $lastTape")
            storage.move(tape, lastTape)
        }
    }
}

private fun modifiedToday(f: File) =
        Date(f.lastModified()).toLocalDate() == LocalDate.now()


private fun Date.toLocalDate() = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

fun isTape(f: File, state: State): Boolean {
    if (!f.isDirectory) {
        return false
    }

    val maxTapeDigits = state.lastTapeNumber.toString().length
    val pattern = """\d{8}-(\d{$maxTapeDigits})"""
    val tapeNum = Regex(pattern).matchEntire(f.name)?.groups?.get(1)?.value?.toInt()
    return tapeNum?.let { it > 0 && it <= state.lastTapeNumber } ?: false
}

private fun toFileName(tapeNum: Int) = "${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}-${tapeNum}"

