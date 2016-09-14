package jeb.cfg

import kotlin.Boolean as KBoolean

sealed class Json {

    class Object(private val fields: Map<kotlin.String, Json>) : Json() {

        operator fun get(key: kotlin.String) = fields[key]

    }

    class String(val value: kotlin.String) : Json() {
        override fun toString(): kotlin.String {
            return value
        }
    }

    class Array(private val items: List<Json>) : Json() {

        operator fun get(idx: Int) = items[idx]

    }

    class Integer(val value: Int) : Json()

    open class Boolean(val value: KBoolean) : Json()

    object True : Boolean(true)

    object False : Boolean(false)

}

