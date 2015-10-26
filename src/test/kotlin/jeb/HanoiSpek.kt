package jeb

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldBeTrue
import org.jetbrains.spek.api.shouldEqual

class HanoiSpek : Spek() {init {

        given("4-disk hanoi tower") {
            val hanoi = Hanoi(listOf(listOf(4, 3, 2, 1), emptyList<Int>(), emptyList<Int>()), 0)

            on("iteration until done") {
                tailrec fun solve(hanoi: Hanoi): Hanoi = when {
                    hanoi.done -> hanoi
                    else -> {
                        val (from, to) = hanoi.nextMove()
                        solve(hanoi.moveDisk(from, to))
                    }
                }

                val solution = solve(hanoi)
                it("should be actually done") {
                    shouldBeTrue(solution[0].isEmpty())
                    shouldBeTrue(solution[1].isEmpty())
                    shouldEqual(solution[2], listOf(4, 3, 2, 1))
                }}}}}
