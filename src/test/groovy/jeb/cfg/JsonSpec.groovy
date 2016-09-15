package jeb.cfg

import spock.lang.Specification


class JsonSpec extends Specification {

    def "json object should be rendered and then parsed correctly"() {
        given:
        Json jsonString = new Json.String("str")
        Json jsonInteger = new Json.Integer(10)
        Json jsonTrue = Json.True.INSTANCE
        Json jsonFalse = Json.False.INSTANCE
        LinkedHashMap<String, Json> fields = ["str"    : jsonString,
                                              "integer": jsonInteger,
                                              "true"   : jsonTrue,
                                              "false"  : jsonFalse,
                                              "array"  : new Json.Array([jsonString, jsonInteger, jsonTrue, jsonFalse, new Json.Array([])]),
                                              "object" : new Json.Object([:])]
        def jsonObject = new Json.Object(fields)

        when:
        def str = jsonObject.toString()
        def parsed = Parser.INSTANCE.parse(str)

        then:
        str == '{"str":"str","integer":10,"true":true,"false":false,"array":["str",10,true,false,[]],"object":{}}'
        parsed.result == jsonObject
    }

}
