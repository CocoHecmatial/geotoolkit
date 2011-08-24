/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2011, Geomatys
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
package org.geotoolkit.image.io.metadata;

import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import javax.imageio.ImageReader;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.acquisition.Instrument;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.coverage.grid.RectifiedGrid;

import org.junit.*;
import org.geotoolkit.test.Depend;

import static org.geotoolkit.test.Assert.*;
import static org.geotoolkit.test.Commons.*;


/**
 * Tests {@link MetadataAccessor}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.16
 *
 * @since 3.06
 */
@Depend(SpatialMetadataFormatTest.class)
public final strictfp class MetadataAccessorTest {
    /**
     * Tests the accessor with some properties defined under the {@code "ImageDescription"} node.
     */
    @Test
    public void testImageDescription() {
        final SpatialMetadata metadata = new SpatialMetadata(SpatialMetadataFormat.IMAGE);
        assertMultilinesEquals("The metadata should initially contains only the root node.",
            SpatialMetadataFormat.FORMAT_NAME + "\n",
            metadata.toString());
        /*
         * Ensure that the metadata is initially empty and that
         * attempts to access attributes do not throw an exception.
         */
        final MetadataAccessor accessor = new MetadataAccessor(metadata, null, "ImageDescription", null);
        assertEquals("ImageDescription", accessor.name());
        assertEquals("Initially empty metadata should have no child.", 0, accessor.childCount());
        assertNull(accessor.getAttribute("imagingCondition"));
        assertNull(accessor.getAttributeAsDouble("cloudCoverPercentage"));
        assertMultilinesEquals("MetadataAccessor constructor should have created its node.",
            SpatialMetadataFormat.FORMAT_NAME + "\n" +
            "└───ImageDescription\n",
            metadata.toString());
        /*
         * Define a few values conform to the structure declared
         * in SpatialMetadataFormat.IMAGE.
         */
        accessor.setAttribute("imagingCondition", "cloud");
        accessor.setAttribute("cloudCoverPercentage", 20.0);
        /*
         * Check the value that we have set.
         */
        assertEquals("cloud", accessor.getAttribute("imagingCondition"));
        assertEquals(Double.valueOf(20), accessor.getAttributeAsDouble("cloudCoverPercentage"));
        assertMultilinesEquals(decodeQuotes(
            SpatialMetadataFormat.FORMAT_NAME + "\n" +
            "└───ImageDescription\n"  +
            "    ├───imagingCondition=“cloud”\n" +
            "    └───cloudCoverPercentage=“20.0”\n"),
            metadata.toString());
        /*
         * Ensure that attempt to select a child thrown an exception,
         * since this accessor does not declare any children.
         */
        try {
            accessor.selectChild(0);
            fail("Selecting a child should be an illegal operation.");
        } catch (IndexOutOfBoundsException e) {
            // This is the expected exception.
        }
    }

    /**
     * Tests the accessor with some properties defined under the {@code "OffsetVectors"} node.
     */
    @Test
    public void testOffsetVectors() {
        testOffsetVectors("OffsetVector");
    }

    /**
     * Same tests than {@link #testOffsetVectors()}, but without explicit specification
     * of the child name.
     */
    @Test
    public void testAutoDetectChilds() {
        testOffsetVectors("#auto");
    }

    /**
     * Tests the accessor with some properties defined under the {@code "OffsetVectors"} node.
     * The child name shall be either {@code "OffsetVector"} or {@code "#auto"}.
     */
    private void testOffsetVectors(final String childName) {
        final SpatialMetadata metadata = new SpatialMetadata(SpatialMetadataFormat.IMAGE);
        assertMultilinesEquals("The metadata should initially contains only the root node.",
            SpatialMetadataFormat.FORMAT_NAME + "\n",
            metadata.toString());
        /*
         * Ensure that the metadata is initially empty and that
         * attempts to access attributes do not throw an exception.
         */
        final MetadataAccessor accessor = new MetadataAccessor(metadata, null, "RectifiedGridDomain/OffsetVectors", childName);
        assertEquals("OffsetVectors", accessor.name());
        assertMultilinesEquals("MetadataAccessor constructor should have created its node.",
            SpatialMetadataFormat.FORMAT_NAME + "\n" +
            "└───RectifiedGridDomain\n" +
            "    └───OffsetVectors\n",
            metadata.toString());
        /*
         * Define a few values conform to the structure declared
         * in SpatialMetadataFormat.IMAGE.
         */
        accessor.selectChild(accessor.appendChild());
        assertNull(accessor.getAttributeAsDoubles("values", false));
        accessor.setAttribute("values", new double[] {2, 5, 8});
        accessor.selectChild(accessor.appendChild());
        assertNull(accessor.getAttributeAsDoubles("values", false));
        accessor.setAttribute("values", new double[] {3, 1, 4});
        /*
         * Check the value that we have set.
         */
        accessor.selectParent();
        assertNull(accessor.getAttributeAsDoubles("values", false));
        accessor.selectChild(0);
        assertTrue(Arrays.equals(accessor.getAttributeAsDoubles("values", false), new double[] {2, 5, 8}));
        accessor.selectChild(1);
        assertTrue(Arrays.equals(accessor.getAttributeAsDoubles("values", false), new double[] {3, 1, 4}));
        assertMultilinesEquals(decodeQuotes(
            SpatialMetadataFormat.FORMAT_NAME + "\n" +
            "└───RectifiedGridDomain\n"  +
            "    └───OffsetVectors\n"    +
            "        ├───OffsetVector\n" +
            "        │   └───values=“2.0 5.0 8.0”\n" +
            "        └───OffsetVector\n" +
            "            └───values=“3.0 1.0 4.0”\n"),
            metadata.toString());
    }

    /**
     * Tests the accessor with a non-existent attribute under the {@code "ImageDescription"} node.
     * Actually nothing special happen except that the resulting {@code IIOMetadata} is invalid,
     * but this is not {@link MetadataAccessor} job to verify that.
     */
    @Test
    public void testNonExistentAttribute() {
        final SpatialMetadata  metadata = new SpatialMetadata(SpatialMetadataFormat.IMAGE);
        final MetadataAccessor accessor = new MetadataAccessor(metadata, null, "ImageDescription", null);
        accessor.setAttribute("cloudCoverPercentage", 20.0); // For comparison purpose.
        accessor.setAttribute("inexistent", 10.0);
    }

    /**
     * Tests the {@link MetadataAccessor#listPaths} static method.
     */
    @Test
    public void testListPaths() {
        /*
         * Simpliest cases: the element below is known to appear only once.
         */
        List<String> paths = MetadataAccessor.listPaths(SpatialMetadataFormat.STREAM, GeographicBoundingBox.class);
        assertEquals(Arrays.asList("DiscoveryMetadata/Extent/GeographicElement"), paths);
        /*
         * Simpliest case again, but deeper path. Note that it cross a collection (Instruments).
         */
        paths = MetadataAccessor.listPaths(SpatialMetadataFormat.STREAM, Identifier.class);
        assertEquals(Arrays.asList("AcquisitionMetadata/Platform/Instruments/Instrument/Identifier"), paths);
        /*
         * The element below appears more than once.
         * We don't consider elements order.
         */
        paths = MetadataAccessor.listPaths(SpatialMetadataFormat.IMAGE, Identifier.class);
        assertEquals(new HashSet<String>(Arrays.asList("ImageDescription/ImageQualityCode",
                "ImageDescription/ProcessingLevelCode")), new HashSet<String>(paths));
        /*
         * More tricky case: 'Instrument' is the type of elements in a collection.
         * But we want the path to the whole collection.
         */
        paths = MetadataAccessor.listPaths(SpatialMetadataFormat.STREAM, Instrument.class);
        assertEquals(Arrays.asList("AcquisitionMetadata/Platform/Instruments"), paths);
    }

    /**
     * Tests the {@link MetadataAccessor} constructors which locate the path
     * automatically from the given type, searching in the whole tree.
     */
    @Test
    public void testAutomaticLocation() {
        SpatialMetadata metadata = new SpatialMetadata(SpatialMetadataFormat.STREAM);
        MetadataAccessor accessor = new MetadataAccessor(metadata, "#auto", GeographicBoundingBox.class);
        accessor.setAttribute("inclusion", true);
        assertMultilinesEquals(decodeQuotes(
            SpatialMetadataFormat.FORMAT_NAME + "\n" +
            "└───DiscoveryMetadata\n" +
            "    └───Extent\n" +
            "        └───GeographicElement\n" +
            "            └───inclusion=“true”\n"),
            metadata.toString());
        /*
         * Following should fails because of ambiguity (there is many identifier).
         */
        metadata = new SpatialMetadata(SpatialMetadataFormat.IMAGE);
        try {
            accessor = new MetadataAccessor(metadata, "#auto", Identifier.class);
            fail(accessor.toString());
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
        }
    }

    /**
     * Tests the {@link MetadataAccessor} constructors which locate the path
     * automatically from the given type, forcing a search in the fallback.
     *
     * @since 3.16
     */
    @Test
    public void testAutomaticLocationInFallback() {
        SpatialMetadata metadata = new SpatialMetadata(SpatialMetadataFormat.STREAM);
        MetadataAccessor accessor = new MetadataAccessor(metadata, "#auto", GeographicBoundingBox.class);
        assertEquals("GeographicElement", accessor.name());
        accessor.setAttribute("inclusion", true);
        /*
         * The stream metadata should not know anything about the RectifiedGrid interface.
         * Note that we will ask exactly the same interface later.
         */
        try {
            accessor = new MetadataAccessor(metadata, "#auto", RectifiedGrid.class);
            fail("Should not find " + accessor);
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
            assertTrue(e.getMessage().contains("RectifiedGrid"));
        }
        /*
         * Creates an image metadata which will use the above stream metadata as a fallback.
         * This is not a recommanded practice (especially since both metadata format share
         * the same name). We are just too lazy for creating a new metadata format, and the
         * approach used here is sufficient for this test.
         */
        metadata = new SpatialMetadata(SpatialMetadataFormat.IMAGE, (ImageReader) null, metadata);
        accessor = new MetadataAccessor(metadata, "#auto", RectifiedGrid.class);
        assertEquals("RectifiedGridDomain", accessor.name());
        /*
         * Now ask a metadata which should work only if the MetadataAccessor has been
         * able to search in the fallback chain.
         */
        accessor = new MetadataAccessor(metadata, "#auto", GeographicBoundingBox.class);
        assertEquals("GeographicElement", accessor.name());
    }
}
