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
package org.geotoolkit.index.tree.basic;

import java.io.File;
import java.io.IOException;
import org.geotoolkit.index.tree.access.TreeAccessFile;
import org.geotoolkit.index.tree.io.StoreIndexException;
import org.geotoolkit.index.tree.mapper.TreeElementMapper;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Remi Marechal(Geomatys).
 */
public class FileBasicRTree<E> extends BasicRTree<E> {

    /**
     * Number to identify tree file.
     */
    private final static int BASIC_NUMBER      = 188047901;
    private final static double VERSION_NUMBER = 0.1;

    public FileBasicRTree(final File outPut, final int maxElements, final CoordinateReferenceSystem crs, final SplitCase choice, final TreeElementMapper treeEltMap) throws StoreIndexException, IOException {
        super(new TreeAccessFile(outPut, BASIC_NUMBER, VERSION_NUMBER, maxElements, crs), choice, treeEltMap);
    }
    
    public FileBasicRTree(final File input, final SplitCase choice, final TreeElementMapper treeEltMap) throws IOException, StoreIndexException, ClassNotFoundException {
        super(new TreeAccessFile(input, BASIC_NUMBER, VERSION_NUMBER), choice, treeEltMap);
    } 
}
