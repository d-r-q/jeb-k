package jeb

import jeb.util.Try
import java.io.BufferedReader
import java.io.File
import java.time.LocalDateTime
import javax.swing.JOptionPane

val usage = "Usage: jeb-k <init|backup <dir>>"

fun main(args: Array<String>) {
    when {
        args.size == 0 -> println(usage)
        args[0] == "init" -> init()
        args[0] == "backup" -> backup(File(args[1]), LocalDateTime.now())
        else -> println(usage)
    }
}

fun init() {
    val currentDir = System.getProperty("user.dir").withTailingSlash()
    val sourceDir = readLine("Source directory", currentDir).withTailingSlash()

    val backupDirDefault = if (File(currentDir) != File(sourceDir)) currentDir else null
    val backupDir = readLine("Backups directory", backupDirDefault)
    val disksCount = readLine("Backups count", "10").toInt()

    val state = State(backupDir, listOf(Source(sourceDir)), Hanoi(disksCount))
    State.saveState(File(backupDir, "jeb.json"), state)
}

fun backup(backupDir: File, time: LocalDateTime) {
    val config = File(backupDir, "jeb.json")
    if (!config.exists()) {
        println("jeb-k config is not found at ${config.absolutePath}")
        return
    }

    val state = State.loadState(config)
    when (state) {
        is Try.Success -> doBackup(config, state.result, time)
        is Try.Failure -> println(state.reason.message)
    }
}

private fun doBackup(config: File, state: State, time: LocalDateTime) {
    try {
        val newState = Backuper(Storage(), time).doBackup(state)
        State.saveState(config, newState)
    } catch(e: JebExecException) {
        JOptionPane.showMessageDialog(null, e.toString(), "title", JOptionPane.ERROR_MESSAGE)
    }
}

// Hack for tests: do not recreate buffered reader on each call
var inReader: BufferedReader? = null
    get() {
        if (field == null) field = System.`in`.bufferedReader()
        return field
    }

private fun readLine(invitation: String, default: String?): String {
    val defaultMsg =
            if (default != null) " [Leave empty to use $default]"
            else ""
    print("$invitation$defaultMsg:")
    val line = inReader!!.readLine()
    return if (line.length > 0 || default == null) line else default
}

private fun String.withTailingSlash() = if (this.endsWith("/")) this else this + "/"
