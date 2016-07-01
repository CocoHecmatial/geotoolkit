/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
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
package org.geotoolkit.processing.coverage.statistics;

import org.geotoolkit.metadata.ImageStatistics;
import org.apache.sis.geometry.GeneralEnvelope;
import org.geotoolkit.coverage.*;
import org.geotoolkit.coverage.grid.GeneralGridGeometry;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.io.GridCoverageReadParam;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.image.internal.SampleType;
import org.geotoolkit.image.iterator.PixelIterator;
import org.geotoolkit.image.iterator.PixelIteratorFactory;
import org.geotoolkit.utility.parameter.ParametersExt;
import org.geotoolkit.processing.AbstractProcess;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.storage.coverage.CoverageReference;
import org.geotoolkit.storage.coverage.CoverageUtilities;
import org.geotoolkit.storage.coverage.GridMosaic;
import org.geotoolkit.storage.coverage.GridMosaicRenderedImage;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Arrays;
import org.geotoolkit.coverage.grid.ViewType;

import static org.geotoolkit.parameter.Parameters.getOrCreate;
import static org.geotoolkit.parameter.Parameters.value;
import static org.geotoolkit.processing.coverage.statistics.StatisticsDescriptor.*;
import org.opengis.geometry.Envelope;
import org.apache.sis.geometry.Envelopes;

/**
 * Process to create a {@link org.geotoolkit.process.coverage.statistics.ImageStatistics}
 * from a {@link org.geotoolkit.coverage.grid.GridCoverage2D} or {@link org.geotoolkit.coverage.io.GridCoverageReader}.
 *
 * Can be directly use using analyse() static methods: <br/>
 * Eg. : <br/>
 * <code>GridCoverage2D myCoverage = ...;</code><br/>
 * <code>ImageStatistics stats = Statistics.analyse(myCoverage, true);</code><br/>
 * <code>Long[] distribution = stats.getBand(0).tightenHistogram(50);</code><br/>s
 *
 * @author bgarcia
 * @author Quentin Boileau (Geomatys)
 */
public class Statistics extends AbstractProcess {

    public Statistics(final RenderedImage image, boolean excludeNoData){
        this(toParameters(image, null, null, null, 0, excludeNoData));
    }

    public Statistics(final GridCoverage2D coverage, boolean excludeNoData){
        this(toParameters(null, coverage, null, null, 0, excludeNoData));
    }

    public Statistics(final CoverageReference ref, boolean excludeNoData){
        this(toParameters(null, null, ref, null, 0, excludeNoData));
    }

    public Statistics(final GridCoverageReader reader, final int imageIdx, boolean excludeNoData){
        this(toParameters(null, null, null, reader, imageIdx, excludeNoData));
    }

    public Statistics(final ParameterValueGroup input) {
        super(StatisticsDescriptor.INSTANCE, input);
    }

    private static ParameterValueGroup toParameters(final RenderedImage image, final GridCoverage2D coverage, final CoverageReference ref,
                                                    final GridCoverageReader reader,  final int imageIdx, boolean excludeNoData) {
        final ParameterValueGroup params = StatisticsDescriptor.INSTANCE.getInputDescriptor().createValue();
        ParametersExt.getOrCreateValue(params, IMAGE.getName().getCode()).setValue(image);
        ParametersExt.getOrCreateValue(params, REF.getName().getCode()).setValue(ref);
        ParametersExt.getOrCreateValue(params, COVERAGE.getName().getCode()).setValue(coverage);
        ParametersExt.getOrCreateValue(params, READER.getName().getCode()).setValue(reader);
        ParametersExt.getOrCreateValue(params, IMAGE_IDX.getName().getCode()).setValue(imageIdx);
        ParametersExt.getOrCreateValue(params, EXCLUDE_NO_DATA.getName().getCode()).setValue(excludeNoData);
        return params;
    }

    /**
     * Run Statistics process with a RenderedImage and return ImageStatistics
     * @param image RenderedImage to analyse
     * @param excludeNoData exclude no-data flag (NaN values)
     * @return ImageStatistics
     * @throws ProcessException
     */
    public static ImageStatistics analyse(RenderedImage image, boolean excludeNoData) throws ProcessException {
        org.geotoolkit.process.Process process = new Statistics(image, excludeNoData);
        ParameterValueGroup out = process.call();
        return value(OUTCOVERAGE, out);
    }

    /**
     * Run Statistics process with a GridCoverage2D and return ImageStatistics
     *
     * @param coverage GridCoverage2D
     * @param excludeNoData exclude no-data flag
     * @return ImageStatistics
     * @throws ProcessException
     */
    public static ImageStatistics analyse(GridCoverage2D coverage, boolean excludeNoData) throws ProcessException {
        org.geotoolkit.process.Process process = new Statistics(coverage, excludeNoData);
        ParameterValueGroup out = process.call();
        return value(OUTCOVERAGE, out);
    }

    /**
     * Run Statistics process with a CoverageReference and return ImageStatistics
     *
     * @param ref CoverageReference
     * @param excludeNoData exclude no-data flag
     * @return ImageStatistics
     * @throws ProcessException
     */
    public static ImageStatistics analyse(CoverageReference ref, boolean excludeNoData) throws ProcessException {
        org.geotoolkit.process.Process process = new Statistics(ref, excludeNoData);
        ParameterValueGroup out = process.call();
        return value(OUTCOVERAGE, out);
    }

    /**
     * Run Statistics process with a CoverageReference and return ImageStatistics
     * the process is run on a reduced version of the data to avoid consuming to much resources.
     *
     * @param ref CoverageReference
     * @param excludeNoData exclude no-data flag
     * @param imageSize sampled image size
     * @return ImageStatistics
     * @throws ProcessException
     */
    public static ImageStatistics analyse(CoverageReference ref, boolean excludeNoData, int imageSize) throws ProcessException, CoverageStoreException{
        GridCoverageReader reader = null;
        try {
            reader = ref.acquireReader();
            final GeneralGridGeometry gridGeom = reader.getGridGeometry(ref.getImageIndex());
            final Envelope env = gridGeom.getEnvelope();
            final GridEnvelope ext = gridGeom.getExtent();

            final double[] res = new double[ext.getDimension()];
            double max = 0;
            for(int i=0;i<res.length;i++){
                res[i] = (env.getSpan(i) / imageSize);
                max = Math.max(max,res[i]);
            }
            Arrays.fill(res, max);


            final GridCoverageReadParam param = new GridCoverageReadParam();
            param.setEnvelope(env);
            param.setResolution(res);
            GridCoverage coverage = reader.read(ref.getImageIndex(), param);
            if(coverage instanceof GridCoverage2D){
                //we want the statistics on the real data values
                coverage = ((GridCoverage2D)coverage).view(ViewType.GEOPHYSICS);
            }
            org.geotoolkit.process.Process process = new Statistics((GridCoverage2D)coverage, excludeNoData);
            ParameterValueGroup out = process.call();
            return value(OUTCOVERAGE, out);

        } finally {
            if(reader!=null){
                ref.recycle(reader);
            }
        }
    }


    /**
     * Run Statistics process with a GridCoverageReader and return ImageStatistics
     *
     * @param reader GridCoverageReader
     * @param imageIdx image index to read
     * @param excludeNoData exclude no-data flag
     * @return ImageStatistics
     * @throws ProcessException
     */
    public static ImageStatistics analyse(GridCoverageReader reader, int imageIdx, boolean excludeNoData)
            throws ProcessException {
        org.geotoolkit.process.Process process = new Statistics(reader, imageIdx, excludeNoData);
        ParameterValueGroup out = process.call();
        return value(OUTCOVERAGE, out);
    }

    @Override
    protected void execute() throws ProcessException {

        final RenderedImage inImage = value(IMAGE, inputParameters);
        final boolean excludeNoData = value(EXCLUDE_NO_DATA, inputParameters);

        fireProgressing("Pre-analysing", 0f, false);
        final RenderedImage image;
        final ImageStatistics sc;
        if (inImage != null) {
            image = inImage;

            final SampleModel sm = image.getSampleModel();
            final SampleType sampleType = SampleType.valueOf(sm.getDataType());
            final int nbBands = sm.getNumBands();
            //create empty statistic object
            sc = new ImageStatistics(nbBands, sampleType);
            getOrCreate(OUTCOVERAGE, outputParameters).setValue(sc);

        } else {

            final GridCoverage2D inCoverage = value(COVERAGE, inputParameters);
            GridCoverage2D candidate = null;
            if (inCoverage != null) {
                candidate = inCoverage;
            } else {
                final GridCoverageReader reader = value(READER, inputParameters);
                final Integer imageIdx = value(IMAGE_IDX, inputParameters);

                if (reader != null && imageIdx != null) {
                    candidate = getCoverage(reader, imageIdx);
                } else {
                    final CoverageReference ref = value(REF, inputParameters);
                    if (ref != null) {
                        candidate = getCoverage(ref);
                    }
                }
            }

            if(candidate instanceof GridCoverage2D){
                //we want the statistics on the real data values
                candidate = ((GridCoverage2D)candidate).view(ViewType.GEOPHYSICS);
            }

            if (candidate == null) {
                throw new ProcessException("Null Coverage.", this, null);
            }

            //TODO extract view as process input parameter.
            //candidate = candidate.view(ViewType.GEOPHYSICS);
            image = candidate.getRenderedImage();

            final SampleModel sm = image.getSampleModel();
            final SampleType sampleType = SampleType.valueOf(sm.getDataType());
            final int nbBands = sm.getNumBands();
            sc = new ImageStatistics(nbBands, sampleType);

            final GridSampleDimension[] sampleDimensions = candidate.getSampleDimensions();
            //add no data values and name on bands
            for (int i = 0; i < sampleDimensions.length; i++) {
                sc.getBand(i).setNoData(sampleDimensions[i].getNoDataValues());
                sc.getBand(i).setName(sampleDimensions[i].getDescription().toString());
            }

            getOrCreate(OUTCOVERAGE, outputParameters).setValue(sc);
            fireProgressing("Pre-analysing finished", 10f, true);
            fireProgressing("Start range/histogram computing", 10f, true);
        }

        final ImageStatistics.Band[] bands = sc.getBands();
        final org.apache.sis.math.Statistics[] stats = new org.apache.sis.math.Statistics[bands.length];
        for(int i=0;i<bands.length;i++) stats[i] = new org.apache.sis.math.Statistics("stats");
        int nbBands = bands.length;

        //optimization for GridMosaicRenderedImage impl
        NumericHistogram[] histo = new NumericHistogram[nbBands];
        if (image instanceof GridMosaicRenderedImage) {
            final GridMosaicRenderedImage mosaicImage = (GridMosaicRenderedImage) image;
            final GridMosaic gridMosaic = mosaicImage.getGridMosaic();
            final Dimension gridSize = gridMosaic.getGridSize();

            int startX = 0;
            int startY = 0;
            int endX = gridSize.width;
            int endY = gridSize.height;
            int totalTiles = gridSize.width * gridSize.height;

            final Rectangle dataArea = gridMosaic.getDataArea();
            if (dataArea != null) {
                startX = dataArea.x;
                startY = dataArea.y;
                endX = dataArea.x + dataArea.width;
                endY = dataArea.y + dataArea.height;
                totalTiles = dataArea.width * dataArea.height;
            }

            //analyse each tiles of GridMosaicRenderedImage
            Raster tile;
            PixelIterator pix;
            int step = 1;
            for (int y = startY; y < endY; y++) {
                for (int x = startX; x < endX; x++) {
                    if (!gridMosaic.isMissing(x,y)) {
                        tile = mosaicImage.getTile(x, y);
                        pix = PixelIteratorFactory.createDefaultIterator(tile);

                        analyseRange(pix, stats, bands, excludeNoData);
                        pix.rewind();

                        mergeHistograms(histo, analyseHistogram(pix, bands, stats, excludeNoData));

                        updateBands(bands, histo);
                        fireProgressing("Histogram progressing", (step/totalTiles)*0.9f, true);
                    }
                    step++;
                }
            }

        } else {
            //standard image
            final PixelIterator pix = PixelIteratorFactory.createDefaultIterator(image);

            //get min/max
            analyseRange(pix, stats, bands, excludeNoData);
            fireProgressing("Start histogram computing", 55f, true);

            //reset iterator
            pix.rewind();

            //compute histogram
            histo = analyseHistogram(pix, bands, stats, excludeNoData);
            updateBands(bands, histo);
        }

        //copy statistics in band container
        for(int i=0;i<bands.length;i++){
            bands[i].setMin(stats[i].minimum());
            bands[i].setMax(stats[i].maximum());
            bands[i].setMean(stats[i].mean());
            bands[i].setStd(stats[i].standardDeviation(true));
        }

    }

    private void updateBands(ImageStatistics.Band[] bands, NumericHistogram[] histo) {
        for (int i = 0; i < bands.length; i++) {
            bands[i].setHistogram(histo[i].getHist());
        }
    }

    private void mergeHistograms(NumericHistogram[] histo, NumericHistogram[] tileHisto) {

        for (int i = 0; i < histo.length; i++) {
            NumericHistogram histo1 = histo[i];
            NumericHistogram histo2 = tileHisto[i];
            histo[i] = mergeHistograms(histo1, histo2);
        }
    }

    static NumericHistogram mergeHistograms(NumericHistogram histo1, NumericHistogram histo2) {

        if (histo1 == null) {
            return histo2;
        }

        int nbBins = histo1.getNbBins();
        double min = Math.min(histo1.getMin(), histo2.getMin());
        double max = Math.max(histo1.getMax(), histo2.getMax());
        NumericHistogram resultHisto = new NumericHistogram(nbBins, min, max);

        //add first histogram values
        long[] hist1 = histo1.getHist();
        double histo1BinSize = (histo1.getMax() - histo1.getMin()) / (double)nbBins;
        for (int j = 0; j <nbBins; j++) {
            double value = histo1.getMin()+histo1BinSize*j;
            long occurs = hist1[j];
            resultHisto.addValue(value, occurs);
        }

        //add second histogram values
        long[] hist2 = histo2.getHist();
        int nbBins2 = histo2.getNbBins();
        double histo2BinSize = (histo2.getMax() - histo2.getMin()) / (double)nbBins2;
        for (int j = 0; j <nbBins; j++) {
            double value = histo2.getMin()+histo2BinSize*j;
            long occurs = hist2[j];
            resultHisto.addValue(value, occurs);
        }

        return resultHisto;
    }

    private void analyseRange(final PixelIterator pix, final org.apache.sis.math.Statistics[] stats,
                              final ImageStatistics.Band[] bands, final boolean excludeNoData) {
        //first pass to compute min/max values
        double [][] noDatas = null;
        if (excludeNoData) {
            noDatas = new double[bands.length][];
            for (int i = 0; i < bands.length; i++) {
                noDatas[i] = bands[i].getNoData();
            }
        }

        int b = 0;
        while (pix.next()) {
            final double d = pix.getSampleDouble();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                continue;
            }

            //remove noData from stats
            if (noDatas != null && noDatas[b] != null && Arrays.binarySearch(noDatas[b], d) >= 0) {
                continue;
            }

            stats[b].accept(d);

            //reset b to loop on first band
            if (++b == stats.length) b = 0;
        }
    }

    /**
     * Analyse each pixels using a PixelIterator
     * @param pix PixelIterator
     * @param bands
     * @param excludeNoData
     */
    private NumericHistogram[] analyseHistogram(final PixelIterator pix, final ImageStatistics.Band[] bands,
                                                org.apache.sis.math.Statistics[] stats, final boolean excludeNoData) {

        int nbBands = bands.length;
        final NumericHistogram[] histograms = new NumericHistogram[nbBands];
        for (int i = 0; i < nbBands; i++) {
            int nbBins = getNbBins(bands[i].getDataType());
            histograms[i] = new NumericHistogram(nbBins, stats[i].minimum(), stats[i].maximum());
        }

        //reset iterator
        pix.rewind();

        //second pass to compute histogram
        // this int permit to loop on images band.
        int b = 0;
        if (excludeNoData) {
            while (pix.next()) {
                final double d = pix.getSampleDouble();

                //add value if not NaN or is flag as no-data
                if (!Double.isNaN(d) &&
                        (bands[b].getNoData() == null || !(Arrays.binarySearch(bands[b].getNoData(), d) >= 0))) {
                    histograms[b].addValue(d);
                }

                //reset b to loop on first band
                if (++b == nbBands) b = 0;
            }
        } else {
            //iter on each pixel band by band to add values on each band.
            while (pix.next()) {
                final double d = pix.getSampleDouble();
                histograms[b].addValue(d);

                //reset b to loop on first band
                if (++b == nbBands) b = 0;
            }
        }
        return histograms;
    }

    private int getNbBins(SampleType dataType) {
        if (dataType != null && dataType.equals(SampleType.BYTE)) {
            return 255;
        }
        return 1000;
    }

    /**
     * Read coverage from CoverageReference
     * @param ref
     * @return
     * @throws ProcessException
     */
    private GridCoverage2D getCoverage(CoverageReference ref) throws ProcessException {
        try {
            final GridCoverageReader reader = ref.acquireReader();
            GridCoverage2D coverage = getCoverage(reader, ref.getImageIndex());
            ref.recycle(reader);
            return coverage;

        } catch (CoverageStoreException e) {
            throw new ProcessException(e.getMessage(), this, e);
        }
    }

    /**
     * Read coverage from a GridCoverageReader.
     * @param reader
     * @param imageIdx
     * @return
     * @throws ProcessException
     */
    private GridCoverage2D getCoverage(GridCoverageReader reader, int imageIdx) throws ProcessException {
        try {
            final GeneralGridGeometry gridGeometry = reader.getGridGeometry(imageIdx);
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
            readParam.setEnvelope(Envelopes.transform(gridToCRS, sliceExtent));
            readParam.setDeferred(true);
            readParam.setCoordinateReferenceSystem(crs);

            final GridCoverage coverage = reader.read(imageIdx, readParam);
            return  CoverageUtilities.firstSlice(coverage);
        } catch (CoverageStoreException | TransformException e) {
            throw new ProcessException(e.getMessage(), this, e);
        }
    }
}
