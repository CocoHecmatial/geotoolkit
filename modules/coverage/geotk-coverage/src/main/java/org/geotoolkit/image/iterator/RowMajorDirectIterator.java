/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Open Source Geospatial Foundation (OSGeo)
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
import java.awt.image.RenderedImage;
import org.geotoolkit.util.ArgumentChecks;

/**
 * An Iterator for traversing anyone rendered Image with Byte type data.
 * <p>
 * Iteration transverse each pixel from rendered image or raster source line per line.
 * <p>
 * Iteration follow this scheme :
 * tiles band --&gt; tiles x coordinates --&gt; next X tile position in rendered image tiles array
 * --&gt; current tiles y coordinates --&gt; next Y tile position in rendered image tiles array.
 *
 * Moreover iterator traversing a read-only each rendered image tiles(raster) in top-to-bottom, left-to-right order.
 *
 * Furthermore iterator directly read in data table within raster {@code DataBuffer}.
 *
 * @author Rémi Marechal       (Geomatys).
 * @author Martin Desruisseaux (Geomatys).
 */
abstract class RowMajorDirectIterator extends PixelIterator {

    /**
     * Cursor position of current raster data.
     */
    protected int dataCursor;

    /**
     * Current raster width.
     */
    private int rasterWidth;

    /**
     * Abstract row index;
     */
    private int row;

    /**
     * Create default rendered image iterator.
     *
     * @param renderedImage image which will be follow by iterator.
     */
    RowMajorDirectIterator(final RenderedImage renderedImage) {
        ArgumentChecks.ensureNonNull("RenderedImage : ", renderedImage);
        this.renderedImage = renderedImage;

        //rect attributs
        this.subAreaMinX = renderedImage.getMinX();
        this.subAreaMinY = renderedImage.getMinY();
        this.subAreaMaxX = this.subAreaMinX + renderedImage.getWidth();
        this.subAreaMaxY = this.subAreaMinY + renderedImage.getHeight();

        //tiles attributs
        this.tMinX = renderedImage.getMinTileX();
        this.tMinY = renderedImage.getMinTileY();
        this.tMaxX = tMinX + renderedImage.getNumXTiles();
        this.tMaxY = tMinY + renderedImage.getNumYTiles();

        //initialize attributs to first iteration
        this.numBand = this.maxX = this.maxY = 1;
        this.tY = tMinY;
        this.tX = tMinX - 1;
    }

    /**
     * Create default rendered image iterator.
     *
     * @param renderedImage image which will be follow by iterator.
     * @param subArea {@code Rectangle} which represent image sub area iteration.
     * @throws IllegalArgumentException if subArea don't intersect image boundary.
     */
    RowMajorDirectIterator(final RenderedImage renderedImage, final Rectangle subArea) {
        ArgumentChecks.ensureNonNull("RenderedImage : ", renderedImage);
        ArgumentChecks.ensureNonNull("sub Area iteration : ", subArea);
        this.renderedImage = renderedImage;

        //rect attributs
        this.subAreaMinX = subArea.x;
        this.subAreaMinY = subArea.y;
        this.subAreaMaxX = this.subAreaMinX + subArea.width;
        this.subAreaMaxY = this.subAreaMinY + subArea.height;

        final int rimx = renderedImage.getMinX();
        final int rimy = renderedImage.getMinY();
        final int mtx = renderedImage.getMinTileX();
        final int mty = renderedImage.getMinTileY();

        final int mix = Math.max(subAreaMinX, rimx) - rimx;
        final int miy = Math.max(subAreaMinY, rimy) - rimy;
        final int max = Math.min(subAreaMaxX, rimx + renderedImage.getWidth()) - rimx;
        final int may = Math.min(subAreaMaxY, rimy + renderedImage.getHeight()) - rimy;
        if(mix > max || miy > may)
            throw new IllegalArgumentException("invalid subArea coordinates, no intersection between it and renderedImage"+renderedImage+subArea);

        final int tw = renderedImage.getTileWidth();
        final int th = renderedImage.getTileHeight();

        //tiles attributs
        this.tMinX = mix / tw + mtx;
        this.tMinY = miy / th + mty;
        this.tMaxX = max / tw + mtx;
        this.tMaxY = may / th + mty;

        //initialize attributs to first iteration
        this.numBand = this.maxX = this.maxY = 1;
        this.tY = tMinY;
        this.tX = tMinX - 1;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public boolean next() {
        if (++band == numBand) {
            band = 0;
            if (++dataCursor == maxX) {
                if (++tX == tMaxX) {
                    tX = tMinX;
                    if (++row == maxY) {
                        row = 0;
                        if (++tY == tMaxY) {
                            return false;
                        }
                    }
                }
                updateCurrentRaster(tX, tY);
            }
        }
        return true;
    }

    /**
     * Update current data array and current raster from tiles array coordinates.
     *
     * @param tileX current X coordinate from rendered image tiles array.
     * @param tileY current Y coordinate from rendered image tiles array.
     */
    protected void updateCurrentRaster(int tileX, int tileY) {
        //update raster
        this.currentRaster = renderedImage.getTile(tileX, tileY);
        final int cRMinX   = currentRaster.getMinX();
        final int cRMinY   = currentRaster.getMinY();
        this.rasterWidth = currentRaster.getWidth();

        //update min max from subArea and raster boundary
        this.minX    = Math.max(subAreaMinX, cRMinX) - cRMinX;
        this.minY    = Math.max(subAreaMinY, cRMinY) - cRMinY;
        this.maxX    = Math.min(subAreaMaxX, cRMinX + rasterWidth) - cRMinX;
        this.maxY    = Math.min(subAreaMaxY, cRMinY + currentRaster.getHeight()) - cRMinY;
        this.numBand = currentRaster.getNumBands();
        this.maxX += (minY + row ) * rasterWidth;
        dataCursor = minX + (minY + row) * rasterWidth;
        this.band = 0;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public int getX() {
        final int minx = (renderedImage == null) ? currentRaster.getMinX() : renderedImage.getMinX();
        return minx + (tX-tMinX) * rasterWidth + dataCursor % rasterWidth;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public int getY() {
        final int miny = (renderedImage == null) ? currentRaster.getMinY() : renderedImage.getMinY();
        return miny + (tY-tMinY) * currentRaster.getHeight() + dataCursor/rasterWidth;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void rewind() {
        this.numBand = this.maxX = this.maxY = 1;
        this.dataCursor = this.band = 0;
        this.tY = tMinY;
        this.tX = tMinX - 1;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void moveTo(int x, int y) {
        final int riMinX = renderedImage.getMinX();
        final int riMinY = renderedImage.getMinY();
        if (x < riMinX || x >= riMinX + renderedImage.getWidth()
        ||  y < riMinY || x >= riMinY + renderedImage.getHeight())
            throw new IllegalArgumentException("coordinate out of rendered image boundary"+renderedImage+x+y);
        tX = (x - riMinX)/renderedImage.getTileWidth() + renderedImage.getMinTileX();
        tY = (y - riMinY)/renderedImage.getTileHeight() + renderedImage.getMinTileY();
        updateCurrentRaster(tX, tY);
        this.band = -1;
        this.row = y;
        this.row -= currentRaster.getMinY();
        this.dataCursor = x;
        this.dataCursor -= currentRaster.getMinX();
        final int step = row * rasterWidth;
        this.dataCursor += step;
        this.maxX += step;
    }
}
