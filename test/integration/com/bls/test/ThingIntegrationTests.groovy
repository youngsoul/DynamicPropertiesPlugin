package com.bls.test

import com.bls.DynamicProperty
import com.bls.DynamicPropertyMixin

import static org.junit.Assert.*
import org.junit.*

class ThingIntegrationTests {

    @Before
    void setUp() {
        // Setup logic here
        Thing.mixin DynamicPropertyMixin
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testSimpleProperty() {
        Thing thing = new Thing(name: "ThingName")

        thing.prop1 = "new value"
        thing.prop2 = 100

        assertEquals thing.prop1, "new value"
        assertEquals thing.prop2, 100
    }

    @Test
    void testSimpleSave() {
        Thing thing = new Thing(name: "ThingName")

        thing.prop1 = "new value"
        thing.prop2 = 100

        thing.save(flush:  true, failOnError: true)

        assertEquals thing.prop1, "new value"
        assertEquals thing.prop2, 100

        Thing thing1 = Thing.findById(thing.id)
        assertNotNull thing1
        assertEquals 1, Thing.count()
        assertEquals 2, DynamicProperty.count()


    }

    @Test
    void testDeletesWithDynamicProperties() {

        Thing thing = new Thing(name: "haynes")
        thing.newProp1 = "newValue1"
        thing.newProp2 = "newValue2"
        thing.newProp3 = "newValue3"
        thing.newProp4 = 42


        thing.save(flush: true, failOnError: true)


        DynamicProperty.withSession { s ->
            s.flush()
            s.clear()
        }

        assertEquals 4, DynamicProperty.count()
        DynamicProperty.all.each {
// apid            assertEquals thing.apId, it.ownerApId
            assertEquals thing.id, it.ownerId
        }
        Thing d = Thing.findById(thing.id)
        List<String> dynProps = d.getAllDynamicPropertyNames()
        assertEquals 4, dynProps.size()
        assertTrue dynProps.containsAll(['newProp1','newProp2','newProp3', 'newProp4'])

        DynamicProperty.withSession { s ->
            s.flush()
        }

        Thing.load(thing.id).delete(flush: true)
        assertEquals 0, Thing.count()
        assertEquals 0, DynamicProperty.count()


    }

    @Test
    void testOnlyUpdateDynamicProperty() {

        Thing thing = new Thing(name: "foo")
        thing.newProp1 = "newValue1"
        thing.newProp2 = "newValue2"
        thing.newProp3 = 100


        thing.save(flush: true)

        thing.newProp4 = 42
        thing.save(flush: true)


        DynamicProperty.withSession { s ->
            s.flush()
            s.clear()
        }

        // note that the newProp4 DID NOT get persisted because it is dynamic and hibernate does not
        // know that the object is dirty because it is not part of the objects state directly
        assertEquals 3, DynamicProperty.count()
        DynamicProperty.all.each {
// apid            assertEquals thing.apId, it.ownerApId
            assertEquals thing.id, it.ownerId
        }
        Thing thing2 = Thing.findById(thing.id)
        assertEquals "newValue1", thing2.newProp1
        assertEquals "newValue2", thing2.newProp2
        assertEquals 100, thing2.newProp3
        assertNull thing2.newProp4  // yes this should be null because hibernate does not know about dyn properties

    }


    @Test
    void testWithTxRollback() {

        Thing thing = new Thing(name: "foo")
        thing.newProp1 = "newValue1"
        thing.newProp2 = "newValue2"
        thing.newProp3 = 100

        Thing.withNewTransaction { txStatus ->
            thing.save(flush: true)

            // rollback transaction and make sure all the dynamic properties are rollbacked.
            txStatus.setRollbackOnly()
        }




        DynamicProperty.withSession { s ->
            s.flush()
        }

        // note that the newProp4 DID NOT get persisted because it is dynamic and hibernate does not
        // know that the object is dirty because it is not part of the objects state directly
        assertEquals 0, DynamicProperty.count()
        assertEquals 0, Thing.count()

    }

    @Test
    void testDynamicPropUpdatesBeforeSave() {
        Thing thing = new Thing(name: "foo")
        // make sure the last write wins and make sure we dont have more than one persisted
        thing.newProp = "newValue"
        thing.newProp = "newValue2"
        thing.newProp = "newValue3"
        thing.newProp = "newValue4"


        thing.save(flush: true)

        assertEquals 1, DynamicProperty.count


        def d = DynamicProperty.first()

        def u1 = Thing.first()
        assertEquals "newValue4", u1.newProp

        def allProps = DynamicProperty.all
        assertEquals 1, allProps.size()

        DynamicProperty.withSession { s ->
            s.flush()
        }

        Thing thing2 = Thing.findById(thing.id)
        assertEquals "newValue4", thing2.newProp



    }

    @Test
    void testDynamicPropUpdatesBetweenSaves() {
        Thing thing = new Thing(name: "foo")
        thing.newProp = "newValue"

        thing.save(flush: true)

        assertEquals 1, DynamicProperty.count


        def d = DynamicProperty.first()

        def u1 = Thing.first()
        assertEquals "newValue", u1.newProp

        def allProps = DynamicProperty.all
        assertEquals 1, allProps.size()

        DynamicProperty.withSession { s ->
            s.flush()
        }

        Thing thing2 = Thing.findById(thing.id)
        assertEquals "newValue", thing2.newProp



        thing2.newProp = "newValue2"
        thing2.name = "foo2"
        thing2.save(flush: true)

        DynamicProperty.withSession { s ->
            s.flush()
            s.clear()
        }


        Thing thing3 = Thing.findById(thing.id)
        assertEquals "foo2", thing3.name
        assertEquals "newValue2", thing3.newProp


    }

    @Test
    void testListDynamicProp() {
        Thing thing = new Thing(name: "foo")
        List<String> names = []
        10.times { i ->
            names << "name_$i"
        }
        thing.newProp = names

        thing.save(flush: true)
        DynamicProperty.withSession { s ->
            s.flush()
            s.clear()
        }


        assertEquals 1, DynamicProperty.count

        def u1 = Thing.first()
        List<String> n2 = u1.newProp
        assertNotNull n2
        assertTrue (n2 instanceof List)
        assertEquals 10, n2.size()

        n2.each {
            assertTrue n2*.toString().contains("name_0")
        }
        assertTrue n2*.toString().containsAll(["name_0","name_1","name_2","name_3","name_4","name_5","name_6","name_7","name_8","name_9"])

    }

    @Test
    void testListOfThingsDynamicProp() {
        Thing thing = new Thing(name: "foo")
        List<Thing> things = []
        10.times { i ->

            things << new Thing(name: "name_$i")
        }
        thing.newProp = things

        thing.save(flush: true)
        DynamicProperty.withSession { s ->
            s.flush()
            s.clear()
        }


        assertEquals 1, DynamicProperty.count

        def u1 = Thing.first()
        List<Thing> n2 = u1.newProp
        assertNotNull n2
        assertTrue (n2 instanceof List)
        assertEquals 10, n2.size()

        assertTrue n2*.name.containsAll(["name_0","name_1","name_2","name_3","name_4","name_5","name_6","name_7","name_8","name_9"])

    }

    @Test
    void testMapDynamicProp() {
        Thing thing = new Thing(name: "foo")
        Map<Integer,String> names = [:]
        10.times { i ->
            names.put(i,"name_$i")
        }
        thing.newProp = names

        thing.save(flush: true)
        DynamicProperty.withSession { s ->
            s.flush()
            s.clear()
        }


        assertEquals 1, DynamicProperty.count

        def u1 = Thing.first()
        Map<Integer,String> n2 = u1.newProp
        assertNotNull n2
        assertTrue (n2 instanceof Map)
        assertEquals 10, n2.size()

        n2.each { k,v ->
            assertTrue names.get(k) == "name_$k"
        }

    }

}
