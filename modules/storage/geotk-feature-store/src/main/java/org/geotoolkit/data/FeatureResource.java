/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2017, Geomatys
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

import java.util.stream.Stream;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.storage.Resource;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

/**
 * A feature resource provides access to feature definition and I/O operations.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface FeatureResource extends Resource {

    /**
     * Gets resource feature type.
     * The feature type contains the definition of all fields including:
     * <ul>
     * <li>type</li>
     * <li>cardinality</li>
     * <li>{@link CoordinateReferenceSystem}</li>
     * </ul>
     *
     * @return the feature type, never null.
     * @throws DataStoreException if an I/O or decoding error occurs.
     */
    FeatureType getType() throws DataStoreException;

    /**
     * Request a subset of features from this resource.
     * The query is optional, if not set this resource will be returned.
     *
     * @param  query a filter to apply on the returned features, or null if none.
     * @return resource of features matching the given query.
     * @throws DataStoreException if an I/O or decoding error occurs.
     */
    default FeatureResource subset(Query query) throws DataStoreException {
        return new SubsetFeatureResource(this, query);
    }

    /**
     * Reads features from the resource.
     *
     * @return stream of features.
     * @throws DataStoreException if an I/O or decoding error occurs.
     */
    Stream<Feature> features() throws DataStoreException;

}
