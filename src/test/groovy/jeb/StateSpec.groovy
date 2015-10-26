package jeb

import spock.lang.Specification

class StateSpec extends Specification {

    def "State should be correctly written to and read from file"() {
        given:
        def originalState = new State("/backupsDir", "/sourceDir", new Hanoi([[1,2,3], [], []], 1))
        def file = File.createTempFile("jeb", "json")

        when:
        State.saveState(file, originalState)
        def loadedState = State.loadState(file)

        then:
        originalState == loadedState
    }

}
