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
package org.geotoolkit.processing.vector.spatialjoin;

import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.memory.WrapFeatureCollection;

import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;

/**
 * FeatureCollection for SpatialJoin process
 * @author Quentin Boileau
 * @module pending
 */
public class SpatialJoinFeatureCollection extends WrapFeatureCollection {

    private final FeatureType newFeatureType;
    private final FeatureCollection sourceFC;
    private final boolean method;

    /**
     * Create the new
     * @param sourceFC the source FeatureCollection
     * @param targetFC the target FeatureCollection
     * @param method boolean to set the used method
     */
    public SpatialJoinFeatureCollection(final FeatureCollection sourceFC,
            final FeatureCollection targetFC, final boolean method) {

        super(targetFC);
        this.sourceFC = sourceFC;
        this.method = method;
        this.newFeatureType = SpatialJoinProcess.concatType(targetFC.getFeatureType(), sourceFC.getFeatureType());
    }

    /**
     * Return the new FeatureType
     * @return FeatureType
     */
    @Override
    public FeatureType getFeatureType() {
        return newFeatureType;
    }

    /**
     *  {@inheritDoc }
     */
    @Override
    protected Feature modify(final Feature original) {
        return SpatialJoinProcess.join(original, newFeatureType, sourceFC, method);
    }
}
