package jeb.ddsl

import java.io.File

abstract class Node(val name: File) {

    abstract fun create()

    abstract fun contentEqualTo(file: File): Boolean

}
