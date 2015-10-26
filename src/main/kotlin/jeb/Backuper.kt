package jeb

import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class Backuper(private val io: Io) {

    private val log = LoggerFactory.getLogger(javaClass)

    public fun doBackup(state: State): State {
        if (io.fileExists(File(state.backupsDir), { isTape(it, state) && modifiedToday(it) })) {
            log.info("Tape modified today exists, so halt backup")
            return state
        }

        val (newState, nextTapeNum) = state.selectTape()
        log.info("Next tape number = $nextTapeNum")

        val lastBackup = io.lastModified(File(state.backupsDir), { isTape(it, state) })
        val fromDir = File(state.source)
        val lastTape = File(state.backupsDir, state.lastTapeNumber.toString())
        val tape = File(state.backupsDir, nextTapeNum.toString())
        val tmpTape = File(tape.parentFile, "${tape.name}-${System.currentTimeMillis()}")
        log.debug("""
        lastBackup=$lastBackup
        fromDir=$fromDir
        lastTape=$lastTape
        tape=$tape
        tmpTape=$tmpTape
        """.trimIndent())

        createBackup(from = fromDir, base = lastBackup, to = tmpTape)
        if (io.fileExists(tape)) {
            prepareTape(tape = tape, lastTape = lastTape)
        }
        log.info("Moving backup from $tmpTape to $tape")
        io.move(from = tmpTape, to = tape)

        return newState
    }

    private fun createBackup(from: File, base: File?, to: File) {
        if (base == null) {
            log.info("Base backup not found, creating original backup")
            io.copy(from, to)
        }
        else {
            log.info("Base backup found at $base")
            io.sync(from, base, to)
        }
    }

    private fun prepareTape(tape: File, lastTape: File) {
        if (io.fileExists(lastTape)) {
            log.info("Last tape taken, so cleaning $tape")
            io.remove(tape)
        }
        else {
            log.info("Last tape free, so moving $tape to $lastTape")
            io.move(tape, lastTape)
        }
    }
}

private fun modifiedToday(f: File) =
        Date(f.lastModified()).toLocalDate() == LocalDate.now()


private fun Date.toLocalDate() = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

private fun isTape(f: File, state: State) =
        try {
            val tapeNum = f.name.toInt()
            f.isDirectory && tapeNum > 0 && tapeNum <= state.lastTapeNumber
        } catch (e: NumberFormatException) {
            false
        }
