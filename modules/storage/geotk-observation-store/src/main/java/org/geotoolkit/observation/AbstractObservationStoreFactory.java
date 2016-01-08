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
package org.geotoolkit.observation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.Classes;
import org.geotoolkit.factory.Factory;
import org.geotoolkit.feature.FeatureUtilities;
import org.geotoolkit.parameter.Parameters;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Abstract Observation Store Factory.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractObservationStoreFactory extends Factory implements ObservationStoreFactory {
 
    /**
     * Identifier, Mandatory.
     * Subclasses should redeclared this parameter with a different default value.
     */
    public static final ParameterDescriptor<String> IDENTIFIER = new ParameterBuilder()
            .addName("identifier")
            .addName(Bundle.formatInternational(Bundle.Keys.paramIdentifierAlias))
            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramIdentifierRemarks))
            .setRequired(true)
            .create(String.class, null);

    /**
     * Namespace, Optional.
     * Default namespace used for feature type.
     */
    public static final ParameterDescriptor<String> NAMESPACE = new ParameterBuilder()
            .addName("namespace")
            .addName(Bundle.formatInternational(Bundle.Keys.paramNamespaceAlias))
            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramNamespaceRemarks))
            .setRequired(false)
            .create(String.class, null);
    
    /**
     * {@inheritDoc }
     *
     * @return a display name derivate from class name.
     */
    @Override
    public CharSequence getDisplayName() {
        String displayName = Classes.getShortClassName(this);
        if(displayName.endsWith("Factory")){
            displayName = displayName.substring(0, displayName.length() - 7);
        }
        return displayName;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public CharSequence getDescription() {
        return getDisplayName();
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationStore open(Map<String, ? extends Serializable> params) throws DataStoreException {
        params = forceIdentifier(params);

        final ParameterValueGroup prm = FeatureUtilities.toParameter(params,getParametersDescriptor());
        if(prm == null){
            return null;
        }
        try{
            return open(prm);
        }catch(InvalidParameterValueException ex){
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationStore create(Map<String, ? extends Serializable> params) throws DataStoreException {
        params = forceIdentifier(params);

        final ParameterValueGroup prm = FeatureUtilities.toParameter(params,getParametersDescriptor());
        if(prm == null){
            return null;
        }
        try{
            return create(prm);
        }catch(InvalidParameterValueException ex){
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean canProcess(Map params) {
        //check the identifier is set
        params = forceIdentifier(params);

        //ensure it's the valid identifier
        final Object id = params.get(IDENTIFIER.getName().getCode());
        try{
            final String expectedId = ((ParameterDescriptor<String>)getParametersDescriptor()
                .descriptor(IDENTIFIER.getName().getCode())).getDefaultValue();
            if(!expectedId.equals(id)){
                return false;
            }
        }catch(ParameterNotFoundException ex){
            //this feature store factory does not declare a identifier id
        }



        final ParameterValueGroup prm = FeatureUtilities.toParameter(params,getParametersDescriptor());
        if(prm == null){
            return false;
        }
        try{
            return canProcess(prm);
        }catch(InvalidParameterValueException ex){
            return false;
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean canProcess(final ParameterValueGroup params) {
        if(params == null){
            return false;
        }

        //check identifier value is exist
        final boolean validId = checkIdentifier(params);
        if(!validId){
            return false;
        }

        final ParameterDescriptorGroup desc = getParametersDescriptor();
        if(!desc.getName().getCode().equalsIgnoreCase(params.getDescriptor().getName().getCode())){
            return false;
        }

        final ConformanceResult result = Parameters.isValid(params, desc);
        return (result != null) && Boolean.TRUE.equals(result.pass());
    }
    
    /**
     * Set the identifier parameter in the map if not present.
     */
    private Map<String,Serializable> forceIdentifier(Map params){

        if(!params.containsKey(IDENTIFIER.getName().getCode())){
            //identifier is not specified, force it
            final ParameterDescriptorGroup desc = getParametersDescriptor();
            params = new HashMap<String, Serializable>(params);
            final Object value = ((ParameterDescriptor)desc.descriptor(IDENTIFIER.getName().getCode())).getDefaultValue();
            params.put(IDENTIFIER.getName().getCode(), (Serializable)value);
        }
        return params;
    }

    /**
     * Check if the Identifier parameter exist.
     * if it exist, it must be set to 'value' otherwise return false.
     * if not present, return true;
     * @return
     */
    protected boolean checkIdentifier(final ParameterValueGroup params){
        final String expectedId = ((ParameterDescriptor<String>)getParametersDescriptor()
                .descriptor(IDENTIFIER.getName().getCode())).getDefaultValue();

        for(GeneralParameterValue val : params.values()){
            if(val.getDescriptor().getName().getCode().equals(IDENTIFIER.getName().getCode())){
                final Object candidate = ((ParameterValue)val).getValue();
                return expectedId.equals(candidate);
            }
        }

        return true;
    }
    
    /**
     * Create the identifier descriptor, and set only one valid value, the one in parameter.
     *
     * TODO : Maybe change the string in parameter to string array.
     * @param idValue the value to use for identifier.
     *
     * @return an identifier descriptor.
     */
    public static ParameterDescriptor<String> createFixedIdentifier(String idValue) {
        return new ParameterBuilder()
                    .addName(IDENTIFIER.getName().getCode())
                    .setRemarks(IDENTIFIER.getRemarks())
                    .setRequired(true)
                    .createEnumerated(String.class, new String[]{idValue}, idValue);
    }
}
