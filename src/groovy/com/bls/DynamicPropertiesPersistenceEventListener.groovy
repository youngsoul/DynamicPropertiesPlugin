package com.bls

import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEvent
import org.grails.datastore.mapping.engine.event.AbstractPersistenceEventListener
import org.grails.datastore.mapping.engine.event.EventType
import org.grails.datastore.mapping.engine.event.PostDeleteEvent
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import org.grails.datastore.mapping.engine.event.PostUpdateEvent
import org.grails.datastore.mapping.engine.event.PreDeleteEvent
import org.grails.datastore.mapping.engine.event.PreInsertEvent
import org.grails.datastore.mapping.engine.event.PreUpdateEvent
import org.springframework.context.ApplicationEvent

/**
 * User: youngsoul
 * applicationContext.getBeansOfType(Datastore)
 */
class DynamicPropertiesPersistenceEventListener extends AbstractPersistenceEventListener {

    public DynamicPropertiesPersistenceEventListener() {
        super(null)
    }

    public DynamicPropertiesPersistenceEventListener(Datastore datastore) {
        super(datastore)
    }

  /**
   * Upon PostInsert or PostDelete save the dynamic properties.  Upon PostDelete, delete any associated dynamic properties
   * 
   * @param event
   */
    @Override
    protected void onPersistenceEvent(AbstractPersistenceEvent event) {


        def thing = event.entityObject

        // see:  https://github.com/grails/grails-core/blob/master/grails-core/src/main/groovy/org/codehaus/groovy/grails/compiler/injection/MixinTransformation.java
        // line 85 for the injection of this property.. very dependent upon the grails.util.Mixin implementation
        //def isDynamicPropertyAware = thing.metaClass.hasProperty(thing, '$dynamicPropertyMixin')
        def isDynamicPropertyAware = thing.metaClass.hasProperty(thing, "isDynamicPropertyAware")


        if( isDynamicPropertyAware != null ) {
            switch(event.eventType) {
                case [EventType.PostInsert, EventType.PostUpdate]:
                    thing.updatedDynamicProperties()
                break

                case EventType.PostDelete:
                    thing.deleteDynamicProperties()
                break
            }
        }

    }

    @Override
    boolean supportsSourceType(Class<?> sourceType) {
        if ( datastore ) {
            return super.supportsSourceType(sourceType)    //To change body of overridden methods use File | Settings | File Templates.
        } else {
            return true
        }
    }

    boolean supportsEventType(Class<? extends ApplicationEvent> eventClass) {
        boolean doesSupport = false
        switch(eventClass) {
            case [PostInsertEvent, PostUpdateEvent, PostDeleteEvent]:
                doesSupport = true
                break
            default:
                doesSupport = false
                break

        }
        return doesSupport
    }
}
