package jeb

import java.io.IOException

class JebExecException(
        public val cmd: String,
        public val stdout: String,
        public val stderr: String,
        public val returnCode: Int,
        cause: Throwable? = null) : IOException("Could not execute $cmd", cause)
