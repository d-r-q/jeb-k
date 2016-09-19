package jeb

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Abstraction of backup source, actually introduced only because the standard File and Path throws away last slash,
 * which is used to specify the rsync to copy the folder or its contents.
 * So Source it is path and backup method of this path, which in the current implementation is expressed in presence last slash.
 */
class Source(val absolutePath: String) {

    val path: Path = Paths.get(absolutePath)

    val type = if (absolutePath.endsWith("/")) BackupType.DIRECTORY else BackupType.CONTENT

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Source

        if (path != other.path) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int{
        var result = path.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String{
        return "Source(path=$path, type=$type)"
    }


}

enum class BackupType { DIRECTORY, CONTENT }