package jeb

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Abstraction of backup source, actually introduced only because the standard File and Path throws away last slash,
 * which is used to specify the rsync to copy the folder or its contents.
 * So Source it is path and backup method of this path, which in the current implementation is expressed in presence last slash.
 */
class Source private constructor(val path: Path, val type: BackupType) {

    constructor(path: File, type: BackupType) : this(path.toPath(), type)

    constructor(absolutePath: String) : this(Paths.get(absolutePath), backupType(absolutePath))

    val absolutePath = path.toString() + (if (type == BackupType.DIRECTORY) "/" else "")

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

fun backupType(absolutePath: String) = if (absolutePath.endsWith("/")) BackupType.DIRECTORY else BackupType.CONTENT