package jeb

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldBeTrue

class ListMoveToSpek : Spek() {init {

    given("List with an element and empty list") {
            val e = Any()
            val eList = listOf(e)
            val emptyList = emptyList<Int>()

            on("move from list with element to empty list") {
                val (from, to) = eList.moveTo(emptyList)

                it("from should be empty and to's last element should be given") {
                    shouldBeTrue(from.isEmpty())
                    shouldBeTrue(to.last() === e)
                }}}}}
