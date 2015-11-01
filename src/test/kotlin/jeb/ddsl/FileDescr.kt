package jeb.ddsl

import java.io.File

class FileDescr(
        name: File,
        fileContent: () -> String) : Node(name) {

    private val content by lazy { fileContent() }

    override fun create() {
        name.writeText(content)
    }

    override fun contentEqualTo(file: File) = file.isFile && content == file.readText()

}
