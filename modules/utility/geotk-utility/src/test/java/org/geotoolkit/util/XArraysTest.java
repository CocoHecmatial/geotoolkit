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
package org.geotoolkit.util;

import org.junit.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link XArrays} utility methods.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.04
 *
 * @since 3.04
 */
public final class XArraysTest {
    /**
     * Tests {@link XArrays#union}.
     */
    @Test
    public void testUnion() {
        final int[] array1 = new int[] {2, 4, 6, 9, 12};
        final int[] array2 = new int[] {1, 2, 3, 12, 13, 18, 22};
        final int[] union = XArrays.union(array1, array2);
        assertArrayEquals(new int[] {1, 2, 3, 4, 6, 9, 12, 13, 18, 22}, union);
    }
}
