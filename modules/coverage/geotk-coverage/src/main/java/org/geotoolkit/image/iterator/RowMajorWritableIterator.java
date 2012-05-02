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
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;

/**
 * An Iterator for traversing anyone rendered Image.
 * <p>
 * Iteration transverse each pixel from rendered image or raster source line per line.
 * <p>
 *
 * Iteration follow this scheme :
 * tiles band --&lt; tiles x coordinates --&lt; next X tile position in rendered image tiles array
 * --&lt; current tiles y coordinates --&lt; next Y tile position in rendered image tiles array.
 *
 * Moreover iterator traversing a write or read each rendered image tiles(raster) in top-to-bottom, left-to-right order.
 *
 * Code example :
 * {@code
 *                  final RowMajorWritableRIIterator dWRII = new RowMajorWritableRIIterator(renderedImage);
 *                  while (dWRII.next()) {
 *                      dWRII.setSample(int value);
 *                  }
 * }
 *
 * @author Rémi Marechal       (Geomatys).
 * @author Martin Desruisseaux (Geomatys).
 */
class RowMajorWritableIterator extends RowMajorIterator {

    /**
     * Current last x tile position in rendered image tile array.
     */
    private int prectX;

    /**
     * Current last y tile position in rendered image tile array.
     */
    private int prectY;

    /**
     * Raster which is followed by iterator and wherein caller write.
     */
    private WritableRaster currentWritableRaster;

    /**
     * Rendered image which is followed by iterator and wherein caller write.
     */
    private WritableRenderedImage writableRenderedImage;

    /**
     * Create an appropriate iterator to read and write in a rendered image, row by row.
     *
     * @param renderedImage rendered image which is followed by read-only iterator.
     * @param writableRI    writable rendered image which is followed by this write-only iterator and wherein value is writing.
     * @throws IllegalArgumentException if renderedImage and writable renderedImage dimensions are not conform.
     * @throws IllegalArgumentException if renderedImage and writable renderedImage tiles configurations are not conform.
     */
    RowMajorWritableIterator(final RenderedImage renderedImage, final WritableRenderedImage writableRI) {
        super(renderedImage);
        checkRenderedImage(renderedImage, writableRI);
        this.writableRenderedImage = writableRI;
    }

    /**
     * Create an appropriate iterator to read and write in a rendered image sub area, row by row.
     *
     * @param renderedImage rendered image which is followed by read-only iterator.
     * @param writableRI    writable rendered image which is followed by this write-only iterator and wherein value is writing.
     * @param subArea       Rectangle which represent rendered image sub area, wherein move read and write iterator.
     * @throws IllegalArgumentException if renderedImage and writable renderedImage dimensions are not conform.
     * @throws IllegalArgumentException if renderedImage and writable renderedImage tiles configurations are not conform.
     */
    RowMajorWritableIterator(final RenderedImage renderedImage, final WritableRenderedImage writableRI, final Rectangle subArea) {
        super(renderedImage, subArea);
        checkRenderedImage(renderedImage, writableRI);
        this.writableRenderedImage = writableRI;
    }

    /**
     * Verify Rendered image conformity.
     */
    private void checkRenderedImage(final RenderedImage renderedImage, final WritableRenderedImage writableRI){
        if (renderedImage.getMinX()   != writableRI.getMinX()
         || renderedImage.getMinY()   != writableRI.getMinY()
         || renderedImage.getWidth()  != writableRI.getWidth()
         || renderedImage.getHeight() != writableRI.getHeight()
         || renderedImage.getSampleModel().getNumBands() != writableRI.getSampleModel().getNumBands())
         throw new IllegalArgumentException("rendered image and writable rendered image dimensions are not conform"+renderedImage+writableRI);

        if (renderedImage.getMinTileX()  != writableRI.getMinTileX()
         || renderedImage.getMinTileY()  != writableRI.getMinTileY()
         || renderedImage.getNumXTiles() != writableRI.getNumXTiles()
         || renderedImage.getNumYTiles() != writableRI.getNumYTiles()
         || renderedImage.getTileGridXOffset() != writableRI.getTileGridXOffset()
         || renderedImage.getTileGridYOffset() != writableRI.getTileGridYOffset()
         || renderedImage.getTileHeight() != writableRI.getTileHeight()
         || renderedImage.getTileWidth()  != writableRI.getTileWidth())
         throw new IllegalArgumentException("rendered image and writable rendered image tiles configuration are not conform"+renderedImage+writableRI);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void rewind() {
        currentWritableRaster = null;
        super.rewind();
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void setSample(int value) {
        currentWritableRaster.setSample(x, y, band, value);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void setSampleDouble(double value) {
        currentWritableRaster.setSample(x, y, band, value);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void setSampleFloat(float value) {
        currentWritableRaster.setSample(x, y, band, value);
    }

    /**
     * Update current writable raster from tiles array coordinates.
     */
    @Override
    protected void updateCurrentRaster(int tileX, int tileY) {
        super.updateCurrentRaster(tileX, tileY);
        if (currentWritableRaster != null) writableRenderedImage.releaseWritableTile(prectX, prectY);
        currentWritableRaster = writableRenderedImage.getWritableTile(tileX, tileY);
        this.prectX = tileX;
        this.prectY = tileY;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void close() {
         writableRenderedImage.releaseWritableTile(prectX, prectY);
    }
}
