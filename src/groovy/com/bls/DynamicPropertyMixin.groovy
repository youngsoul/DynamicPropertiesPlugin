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

  /**
   * Method is execute for a read of a missing property.
   * This method will first check the local cache of property values before executing a query.
   *
   * Usage:  def x = myobject.unknownprop
   *
   * @param name property name to get the value for
   * @return value of the property name or null if it does not exist
   */
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
     * Method is executed for a write of a missing property.
     * myobject.unknownproperty = "some value"
     *
     * @param name name of the property
     * @param value of the property
     * @return value
     */
    def propertyMissing(String name, Object value) {
        _unSavedProperties[name] = value
        _cachedProperties[name] = value
    }

  /**
   * Delete all of the dynamic properties associated with the host object.
   *
   * This method is used by the DynamicPropertiesPersistenceEventHandler to delete any DynamicProperties
   * associated with the host object.
   */
    void deleteDynamicProperties() {
        DynamicProperty.withNewSession {
            DynamicProperty.executeUpdate("delete from DynamicProperty a where a.ownerId = :ownerId", [ownerId: id])
        }

    }

  /**
   * Save or update any dynamic properties
   *
   * Method is used by the DynamicPropertiesPeristenceEventHandler to save any unsaved DynamicProperties AFTER
   * the host object has been saved.
   *
   *
   */
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

  /**
   * Return a list of all of the Dynamic properties associated with the host object.
   *
   * @return List of property names
   */
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
