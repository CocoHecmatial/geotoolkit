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

package org.geotoolkit.gui.javafx.layer;

import com.vividsolutions.jts.geom.Geometry;
import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.logging.Level;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.internal.Loggers;
import org.geotoolkit.map.FeatureMapLayer;
import org.opengis.feature.PropertyType;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXFeatureTable extends FXPropertyPane{
    
    private final TableView<Feature> table = new TableView<>();

    public FXFeatureTable() {
        setCenter(table);
    }

    @Override
    public String getTitle() {
        return "Feature table";
    }
    
    public String getCategory(){
        return "";
    }
    
    public boolean init(Object candidate){
        if(!(candidate instanceof FeatureMapLayer)) return false;
        
        final FeatureMapLayer layer = (FeatureMapLayer) candidate;
        final FeatureCollection<? extends Feature> col = layer.getCollection();
        
        //TODO make a caching versio, this is too slow for today use.
//        final ObservableFeatureCollection obsCol = new ObservableFeatureCollection((FeatureCollection<Feature>) col);
//        table.setItems(obsCol);
//        table.getColumns().clear();
        
        final FeatureType ft = col.getFeatureType();
        for(PropertyType prop : ft.getProperties(true)){
            final TableColumn<Feature,String> tc = new TableColumn<Feature,String>(prop.getName().toString());
            tc.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Feature, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Feature, String> param) {
                    final Object val = param.getValue().getPropertyValue(prop.getName().toString());
                    if(val instanceof Geometry){
                        return new SimpleObjectProperty<>("{geometry}");
                    }else{
                        return new SimpleObjectProperty<>(String.valueOf(val));
                    }
                }
            });
            tc.setCellFactory(TextFieldTableCell.forTableColumn());
            table.getColumns().add(tc);
        }
        
        return true;
    }
        
    private static class ObservableFeatureCollection extends AbstractSequentialList<Feature> implements ObservableList<Feature>{

        private final FeatureCollection<Feature> features;
        
        private ObservableFeatureCollection(FeatureCollection<Feature> features){
            this.features = features;
        }
        
        @Override
        public ListIterator<Feature> listIterator(int index) {
            
            final QueryBuilder qb = new QueryBuilder(features.getFeatureType().getName());
            qb.setStartIndex(index);
            
            final FeatureCollection subcol;            
            try {
                subcol = features.subCollection(qb.buildQuery());
            } catch (DataStoreException ex) {
                Loggers.JAVAFX.log(Level.WARNING, ex.getMessage(),ex);
                return Collections.EMPTY_LIST.listIterator();
            }
            final Iterator ite = subcol.iterator();
            
            final ListIterator lite = new ListIterator() {

                @Override
                public boolean hasNext() {
                    return ite.hasNext();
                }

                @Override
                public Object next() {
                    return ite.next();
                }

                @Override
                public boolean hasPrevious() {
                    return index>0;
                }

                @Override
                public Object previous() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public int nextIndex() {
                    return index+1;
                }

                @Override
                public int previousIndex() {
                    return index-1;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void set(Object e) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void add(Object e) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };
            
            return lite;
        }

        @Override
        public int size() {
            return features.size();
        }

        @Override
        public void addListener(ListChangeListener<? super Feature> listener) {
        }

        @Override
        public void removeListener(ListChangeListener<? super Feature> listener) {
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }
        
        @Override
        public boolean addAll(Feature... elements) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean setAll(Feature... elements) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean setAll(Collection<? extends Feature> col) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean removeAll(Feature... elements) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean retainAll(Feature... elements) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void remove(int from, int to) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
    
    
}
