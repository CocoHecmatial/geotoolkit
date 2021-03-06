/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012-2018, Geomatys
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
package org.geotoolkit.coverage.sql;

import java.awt.Image;
import java.util.List;
import java.util.Collection;
import java.nio.file.Path;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.opengis.metadata.Metadata;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.FactoryException;

import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.referencing.NamedIdentifier;

import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.geotoolkit.storage.coverage.AbstractCoverageResource;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.coverage.io.GridCoverageWriter;


/**
 * Provides access to resource read from the database.
 */
public final class DatabaseStore extends DataStore implements Aggregate {
    /**
     * Provider of {@link DatabaseStore}.
     */
    @StoreMetadataExt(resourceTypes = ResourceType.GRID, canWrite = true)
    public static final class Provider extends DataStoreProvider {
        /**
         * Factory identification.
         */
        private static final String NAME = "coverage-sql";

         /**
          * Parameter for getting connections to the database.
          */
        private static final ParameterDescriptor<DataSource> DATABASE;

        /**
         * Parameter for the root directory.
         */
        private static final ParameterDescriptor<Path> ROOT_DIRECTORY;

        /**
         * All parameters.
         */
        private static final ParameterDescriptorGroup PARAMETERS;
        static {
            final ParameterBuilder builder = new ParameterBuilder();
            DATABASE       = builder.addName("database").setRequired(true).create(DataSource.class, null);
            ROOT_DIRECTORY = builder.addName("rootDirectory").setRemarks("local data directory root").setRequired(true).create(Path.class, null);
            PARAMETERS     = builder.addName(NAME).createGroup(DATABASE, ROOT_DIRECTORY);
        }

        @Override
        public String getShortName() {
            return NAME;
        }

        @Override
        public ParameterDescriptorGroup getOpenParameters() {
            return PARAMETERS;
        }

        @Override
        public DatabaseStore open(final ParameterValueGroup params) throws DataStoreException {
            if (canProcess(params)) {
                return new DatabaseStore(this, Parameters.castOrWrap(params));
            }
            throw new DataStoreException("Parameter values not supported by this factory.");
        }

        @Override
        public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
            return new ProbeResult(false, null, null);
        }

        @Override
        public DataStore open(StorageConnector connector) throws DataStoreException {
            throw new DataStoreException("Not supported.");
        }

        private boolean canProcess(final ParameterValueGroup params) {
            if (params != null) {
                final ParameterDescriptorGroup desc = getOpenParameters();
                if (desc.getName().getCode().equalsIgnoreCase(params.getDescriptor().getName().getCode())) {
                    final ConformanceResult result = org.geotoolkit.parameter.Parameters.isValid(params, desc);
                    if (result != null) {
                        return Boolean.TRUE.equals(result.pass());
                    }
                }
            }
            return false;
        }
    }

    private final Parameters parameters;

    private final Database database;

    private List<Resource> components;

    public DatabaseStore(final Provider provider, final Parameters parameters) throws DataStoreException {
        super(provider, new StorageConnector(parameters.getMandatoryValue(Provider.DATABASE)));
        try {
            database = new Database(parameters.getMandatoryValue(Provider.DATABASE),
                                    parameters.getMandatoryValue(Provider.ROOT_DIRECTORY));
        } catch (FactoryException e) {
            throw new CatalogException(e);
        }
        this.parameters = Parameters.unmodifiable(parameters);
    }

    @Override
    public ParameterValueGroup getOpenParameters() {
        return parameters;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public synchronized Collection<Resource> components() throws DataStoreException {
        if (components == null) {
            final List<String> names;
            try (Transaction transaction = database.transaction();
                 ProductTable table = new ProductTable(transaction))
            {
                names = table.list();
            } catch (SQLException e) {
                throw new CatalogException(e);
            }
            final Raster[] resources = new Raster[names.size()];
            for (int i=0; i<resources.length; i++) {
                resources[i] = new Raster(this, names.get(i));
            }
            components = UnmodifiableArrayList.wrap(resources);
        }
        return components;
    }

    @Override
    public void close() throws DataStoreException {
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return null;
    }

    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    static final class Raster extends AbstractCoverageResource {
        private Product product;

        Raster(final DatabaseStore store, final String product) {
            super(store, new NamedIdentifier(null, product));
        }

        @Override
        public int getImageIndex() {
            return 0;
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        final Transaction transaction() throws SQLException {
            return ((DatabaseStore) store).database.transaction();
        }

        final synchronized Product product(final Transaction transaction) throws SQLException, CatalogException {
            if (product == null) {
                try (ProductTable table = new ProductTable(transaction)) {
                    product = table.getEntry(identifier.getCode());
                }
            }
            return product;
        }

        @Override
        public GridCoverageReader acquireReader() throws CatalogException {
            return new Reader(this);
        }

        @Override
        public GridCoverageWriter acquireWriter() throws CatalogException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Image getLegend() throws DataStoreException {
            return null;
        }
    }
}
