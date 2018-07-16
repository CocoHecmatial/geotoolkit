/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007 - 2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2008 - 2009, Johann Sorel
 *    (C) 2011, Geomatys
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
package org.geotoolkit.gui.swing.render2d.control.edition;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStoreContentEvent;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.swing.resource.MessageBundle;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.storage.StorageListener;

/**
 *
 * @author Johann Sorel
 * @module
 */
public class SessionRollbackAction extends AbstractAction implements ChangeListener<ChangeEvent> {

    private static final ImageIcon ICON_ROLLBACK = IconBuilder.createIcon(FontAwesomeIcons.ICON_UNDO, 16, FontAwesomeIcons.DEFAULT_COLOR);
    private static final ImageIcon ICON_WAIT = IconBuilder.createIcon(FontAwesomeIcons.ICON_SPINNER, 16, FontAwesomeIcons.DEFAULT_COLOR);

    private final StorageListener.Weak weakListener = new StorageListener.Weak(this);
    private FeatureMapLayer layer;

    public SessionRollbackAction() {
        this(null);
    }

    public SessionRollbackAction(final FeatureMapLayer layer) {
        putValue(SMALL_ICON, ICON_ROLLBACK);
        putValue(NAME, MessageBundle.format("sessionRollback"));
        putValue(SHORT_DESCRIPTION, MessageBundle.format("sessionRollback"));
        setLayer(layer);
    }

    @Override
    public boolean isEnabled() {
        if (!(layer.getResource() instanceof FeatureCollection)) return false;
        final FeatureCollection col = (FeatureCollection) layer.getResource();
        return super.isEnabled() && (layer != null)
                && (col.getSession().hasPendingChanges());
    }

    public FeatureMapLayer getLayer() {
        return layer;
    }

    public void setLayer(final FeatureMapLayer layer) {
        //remove previous listener
        weakListener.unregisterAll();

        final boolean newst = isEnabled();
        this.layer = layer;
        firePropertyChange("enabled", !newst, newst);

        if(this.layer != null){
            final FeatureCollection col = (FeatureCollection) this.layer.getResource();
            weakListener.registerSource(col.getSession());
        }
    }

    @Override
    public void actionPerformed(final ActionEvent event) {

        if (layer != null ) {
            putValue(SMALL_ICON, ICON_WAIT);
            final Thread t = new Thread(){
                @Override
                public void run() {
                    try {
                        final FeatureCollection col = (FeatureCollection) layer.getResource();
                        col.getSession().rollback();
                    }finally{
                        putValue(SMALL_ICON, ICON_ROLLBACK);
                    }
                }
            };
            t.start();
        }
    }

    @Override
    public void changeOccured(ChangeEvent event) {
        if (event instanceof FeatureStoreContentEvent) {
            final FeatureStoreContentEvent fevent = (FeatureStoreContentEvent) event;
            if(fevent.getType() == FeatureStoreContentEvent.Type.SESSION){
                //refresh enable state
                setLayer(layer);
            }
        }
    }

}
