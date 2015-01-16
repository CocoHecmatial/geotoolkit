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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import static org.geotoolkit.gui.javafx.style.FXStyleElementController.getStyleFactory;
import org.geotoolkit.map.MapLayer;
import static org.geotoolkit.style.StyleConstants.DEFAULT_GRAPHIC;
import org.opengis.style.PointSymbolizer;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXPointSymbolizer extends FXStyleElementController<PointSymbolizer>{

    @FXML protected FXGraphic uiGraphic;
    
    @Override
    public Class<PointSymbolizer> getEditedClass() {
        return PointSymbolizer.class;
    }

    @Override
    public PointSymbolizer newValue() {
        return getStyleFactory().pointSymbolizer(DEFAULT_GRAPHIC,null);
    }
    
    @Override
    public void initialize() {
        super.initialize();        
        final ChangeListener changeListener = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
            if(updating) return;
            value.set(getStyleFactory().pointSymbolizer(uiGraphic.valueProperty().get(), null));
        };
        
        uiGraphic.valueProperty().addListener(changeListener);
    }
    
    @Override
    public void setLayer(MapLayer layer) {
        super.setLayer(layer);
        uiGraphic.setLayer(layer);
    }
    
    @Override
    protected void updateEditor(PointSymbolizer pointSymbolizer) {
        uiGraphic.valueProperty().setValue(pointSymbolizer.getGraphic());
    }
    
}
