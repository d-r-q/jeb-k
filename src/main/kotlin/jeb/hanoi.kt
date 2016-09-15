package jeb

import jeb.cfg.Json
import jeb.cfg.toJson
import jeb.util.Try
import java.util.*

data class Hanoi private constructor(
        val pegs: List<List<Int>>,
        val step: Int) {

    private val log = jeb.log

    private val disks = pegs.fold(hashSetOf<Int>()) { acc, e -> acc.addAll(e); acc }.sortedDescending()

    private val oddMoves = listOf(0 to 2, 0 to 1, 1 to 2)
    private val evenMoves = listOf(0 to 1, 0 to 2, 1 to 2)
    private val moves = if (disks.size % 2 == 0) evenMoves else oddMoves

    val done = pegs[0].size == 0 && pegs[1].size == 0

    val largestDisc = disks.first()

    constructor(disksCount: Int) : this(listOf(createPeg(disksCount), emptyList(), emptyList()), 0)

    companion object {

        fun from(hanoiJson: Json.Object): Try<Hanoi> {
            val pegs = hanoiJson["pegs"]
            if (pegs !is Json.Array) return Try.Failure(RuntimeException("Invalid hanoi json representation: ${hanoiJson.toString()}"))

            val firstPeg = pegs[0]
            if (firstPeg !is Json.Array) return Try.Failure(RuntimeException("Invalid hanoi json representation: ${hanoiJson.toString()}"))
            if (firstPeg.any { it !is Json.Integer }) return Try.Failure(RuntimeException("Invalid hanoi json representation: ${hanoiJson.toString()}"))

            val secondPeg = pegs[1]
            if (secondPeg !is Json.Array) return Try.Failure(RuntimeException("Invalid hanoi json representation: ${hanoiJson.toString()}"))
            if (secondPeg.any { it !is Json.Integer }) return Try.Failure(RuntimeException("Invalid hanoi json representation: ${hanoiJson.toString()}"))

            val thirdPeg = pegs[2]
            if (thirdPeg !is Json.Array) return Try.Failure(RuntimeException("Invalid hanoi json representation: ${hanoiJson.toString()}"))
            if (thirdPeg.any { it !is Json.Integer }) return Try.Failure(RuntimeException("Invalid hanoi json representation: ${hanoiJson.toString()}"))

            val stepJson = hanoiJson["step"]
            if (stepJson !is Json.Integer) return Try.Failure(RuntimeException("Invalid hanoi json representation: ${hanoiJson.toString()}"))

            return Try.Success(Hanoi(
                    listOf(firstPeg.map { (it as Json.Integer).value }, secondPeg.map { (it as Json.Integer).value }, thirdPeg.map { (it as Json.Integer).value }),
                    stepJson.value))
        }

    }

    fun reset() = Hanoi(disks.size)

    fun moveDisk(): Pair<Hanoi, Int> {
        val (from, to) = nextMove()
        log.debug("Moving disk from $from to $to with pegs: $pegs")

        val newPegs = ArrayList(pegs)
        with(pegs[from]) {
            newPegs[from] = subList(0, size - 1)
            newPegs[to] = ArrayList(pegs[to]) + last()
        }

        log.debug("New pegs: $newPegs")
        return Pair(Hanoi(newPegs, step + 1), newPegs[to].last())
    }

    private fun nextMove(): Pair<Int, Int> {
        val (peg1, peg2) = moves[step % 3]
        log.debug("Pegs for move on $step step: $peg1 and $peg2")
        return if (pegs[peg2] > pegs[peg1]) {
            Pair(peg1, peg2)
        } else {
            Pair(peg2, peg1)
        }
    }

    operator fun get(pegIdx: Int) = pegs[pegIdx]

    fun toJson(): Json.Object = Json.Object(linkedMapOf(
            "pegs" to pegs.toJson(),
            "step" to step.toJson(),
            "done" to done.toJson(),
            "largestDisc" to largestDisc.toJson()))
}

private operator fun List<Int>.compareTo(another: List<Int>) = when {
    this.isEmpty() && another.isEmpty() -> 0
    this.isEmpty() -> 1
    another.isEmpty() -> -1
    else -> this.last() - another.last()
}

fun createPeg(disksCount: Int): List<Int> = (disksCount downTo 1).toList()
