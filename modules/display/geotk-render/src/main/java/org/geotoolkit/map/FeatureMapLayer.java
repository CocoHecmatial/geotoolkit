/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008 - 2009, Geomatys
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
package org.geotoolkit.map;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.util.Range;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * MapLayer holding a collection of features.
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public interface FeatureMapLayer extends CollectionMapLayer{

    public static final String PROP_EXTRA_DIMENSIONS = "extra_dims";

    /**
     * The feature collection of this layer.
     *
     * @return The features for this layer, can not be null.
     */
    @Override
    FeatureCollection<? extends Feature> getCollection();


    /**
     * Returns the definition query (filter) for this layer. If no definition
     * query has  been defined {@link Query#ALL} is returned.
     */
    Query getQuery();

    /**
     * Sets a filter query for this layer.
     *
     * <p>
     * Query filters should be used to reduce searched or displayed feature
     * when rendering or analyzing this layer.
     * </p>
     *
     * @param query the full filter for this layer. can not be null.
     */
    void setQuery(Query query);

    /**
     * Manage extra dimensions.
     *
     * @return live list of dimensiondef, never null.
     */
    List<DimensionDef> getExtraDimensions();

    /**
     * Get all values of given extra dimension.
     * @param def
     * @return collection never null, can be empty.
     */
    Collection<Range> getDimensionRange(DimensionDef def) throws DataStoreException;

    public static final class DimensionDef {
        private final CoordinateReferenceSystem crs;
        private final Expression lower;
        private final Expression upper;

        public DimensionDef(CoordinateReferenceSystem crs, Expression lower, Expression upper) {
            this.crs = crs;
            this.lower = lower;
            this.upper = upper;
        }

        public CoordinateReferenceSystem getCrs() {
            return crs;
        }

        public Expression getLower() {
            return lower;
        }

        public Expression getUpper() {
            return upper;
        }
    }

}
