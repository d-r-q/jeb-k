package jeb.cfg

import java.util.*
import kotlin.Boolean as KBoolean
import kotlin.String as KString

sealed class Json<out T : Any>(protected open val value: T,
                               private val cmp: (T, T) -> KBoolean = { value, other -> value == other }) {

    class String(override public val value: KString) : Json<KString>(value) {

        override fun toString(): KString = "\"$value\""

    }

    class Integer(override public val value: Int) : Json<Int>(value) {

        override fun toString() = value.toString()

    }

    object True : Json.Boolean(true)

    object False : Json.Boolean(false)

    class Object(private val fields: LinkedHashMap<KString, Json<Any>>) : Json<LinkedHashMap<KString, Json<Any>>>(fields, Json.Object.compareValue) {

        operator fun get(key: KString) = fields[key]

        override fun toString(): KString =
                fields.map { "\"${it.key}\":${it.value.toString()}" }.
                        joinToString(",", "{", "}")

        companion object {
            val compareValue: (LinkedHashMap<KString, Json<Any>>, LinkedHashMap<KString, Json<Any>>) -> kotlin.Boolean = { fields, other ->
                fields.keys.zip(other.keys).all {
                    it.first == it.second && fields[it.first] == fields[it.first]
                }
            }
        }

    }

    class Array(private val items: List<Json<Any>>) : Json<List<Json<Any>>>(items, compareValue), List<Json<Any>> by items {

        override fun toString(): KString = items.joinToString(",", "[", "]")

        inline fun <reified T> toListOf(): List<T> {
            return map { it.value as T }
        }

        companion object {
            val compareValue: (List<Json<*>>, List<Json<*>>) -> kotlin.Boolean = { value, other ->
                value.zip(value).all { it.first == it.second }
            }
        }

    }

    open class Boolean(value: KBoolean) : Json<KBoolean>(value) {

        override fun toString() = value.toString()

    }

    override fun equals(other: Any?): kotlin.Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        @Suppress("UNCHECKED_CAST")
        val otherValue = (other as Json<T>).value

        return cmp(value, otherValue)
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

}

fun Any.toJson(): Json<Any> {
    return when (this) {
        is KString -> Json.String(this)
        is Int -> Json.Integer(this)
        is KBoolean -> if (this) Json.True else Json.False
        is List<*> -> Json.Array(this.map { it!!.toJson() })
        else -> throw IllegalStateException("Could not render to json instances of ${this.javaClass}")
    }
}

