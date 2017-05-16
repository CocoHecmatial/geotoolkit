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

package org.geotoolkit.gui.javafx.render2d;

import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import org.controlsfx.control.action.ActionUtils;
import org.geotoolkit.gui.javafx.render2d.data.FXAddCoverageStoreAction;
import org.geotoolkit.gui.javafx.render2d.data.FXAddDataStoreAction;
import org.geotoolkit.gui.javafx.render2d.data.FXAddFeatureStoreAction;
import org.geotoolkit.gui.javafx.render2d.data.FXAddServerStoreAction;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXAddDataBar extends ToolBar {

    private static final String LEFT   = "buttongroup-left";
    private static final String CENTER = "buttongroup-center";
    private static final String RIGHT  = "buttongroup-right";
    private static final String SINGLE = "buttongroup-single";

    public FXAddDataBar(FXMap map, boolean singleButon) {
        getStylesheets().add("/org/geotoolkit/gui/javafx/buttonbar.css");

        if(singleButon){
            final Button butStore = new FXAddDataStoreAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
            butStore.getStyleClass().add(SINGLE);
            final HBox hboxAction = new HBox(butStore);
            getItems().add(hboxAction);
        }else{
            final Button butFeatureStore = new FXAddFeatureStoreAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
            final Button butCoverageStore = new FXAddCoverageStoreAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
            final Button butServerStore = new FXAddServerStoreAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
            butFeatureStore.getStyleClass().add(LEFT);
            butCoverageStore.getStyleClass().add(CENTER);
            butServerStore.getStyleClass().add(RIGHT);
            final HBox hboxAction = new HBox(butFeatureStore,butCoverageStore,butServerStore);
            getItems().add(hboxAction);
        }
    }


}
