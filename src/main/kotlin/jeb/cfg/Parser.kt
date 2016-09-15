package jeb.cfg

import jeb.util.Try
import java.util.*

object Parser {

    fun parse(input: String): Try<Json.Object> = Try.Success(parseObject(Scanner(input)))

    private fun parseObject(input: Scanner): Json.Object {
        input.expect('{')

        val fields = LinkedHashSet<Pair<String, Json<*>>>()
        val firstField: Pair<String, Json<*>>? = when (input.current()) {
            '\"' -> parseField(input)
            '}' -> null
            else -> throw ParseException("Unexpected char: ${input.current()}")
        }
        if (firstField != null) {
            fields.add(firstField)
            while (input.swallow(',')) {
                fields.add(parseField(input))
            }
        }

        input.expect('}')
        return Json.Object(fields.toMap(LinkedHashMap()))
    }

    private fun parseField(input: Scanner): Pair<String, Json<*>> {
        val name = parseStringLiteral(input)
        input.expect(':')
        val value = parseValue(input)
        return Pair(name.value, value)
    }

    private fun parseValue(input: Scanner): Json<*> {
        val value = when (input.current()) {
            '"' -> parseStringLiteral(input)
            '{' -> parseObject(input)
            '[' -> parseArray(input)
            in '0' .. '9' -> parseInteger(input)
            't', 'f' -> parseSymbol(input)
            else -> throw ParseException("Unexpected input: ${input.current()}")
        }
        return value
    }

    private fun parseSymbol(input: Scanner): Json.Boolean {
        val buffer = StringBuilder()
        while (input.current().isLetter()) {
            buffer.append(input.scan())
        }

        return when (buffer.toString()) {
            "true" -> Json.True
            "false" -> Json.False
            else -> throw ParseException("Unknown symbol: $buffer")
        }
    }

    private fun parseInteger(input: Scanner): Json.Integer {
        val buffer = StringBuilder()
        while (input.current() in '0' .. '9') {
            buffer.append(input.scan())
        }
        return Json.Integer(buffer.toString().toInt())
    }

    private fun parseArray(input: Scanner): Json.Array {
        input.expect('[')
        val items = ArrayList<Json<*>>()
        if (input.swallow(']')) {
            return Json.Array(items)
        }
        items.add(parseValue(input))
        while (input.swallow(',')) {
            items.add(parseValue(input))
        }
        input.expect(']')
        return Json.Array(items)
    }

    private fun parseStringLiteral(input: Scanner): Json.String {
        input.expect('\"')

        val buffer = StringBuilder()
        while (true) {
            val c = input.scan()
            if (c == '"') {
                if (buffer.last() != '\\') {
                    input.back(1)
                } else {
                    buffer[buffer.length - 1] = c
                }
                break
            } else {
                buffer.append(c)
            }
        }

        input.expect('"')

        return Json.String(buffer.toString())
    }

}