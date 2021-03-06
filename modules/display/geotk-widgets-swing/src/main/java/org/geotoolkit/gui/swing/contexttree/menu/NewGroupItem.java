/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Johann Sorel
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
package org.geotoolkit.gui.swing.contexttree.menu;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.geotoolkit.gui.swing.contexttree.AbstractTreePopupItem;
import org.geotoolkit.gui.swing.resource.MessageBundle;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.DefaultDescription;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;

/**
 * New MapItem menu item for JContextTree
 *
 * @author Johann Sorel (Puzzle-GIS)
 * @module
 */
public class NewGroupItem extends AbstractTreePopupItem{

    private WeakReference<MapItem> pathRef;

    /**
     * delete item for jcontexttree
     */
    public NewGroupItem(){
        super( MessageBundle.format("contexttreetable_newgroup") );
        init();
    }

    private void init(){
        setIcon( IconBuilder.createIcon(FontAwesomeIcons.ICON_FOLDER,16,FontAwesomeIcons.DEFAULT_COLOR) );

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(pathRef == null) return;

                final MapItem item = pathRef.get();
                if(item == null) return;

                final MapItem child = MapBuilder.createItem();
                child.setDescription(new DefaultDescription(new SimpleInternationalString("group"), new SimpleInternationalString("group")));
                item.items().add(child);
            }
        }
        );
    }

    @Override
    public boolean isValid(final TreePath[] selection) {
        if(selection.length > 1 || selection.length == 0) return false;
        Object obj = ((DefaultMutableTreeNode)selection[0].getLastPathComponent()).getUserObject();
        return (obj instanceof MapItem && !(obj instanceof MapLayer));
    }

    @Override
    public Component getComponent(final TreePath[] selection) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selection[0].getLastPathComponent();
        pathRef = new WeakReference<MapItem>((MapItem) node.getUserObject());
        return this;
    }

}
