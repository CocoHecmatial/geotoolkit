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
package org.geotoolkit.gui.swing.render2d.control.navigation;


import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.event.MouseInputListener;
import org.geotoolkit.gui.swing.render2d.JMap2D;

/**
 * Zoom in Handler for GoMap2D.
 *
 * @author Johann Sorel
 * @module
 */
public class ZoomInHandler extends AbstractNavigationHandler {

    //we could use this cursor, but java do not handle translucent cursor correctly on every platform
    //private static final Cursor CUR_ZOOM_IN = cleanCursor(ZoomInAction.ICON.getImage(),new Point(0,0),"zoomin");
    private static  final Cursor CUR_ZOOM_IN = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private final MouseListen mouseInputListener = new MouseListen();
    private double zoomFactor = 2;

    public ZoomInHandler(final JMap2D map) {
        super(map);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void install(final Component component) {
        super.install(component);
        component.addMouseListener(mouseInputListener);
        component.addMouseMotionListener(mouseInputListener);
        component.addMouseWheelListener(mouseInputListener);
        map.setCursor(CUR_ZOOM_IN);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void uninstall(final Component component) {
        super.uninstall(component);
        component.removeMouseListener(mouseInputListener);
        component.removeMouseMotionListener(mouseInputListener);
        component.removeMouseWheelListener(mouseInputListener);
        map.setCursor(null);
    }

    private class MouseListen implements MouseInputListener, MouseWheelListener {

        private int startX;
        private int startY;
        private int lastX;
        private int lastY;
        private int mousebutton = 0;

        @Override
        public void mouseClicked(final MouseEvent e) {

            mousebutton = e.getButton();

            // left mouse button
            if (e.getButton() == MouseEvent.BUTTON1) {
                scale(e.getPoint(), zoomFactor);
            }

        }

        @Override
        public void mousePressed(final MouseEvent e) {
            startX = e.getX();
            startY = e.getY();
            lastX = 0;
            lastY = 0;

            mousebutton = e.getButton();
            if (mousebutton == MouseEvent.BUTTON1) {

            } else if (mousebutton == MouseEvent.BUTTON3) {
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

            if (mousebutton == MouseEvent.BUTTON1) {

                if(startX != endX && startY != endY){
                    zoom(startX,startY,endX,endY);
                }

                decorationPane.setBuffer(null);
                decorationPane.setFill(false);
                decorationPane.setCoord(-10, -10,-10, -10, false);

//                int width = map.getComponent().getWidth() / 2;
//                int height = map.getComponent().getHeight() / 2;
//                int left = e.getX() - (width / 2);
//                int bottom = e.getY() - (height / 2);
//                decorationPane.setFill(false);
//                decorationPane.setCoord(left, bottom, width, height, true);

            } //right mouse button : pan action
            else if (mousebutton == MouseEvent.BUTTON3) {

                if(!isStateFull()){
                    decorationPane.setBuffer(null);
                    decorationPane.setFill(false);
                    decorationPane.setCoord(-10, -10,-10, -10, false);
                    processDrag(startX, startY, endX, endY);
                }
            }

            lastX = 0;
            lastY = 0;
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
        public void mouseDragged(final MouseEvent e) {
            int x = e.getX();
            int y = e.getY();


            // left mouse button
            if (mousebutton == MouseEvent.BUTTON1) {

                if ((lastX > 0) && (lastY > 0)) {
                    drawRectangle(startX,startY,lastX,lastY,true, true);
                }

                // draw new box
                lastX = x;
                lastY = y;
                drawRectangle(startX,startY,lastX,lastY,true, true);

            } //right mouse button : pan action
            else if (mousebutton == MouseEvent.BUTTON3) {
                if ((lastX > 0) && (lastY > 0)) {
                    int dx = lastX - startX;
                    int dy = lastY - startY;

                    if(isStateFull()){
                        processDrag(lastX, lastY, x, y);
                    }else{
                        decorationPane.setFill(true);
                        decorationPane.setCoord(dx, dy, map.getComponent().getWidth(), map.getComponent().getHeight(), true);
                    }
                }
                lastX = x;
                lastY = y;

            }

        }

        @Override
        public void mouseMoved(final MouseEvent e) {

//            int width = map.getComponent().getWidth() / 2;
//            int height = map.getComponent().getHeight() / 2;
//
//            int left = e.getX() - (width / 2);
//            int bottom = e.getY() - (height / 2);
//
//            decorationPane.setFill(false);
//            decorationPane.setCoord(left, bottom, width, height, true);

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
