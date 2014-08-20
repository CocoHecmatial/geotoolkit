/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
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
package org.geotoolkit.image.iterator;

import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import org.opengis.coverage.grid.SequenceType;

/**
 * An Iterator for traversing anyone rendered Image.
 * <p>
 * Iteration transverse each tiles(raster) from rendered image or raster source one by one in order.
 * Iteration to follow tiles(raster) begin by raster bands, next, raster x coordinates,
 * and to finish raster y coordinates.
 * <p>
 * Iteration follow this scheme :
 * tiles band --&gt; tiles x coordinates --&gt; tiles y coordinates --&gt; next rendered image tiles.
 *
 * Moreover iterator traversing a read-only each rendered image tiles(raster) in top-to-bottom, left-to-right order.
 * Furthermore iterator directly read in data table within raster {@code DataBuffer}.
 *
 * @author Rémi Marechal       (Geomatys).
 * @author Martin Desruisseaux (Geomatys).
 */
abstract class DefaultDirectIterator extends PixelIterator{

    /**
     * Current raster width.
     */
    protected final int rasterWidth;

    /**
     * Step value to move forward in iteration.
     * @see DefaultDirectIterator#next()
     */
    private int cursorStep;

    /**
     * First iteration position.
     * @see DefaultDirectIterator#rewind()
     */
    private int baseCursor;

    /**
     * Cursor position in current raster {@link java.awt.image.DataBuffer}.
     */
    protected int dataCursor;

    /**
     * Maximum iteration value from current raster.
     */
    protected int maxBanks;

    /**
     * Tile scanLineStride from Raster or RenderedImage Sample model.
     */
    protected int scanLineStride;

    /**
     * Number of steps to move to get to next sample.
     */
    protected int sampleStride;

    /**
     * Current raster lower corner X coordinate;
     */
    protected int crMinX;

    /**
     * Current raster lower corner Y coordinate;
     */
    protected int crMinY;

    /**
     * Create raster iterator to follow from minX, minY raster and rectangle intersection coordinate.
     *
     * @param raster will be followed by this iterator.
     * @param subArea {@code Rectangle} which define read iterator area.
     * @throws IllegalArgumentException if subArea don't intersect raster boundary.
     */
    protected DefaultDirectIterator(final Raster raster, final Rectangle subArea) {
        super(raster, subArea);
        final SampleModel sampleM = raster.getSampleModel();
        if (sampleM instanceof ComponentSampleModel) {
            ComponentSampleModel sModel = (ComponentSampleModel) sampleM;
            this.sampleStride = sModel.getPixelStride();
            this.scanLineStride = sModel.getScanlineStride();
        } else {
            throw new IllegalArgumentException("DefaultDirectIterator constructor : sample model not conform");
        }
        this.rasterWidth = raster.getWidth();
        this.crMinX = raster.getMinX();
        this.crMinY = raster.getMinY();

        if (subArea != null) {
            this.maxBanks  = (areaIterateMaxX - crMinX)*rasterNumBand* sampleStride + (areaIterateMaxY-crMinY-1) * scanLineStride;
            this.cursorStep = scanLineStride - (areaIterateMaxX - areaIterateMinX)*rasterNumBand* sampleStride;
            this.dataCursor = baseCursor = (areaIterateMinX - crMinX) * rasterNumBand* sampleStride + (areaIterateMinY-crMinY) * scanLineStride - 1;
            //this.maxX = (areaIterateMaxX-crMinX) * rasterNumBand + (areaIterateMinY-crMinY) * scanLineStride;
        } else {
            this.maxX = rasterWidth * rasterNumBand;
            this.maxBanks   = rasterWidth * rasterNumBand* sampleStride + (raster.getHeight()-1) * scanLineStride;
            this.dataCursor = baseCursor = -1;
            this.cursorStep = 0;
            this.band       = -1;
        }
    }

    /**
     * Create default rendered image iterator.
     *
     * @param renderedImage image which will be follow by iterator.
     * @param subArea {@code Rectangle} which represent image sub area iteration.
     * @throws IllegalArgumentException if subArea don't intersect image boundary.
     */
    protected DefaultDirectIterator(final RenderedImage renderedImage, final Rectangle subArea) {
        super(renderedImage, subArea);
        final SampleModel sampleM = renderedImage.getSampleModel();
        if (sampleM instanceof ComponentSampleModel) {
            ComponentSampleModel sModel = (ComponentSampleModel) sampleM;
            this.sampleStride = sModel.getPixelStride();
            this.scanLineStride = sModel.getScanlineStride();

        } else {
            throw new IllegalArgumentException("DefaultDirectIterator constructor : sample model not conform");
        }
        this.rasterWidth = renderedImage.getTileWidth();
        this.rasterNumBand = sampleM.getNumBands();
        //initialize attributs to first iteration
        this.maxBanks = this.maxX = 1;
        this.tY       = tMinY;
        this.tX       = tMinX - 1;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public boolean next() {
        //if ((dataCursor+=sampleStride) == maxX) {
            if ((dataCursor+= sampleStride) >= maxBanks) {
                if (++tX == tMaxX) {
                    tX = tMinX;
                    if(++tY == tMaxY) return false;
                }
                updateCurrentRaster(tX, tY);
            } else {
                dataCursor += cursorStep;
                //maxX += scanLineStride;
            }
        //}
        return true;
    }

    /**
     * Update current data array and current raster from tiles array coordinates.
     *
     * @param tileX current X coordinate from rendered image tiles array.
     * @param tileY current Y coordinate from rendered image tiles array.
     */
    protected void updateCurrentRaster(int tileX, int tileY){
        //update raster
        this.currentRaster = renderedImage.getTile(tileX, tileY);
        this.crMinX = currentRaster.getMinX();
        this.crMinY = currentRaster.getMinY();
        final ComponentSampleModel sModel = (ComponentSampleModel) currentRaster.getSampleModel();
        this.scanLineStride = sModel.getScanlineStride();
        this.sampleStride = sModel.getPixelStride();

        //update min max from subArea and raster boundary
        final int minx  = Math.max(areaIterateMinX, crMinX) - crMinX;
        final int miny  = Math.max(areaIterateMinY, crMinY) - crMinY;
        final int maxx  = Math.min(areaIterateMaxX, crMinX + rasterWidth) - crMinX;
        final int maxy  = Math.min(areaIterateMaxY, crMinY + currentRaster.getHeight()) - crMinY;
        this.maxBanks   = maxx*rasterNumBand* sampleStride + (maxy-1) * scanLineStride;

        //step
        this.cursorStep = scanLineStride - (maxx - minx) * rasterNumBand * sampleStride;
        this.dataCursor = baseCursor = minx * rasterNumBand* sampleStride + miny * scanLineStride;
        //this.maxX       = baseCursor + (maxx - minx) * rasterNumBand;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public int getX() {
        return crMinX + dataCursor % scanLineStride / (rasterNumBand* sampleStride);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public int getY() {
        return crMinY + dataCursor / scanLineStride;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void rewind() {
        //initialize attributs like first iteration
        if (renderedImage == null) {
            band = -1;
            tX = tY = 0;
            tMaxX = tMaxY = 1;
            this.dataCursor = baseCursor;
            this.maxX  = baseCursor + 1 + (areaIterateMaxX - areaIterateMinX) * rasterNumBand;
        } else {
            this.maxX = this.maxBanks = 1;
            this.dataCursor = 0;
            this.tY = tMinY;
            this.tX = tMinX - 1;
        }
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void moveTo(int x, int y, int b) {
        super.moveTo(x, y, b);
        if (renderedImage != null) {
            final int riMinX = renderedImage.getMinX();
            final int riMinY = renderedImage.getMinY();
//            tX = (x - riMinX) / rasterWidth                   + renderedImage.getMinTileX();
//            tY = (y - riMinY) / renderedImage.getTileHeight() + renderedImage.getMinTileY();
            final int tTempX = (x - riMinX) / rasterWidth                   + renderedImage.getMinTileX();
            final int tTempY = (y - riMinY) / renderedImage.getTileHeight() + renderedImage.getMinTileY();
            if (tTempX != tX || tTempY != tY) {
                tX = tTempX;
                tY = tTempY;
                updateCurrentRaster(tX, tY);
            }
        }
        this.dataCursor = (x - crMinX) * rasterNumBand * sampleStride + (y - crMinY) * scanLineStride + b;// - 1;
        //this.maxX       = (y - crMinY) * scanLineStride + (Math.min(areaIterateMaxX, crMinX + rasterWidth) - crMinX)*rasterNumBand;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public SequenceType getIterationDirection() {
        if (renderedImage == null) return SequenceType.LINEAR;//1 raster seul
        if (renderedImage.getNumXTiles() <=1 && renderedImage.getNumYTiles() <= 1)
            return SequenceType.LINEAR;
        return null;
    }
}
