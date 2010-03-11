/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007-2010, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.image.io.plugin;

import java.util.Locale;
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.RenderedImage;

import org.geotoolkit.test.TestData;
import org.geotoolkit.internal.io.IOUtilities;
import org.geotoolkit.internal.io.TemporaryFile;
import org.geotoolkit.image.io.TextImageWriterTestBase;
import org.geotoolkit.image.io.mosaic.TileTest;
import org.geotoolkit.image.io.XImageIO;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.geotoolkit.test.Commons.*;


/**
 * Tests {@link WorldFileImageWriter}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.10
 *
 * @since 3.07
 */
public final class WorldFileImageWriterTest extends TextImageWriterTestBase {
    /**
     * The previous locale before the test is run.
     * This is usually the default locale.
     */
    private Locale defaultLocale;

    /**
     * Sets the locale to a compile-time value. We need to use a fixed value because the
     * name of the coordinate system is locale-sensitive in this test.
     */
    @Before
    public void fixLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE);
    }

    /**
     * Restores the locales to its original value.
     */
    @After
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    /**
     * Creates a writer.
     */
    @Override
    protected WorldFileImageWriter createImageWriter() throws IOException {
        final WorldFileImageWriter.Spi spi = new WorldFileImageWriter.Spi(new TextMatrixImageWriter.Spi());
        final WorldFileImageWriter writer = new WorldFileImageWriter(spi);
        return writer;
    }

    /**
     * Returns a new file with the same path than the given file but a different extension,
     * and ensure that this file does not exist. If a file with that name exists, the test
     * will be stopped but is will not be considered a failure.
     */
    private static File changeExtension(final File mainFile, final String extension) throws IOException {
        final File file = (File) IOUtilities.changeExtension(mainFile, extension);
        assumeTrue(file.createNewFile());
        return file;
    }

    /**
     * Tests the write operation.
     *
     * @throws IOException If an error occured while writing to the temporary file.
     */
    @Test
    public void testWrite() throws IOException {
        final IIOImage image = createImage(true);
        clearUserObjects(image.getMetadata());
        final WorldFileImageWriter writer = createImageWriter();
        final File file = TemporaryFile.createTempFile("TEST", ".txt", null);
        File fileTFW = null, filePRJ = null;
        try {
            fileTFW = changeExtension(file, "ttw");
            filePRJ = changeExtension(file, "prj");
            writer.setOutput(file);
            writer.write(image);
            assertTrue("The main file should contains data.", file   .length() > 0);
            assertTrue("The TFW file should contains data.",  fileTFW.length() > 0);
            assertTrue("The PRJ file should contains data.",  filePRJ.length() > 0);
            assertMultilinesEquals(
                "100.0\n" +
                "0.0\n" +
                "0.0\n" +
                "-100.0\n" +
                "-500.0\n" +
                "400.0\n", TestData.readLatinText(fileTFW));
            /*
             * In the test for CRS, we will insert some new lines in the WKT
             * for making easier the debugging in case of comparison failure.
             */
            String wkt = TestData.readLatinText(filePRJ);
            wkt = wkt.replace("], ", "],\n");
            assertMultilinesEquals(decodeQuotes(
                "PROJCS[“WGS 84 / World Mercator”, " +
                "GEOGCS[“Sans-titre”, DATUM[“World Geodetic System 1984”, " +
                "SPHEROID[“WGS 84”, 6378137.0, 298.257223563]],\n" +
                "PRIMEM[“Greenwich”, 0.0],\n" +
                "UNIT[“degree”, 0.017453292519943295],\n" +
                "AXIS[“Geodetic longitude”, EAST],\n" +
                "AXIS[“Geodetic latitude”, NORTH]],\n" +
                "PROJECTION[“Mercator_1SP”, AUTHORITY[“EPSG”,“9804”]],\n" +
                "PARAMETER[“latitude_of_origin”, 0.0],\n" +
                "PARAMETER[“central_meridian”, 0.0],\n" +
                "PARAMETER[“scale_factor”, 1.0],\n" +
                "PARAMETER[“false_easting”, 0.0],\n" +
                "PARAMETER[“false_northing”, 0.0],\n" +
                "UNIT[“metre”, 1.0],\n" +
                "AXIS[“Easting”, EAST],\n" +
                "AXIS[“Northing”, NORTH]]"), wkt);
        } finally {
            assertTrue(TemporaryFile.delete(file));
            if (fileTFW != null) {
                assertTrue(fileTFW.delete());
            }
            if (filePRJ != null) {
                assertTrue(filePRJ.delete());
            }
        }
        writer.dispose();
    }

    /**
     * Tests writing an image though the standard {@link ImageIO} API and
     * the {@link XImageIO} extension.
     *
     * @throws IOException If an error occured while reading the test image or
     *         writing it to the temporary file.
     *
     * @since 3.10
     */
    @Test
    public void testImageIO() throws IOException {
        final RenderedImage image = ImageIO.read(TestData.file(TileTest.class, "A1.png"));
        final File file = TemporaryFile.createTempFile("TEST", ".png", null);
        WorldFileImageWriter.Spi.registerDefaults(null);
        try {
            assertTrue("Should use the standard image writer.", ImageIO.write(image, "png", file));
            assertTrue(file.length() != 0);
            /*
             * When using the XImageIO methods, the WorldFileImageWriter plugin
             * should be selected in the input is a file.
             */
            ImageWriter writer = XImageIO.getWriterBySuffix(file, image);
            assertTrue(writer instanceof WorldFileImageWriter);
            writer.write(image);
            writer.dispose();
            assertTrue(file.length() != 0);
            /*
             * If the input is a stream, then the standard writer should be selected.
             */
            final ImageOutputStream out = ImageIO.createImageOutputStream(file);
            try {
                writer = XImageIO.getWriterByFormatName("png", out, image);
                assertFalse(writer instanceof WorldFileImageWriter);
                // Don't botter to write the image. The purpose of
                // this test is not to test the JDK PNG ImageWriter.
                writer.dispose();
            } finally {
                out.close();
            }
        } finally {
            WorldFileImageWriter.Spi.unregisterDefaults(null);
            assertTrue(TemporaryFile.delete(file));
        }
    }
}
