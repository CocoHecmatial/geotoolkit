/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2018, Geomatys
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
package org.geotoolkit.internal.data;

import java.util.Collection;
import java.util.stream.Stream;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.geotoolkit.data.query.QueryFeatureSet;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;

/**
 * FeatureSet implementation stored in memory.
 *
 * <p>
 * Note-1 : This implementation is read-only for now but will become writable.
 * </p>
 * <p>
 * Note-2 : this class is experimental and should be moved to SIS when ready.
 * </p>
 *
 * @author Johann Sorel (Geomatys)
 */
public class ArrayFeatureSet implements FeatureSet {

    private final Metadata metadata;
    private final FeatureType type;
    private final Collection<Feature> features;

    /**
     *
     * @param type stored features type.
     * @param features collection of stored features, this collection will not be copied.
     * @param metadata can be null
     */
    public ArrayFeatureSet(FeatureType type, Collection<Feature> features, Metadata metadata) {
        this.metadata = metadata;
        this.type = type;
        this.features = features;

    }

    @Override
    public FeatureType getType() throws DataStoreException {
        return type;
    }

    @Override
    public Stream<Feature> features(boolean bln) throws DataStoreException {
        return bln ? features.parallelStream() : features.stream();
    }

    @Override
    public FeatureSet subset(Query query) throws UnsupportedQueryException, DataStoreException {
        if (query instanceof org.geotoolkit.data.query.Query) {
            return QueryFeatureSet.apply(this, (org.geotoolkit.data.query.Query)query);
        }
        return FeatureSet.super.subset(query);
    }

    /**
     * Envelope is not stored or computed.
     *
     * @return always null
     * @throws DataStoreException
     */
    @Override
    public Envelope getEnvelope() throws DataStoreException {
        return null;
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return metadata;
    }

}
