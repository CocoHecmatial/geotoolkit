/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2011, Geomatys
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.geotoolkit.util.NumberRange;
import org.geotoolkit.coverage.Category;
import org.geotoolkit.coverage.GridSampleDimension;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.io.ImageCoverageReader;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.geotoolkit.image.io.metadata.SpatialMetadataFormat;

import org.junit.*;
import static org.geotoolkit.test.Assert.*;
import static org.geotoolkit.test.Commons.*;


/**
 * Tests reading a format in which the data are already geophysics.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.19
 *
 * @since 3.19
 */
public final strictfp class GeophysicsFormatTest extends NetcdfTestBase {
    /**
     * Returns the file to test, which is optional. If the test file is not present,
     * the test will be interrupted by the JUnit {@link org.junit.Assume} class.
     *
     * @return The test file (never null).
     */
    public static File getTestFile() {
        return getLocallyInstalledFile("Norway/b_cf.nc");
    }

    /**
     * Creates a reader and initializes its input to the test file defined in
     * {@link #getTestFile()}. This method is invoked by each tests inherited
     * from the parent class, and by the tests defined in this class.
     */
    @Override
    protected NetcdfImageReader createImageReader() throws IOException {
        final NetcdfImageReader reader = new NetcdfImageReader(null);
        reader.setInput(getTestFile());
        return reader;
    }

    /**
     * Tests the metadata tree.
     *
     * @throws IOException if an error occurred while reading the file.
     * @throws CoverageStoreException Should never happen.
     */
    @Test
    public void testMetadata() throws IOException, CoverageStoreException {
        final NetcdfImageReader reader = createImageReader();
        final SpatialMetadata metadata = reader.getImageMetadata(0);
        final String asTree = metadata.toString();
        assertNotNull(metadata);
        assertMultilinesEquals(decodeQuotes(
            SpatialMetadataFormat.FORMAT_NAME + '\n' +
            "├───RectifiedGridDomain\n" +
            "│   ├───origin=“6.0 81.0 55.0 73.0”\n" +
            "│   ├───Limits\n" +
            "│   │   ├───low=“0 0 0 0”\n" +
            "│   │   └───high=“128 65 0 107”\n" +
            "│   ├───OffsetVectors\n" +
            "│   │   ├───OffsetVector\n" +
            "│   │   │   └───values=“0.5 0.0 0.0 0.0”\n" +
            "│   │   ├───OffsetVector\n" +
            "│   │   │   └───values=“0.0 -0.2 0.0 0.0”\n" +
            "│   │   ├───OffsetVector\n" +
            "│   │   │   └───values=“0.0 0.0 NaN 0.0”\n" +
            "│   │   └───OffsetVector\n" +
            "│   │       └───values=“0.0 0.0 0.0 NaN”\n" +
            "│   └───CoordinateReferenceSystem\n" +
            "│       ├───name=“NetCDF:time depth lat lon”\n" +
            "│       └───CoordinateSystem\n" +
            "│           ├───name=“NetCDF:time depth lat lon”\n" +
            "│           ├───dimension=“4”\n" +
            "│           └───Axes\n" +
            "│               ├───CoordinateSystemAxis\n" +
            "│               │   ├───name=“NetCDF:lon”\n" +
            "│               │   ├───axisAbbrev=“l”\n" +
            "│               │   ├───direction=“east”\n" +
            "│               │   ├───minimumValue=“6.0”\n" +
            "│               │   ├───maximumValue=“70.0”\n" +
            "│               │   └───unit=“deg”\n" +
            "│               ├───CoordinateSystemAxis\n" +
            "│               │   ├───name=“NetCDF:lat”\n" +
            "│               │   ├───axisAbbrev=“l”\n" +
            "│               │   ├───direction=“north”\n" +
            "│               │   ├───minimumValue=“68.0”\n" +
            "│               │   ├───maximumValue=“81.0”\n" +
            "│               │   └───unit=“deg”\n" +
            "│               ├───CoordinateSystemAxis\n" +
            "│               │   ├───name=“NetCDF:depth”\n" +
            "│               │   ├───axisAbbrev=“d”\n" +
            "│               │   ├───direction=“down”\n" +
            "│               │   ├───minimumValue=“55.0”\n" +
            "│               │   ├───maximumValue=“55.0”\n" +
            "│               │   └───unit=“m”\n" +
            "│               └───CoordinateSystemAxis\n" +
            "│                   ├───name=“NetCDF:time”\n" +
            "│                   ├───axisAbbrev=“t”\n" +
            "│                   ├───direction=“future”\n" +
            "│                   ├───minimumValue=“73.0”\n" +
            "│                   ├───maximumValue=“9845.0”\n" +
            "│                   └───unit=“d”\n" +
            "└───ImageDescription\n" +
            "    └───Dimensions\n" +
            "        └───Dimension\n" +
            "            ├───descriptor=“temp”\n" +
            "            ├───units=“Celcius”\n" +
            "            ├───minValue=“-2.0”\n" +
            "            ├───maxValue=“30.0”\n" +
            "            ├───validSampleValues=“[-2 … 30]”\n" +
            "            └───fillSampleValues=“-99.99”\n"), asTree);
        reader.dispose();
    }

    /**
     * Tests reading the data with the {@link ImageCoverageReader}.
     *
     * @throws IOException if an error occurred while reading the file.
     * @throws CoverageStoreException Should never happen.
     */
    @Test
    public void testCoverageReader() throws IOException, CoverageStoreException {
        final ImageCoverageReader reader = new ImageCoverageReader();
        reader.setInput(createImageReader());
        assertEquals(Collections.singletonList("temp"), reader.getCoverageNames());
        final GridCoverage2D coverage = reader.read(0, null);
        reader.dispose(); // Dispose also the NetcdfImageReader.
        //
        // Inspect the sample dimensions.
        //
        final GridSampleDimension band = coverage.getSampleDimension(0);
        assertTrue("Expected geophysics data.", band.getSampleToGeophysics().isIdentity());
        final List<Category> categories = band.getCategories();
        assertEquals(2, categories.size());
        //
        // Quantitative category
        //
        Category category = categories.get(0);
        NumberRange<?> range = category.getRange();
        assertEquals("temp", category.getName().toString());
        assertEquals("temp", -2.0, range.getMinimum(), 0.0);
        assertEquals("temp", 30.0, range.getMaximum(), 0.0);
        //
        // "No data" category.
        //
        category = categories.get(1);
        range = category.getRange();
        assertTrue(Double.isNaN(range.getMinimum()));
        assertTrue(Double.isNaN(range.getMaximum()));
        category = category.geophysics(false);
        range = category.getRange();
        assertEquals(-99.99, range.getMinimum(), 0.001);
        assertEquals(-99.99, range.getMaximum(), 0.001);
        //
        // The raster should contains only values in the [-2 .. 30] range.
        // If some -99.99 values have not been properly converted to NaN,
        // then this test will fail.
        //
        image = coverage.getRenderedImage();
        assertSampleValuesInRange(-2, 30, image);
    }
}
