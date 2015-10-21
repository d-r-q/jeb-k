package jeb.ddsl

import java.io.File

abstract class Node(protected val name: File) {

    abstract fun create()

    abstract fun contentEqualTo(file: File): Boolean

}
