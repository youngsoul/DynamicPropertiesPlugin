package com.bls

import com.thoughtworks.xstream.XStream
import org.codehaus.jackson.map.ObjectMapper

/**
 * User: youngsoul
 */
class DynamicPropertyMixin {

    // _unSavedProperties are those have have been set, but have not been updated or persisted to the db.
    // this operation is not done until a save or update is done
    transient Map<String,Object> _unSavedProperties = [:]

    // _cachedProperties are used when we have read a property before to save the lookup
    transient Map<String,Object> _cachedProperties = [:]

    // this will NOT BE PERSISTED
    transient boolean isDynamicPropertyAware = true


    def propertyMissing(String name ) {
        ObjectMapper objectMapper = new ObjectMapper()
        XStream xs = new XStream()
        if( !_cachedProperties.containsKey(name) ) {
// apid            DynamicProperty dynamicProperty = DynamicProperty.findByOwnerApIdAndPropName(apId, name)
            DynamicProperty dynamicProperty = DynamicProperty.findByOwnerIdAndPropName(id, name)

            // this could be null if the user is asking for a non-existent property
            if (dynamicProperty) {
                //def propValue = objectMapper.readValue(dynamicProperty.propValue, Class.forName(dynamicProperty.propType))
                def propValue = xs.fromXML(dynamicProperty.propValue)
                _cachedProperties[name] = propValue
            }
        }
        println "Property missing: $name has value: ${_cachedProperties[name].toString()}"
        return _cachedProperties[name]
    }

    def propertyMissing(String name, Object value) {
        println "Set missing property: $name with value ${value?.toString()}"

        _unSavedProperties[name] = value
        _cachedProperties[name] = value
    }

    void deleteDynamicProperties() {
        DynamicProperty.withNewSession {
// apid            DynamicProperty.executeUpdate("delete from DynamicProperty a where a.ownerApId = :ownerapid", [ownerapid: apId])
            DynamicProperty.executeUpdate("delete from DynamicProperty a where a.ownerId = :ownerId", [ownerId: id])
        }

    }
    void updatedDynamicProperties() {
        DynamicProperty.withNewSession {
//            ObjectMapper objectMapper = new ObjectMapper()
            XStream xs = new XStream()
            _unSavedProperties.each { k, v ->
// apid                DynamicProperty dynamicProperty = DynamicProperty.findByOwnerApIdAndPropName(apId, k)


                DynamicProperty dynamicProperty = DynamicProperty.findByOwnerIdAndPropName(id, k)
                if (!dynamicProperty) {
// apid                    dynamicProperty = new DynamicProperty(propName: k, propType: v.getClass().name, ownerApId: apId)
                    dynamicProperty = new DynamicProperty(propName: k, propType: v.getClass().name, ownerId: id)
                }
                dynamicProperty.propValue = xs.toXML(v)  //objectMapper.writeValueAsString(v)

                dynamicProperty.save(flush:true, failOnError: true)
            }
        }
        _unSavedProperties = [:]
    }

    List<String> getAllDynamicPropertyNames() {
        def propNames = DynamicProperty.withCriteria {
            projections {
                property('propName')
            }
// apid            eq('ownerApId', apId)
            eq('ownerId', id)
        }

        return propNames
    }

}
