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

package org.geotoolkit.data.wfs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotoolkit.client.AbstractServerFactory;
import org.geotoolkit.client.Server;
import org.geotoolkit.client.ServerFactory;
import org.geotoolkit.data.AbstractFeatureStoreFactory;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.metadata.iso.DefaultIdentifier;
import org.geotoolkit.metadata.iso.citation.DefaultCitation;
import org.geotoolkit.metadata.iso.identification.DefaultServiceIdentification;
import org.geotoolkit.parameter.DefaultParameterDescriptor;
import org.geotoolkit.parameter.DefaultParameterDescriptorGroup;
import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.util.ResourceInternationalString;
import org.geotoolkit.wfs.xml.WFSVersion;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.Identification;
import org.opengis.parameter.*;

/**
 * Datastore factory for WFS client.
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class WFSDataStoreFactory extends AbstractFeatureStoreFactory implements ServerFactory{

    /** factory identification **/
    public static final String NAME = "wfs";
    public static final DefaultServiceIdentification IDENTIFICATION;
    static {
        IDENTIFICATION = new DefaultServiceIdentification();
        final Identifier id = new DefaultIdentifier(NAME);
        final DefaultCitation citation = new DefaultCitation(NAME);
        citation.setIdentifiers(Collections.singleton(id));
        IDENTIFICATION.setCitation(citation);
    }
    
    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);
    
    /**
     * Version, Mandatory.
     */
    public static final ParameterDescriptor<String> VERSION;
    static{
        final String code = "version";
        final CharSequence remarks = AbstractServerFactory.I18N_VERSION;
        final Map<String,Object> params = new HashMap<String, Object>();
        params.put(DefaultParameterDescriptor.NAME_KEY, code);
        params.put(DefaultParameterDescriptor.REMARKS_KEY, remarks);
        final List<String> validValues =  new ArrayList<String>();
        for(WFSVersion version : WFSVersion.values()){
            validValues.add(version.getCode());
        }
        
        VERSION = new DefaultParameterDescriptor<String>(params, String.class, 
                validValues.toArray(new String[validValues.size()]), 
                WFSVersion.v110.getCode(), null, null, null, true);
    }
    /**
     * Optional -post request
     */
    public static final ParameterDescriptor<Boolean> POST_REQUEST =
            new DefaultParameterDescriptor<Boolean>("post",
                    new ResourceInternationalString("org/geotoolkit/wfs/bundle", "post"),
                    Boolean.class,false,false);    
    /**
     * Optional use true CRS axis ordering.
     */
    public static final ParameterDescriptor<Boolean> LONGITUDE_FIRST =
            new DefaultParameterDescriptor<Boolean>("longitudeFirst",
                    new ResourceInternationalString("org/geotoolkit/wfs/bundle", "longitudeFirst"),
                    Boolean.class,false,false);    

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            new DefaultParameterDescriptorGroup("WFSParameters",
                IDENTIFIER, AbstractServerFactory.URL, VERSION, AbstractServerFactory.SECURITY, LONGITUDE_FIRST,POST_REQUEST);

    @Override
    public Identification getIdentification() {
        return IDENTIFICATION;
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public CharSequence getDescription() {
        return new ResourceInternationalString("org/geotoolkit/wfs/bundle", "datastoreDescription");
    }

    @Override
    public CharSequence getDisplayName() {
        return new ResourceInternationalString("org/geotoolkit/wfs/bundle", "datastoreTitle");
    }
    
    

    /**
     * {@inheritDoc }
     */
    @Override
    public ParameterDescriptorGroup getParametersDescriptor() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public WebFeatureServer open(Map<String, ? extends Serializable> params) throws DataStoreException {
        return (WebFeatureServer)super.open(params);
    }

    @Override
    public WebFeatureServer open(ParameterValueGroup params) throws DataStoreException {
        checkCanProcessWithError(params);
        return new WebFeatureServer(params);
    }

    @Override
    public WebFeatureServer create(Map<String, ? extends Serializable> params) throws DataStoreException {
        return (WebFeatureServer)super.create(params);
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public WebFeatureServer create(final ParameterValueGroup params) throws DataStoreException {
        throw new DataStoreException("Can not create any new WFS DataStore");
    }
}
