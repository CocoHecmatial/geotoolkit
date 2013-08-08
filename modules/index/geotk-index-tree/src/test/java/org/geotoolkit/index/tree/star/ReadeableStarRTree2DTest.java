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
package org.geotoolkit.index.tree.star;

import java.io.File;
import java.io.IOException;
import org.geotoolkit.index.tree.AbstractTreeTest;
import org.geotoolkit.index.tree.FileTreeElementMapperTest;
import org.geotoolkit.index.tree.StoreIndexException;
import org.geotoolkit.referencing.crs.DefaultEngineeringCRS;

/**
 *
 * @author rmarechal
 */
public class ReadeableStarRTree2DTest  extends AbstractTreeTest {

    /**
     * 
     * @throws StoreIndexException
     * @throws IOException
     * @throws ClassNotFoundException if there is a problem during {@link CoordinateReferenceSystem} invert serialization.
     */
    public ReadeableStarRTree2DTest() throws StoreIndexException, IOException, ClassNotFoundException {
        super(DefaultEngineeringCRS.CARTESIAN_2D);
        final File inOutFile = File.createTempFile("test", "tree");
        tEM = new FileTreeElementMapperTest(crs, File.createTempFile("test", "mapper"));
        tree = new FileStarRTree(inOutFile, 4, crs, tEM);
        tAF  = ((StarRTree)tree).getTreeAccess();
        insert();
        tree.close();
        tree = new FileStarRTree(inOutFile, tEM);
        tAF  = ((StarRTree)tree).getTreeAccess();
    }
}
