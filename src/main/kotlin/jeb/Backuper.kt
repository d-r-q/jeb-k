package jeb

import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class Backuper(private val io: Io) {

    fun doBackup(state: State): State {
        val modifiedToday = { f: File -> Date(f.lastModified()).toLocalDate() == LocalDate.now() }
        if (io.fileExists(File(state.backupsDir), modifiedToday)) {
            return state
        }
        val hanoi = with(state.hanoi) {
            if (done) reset() else this
        }

        val (from, to)  = hanoi.nextMove()
        val disk = hanoi[from].last()
        val newHanoi = hanoi.moveDisk(from, to)

        val latestBackup = io.latestDir(File(state.backupsDir))
        backupTo(File(state.backupsDir, disk.toString()), File(state.source), latestBackup)

        return state.copy(hanoi = newHanoi)
    }

    private fun backupTo(tape: File, from: File, base: File?) {
        val tmpTape = File(tape.parentFile, "${tape.name}-${System.currentTimeMillis()}")
        if (base == null) {
            io.copy(from, tmpTape)
        } else {
            io.sync(from, base, tmpTape)
        }
        io.remove(tape)
        io.move(tmpTape, tape)
    }

}

private fun Date.toLocalDate() = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
