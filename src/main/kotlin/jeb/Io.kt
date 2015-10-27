package jeb

import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

open class Io {

    private val log = LoggerFactory.getLogger(javaClass)

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

    open fun copy(from: File, to: File) =
            !"cp -r ${from.absolutePath} ${to.absolutePath}"

    open fun sync(from: File, base: File, to: File) =
            !"rsync -avh --delete --link-dest=${base.absolutePath} ${from.absolutePath}/ ${to.absolutePath}"

    open fun move(from: File, to: File) =
            !"mv ${from.absolutePath} ${to.absolutePath}"

    private operator fun String.not() {
        log.debug("Executing command: $this")
        val proc = Runtime.getRuntime().exec(this)
        val returnCode = proc.waitFor()
        val stdoutLines = LinkedList<String>()
        val stderrLines = LinkedList<String>()
        proc.inputStream.bufferedReader().lines().forEach(handleStdOutputLine(stdoutLines, OutputType.STDOUT))
        proc.errorStream.bufferedReader().lines().forEach(handleStdOutputLine(stderrLines, OutputType.STDERR))
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

