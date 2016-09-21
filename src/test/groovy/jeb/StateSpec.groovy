package jeb

import spock.lang.Specification

class StateSpec extends Specification {

    def "State should be correctly written to and read from file"() {
        given:
        def originalState = new State("/backupsDir", [new Source("/sourceDir")], new Hanoi([[1,2,3], [], []], 1))
        def file = File.createTempFile("jeb", "json")

        when:
        State.saveState(file, originalState)
        def loadedState = State.loadState(file)

        then:
        originalState == loadedState.result
    }

    def "State should be correctly read from config v1 file"() {
        given:
        def file = File.createTempFile("jeb", "json")
        file.write("""{"backupsDir": "/data/azhidkov/tmp/","source": "/home/azhidkov/","hanoi": {"pegs": [[10, 9, 8, 7, 6, 5, 4, 3, 2, 1], [], []],"step": 0,"done": false,"largestDisc": 10}}""")

        when:
        def loadedState = State.loadState(file)

        then:
        new State("/data/azhidkov/tmp/", [new Source("/home/azhidkov/")], new Hanoi([[10,9,8,7,6,5,4,3,2,1], [], []], 0)) == loadedState.result

    }

}
