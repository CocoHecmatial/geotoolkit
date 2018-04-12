/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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
package org.geotoolkit.osmtms.model;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.geotoolkit.storage.coverage.AbstractGridMosaic;
import org.geotoolkit.storage.coverage.Pyramid;
import org.geotoolkit.storage.coverage.TileReference;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.storage.coverage.PyramidSet;
import org.opengis.geometry.DirectPosition;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module
 */
public class OSMTMSMosaic extends AbstractGridMosaic{

    private final int scaleLevel;

    public OSMTMSMosaic(Pyramid pyramid, DirectPosition upperLeft, Dimension gridSize,
            Dimension tileSize, double scale, int scaleLevel) {
        super(pyramid,upperLeft,gridSize,tileSize,scale);
        this.scaleLevel = scaleLevel;
    }

    public int getScaleLevel() {
        return scaleLevel;
    }

    @Override
    public TileReference getTile(int col, int row, Map hints) throws DataStoreException {
        hints.put(PyramidSet.HINT_FORMAT, "image/png");
        return ((OSMTMSPyramidSet)getPyramid().getPyramidSet()).getTile(this, col, row, hints);
    }

    @Override
    public BlockingQueue<Object> getTiles(Collection<? extends Point> positions, Map hints) throws DataStoreException {
        hints.put(PyramidSet.HINT_FORMAT, "image/png");
        return ((OSMTMSPyramidSet)getPyramid().getPyramidSet()).getTiles(this, positions, hints);
    }

}
