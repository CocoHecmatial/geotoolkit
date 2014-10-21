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

import java.util.Iterator;
import org.geotoolkit.data.FeatureReader;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class BeanFeatureReader implements FeatureReader<FeatureType, Feature>{

    private final BeanFeature.Mapping mapping;
    private final Iterator<Object> candidates;

    public BeanFeatureReader(BeanFeature.Mapping mapping, Iterable<Object> candidates) {
        this.mapping = mapping;
        this.candidates = candidates.iterator();
    }

    @Override
    public FeatureType getFeatureType() {
        return mapping.featureType;
    }

    @Override
    public Feature next() throws FeatureStoreRuntimeException {
        return new BeanFeature(candidates.next(), mapping);
    }

    @Override
    public boolean hasNext() throws FeatureStoreRuntimeException {
        return candidates.hasNext();
    }

    @Override
    public void close() {
    }

    @Override
    public void remove() {
        throw new FeatureStoreRuntimeException("Remove not supported");
    }
    
}
