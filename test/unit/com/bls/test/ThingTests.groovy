package com.bls.test

import com.thoughtworks.xstream.XStream
import grails.test.mixin.*
import org.codehaus.jackson.map.ObjectMapper
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(Thing)
class ThingTests {

    void testDynamicPropsNoSave() {
        Thing thing = new Thing(name: "ThingName")

        thing.prop1 = "new value"
        thing.prop2 = 100

        assertEquals thing.prop1, "new value"
        assertEquals thing.prop2, 100

    }


    @Ignore
    void testJackson() {
        ObjectMapper objectMapper = new ObjectMapper()
        List<String> x = []
        10.times { i ->
            x << "name_$i".toString()
        }

        String y = objectMapper.writeValueAsString(x)

        println y

        List<String> z = objectMapper.readValue(y,ArrayList.getClass())

        println z

    }

    void testXStream() {
        XStream xs = new XStream()

        List<String> x = []
        10.times { i ->
            x << "name_$i"
        }

        String y = xs.toXML(x)

        println y

        List<String> z = xs.fromXML(y)

        println z

    }
}
