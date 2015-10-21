package jeb.ddsl

import java.io.File

class FileDescr(
        name: File,
        fileContent: () -> String) : Node(name) {

    private val content = lazy { fileContent() }

    override fun create() {
        name.writeText(content.value)
    }

    override fun contentEqualTo(file: File) = file.isFile && content.value == file.readText()

}
