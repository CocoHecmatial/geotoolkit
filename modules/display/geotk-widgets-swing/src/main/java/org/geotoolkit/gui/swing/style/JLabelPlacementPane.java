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
package org.geotoolkit.gui.swing.style;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import org.geotoolkit.gui.swing.resource.MessageBundle;
import org.geotoolkit.map.MapLayer;
import org.opengis.style.LabelPlacement;
import org.opengis.style.LinePlacement;
import org.opengis.style.PointPlacement;

/**
 * Label placement panel
 *
 * @author Johann Sorel
 * @module
 */
public class JLabelPlacementPane extends StyleElementEditor<LabelPlacement> {

    private MapLayer layer = null;

    /** Creates new form JPointPlacementPanel */
    public JLabelPlacementPane() {
        super(LabelPlacement.class);
        initComponents();
    }

    @Override
    public void setLayer(final MapLayer layer) {
        this.layer = layer;
        guiLine.setLayer(layer);
        guiPoint.setLayer(layer);
    }

    @Override
    public MapLayer getLayer() {
        return layer;
    }

    @Override
    public void parse(final LabelPlacement target) {

        if(target instanceof LinePlacement){
            guiLine.parse( (LinePlacement)target);
            jrbLine.setSelected(true);
        }else if(target instanceof PointPlacement){
            guiPoint.parse( (PointPlacement)target);
            jrbPoint.setSelected(true);
        }else{
            jrbPoint.setSelected(true);
        }
        updateActivePlacement();
    }

    @Override
    public LabelPlacement create() {
        if(jrbLine.isSelected()){
            return guiLine.create();
        }else{
            return guiPoint.create();
        }
    }

    private void updateActivePlacement(){
        guiPlacePane.removeAll();
        if(jrbLine.isSelected()){
            guiPlacePane.add(BorderLayout.CENTER,guiLine);
        }else{
            guiPlacePane.add(BorderLayout.CENTER,guiPoint);
        }
        guiPlacePane.revalidate();
        guiPlacePane.repaint();
    }

    @Override
    protected Object[] getFirstColumnComponents() {
        return new Object[]{guiLine,guiPoint};
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        grpType = new ButtonGroup();
        guiLine = new JLinePlacementPane();
        guiPoint = new JPointPlacementPane();
        jPanel1 = new JPanel();
        jrbLine = new JRadioButton();
        jrbPoint = new JRadioButton();
        guiPlacePane = new JPanel();

        guiLine.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                JLabelPlacementPane.this.propertyChange(evt);
            }
        });

        guiPoint.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                JLabelPlacementPane.this.propertyChange(evt);
            }
        });

        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        grpType.add(jrbLine);
        jrbLine.setText(MessageBundle.format("lineplacement")); // NOI18N
        jrbLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jrbLineActionPerformed(evt);
            }
        });

        grpType.add(jrbPoint);
        jrbPoint.setText(MessageBundle.format("pointplacement")); // NOI18N
        jrbPoint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jrbPointActionPerformed(evt);
            }
        });

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jrbPoint)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jrbLine)
                .addContainerGap(74, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(jrbPoint)
                .addComponent(jrbLine))
        );

        add(jPanel1, BorderLayout.NORTH);

        guiPlacePane.setLayout(new BorderLayout());
        add(guiPlacePane, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void jrbLineActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jrbLineActionPerformed
        updateActivePlacement();
    }//GEN-LAST:event_jrbLineActionPerformed

    private void jrbPointActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jrbPointActionPerformed
        updateActivePlacement();
    }//GEN-LAST:event_jrbPointActionPerformed

    private void propertyChange(PropertyChangeEvent evt) {//GEN-FIRST:event_propertyChange
        // TODO add your handling code here:
        if (PROPERTY_UPDATED.equalsIgnoreCase(evt.getPropertyName())) {
            firePropertyChange(PROPERTY_UPDATED, null, create());
            parse(create());
        }
    }//GEN-LAST:event_propertyChange

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ButtonGroup grpType;
    private JLinePlacementPane guiLine;
    private JPanel guiPlacePane;
    private JPointPlacementPane guiPoint;
    private JPanel jPanel1;
    private JRadioButton jrbLine;
    private JRadioButton jrbPoint;
    // End of variables declaration//GEN-END:variables
}
