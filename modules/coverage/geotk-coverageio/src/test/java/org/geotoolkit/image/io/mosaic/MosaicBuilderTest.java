/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008-2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.image.io.mosaic;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.*;
import org.geotoolkit.test.Depend;
import static org.junit.Assert.*;


/**
 * Tests {@link MosaicBuilder}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.17
 *
 * @since 2.5
 */
@Depend(TileManagerTest.class)
public final class MosaicBuilderTest extends MosaicTestBase {
    /**
     * Tests subsampling calculation.
     *
     * @throws IOException Should never occurs.
     */
    @Test
    public void testSubsampling() throws IOException {
        assertTrue(MosaicBuilder.class.desiredAssertionStatus());
        builder.setSubsamplings((Dimension[]) null);
        builder.setTileSize(null);

        Rectangle bounds = new Rectangle(SOURCE_SIZE*4, SOURCE_SIZE*2);
        builder.setUntiledImageBounds(bounds);
        assertEquals(bounds, builder.getUntiledImageBounds());

        Dimension size = builder.getTileSize();
        assertEquals(480, size.width);
        assertEquals(480, size.height);

        Dimension[] subsamplings = builder.getSubsamplings();
        int[] expected = new int[] {1,2,4,8,16,32,64,128,256};
        for (int i=0; i<subsamplings.length; i++) {
            assertEquals("width["  + i + ']', expected[i], subsamplings[i].width);
            assertEquals("height[" + i + ']', expected[i], subsamplings[i].height);
        }

        builder.setTileSize(new Dimension(960,960));
        builder.setSubsamplings((Dimension[]) null); // For forcing new computation.
        subsamplings = builder.getSubsamplings();
        expected = new int[] {1,2,4,8,16,32,64,128};
        for (int i=0; i<subsamplings.length; i++) {
            assertEquals("width["  + i + ']', expected[i], subsamplings[i].width);
            assertEquals("height[" + i + ']', expected[i], subsamplings[i].height);
        }
    }

    /**
     * Tests the serialization of current {@linkplain #builder}.
     *
     * @throws IOException If an I/O operation failed.
     * @throws ClassNotFoundException if a deserialization failed.
     */
    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final TileManager tileManager = builder.createTileManager(sourceTiles);
        assertEquals(4733, tileManager.getTiles().size());
        final String asText = tileManager.toString();
        assertFalse(asText.trim().isEmpty());
        assertTrue("Expected tiles created as in setUp()", manager.equals(tileManager));
        // we don't use assertEquals because the message is too long to format in case of failure.

        // Tests serialization
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(buffer);
        out.writeObject(tileManager);
        out.close();

        final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
        final TileManager serialized = (TileManager) in.readObject();
        in.close();

        assertNotSame(tileManager, serialized);
        assertEquals(tileManager, serialized);
        assertEquals(tileManager.getImageReaderSpis(), serialized.getImageReaderSpis());
    }
}
