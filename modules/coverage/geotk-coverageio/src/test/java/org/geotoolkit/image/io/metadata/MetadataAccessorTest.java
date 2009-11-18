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
package org.geotoolkit.image.io.metadata;

import java.util.Arrays;

import org.geotoolkit.test.Depend;
import org.junit.*;

import static org.junit.Assert.*;
import static org.geotoolkit.test.Commons.*;


/**
 * Tests {@link MetadataAccessor}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.06
 *
 * @since 3.06
 */
@Depend(SpatialMetadataFormatTest.class)
public final class MetadataAccessorTest {
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
        final MetadataAccessor accessor = new MetadataAccessor(metadata, "ImageDescription", null);
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
        final MetadataAccessor accessor = new MetadataAccessor(metadata, "RectifiedGridDomain/OffsetVectors", childName);
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
        final MetadataAccessor accessor = new MetadataAccessor(metadata, "ImageDescription", null);
        accessor.setAttribute("cloudCoverPercentage", 20.0); // For comparison purpose.
        accessor.setAttribute("inexistent", 10.0);
    }
}
