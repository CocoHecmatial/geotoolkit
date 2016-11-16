/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Johann Sorel
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

package org.geotoolkit.gui.swing.render2d.control.information.presenter;

import javax.swing.JComponent;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;


/**
 *
 * @author Johann Sorel (Geomatys)
 * @module
 */
public interface InformationPresenter extends Comparable<InformationPresenter> {

    /**
     * Determinate the presenter order.
     *
     * @return higher value for high priority.
     */
    double getPriority();

    /**
     * Create a user interface component to display the given object.
     * @param candidate , object to display
     * @return JComponent or null if no component appropriate.
     */
    JComponent createComponent(final Object graphic, final RenderingContext2D context, final SearchAreaJ2D area);

}
