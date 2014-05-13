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

package org.geotoolkit.data.om;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.data.AbstractFeatureStore;
import org.geotoolkit.data.query.DefaultQueryCapabilities;
import org.geotoolkit.data.query.QueryCapabilities;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.feature.LenientFeatureFactory;
import org.opengis.feature.FeatureFactory;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractOMFeatureStore extends AbstractFeatureStore {
 
    private static final QueryCapabilities capabilities = new DefaultQueryCapabilities(false);

    private final Map<Name, FeatureType> types = OMFeatureTypes.getFeatureTypes();
 
    protected static final Logger LOGGER = Logging.getLogger(AbstractOMFeatureStore.class);
    
    protected static final FeatureFactory FF = FactoryFinder.getFeatureFactory(new Hints(Hints.FEATURE_FACTORY,LenientFeatureFactory.class));
    
    public AbstractOMFeatureStore(final ParameterValueGroup params) {
        super(params);
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public Set<Name> getNames() throws DataStoreException {
        return types.keySet();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureType getFeatureType(final Name typeName) throws DataStoreException {
        typeCheck(typeName);
        return types.get(typeName);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public QueryCapabilities getQueryCapabilities() {
        return capabilities;
    }
}