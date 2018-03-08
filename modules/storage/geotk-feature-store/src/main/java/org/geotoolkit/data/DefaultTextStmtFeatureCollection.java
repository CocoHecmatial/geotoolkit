/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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

package org.geotoolkit.data;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.query.QueryUtilities;
import org.geotoolkit.data.query.TextStatement;
import org.geotoolkit.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.MismatchedFeatureException;
import org.opengis.filter.Filter;
import org.opengis.geometry.Envelope;

/**
 * Feature collection that takes it's source from a text language query.
 * Example : sql query.
 *
 * @author Johann Sorel (Geomatys)
 * @module
 */
public class DefaultTextStmtFeatureCollection extends AbstractFeatureCollection{

    private final Query query;
    private FeatureType ft = null;

    public DefaultTextStmtFeatureCollection(final NamedIdentifier id, final Query query){
        super(id,query.getSource());

        if(!(query.getSource() instanceof TextStatement)){
            throw new IllegalArgumentException("Query must have a text statement source.");
        }

        if(!QueryUtilities.isAbsolute(query.getSource())){
            throw new IllegalArgumentException("Statement must be absolute.");
        }

        this.query = query;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureCollection subset(final Query query) throws DataStoreException {
        //we can't make a subcollection, use generic wrapping
        final FeatureCollection col = getSession().getFeatureCollection(this.query);
        return FeatureStreams.subset(col, query);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized FeatureType getType() throws FeatureStoreRuntimeException{
        if(ft == null){
            try {
                ft = getSession().getFeatureStore().getFeatureType(query);
            } catch (DataStoreException ex) {
                throw new FeatureStoreRuntimeException(ex);
            } catch (MismatchedFeatureException ex) {
                throw new FeatureStoreRuntimeException(ex);
            }
        }
        return ft;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureIterator iterator(final Hints hints) throws FeatureStoreRuntimeException{

        final Query iteQuery;
        if(hints != null){
            final QueryBuilder qb = new QueryBuilder(this.query);
            final Hints hts = new Hints(hints);
            if(this.query.getHints()!=null){hts.add(this.query.getHints());}
            qb.setHints(hts);
            iteQuery = qb.buildQuery();
        }else{
            iteQuery = this.query;
        }

        try {
            return getSession().getFeatureIterator(iteQuery);
        } catch (DataStoreException ex) {
            throw new FeatureStoreRuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int size() throws FeatureStoreRuntimeException {
        try {
            return (int) getSession().getCount(query);
        } catch (DataStoreException ex) {
            throw new FeatureStoreRuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Envelope getEnvelope() throws FeatureStoreRuntimeException{
        try {
            return getSession().getEnvelope(query);
        } catch (DataStoreException ex) {
            throw new FeatureStoreRuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean add(final Feature e) {
        return addAll(Collections.singletonList(e));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean addAll(final Collection<? extends Feature> clctn) {
        throw new FeatureStoreRuntimeException("Statement collection are not editable.");
    }

    @Override
    public boolean isWritable(){
        return false;
    }

    @Override
    public boolean remove(final Object o) throws FeatureStoreRuntimeException{
        throw new FeatureStoreRuntimeException("Statement collection are not editable.");
    }

    @Override
    public boolean removeAll(final Collection<?> clctn) {
        throw new FeatureStoreRuntimeException("Statement collection are not editable.");
    }

    @Override
    public void clear() {
        throw new FeatureStoreRuntimeException("Statement collection are not editable.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void update(final Filter filter, final Map<String, ?> values) throws DataStoreException {
        throw new FeatureStoreRuntimeException("Statement collection are not editable.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void remove(final Filter filter) throws DataStoreException {
        throw new FeatureStoreRuntimeException("Statement collection are not editable.");
    }

}
