package jeb

import java.io.File

class Backuper(private val io: Io) {

    fun doBackup(state: State): State {
        val hanoi = with(state.hanoi) {
            if (done) reset() else this
        }

        val (from, to)  = hanoi.nextMove()
        val disk = hanoi[from].last()
        val newHanoi = hanoi.moveDisk(from, to)

        val latestBackup = io.latestDir(state.backupsDir)
        backupTo(File(state.backupsDir, disk.toString()), File(state.source), latestBackup)

        return state.copy(hanoi = newHanoi)
    }

    private fun backupTo(tape: File, from: File, base: File?) {
        io.remove(tape)
        if (base == null) {
            io.copy(from, tape)
        } else {
            io.sync(from, base, tape)
        }
    }

}
