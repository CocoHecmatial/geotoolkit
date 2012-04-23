/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Geomatys
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

package org.geotoolkit.data.osm.bin;

import java.util.Collections;
import org.geotoolkit.data.AbstractDataStoreFactory;
import org.geotoolkit.data.AbstractFileDataStoreFactory;
import org.geotoolkit.data.DataStore;
import org.geotoolkit.metadata.iso.DefaultIdentifier;
import org.geotoolkit.metadata.iso.citation.DefaultCitation;
import org.geotoolkit.metadata.iso.identification.DefaultServiceIdentification;
import org.geotoolkit.parameter.DefaultParameterDescriptor;
import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.parameter.DefaultParameterDescriptorGroup;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.Identification;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Datastore factory for Open Street Map Binary files (*.obm).
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class OSMBinDataStoreFactory extends AbstractFileDataStoreFactory{

    /** factory identification **/
    public static final String NAME = "osm-bin";
    public static final DefaultServiceIdentification IDENTIFICATION;
    static {
        IDENTIFICATION = new DefaultServiceIdentification();
        final Identifier id = new DefaultIdentifier(NAME);
        final DefaultCitation citation = new DefaultCitation(NAME);
        citation.setIdentifiers(Collections.singleton(id));
        IDENTIFICATION.setCitation(citation);
    }
    
    public static final ParameterDescriptor<String> IDENTIFIER = new DefaultParameterDescriptor<String>(
                    AbstractDataStoreFactory.IDENTIFIER.getName().getCode(),
                    AbstractDataStoreFactory.IDENTIFIER.getRemarks(), String.class,NAME,true);
    
    private static final String[] EXTENSIONS = new String[]{".obm"};

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            new DefaultParameterDescriptorGroup("OSMBinParameters",IDENTIFIER,URLP,NAMESPACE);

    @Override
    public Identification getIdentification() {
        return IDENTIFICATION;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getDescription() {
        return "Open Street Map binary files (*.obm)";
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ParameterDescriptorGroup getParametersDescriptor() {
        return PARAMETERS_DESCRIPTOR;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String[] getFileExtensions() {
        return EXTENSIONS;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public DataStore create(final ParameterValueGroup params) throws DataStoreException {
        checkCanProcessWithError(params);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public DataStore createNew(final ParameterValueGroup params) throws DataStoreException {
        throw new DataStoreException("Creation of OSMbin datastore not supported yet.");
    }

}
