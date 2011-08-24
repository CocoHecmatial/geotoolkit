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

import java.awt.Rectangle;
import java.io.IOException;

import org.junit.*;
import static org.junit.Assert.*;


/**
 * Tests {@link Tile}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.00
 *
 * @since 2.5
 */
public final strictfp class TileTest {
    /**
     * Ensures that the tiles size is stored as unsigned short.
     *
     * @throws IOException Should never occurs.
     */
    @Test
    public void testUnsignedShort() throws IOException {
        Rectangle bounds = new Rectangle(0, 0, 1000, 1000);
        Tile tile = new Tile(null, "Tile.png", 0, bounds);
        assertEquals(bounds, tile.getRegion());

        bounds = new Rectangle(0, 0, 40000, 40000);
        tile = new Tile(null, "Tile.png", 0, bounds);
        assertEquals(bounds, tile.getRegion());

        bounds = new Rectangle(0, 0, 60000, 60000);
        tile = new Tile(null, "Tile.png", 0, bounds);
        assertEquals(bounds, tile.getRegion());

        bounds = new Rectangle(0, 0, 70000, 70000);
        try {
            tile = new Tile(null, "Tile.png", 0, bounds);
            fail("Should be out of unsigned short range.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
        }
    }
}
