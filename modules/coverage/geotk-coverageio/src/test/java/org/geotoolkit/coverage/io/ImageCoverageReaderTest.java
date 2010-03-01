/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2010, Geomatys
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
package org.geotoolkit.coverage.io;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

import org.opengis.geometry.Envelope;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.metadata.spatial.PixelOrientation;

import org.junit.*;
import static org.junit.Assert.*;

import org.geotoolkit.test.Depend;
import org.geotoolkit.test.TestData;
import org.geotoolkit.geometry.Envelope2D;
import org.geotoolkit.coverage.grid.GridGeometry2D;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.image.io.plugin.TextMatrixImageReader;
import org.geotoolkit.image.io.plugin.TextMatrixImageReaderTest;
import org.geotoolkit.image.io.plugin.WorldFileImageReader;
import org.geotoolkit.image.io.plugin.WorldFileImageReaderTest;


/**
 * Tests {@link ImageCoverageReader}. This test will read the {@code "matrix.txt"} file
 * defined in the {@code org/geotoolkit/image/io/plugin/test-data} directory because it
 * is the easiest one to debug.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.09
 *
 * @since 3.09
 */
@Depend({TextMatrixImageReaderTest.class, WorldFileImageReaderTest.class})
public final class ImageCoverageReaderTest {
    /**
     * Small number for comparison of floating point values.
     */
    private static final float EPS = 1E-9f;

    /**
     * Register a "matrix" reader forced to the US format.
     */
    @BeforeClass
    public static void registerReaderUS() {
        final IIORegistry registry = IIORegistry.getDefaultInstance();
        final ImageReaderSpi spi = new TextMatrixImageReaderTest.Spi();
        final ImageReaderSpi old = registry.getServiceProviderByClass(TextMatrixImageReader.Spi.class);
        assertTrue(registry.registerServiceProvider(spi, ImageReaderSpi.class));
        assertTrue(registry.setOrdering(ImageReaderSpi.class, spi, old));
        WorldFileImageReader.Spi.registerDefaults(registry);
    }

    /**
     * Deregister the reader defined by {@link #registerReaderUS()}.
     */
    @AfterClass
    public static void deregisterReaderUS() {
        final IIORegistry registry = IIORegistry.getDefaultInstance();
        final ImageReaderSpi spi= registry.getServiceProviderByClass(TextMatrixImageReaderTest.Spi.class);
        assertTrue(registry.deregisterServiceProvider(spi, ImageReaderSpi.class));
        WorldFileImageReader.Spi.unregisterDefaults(registry);
    }

    /**
     * Reads the full image.
     *
     * @throws IOException If the text file can not be open (should not happen).
     * @throws CoverageStoreException Should not happen.
     */
    @Test
    public void readFull() throws IOException, CoverageStoreException {
        final ImageCoverageReader reader = new ImageCoverageReader();
        reader.setInput(TestData.file(TextMatrixImageReaderTest.class, "matrix.txt"));
        assertEquals(WorldFileImageReader.class, reader.imageReader.getClass());
        /*
         * Check the grid geometry before to attempt to read the image.
         */
        final GridGeometry2D gridGeometry = reader.getGridGeometry(0);
        final GridEnvelope gridEnvelope = gridGeometry.getGridRange();
        assertEquals("Grid dimension", 2, gridEnvelope.getDimension());
        assertEquals("Image columns",  0, gridEnvelope.getLow(0));
        assertEquals("Image rows",     0, gridEnvelope.getLow(1));
        assertEquals("Image columns", 19, gridEnvelope.getHigh(0)); // Inclusive
        assertEquals("Image rows",    41, gridEnvelope.getHigh(1)); // Inclusive
        assertTrue("Image bounds", new Rectangle(20,42).equals(gridGeometry.getGridRange2D()));
        assertTrue("Grid to CRS (Java2D)", new AffineTransform(1000, 0, 0, -1000, -10000, 21000)
                .equals(gridGeometry.getGridToCRS(PixelOrientation.UPPER_LEFT)));
        assertTrue("Grid to CRS (OGC)", new AffineTransform(1000, 0, 0, -1000, -9500, 20500)
                .equals(gridGeometry.getGridToCRS())); // Equivalent to PixelOrientation.CENTER
        /*
         * Read the image and check again its grid geometry, this time directly on the
         * rendered image. The grid geometry should be equivalent to the one checked above.
         */
        final GridCoverage2D gridCoverage = reader.read(0, null);
        final RenderedImage image = gridCoverage.getRenderedImage();
        assertEquals("Image columns",  0, image.getMinX());
        assertEquals("Image rows",     0, image.getMinY());
        assertEquals("Image columns", 20, image.getWidth());
        assertEquals("Image rows",    42, image.getHeight());
        assertEquals("Grid geometry", gridGeometry, gridCoverage.getGridGeometry());
        /*
         * Check the envelope, which should be the envelope of the full coverage.
         */
        final Envelope envelope = gridCoverage.getEnvelope();
        assertEquals("Envelope X", -10000, envelope.getMinimum(0), EPS);
        assertEquals("Envelope Y", -21000, envelope.getMinimum(1), EPS);
        assertEquals("Envelope X",  10000, envelope.getMaximum(0), EPS);
        assertEquals("Envelope Y",  21000, envelope.getMaximum(1), EPS);
        /*
         * Check a few values. This is a convenient way to ensure that
         * the right portion of the image has been read.
         */
        final Raster raster = image.getData();
        assertEquals(-1.123f,   raster.getSampleFloat( 0,  0, 0), EPS);
        assertEquals(-1.251f,   raster.getSampleFloat(19,  0, 0), EPS);
        assertEquals(-1.075f,   raster.getSampleFloat( 0, 41, 0), EPS);
        assertEquals(Float.NaN, raster.getSampleFloat(19, 41, 0), EPS);
    }

    /**
     * Reads a region of the image.
     *
     * @throws IOException If the text file can not be open (should not happen).
     * @throws CoverageStoreException Should not happen.
     */
    @Test
    public void readRegion() throws IOException, CoverageStoreException {
        final ImageCoverageReader reader = new ImageCoverageReader();
        reader.setInput(TestData.file(TextMatrixImageReaderTest.class, "matrix.txt"));
        assertEquals(WorldFileImageReader.class, reader.imageReader.getClass());
        /*
         * Read the image and check its grid geometry directly on the rendered image.
         */
        final GridCoverageReadParam param = new GridCoverageReadParam();
        param.setEnvelope(new Envelope2D(null, -1000, -2000, 8000 - -1000, 12000 - -2000));
        final GridCoverage2D gridCoverage = reader.read(0, param);
        final RenderedImage image = gridCoverage.getRenderedImage();
        assertEquals("Image columns",  0, image.getMinX());
        assertEquals("Image rows",     0, image.getMinY());
        assertEquals("Image columns",  9, image.getWidth());
        assertEquals("Image rows",    14, image.getHeight());
        /*
         * Check the envelope, which should be the same than the requested one
         * (in this particular test case, since the reader does not have to clip
         * the envelope or snap to grid coordinates).
         */
        final Envelope envelope = gridCoverage.getEnvelope();
        assertEquals("Envelope X", -1000, envelope.getMinimum(0), EPS);
        assertEquals("Envelope Y", -2000, envelope.getMinimum(1), EPS);
        assertEquals("Envelope X",  8000, envelope.getMaximum(0), EPS);
        assertEquals("Envelope Y", 12000, envelope.getMaximum(1), EPS);
        /*
         * Check a few values. This is a convenient way to ensure that
         * the right portion of the image has been read.
         */
        final Raster raster = image.getData();
        assertEquals(12.783f,   raster.getSampleFloat(0,  0, 0), EPS);
        assertEquals(Float.NaN, raster.getSampleFloat(8,  0, 0), EPS);
        assertEquals(26.769f,   raster.getSampleFloat(0, 13, 0), EPS);
        assertEquals(27.949f,   raster.getSampleFloat(8, 13, 0), EPS);
    }

    /**
     * Reads the same region than above, with a subsampling.
     *
     * @throws IOException If the text file can not be open (should not happen).
     * @throws CoverageStoreException Should not happen.
     */
    @Test
    public void readSubsampledRegion() throws IOException, CoverageStoreException {
        final ImageCoverageReader reader = new ImageCoverageReader();
        reader.setInput(TestData.file(TextMatrixImageReaderTest.class, "matrix.txt"));
        assertEquals(WorldFileImageReader.class, reader.imageReader.getClass());
        /*
         * Read the image and check its grid geometry directly on the rendered image.
         */
        final GridCoverageReadParam param = new GridCoverageReadParam();
        param.setEnvelope(new Envelope2D(null, -1000, -2000, 8000 - -1000, 12000 - -2000));
        param.setResolution(2000, 3000);
        final GridCoverage2D gridCoverage = reader.read(0, param);
        final RenderedImage image = gridCoverage.getRenderedImage();
        assertEquals("Image columns", 0, image.getMinX());
        assertEquals("Image rows",    0, image.getMinY());
        assertEquals("Image columns", 5, image.getWidth());
        assertEquals("Image rows",    5, image.getHeight());
        /*
         * Check the envelope, which should be sightly bigger than the requested one in
         * order to contains fully the requested envelope, taking subsampling on account.
         */
        final Envelope envelope = gridCoverage.getEnvelope();
        assertEquals("Envelope X", -1000, envelope.getMinimum(0), EPS);
        assertEquals("Envelope Y", -3000, envelope.getMinimum(1), EPS);
        assertEquals("Envelope X",  9000, envelope.getMaximum(0), EPS);
        assertEquals("Envelope Y", 12000, envelope.getMaximum(1), EPS);
        /*
         * Check a few values. This is a convenient way to ensure that
         * the right portion of the image has been read.
         */
        final Raster raster = image.getData();
        assertEquals(12.783f,   raster.getSampleFloat(0, 0, 0), EPS);
        assertEquals(Float.NaN, raster.getSampleFloat(4, 0, 0), EPS);
        assertEquals(25.454f,   raster.getSampleFloat(0, 4, 0), EPS);
        assertEquals(28.902f,   raster.getSampleFloat(4, 4, 0), EPS);
    }
}
