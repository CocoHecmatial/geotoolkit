/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2015, Geomatys
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
import org.geotoolkit.storage.DataNode;
import org.geotoolkit.storage.StorageListener;
import org.geotoolkit.version.Version;
import org.geotoolkit.version.VersionControl;
import org.geotoolkit.version.VersioningException;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

/**
 * Wrap a CoverageStore.
 * All method calls are redirected to the wrapped coverage store.
 *
 * @author Johann Sorel
 */
public class WrapCoverageStore extends CoverageStore{

    protected final CoverageStore store;

    /**
     *
     * @param store wrapped store
     */
    public WrapCoverageStore(CoverageStore store) {
        this.store = store;
    }

    @Override
    public ParameterValueGroup getConfiguration() {
        return store.getConfiguration();
    }

    @Override
    public CoverageStoreFactory getFactory() {
        return store.getFactory();
    }

    @Override
    public DataNode getRootNode() throws DataStoreException {
        return store.getRootNode();
    }

    @Override
    public Set<GenericName> getNames() throws DataStoreException {
        return store.getNames();
    }

    @Override
    public boolean handleVersioning() {
        return store.handleVersioning();
    }

    @Override
    public VersionControl getVersioning(GenericName typeName) throws VersioningException {
        return store.getVersioning(typeName);
    }

    @Override
    public CoverageReference getCoverageReference(GenericName name) throws DataStoreException {
        return store.getCoverageReference(name);
    }

    @Override
    public CoverageReference getCoverageReference(GenericName name, Version version) throws DataStoreException {
        return store.getCoverageReference(name, version);
    }

    @Override
    public CoverageReference create(GenericName name) throws DataStoreException {
        return store.create(name);
    }

    @Override
    public CoverageType getType() {
        return store.getType();
    }

    @Override
    public void delete(GenericName name) throws DataStoreException {
        store.delete(name);
    }

    @Override
    public void addStorageListener(StorageListener listener) {
        store.addStorageListener(listener);
    }

    @Override
    public void removeStorageListener(StorageListener listener) {
        store.removeStorageListener(listener);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return store.getMetadata();
    }

    @Override
    public void close() throws DataStoreException {
        store.close();
    }

}
