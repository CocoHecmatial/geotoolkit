/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011-2014, Geomatys
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
package org.geotoolkit.process.coverage.copy;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.sis.geometry.GeneralEnvelope;
import org.geotoolkit.coverage.CoverageUtilities;
import org.geotoolkit.coverage.GridSampleDimension;
import org.geotoolkit.coverage.grid.GeneralGridGeometry;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.grid.ViewType;
import org.geotoolkit.coverage.io.CoverageReader;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.io.GridCoverageReadParam;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.image.iterator.PixelIterator;
import org.geotoolkit.image.iterator.PixelIteratorFactory;
import org.geotoolkit.referencing.CRS;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Remi Marechal (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @author Quentin Boileau (Geomatys)
 * @module pending
 */
public class StatisticOp{

    public static final String MINIMUM = "min";
    public static final String MAXIMUM = "max";
    
    private StatisticOp(){
    }

    /**
     * Analyse image to return min and max value per bands.
     * @param reader
     * @param imageIndex
     * @return A Map with two Entry.
     * Each Entry have a name ("min", "max") and values are an double[] for each bands.
     * @throws CoverageStoreException
     */
    public static Map<String,Object> analyze(CoverageReader reader, int imageIndex) throws CoverageStoreException {
        if (reader instanceof GridCoverageReader) {
            return analyze((GridCoverageReader)reader, imageIndex);
        } else {
            throw new UnsupportedOperationException("Support GridCoverageReader only.");
        }
    }

    /**
     * Analyse image to return min and max value per bands. No-data are excluded.
     * @param reader
     * @param imageIndex
     * @return A Map with two Entry.
     * Each Entry have a name ("min", "max") and values are an double[] for each bands.
     * @throws CoverageStoreException
     */
    public static Map<String,Object> analyze(GridCoverageReader reader, int imageIndex) throws CoverageStoreException {
        return analyze(reader, imageIndex, ViewType.GEOPHYSICS);
    }

    /**
     * Analyse image to return min and max value per bands. No-data are excluded.
     * @param reader
     * @param imageIndex
     * @return A Map with two Entry.
     * Each Entry have a name ("min", "max") and values are an double[] for each bands.
     * @throws CoverageStoreException
     */
    public static Map<String,Object> analyze(GridCoverageReader reader, int imageIndex, ViewType viewType) throws CoverageStoreException {

        try {
            final GeneralGridGeometry gridGeometry = reader.getGridGeometry(imageIndex);
            CoordinateReferenceSystem crs = gridGeometry.getCoordinateReferenceSystem();
            final MathTransform gridToCRS = gridGeometry.getGridToCRS();
            final GridEnvelope extent = gridGeometry.getExtent();
            final int dim = extent.getDimension();

            //TODO analyse CRS to find lat/lon dimension position in extent envelope.
            final double[] low  = new double[dim];
            final double[] high = new double[dim];
            low[0]  = extent.getLow(0);
            high[0] = extent.getHigh(0);
            low[1]  = extent.getLow(1);
            high[1] = extent.getHigh(1);

            final GeneralEnvelope sliceExtent = new GeneralEnvelope(crs);
            for (int i = 0; i < dim; i++) {
                sliceExtent.setRange(i, low[i], high[i]);
            }

            final GridCoverageReadParam readParam = new GridCoverageReadParam();
            readParam.setEnvelope(CRS.transform(gridToCRS, sliceExtent));
            readParam.setCoordinateReferenceSystem(crs);

            final GridCoverage coverage = reader.read(imageIndex, readParam);
            GridCoverage2D coverage2D = CoverageUtilities.firstSlice(coverage);
            coverage2D = coverage2D.view(viewType);
            double[][] noData = getNoData(coverage2D);

            return analyze(coverage2D.getRenderedImage(), noData);
        } catch (TransformException e) {
            throw new CoverageStoreException(e.getMessage(), e);
        }
    }

    /**
     * Extract no-data values from GridCoverage2D.
     * @param coverage
     * @return
     */
    private static double[][] getNoData(GridCoverage2D coverage) {
        final GridSampleDimension[] dimensions = coverage.getSampleDimensions();
        double[][] noData = null;
        if(dimensions!=null){
            noData = new double[dimensions.length][0];
            for(int i=0;i<dimensions.length;i++){
                double[] candidate = dimensions[i].getNoDataValues();
                if(candidate!=null) noData[i] = candidate;
            }
        }
        return noData;
    }

    /**
     * Analyse image to return min and max value per bands.
     * @param image
     * @return A Map with two Entry. 
     * Each Entry have a name ("min", "max") and values are an double[] for each bands.
     */
    public static Map<String,Object> analyze(RenderedImage image) {
        return analyze(image,null);
    }
    
    
    /**
     * Analyse image to return min and max value per bands.
     * @param image
     * @param noData those values will be ignored in the analyze
     * @return A Map with two Entry. 
     * Each Entry have a name ("min", "max") and values are an double[] for each bands.
     */
    public static Map<String,Object> analyze(RenderedImage image, double[][] noData) {
        final Map<String,Object> analyze = new HashMap<>();
        if(noData !=null){
            noData = noData.clone();
            Arrays.sort(noData);
        }

        final SampleModel sm = image.getSampleModel();
        final int nbBands = sm.getNumBands();
        double[] min = new double[nbBands];
        double[] max = new double[nbBands];
        Arrays.fill(min, Double.MAX_VALUE);
        Arrays.fill(max, Double.MIN_VALUE);
        
        int b = 0;        
        final PixelIterator pix = PixelIteratorFactory.createDefaultIterator(image);
        while (pix.next()) {
            final double d = pix.getSampleDouble();
            if (!Double.isNaN(d) && (noData==null || !(Arrays.binarySearch(noData[b],d)>=0)) ) {
                min[b] = Math.min(min[b], d);
                max[b] = Math.max(max[b], d);
            }
            if (++b == nbBands) b = 0; 
        }
        analyze.put(MINIMUM, min);
        analyze.put(MAXIMUM, max);
        return analyze;
    }

}
