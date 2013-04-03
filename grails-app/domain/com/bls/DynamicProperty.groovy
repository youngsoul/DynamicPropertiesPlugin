package com.bls

class DynamicProperty {

    String propName
    String propValue
    String propType = "String"

    Long ownerId
    static constraints = {
    }
    static mapping = {
        version false
        propValue type: 'text'
    }

}
