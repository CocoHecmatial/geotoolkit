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
package org.geotoolkit.display2d.container.stateless;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.imageio.ImageReader;
import org.geotoolkit.coverage.*;
import org.geotoolkit.coverage.finder.CoverageFinder;
import org.geotoolkit.coverage.finder.CoverageFinderFactory;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.grid.GridCoverageBuilder;
import org.geotoolkit.coverage.grid.GridEnvelope2D;
import org.geotoolkit.coverage.grid.GridGeometry2D;
import org.geotoolkit.display.canvas.RenderingContext;
import org.geotoolkit.display.canvas.VisitFilter;
import org.geotoolkit.display.canvas.control.CanvasMonitor;
import org.geotoolkit.display.exception.PortrayalException;
import org.geotoolkit.display.primitive.SearchArea;
import org.geotoolkit.display2d.GO2Hints;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.display2d.canvas.J2DCanvas;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.display2d.style.CachedRule;
import org.geotoolkit.display2d.style.CachedSymbolizer;
import org.geotoolkit.display2d.style.renderer.DefaultRasterSymbolizerRenderer;
import org.apache.sis.geometry.GeneralEnvelope;
import org.geotoolkit.internal.referencing.CRSUtilities;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.util.Cancellable;
import org.geotoolkit.util.ImageIOUtilities;
import org.opengis.coverage.SampleDimension;
import org.opengis.display.primitive.Graphic;
import org.opengis.feature.type.Name;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Graphic for pyramidal coverage layers.
 * 
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class StatelessPyramidalCoverageLayerJ2D extends StatelessMapLayerJ2D<CoverageMapLayer> implements CoverageStoreListener{
    
    protected CoverageStoreListener.Weak weakStoreListener = new CoverageStoreListener.Weak(this);
    
    private final PyramidalCoverageReference model;
    private final double tolerance;
    private final CoverageFinder coverageFinder;

    @Deprecated
    public StatelessPyramidalCoverageLayerJ2D(final J2DCanvas canvas, final CoverageMapLayer layer){
        super(canvas, layer);
        this.coverageFinder = CoverageFinderFactory.createDefaultCoverageFinder();
        model = (PyramidalCoverageReference)layer.getCoverageReference();
        tolerance = 0.1; // in % , TODO use a flag to allow change value
        this.weakStoreListener.registerSource(layer.getCoverageReference());
    }
    
    public StatelessPyramidalCoverageLayerJ2D(final J2DCanvas canvas, final CoverageMapLayer layer, CoverageFinder coverageFinder){
        super(canvas, layer);
        this.coverageFinder = coverageFinder;
        model = (PyramidalCoverageReference)layer.getCoverageReference();
        tolerance = 0.1; // in % , TODO use a flag to allow change value
        this.weakStoreListener.registerSource(layer.getCoverageReference());
    }

    /**
     * {@inheritDoc }
     * @param context2D 
     */
    @Override
    public void paintLayer(final RenderingContext2D context2D) {
        
        final Name coverageName = item.getCoverageName();
        final CachedRule[] rules = GO2Utilities.getValidCachedRules(item.getStyle(),
                context2D.getSEScale(), coverageName,null);

        //we perform a first check on the style to see if there is at least
        //one valid rule at this scale, if not we just continue.
        if (rules.length == 0) {
            return;
        }

        final CanvasMonitor monitor = context2D.getMonitor();

        final Envelope canvasEnv2D = context2D.getCanvasObjectiveBounds2D();   
        final Envelope canvasEnv = context2D.getCanvasObjectiveBounds();   
        final PyramidSet pyramidSet;
        try {
            pyramidSet = model.getPyramidSet();
        } catch (DataStoreException ex) {
            monitor.exceptionOccured(ex, Level.WARNING);
            return;
        }
        
        Pyramid pyramid = null;
        try {
            pyramid = coverageFinder.findPyramid(pyramidSet, canvasEnv2D.getCoordinateReferenceSystem());
        } catch (FactoryException ex) {
            monitor.exceptionOccured(ex, Level.WARNING);
            return;
        }
                
        if(pyramid == null){
            //no reliable pyramid
            return;
        }
        
        final CoordinateReferenceSystem pyramidCRS = pyramid.getCoordinateReferenceSystem();
        final CoordinateReferenceSystem pyramidCRS2D;
        GeneralEnvelope wantedEnv2D;
        GeneralEnvelope wantedEnv;
        try {
            pyramidCRS2D = CRSUtilities.getCRS2D(pyramidCRS);
            wantedEnv2D = new GeneralEnvelope(CRS.transform(canvasEnv2D, pyramidCRS2D));
            wantedEnv = new GeneralEnvelope(ReferencingUtilities.transform(canvasEnv, pyramidCRS));
        } catch (TransformException ex) {
            monitor.exceptionOccured(ex, Level.WARNING);
            return;
        }

        /*
         * Apply CoverageMapLayer query (if not null) to wantedEnv Envelope.
         */
        final Map<String, Double> queryValues = DefaultRasterSymbolizerRenderer.extractQuery(item);
        wantedEnv = new GeneralEnvelope(DefaultRasterSymbolizerRenderer.fixEnvelopeWithQuery(queryValues, wantedEnv, pyramidCRS));

        //ensure we don't go out of the crs envelope
        final Envelope maxExt = CRS.getEnvelope(pyramidCRS);
        if(maxExt != null){
            wantedEnv2D.intersect(maxExt);
            if(Double.isNaN(wantedEnv2D.getMinimum(0))){ wantedEnv2D.setRange(0, maxExt.getMinimum(0), wantedEnv2D.getMaximum(0));  }
            if(Double.isNaN(wantedEnv2D.getMaximum(0))){ wantedEnv2D.setRange(0, wantedEnv2D.getMinimum(0), maxExt.getMaximum(0));  }
            if(Double.isNaN(wantedEnv2D.getMinimum(1))){ wantedEnv2D.setRange(1, maxExt.getMinimum(1), wantedEnv2D.getMaximum(1));  }
            if(Double.isNaN(wantedEnv2D.getMaximum(1))){ wantedEnv2D.setRange(1, wantedEnv2D.getMinimum(1), maxExt.getMaximum(1));  }
            wantedEnv.setRange(0, wantedEnv2D.getMinimum(0), wantedEnv2D.getMaximum(0));
            wantedEnv.setRange(1, wantedEnv2D.getMinimum(1), wantedEnv2D.getMaximum(1));
        }
        
        //the wanted image resolution
        final double wantedResolution;
        try {
            wantedResolution = GO2Utilities.pixelResolution(context2D, wantedEnv);
        } catch (TransformException ex) {
            monitor.exceptionOccured(ex, Level.WARNING);
            return;
        }

        GridMosaic mosaic = null;
        try {
            mosaic = coverageFinder.findMosaic(pyramid, wantedResolution, tolerance, wantedEnv,100);
        } catch (FactoryException ex) {
            monitor.exceptionOccured(ex, Level.WARNING);
            return;
        }
        if(mosaic == null){
            //no reliable mosaic
            return;
        }
        
        
        //we definitly do not want some NaN values
        if(Double.isNaN(wantedEnv.getMinimum(0))){ wantedEnv.setRange(0, Double.NEGATIVE_INFINITY, wantedEnv.getMaximum(0));  }
        if(Double.isNaN(wantedEnv.getMaximum(0))){ wantedEnv.setRange(0, wantedEnv.getMinimum(0), Double.POSITIVE_INFINITY);  }
        if(Double.isNaN(wantedEnv.getMinimum(1))){ wantedEnv.setRange(1, Double.NEGATIVE_INFINITY, wantedEnv.getMaximum(1));  }
        if(Double.isNaN(wantedEnv.getMaximum(1))){ wantedEnv.setRange(1, wantedEnv.getMinimum(1), Double.POSITIVE_INFINITY);  }
        
        
        final DirectPosition ul = mosaic.getUpperLeftCorner();
        final double tileMatrixMinX = ul.getOrdinate(0);
        final double tileMatrixMaxY = ul.getOrdinate(1);
        final Dimension gridSize = mosaic.getGridSize();
        final Dimension tileSize = mosaic.getTileSize();
        final double scale = mosaic.getScale();
        final double tileSpanX = scale * tileSize.width;
        final double tileSpanY = scale * tileSize.height;
        final int gridWidth = gridSize.width;
        final int gridHeight = gridSize.height;

        //find all the tiles we need --------------------------------------

        final double epsilon = 1e-6;
        final double bBoxMinX = wantedEnv.getMinimum(0);
        final double bBoxMaxX = wantedEnv.getMaximum(0);
        final double bBoxMinY = wantedEnv.getMinimum(1);
        final double bBoxMaxY = wantedEnv.getMaximum(1);
        double tileMinCol = Math.floor( (bBoxMinX - tileMatrixMinX) / tileSpanX + epsilon);
        double tileMaxCol = Math.floor( (bBoxMaxX - tileMatrixMinX) / tileSpanX - epsilon)+1;
        double tileMinRow = Math.floor( (tileMatrixMaxY - bBoxMaxY) / tileSpanY - epsilon);
        double tileMaxRow = Math.floor( (tileMatrixMaxY - bBoxMinY) / tileSpanY + epsilon)+1;

        //ensure we dont go out of the grid
        if(tileMinCol < 0) tileMinCol = 0;
        if(tileMaxCol > gridWidth) tileMaxCol = gridWidth;
        if(tileMinRow < 0) tileMinRow = 0;
        if(tileMaxRow > gridHeight) tileMaxRow = gridHeight;
                
        //tiles to render         
        final Map<Point,MathTransform> queries = new HashMap<Point,MathTransform>();
        final Map hints = new HashMap(item.getUserProperties());
        
        for(int tileCol=(int)tileMinCol; tileCol<tileMaxCol; tileCol++){   
            for(int tileRow=(int)tileMinRow; tileRow<tileMaxRow; tileRow++){
                if(mosaic.isMissing(tileCol, tileRow)){
                    //tile not available
                    continue;
                }
                
                final Point pt = new Point(tileCol, tileRow);
                final MathTransform trs = AbstractGridMosaic.getTileGridToCRS(mosaic, pt);
                queries.put(pt,trs);
            }
        }

        //paint tiles ----------------------------------------------------------
        if(queries.isEmpty()){
            //bypass if no queries
            return;
        } 
        Integer maxTiles = (Integer)context2D.getRenderingHints().get(GO2Hints.KEY_MAX_TILES);
        if(maxTiles==null) maxTiles = 500;
        if( queries.size() > maxTiles) {
            LOGGER.log(Level.INFO, "Too much tiles requiered to render layer at this scale.");
            return;
        }
        
        final BlockingQueue<Object> queue;
        try {
            queue = mosaic.getTiles(queries.keySet(), hints);
        } catch (DataStoreException ex) {
            monitor.exceptionOccured(ex, Level.WARNING);
            return;
        }
        
        final StatelessContextParams params = new StatelessContextParams(getCanvas(), getUserObject());
        params.update(context2D);
        while(true){
            Object obj = null;
            try {
                obj = queue.poll(50, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                monitor.exceptionOccured(ex, Level.INFO);
            }

            if(monitor.stopRequested()){
                if(queue instanceof Cancellable){
                    ((Cancellable)queue).cancel();
                }
                break;
            }

            if(obj == GridMosaic.END_OF_QUEUE){
                break;
            }

            if(obj instanceof TileReference){
                final TileReference tile = (TileReference)obj;
                final MathTransform trs = queries.get(tile.getPosition());
                paintTile(context2D, params, rules, pyramidCRS2D, tile, trs);
            }
        }
        
    }

    /**
     * {@inheritDoc }
     * @param context 
     * @param mask
     * @param filter
     * @param graphics  
     */
    @Override
    public List<Graphic> getGraphicAt(final RenderingContext context, 
            final SearchArea mask, final VisitFilter filter, List<Graphic> graphics) {

        if(!(context instanceof RenderingContext2D) ) return graphics;
        if(!item.isSelectable())                     return graphics;
        if(!item.isVisible())                        return graphics;

        final RenderingContext2D renderingContext = (RenderingContext2D) context;

        final Name coverageName = item.getCoverageName();
        final CachedRule[] rules = GO2Utilities.getValidCachedRules(item.getStyle(),
                renderingContext.getSEScale(), coverageName,null);

        //we perform a first check on the style to see if there is at least
        //one valid rule at this scale, if not we just continue.
        if (rules.length == 0) {
            return graphics;
        }

        if(graphics == null) graphics = new ArrayList<Graphic>();
        if(mask instanceof SearchAreaJ2D){
            //TODO
        }else{
            //TODO
        }

        return graphics;
    }

    private void paintTile(final RenderingContext2D context, StatelessContextParams params, CachedRule[] rules,
            final CoordinateReferenceSystem tileCRS ,final TileReference tile, MathTransform trs) {
        final CanvasMonitor monitor = context.getMonitor();
        final CoordinateReferenceSystem objCRS2D = context.getObjectiveCRS2D();
                
        if(monitor.stopRequested()){
            return;
        }
        
        Object input = tile.getInput();
        RenderedImage image;
        if(input instanceof RenderedImage){
            image = (RenderedImage) input;
        }else{
            ImageReader reader = null;
            try {
                reader = tile.getImageReader();
                image = reader.read(tile.getImageIndex());
            } catch (IOException ex) {
                monitor.exceptionOccured(ex, Level.WARNING);
                return;
            } finally {
                //dispose reader and substream
                ImageIOUtilities.releaseReader(reader);
            }
        }
        
        //check the crs
        if(!CRS.equalsIgnoreMetadata(tileCRS,objCRS2D) ){            
            //will be reprojected, we must check that image has alpha support
            //otherwise we will have black borders after reprojection
            if(!image.getColorModel().hasAlpha()){
                final BufferedImage buffer = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                buffer.createGraphics().drawRenderedImage(image, new AffineTransform());
                image = buffer;
            }            
        }
        
        //build the coverage ---------------------------------------------------
        final GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setName("tile");
        
        final GridEnvelope2D ge = new GridEnvelope2D(0, 0, image.getWidth(), image.getHeight());
        final GridGeometry2D gridgeo = new GridGeometry2D(ge, PixelInCell.CELL_CORNER, trs, tileCRS, null);
        gcb.setGridGeometry(gridgeo);
        gcb.setRenderedImage(image);
        final SampleDimension[] sampleDims = new SampleDimension[image.getSampleModel().getNumBands()];
        for(int i=0;i<sampleDims.length;i++){
            sampleDims[i] = new GridSampleDimension(String.valueOf(i));
        }
        gcb.setSampleDimensions(sampleDims);
        final GridCoverage2D coverage = (GridCoverage2D) gcb.build();
        
        
        final CoverageMapLayer tilelayer = MapBuilder.createCoverageLayer(coverage, getUserObject().getStyle(), "");
        final ProjectedCoverage projectedCoverage = new DefaultProjectedCoverage(params, tilelayer);
        for(final CachedRule rule : rules){
            for(final CachedSymbolizer symbol : rule.symbolizers()){
                try {
                    GO2Utilities.portray(projectedCoverage, symbol, context);
                } catch (PortrayalException ex) {
                    context.getMonitor().exceptionOccured(ex, Level.WARNING);
                }
            }
        }
    }
 
    @Override
    public void structureChanged(CoverageStoreManagementEvent event) {
    }

    @Override
    public void contentChanged(CoverageStoreContentEvent event) {
        if(item.isVisible() && getCanvas().getController().isAutoRepaint()){
            //TODO should call a repaint only on this graphic
            getCanvas().getController().repaint();
        }
    }
    
}
