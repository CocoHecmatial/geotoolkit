/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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

package org.geotoolkit.process.vector;

import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.parameter.DefaultParameterDescriptor;
import org.geotoolkit.parameter.DefaultParameterDescriptorGroup;
import org.geotoolkit.process.AbstractProcessDescriptor;
import org.geotoolkit.util.SimpleInternationalString;

import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;

/**
 * Description of vector process.
 * @author Quentin Boleau
 * @module pending
 */
public abstract class VectorDescriptor extends AbstractProcessDescriptor {

  
     /**
     * Mandatory - Feature Collection
     */
    public static final ParameterDescriptor <FeatureCollection<?>> FEATURE_IN=
            new DefaultParameterDescriptor("feature_in","Inpute Feature",FeatureCollection.class,null,true);


    public static final ParameterDescriptorGroup INPUT_DESC =
            new DefaultParameterDescriptorGroup("InputParameters",
                new GeneralParameterDescriptor[]{FEATURE_IN});


    /**
     * Mandatory - Resulting Feature Collection
     */
    public static final ParameterDescriptor <FeatureCollection<?>> FEATURE_OUT=
            new DefaultParameterDescriptor("feature_out","Outpute Feature",FeatureCollection.class,null,true);

     public static final ParameterDescriptorGroup OUTPUT_DESC =
            new DefaultParameterDescriptorGroup("OutputParameters",
                new GeneralParameterDescriptor[]{FEATURE_OUT});


    protected VectorDescriptor(String name,String msg){
        super(name, VectorProcessFactory.IDENTIFICATION,
                new SimpleInternationalString(msg),
                INPUT_DESC, OUTPUT_DESC);
    }



}
