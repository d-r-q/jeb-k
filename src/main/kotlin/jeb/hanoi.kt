package jeb

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class Hanoi @JsonCreator constructor(
        val pegs: List<List<Int>>,
        val step: Int) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val diskComparator = Comparator { a: Int, b: Int -> b - a }
    private val disks = pegs.fold(sortedSetOf<Int>(diskComparator)) { acc, e -> acc.addAll(e); acc }

    private val oddMoves = listOf(0 to 2, 0 to 1, 1 to 2)
    private val evenMoves = listOf(0 to 1, 0 to 2, 1 to 2)
    private val moves = if (disks.size % 2 == 0) evenMoves else oddMoves

    val done = pegs[0].size == 0 && pegs[1].size == 0

    val largestDisc = disks.first()

    fun reset(): Hanoi {
        val resetHanoi = Hanoi(listOf(disks.toList(), emptyList(), emptyList()), 0)
        log.debug("Resetting hanoi, new hanoi: $resetHanoi")
        return resetHanoi
    }

    fun nextMove(): Pair<Int, Int> {
        val (peg1, peg2) = moves[step % 3]
        log.debug("Pegs for move on $step step: $peg1 and $peg2")
        return if (pegs[peg2] > pegs[peg1]) {
            Pair(peg1, peg2)
        } else {
            Pair(peg2, peg1)
        }
    }

    fun moveDisk(from: Int, to: Int): Hanoi {

        log.debug("Moving disk from $from to $to with pegs: $pegs")
        val source = pegs[from]
        val (newSource, dest) = source.moveTo(pegs[to])

        val newPegs = ArrayList(pegs)
        newPegs[from] = newSource
        newPegs[to] = dest
        log.debug("New pegs: $newPegs")
        return Hanoi(newPegs, step + 1)
    }

    operator fun get(pegIdx: Int) = pegs[pegIdx]
}

operator fun List<Int>.compareTo(another: List<Int>) = when {
    this.isEmpty() && another.isEmpty() -> 0
    this.isEmpty() -> 1
    another.isEmpty() -> -1
    else -> this.last() - another.last()
}

fun <T> List<T>.moveTo(another: List<T>): Pair<List<T>, List<T>> = Pair(
        this.subList(0, this.size - 1),
        ArrayList(another) + this[this.lastIndex])
