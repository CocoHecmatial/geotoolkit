

package org.geotoolkit.gui.javafx.render2d;

import org.geotoolkit.gui.javafx.render2d.navigation.FXPanAction;
import org.geotoolkit.gui.javafx.render2d.navigation.FXZoomAllAction;
import org.geotoolkit.gui.javafx.render2d.navigation.FXZoomInAction;
import org.geotoolkit.gui.javafx.render2d.navigation.FXZoomNextAction;
import org.geotoolkit.gui.javafx.render2d.navigation.FXZoomOutAction;
import org.geotoolkit.gui.javafx.render2d.navigation.FXZoomPreviousAction;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import org.controlsfx.control.action.ActionUtils;
import org.geotoolkit.gui.javafx.render2d.navigation.FXRepaintAction;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXNavigationBar extends ToolBar {

    private static final String LEFT   = "buttongroup-left";
    private static final String CENTER = "buttongroup-center";
    private static final String RIGHT  = "buttongroup-right";
    
    public FXNavigationBar(FXMap map) {
        getStylesheets().add("/fr/sym/buttonbar.css");
        
        final Button butAll = new FXZoomAllAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
        final Button butRepaint = new FXRepaintAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
        final Button butPrevious = new FXZoomPreviousAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
        final Button butNext = new FXZoomNextAction(map).createButton(ActionUtils.ActionTextBehavior.HIDE);
        butAll.getStyleClass().add(LEFT);
        butRepaint.getStyleClass().add(CENTER);
        butPrevious.getStyleClass().add(CENTER);
        butNext.getStyleClass().add(RIGHT);
        final HBox hboxAction = new HBox(butAll,butRepaint,butPrevious,butNext);
        
        final ToggleButton butIn = new FXZoomInAction(map).createToggleButton(ActionUtils.ActionTextBehavior.HIDE);
        final ToggleButton butOut = new FXZoomOutAction(map).createToggleButton(ActionUtils.ActionTextBehavior.HIDE);
        final ToggleButton butPan = new FXPanAction(map,false).createToggleButton(ActionUtils.ActionTextBehavior.HIDE);
        butIn.getStyleClass().add(LEFT);
        butOut.getStyleClass().add(CENTER);
        butPan.getStyleClass().add(RIGHT);
        final HBox hboxHandler = new HBox(butIn,butOut,butPan);
        
        
        getItems().add(hboxAction);
        getItems().add(hboxHandler);
        
    }
    
    
}
