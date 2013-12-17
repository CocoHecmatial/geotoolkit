/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007 - 2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2008 - 2009, Johann Sorel
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
package org.geotoolkit.gui.swing.go2.control.navigation;


import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseListener;
import org.geotoolkit.gui.swing.go2.JMap2D;

/**
 * Zoom in Handler for GoMap2D.
 * 
 * @author Johann Sorel
 * @module pending
 */
public class ZoomOutHandler extends AbstractNavigationHandler {

    private static final Cursor CUR_ZOOM_OUT = Toolkit.getDefaultToolkit().createCustomCursor(ZoomOutAction.ICON.getImage(),new Point(0, 0),"zoomout");
    private final MouseListen mouseInputListener = new MouseListen();
    private double zoomFactor = 2;

    public ZoomOutHandler(final JMap2D map) {
        super(map);
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public void install(final Component component) {
        super.install(component);
        component.addMouseListener(mouseInputListener);
        component.addMouseWheelListener(mouseInputListener);
        map.setCursor(CUR_ZOOM_OUT);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void uninstall(final Component component) {
        super.uninstall(component);
        component.removeMouseListener(mouseInputListener);
        component.removeMouseWheelListener(mouseInputListener);
        map.setCursor(null);
    }

    private class MouseListen implements MouseListener, MouseWheelListener {

        private int startX;
        private int startY;
        private int mousebutton = 0;

        @Override
        public void mouseClicked(final MouseEvent e) {

            mousebutton = e.getButton();

            // left mouse button
            if (mousebutton == MouseEvent.BUTTON1) {
                scale(e.getPoint(), 1d/zoomFactor);
            }

        }

        @Override
        public void mousePressed(final MouseEvent e) {
            startX = e.getX();
            startY = e.getY();

            mousebutton = e.getButton();
            if (mousebutton == MouseEvent.BUTTON3) {
                if(!isStateFull()){
                    decorationPane.setBuffer(map.getCanvas().getSnapShot());
                    decorationPane.setCoord(0, 0, map.getComponent().getWidth(), map.getComponent().getHeight(), true);
                }
            }

        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            int endX = e.getX();
            int endY = e.getY();

            decorationPane.setBuffer(null);
            
            //right mouse button : pan action
            if (mousebutton == MouseEvent.BUTTON3) {
                
                if(!isStateFull()){
                    decorationPane.setBuffer(null);
                    decorationPane.setFill(false);
                    decorationPane.setCoord(-10, -10,-10, -10, false);
                    processDrag(startX, startY, endX, endY);
                }
            }
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            decorationPane.setFill(false);
            decorationPane.setCoord(-10, -10,-10, -10, true);
        }

        @Override
        public void mouseWheelMoved(final MouseWheelEvent e) {
            int rotate = e.getWheelRotation();

            if(rotate<0){
                scale(e.getPoint(),zoomFactor);
            }else if(rotate>0){
                scale(e.getPoint(),1d/zoomFactor);
            }

        }
    }

}
