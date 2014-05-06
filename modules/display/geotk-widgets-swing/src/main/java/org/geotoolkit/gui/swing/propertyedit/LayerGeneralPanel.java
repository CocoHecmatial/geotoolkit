/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007 - 2009, Johann Sorel
 *    (C) 2010 - 2013, Geomatys
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
package org.geotoolkit.gui.swing.propertyedit;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.measure.unit.Unit;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.gui.swing.util.ActionCell;
import org.geotoolkit.gui.swing.resource.IconBundle;
import org.geotoolkit.gui.swing.resource.MessageBundle;
import org.geotoolkit.gui.swing.style.JTextExpressionPane;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.FeatureMapLayer.DimensionDef;
import org.geotoolkit.map.MapLayer;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.cs.AbstractCS;
import org.geotoolkit.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.geotoolkit.gui.swing.resource.FontAwesomeIcons;
import org.geotoolkit.gui.swing.resource.IconBuilder;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;
import org.netbeans.swing.outline.RenderDataProvider;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.EngineeringDatum;


/**
 * layer general information panel
 *
 * @author Johann Sorel
 * @author Cédric Briançon (Geomatys)
 * @module pending
 */
public class LayerGeneralPanel extends AbstractPropertyPane {

    private MapLayer layer = null;

    private static final ImageIcon ICON_DELETE = IconBuilder.createIcon(FontAwesomeIcons.ICON_TRASH, 16, Color.BLACK);

    /** Creates new form LayerGeneralPanel */
    public LayerGeneralPanel() {
        super(MessageBundle.getString("property_general_title"),null,null,MessageBundle.getString("property_general_title"));
        initComponents();

        guiTable.setDefaultRenderer(LayerGeneralTableRowModel.CrsCookie.class, new CrsRenderer());
        guiTable.setDefaultRenderer(LayerGeneralTableRowModel.LowerCookie.class, new ExpressionLowerRenderer());
        guiTable.setDefaultRenderer(LayerGeneralTableRowModel.UpperCookie.class, new ExpressionUpperRenderer());

        guiTable.setDefaultRenderer(LayerGeneralTableRowModel.DeleteCookie.class, new ActionCell.Renderer(ICON_DELETE));
        guiTable.setDefaultEditor(LayerGeneralTableRowModel.DeleteCookie.class, new ActionCell.Editor(ICON_DELETE) {
            @Override
            public void actionPerformed(final ActionEvent e, Object value) {
                if (value instanceof DefaultMutableTreeNode){
                    value = ((DefaultMutableTreeNode)value).getUserObject();
                }
                if (value instanceof DimensionDef) {
                    final DimensionDef dimDef = (DimensionDef)value;
                    removeDimensionFromLayer(dimDef);
                    updateTableModel();
                }

            }
        });

        guiTable.setRenderDataProvider(new DimensionRenderer());
        guiTable.setFillsViewportHeight(true);
        guiTable.setShowVerticalLines(false);

        updateTableModel();
    }

    private void removeDimensionFromLayer(final DimensionDef dimension) {
        if (!(layer instanceof FeatureMapLayer)) {
            return;
        }

        final FeatureMapLayer fml = (FeatureMapLayer)layer;
        fml.getExtraDimensions().remove(dimension);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel15 = new JLabel();
        gui_jtf_name = new JTextField();
        jPanelDimensions = new JPanel();
        jPanelAddDimension = new JPanel();
        guiTextExpressionUpper = new JTextExpressionPane();
        guiTextExpressionLower = new JTextExpressionPane();
        jLabel1 = new JLabel();
        jLabel2 = new JLabel();
        jLabel3 = new JLabel();
        guiBtnAddDimension = new JButton();
        guiCrsName = new JTextField();
        jScrollPane1 = new JScrollPane();
        guiTable = new Outline();

        jLabel15.setFont(jLabel15.getFont().deriveFont(jLabel15.getFont().getStyle() | Font.BOLD));
        jLabel15.setText(MessageBundle.getString("property_title")); // NOI18N

        jPanelDimensions.setName("New attribute"); // NOI18N

        jLabel1.setText("Crs");

        jLabel2.setText("lower");

        jLabel3.setText("upper");

        guiBtnAddDimension.setText(MessageBundle.getString("add_dimension")); // NOI18N
        guiBtnAddDimension.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                guiBtnAddDimensionActionPerformed(evt);
            }
        });

        GroupLayout jPanelAddDimensionLayout = new GroupLayout(jPanelAddDimension);
        jPanelAddDimension.setLayout(jPanelAddDimensionLayout);
        jPanelAddDimensionLayout.setHorizontalGroup(
            jPanelAddDimensionLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanelAddDimensionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelAddDimensionLayout.createParallelGroup(Alignment.TRAILING, false)
                    .addComponent(guiBtnAddDimension, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelAddDimensionLayout.createSequentialGroup()
                        .addGroup(jPanelAddDimensionLayout.createParallelGroup(Alignment.LEADING)
                            .addGroup(jPanelAddDimensionLayout.createSequentialGroup()
                                .addGroup(jPanelAddDimensionLayout.createParallelGroup(Alignment.LEADING)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel2))
                                .addGap(20, 20, 20))
                            .addGroup(Alignment.TRAILING, jPanelAddDimensionLayout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(18, 18, 18)))
                        .addGroup(jPanelAddDimensionLayout.createParallelGroup(Alignment.LEADING, false)
                            .addComponent(guiTextExpressionUpper, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(guiTextExpressionLower, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(guiCrsName))))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelAddDimensionLayout.setVerticalGroup(
            jPanelAddDimensionLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanelAddDimensionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelAddDimensionLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(guiCrsName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanelAddDimensionLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(guiTextExpressionLower, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanelAddDimensionLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(guiTextExpressionUpper, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(18, 18, 18)
                .addComponent(guiBtnAddDimension)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(guiTable);

        GroupLayout jPanelDimensionsLayout = new GroupLayout(jPanelDimensions);
        jPanelDimensions.setLayout(jPanelDimensionsLayout);
        jPanelDimensionsLayout.setHorizontalGroup(
            jPanelDimensionsLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanelDimensionsLayout.createSequentialGroup()
                .addComponent(jPanelAddDimension, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 618, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelDimensionsLayout.setVerticalGroup(
            jPanelDimensionsLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanelDimensionsLayout.createSequentialGroup()
                .addGroup(jPanelDimensionsLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanelDimensionsLayout.createSequentialGroup()
                        .addComponent(jPanelAddDimension, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel15)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(gui_jtf_name)
                .addContainerGap())
            .addComponent(jPanelDimensions, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(gui_jtf_name, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanelDimensions, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelDimensions.getAccessibleContext().setAccessibleName("New attribute");
    }// </editor-fold>//GEN-END:initComponents

    private void guiBtnAddDimensionActionPerformed(ActionEvent evt) {//GEN-FIRST:event_guiBtnAddDimensionActionPerformed
        if (!(layer instanceof FeatureMapLayer)) {
            return;
        }

        final FeatureMapLayer fml = (FeatureMapLayer)layer;
        final CoordinateReferenceSystem crs = buildCrs1DFromName(guiCrsName.getText());
        final DimensionDef dimToAdd = new DimensionDef(crs, guiTextExpressionLower.create(), guiTextExpressionUpper.create());
        if (!fml.getExtraDimensions().contains(dimToAdd)) {
            fml.getExtraDimensions().add(dimToAdd);
        }

        updateTableModel();
    }//GEN-LAST:event_guiBtnAddDimensionActionPerformed

    private CoordinateReferenceSystem buildCrs1DFromName(final String crsName) {
        final EngineeringDatum customDatum = new DefaultEngineeringDatum(Collections.singletonMap("name", crsName));
        final CoordinateSystemAxis csAxis = new DefaultCoordinateSystemAxis(crsName, "u", AxisDirection.valueOf(crsName), Unit.ONE);
        final AbstractCS customCs = new AbstractCS(Collections.singletonMap("name", crsName), csAxis);
        return new DefaultEngineeringCRS(Collections.singletonMap("name", crsName), customDatum, customCs);
    }

    private void parse() {

        if (layer != null) {
            gui_jtf_name.setText(layer.getDescription().getTitle().toString());

        } else {
            gui_jtf_name.setText("");
        }

        // Do not display the whole panel if not a feature map layer
        final boolean isFeature = (layer instanceof FeatureMapLayer);
        jPanelDimensions.setVisible(isFeature);

        if (isFeature) {
            guiTextExpressionLower.setLayer(layer);
            guiTextExpressionUpper.setLayer(layer);
            updateTableModel();
        }
    }

    @Override
    public void setTarget(final Object target) {
        if (target instanceof MapLayer) {
            layer = (MapLayer) target;
        } else {
            layer = null;
        }
        parse();
    }

    @Override
    public void apply() {
        if (layer != null) {
            layer.setDescription(FactoryFinder.getStyleFactory(null).description(
                    new SimpleInternationalString(gui_jtf_name.getText()),
                    new SimpleInternationalString("")));

            if (layer instanceof FeatureMapLayer) {
                final FeatureMapLayer fml = (FeatureMapLayer)layer;
                final OutlineModel model = (OutlineModel) guiTable.getModel();

                final List<DimensionDef> dimensions = new ArrayList<DimensionDef>();

                final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) model.getRoot();
                for (int i=0; i < rootNode.getChildCount(); i++) {
                    final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
                    if (childNode.getUserObject() instanceof DimensionDef) {
                        final DimensionDef dim = (DimensionDef) childNode.getUserObject();
                        dimensions.add(dim);
                    }
                }

                if (!dimensions.isEmpty()) {
                    fml.getExtraDimensions().clear();
                    fml.getExtraDimensions().addAll(dimensions);
                }
            }
        }
    }

    @Override
    public void reset() {
        parse();
    }

    @Override
    public boolean canHandle(Object target) {
        return true;
    }

    private void updateTableModel() {
        if (!(layer instanceof FeatureMapLayer)) {
            return;
        }

        final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        final DefaultTreeModel treeModel = new org.geotoolkit.gui.swing.tree.DefaultTreeModel(root);

        final FeatureMapLayer fml = (FeatureMapLayer)layer;
        for (DimensionDef dim : fml.getExtraDimensions()) {
            root.add(new DefaultMutableTreeNode(dim, false));
        }

        final LayerGeneralTableRowModel model = new LayerGeneralTableRowModel();

        guiTable.setRootVisible(false);
        guiTable.setModel(DefaultOutlineModel.createOutlineModel(treeModel, model));
        guiTable.repaint();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton guiBtnAddDimension;
    private JTextField guiCrsName;
    private Outline guiTable;
    private JTextExpressionPane guiTextExpressionLower;
    private JTextExpressionPane guiTextExpressionUpper;
    private JTextField gui_jtf_name;
    private JLabel jLabel1;
    private JLabel jLabel15;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JPanel jPanelAddDimension;
    private JPanel jPanelDimensions;
    private JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

    private static class DimensionRenderer implements RenderDataProvider{

        @Override
        public String getDisplayName(Object o) {
            return "";
        }

        @Override
        public boolean isHtmlDisplayName(Object o) {
            return true;
        }

        @Override
        public Color getBackground(Object o) {
            return null;
        }

        @Override
        public Color getForeground(Object o) {
            return null;
        }

        @Override
        public String getTooltipText(Object o) {
            return null;
        }

        @Override
        public Icon getIcon(Object o) {
            return IconBundle.EMPTY_ICON_16;
        }

    }

    private static class ExpressionLowerRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setText("");
            if (value instanceof DefaultMutableTreeNode) {
                value = ((DefaultMutableTreeNode)value).getUserObject();
            }
            if (value instanceof DimensionDef) {
                final DimensionDef def = (DimensionDef)value;
                if (def.getLower() != null) {
                    lbl.setText(def.getLower().toString());
                }
            }
            return lbl;
        }

    }

    private static class ExpressionUpperRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setText("");
            if (value instanceof DefaultMutableTreeNode) {
                value = ((DefaultMutableTreeNode)value).getUserObject();
            }
            if (value instanceof DimensionDef) {
                final DimensionDef def = (DimensionDef)value;
                if (def.getUpper() != null) {
                    lbl.setText(def.getUpper().toString());
                }
            }
            return lbl;
        }

    }

    private static class CrsRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setText("");
            if (value instanceof DefaultMutableTreeNode) {
                value = ((DefaultMutableTreeNode)value).getUserObject();
            }
            if (value instanceof DimensionDef) {
                final DimensionDef def = (DimensionDef)value;
                if (def.getCrs() != null) {
                    lbl.setText(def.getCrs().getName().toString());
                }
            }
            return lbl;
        }

    }
}
