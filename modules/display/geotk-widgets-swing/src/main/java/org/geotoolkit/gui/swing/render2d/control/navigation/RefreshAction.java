/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007 - 2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2008 - 2009, Johann Sorel
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
package org.geotoolkit.gui.swing.render2d.control.navigation;

import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;

import org.geotoolkit.gui.swing.render2d.control.AbstractMapAction;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.swing.resource.MessageBundle;

/**
 *
 * @author Johann Sorel (Puzzle-GIS)
 * @module
 */
public class RefreshAction extends AbstractMapAction {

    private static final ImageIcon ICON = IconBuilder.createIcon(FontAwesomeIcons.ICON_SYNC_ALT, 16, FontAwesomeIcons.DEFAULT_COLOR);

    public RefreshAction() {
        putValue(SMALL_ICON, ICON);
        putValue(SHORT_DESCRIPTION, MessageBundle.format("map_refresh"));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void actionPerformed(final ActionEvent arg0) {
        if (map != null) {
            map.getCanvas().repaint();
        }
    }

}
