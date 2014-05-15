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

package org.geotoolkit.observation.file;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.feature.DefaultName;
import org.geotoolkit.observation.AbstractObservationStore;
import static org.geotoolkit.observation.file.FileObservationStoreFactory.FILE_PATH;
import org.geotoolkit.sos.netcdf.ExtractionResult;
import org.geotoolkit.sos.netcdf.Field;
import org.geotoolkit.sos.netcdf.NCFieldAnalyze;
import org.geotoolkit.sos.netcdf.NetCDFExtractor;
import org.geotoolkit.storage.DataFileStore;
import org.opengis.feature.type.Name;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileObservationStore extends AbstractObservationStore implements DataFileStore {
    
    private final File dataFile;
    private final NCFieldAnalyze analyze;
    
    public FileObservationStore(final ParameterValueGroup params) {
        super(params);
        dataFile = (File) params.parameter(FILE_PATH.getName().toString()).getValue();
        analyze = NetCDFExtractor.analyzeResult(dataFile, null);
    }
    
    public FileObservationStore(final File observationFile) {
        super(null);
        dataFile = observationFile;
        analyze = NetCDFExtractor.analyzeResult(dataFile, null);
    }

    /**
     * @return the dataFile
     */
    public File getDataFile() {
        return dataFile;
    }
    
    @Override
    public Set<Name> getProcedureNames() {
        final Set<Name> names = new HashSet<>();
        names.add(new DefaultName(getProcedureID()));
        return names;
    }
    
    private String getProcedureID() {
        String local;
        if (dataFile.getName().indexOf('.') != -1) {
            local = dataFile.getName().substring(0, dataFile.getName().lastIndexOf('.'));
        } else {
            local = dataFile.getName();
        }
        return local;
    }
    
    @Override
    public ExtractionResult getResults() {
        return NetCDFExtractor.getObservationFromNetCDF(analyze, getProcedureID(), null);
    }
    
    @Override
    public ExtractionResult getResults(final List<String> sensorIDs) {
        return NetCDFExtractor.getObservationFromNetCDF(analyze, getProcedureID(), sensorIDs);
    }
    
    @Override
    public void close() throws DataStoreException {
        // do nothing
    }

    @Override
    public Set<String> getPhenomenonNames() {
        final Set<String> phenomenons = new HashSet<>();
        for (Field field : analyze.phenfields) {
            phenomenons.add(field.label);
        }
        return phenomenons;
    }
    
    @Override
    public TemporalGeometricPrimitive getTemporalBounds() {
        final ExtractionResult result = NetCDFExtractor.getObservationFromNetCDF(analyze, getProcedureID(), null);
        if (result != null && result.spatialBound != null) {
            return result.spatialBound.getTimeObject("2.0.0");
        }
        return null;
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public File[] getDataFiles() throws DataStoreException {
        return new File[]{dataFile};
    }
}