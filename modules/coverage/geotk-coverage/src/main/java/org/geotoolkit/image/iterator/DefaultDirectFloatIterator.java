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
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * An Iterator for traversing anyone rendered Image which contains only Float type data.
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
 * Code example :
 * {@code
 *                  final DefaultFloatIterator dBI = new DefaultFloatIterator(renderedImage);
 *                  while (dBI.next()) {
 *                      dBI.getSample();
 *                  }
 * }
 *
 * @author Rémi Marechal       (Geomatys).
 * @author Martin Desruisseaux (Geomatys).
 */
public class DefaultDirectFloatIterator extends DefaultDirectIterator {

    /**
     * Current raster data table.
     */
    private float[][] currentDataArray;

    /**
     * Create Byte type raster iterator to follow from its minX and minY coordinates.
     *
     * @param raster will be followed by this iterator.
     */
    DefaultDirectFloatIterator(final Raster raster) {
        super(raster);
        final DataBuffer databuf = raster.getDataBuffer();
        assert (databuf.getDataType() == DataBuffer.TYPE_FLOAT) : "raster datas or not Byte type"+databuf;
        this.currentRaster = raster;
        this.currentDataArray = ((DataBufferFloat)databuf).getBankData();
    }

    /**
     * Create default Byte type rendered image iterator.
     *
     * @param renderedImage image which will be follow by iterator.
     */
    DefaultDirectFloatIterator(final RenderedImage renderedImage) {
        super(renderedImage);
        assert (renderedImage.getTile(tMinX, tMinY).getDataBuffer().getDataType() == DataBuffer.TYPE_FLOAT)
               : "renderedImage datas or not Byte type";
    }

    /**
     * Create Byte type raster iterator to follow from minX, minY raster and rectangle intersection coordinate.
     *
     * @param raster will be followed by this iterator.
     * @param subArea {@code Rectangle} which define read iterator area.
     * @throws IllegalArgumentException if subArea don't intersect raster boundary.
     */
    DefaultDirectFloatIterator(final Raster raster, final Rectangle subArea) {
        super(raster, subArea);
        final DataBuffer databuf = raster.getDataBuffer();
        assert (databuf.getDataType() == DataBuffer.TYPE_FLOAT) : "raster data or not Byte type"+databuf;
        this.currentDataArray = ((DataBufferFloat)databuf).getBankData();
    }

    /**
     * Create Byte type default rendered image iterator.
     *
     * @param renderedImage image which will be follow by iterator.
     * @param subArea {@code Rectangle} which represent image sub area iteration.
     * @throws IllegalArgumentException if subArea don't intersect image boundary.
     */
    DefaultDirectFloatIterator(final RenderedImage renderedImage, final Rectangle subArea) {
        super(renderedImage, subArea);
        assert (renderedImage.getTile(tMinX, tMinY).getDataBuffer().getDataType() == DataBuffer.TYPE_FLOAT)
               : "renderedImage datas or not Byte type";
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    protected void updateCurrentRaster(final int tileX, final int tileY){
        super.updateCurrentRaster(tileX, tileY);
        this.currentDataArray = ((DataBufferFloat)currentRaster.getDataBuffer()).getBankData();
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public int getSample() {
        return (int) currentDataArray[band][dataCursor];
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public float getSampleFloat() {
        return currentDataArray[band][dataCursor];
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public double getSampleDouble() {
        return currentDataArray[band][dataCursor];
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void setSample(int value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void setSampleFloat(float value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void setSampleDouble(double value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
