package jeb

import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class Backuper(private val io: Io) {

    public fun doBackup(state: State): State {
        if (io.fileExists(File(state.backupsDir), ::modifiedToday)) {
            return state
        }

        val (newState, nextTapeNum) = selectTape(state)

        val lastBackup = io.lastModifiedDir(File(state.backupsDir))
        val fromDir = File(state.source)
        val lastTape = File(state.backupsDir, state.lastTapeNumber.toString())
        val tape = File(state.backupsDir, nextTapeNum.toString())
        val tmpTape = File(tape.parentFile, "${tape.name}-${System.currentTimeMillis()}")

        createBackup(fromDir, lastBackup, tmpTape)
        prepareTape(tape, lastTape)
        io.move(tmpTape, tape)

        return newState
    }

    private fun createBackup(fromDir: File, latestBackup: File?, to: File) {
        if (latestBackup == null) io.copy(fromDir, to)
        else io.sync(fromDir, latestBackup, to)
    }

    private fun prepareTape(tape: File, lastTape: File) {
        if (io.fileExists(lastTape)) io.remove(tape)
        else io.move(tape, lastTape)
    }
}

private fun selectTape(state: State): Pair<State, Int> {
    val hanoi = with(state.hanoi) {
        if (done) reset() else this
    }

    val (from, to) = hanoi.nextMove()
    val disk = hanoi[from].last()
    val newHanoi = hanoi.moveDisk(from, to)

    return Pair(state.copy(hanoi = newHanoi), disk)
}

private fun modifiedToday(f: File) = Date(f.lastModified()).toLocalDate() == LocalDate.now()

private fun Date.toLocalDate() = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
