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

package org.geotoolkit.data.gml;

import java.util.Collections;
import org.geotoolkit.data.AbstractFileFeatureStoreFactory;
import org.geotoolkit.data.FeatureStore;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.geotoolkit.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.ResourceInternationalString;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.Identification;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

import org.geotoolkit.utility.parameter.ParametersExt;
import org.geotoolkit.storage.DataType;
import org.geotoolkit.storage.DefaultFactoryMetadata;
import org.geotoolkit.storage.FactoryMetadata;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValue;

/**
 * GML featurestore factory.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GMLFeatureStoreFactory extends AbstractFileFeatureStoreFactory {

    /** factory identification **/
    public static final String NAME = "gml";
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
     * Open a folder of sparsed features
     */
    public static final ParameterDescriptor<Boolean> SPARSE = createDescriptor("sparse",
                    new ResourceInternationalString("org/geotoolkit/gml/bundle","paramSparseAlias"),
                    new ResourceInternationalString("org/geotoolkit/gml/bundle","paramSparseRemarks"),
                    Boolean.class,null,Boolean.FALSE,null,null,null,false);
    
    /**
     * Open a store where gml files may not exist, in this case the given xsd is used
     * to list the possible types.
     */
    public static final ParameterDescriptor<String> XSD = createDescriptor("xsd",
                    new ResourceInternationalString("org/geotoolkit/gml/bundle","paramXSDAlias"),
                    new ResourceInternationalString("org/geotoolkit/gml/bundle","paramXSDRemarks"),
                    String.class,null,null,null,null,null,false);

    /**
     * Name of the feature type to use in the XSD.
     */
    public static final ParameterDescriptor<String> XSD_TYPE_NAME = createDescriptor("xsdtypename",
                    new ResourceInternationalString("org/geotoolkit/gml/bundle","paramXSDTypeNameAlias"),
                    new ResourceInternationalString("org/geotoolkit/gml/bundle","paramXSDTypeNameRemarks"),
                    String.class,null,null,null,null,null,false);
    
    public static final ParameterDescriptor<Boolean> LONGITUDE_FIRST = createDescriptor("longitudeFirst",
                    new ResourceInternationalString("org/geotoolkit/gml/bundle","longitudeFirstAlias"),
                    new ResourceInternationalString("org/geotoolkit/gml/bundle","longitudeFirstRemarks"),
                    Boolean.class,null,true,null,null,null,false);
    
    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            new DefaultParameterDescriptorGroup("GMLParameters",
                IDENTIFIER,URLP,SPARSE,XSD,XSD_TYPE_NAME,LONGITUDE_FIRST,NAMESPACE);

    @Override
    public Identification getIdentification() {
        return IDENTIFICATION;
    }

    @Override
    public CharSequence getDescription() {
        return new ResourceInternationalString("org/geotoolkit/gml/bundle", "datastoreDescription");
    }

    @Override
    public CharSequence getDisplayName() {
        return new ResourceInternationalString("org/geotoolkit/gml/bundle", "datastoreTitle");
    }

    @Override
    public ParameterDescriptorGroup getParametersDescriptor() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public FeatureStore open(final ParameterValueGroup params) throws DataStoreException {
        checkCanProcessWithError(params);
        final Boolean sparse = ParametersExt.getOrCreateValue(params, SPARSE.getName().getCode()).booleanValue();
        if(sparse){
            return new GMLSparseFeatureStore(params);
        }else{
            return new GMLFeatureStore(params);
        }
    }

    @Override
    public FeatureStore create(final ParameterValueGroup params) throws DataStoreException {
        return open(params);
    }

    @Override
    public String[] getFileExtensions() {
        return new String[] {".gml"};
    }
 
    @Override
    public FactoryMetadata getMetadata() {
        return new DefaultFactoryMetadata(DataType.VECTOR, true, false, false, false, GEOMS_ALL);
    }

    @Override
    public boolean canProcess(ParameterValueGroup params) {
        Boolean sparse = null;
        try{
            ParameterValue<?> parameter = params.parameter(SPARSE.getName().getCode());
            if(parameter!=null && parameter.getValue() instanceof Boolean){
                sparse = (Boolean) parameter.getValue();
            }
        }catch(ParameterNotFoundException ex){
        }
        if(sparse != null && sparse){
            return true;
        }else{
            return super.canProcess(params);
        }
    }



}
