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

package org.geotoolkit.data;

import static org.apache.sis.util.ArgumentChecks.*;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public abstract class AbstractFeatureWriterAppend<T extends FeatureType, F extends Feature> implements FeatureWriter<T,F>{

    protected final T type;

    public AbstractFeatureWriterAppend(final T type){
        ensureNonNull("type", type);
        this.type = type;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public T getFeatureType() {
        return type;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void remove() throws FeatureStoreRuntimeException {
        throw new FeatureStoreRuntimeException("Can not remove from a feature writer append.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void close() {
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean hasNext() {
        return false;
    }

}
