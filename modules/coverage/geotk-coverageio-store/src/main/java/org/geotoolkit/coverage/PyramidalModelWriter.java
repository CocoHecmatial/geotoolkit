/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2013, Geomatys
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
package org.geotoolkit.coverage;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.grid.GridCoverageBuilder;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.io.GridCoverageWriteParam;
import org.geotoolkit.coverage.io.GridCoverageWriter;
import org.geotoolkit.geometry.GeneralEnvelope;
import org.geotoolkit.image.interpolation.Interpolation;
import org.geotoolkit.image.interpolation.InterpolationCase;
import org.geotoolkit.image.interpolation.Resample;
import org.geotoolkit.image.iterator.PixelIteratorFactory;
import org.geotoolkit.internal.referencing.CRSUtilities;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.referencing.operation.MathTransforms;
import org.geotoolkit.referencing.operation.transform.AffineTransform2D;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;


/**
 *
 * @author Cédric Briançon (Geomatys)
 * @author Rémi Maréchal (Geomatys)
 */
public class PyramidalModelWriter extends GridCoverageWriter {
    
    private final CoverageReference reference;
    
    /**
     * Build a writer on an existing {@linkplain CoverageReference coverage reference}.
     *
     * @param reference A valid {@linkplain CoverageReference coverage reference}.
     *                  Should not be {@code null} and an instance of {@link PyramidalModel}.
     * @throws IllegalArgumentException if the given {@linkplain CoverageReference coverage reference}
     *                                  is not an instance of {@link PyramidalModel}.
     */
    public PyramidalModelWriter(final CoverageReference reference) {
        if (!(reference instanceof PyramidalCoverageReference)) {
            throw new IllegalArgumentException("Given coverage reference should be an instance of PyramidalModel!");
        }
        this.reference = reference;
    }

    /**
     * Write a {@linkplain GridCoverage grid coverage} into the coverage store pointed by the
     * {@link CoverageReference coverage reference}.
     *
     * @param coverage {@link GridCoverage} to write.
     * @param param Writing parameters for the given coverage. Should not be {@code null}, and
     *              should contain a valid envelope.
     * @throws CoverageStoreException if something goes wrong during writing.
     * @throws CancellationException never thrown in this implementation.
     */
    @Override
    public void write(GridCoverage coverage, final GridCoverageWriteParam param) throws CoverageStoreException, CancellationException {
        ArgumentChecks.ensureNonNull("param", param);
        if (coverage == null) {
            return;
        }
        //geographic area where pixel values changes.
        Envelope requestedEnvelope = param.getEnvelope();
        ArgumentChecks.ensureNonNull("requestedEnvelope", requestedEnvelope);
        final CoordinateReferenceSystem crsCoverage2D;
        final CoordinateReferenceSystem envelopeCrs;
        try {
            crsCoverage2D = CRSUtilities.getCRS2D(coverage.getCoordinateReferenceSystem());
            envelopeCrs   = CRSUtilities.getCRS2D(requestedEnvelope.getCoordinateReferenceSystem());
        } catch (TransformException ex) {
            throw new CoverageStoreException(ex);
        }

        if (!CRS.equalsIgnoreMetadata(crsCoverage2D, envelopeCrs)) {
            try {
                requestedEnvelope = ReferencingUtilities.transform2DCRS(requestedEnvelope, crsCoverage2D);
            } catch (TransformException ex) {
                throw new CoverageStoreException(ex);
            }
        }

        //source image CRS to grid
        final MathTransform srcCRSToGrid;
        RenderedImage image = null;
        try {
            if (coverage instanceof GridCoverage2D) {
                image        = ((GridCoverage2D)coverage).getRenderedImage();
                srcCRSToGrid = ((GridCoverage2D)coverage).getGridGeometry().getGridToCRS(PixelInCell.CELL_CENTER).inverse();
            } else {
                final GridCoverageBuilder gcb = new GridCoverageBuilder();
                gcb.setGridCoverage(coverage);
                gcb.setPixelAnchor(PixelInCell.CELL_CENTER);
                image        = gcb.getRenderedImage();
                srcCRSToGrid = gcb.getGridToCRS().inverse();
            }
        } catch (NoninvertibleTransformException ex) {
            throw new CoverageStoreException(ex);
        }
        
        //to fill value table : see resample.
        final int nbBand        = image.getSampleModel().getNumBands();        
        final PyramidalCoverageReference pm = (PyramidalCoverageReference)reference;
        
        final BlockingQueue<Runnable> tileQueue;
        try {
            tileQueue = new ByTileQueue(pm, requestedEnvelope, crsCoverage2D, image, nbBand, srcCRSToGrid);
        } catch (DataStoreException ex) {
            throw new CoverageStoreException(ex);
        }
        final ThreadPoolExecutor service = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(), 
                Runtime.getRuntime().availableProcessors(), 
                1, TimeUnit.MINUTES, tileQueue);        
        service.prestartAllCoreThreads();
        service.shutdown();
        while(true){
            try {
                if(service.awaitTermination(1, TimeUnit.DAYS)){
                    break;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(PyramidalModelWriter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private static class ByTileQueue extends AbstractQueue<Runnable> implements BlockingQueue<Runnable> {

        private final PyramidalCoverageReference model;
        private final Envelope requestedEnvelope;
        private final CoordinateReferenceSystem crsCoverage2D;
        private final RenderedImage sourceImage;
        private final MathTransform srcCRSToGrid;
        private final int nbBand;
        private volatile boolean finished = false;
        
        //iteration state informations
        private final Iterator<Pyramid> pyramidsIte;
        private Iterator<GridMosaic> mosaics;
        private Pyramid currentPyramid = null;
        private MathTransform crsDestToSrcGrid;
        private GridMosaic currentMosaic = null;
        private CoordinateReferenceSystem destCrs2D;
        private MathTransform crsDestToCrsCoverage;
        private Envelope pyramidEnvelope;
        
        //mosaic infos
        private double res;
        private double mosULX;
        private double mosULY;
        private int mosAreaX;
        private int mosAreaY;
        private int mosAreaMaxX;
        private int mosAreaMaxY;
        private int idminx;
        private int idminy;
        private int idmaxx;
        private int idmaxy;
        private int idx = -1;
        private int idy = -1;
        
        private ByTileQueue(PyramidalCoverageReference model, Envelope requestedEnvelope, 
                CoordinateReferenceSystem crsCoverage2D, RenderedImage sourceImage, int nbBand, MathTransform srcCRSToGrid) throws DataStoreException{
            this.model = model;
            this.requestedEnvelope = requestedEnvelope;
            this.crsCoverage2D = crsCoverage2D;
            this.sourceImage = sourceImage;
            this.nbBand = nbBand;
            this.srcCRSToGrid = srcCRSToGrid;
            pyramidsIte = model.getPyramidSet().getPyramids().iterator();
        }
        
        @Override
        public Iterator<Runnable> iterator() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean isEmpty() {
            //we don't know the size, just give a value to indicate there are more.
            return finished;
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("Not supported.");
//            //we don't know the size, just give a value to indicate there are more.
//            return finished ? 0 : 100;
        }

        @Override
        public boolean offer(Runnable e) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Runnable peek() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void put(Runnable e) throws InterruptedException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean offer(Runnable e, long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Runnable take() throws InterruptedException {
            return poll();
        }

        @Override
        public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
            return poll();
        }

        @Override
        public int remainingCapacity() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public int drainTo(Collection<? super Runnable> c) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public int drainTo(Collection<? super Runnable> c, int maxElements) {
            throw new UnsupportedOperationException("Not supported.");
        }

        private boolean calculateMosaicRange(final GridMosaic mosaic, Envelope pyramidEnvelope){
            
            res = mosaic.getScale();
            final DirectPosition moUpperLeft = mosaic.getUpperLeftCorner();

            // define geographic intersection
            final GeneralEnvelope intersection = new GeneralEnvelope(mosaic.getEnvelope());
            final int minOrdinate = CoverageUtilities.getMinOrdinate(intersection.getCoordinateReferenceSystem());
            if (!intersection.intersects(pyramidEnvelope, true)) {
                return false;
            }
            intersection.intersect(pyramidEnvelope);

            // mosaic upper left corner coordinates.
            mosULX      = moUpperLeft.getOrdinate(minOrdinate);
            mosULY      = moUpperLeft.getOrdinate(minOrdinate+1);

            //define pixel work area of current mosaic.
            mosAreaX       = (int) Math.round(Math.abs((mosULX - intersection.getMinimum(minOrdinate))) /res);
            mosAreaY       = (int) Math.round(Math.abs((mosULY - intersection.getMaximum(minOrdinate+1))) /res);
            final int mosAreaWidth   = (int)((intersection.getSpan(minOrdinate) / res));
            
            final int mosAreaHeight  = (int)((intersection.getSpan(minOrdinate+1) / res));
            mosAreaMaxX    = mosAreaX + mosAreaWidth;
            mosAreaMaxY    = mosAreaY + mosAreaHeight;

            // mosaic tiles properties.
            final Dimension tileSize = mosaic.getTileSize();
            final int tileWidth      = tileSize.width;
            final int tileHeight     = tileSize.height;

            // define tiles indexes from current mosaic which will be changed.
            idminx         = mosAreaX / tileWidth;
            idminy         = mosAreaY / tileHeight;
            idmaxx         = (mosAreaMaxX + tileWidth-1) / tileWidth;
            idmaxy         = (mosAreaMaxY + tileHeight - 1) / tileHeight;
            
            return true;
        }
        
        @Override
        public synchronized Runnable poll() {
            
            //find next tile to build
            loop:
            while(true){
                if(currentPyramid==null){
                    if(pyramidsIte.hasNext()){
                        currentPyramid = pyramidsIte.next();
                        mosaics = currentPyramid.getMosaics().iterator();
                        
                        //define CRS and mathTransform from current pyramid to source coverage.
                        try {
                            destCrs2D = CRSUtilities.getCRS2D(currentPyramid.getCoordinateReferenceSystem());
                            crsDestToCrsCoverage = CRS.findMathTransform(destCrs2D, crsCoverage2D);
                            //geographic
                            pyramidEnvelope = ReferencingUtilities.transform2DCRS(requestedEnvelope, destCrs2D);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                        
                        crsDestToSrcGrid = MathTransforms.concatenate(crsDestToCrsCoverage, srcCRSToGrid);
                        
                    }else{
                        //we have finish
                        finished = true;
                        return null;
                    }
                }

                if(currentMosaic==null){
                    if(mosaics.hasNext()){
                        //next mosaic
                        currentMosaic = mosaics.next();
                        idx=-1;
                    }else{
                        //next pyramid
                        mosaics = null;
                        currentMosaic = null;
                        currentPyramid = null;
                        continue;
                    }
                }
                
                if(idx==-1){
                    calculateMosaicRange(currentMosaic, pyramidEnvelope);
                    idx = idminx-1;
                    idy = idminy;
                }
                
                while(true){
                    idx++;
                    if(idx>=idmaxx){
                        idy++;
                        idx = idminx;
                    }
                    if(idy>=idmaxy){
                        //finished thie mosaic
                        currentMosaic = null;
                        continue loop;
                    }
                    break;
                }
                
                return new TileUpdater(model, currentMosaic, 
                        idx, idy, 
                        mosAreaX, mosAreaY, 
                        mosAreaMaxX, mosAreaMaxY, 
                        mosULX, mosULY, 
                        crsDestToSrcGrid, 
                        sourceImage, nbBand, res);
            }
        }
        
    }
    
    private static class TileUpdater implements Runnable{

        private final PyramidalCoverageReference pm;
        private final Pyramid pyramid;
        private final GridMosaic mosaic;
        private final int idx;
        private final int idy;
        private final int mosAreaX;
        private final int mosAreaY;
        private final int mosAreaMaxX;
        private final int mosAreaMaxY;
        private final double mosULX;
        private final double mosULY;
        private final Interpolation interpolation;
        private final int nbBand;
        private final double res;
        private final MathTransform crsDestToSrcGrid;
        private final RenderedImage baseImage;
        
        private final int tileWidth;
        private final int tileHeight;

        public TileUpdater(PyramidalCoverageReference pm, GridMosaic mosaic, int idx, int idy, 
                int mosAreaX, int mosAreaY, int mosAreaMaxX, int mosAreaMaxY, 
                double mosULX, double mosULY, MathTransform crsDestToSrcGrid,
                RenderedImage image, int nbBand, double res) {
            this.pm = pm;
            this.mosaic = mosaic;
            this.pyramid = mosaic.getPyramid();
            this.idx = idx;
            this.idy = idy;
            this.mosAreaX = mosAreaX;
            this.mosAreaY = mosAreaY;
            this.mosAreaMaxX = mosAreaMaxX;
            this.mosAreaMaxY = mosAreaMaxY;
            this.mosULX = mosULX;
            this.mosULY = mosULY;
            this.interpolation = Interpolation.create(PixelIteratorFactory.createRowMajorIterator(image), InterpolationCase.NEIGHBOR, 2);;
            this.nbBand = nbBand;
            this.res = res;
            this.crsDestToSrcGrid = crsDestToSrcGrid;
            this.tileWidth = mosaic.getTileSize().width;
            this.tileHeight = mosaic.getTileSize().height;
            this.baseImage = image;
        }
        
        
        
        @Override
        public void run() {
            final BufferedImage currentlyTile;
            
            if(!mosaic.isMissing(idx, idy)){
                try {
                    currentlyTile = mosaic.getTile(idx, idy, null).getImageReader().read(0);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }else{
                //todo not exact
                if(nbBand==3){
                    currentlyTile = new BufferedImage(tileWidth, tileHeight,BufferedImage.TYPE_INT_RGB);
                }else if(nbBand==4){
                    currentlyTile = new BufferedImage(tileWidth, tileHeight,BufferedImage.TYPE_INT_ARGB);
                }else{
                    throw new RuntimeException("Can not create this type of tile");
                }
            }

            // define tile translation from bufferedImage min pixel position to mosaic pixel position.
            final int minidx = idx * tileWidth;
            final int minidy = idy * tileHeight;

            //define destination grid to CRS.
            final AffineTransform2D destImgToCRSDest = new AffineTransform2D(res, 0, 0, -res, mosULX + (minidx + 0.5) * res, mosULY - (minidy + 0.5) * res);
            final MathTransform destImgToCrsCoverage = MathTransforms.concatenate(destImgToCRSDest, crsDestToSrcGrid);

            // define currently tile work area.
            final int tminx  = Math.max(mosAreaX, minidx);
            final int tminy  = Math.max(mosAreaY, minidy);
            final int tmaxx  = Math.min(mosAreaMaxX, minidx + tileWidth);
            final int tmaxy  = Math.min(mosAreaMaxY, minidy + tileHeight);
            final Rectangle tileAreaWork = new Rectangle();
            tileAreaWork.setBounds(tminx - minidx, tminy - minidy, tmaxx - tminx, tmaxy - tminy);
            
            try {
                final Resample resample = new Resample(destImgToCrsCoverage.inverse(), currentlyTile, tileAreaWork, interpolation, new double[nbBand]);
                resample.fillImage();
                pm.writeTile(pyramid.getId(), mosaic.getId(), idx, idy, currentlyTile);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        
    }
    
}
