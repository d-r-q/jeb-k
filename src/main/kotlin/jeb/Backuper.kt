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
        val tmpTape = File(tape.parentFile, "${tape.name}-${System.currentTimeMillis()}")
        if (base == null) {
            io.copy(from, tmpTape)
        } else {
            io.sync(from, base, tmpTape)
        }
        io.remove(tape)
        io.mv(tmpTape, tape)
    }

}
