DynamicPropertiesPlugin
=======================

Plugin to support dynamic properties on a Domain class

Mixin that will add the capability to add properties to a domain object that were not part of the original Domain
definition.

Usage:
@Mixin(com.bls.DynamicPropertyMixin)
class Thing {

    String name

    static constraints = {
    }
}

