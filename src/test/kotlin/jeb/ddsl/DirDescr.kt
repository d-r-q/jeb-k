package jeb.ddsl

import java.io.File

class DirDescr(name: File) : Node(name) {

    private val nodes = arrayListOf<Node>()

    fun file(name: String, content: () -> String) {
        nodes.add(FileDescr(File(this.name, name), content))
    }

    fun dir(name: String, content: DirDescr.() -> Unit) {
        val dir = DirDescr(File(this.name, name))
        nodes.add(dir)
        dir.content()
    }

    override fun create() {
        name.mkdirs()
        nodes.forEach { it.create() }
    }

    override fun contentEqualTo(file: File): Boolean {
        if (file.isDirectory) {
            val files = file.listFiles()
            return files.all { f ->
                val node = nodes.find { it.name.name == f.name }
                node != null && node.contentEqualTo(f)
            }
        } else {
            return false
        }
    }

}

inline fun dir(name: File, content: DirDescr.() -> Unit): DirDescr {
    val dir = DirDescr(name)
    dir.content()
    return dir
}
