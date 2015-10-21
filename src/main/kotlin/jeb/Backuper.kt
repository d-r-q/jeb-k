package jeb

import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class Backuper(private val io: Io) {

    fun doBackup(state: State): State {
        if (io.fileExists(File(state.backupsDir), ::modifiedToday)) {
            return state
        }
        val hanoi = with(state.hanoi) {
            if (done) reset() else this
        }

        val (from, to) = hanoi.nextMove()
        val disk = hanoi[from].last()
        val newHanoi = hanoi.moveDisk(from, to)

        val latestBackup = io.latestDir(File(state.backupsDir))
        val fromDir = File(state.source)
        val lastTape = File(state.backupsDir, state.hanoi.largestDisc.toString())
        val tape = File(state.backupsDir, disk.toString())

        val createBackup: (File) -> Unit =
                if (latestBackup == null) { to -> io.copy(fromDir, to) }
                else { to -> io.sync(fromDir, latestBackup, to) }

        val prepareTape: (File) -> Unit =
                if (io.fileExists(lastTape)) { tape -> io.remove(tape) }
                else { tape -> io.move(tape, lastTape) }

        backupTo(tape, createBackup, prepareTape)

        return state.copy(hanoi = newHanoi)
    }

    private fun backupTo(tape: File, createBackup: (File) -> Unit, prepareTape: (File) -> Unit) {
        val tmpTape = File(tape.parentFile, "${tape.name}-${System.currentTimeMillis()}")
        createBackup(tmpTape)
        prepareTape(tape)
        io.move(tmpTape, tape)
    }

}

private fun modifiedToday(f: File) = Date(f.lastModified()).toLocalDate() == LocalDate.now()

private fun Date.toLocalDate() = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
