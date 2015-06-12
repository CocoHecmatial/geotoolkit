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
package org.geotoolkit.processing.io.unpackfile;

import java.net.URL;
import org.geotoolkit.parameter.DefaultParameterDescriptor;
import org.geotoolkit.parameter.DefaultParameterDescriptorGroup;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.processing.io.IOProcessingRegistry;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Parameters description of UnpackFile process.
 * name of the process : "unpack"
 * inputs :
 * <ul>
 *     <li>SOURCE_IN "source" url,uri,file to read from</li>
 *     <li>TARGET_IN "target" url,uri,file destination</li>
 * </ul>
 * outputs :
 * <ul>
 *     <li>RESULT_OUT "files" result files</li>
 * </ul>
 * 
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public final class UnpackFileDescriptor extends AbstractProcessDescriptor {

    /**
     * Process name : unpack
     */
    public static final String NAME = "unpackFile";

    /**
     * Mandatory - path
     */
    public static final ParameterDescriptor<Object> SOURCE_IN =
            new DefaultParameterDescriptor("source", "url,uri,file to read from", 
            Object.class, null, true);
    
    /**
     * Mandatory - path
     */
    public static final ParameterDescriptor<Object> TARGET_IN =
            new DefaultParameterDescriptor("target", "url,uri,file destination", 
            Object.class, null, true);
            
    /** 
     * Input Parameters 
     */
    public static final ParameterDescriptorGroup INPUT_DESC =
            new DefaultParameterDescriptorGroup("InputParameters",
            new GeneralParameterDescriptor[]{SOURCE_IN,TARGET_IN});

    /**
     * Mandatory - result files
     */
    public static final ParameterDescriptor<URL[]> RESULT_OUT =
            new DefaultParameterDescriptor("files", "unpacked files", 
            URL[].class, null, true);
    
    /** 
     * Output Parameters 
     */
    public static final ParameterDescriptorGroup OUTPUT_DESC =
            new DefaultParameterDescriptorGroup("OutputParameters",RESULT_OUT);
    
    public static final ProcessDescriptor INSTANCE = new UnpackFileDescriptor();

    private UnpackFileDescriptor() {
        super(NAME, IOProcessingRegistry.IDENTIFICATION,
                new SimpleInternationalString("Unpack a compressed archive in a given directory, supports : zip,jar,tar,tar.gz ."),
                INPUT_DESC, OUTPUT_DESC);
    }

    /**
     *  {@inheritDoc }
     */
    @Override
    public Process createProcess(final ParameterValueGroup input) {
        return new UnpackFile(input);
    }
}
