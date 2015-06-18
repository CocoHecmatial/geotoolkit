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

package org.geotoolkit.gui.javafx.render2d.data;

import java.awt.Dimension;
import java.util.List;
import java.util.logging.Level;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.gui.javafx.chooser.FXStoreChooser;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXMapAction;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.internal.Loggers;
import org.geotoolkit.map.MapLayer;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXAddServerStoreAction extends FXMapAction {
    public static final Image ICON = SwingFXUtils.toFXImage(GeotkFX.getBufferedImage("add-server", new Dimension(16, 16)), null);
    
    public FXAddServerStoreAction(FXMap map) {
        super(map,GeotkFX.getString(FXAddServerStoreAction.class,"label"),
                GeotkFX.getString(FXAddServerStoreAction.class,"label"),ICON);
    }
    
    @Override
    public void accept(ActionEvent event) {
        
        try {
            final List<MapLayer> layers = FXStoreChooser.showLayerDialog(null,FXStoreChooser.CLIENTFACTORY_ONLY);

            for(MapLayer layer : layers){
                if(layer == null) continue;
                map.getContainer().getContext().items().add(layer);
            }

        } catch (DataStoreException ex) {
            Loggers.DATA.log(Level.WARNING, ex.getMessage(), ex);
        }
        
    }
    
}
