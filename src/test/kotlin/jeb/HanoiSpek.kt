package jeb

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldBeTrue
import org.jetbrains.spek.api.shouldEqual

class HanoiSpek : Spek() {init {

        given("4-disk hanoi tower") {
            val hanoi = Hanoi(4)

            on("iteration until done") {
                tailrec fun solve(hanoi: Hanoi): Hanoi = when {
                    hanoi.done -> hanoi
                    else -> {
                        solve(hanoi.moveDisk().first)
                    }
                }

                val solution = solve(hanoi)
                it("should be actually done") {
                    shouldBeTrue(solution[0].isEmpty())
                    shouldBeTrue(solution[1].isEmpty())
                    shouldEqual(solution[2], listOf(4, 3, 2, 1))
                }}}}}
