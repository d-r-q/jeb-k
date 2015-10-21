package jeb

import spock.lang.Specification

class HanoiSpec extends Specification {

    def "Hanoi should solve problem with odd count of disks"() {
        given:
        def hanoi = new Hanoi([[5, 4, 3, 2, 1], [], []], 0)

        when:
        while (!hanoi.done) {
            def move = hanoi.nextMove()
            hanoi = hanoi.moveDisk(move.first, move.second)
        }

        then:
        hanoi == new Hanoi([[], [], [5, 4, 3, 2, 1]], 31)
    }

}
