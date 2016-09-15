package jeb

import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.text.Regex

class Backuper(private val storage: Storage, private val now: LocalDateTime) {

    private val log = jeb.log

    fun doBackup(state: State): State {
        if (storage.fileExists(File(state.backupsDir), { isTape(it, state) && modifiedToday(it) })) {
            log.info("Tape modified today exists, so halt backup")
            return state
        }

        val (newState, nextTapeNum) = state.selectTape()
        log.info("Next tape number = $nextTapeNum")

        val lastBackup = storage.lastModified(File(state.backupsDir), { isTape(it, state) })
        val fromDir = File(state.source)
        val lastTape = storage.findOne(File(state.backupsDir), fileForTape(state, state.lastTapeNumber)) ?: File(state.backupsDir, toFileName(state.lastTapeNumber))
        val tape = File(state.backupsDir, toFileName(nextTapeNum))
        val prevTape = storage.findOne(File(state.backupsDir), fileForTape(state, nextTapeNum))
        val tmpTape = File(tape.parentFile, "${tape.name}-${now.toEpochMilli()}")
        log.debug("""
            lastBackup=$lastBackup
            fromDir=$fromDir
            lastTape=$lastTape
            tape=$tape
            tmpTape=$tmpTape
        """.trimIndent())

        createBackup(from = fromDir, base = lastBackup, to = tmpTape)
        if (prevTape != null) {
            prepareTape(tape = prevTape, lastTape = lastTape)
        }
        log.info("Moving backup from $tmpTape to $tape")
        storage.move(from = tmpTape, to = tape)

        return newState
    }

    private fun createBackup(from: File, base: File?, to: File) {
        if (base == null) {
            log.info("Base backup not found, creating full backup")
            storage.fullBackup(from, to)
        } else {
            log.info("Base backup found at $base, creating incremental backup")
            storage.incBackup(from, base, to)
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

    private fun modifiedToday(f: File) =
            Date(f.lastModified()).toLocalDate() == now.toLocalDate()

    private fun toFileName(tapeNum: Int) = "${now.format(DateTimeFormatter.BASIC_ISO_DATE)}-$tapeNum"

}

private fun Date.toLocalDate() = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

private fun LocalDateTime.toEpochMilli() = this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun isTape(f: File, state: State): Boolean {
    if (!f.isDirectory) {
        return false
    }

    val maxTapeDigits = state.lastTapeNumber.toString().length
    val pattern = """\d{8}-(\d{1,$maxTapeDigits})"""
    val tapeNum = Regex(pattern).matchEntire(f.name)?.groups?.get(1)?.value?.toInt()
    return tapeNum?.let { it > 0 && it <= state.lastTapeNumber } ?: false
}


private fun fileForTape(state: State, tape: Int): (File) -> Boolean = { isTape(it, state) && it.absolutePath.endsWith("-$tape") }

