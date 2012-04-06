/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008-2012, Open Source Geospatial Foundation (OSGeo)
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

import java.util.ArrayList;
import java.util.List;
import org.geotoolkit.index.tree.hilbert.HilbertRTree;
import org.geotoolkit.index.tree.nodefactory.TreeNodeFactory;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;

/**Create Hilbert R-Tree test suite in 2D Geographical space.
 *
 * @author Rémi Marechal (Geomatys).
 */
public class HilbertRTreeGeo2DTest extends SpatialTreeTest{

    public HilbertRTreeGeo2DTest() throws TransformException {
        super(new HilbertRTree(4,3, DefaultGeographicCRS.WGS84, TreeNodeFactory.DEFAULT_FACTORY));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkBoundaryNode(final Node node) {
        final List<Envelope> lS = new ArrayList<Envelope>();
        if (node.isLeaf()) {
            for (Node no : node.getChildren()) {
                if (!no.isEmpty()) {
                    lS.addAll(no.getEntries());
                }
            }
        } else {
            for (Node no : node.getChildren()) {
                lS.add(no.getBoundary());
            }
        }
        return (DefaultTreeUtils.getEnveloppeMin(lS).equals(node.getBoundary()));
    }
}
