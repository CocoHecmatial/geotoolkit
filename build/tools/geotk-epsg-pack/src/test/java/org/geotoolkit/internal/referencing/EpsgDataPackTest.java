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
package org.geotoolkit.internal.referencing;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Tests the {@link EpsgDataPack} class.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.18
 *
 * @since 3.18 (derived from 3.00)
 */
public final class EpsgDataPackTest {
    /**
     * Tests the {@link EpsgDataPack#removeLF} method.
     *
     * @since 3.18 (derived from 3.00)
     */
    @Test
    public void testRemoveLF() {
        final StringBuilder buffer = new StringBuilder(" \nOne,\nTwo, \n Three Four\nFive \nSix \n");
        EpsgDataPack.removeLF(buffer);
        assertEquals("One,Two,Three Four Five Six", buffer.toString());
    }
}