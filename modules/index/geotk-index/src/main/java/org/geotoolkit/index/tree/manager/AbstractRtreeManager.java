/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 3 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotoolkit.index.tree.manager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.index.tree.StoreIndexException;
import org.geotoolkit.index.tree.Tree;
import org.geotoolkit.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AbstractRtreeManager {

    protected static final Map<File, Tree<NamedEnvelope>> CACHED_TREES = new HashMap<>();
    protected static final Map<File, List<Object>> TREE_OWNERS = new HashMap<>();

    protected static final Logger LOGGER = Logging.getLogger(FileRtreeManager.class);

    public static final CoordinateReferenceSystem DEFAULT_CRS;

    static {
        CoordinateReferenceSystem crs = null;
        try {
            crs = CRS.decode("CRS:84");
        } catch (FactoryException ex) {
            LOGGER.log(Level.SEVERE, "Error while reading CRS:84", ex);
        }
        DEFAULT_CRS = crs;
    }

    public static void close(final File directory, final Tree rTree, final Object owner) throws StoreIndexException, IOException {
        final List<Object> owners = TREE_OWNERS.get(directory);
        owners.remove(owner);

        if (owners.isEmpty()) {
            if (rTree != null) {
                if (!rTree.isClosed()) {
                    rTree.close();
                    if (rTree.getTreeElementMapper() != null) {
                        rTree.getTreeElementMapper().close();
                    }
                }
            }
        } else {
            LOGGER.config("R-tree is used by another object. Not closing");
        }
    }

}
