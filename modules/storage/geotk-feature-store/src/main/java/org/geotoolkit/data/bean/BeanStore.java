/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
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
package org.geotoolkit.data.bean;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.geotoolkit.data.AbstractFeatureStore;
import org.geotoolkit.data.FeatureReader;
import org.geotoolkit.data.FeatureStreams;
import org.geotoolkit.internal.data.GenericNameIndex;
import org.geotoolkit.data.query.DefaultQueryCapabilities;
import org.geotoolkit.data.query.QueryCapabilities;
import org.geotoolkit.storage.DataStoreFactory;
import org.opengis.util.GenericName;
import org.geotoolkit.storage.StorageEvent;
import org.geotoolkit.storage.StorageListener;
import org.opengis.feature.FeatureType;

/**
 * A BeanStore decorate collections of bean objects as FeatureCollections.
 * Only reading operations are supported.
 *
 * @author Johann Sorel (Geomatys)
 */
public class BeanStore extends AbstractFeatureStore implements StorageListener{

    private static final QueryCapabilities CAPABILITIES = new DefaultQueryCapabilities(false);

    /**
     * A FeatureSupplier provides access to an iterable of bean objects.
     * TODO JDK8 : replace with Supplier
     */
    public static interface FeatureSupplier{
        Iterable get();
    }

    private final GenericNameIndex<BeanFeatureSupplier> types = new GenericNameIndex<>();

    public BeanStore(BeanFeatureSupplier ... types) throws IllegalNameException {
        super(null);
        for(BeanFeatureSupplier bt : types){
            this.types.add(this, bt.mapping.featureType.getName(), bt);
            //catch events and propage them
            bt.addStorageListener(this);
        }
    }

    public Collection<BeanFeatureSupplier> getBeanSuppliers(){
        return Collections.unmodifiableCollection(types.getValues());
    }

    public BeanFeatureSupplier getBeanSupplier(String typeName) throws DataStoreException{
        typeCheck(typeName);
        return types.get(this, typeName);
    }

    @Override
    public DataStoreFactory getProvider() {
        return null;
    }

    @Override
    public Set<GenericName> getNames() throws DataStoreException {
        return types.getNames();
    }

    @Override
    public FeatureType getFeatureType(String typeName) throws DataStoreException {
        typeCheck(typeName);
        return types.get(this, typeName).mapping.featureType;
    }

    @Override
    public FeatureReader getFeatureReader(Query query) throws DataStoreException {
        if (!(query instanceof org.geotoolkit.data.query.Query)) throw new UnsupportedQueryException();

        final org.geotoolkit.data.query.Query gquery = (org.geotoolkit.data.query.Query) query;
        typeCheck(gquery.getTypeName());

        final BeanFeatureSupplier bt = types.get(this, gquery.getTypeName());
        final Iterable candidates = bt.supplier.get();
        final BeanFeature.Mapping mapping = bt.mapping;

        final FeatureReader reader = new BeanFeatureReader(mapping, candidates);
        return FeatureStreams.subset(reader, gquery);
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public void refreshMetaModel() {
        fireFeaturesAdded(null, null);
    }

    ////////////////////////////////////////////////////////////////////////////
    // BEAN SUPPLIER EVENTS ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void structureChanged(StorageEvent event) {
        sendStructureEvent(event.copy(this));
    }

    @Override
    public void contentChanged(StorageEvent event) {
        sendContentEvent(event.copy(this));
    }

}
