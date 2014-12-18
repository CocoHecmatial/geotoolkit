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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.AbstractFeatureStore;
import org.geotoolkit.data.FeatureReader;
import org.geotoolkit.data.FeatureStoreFactory;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.data.query.DefaultQueryCapabilities;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryCapabilities;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.Name;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.geotoolkit.storage.StorageEvent;
import org.geotoolkit.storage.StorageListener;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;

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
    
    private final Map<Name,BeanFeatureSupplier> types = new HashMap<>();
    
    public BeanStore(BeanFeatureSupplier ... types) throws DataStoreException {
        super(null);
        for(BeanFeatureSupplier bt : types){
            this.types.put(bt.mapping.featureType.getName(), bt);
            //catch events and propage them
            bt.addStorageListener(this);
        }
    }
    
    public Collection<BeanFeatureSupplier> getBeanSuppliers(){
        return Collections.unmodifiableCollection(types.values());
    }
    
    @Override
    public FeatureStoreFactory getFactory() {
        return null;
    }

    @Override
    public Set<Name> getNames() throws DataStoreException {
        return Collections.unmodifiableSet(types.keySet());
    }

    @Override
    public FeatureType getFeatureType(Name typeName) throws DataStoreException {
        typeCheck(typeName);
        return types.get(typeName).mapping.featureType;
    }
        
    @Override
    public FeatureReader getFeatureReader(Query query) throws DataStoreException {
        typeCheck(query.getTypeName());
        
        final BeanFeatureSupplier bt = types.get(query.getTypeName());
        final Iterable candidates = bt.supplier.get();
        final BeanFeature.Mapping mapping = bt.mapping;
        
        final FeatureReader reader = new BeanFeatureReader(mapping, candidates);
        return handleRemaining(reader, query);
    }
    
    @Override
    public QueryCapabilities getQueryCapabilities() {
        return CAPABILITIES;
    }
    
    @Override
    public List<FeatureId> addFeatures(Name groupName, Collection<? extends Feature> newFeatures, Hints hints) throws DataStoreException {
        throw new DataStoreException("Not supported.");
    }

    @Override
    public void updateFeatures(Name groupName, Filter filter, Map<? extends PropertyDescriptor, ? extends Object> values) throws DataStoreException {
        throw new DataStoreException("Not supported.");
    }

    @Override
    public void removeFeatures(Name groupName, Filter filter) throws DataStoreException {
        throw new DataStoreException("Not supported.");
    }

    @Override
    public FeatureWriter getFeatureWriter(Name typeName, Filter filter, Hints hints) throws DataStoreException {
        throw new DataStoreException("Not supported.");
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
    
    ////////////////////////////////////////////////////////////////////////////
    // NOT SUPPORTED ///////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
        
    @Override
    public void createFeatureType(Name typeName, FeatureType featureType) throws DataStoreException {
        throw new DataStoreException("Not supported.");
    }

    @Override
    public void updateFeatureType(Name typeName, FeatureType featureType) throws DataStoreException {
        throw new DataStoreException("Not supported.");
    }

    @Override
    public void deleteFeatureType(Name typeName) throws DataStoreException {
        throw new DataStoreException("Not supported.");
    }
    
}
