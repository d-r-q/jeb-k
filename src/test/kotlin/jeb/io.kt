package jeb

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

val File.inode: String
    get() {
        val attr = Files.readAttributes(this.toPath(), BasicFileAttributes::class.java)
        return with(attr.fileKey().toString()) {
            substring(indexOf("ino=") + 4, indexOf(")"))
        }
    }

fun forSameFiles(dir1: File, dir2: File, body: (File, File) -> Unit) {
    dir1.walkTopDown().forEach {
        if (it.isFile) {
            val subPath = it.absolutePath.substring(dir1.absolutePath.length + 1)
            val file2 = dir2.toPath().resolve(subPath).toFile()
            if (file2.exists()) {
                body(it, file2)
            }
        }
    }
}
