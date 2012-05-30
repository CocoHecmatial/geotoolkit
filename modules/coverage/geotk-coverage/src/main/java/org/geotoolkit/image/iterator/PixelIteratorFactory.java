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
import java.awt.image.*;

/**
 * Create an appropriate iterator.
 *
 * @author Rémi Marechal (Geomatys).
 */
public final class PixelIteratorFactory {

    private PixelIteratorFactory() {
    }

    /**
     * Create and return an adapted default raster iterator.
     *
     * @param raster   {@link Raster} will be traveled by iterator.
     * @return adapted {@link PixelIterator}.
     */
    public static PixelIterator createDefaultIterator(final Raster raster) {
        switch (raster.getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new DefaultDirectByteIterator(raster);
            case DataBuffer.TYPE_FLOAT : return new DefaultDirectFloatIterator(raster);
            default : return new DefaultIterator(raster);
        }
    }

    /**
     * Create and return an adapted default read-only raster iterator to read on raster sub-area.
     *
     * @param raster      {@link Raster} will be traveled by iterator from it's sub-area.
     * @param subReadArea {@link Rectangle} which define raster read area.
     * @return adapted    {@link PixelIterator}.
     */
    public static PixelIterator createDefaultIterator(final Raster raster, final Rectangle subReadArea) {
        switch (raster.getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new DefaultDirectByteIterator(raster, subReadArea);
            case DataBuffer.TYPE_FLOAT : return new DefaultDirectFloatIterator(raster, subReadArea);
            default : return new DefaultIterator(raster, subReadArea);
        }
    }

    /**
     * Create and return an adapted default read-only rendered image iterator.
     *
     * @param renderedImage {@link RenderedImage} will be traveled by iterator.
     * @return adapted      {@link PixelIterator}.
     */
    public static PixelIterator createDefaultIterator(final RenderedImage renderedImage) {
        switch (renderedImage.getTile(renderedImage.getMinTileX(), renderedImage.getMinTileY()).getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new DefaultDirectByteIterator(renderedImage);
            case DataBuffer.TYPE_FLOAT : return new DefaultDirectFloatIterator(renderedImage);
            default : return new DefaultIterator(renderedImage);
        }
    }

    /**
     * Create and return an adapted default read-only raster iterator to read on raster sub-area.
     *
     * @param renderedImage {@link RenderedImage} will be traveled by iterator from it's sub-area.
     * @param subReadArea   {@link Rectangle} which define rendered image read area.
     * @return adapted      {@link PixelIterator}.
     */
    public static PixelIterator createDefaultIterator(final RenderedImage renderedImage, final Rectangle subReadArea) {
        switch (renderedImage.getTile(renderedImage.getMinTileX(), renderedImage.getMinTileY()).getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new DefaultDirectByteIterator(renderedImage, subReadArea);
            case DataBuffer.TYPE_FLOAT : return new DefaultDirectFloatIterator(renderedImage, subReadArea);
            default : return new DefaultIterator(renderedImage, subReadArea);
        }
    }

    /**
     * Create and return an adapted default read and write raster iterator.
     *
     * @param raster          {@link Raster} will be traveled by read-only iterator.
     * @param writeableRaster {@link WritableRaster} raster wherein value is set (write).
     * @return adapted        {@link PixelIterator} .
     */
    public static PixelIterator createDefaultWriteableIterator(final Raster raster, final WritableRaster writeableRaster) {
        switch (raster.getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new DefaultWritableDirectByteIterator(raster, writeableRaster);
            case DataBuffer.TYPE_FLOAT : return new DefaultWritableDirectFloatIterator(raster, writeableRaster);
            default : return new DefaultWritableIterator(raster, writeableRaster);
        }
    }

    /**
     * Create and return an adapted default read and write raster iterator.
     * Iterator move in a raster sub-area.
     *
     * @param raster          {@link Raster} will be traveled by read-only iterator.
     * @param writeableRaster {@link WritableRaster} raster wherein value is set (write).
     * @param subArea     {@link Rectangle} which define raster read and write area.
     * @return adapted        {@link PixelIterator}.
     */
    public static PixelIterator createDefaultWriteableIterator(final Raster raster, final WritableRaster writeableRaster, final Rectangle subArea) {
        switch (raster.getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new DefaultWritableDirectByteIterator(raster, writeableRaster, subArea);
            case DataBuffer.TYPE_FLOAT : return new DefaultWritableDirectFloatIterator(raster, writeableRaster, subArea);
            default : return new DefaultWritableIterator(raster, writeableRaster, subArea);
        }
    }

    /**
     * Create and return an adapted default read and write rendered image iterator.
     *
     * @param renderedImage         {@link RenderedImage} will be traveled by iterator.
     * @param writableRenderedImage {@link WritableRenderedImage} rendered image wherein value is set (write).
     * @return adapted              {@link PixelIterator}.
     */
    public static PixelIterator createDefaultWriteableIterator(final RenderedImage renderedImage, final WritableRenderedImage writableRenderedImage) {
        switch (renderedImage.getTile(renderedImage.getMinTileX(), renderedImage.getMinTileY()).getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new DefaultWritableDirectByteIterator(renderedImage, writableRenderedImage);
            case DataBuffer.TYPE_FLOAT : return new DefaultWritableDirectFloatIterator(renderedImage, writableRenderedImage);
            default : return new DefaultWritableIterator(renderedImage, writableRenderedImage);
        }
    }

    /**
     * Create and return an adapted default read and write rendered image iterator from it's sub-area.
     *
     * @param renderedImage         {@link RenderedImage} will be traveled by iterator from it's sub-area.
     * @param writableRenderedImage {@link WritableRenderedImage} rendered image wherein value is set (write).
     * @param subArea               {@link Rectangle} which define rendered image read and write area.
     * @return adapted              {@link PixelIterator}.
     */
    public static PixelIterator createDefaultWriteableIterator(final RenderedImage renderedImage, final WritableRenderedImage writableRenderedImage, final Rectangle subArea) {
        switch (renderedImage.getTile(renderedImage.getMinTileX(), renderedImage.getMinTileY()).getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new DefaultWritableDirectByteIterator(renderedImage, writableRenderedImage, subArea);
            case DataBuffer.TYPE_FLOAT : return new DefaultWritableDirectFloatIterator(renderedImage, writableRenderedImage, subArea);
            default : return new DefaultWritableIterator(renderedImage, writableRenderedImage, subArea);
        }
    }


    ////////////////////////////// Row Major Iterator ////////////////////////////

    /**
     * Create and return an adapted Row Major read-only rendered image iterator.
     * RowMajor : iterator move forward line per line one by one in downward order.
     *
     * @param renderedImage {@link RenderedImage} will be traveled by iterator.
     * @return adapted      {@link PixelIterator}.
     */
    public static PixelIterator createRowMajorIterator(final RenderedImage renderedImage) {
        switch (renderedImage.getTile(renderedImage.getMinTileX(), renderedImage.getMinTileY()).getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new RowMajorDirectByteIterator(renderedImage);
            case DataBuffer.TYPE_FLOAT : return new RowMajorDirectFloatIterator(renderedImage);
            default : return new RowMajorIterator(renderedImage);
        }
    }

    /**
     * Create and return an adapted Row Major read-only rendered image iterator from it's sub-area.
     * RowMajor : iterator move forward line per line one by one in downward order.
     *
     * @param renderedImage {@link RenderedImage} will be traveled by iterator from it's sub-area.
     * @param subReadArea   {@link Rectangle} which define rendered image read-only area.
     * @return adapted      {@link PixelIterator}.
     */
    public static PixelIterator createRowMajorIterator(final RenderedImage renderedImage, final Rectangle subReadArea) {
        switch (renderedImage.getTile(renderedImage.getMinTileX(), renderedImage.getMinTileY()).getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new RowMajorDirectByteIterator(renderedImage, subReadArea);
            case DataBuffer.TYPE_FLOAT : return new RowMajorDirectFloatIterator(renderedImage, subReadArea);
            default : return new RowMajorIterator(renderedImage, subReadArea);
        }
    }

    /**
     * Create and return an adapted Row Major read and write rendered image iterator.
     * RowMajor : iterator move forward line per line one by one in downward order.
     *
     * @param renderedImage         {@link RenderedImage} will be traveled by iterator.
     * @param writableRenderedImage {@link WritableRenderedImage}  rendered image wherein value is set (write).
     * @return adapted              {@link PixelIterator}.
     */
    public static PixelIterator createRowMajorWriteableIterator(final RenderedImage renderedImage, final WritableRenderedImage writableRenderedImage) {
        switch (renderedImage.getTile(renderedImage.getMinTileX(), renderedImage.getMinTileY()).getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new RowMajorWritableDirectByteIterator(renderedImage, writableRenderedImage);
            case DataBuffer.TYPE_FLOAT : return new RowMajorWritableDirectFloatIterator(renderedImage, writableRenderedImage);
            default : return new RowMajorWritableIterator(renderedImage, writableRenderedImage);
        }
    }

    /**
     * Create and return an adapted Row Major read and write rendered image iterator from it's sub-area.
     * RowMajor : iterator move forward line per line one by one in downward order.
     *
     * @param renderedImage         {@link RenderedImage} will be traveled by iterator from it's sub-area.
     * @param writableRenderedImage {@link WritableRenderedImage}  rendered image wherein value is set (write).
     * @param subArea               {@link Rectangle} which define rendered image read and write area.
     * @return adapted              {@link PixelIterator}.
     */
    public static PixelIterator createRowMajorWriteableIterator(final RenderedImage renderedImage, final WritableRenderedImage writableRenderedImage, final Rectangle subArea) {
        switch (renderedImage.getTile(renderedImage.getMinTileX(), renderedImage.getMinTileY()).getDataBuffer().getDataType()) {
            case DataBuffer.TYPE_BYTE  : return new RowMajorWritableDirectByteIterator(renderedImage, writableRenderedImage, subArea);
            case DataBuffer.TYPE_FLOAT : return new RowMajorWritableDirectFloatIterator(renderedImage, writableRenderedImage, subArea);
            default : return new RowMajorWritableIterator(renderedImage, writableRenderedImage, subArea);
        }
    }
}
