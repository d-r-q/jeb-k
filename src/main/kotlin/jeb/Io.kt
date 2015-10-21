package jeb

import java.io.File

open class Io {

    open fun latestDir(dir: String) = File(dir).
            listFiles().
            filter { it.isDirectory }.
            maxBy { it.lastModified() }

    open fun remove(f: File) = f.deleteRecursively()

    open fun copy(from: File, to: File) =
            !"cp -r ${from.absolutePath} ${to.absolutePath}"

    open fun sync(from: File, base: File, to: File) =
            !"rsync -avh --delete --link-dest=${base.absolutePath} ${from.absolutePath}/ ${to.absolutePath}"

    open fun mv(from: File, to: File) =
            !"mv ${from.absolutePath} ${to.absolutePath}"

    private operator fun String.not(): Int {
        val proc = Runtime.getRuntime().exec(this)
        return proc.waitFor()
    }

}
