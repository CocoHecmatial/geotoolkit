/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Johann Sorel
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
package org.geotoolkit.wps;

import org.geotoolkit.client.AbstractClientFactory;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.storage.DataType;
import org.geotoolkit.storage.DefaultFactoryMetadata;
import org.geotoolkit.storage.FactoryMetadata;
import org.opengis.parameter.*;

/**
 * WPS Server factory.
 *
 * @author Johann Sorel (Puzzle-GIS)
 * @module
 */
public class WPSClientFactory extends AbstractClientFactory{

    /** factory identification **/
    public static final String NAME = "wps";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    /**
     * Version, Mandatory.
     */
    public static final ParameterDescriptor<String> VERSION;
    static{
        final WPSVersion[] values = WPSVersion.values();
        final String[] validValues =  new String[values.length];
        for(int i=0;i<values.length;i++){
            validValues[i] = values[i].getCode();
        }
        VERSION = createVersionDescriptor(validValues, WPSVersion.auto.getCode());
    }

     /**
     * Dynamic loading, Optional.
     */
    public static final ParameterDescriptor<Boolean> DYNAMIC_LOADING = new ParameterBuilder()
            .addName("dynamic_loading")
            .setRequired(false)
            .create(Boolean.class, false);

    public static final ParameterDescriptorGroup PARAMETERS =
            new ParameterBuilder().addName(NAME).addName("WPSParameters").createGroup(IDENTIFIER, URL,VERSION,SECURITY,TIMEOUT, DYNAMIC_LOADING);

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS;
    }

    @Override
    public CharSequence getDescription() {
        return Bundle.formatInternational(Bundle.Keys.serverDescription);
    }

    @Override
    public CharSequence getDisplayName() {
        return Bundle.formatInternational(Bundle.Keys.serverTitle);
    }

    @Override
    public FactoryMetadata getMetadata() {
        return new DefaultFactoryMetadata(DataType.OTHER, false, false, false);
    }

    @Override
    public WebProcessingClient open(ParameterValueGroup params) throws DataStoreException {
        ensureCanProcess(params);
        return new WebProcessingClient(params);
    }

}
