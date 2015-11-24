package jeb

import java.io.File
import java.time.LocalDateTime
import javax.swing.JOptionPane

val usage = "Usage: java jeb.jeb <init|backup dir>"

public fun main(args: Array<String>) {
    when {
        args.size == 0 -> println(usage)
        args[0] == "init" -> init()
        args[0] == "backup" -> backup(File(args[1]), LocalDateTime.now())
        else -> println(usage)
    }
}

fun init() {
    val sourceDir = with(readLine("Source directory: ")) {
        if (this.endsWith('/')) this else "$this/"
    }

    val backupDir = readLine("Backups directory: ")
    val disksCount = readLine("Backups count: ").toInt()

    val state = State(backupDir, sourceDir, Hanoi(disksCount))
    State.saveState(File(backupDir, "jeb.json"), state)
}

fun backup(backupDir: File, time: LocalDateTime) {
    val config = File(backupDir, "jeb.json")
    val state = State.loadState(config)
    try {
        val newState = Backuper(Storage(), time).doBackup(state)
        State.saveState(config, newState)
    } catch(e: JebExecException) {
        JOptionPane.showMessageDialog(null, e.toString(), "title", JOptionPane.ERROR_MESSAGE)
    }

}

// Hack for tests: do not recreate buffered reader on each call
val inReader = System.`in`.bufferedReader()
private fun readLine(invitation: String): String {
    print(invitation)
    return inReader.readLine()
}
