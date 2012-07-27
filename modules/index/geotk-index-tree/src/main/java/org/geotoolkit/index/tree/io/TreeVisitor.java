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
package org.geotoolkit.index.tree.io;

import org.geotoolkit.index.tree.Node;
import org.opengis.geometry.Envelope;


/**Tree visitor.
 *
 * @author Rémi Maréchal (Geomatys).
 */
public interface TreeVisitor {
    TreeVisitorResult filter(Node node);
    TreeVisitorResult visit(Envelope element);
}
