package jeb

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

open class Storage {

    private val log = jeb.log

    open fun lastModified(dir: File, predicate: (File) -> Boolean) = dir.
            listFiles().
            filter { predicate(it) }.
            maxBy { it.lastModified() }

    open fun fileExists(dir: File, predicate: (File) -> Boolean) = dir.
            listFiles().
            filter(predicate).
            any()

    open fun fileExists(file: File) = file.exists()

    open fun remove(f: File) = f.deleteRecursively()

    open fun fullBackup(from: List<Source>, excludeFile: Path?, to: File) =
            rsync(from, excludeFile, null, to)

    open fun incBackup(from: List<Source>, excludeFile: Path?, base: File, to: File) =
            rsync(from, excludeFile, base, to)

    private fun rsync(sources: List<Source>, excludeFile: Path?, base: File?, dest: File) =
            try {
                // when single file is passed as source to rsync, then it's interprets
                // destination as full file name and do not create parent directory
                // but when sources is more than one or it's a directory, then it's
                // interprets destination as parent directory and creates it
                val (from, to) =
                        if (sources.size > 1 || sources.first().path.toFile().isDirectory) {
                            Pair(sources.map { toRsync(it) }.joinToString(" "),
                                    dest.absolutePath)
                        } else {
                            dest.absoluteFile.mkdirs()
                            Pair(sources.first().absolutePath,
                                    Paths.get(dest.absolutePath, sources.first().path.fileName.toString()))
                        }
                !"rsync -avh ${excludeFile?.let { "--exclude-from=" + it} ?: ""} --delete ${base?.let { "--link-dest=" + it.absolutePath } ?: ""} $from $to"
            } catch (e: JebExecException) {
                if (e.stderr.contains("vanished") && e.stderr.contains("/.sync/status")) {
                    // it's workaround for case, when service's status file has been changed, while syncing
                    // it's harmless and in this case follow output printed:
                    // file has vanished: "/data/yandex-disk/.sync/status"
                    // rsync warning: some files vanished before they could be transferred (code 24) at main.c(1183) [sender=3.1.0]

                    // Do nothing in this case
                    log.warn("Rsync error ignored")
                    log.warn(e.toString())
                } else {
                    throw e
                }
            }

    open fun move(from: File, to: File) {
        !"mv ${from.absolutePath} ${to.absolutePath}"
        Thread.sleep(1000) // Dirty hack for functional test...
        to.setLastModified(System.currentTimeMillis())
    }

    open fun findOne(dir: File, predicate: (File) -> Boolean): File? {
        val res = dir.
                listFiles().
                filter(predicate)
        assert(res.size <= 1, { "To many files matches predicate: $res"})
        return res.firstOrNull()
    }

    private fun toRsync(src: Source) = src.path.toString() + (if (src.type == BackupType.DIRECTORY) "/" else "")

    private operator fun String.not() {
        log.debug("Executing command: $this")
        val proc = Runtime.getRuntime().exec(this)
        val stdoutLines = LinkedList<String>()
        val stderrLines = LinkedList<String>()
        proc.inputStream.bufferedReader().lines().forEach(handleStdOutputLine(stdoutLines, OutputType.STDOUT))
        proc.errorStream.bufferedReader().lines().forEach(handleStdOutputLine(stderrLines, OutputType.STDERR))
        val returnCode = proc.waitFor()
        log.debug("""
            returnCode=$returnCode
            stdout:
            ======
            ${stdoutLines.joinToString("\n")}
            ======
            stderr:
            ======
            ${stderrLines.joinToString("\n")}
            ======
        """.trimIndent())
        if (returnCode != 0 || stderrLines.isNotEmpty()) {
            throw JebExecException(
                    cmd = this,
                    stdout = stdoutLines.joinToString("\n"),
                    stderr = stderrLines.joinToString("\n"),
                    returnCode = returnCode)
        }
    }

    private fun handleStdOutputLine(buffer: LinkedList<String>, type: OutputType): (String) -> Unit = {
        val msg = "[$type]: $it"
        when (type) {
            OutputType.STDOUT -> log.debug(msg)
            OutputType.STDERR -> log.error(msg)
            else -> throw AssertionError("Unknown type: $type")
        }

        buffer.add(it)
        if (buffer.size > 1000) {
            buffer.removeFirst()
        }
    }

    private enum class OutputType{ STDOUT, STDERR }

}

