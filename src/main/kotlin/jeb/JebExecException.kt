package jeb

import java.io.IOException

class JebExecException(
        val cmd: String,
        val stdout: String,
        val stderr: String,
        val returnCode: Int,
        cause: Throwable? = null) : IOException("Could not execute $cmd")
