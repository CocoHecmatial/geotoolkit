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

package org.geotoolkit.gui.javafx.contexttree;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.image.ImageView;
import org.geotoolkit.display2d.service.DefaultGlyphService;
import org.geotoolkit.gui.javafx.util.ButtonTreeTableCell;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class MapItemGlyphColumn extends TreeTableColumn<MapItem, MapItem>{

    public MapItemGlyphColumn() {                
        setCellValueFactory(param -> ((CellDataFeatures)param).getValue().valueProperty());     
        setCellFactory((TreeTableColumn<MapItem, MapItem> p) -> new GlyphButton());
        setEditable(true);
        setPrefWidth(34);
        setMinWidth(34);
        setMaxWidth(34);
    }
    
    private static class GlyphButton extends ButtonTreeTableCell<MapItem, MapItem>{

        public GlyphButton() {
            super(false, null,
                  (MapItem t) -> t instanceof MapLayer,
                  (MapItem t) -> openEditor(t));
        }

        @Override
        protected void updateItem(MapItem item, boolean empty) {
            super.updateItem(item, empty);
            
            if(item instanceof MapLayer){
                final BufferedImage img = DefaultGlyphService.create(((MapLayer)item).getStyle(), new Dimension(24, 22), null);
                button.setGraphic(new ImageView(SwingFXUtils.toFXImage(img,null)));
            }
            
        }
        
        private static void openEditor(MapItem t){
            System.out.println("todo");
        }
        
    }
    
    
}
