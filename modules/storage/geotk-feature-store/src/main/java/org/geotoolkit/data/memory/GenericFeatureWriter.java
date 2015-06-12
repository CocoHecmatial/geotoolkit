/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Geomatys
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

package org.geotoolkit.data.memory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureReader;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.feature.AbstractFeature;
import org.geotoolkit.feature.FeatureUtilities;
import org.apache.sis.util.Classes;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.opengis.util.GenericName;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;

/**
 * Basic support for a  FeatureWriter that redicts it's calls to
 * the more casual methods : addFeatures, removeFeatures and updateFeatures
 * of the feature store.
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class GenericFeatureWriter implements FeatureWriter {

    private static final FilterFactory FF = FactoryFinder.getFilterFactory(null);

    protected final FeatureStore store;
    protected final GenericName typeName;
    protected final FeatureReader reader;
    protected final FeatureType type;
    protected Feature currentFeature = null;
    protected Feature modified = null;
    private boolean remove = false;

    private GenericFeatureWriter(final FeatureStore store, final GenericName typeName, final Filter filter) throws DataStoreException {
        this.store = store;
        this.typeName = typeName;
        reader = store.getFeatureReader(QueryBuilder.filtered(typeName, filter));
        type = store.getFeatureType(typeName);
    }


    @Override
    public FeatureType getFeatureType() throws FeatureStoreRuntimeException{
        return type;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Feature next() throws FeatureStoreRuntimeException {
        remove = false;
        if(hasNext()){
            currentFeature = reader.next();
            modified = FeatureUtilities.copy(currentFeature);
        }else{
            currentFeature = null;
            modified = FeatureUtilities.defaultFeature(type, "");
        }

        return modified;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void close() throws FeatureStoreRuntimeException {
        reader.close();
        write();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean hasNext() throws FeatureStoreRuntimeException {
        return reader.hasNext();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void remove() {
        remove = true;
        write();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void write() throws FeatureStoreRuntimeException {
        if(currentFeature != null){
            final Filter filter = FF.id(Collections.singleton(currentFeature.getIdentifier()));
            if(remove){
                //it's a remove operation
                try {
                    store.removeFeatures(typeName, filter);
                } catch (DataStoreException ex) {
                    throw new FeatureStoreRuntimeException(ex);
                }
            }else{
                //it's a modify operation
                final Map<PropertyDescriptor,Object> values = new HashMap<PropertyDescriptor, Object>();

                for(PropertyDescriptor desc : type.getDescriptors()){
                    final Object original = currentFeature.getProperty(desc.getName()).getValue();
                    final Object mod = modified.getProperty(desc.getName()).getValue();
                    //check if the values was modified
                    if(!safeEqual(original, mod)){
                        //value has changed
                        values.put(desc, mod);
                    }
                }

                if(!values.isEmpty()){
                    try {
                        store.updateFeatures(typeName, filter, values);
                    } catch (DataStoreException ex) {
                        throw new FeatureStoreRuntimeException(ex);
                    }
                }
            }

        }else{
            if(modified != null){
                //it's an add operation
                try {
                    final List<FeatureId> res = store.addFeatures(typeName, Collections.singleton(modified));
                    if(modified instanceof AbstractFeature){
                        ((AbstractFeature)modified).setIdentifier(res.get(0));
                    }
                } catch (DataStoreException ex) {
                    throw new FeatureStoreRuntimeException(ex);
                }
                modified = null;
            }
        }

        remove = false;
    }

    private boolean safeEqual(final Object o1, final Object o2){
        if(o1 == null && o2 == null){
            return true;
        }else if(o1 != null){
            return o1.equals(o2);
        }else{
            return o2.equals(o1);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(Classes.getShortClassName(this));
        sb.append('\n');
        String subIterator = "\u2514\u2500\u2500" + reader.toString(); //move text to the right
        subIterator = subIterator.replaceAll("\n", "\n\u00A0\u00A0\u00A0"); //move text to the right
        sb.append(subIterator);
        return sb.toString();
    }

    public static FeatureWriter wrap(final FeatureStore store, final GenericName typeName, final Filter filter) throws DataStoreException{
        return new GenericFeatureWriter(store, typeName, filter);
    }

    public static FeatureWriter wrapAppend(final FeatureStore store, final GenericName typeName) throws DataStoreException{
        return wrap(store,typeName,Filter.EXCLUDE);
    }

}
