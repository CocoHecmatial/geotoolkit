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

import java.beans.PropertyChangeEvent;
import java.util.IdentityHashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.geotoolkit.map.ItemListener;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.util.collection.CollectionChangeEvent;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class TreeMapItem extends TreeItem implements ItemListener {

    public TreeMapItem(MapItem item) {
        super(item);

        if(item instanceof MapLayer){
            getChildren().add(new StyleMapItem((MapLayer) item));
        }else{
            for(int i=item.items().size()-1;i>=0;i--){
                final MapItem child = item.items().get(i);
                getChildren().add(new TreeMapItem(child));
            }
        }
        /** listen to children changes */
        item.addItemListener(new ItemListener.Weak(item, this));
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void itemChange(CollectionChangeEvent<MapItem> event) {
        final int type = event.getType();
        if(type != CollectionChangeEvent.ITEM_ADDED && type != CollectionChangeEvent.ITEM_REMOVED) return;

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                final MapItem item = (MapItem) getValue();

                //rebuild structure
                final Map<MapItem,TreeMapItem> cache = new IdentityHashMap<>();
                for(Object ti : getChildren()){
                    final TreeMapItem mi = (TreeMapItem) ti;
                    cache.put((MapItem)mi.getValue(), (TreeMapItem)ti);
                }

                getChildren().clear();

                for(int i=item.items().size()-1;i>=0;i--){
                    final MapItem child = item.items().get(i);
                    TreeMapItem tmi = cache.get(child);
                    if(tmi==null) tmi = new TreeMapItem(child);
                    getChildren().add(tmi);
                }
            }
        });
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
    }

}
