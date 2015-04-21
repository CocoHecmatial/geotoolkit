/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 3 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.gui.javafx.crs;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.util.Callback;
import org.apache.sis.metadata.iso.citation.Citations;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.internal.Loggers;
import org.geotoolkit.referencing.CRS;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.ConicProjection;
import org.opengis.referencing.operation.CylindricalProjection;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.PlanarProjection;
import org.opengis.referencing.operation.Projection;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXCRSTable extends ScrollPane{
    
    private static final Color COLOR = new Color(30, 150, 250);
    public static final Image ICON_GEO = SwingFXUtils.toFXImage(GeotkFX.getBufferedImage("proj_geo", new Dimension(16, 16)), null);
    public static final Image ICON_SQUARE = SwingFXUtils.toFXImage(GeotkFX.getBufferedImage("proj_square", new Dimension(16, 16)), null);
    public static final Image ICON_STEREO = SwingFXUtils.toFXImage(GeotkFX.getBufferedImage("proj_stereo", new Dimension(16, 16)), null);
    public static final Image ICON_UTM = SwingFXUtils.toFXImage(GeotkFX.getBufferedImage("proj_utm", new Dimension(16, 16)), null);
    public static final Image ICON_CONIC = SwingFXUtils.toFXImage(GeotkFX.getBufferedImage("proj_conic", new Dimension(16, 16)), null);
    public static final Image ICON_UNKNOWNED = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_QUESTION,16,COLOR),null);
    
    private final ObjectProperty<CoordinateReferenceSystem> crsProperty = new SimpleObjectProperty<>();
    private final TableView<Code> uiTable = new TableView<>();
    
    private List<Code> allValues;
    
    public FXCRSTable(){
        uiTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        setContent(uiTable);
        setFitToHeight(true);
        setFitToWidth(true);
        
        //add a loader while we load datas
        final ProgressIndicator loading = new ProgressIndicator();
        loading.setMaxWidth(60);
        loading.setMaxHeight(60);
        loading.setBackground(new Background(new BackgroundFill(new javafx.scene.paint.Color(0, 0, 0, 0), CornerRadii.EMPTY, Insets.EMPTY))); 
        loading.setProgress(-1);        
        uiTable.setPlaceholder(loading);
        uiTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        uiTable.setTableMenuButtonVisible(false);
        
        uiTable.getSelectionModel().getSelectedCells().addListener(new ListChangeListener<TablePosition>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends TablePosition> c) {
                final ObservableList<TablePosition> cells = uiTable.getSelectionModel().getSelectedCells();
                if(!cells.isEmpty()){
                    final TablePosition cell = cells.get(0);
                    final Code code = uiTable.getItems().get(cell.getRow());
                    try {
                        crsProperty.set((CoordinateReferenceSystem)code.createObject());
                    } catch (FactoryException ex) {
                        Loggers.JAVAFX.log(Level.INFO,ex.getMessage(),ex);
                    }
                }
            }
        });
        
        uiTable.getColumns().add(new TypeColumn());
        uiTable.getColumns().add(new CodeColumn());
        uiTable.getColumns().add(new DescColumn());
        
        
        //load list
        new Thread(){
            @Override
            public void run() {
                try {
                    allValues = getCodes();
                    Platform.runLater(() -> {
                        uiTable.setItems(FXCollections.observableArrayList(allValues));
                        uiTable.setPlaceholder(new Label(""));
                    });
                } catch (FactoryException ex) {
                    Loggers.JAVAFX.log(Level.WARNING,ex.getMessage(),ex);
                }
            }
        }.start();
        
    }
    
    public ObjectProperty<CoordinateReferenceSystem> crsProperty(){
        return crsProperty;
    }
        
    public void searchCRS(final String searchword){
        filter(searchword);
    }
    
    /**
     * Display only the CRS name that contains the specified keywords. The {@code keywords}
     * argument is a space-separated list, usually provided by the user after he pressed the
     * "Search" button.
     *
     * @param keywords space-separated list of keywords to look for.
     */
    private void filter(String keywords) {
        List<Code> model = allValues;
        if (keywords != null) {
            final Locale locale = Locale.getDefault();
            keywords = keywords.toLowerCase(locale).trim();
            final String[] tokens = keywords.split("\\s+");
            if (tokens.length != 0) {
                model = new ArrayList<>();
                scan:
                for(Code code : allValues){
                    final String name = code.toString().toLowerCase(locale);
                    for (int j=0; j<tokens.length; j++) {
                        if (!name.contains(tokens[j])) {
                            continue scan;
                        }
                    }
                    model.add(code);
                }
            }
        }
        uiTable.getItems().setAll(model);
    }
    
    /**
     * Returns a collection containing only the factories of the specified authority.
     */
    private static Collection<CRSAuthorityFactory> filter(
            final Collection<? extends CRSAuthorityFactory> factories, final String authority){
        final List<CRSAuthorityFactory> filtered = new ArrayList<>();
        for (final CRSAuthorityFactory factory : factories) {
            if (Citations.identifierMatches(factory.getAuthority(), authority)) {
                filtered.add(factory);
            }
        }
        return filtered;
    }
    
    private List<Code> getCodes() throws FactoryException{
        final CRSAuthorityFactory factory = CRS.getAuthorityFactory(Boolean.FALSE);
        final Set<String> strs = factory.getAuthorityCodes(CoordinateReferenceSystem.class);
        final List<Code> codes = new ArrayList<>();
        for(String str : strs){
            codes.add(new Code(factory, str));
        }
        return codes;
    }
    
    private static class TypeColumn extends TableColumn<Code, Code>{

        public TypeColumn() {
            setEditable(false);
            setPrefWidth(30);
            setMinWidth(30);
            setMaxWidth(30);
            setCellValueFactory((CellDataFeatures<Code, Code> param) -> new SimpleObjectProperty<>(param.getValue()));
            setCellFactory(new Callback<TableColumn<Code, Code>, TableCell<Code, Code>>() {

                @Override
                public TableCell<Code, Code> call(TableColumn<Code, Code> param) {
                    return new TableCell<Code,Code>(){
                        @Override
                        protected void updateItem(Code item, boolean empty) {
                            super.updateItem(item, empty);
                            setGraphic(null);
                            if(item!=null){
                                Image icon = ICON_UNKNOWNED;
                                try{
                                    final IdentifiedObject obj = item.createObject();
                                    if(obj instanceof GeographicCRS){
                                        icon = ICON_GEO;
                                    }else if(obj instanceof ProjectedCRS){
                                        final ProjectedCRS pcrs = (ProjectedCRS) obj;
                                        final Projection proj = pcrs.getConversionFromBase();
                                        final OperationMethod method = proj.getMethod();
                                        
                                        //TODO need to detect UTM and stereo
                                        if(String.valueOf(proj.getName()).toLowerCase().contains("utm")){
                                            icon = ICON_UTM;
                                        }else if(proj instanceof ConicProjection){
                                            icon = ICON_CONIC;
                                        }else if(proj instanceof CylindricalProjection){
                                            icon = ICON_SQUARE;
                                        }else if(proj instanceof PlanarProjection){
                                            icon = ICON_SQUARE;
                                        }else{
                                            icon = ICON_SQUARE;
                                        }
                                    }else{
                                        icon = ICON_SQUARE;
                                    }
                                }catch(FactoryException ex){
                                    Loggers.JAVAFX.log(Level.INFO, ex.getMessage(),ex);
                                }
                                setGraphic(new ImageView(icon));
                            }
                        }
                    };
                }
            });
        }
        
    }
    
    private static class CodeColumn extends TableColumn<Code, String>{

        public CodeColumn() {
            super(GeotkFX.getString(FXCRSChooser.class, "code"));
            setEditable(false);
            setPrefWidth(150);
            setCellValueFactory((TableColumn.CellDataFeatures<Code, String> param) -> new SimpleObjectProperty<>(param.getValue().code));
        }
        
    }
    
    private static class DescColumn extends TableColumn<Code, String>{

        public DescColumn() {
            super(GeotkFX.getString(FXCRSChooser.class, "description"));
            setEditable(false);
            setCellValueFactory((TableColumn.CellDataFeatures<Code, String> param) -> new SimpleObjectProperty<>(param.getValue().getDescription()));
        }
        
    }
    
}
