/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2012, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotoolkit.data.session;

import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.FeatureStoreContentEvent;
import org.geotoolkit.data.FeatureStoreListener;
import org.geotoolkit.data.FeatureStoreManagementEvent;
import org.geotoolkit.storage.AbstractStorage;
import static org.apache.sis.util.ArgumentChecks.*;
import org.opengis.util.GenericName;
import org.opengis.filter.Id;

/**
 *  Abstract session which handle listeners and add convenient fire event methods.
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public abstract class AbstractSession extends AbstractStorage implements Session, FeatureStoreListener{

    private final FeatureStoreListener.Weak weakListener = new Weak(this);
    protected final FeatureStore store;

    public AbstractSession(final FeatureStore store){
        ensureNonNull("feature store", store);
        this.store = store;
        weakListener.registerSource(store);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureStore getFeatureStore() {
        return store;
    }

    ////////////////////////////////////////////////////////////////////////////
    // listeners methods ///////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Forward event to listeners by changing source.
     */
    @Override
    public void structureChanged(FeatureStoreManagementEvent event){
        event = event.copy(this);
        sendStructureEvent(event);
    }

    /**
     * Forward event to listeners by changing source.
     */
    @Override
    public void contentChanged(final FeatureStoreContentEvent event){
        sendContentEvent(event.copy(this));
    }

    /**
     * Fires a features add event.
     *
     * @param name of the schema where features where added.
     * @param ids modified feature ids.
     */
    protected void fireFeaturesAdded(final GenericName name, final Id ids){
        sendContentEvent(FeatureStoreContentEvent.createAddEvent(this, name, ids));
    }

    /**
     * Fires a features update event.
     *
     * @param name of the schema where features where updated.
     * @param ids modified feature ids.
     */
    protected void fireFeaturesUpdated(final GenericName name, final Id ids){
        sendContentEvent(FeatureStoreContentEvent.createUpdateEvent(this, name, ids));
    }

    /**
     * Fires a features delete event.
     *
     * @param name of the schema where features where deleted
     * @param ids modified feature ids.
     */
    protected void fireFeaturesDeleted(final GenericName name, final Id ids){
        sendContentEvent(FeatureStoreContentEvent.createDeleteEvent(this, name, ids));
    }

    /**
     * Fires a session event. when new pending changes are added.
     */
    protected void fireSessionChanged(){
        sendContentEvent(FeatureStoreContentEvent.createSessionEvent(this));
    }

}
