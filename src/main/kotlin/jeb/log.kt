package jeb

import java.io.PrintStream

interface Log {

    fun info(msg: String)

    fun debug(msg: String)

    fun warn(msg: String)

    fun error(msg: String)

}

val log: Log = stdLog

object stdLog : Log {
    private val level = LogLevel.valueOf(System.getProperty("jeb.log.level", "INFO").toUpperCase())

    override fun debug(msg: String) {
        print(LogLevel.DEBUG, msg)
    }

    override fun info(msg: String) {
        print(LogLevel.INFO, msg)
    }

    override fun warn(msg: String) {
        print(LogLevel.WARN, msg)
    }

    override fun error(msg: String) {
        print(LogLevel.ERROR, msg)
    }

    private fun print(level: LogLevel, msg: String) {
        if (this.level.ordinal <= level.ordinal) {
            level.out.println(msg)
        }
    }

    private enum class LogLevel(val out: PrintStream) { DEBUG(System.out), INFO(System.out), WARN(System.out), ERROR(System.err) }
}
