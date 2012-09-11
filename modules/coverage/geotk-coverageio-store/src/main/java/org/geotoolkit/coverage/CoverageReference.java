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
package org.geotoolkit.coverage;

import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.storage.DataStoreException;
import org.opengis.feature.type.Name;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public interface CoverageReference {
    
    /**
     * Name of the coverage. act as an identifier in the coverage store
     * 
     * @return Name
     */
    Name getName();
    
    /**
     * Get the coverage store this coverage comes from.
     * 
     * @return CoverageStore, can be null if coverage has a different kind of source.
     */
    CoverageStore getStore();
    
    /**
     * Get a new reader for this coverage.
     * 
     * @return GridCoverageReader
     * @throws DataStoreException  
     */
    GridCoverageReader createReader() throws DataStoreException;
    
}
