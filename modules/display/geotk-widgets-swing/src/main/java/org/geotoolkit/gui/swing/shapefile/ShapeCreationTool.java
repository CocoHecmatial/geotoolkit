/*
 *    Puzzle GIS - Desktop GIS Platform
 *    http://puzzle-gis.codehaus.org
 *
 *    (C) 2007-2009, Johann Sorel
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
package org.geotoolkit.gui.swing.shapefile;

import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;

import org.geotoolkit.data.FileFeatureStoreFactory;
import org.geotoolkit.data.shapefile.ShapefileFeatureStore;
import org.geotoolkit.data.shapefile.ShapefileFeatureStoreFactory;
import org.geotoolkit.gui.swing.crschooser.JCRSChooser;
import org.geotoolkit.gui.swing.resource.MessageBundle;
import org.apache.sis.referencing.CommonCRS;
import org.geotoolkit.gui.swing.util.SwingUtilities;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTitledSeparator;
import org.opengis.feature.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;



/**
 * Widget Panel to open ShapeFiles.
 *
 * @author Johann Sorel (Puzzle-GIS)
 */
public class ShapeCreationTool extends JPanel {

    private final ShapeAttModel model = new ShapeAttModel();
    private Class geotype = Point.class;
    private CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
    private File file = new File("default.shp");

    /**
     * Creates new form shapeCreationPanel
     */
    public ShapeCreationTool() {
        initComponents();
        guiTable.getSelectionModel().setSelectionMode(guiTable.getSelectionModel().SINGLE_SELECTION);
        guiTable.getColumn(1).setCellEditor(new TypeEditor());

        guiCrs.setText(crs.getName().toString());
        guiFileName.setText(file.getAbsolutePath());
    }

    private void createShape(final String name) {
        try {
            // Create the DataStoreFactory
            final FileFeatureStoreFactory factory = new ShapefileFeatureStoreFactory();

            // Create a Map object used by our FeatureStore Factory
            // NOTE: file.toURI().toURL() is used because file.toURL() is deprecated
            String pathIndentifier = ShapefileFeatureStoreFactory.PATH.getName().getCode();
            final Map<String, Serializable> map = Collections.singletonMap(pathIndentifier, (Serializable)file.toURI());

            // Create the ShapefileFeatureStore from our factory based on our Map object
            final ShapefileFeatureStore myData = (ShapefileFeatureStore) factory.create(map);

            // Tell this shapefile what type of data it will store
            final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
            ftb.setName(name);
            ftb.addAttribute(geotype).setName("geom").addRole(AttributeRole.DEFAULT_GEOMETRY);

            final Field[] datas = model.getDatas();

            for (Field data : datas) {

                switch (data.getType()) {
                    case INTEGER:
                        ftb.addAttribute(Integer.class);
                        break;
                    case LONG:
                        ftb.addAttribute(Long.class);
                        break;
                    case DOUBLE:
                        ftb.addAttribute(Double.class);
                        break;
                    case STRING:
                        ftb.addAttribute(String.class);
                        break;
                    case DATE:
                        ftb.addAttribute(Date.class);
                        break;
                    default : break;
                }
            }

            final FeatureType featureType = ftb.build();

            // Create the Shapefile (empty at this point)
            myData.createFeatureType(featureType);

            // Tell the featurestore what type of Coordinate Reference System (CRS) to use
            //myData.forceSchemaCRS(crs);

            myData.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Incorrect File : " + e.getMessage());
        }

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        final ButtonGroup guiGeom = new ButtonGroup();
        final JLabel jLabel1 = new JLabel();
        final JXTitledSeparator jXTitledSeparator1 = new JXTitledSeparator();
        final JButton guiCreate = new JButton();
        final JButton guiFileChoose = new JButton();
        final JPanel jPanel2 = new JPanel();
        final JScrollPane jScrollPane1 = new JScrollPane();
        final JButton guiAdd = new JButton();
        final JButton guiUp = new JButton();
        final JButton guiDown = new JButton();
        final JButton guiDelete = new JButton();
        final JPanel jPanel1 = new JPanel();
        final JRadioButton guiPoint = new JRadioButton();
        final JRadioButton guiMultiPoint = new JRadioButton();
        final JRadioButton guiMultiLine = new JRadioButton();
        final JRadioButton guiMultiPolygon = new JRadioButton();
        final JButton guiCrsChoose = new JButton();
        final JLabel jLabel2 = new JLabel();

        jLabel1.setText(MessageBundle.format("shp_file")); // NOI18N
        jXTitledSeparator1.setTitle(MessageBundle.format("shp_shapefile_creation")); // NOI18N

        guiFileName.setEditable(false);

        guiFileName.setText(MessageBundle.format("shp_default")); // NOI18N
        guiCreate.setText(MessageBundle.format("shp_create")); // NOI18N
        guiCreate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                createAction(evt);
            }
        });

        guiFileChoose.setText(MessageBundle.format("shp_ppp")); // NOI18N
        guiFileChoose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                fileAction(evt);
            }
        });

        jPanel2.setBorder(BorderFactory.createTitledBorder(MessageBundle.format("shp_attributs"))); // NOI18N

        guiTable.setModel(model);
        jScrollPane1.setViewportView(guiTable);

        guiAdd.setText(MessageBundle.format("shp_add")); // NOI18N
        guiAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                addAction(evt);
            }
        });

        guiUp.setText(MessageBundle.format("shp_up")); // NOI18N
        guiUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                upAction(evt);
            }
        });

        guiDown.setText(MessageBundle.format("shp_down")); // NOI18N
        guiDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                downAction(evt);
            }
        });

        guiDelete.setText(MessageBundle.format("shp_delete")); // NOI18N
        guiDelete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                deleteAction(evt);
            }
        });

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(guiAdd)
                    .addComponent(guiUp)
                    .addComponent(guiDown)
                    .addComponent(guiDelete)))
        );

        jPanel2Layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {guiAdd, guiDelete, guiDown, guiUp});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(guiAdd)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(guiDelete)
                .addGap(24, 24, 24)
                .addComponent(guiUp)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(guiDown)
                .addContainerGap(139, Short.MAX_VALUE))
            .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
        );

        jPanel1.setBorder(BorderFactory.createTitledBorder(MessageBundle.format("shp_geometry"))); // NOI18N

        guiGeom.add(guiPoint);
        guiPoint.setSelected(true);
        guiPoint.setText(MessageBundle.format("shp_point")); // NOI18N
        guiPoint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                geomPointAction(evt);
            }
        });

        guiGeom.add(guiMultiPoint);
        guiMultiPoint.setText(MessageBundle.format("shp_multipoint")); // NOI18N
        guiMultiPoint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                geomMultiPointAction(evt);
            }
        });

        guiGeom.add(guiMultiLine);
        guiMultiLine.setText(MessageBundle.format("shp_multiline")); // NOI18N
        guiMultiLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                geomMultilineAction(evt);
            }
        });

        guiGeom.add(guiMultiPolygon);
        guiMultiPolygon.setText(MessageBundle.format("shp_multipolygon")); // NOI18N
        guiMultiPolygon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                geomMultiPolygonAction(evt);
            }
        });

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(guiPoint)
                    .addComponent(guiMultiPoint)
                    .addComponent(guiMultiLine)
                    .addComponent(guiMultiPolygon))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(guiPoint)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(guiMultiPoint)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(guiMultiLine)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(guiMultiPolygon)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        guiCrsChoose.setText(MessageBundle.format("shp_list")); // NOI18N
        guiCrsChoose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                crsAction(evt);
            }
        });

        jLabel2.setText(MessageBundle.format("shp_crs")); // NOI18N

        guiCrs.setEditable(false);
        guiCrs.setText("EPSG:4326");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jXTitledSeparator1, GroupLayout.DEFAULT_SIZE, 642, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiFileName, GroupLayout.DEFAULT_SIZE, 571, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiFileChoose))
                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                            .addComponent(guiCreate)
                            .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiCrs, GroupLayout.DEFAULT_SIZE, 556, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiCrsChoose)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jXTitledSeparator1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(guiFileChoose)
                    .addComponent(guiFileName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(guiCrsChoose)
                    .addComponent(guiCrs, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED, 176, Short.MAX_VALUE)
                        .addComponent(guiCreate))
                    .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    private void createAction(final ActionEvent evt) {//GEN-FIRST:event_createAction
        createShape(guiFileName.getText());
    }//GEN-LAST:event_createAction

    private void addAction(final ActionEvent evt) {//GEN-FIRST:event_addAction
        model.addAttribut();
    }//GEN-LAST:event_addAction

    private void deleteAction(final ActionEvent evt) {//GEN-FIRST:event_deleteAction

        final int selected = guiTable.getSelectionModel().getMinSelectionIndex();
        if (selected >= 0) {
            final Field data = model.getDataAt(selected);
            model.deleteAttribut(data);
        }

    }//GEN-LAST:event_deleteAction

    private void upAction(final ActionEvent evt) {//GEN-FIRST:event_upAction
        final int selected = guiTable.getSelectionModel().getMinSelectionIndex();
        if (selected >= 0) {
            final Field data = model.getDataAt(selected);
            model.moveUp(data);
        }
    }//GEN-LAST:event_upAction

    private void downAction(final ActionEvent evt) {//GEN-FIRST:event_downAction
        final int selected = guiTable.getSelectionModel().getMinSelectionIndex();
        if (selected >= 0) {
            final Field data = model.getDataAt(selected);
            model.moveDown(data);
        }
    }//GEN-LAST:event_downAction

    private void geomPointAction(final ActionEvent evt) {//GEN-FIRST:event_geomPointAction
        geotype = Point.class;
    }//GEN-LAST:event_geomPointAction

    private void geomMultiPointAction(final ActionEvent evt) {//GEN-FIRST:event_geomMultiPointAction
        geotype = MultiPoint.class;
    }//GEN-LAST:event_geomMultiPointAction

    private void geomMultilineAction(final ActionEvent evt) {//GEN-FIRST:event_geomMultilineAction
        geotype = MultiLineString.class;
    }//GEN-LAST:event_geomMultilineAction

    private void geomMultiPolygonAction(final ActionEvent evt) {//GEN-FIRST:event_geomMultiPolygonAction
        geotype = MultiPolygon.class;
    }//GEN-LAST:event_geomMultiPolygonAction

    private void crsAction(final ActionEvent evt) {//GEN-FIRST:event_crsAction

        final Window frame = SwingUtilities.windowForComponent(this);
        final JCRSChooser jcrs = JCRSChooser.create(frame, true);
        jcrs.setCRS(crs);
        final JCRSChooser.ACTION act = jcrs.showDialog();

        if (act == JCRSChooser.ACTION.APPROVE) {
            crs = jcrs.getCRS();
        }

        guiCrs.setText(crs.getName().toString());

    }//GEN-LAST:event_crsAction

    private void fileAction(final ActionEvent evt) {//GEN-FIRST:event_fileAction
        final JFileChooser jfc = new JFileChooser(file);
        final int act = jfc.showSaveDialog(null);

        if (act == JFileChooser.APPROVE_OPTION) {
            File f = jfc.getSelectedFile();

            if (f != null) {
                if (f.getAbsolutePath().endsWith(".shp")) {
                    file = f;
                    guiFileName.setText(file.getAbsolutePath());
                } else {
                    int lastdot = f.getAbsolutePath().lastIndexOf(".");
                    if(lastdot>0){
                        f = new File(f.getAbsolutePath().substring(0,lastdot) +".shp");
                    }else{
                        f = new File(f.getAbsolutePath() +".shp");
                    }

                    file = f;
                    guiFileName.setText(file.getAbsolutePath());
                }
            }

        }

    }//GEN-LAST:event_fileAction

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private final JTextField guiCrs = new JTextField();
    private final JTextField guiFileName = new JTextField();
    private final JXTable guiTable = new JXTable();
    // End of variables declaration//GEN-END:variables
}
