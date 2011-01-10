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

import org.geotoolkit.gui.swing.resource.MessageBundle;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;
import org.geotoolkit.gui.swing.style.StyleElementEditor;

/**
 *
 * @author Johann Sorel
 * @module pending
 */
public class JUOMPane extends StyleElementEditor<Unit<Length>>{

    /** Creates new form JUOMPane */
    public JUOMPane() {
        initComponents();
    }

    @Override
    public void parse(final Unit<Length> target) {
        if(SI.METRE.equals(target)){
            jcb_uom.setSelectedIndex(1);
        }else if(NonSI.FOOT.equals(target)){
            jcb_uom.setSelectedIndex(2);
        }else {
            jcb_uom.setSelectedIndex(0);
        }
    }

    @Override
    public Unit<Length> create() {
        switch(jcb_uom.getSelectedIndex()){
            case 1 :
                return SI.METRE;
            case 2 :
                return NonSI.FOOT;
            default :
                return NonSI.PIXEL;
        }
    }

    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new JLabel();
        jcb_uom = new JComboBox();

        setOpaque(false);


        jLabel1.setText(MessageBundle.getString("unit")); // NOI18N
        jcb_uom.setModel(new DefaultComboBoxModel(new String[] { "Pixels", "Meters", "Feet" }));

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jcb_uom, 0, 63, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(jLabel1)
                .addComponent(jcb_uom, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel jLabel1;
    private JComboBox jcb_uom;
    // End of variables declaration//GEN-END:variables

}
