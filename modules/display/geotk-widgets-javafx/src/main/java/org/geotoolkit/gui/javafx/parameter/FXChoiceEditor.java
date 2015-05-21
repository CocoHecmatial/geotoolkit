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
package org.geotoolkit.gui.javafx.parameter;

import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import org.geotoolkit.feature.FeatureTypeUtilities;
import org.opengis.feature.AttributeType;
import org.opengis.feature.PropertyType;
import org.opengis.parameter.ParameterDescriptor;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXChoiceEditor extends FXValueEditor{

    private final BorderPane pane = new BorderPane();
    private final ComboBox guiCombo = new ComboBox();

    public FXChoiceEditor() {
        currentAttributeType.addListener(this::updateChoices);
        currentParamDesc.addListener(this::updateChoices);
    }
    
    protected void updateChoices(ObservableValue observable, Object oldValue, Object newValue) {
        pane.getChildren().clear();
        List choices = null;
        if (newValue instanceof AttributeType) {
            choices = extractChoices((AttributeType) newValue);
        } else if (newValue instanceof ParameterDescriptor) {

            final PropertyType pt = FeatureTypeUtilities.toPropertyType((ParameterDescriptor) newValue);
            if (pt instanceof AttributeType) {
                choices = extractChoices((AttributeType) pt);
            }
        }

        guiCombo.setItems(FXCollections.observableList(choices));
        pane.setCenter(guiCombo);
    }
    
    @Override
    public boolean canHandle(AttributeType property) {
        return extractChoices(property) != null;
    }

    @Override
    public boolean canHandle(ParameterDescriptor param) {
        final PropertyType pt = FeatureTypeUtilities.toPropertyType(param);
        if(pt instanceof AttributeType){
            return canHandle((AttributeType)pt);
        }
        return false;
    }
    
    @Override
    public boolean canHandle(Class binding) {
        return false;
    }

    @Override
    public Node getComponent() {
        return pane;
    }

    @Override
    public Property valueProperty() {
        return guiCombo.valueProperty();
    }

}
