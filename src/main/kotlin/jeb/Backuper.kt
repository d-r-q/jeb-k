package jeb

import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class Backuper(private val io: Io) {

    public fun doBackup(state: State): State {
        if (io.fileExists(File(state.backupsDir), ::dirModifiedToday)) {
            return state
        }

        val (newState, nextTapeNum) = state.selectTape()

        val lastBackup = io.lastModifiedDir(File(state.backupsDir))
        val fromDir = File(state.source)
        val lastTape = File(state.backupsDir, state.lastTapeNumber.toString())
        val tape = File(state.backupsDir, nextTapeNum.toString())
        val tmpTape = File(tape.parentFile, "${tape.name}-${System.currentTimeMillis()}")

        createBackup(from = fromDir, base = lastBackup, to = tmpTape)
        if (io.fileExists(tape)) {
            prepareTape(tape = tape, lastTape = lastTape)
        }
        io.move(from = tmpTape, to = tape)

        return newState
    }

    private fun createBackup(from: File, base: File?, to: File) {
        if (base == null) io.copy(from, to)
        else io.sync(from, base, to)
    }

    private fun prepareTape(tape: File, lastTape: File) {
        if (io.fileExists(lastTape)) io.remove(tape)
        else io.move(tape, lastTape)
    }
}

private fun dirModifiedToday(f: File) = f.isDirectory && Date(f.lastModified()).toLocalDate() == LocalDate.now()

private fun Date.toLocalDate() = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
