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

package org.geotoolkit.process.coverage.metadataextractor;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotoolkit.coverage.CoverageReference;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.coverage.io.ImageCoverageReader;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.process.AbstractProcess;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import static org.geotoolkit.process.coverage.metadataextractor.ExtractionDescriptor.*;
import org.geotoolkit.storage.DataStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.coverage.Coverage;
import org.opengis.metadata.Metadata;

/**
 *
 * @author Alexis Manin(Geomatys)
 */
public class ExtractionProcess extends AbstractProcess {

    ExtractionProcess(final ParameterValueGroup input) {
       super(INSTANCE, input); 
    }
    
    @Override
    protected void execute() throws ProcessException {
        ArgumentChecks.ensureNonNull("inputParameter", inputParameters);

        Object input = Parameters.getOrCreate(IN_SOURCE, inputParameters).getValue();
        Object reader = null;
        Metadata output = null;
        //Check if we get a file, or a reference to file.
        if (input instanceof String || input instanceof URL || input instanceof File) {
            reader = new ImageCoverageReader();
            try {
                ((ImageCoverageReader)reader).setInput(input);
            } catch (CoverageStoreException ex) {
                Logger.getLogger(ExtractionProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //Coverage case is not supported yet
        if (input instanceof Coverage) {
            //TODO : add a convenience method into coverage interface to get metadata
        } else if (input instanceof CoverageReference) {
            try {
                reader = ((CoverageReference)input).createReader();
            } catch (DataStoreException ex) {
                Logger.getLogger(ExtractionProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
            //Case if we directly get a reader
        } else if (input instanceof GridCoverageReader || input instanceof ImageCoverageReader) {
            reader = input;
        }
        if (reader == null){
            throw new ProcessException("Input object is not supported for this operation", this, null);
        }
        //Try to find metadata
        if (reader instanceof GridCoverageReader){
            try {
                output = ((GridCoverageReader)reader).getMetadata();
            } catch (CoverageStoreException ex) {
                Logger.getLogger(ExtractionProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (reader instanceof ImageCoverageReader){
            try {
                output = ((ImageCoverageReader)reader).getMetadata();
            } catch (CoverageStoreException ex) {
                Logger.getLogger(ExtractionProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Parameters.getOrCreate(OUT_METADATA, outputParameters).setValue(output);
    }
    
}
