package jeb

import jeb.util.Try
import java.io.BufferedReader
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime

val usage = "Usage: jeb-k <init|backup [--force] <dir>>"

fun main(args: Array<String>) {
    when {
        args.size == 0 -> println(usage)
        args[0] == "init" -> init()
        args[0] == "backup" -> {
            val force = args[1] == "--force"
            val dir = if (force) {
                args[2]
            } else {
                args[1]
            }
            backup(File(dir), LocalDateTime.now(), force)
        }
        else -> println(usage)
    }
}

fun init() {
    val currentDir = System.getProperty("user.dir").withTailingSlash()
    val sourceDirs = readSourceDirs(currentDir)

    val backupDirDefault = if (sourceDirs.contains(currentDir)) null else currentDir
    val backupDir = readLine("Backups directory", backupDirDefault)
    val disksCount = readLine("Backups count", "10").toInt()

    val state = State(backupDir, sourceDirs.map(::Source), Hanoi(disksCount))
    State.saveState(File(backupDir, "jeb.json"), state)
}

private fun backup(backupDir: File, time: LocalDateTime, force: Boolean) {
    var config = File(backupDir, "jeb.json")
    if (!config.exists()) {
        config = backupDir
    }
    if (!config.exists()) {
        println("jeb-k config is not found at ${config.absolutePath}")
        return
    }

    val state = State.loadState(config)
    when (state) {
        is Try.Success -> doBackup(config, state.result, time, force)
        is Try.Failure -> println(state.reason.message)
    }
}

private fun doBackup(config: File, state: State, time: LocalDateTime, force: Boolean) {
    try {
        var excludesFile = Paths.get(state.backupsDir, "excludes.txt")
        if (!excludesFile.toFile().exists()) {
            excludesFile = Paths.get(config.parent, "excludes.txt")
            if (!excludesFile.toFile().exists()) {
                excludesFile = null
            }
        }
        val newState = Backuper(Storage(), time).doBackup(state, force, excludesFile)
        when (newState) {
            is Try.Success -> newState.result?.let { State.saveState(config, it) }
            is Try.Failure -> log.error(newState.reason)
        }
    } catch(e: JebExecException) {
        log.error(e)
    }
}

private fun readSourceDirs(currentDir: String): List<String> {
    val sources = arrayListOf<String>()
    var defaultSourceDir: String? = currentDir
    do {
        val invitation = if (sources.size == 0) "Source directory (add trailing slash to backup directory content or leave it blank to backup directory)\n"
        else "Source directory"
        val source = readLine(invitation, defaultSourceDir)
        if (source == defaultSourceDir) {
            defaultSourceDir = null
        }
        sources.add(source)
    } while (readLine("Add another source? [yes/No]", "no").toLowerCase() == "yes")
    return sources
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
