package jeb.cfg

import jeb.util.Try
import spock.lang.Specification

class ParserSpec extends Specification {

    def "empty object should be successfully parsed"() {
        given:
        def input = "{}"

        when:
        def result = Parser.INSTANCE.parse(input)

        then:
        result instanceof Try.Success
        result.result instanceof Json.Object
    }

    def "object with string literal should be parsed"() {
        given:
        def input = """{"key":"value"}"""

        when:
        def result = Parser.INSTANCE.parse(input)

        then:
        result instanceof Try.Success
        result.result instanceof Json.Object
        result.result.get("key").value == "value"
    }

    def "object with two fields should be parsed"() {
        given:
        def input = """{"key1":"value1","key2":"value2"}"""

        when:
        def result = Parser.INSTANCE.parse(input)

        then:
        result instanceof Try.Success
        result.result instanceof Json.Object
        result.result.get("key1").value == "value1"
        result.result.get("key2").value == "value2"
    }

    def "object with object field should be parsed"() {
        given:
        def input = """{"key":{"subkey":"value"}}"""

        when:
        def result = Parser.INSTANCE.parse(input)

        then:
        result instanceof Try.Success
        result.result instanceof Json.Object
        result.result["key"] instanceof Json.Object
        result.result["key"]["subkey"].value == "value"
    }

    def "object with array field should be parsed"() {
        given:
        def input = """{"key":["value1","value2"]}"""

        when:
        def result = Parser.INSTANCE.parse(input)

        then:
        result instanceof Try.Success
        result.result instanceof Json.Object
        result.result["key"] instanceof Json.Array
        result.result["key"].get(0).value == "value1"
        result.result["key"].get(1).value == "value2"
    }

    def "object with integer field should be parsed"() {
        given:
        def input = """{"key":13}"""

        when:
        def result = Parser.INSTANCE.parse(input)

        then:
        result instanceof Try.Success
        result.result instanceof Json.Object
        result.result["key"] instanceof Json.Integer
        result.result["key"].value == 13
    }

    def "object with boolean fields should be parsed"() {
        given:
        def input = """{"true": true, "false": false}"""

        when:
        def result = Parser.INSTANCE.parse(input)

        then:
        result instanceof Try.Success
        result.result instanceof Json.Object
        result.result["true"].value == true
        result.result["false"].value == false

    }

    def "formatted config v1 should be parsed"() {
        given:
        def input = """{
  "backupsDir": "/data/azhidkov/tmp/",
  "source": "/home/azhidkov/",
  "hanoi": {
    "pegs": [[10, 9, 8, 7, 6, 5, 4, 3, 2, 1], [], []],
    "step": 0,
    "done": false,
    "largestDisc": 10
  }
}"""

        when:
        def config = Parser.INSTANCE.parse(input).result
        def backupsDir = config["backupsDir"]
        def hanoi = config["hanoi"]
        def pegs = hanoi["pegs"]
        def done = hanoi["done"]
        def largestDisc = hanoi["largestDisc"]

        then:
        backupsDir.value == "/data/azhidkov/tmp/"
        pegs.get(0).get(0).value == 10
        pegs.get(0).get(9).value == 1
        done.value == false
        largestDisc.value == 10
    }
}
