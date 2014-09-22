/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
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

package org.geotoolkit.gui.javafx.style;

import org.geotoolkit.style.StyleConstants;
import org.opengis.style.LinePlacement;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXLinePlacement extends FXStyleElementController<FXLinePlacement, LinePlacement>{

    @Override
    public Class<LinePlacement> getEditedClass() {
        return LinePlacement.class;
    }

    @Override
    public LinePlacement newValue() {
        return getStyleFactory().linePlacement(StyleConstants.DEFAULT_LINEPLACEMENT_OFFSET);
    }
    
}
