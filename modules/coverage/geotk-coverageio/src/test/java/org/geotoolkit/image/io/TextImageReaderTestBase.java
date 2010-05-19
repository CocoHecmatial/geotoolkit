/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2010, Geomatys
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
package org.geotoolkit.image.io;

import java.io.IOException;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import javax.imageio.ImageTypeSpecifier;

import org.junit.*;
import static java.lang.Float.NaN;
import static org.junit.Assert.*;


/**
 * The base class for {@link TextImageReader} tests.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.12
 *
 * @since 3.06
 */
public abstract class TextImageReaderTestBase extends ImageReaderTestBase {
    /**
     * The precision for comparison of sample values. The values in the test files provided
     * in this package have 3 significant digits, so the precision is set to the next digit.
     */
    protected static final float EPS = 0.0001f;

    /**
     * Creates the image reader initialized to the input to read. The input
     * content shall be the same than the {@code "test-data/matrix.txt"} file.
     *
     * @return The reader to test.
     * @throws IOException If an error occured while creating the format.
     */
    @Override
    protected abstract SpatialImageReader createImageReader() throws IOException;

    /**
     * Tests the {@link TextImageReader.Spi#canDecodeInput(Object)}.
     *
     * @throws IOException if an error occured while reading the file.
     */
    @Test
    public void testCanRead() throws IOException {
        final SpatialImageReader reader = createImageReader();
        assertTrue(reader.getOriginatingProvider().canDecodeInput(reader.getInput()));
        /*
         * Ensure that the above check did not caused the lost of data.
         */
        final BufferedImage image = reader.read(0);
        reader.dispose();
        assertEquals(20, image.getWidth());
        assertEquals(42, image.getHeight());
        final Raster raster = image.getRaster();
        assertEquals(-1.123f, raster.getSampleFloat(0, 0, 0), EPS);
    }

    /**
     * Tests the reading of the {@link "matrix.txt"} file.
     *
     * @throws IOException if an error occured while reading the file.
     */
    @Test
    public void testReadFile() throws IOException {
        final SpatialImageReader reader = createImageReader();
        final SpatialImageReadParam param = reader.getDefaultReadParam();
        param.setSampleConversionAllowed(SampleConversionType.REPLACE_FILL_VALUES, true);
        final BufferedImage image = reader.read(0, param);
        reader.dispose();
        assertEquals(20, image.getWidth());
        assertEquals(42, image.getHeight());
        assertEquals(DataBuffer.TYPE_FLOAT, image.getSampleModel().getDataType());

        final ColorSpace cs = image.getColorModel().getColorSpace();
        assertEquals(1, cs.getNumComponents());
        assertEquals(-1.893, cs.getMinValue(0), EPS);
        assertEquals(31.140, cs.getMaxValue(0), EPS);

        final Raster raster = image.getRaster();
        assertEquals(-1.123f, raster.getSampleFloat( 0,  0, 0), EPS);
        assertEquals( 0.273f, raster.getSampleFloat( 2,  3, 0), EPS);
        assertEquals(   NaN , raster.getSampleFloat( 3,  4, 0), EPS);
        assertEquals(-1.075f, raster.getSampleFloat( 0, 41, 0), EPS);
        assertEquals(   NaN , raster.getSampleFloat(19,  4, 0), EPS);
    }

    /**
     * Tests the reading of a sub-region.
     *
     * @throws IOException if an error occured while reading the file.
     */
    @Test
    public void testSubRegion() throws IOException {
        final SpatialImageReader reader = createImageReader();
        final SpatialImageReadParam param = reader.getDefaultReadParam();
        param.setSampleConversionAllowed(SampleConversionType.REPLACE_FILL_VALUES, true);
        param.setSourceRegion(new Rectangle(5, 10, 10, 20));
        param.setSourceSubsampling(2, 3, 1, 2);
        final BufferedImage image = reader.read(0, param);
        reader.dispose();

        final Raster raster = image.getRaster();
        assertEquals(  NaN , raster.getSampleFloat(0, 0, 0), EPS);
        assertEquals(16.470, raster.getSampleFloat(1, 0, 0), EPS);
        assertEquals(22.161, raster.getSampleFloat(1, 1, 0), EPS);
        assertEquals(27.619, raster.getSampleFloat(1, 3, 0), EPS);
        assertEquals(29.347, raster.getSampleFloat(4, 3, 0), EPS);
    }

    /**
     * Tests the reading with conversion to the {@link DataBuffer#TYPE_FLOAT}.
     * The floating point numbers will be casted to integer types.
     *
     * @throws IOException if an error occured while reading the file.
     */
    @Test
    public void testByteType() throws IOException {
        final SpatialImageReader reader = createImageReader();
        final SpatialImageReadParam param = reader.getDefaultReadParam();
        param.setSampleConversionAllowed(SampleConversionType.REPLACE_FILL_VALUES, true);
        final byte[] RGB = new byte[256];
        for (int i=0; i<RGB.length; i++) {
            RGB[i] = (byte) i;
        }
        param.setDestinationType(ImageTypeSpecifier.createIndexed(RGB, RGB, RGB, null, 8, DataBuffer.TYPE_BYTE));
        /*
         * Reads the image twice. 'image' is the reference that we want to test with byte values.
         * 'original' is the one with floating point value, to be used only for comparison.
         */
        final BufferedImage image = reader.read(0, param);
        param.setDestinationType(null);
        final BufferedImage original = reader.read(0, param);
        reader.dispose();
        assertEquals(IndexColorModel.class, image.getColorModel().getClass());
        assertEquals(DataBuffer.TYPE_BYTE,  image   .getSampleModel().getDataType());
        assertEquals(DataBuffer.TYPE_FLOAT, original.getSampleModel().getDataType());
        /*
         * Only the NaN values in the original data should be zero in the byte data.
         */
        final Raster byteRaster  = image.getRaster();
        final Raster floatRaster = original.getRaster();
        final int width  = floatRaster.getWidth();
        final int height = floatRaster.getHeight();
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                final int   byteValue  =  byteRaster.getSample     (x, y, 0);
                final float floatValue = floatRaster.getSampleFloat(x, y, 0);
                assertEquals("At (" + x + ',' + y + ") float=" + floatValue + " byte=" + byteValue,
                        Float.isNaN(floatValue), byteValue == 0);
            }
        }
        /*
         * Test the same pixel values than 'testReadFile()'.
         * We have an offset of 3, rounded toward zero.
         */
        assertEquals(1, byteRaster.getSample( 0,  0, 0));
        assertEquals(3, byteRaster.getSample( 2,  3, 0));
        assertEquals(0, byteRaster.getSample( 3,  4, 0));
        assertEquals(1, byteRaster.getSample( 0, 41, 0));
        assertEquals(0, byteRaster.getSample(19,  4, 0));
    }
}
