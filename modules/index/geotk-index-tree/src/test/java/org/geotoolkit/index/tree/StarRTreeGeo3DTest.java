/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2012, Geomatys
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
package org.geotoolkit.index.tree;

import java.io.IOException;
import org.geotoolkit.index.tree.io.StoreIndexException;
import org.geotoolkit.index.tree.star.StarRTree;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;

/**
 * Create R*Tree test suite in geographic 3D space.
 *
 * @author Rémi Marechal (Geomatys).
 */
public class StarRTreeGeo3DTest extends SpatialTreeTest {

    public StarRTreeGeo3DTest() throws StoreIndexException, IOException {
        super(DefaultGeographicCRS.WGS84_3D);
        tree = new StarRTree(4, crs, tEM);
    }
}
