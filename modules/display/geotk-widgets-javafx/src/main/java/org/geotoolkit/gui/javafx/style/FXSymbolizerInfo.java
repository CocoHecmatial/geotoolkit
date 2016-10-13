/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014-2015, Geomatys
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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import javax.measure.quantity.Length;
import javax.measure.Unit;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.MapLayer;
import org.opengis.filter.expression.Expression;
import org.opengis.style.Description;
import org.opengis.style.Symbolizer;
import org.apache.sis.measure.Units;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXSymbolizerInfo extends GridPane{

    protected MapLayer layer = null;
    protected volatile boolean updating = false;

    private final SimpleBooleanProperty value = new SimpleBooleanProperty();

    @FXML private Label uiNameLbl;
    @FXML private Label uiTitleLbl;
    @FXML private Label uiAbstractLbl;
    @FXML private Label uiUnitLbl;
    @FXML private Label uiGeomLbl;
    @FXML private TextField uiName;
    @FXML private TextField uiTitle;
    @FXML private TextField uiAbstract;
    @FXML private ChoiceBox<Unit> uiUnit;
    @FXML private FXTextExpression uiGeom;


    public FXSymbolizerInfo() {
        GeotkFX.loadJRXML(this,FXSymbolizerInfo.class);

        FXStyleElementController.configureAdvancedProperty(
                uiTitle,uiTitleLbl,
                uiAbstract,uiAbstractLbl,
                uiUnit,uiUnitLbl,
                uiGeom,uiGeomLbl);
        final FXMode mode = new FXMode();
        add(mode, 0, 0, 3, 1);
        setHalignment(mode, HPos.RIGHT);

    }

    /**
     *
     * @return fake property, just to have events
     */
    public SimpleBooleanProperty valueProperty(){
        return value;
    }

    public String getName(){
        return uiName.getText();
    }

    public Description getDescription(){
        return GO2Utilities.STYLE_FACTORY.description(uiTitle.getText(), uiAbstract.getText());
    }

    public Unit getUnit(){
        return uiUnit.getValue();
    }

    public Expression getGeom(){
        return uiGeom.valueProperty().get();
    }

    public void initialize() {
        uiUnit.setItems(FXCollections.observableArrayList(Units.POINT,Units.METRE, Units.INCH, Units.MILE, Units.FOOT));
        uiUnit.getSelectionModel().select(0);
        uiUnit.setConverter(new StringConverter<Unit>() {

            @Override
            public String toString(Unit object) {
                if(object == Units.POINT) return "pixel";
                else if(object == Units.METRE) return "metre";
                else if(object == Units.INCH) return "inch";
                else if(object == Units.MILE) return "mile";
                else if(object == Units.FOOT) return "foot";
                else return "pixel";
            }

            @Override
            public Unit fromString(String string) {
                if("pixel".equals(string)) return Units.POINT;
                else if("metre".equals(string)) return Units.METRE;
                else if("inch".equals(string)) return Units.INCH;
                else if("mile".equals(string)) return Units.MILE;
                else if("foot".equals(string)) return Units.FOOT;
                else return Units.POINT;
            }
        });

        //catch change events
        final EventHandler<ActionEvent> eventHandler = (ActionEvent event) -> {
            if(updating) return;
            value.setValue(!value.get());
        };
        uiName.setOnAction(eventHandler);
        uiTitle.setOnAction(eventHandler);
        uiAbstract.setOnAction(eventHandler);

        final ChangeListener changeListener = new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if(updating) return;
                value.setValue(!value.get());
            }
        };

        uiUnit.valueProperty().addListener(changeListener);
        uiGeom.valueProperty().addListener(changeListener);

    }

    public MapLayer getLayer() {
        return layer;
    }

    public void setLayer(MapLayer layer) {
        this.layer = layer;
        uiGeom.setLayer(layer);
    }

    public void parse(Symbolizer styleElement) {
        updating = true;

        uiName.setText(notNull(styleElement.getName()));
        final Description desc = styleElement.getDescription();
        if(desc!=null){
            uiTitle.setText(notNull(desc.getTitle()));
            uiAbstract.setText(notNull(desc.getAbstract()));
        }else{
            uiTitle.setText("");
            uiAbstract.setText("");
        }
        final Unit<Length> uom = styleElement.getUnitOfMeasure();
        uiUnit.setValue(uom==null? Units.POINT : uom);
        uiGeom.valueProperty().set(styleElement.getGeometry());

        updating = false;
    }

    private static String notNull(CharSequence str){
        return (str==null) ? "" : str.toString();
    }

}
