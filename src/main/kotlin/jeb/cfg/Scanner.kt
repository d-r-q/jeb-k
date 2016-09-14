package jeb.cfg

class Scanner(str: String) {

    private var idx = 0
    private val chars = str.toCharArray()

    init {
        skipWhitespaces()
    }

    fun expect(expected: Char) {
        skipWhitespaces()
        if (chars[idx++] != expected) {
            throw ParseException("$expected expected, ${chars[idx - 1]} got")
        }
    }

    fun current(): Char {
        skipWhitespaces()
        return chars[idx]
    }

    fun scan() =
            if (idx < chars.size) chars[idx++]
            else (-1).toChar()

    fun back(count: Int) {
        idx = Math.max(0, idx - count)
    }

    fun swallow(c: Char): Boolean {
        skipWhitespaces()
        if (chars[idx] == c) {
            idx++
            return true
        } else {
            return false
        }
    }

    private fun skipWhitespaces() {
        while (idx < chars.size && chars[idx].isWhitespace()) {
            idx++
        }
    }
}
