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

import java.io.Serializable;
import java.util.Map;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.FactoryMetadata;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Factory used to construct a CoverageStore from a set of parameters.
 *
 * <h2>Implementation Notes</h2>
 * <p>
 * An instance of this interface should exist for all data stores which want to
 * take advantage of the dynamic plug-in system. In addition to implementing
 * this factory interface each CoverageStore implementation should have a services file:
 * </p>
 *
 * <p>
 * <code>META-INF/services/org.geotoolkit.storage.DataStoreFactory</code>
 * </p>
 *
 * <p>
 * The file should contain a single line which gives the full name of the
 * implementing class.
 * </p>
 *
 * <p>
 * example:<br/><code>e.g.
 * org.geotoolkit.data.mytype.MyTypeDataSourceFacotry</code>
 * </p>
 *
 * <p>
 * The factories are never called directly by client code, instead the
 * CoverageStoreFinder class is used.
 * </p>
 *
 * @author Johann Sorel (Geomatys)
 */
public interface CoverageStoreFactory {

    /**
     *
     * @return A metadata object giving general information about data support of this factory.
     */
    FactoryMetadata getMetadata();

    /**
     * Name suitable for display to end user.
     *
     * <p>
     * A display name for this data store type with several translations.
     * </p>
     *
     * @return A short name suitable for display in a user interface. Must be an International string.
     */
    CharSequence getDisplayName();

    /**
     * Describe the nature of the data source constructed by this factory.
     *
     * <p>
     * A description of this data store type with several translations.
     * </p>
     *
     * @return A human readable description that is suitable for inclusion in a
     *         list of available data sources.
     */
    CharSequence getDescription();

    /**
     * @return Description of the parameters required for the creation of a {@link org.apache.sis.storage.DataStore}.
     */
    ParameterDescriptorGroup getOpenParameters();

    /**
     * Test to see if this factory is suitable for processing the data pointed
     * to by the params map.
     *
     * <p>
     * If this data source requires a number of parameters then this method
     * should check that they are all present and that they are all valid. If
     * the data source is a file reading data source then the extensions or
     * mime types of any files specified should be checked. For example, a
     * Shapefile data source should check that the url param ends with shp,
     * such tests should be case insensitive.
     * </p>
     *
     * @param params The full set of information needed to construct a live
     *        data source.
     *
     * @return boolean true if and only if this factory can process the resource
     *         indicated by the param set and all the required params are
     *         present.
     */
    boolean canProcess(Map<String, ? extends Serializable> params);

    /**
     * @see org.geotoolkit.storage.DataStoreFactory#canProcess(java.util.Map)
     */
    boolean canProcess(ParameterValueGroup params);

    /**
     * @see DataStoreFactory#open(org.opengis.parameter.ParameterValueGroup)
     */
    DataStore open(Map<String, ? extends Serializable> params) throws DataStoreException;

    /**
     * Open a link to the storage location.
     * This method is intended to open an existing storage.
     * <br/>
     * If the purpose is to create a new one storage use the create method :
     * @see DataStoreFactory#create(org.opengis.parameter.ParameterValueGroup)
     *
     * @param params
     * @return DataStore opened store
     * @throws DataStoreException if parameters are incorrect or connexion failed.
     */
    DataStore open(ParameterValueGroup params) throws DataStoreException;

    /**
     * @see DataStoreFactory#create(org.opengis.parameter.ParameterValueGroup)
     */
    DataStore create(Map<String, ? extends Serializable> params) throws DataStoreException;

    /**
     * Create a new storage location.
     * This method is intended to create from scratch a new storage location.
     * <br/>
     * If the purpose is to open an already existing  storage use the open method :
     * @see DataStoreFactory#open(org.opengis.parameter.ParameterValueGroup)
     *
     * @param params
     * @return FeatureStore created store
     * @throws DataStoreException if parameters are incorrect or creation failed.
     */
    DataStore create(ParameterValueGroup params) throws DataStoreException;

}
