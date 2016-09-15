package jeb.cfg

import java.util.*
import kotlin.Boolean as KBoolean
import kotlin.String as KString

sealed class Json<T : Any>(protected open val value: T) {

    class String(override public val value: KString) : Json<KString>(value) {

        override fun toString(): KString = "\"$value\""

    }

    class Integer(value: Int) : Json<Int>(value) {

        override fun toString() = value.toString()

    }

    object True : Json.Boolean(true)

    object False : Json.Boolean(false)

    class Object(private val fields: LinkedHashMap<KString, Json<*>>) : Json<LinkedHashMap<KString, Json<*>>>(fields) {

        operator fun get(key: KString) = fields[key]

        override fun toString(): KString =
                fields.map { "\"${it.key}\":${it.value.toString()}" }.
                        joinToString(",", "{", "}")

        override fun compareValue(other: LinkedHashMap<KString, Json<*>>): kotlin.Boolean {
            return fields.keys.zip(fields.keys).all {
                it.first == it.second && fields[it.first] == fields[it.first]
            }
        }

    }

    class Array(private val items: List<Json<*>>) : Json<List<Json<*>>>(items) {

        operator fun get(idx: Int) = items[idx]

        override fun toString(): KString = items.joinToString(",", "[", "]")

        override fun compareValue(other: List<Json<*>>): kotlin.Boolean {
            return items.zip(items).all { it.first == it.second}
        }

    }

    open class Boolean(value: KBoolean) : Json<KBoolean>(value) {

        override fun toString() = value.toString()

    }

    override fun equals(other: Any?): kotlin.Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        @Suppress("UNCHECKED_CAST")
        val otherValue = (other as Json<T>).value

        return compareValue(otherValue)
    }

    override fun hashCode(): Int{
        return value.hashCode()
    }

    open protected fun compareValue(other: T): KBoolean {
        return value == other
    }

}

