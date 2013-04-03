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
        XStream xs = new XStream()

        // see if we have already read/saved this property value
        if( !_cachedProperties.containsKey(name) ) {
            // if not create new one and save it to db
            DynamicProperty dynamicProperty = DynamicProperty.findByOwnerIdAndPropName(id, name)

            // this could be null if the user is asking for a non-existent property
            if (dynamicProperty) {
                def propValue = xs.fromXML(dynamicProperty.propValue)
                _cachedProperties[name] = propValue
            }
        }
        return _cachedProperties[name]
    }

    /**
     *
     * @param name
     * @param value
     * @return
     */
    def propertyMissing(String name, Object value) {
        _unSavedProperties[name] = value
        _cachedProperties[name] = value
    }

    void deleteDynamicProperties() {
        DynamicProperty.withNewSession {
            DynamicProperty.executeUpdate("delete from DynamicProperty a where a.ownerId = :ownerId", [ownerId: id])
        }

    }
    void updatedDynamicProperties() {
        DynamicProperty.withNewSession {
            XStream xs = new XStream()
            _unSavedProperties.each { k, v ->


                DynamicProperty dynamicProperty = DynamicProperty.findByOwnerIdAndPropName(id, k)
                if (!dynamicProperty) {
                    dynamicProperty = new DynamicProperty(propName: k, propType: v.getClass().name, ownerId: id)
                }
                dynamicProperty.propValue = xs.toXML(v)

                dynamicProperty.save(flush:true, failOnError: true)
            }
        }
        _unSavedProperties = [:]
    }

    List<String> getAllDynamicPropertyNames() {
        List propNames = []
        DynamicProperty.withNewSession {
            propNames = DynamicProperty.withCriteria {
                projections {
                    property('propName')
                }
                eq('ownerId', id)
            }
        }

        return propNames

    }

}
