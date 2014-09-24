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
import org.geotoolkit.style.StyleConstants;
import org.opengis.style.Fill;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXFill extends FXStyleElementController<FXFill,Fill> {

    @FXML
    private FXColorExpression uiColor;    
    @FXML
    private FXNumberExpression uiOpacity;
            
    @Override
    public Class<Fill> getEditedClass() {
        return Fill.class;
    }

    @Override
    public Fill newValue() {
        return StyleConstants.DEFAULT_FILL;
    }
    
    @Override
    public void initialize() {
        super.initialize();        
        final ChangeListener changeListener = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
            if(updating) return;
            final Fill fill;
            //TODO graphic fill
            fill = getStyleFactory().fill(uiColor.valueProperty().get(), uiOpacity.valueProperty().get());
            value.set(fill);
        };
        
        uiColor.valueProperty().addListener(changeListener);
        uiOpacity.valueProperty().addListener(changeListener);
    }
    
    @Override
    protected void updateEditor(Fill styleElement) {
        uiColor.valueProperty().setValue(styleElement.getColor());
        uiOpacity.valueProperty().setValue(styleElement.getOpacity());
    }

}
