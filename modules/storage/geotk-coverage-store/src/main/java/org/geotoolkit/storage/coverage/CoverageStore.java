/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Geomatys
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
package org.geotoolkit.storage.coverage;

import java.util.Set;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.geotoolkit.storage.DataStoreFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module
 */
public interface CoverageStore extends AutoCloseable, Resource {

    /**
     * Get the parameters used to initialize this source from it's factory.
     *
     * @return source configuration parameters
     */
    ParameterValueGroup getOpenParameters();

    /**
     * Get the factory which created this source.
     *
     * @return this source original factory
     */
    DataStoreFactory getProvider();

    /**
     * Get a collection of all available coverage names.
     *
     * @return Set<GenericName> , never null, but can be empty.
     * @throws DataStoreException
     */
    Set<GenericName> getNames() throws DataStoreException;

    Resource findResource(final String name) throws DataStoreException;

    @Override
    void close() throws DataStoreException;

}
