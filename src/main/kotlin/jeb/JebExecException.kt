package jeb

import java.io.IOException

class JebExecException(
        public val cmd: String,
        public val stdout: String,
        public val stderr: String,
        public val returnCode: Int,
        cause: Throwable? = null) : IOException("Could not execute $cmd", cause) {

    override fun toString(): String {
        return """
        |Jeb error
        |Command: $cmd
        |Return code: $returnCode
        |Standard Out:
        |$stdout
        | ====
        |Standard Error:
        |$stderr
        | ====
        """.trimMargin()
    }
}
