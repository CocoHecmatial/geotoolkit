/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009, Geomatys
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
package org.geotoolkit.image.io.text;

import java.util.Locale;
import java.util.Iterator;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.IIOException;

import org.geotoolkit.test.TestData;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.geotoolkit.image.io.metadata.SpatialMetadataFormat;

import org.junit.*;
import static org.junit.Assert.*;
import static org.geotoolkit.test.Commons.*;


/**
 * Tests {@link TextRecordImageReader}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.07
 *
 * @since 3.06
 */
public final class TextRecordImageReaderTest extends TextImageReaderTestBase {
    /**
     * Creates a reader using the {@link Locale#CANADA}.
     */
    @Override
    protected TextRecordImageReader createImageReader() throws IOException {
        return createImageReader(true);
    }

    /**
     * Creates a reader using the {@link Locale#CANADA}. If {@code lenient} is {@code true},
     * uses a relaxed threshold value for determining if the grid is regular.
     */
    private TextRecordImageReader createImageReader(final boolean lenient) throws IOException {
        final TextRecordImageReader reader = new TextRecordImageReader(createImageReaderSpi(lenient));
        reader.setInput(TestData.file(this, "records.txt"));
        return reader;
    }

    /**
     * Creates a provider using the {@link Locale#CANADA}. If {@code lenient} is {@code true},
     * uses a relaxed threshold value for determining if the grid is regular.
     */
    private TextRecordImageReader.Spi createImageReaderSpi(final boolean lenient) throws IOException {
        TextRecordImageReader.Spi spi = new TextRecordImageReader.Spi();
        spi.padValue = -9999;
        spi.locale   = Locale.CANADA;
        spi.charset  = Charset.forName("UTF-8");
        if (lenient) {
            spi.gridTolerance = 0.002f;
        }
        return spi;
    }

    /**
     * Ensures that {@link TextImageReader.Spi#canDecodeInput(Object)}
     * correctly detect that it can not parse the {@code "matrix.txt"} file.
     *
     * @throws IOException if an error occured while reading the file.
     */
    @Test
    public void testCanNotRead() throws IOException {
        final TextRecordImageReader.Spi spi = createImageReaderSpi(true);
        assertFalse(spi.canDecodeInput(TestData.file(this, "matrix.txt")));
    }

    /**
     * Ensure that the image reader can detect that the grid is not regular.
     * We rely on the fact that the interval between latitude values in the
     * {@code "records"} file is not supposed to be an integer, while the
     * values are formatted as integers.
     *
     * @throws IOException if an error occured while reading the file.
     */
    @Test
    public void testGridRegularity() throws IOException {
        final TextImageReader reader = createImageReader(false);
        try {
            assertNotNull(reader.read(0));
            fail("The grid should have been considered irregular.");
        } catch (IIOException e) {
            // This is the expected exception.
        }
    }

    /**
     * Tests the metadata of the {@link "records.txt"} file.
     *
     * @throws IOException if an error occured while reading the file.
     */
    @Test
    public void testMetadata() throws IOException {
        final TextImageReader reader = createImageReader();
        assertEquals(20, reader.getWidth (0));
        assertEquals(42, reader.getHeight(0));
        assertNull(reader.getStreamMetadata());
        final SpatialMetadata metadata = reader.getImageMetadata(0);
        assertNotNull(metadata);
        assertMultilinesEquals(decodeQuotes(
            SpatialMetadataFormat.FORMAT_NAME + '\n' +
            "├───RectifiedGridDomain\n" +
            "│   ├───origin=“-19000.0 12690.0”\n" +
            "│   ├───OffsetVectors\n" +
            "│   │   ├───OffsetVector\n" +
            "│   │   │   └───values=“2000.0 0.0”\n" +
            "│   │   └───OffsetVector\n" +
            "│   │       └───values=“0.0 -619.0243902439024”\n" +
            "│   └───Limits\n" +
            "│       ├───low=“0 0”\n" +
            "│       └───high=“19 41”\n" +
            "├───SpatialRepresentation\n" +
            "│   ├───numberOfDimensions=“2”\n" +
            "│   ├───centerPoint=“0.0 0.0”\n" +
            "│   └───pointInPixel=“center”\n" +
            "└───ImageDescription\n" +
            "    └───Dimensions\n" +
            "        └───Dimension\n" +
            "            ├───minValue=“-1.893”\n" +
            "            ├───maxValue=“31.14”\n" +
            "            └───fillSampleValues=“-9999.0”\n"), metadata.toString());
    }

    /**
     * Tests the registration of the image reader in the Image I/O framework.
     */
    @Test
    public void testRegistrationByFormatName() {
        Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("records");
        assertTrue("Expected a reader.", it.hasNext());
        assertTrue(it.next() instanceof TextRecordImageReader);
        assertFalse("Expected no more reader.", it.hasNext());
    }

    /**
     * Tests the registration by MIME type.
     * Note that more than one writer may be registered.
     */
    @Test
    public void testRegistrationByMIMEType() {
        Iterator<ImageReader> it = ImageIO.getImageReadersByMIMEType("text/plain");
        while (it.hasNext()) {
            if (it.next() instanceof TextRecordImageReader) {
                return;
            }
        }
        fail("Reader not found.");
    }
}
