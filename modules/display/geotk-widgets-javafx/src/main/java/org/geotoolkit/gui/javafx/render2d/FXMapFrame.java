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

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.gui.javafx.contexttree.FXMapContextTree;
import org.geotoolkit.gui.javafx.contexttree.MapItemFilterColumn;
import org.geotoolkit.gui.javafx.contexttree.MapItemSelectableColumn;
import org.geotoolkit.gui.javafx.contexttree.menu.DeleteItem;
import org.geotoolkit.gui.javafx.contexttree.menu.LayerPropertiesItem;
import org.geotoolkit.gui.javafx.contexttree.menu.OpacityItem;
import org.geotoolkit.gui.javafx.contexttree.menu.ZoomToItem;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXMapFrame {

    private final FXMap map;
    private final FXAddDataBar dataBar;
    private final FXNavigationBar navBar;
    private final FXGeoToolBar geotoolBar;
    private final FXCoordinateBar coordBar;
    private final FXMapContextTree tree;
    
    public FXMapFrame(MapContext context){
        this(context, new Hints());
    }
    
    public FXMapFrame(MapContext context, Hints hints){
        
        map = new FXMap(false,hints);
        map.getContainer().setContext(context);
        tree = new FXMapContextTree(context);
        tree.getTreetable().setShowRoot(false);
        tree.getMenuItems().add(new OpacityItem());
        tree.getMenuItems().add(new SeparatorMenuItem());
        tree.getMenuItems().add(new LayerPropertiesItem(map));
        tree.getMenuItems().add(new SeparatorMenuItem());
        tree.getMenuItems().add(new ZoomToItem(map));
        tree.getMenuItems().add(new DeleteItem());
        tree.getTreetable().getColumns().add(2,new MapItemFilterColumn());
        tree.getTreetable().getColumns().add(3,new MapItemSelectableColumn());
        
        dataBar = new FXAddDataBar(map,true);
        navBar = new FXNavigationBar(map);
        geotoolBar = new FXGeoToolBar(map);
        coordBar = new FXCoordinateBar(map);
        
        final GridPane topgrid = new GridPane();
        dataBar.setMaxHeight(Double.MAX_VALUE);
        navBar.setMaxHeight(Double.MAX_VALUE);
        geotoolBar.setMaxHeight(Double.MAX_VALUE);
        topgrid.add(dataBar,  0, 0);
        topgrid.add(navBar,  1, 0);
        topgrid.add(geotoolBar, 2, 0);
        
        final ColumnConstraints col0 = new ColumnConstraints();
        final ColumnConstraints col1 = new ColumnConstraints();
        final ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        final RowConstraints row0 = new RowConstraints();
        row0.setVgrow(Priority.ALWAYS);
        topgrid.getColumnConstraints().addAll(col0,col1,col2);
        topgrid.getRowConstraints().addAll(row0);
        
        
        final BorderPane border = new BorderPane();
        border.setTop(topgrid);
        border.setCenter(map);
        
        final SplitPane split = new SplitPane();
        split.getItems().add(tree);
        split.getItems().add(border);
        tree.setMinWidth(200);
        tree.setMaxWidth(400);
        
        
        //mainf frame with menu bar
        final MenuBar menuBar = new MenuBar();
        final Menu menu = new Menu("File");
        final MenuItem exit = new MenuItem("Exit");
        exit.setOnAction((ActionEvent event) -> {System.exit(0);});
        menu.getItems().add(exit);
        menuBar.getMenus().add(menu);
        
        final BorderPane framePane = new BorderPane(split,menuBar,null,coordBar,null);
        
        final Scene scene = new Scene(framePane);
        final Stage stage = new Stage();
        stage.setScene(scene);
        stage.setWidth(1024);
        stage.setHeight(768);
        
        stage.show();
        
    }
        
    public static void show(final MapContext context){
        show(context,null);
    }

    public static void show(final MapContext context, final Hints hints){
        show(context,false,hints);
    }

    public static void show(MapContext context, final boolean statefull, final Hints hints){
        if(context == null) context = MapBuilder.createContext();
        final MapContext mc = context;
        
        //Init JavaFX, ugly, but we only have 2 choices, extent Application or create this.
        new JFXPanel(); 
        
        Platform.runLater(() -> new FXMapFrame(mc,hints));
        
    }
    
    public static void main(String[] args) {
        FXMapFrame.show(null);
    }
    
}
