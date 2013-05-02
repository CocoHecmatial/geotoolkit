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

import java.util.Set;
import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.storage.StorageListener;
import org.geotoolkit.version.Version;
import org.geotoolkit.version.VersionControl;
import org.geotoolkit.version.VersioningException;
import org.opengis.feature.type.Name;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public interface CoverageStore {

    /**
     * Get the parameters used to initialize this source from it's factory.
     *
     * @return source configuration parameters
     */
    ParameterValueGroup getConfiguration();

    /**
     * Get the factory which created this source.
     *
     * @return this source original factory
     */
    CoverageStoreFactory getFactory();

    /**
     * Get a collection of all available coverage names.
     * @return Set<Name> , never null, but can be empty.
     * @throws DataStoreException
     */
    Set<Name> getNames() throws DataStoreException;

    /**
     * Check if this coverage store support versioning.
     * @return true if versioning is supported.
     */
    boolean handleVersioning();
        
    /**
     * Get version history for given coverage.
     * @return VersionHistory for given name.
     */
    VersionControl getVersioning(Name typeName) throws VersioningException;
    
    /**
     * Get the coverage reference for the given name.
     * @param name reference name
     * @return CoverageReference
     * @throws DataStoreException
     */
    CoverageReference getCoverageReference(Name name) throws DataStoreException;

    /**
     * Get the coverage reference for the given name and version.
     * If the version do not exist it will be created.
     * 
     * @param name reference name
     * @param version version
     * @return CoverageReference
     * @throws DataStoreException
     */
    CoverageReference getCoverageReference(Name name, Version version) throws DataStoreException;
    
    /**
     * Create a new coverage reference.
     * The returned coverage reference might have a different namespace.
     *
     * @param name
     * @return CoverageReference
     * @throws DataStoreException
     */
    CoverageReference create(Name name) throws DataStoreException;

    /**
     * Delete an existing coverage reference.
     *
     * @param name
     * @throws DataStoreException
     */
    void delete(Name name) throws DataStoreException;

    /**
     * Dispose the coveragestore caches and underlying resources.
     * The CoverageStore should not be used after this call or it may raise errors.
     */
    void dispose();

    
    /**
     * Add a storage listener which will be notified when structure changes or
     * when coverage data changes.
     * @param listener to add
     */
    void addStorageListener(StorageListener listener);

    /**
     * Remove a storage listener
     * @param listener to remove
     */
    void removeStorageListener(StorageListener listener);
    
}
