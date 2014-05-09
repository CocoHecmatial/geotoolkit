/*//GEN-FIRST:event_guiRemoveAllActionPerformed
 *    Geotoolkit - An Open Source Java GIS Toolkit//GEN-LAST:event_guiRemoveAllActionPerformed
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2014 Geomatys
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

package org.geotoolkit.gui.swing.propertyedit.styleproperty;


import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.coverage.CoverageReference;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.filter.DefaultLiteral;
import org.geotoolkit.gui.swing.propertyedit.AbstractPropertyPane;
import org.geotoolkit.gui.swing.resource.FontAwesomeIcons;
import org.geotoolkit.gui.swing.resource.IconBuilder;
import org.geotoolkit.gui.swing.resource.IconBundle;
import org.geotoolkit.gui.swing.resource.MessageBundle;
import org.geotoolkit.gui.swing.util.ColorCellEditor;
import org.geotoolkit.gui.swing.util.ColorCellRenderer;
import org.geotoolkit.gui.swing.util.NumberAlignRenderer;
import org.geotoolkit.image.io.PaletteFactory;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.process.coverage.copy.StatisticOp;
import org.geotoolkit.style.MutableFeatureTypeStyle;
import org.geotoolkit.style.MutableRule;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.style.StyleConstants;

import static org.geotoolkit.style.StyleConstants.*;
import org.geotoolkit.style.function.Categorize;
import org.geotoolkit.style.function.DefaultInterpolate;
import org.geotoolkit.style.function.DefaultInterpolationPoint;
import org.geotoolkit.style.function.Interpolate;
import org.geotoolkit.style.function.InterpolationPoint;
import org.geotoolkit.style.function.Jenks;
import org.geotoolkit.style.function.Method;
import org.geotoolkit.style.function.Mode;
import org.geotoolkit.style.function.ThreshholdsBelongTo;
import org.geotoolkit.style.interval.DefaultIntervalPalette;
import org.geotoolkit.style.interval.DefaultRandomPalette;
import org.geotoolkit.style.interval.Palette;
import org.geotoolkit.util.Converters;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.combobox.ListComboBoxModel;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.style.ChannelSelection;
import org.opengis.style.ColorMap;
import org.opengis.style.ContrastEnhancement;
import org.opengis.style.ContrastMethod;
import org.opengis.style.Description;
import org.opengis.style.OverlapBehavior;
import org.opengis.style.RasterSymbolizer;
import org.opengis.style.SelectedChannelType;
import org.opengis.style.ShadedRelief;
import org.opengis.style.Symbolizer;

/**
 * Style editor which handle Raster colormap edition.
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class JRasterColorMapStylePanel extends AbstractPropertyPane{

    private static final Logger LOGGER = Logging.getLogger(JRasterColorMapStylePanel.class);

    private static final PaletteFactory PF = PaletteFactory.getDefault();
    private static final List<Object> PALETTES = new ArrayList<>();
    private static final List<Object> PALETTES_NAMED = new ArrayList<>();

    static{
        PALETTES.add(new DefaultRandomPalette());
        final Set<String> paletteNames = PF.getAvailableNames();

        for (String palName : paletteNames) {
            PALETTES.add(palName);
            PALETTES_NAMED.add(palName);
        }

        double[] fractions = new double[]{
            -3000,
            -1500,
            -0.1,
            +0,
            556,
            1100,
            1600,
            2200,
            3000};
        Color[] colors = new Color[]{
            new Color(9, 9, 145, 255),
            new Color(31, 131, 224, 255),
            new Color(182, 240, 240, 255),
            new Color(5, 90, 5, 255),
            new Color(150, 200, 150, 255),
            new Color(190, 150, 20, 255),
            new Color(100, 100, 50, 255),
            new Color(200, 210, 220, 255),
            new Color(255, 255, 255, 255),
            };
        PALETTES.add(new DefaultIntervalPalette(fractions,colors));

    }

    private static final MutableStyleFactory SF = (MutableStyleFactory) FactoryFinder.getStyleFactory(new Hints(Hints.STYLE_FACTORY, MutableStyleFactory.class));
    private static final FilterFactory FF = FactoryFinder.getFilterFactory(null);
    private static final Literal TRS = SF.literal(new Color(0, 0, 0, 0));

    private ColorMapModel model = new InterpolateColorModel(Collections.EMPTY_LIST);
    private MapLayer layer = null;
    //keep track of where the symbolizer was to avoid rewriting the complete style
    private MutableRule parentRule = null;
    private int parentIndex = 0;
    
    private String name = "";
    private String desc = "";
    private List<Object> currentPaletteList = null;

    public JRasterColorMapStylePanel() {
        super(MessageBundle.getString("property_style_colormap"), 
              IconBundle.getIcon("16_classification_single"), 
              null, "");
        initComponents();
        setPalettes(PALETTES);
        guiPalette.setRenderer(new PaletteCellRenderer());
        guiPalette.setSelectedIndex(0);
        guiTable.setShowGrid(false, false);
        
        final List<Class> methods = new ArrayList<>();
        methods.add(Interpolate.class);
        methods.add(Categorize.class);
        methods.add(Jenks.class);
        guiMethod.setModel(new ListComboBoxModel(methods));
        guiMethod.setRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if(value instanceof Class){
                    lbl.setText(((Class)value).getSimpleName());
                }
                return lbl;
            }
        });
        parse(null);
    }

    private void setPalettes(List<Object> palettes){
        if(currentPaletteList == palettes) return;
        this.currentPaletteList = palettes;
        guiPalette.setModel(new ListComboBoxModel(palettes));
    }
    
    private void parse(){
        guiTable.revalidate();
        guiTable.repaint();
        guiNaN.setSelected(true);

        RasterSymbolizer rs = null;
        parentRule = null;
        parentIndex = 0;
        search:
        if(layer != null){
            for(final MutableFeatureTypeStyle fts : layer.getStyle().featureTypeStyles()){
                for(MutableRule r : fts.rules()){
                    for(int i=0,n=r.symbolizers().size();i<n;i++){
                        Symbolizer s = r.symbolizers().get(i);
                        if(s instanceof RasterSymbolizer){
                            rs = (RasterSymbolizer) s;
                            parentRule = r;
                            parentIndex = i;
                            break search;
                        }
                    }
                }
            }
        }

        parse(rs);
    }

    private void parse(RasterSymbolizer rs){
        //find channel
        if(rs!=null){
            final ChannelSelection selection = rs.getChannelSelection();
            guiBand.setValue(0);
            if(selection!=null){
                final SelectedChannelType sct = selection.getGrayChannel();
                if(sct!=null){
                    try{
                        guiBand.setValue(Integer.valueOf(sct.getChannelName()));
                    }catch(Exception ex){ //nullpointer or numberformat exception
                        LOGGER.log(Level.INFO, "chanel name is not a number : {0}", sct.getChannelName());
                    }
                }
            }
        }

        model = null;
        name = "";
        desc = "";

        if(rs != null){
            name = rs.getName();
            desc = rs.getDescription()== null ? "" : ((rs.getDescription().getTitle()==null) ? "" : rs.getDescription().getTitle().toString());

            if(rs.getColorMap()!=null && rs.getColorMap().getFunction()!=null){
                final Function fct = rs.getColorMap().getFunction();
                if(fct instanceof Interpolate){
                    final List<InterpolationPoint> points = ((Interpolate)fct).getInterpolationPoints();
                    model = new InterpolateColorModel(points);
                }else if(fct instanceof Categorize){
                    final Map<Expression,Expression> th = ((Categorize)fct).getThresholds();
                    model = new CategorizeColorModel(th);
                }else{
                    model = new InterpolateColorModel(Collections.EMPTY_LIST);
                    LOGGER.log(Level.WARNING, "Unknowned colormap function : {0}", fct);
                }
            }else{
                //create an empty interpolate colormodel
                model = new InterpolateColorModel(Collections.EMPTY_LIST);
            }
        }else{
            //create an empty interpolate colormodel
            model = new InterpolateColorModel(Collections.EMPTY_LIST);
        }
        
        postParse();
    }

    private void postParse(){
        guiTable.setModel(model);
        if(model instanceof InterpolateColorModel){
            guiMethod.setSelectedItem(Interpolate.class);
            setPalettes(PALETTES);
            guiTable.getColumnModel().getColumn(0).setCellRenderer(new NumberAlignRenderer());
            guiTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JTextField()));
            guiTable.getColumnModel().getColumn(1).setCellRenderer(new ColorCellRenderer());
            guiTable.getColumnModel().getColumn(1).setCellEditor(new ColorCellEditor());
            guiTable.getColumnModel().getColumn(2).setCellRenderer(new DeleteRenderer());
            guiTable.getColumnModel().getColumn(2).setCellEditor(new DeleteEditor());
            guiTable.getColumnExt(2).setMaxWidth(20);
        }else if(model instanceof CategorizeColorModel){
            guiMethod.setSelectedItem(Categorize.class);
            setPalettes(PALETTES);
            guiTable.getColumnModel().getColumn(0).setCellRenderer(new NumberAlignRenderer());
            guiTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JTextField()));
            guiTable.getColumnModel().getColumn(1).setCellRenderer(new ColorCellRenderer());
            guiTable.getColumnModel().getColumn(1).setCellEditor(new ColorCellEditor());
            guiTable.getColumnModel().getColumn(2).setCellRenderer(new NumberAlignRenderer());
            guiTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JTextField()));
            guiTable.getColumnModel().getColumn(3).setCellRenderer(new DeleteRenderer());
            guiTable.getColumnModel().getColumn(3).setCellEditor(new DeleteEditor());
            guiTable.getColumnExt(3).setMaxWidth(20);
        }else if(model instanceof JenksColorModel){
            guiMethod.setSelectedItem(Jenks.class);
            setPalettes(PALETTES_NAMED);
        }

        //disable and hide value table for jenks method
        guiAddOne.setVisible(!(model instanceof JenksColorModel));
        guiRemoveAll.setVisible(!(model instanceof JenksColorModel));
        guiTableScroll.setVisible(!(model instanceof JenksColorModel));
        guiJenksMessage.setVisible(model instanceof JenksColorModel);

        if(layer instanceof CoverageMapLayer){
            guiLblPalette.setVisible(true);
            guiPalette.setVisible(true);
            guiBand.setVisible(true);
            guiLblBand.setVisible(true);
            guiNaN.setVisible(true);
            guiInvert.setVisible(true);
            guiGenerate.setVisible(true);
            guiLblStep.setVisible(true);
            guiNbStep.setVisible(true);

        }else{
            guiLblPalette.setVisible(false);
            guiPalette.setVisible(false);
            guiBand.setVisible(false);
            guiLblBand.setVisible(false);
            guiNaN.setVisible(false);
            guiInvert.setVisible(false);
            guiGenerate.setVisible(false);
            guiLblStep.setVisible(false);
            guiNbStep.setVisible(false);
        }

        revalidate();
        repaint();
    }

    private void initializeSpinners() {
        if(layer != null && layer instanceof CoverageMapLayer){
            final CoverageMapLayer cml = (CoverageMapLayer)layer;
            final CoverageReference cref = cml.getCoverageReference();
            GridCoverageReader reader = null;
            try {
                reader = cref.acquireReader();
                final Map<String,Object> an = StatisticOp.analyze(reader,cref.getImageIndex());
                final double[] minArray = (double[])an.get(StatisticOp.MINIMUM);
                final double[] maxArray = (double[])an.get(StatisticOp.MAXIMUM);
                final double min = findExtremum(minArray, true);
                final double max = findExtremum(maxArray, false);

                final SpinnerModel minModel = 
                        new SpinnerNumberModel(min, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.1d);
                final SpinnerModel maxModel = 
                        new SpinnerNumberModel(max, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.1d);
                guiMinSpinner.setModel(minModel);
                guiMaxSpinner.setModel(maxModel);
                cref.recycle(reader);
            } catch (CoverageStoreException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(),ex);
            }
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

        guiAddOne = new JButton();
        guiRemoveAll = new JButton();
        jPanel1 = new JPanel();
        guiNaN = new JCheckBox();
        guiLblPalette = new JLabel();
        guiGenerate = new JButton();
        guiPalette = new JComboBox();
        guiInvert = new JCheckBox();
        guiLblBand = new JLabel();
        guiBand = new JSpinner();
        guiLblStep = new JLabel();
        guiNbStep = new JSpinner();
        jLabel1 = new JLabel();
        guiMethod = new JComboBox();
        minLabel = new JLabel();
        guiMinSpinner = new JSpinner();
        maxLabel = new JLabel();
        guiMaxSpinner = new JSpinner();
        guiFitToData = new JButton();
        guiTableScroll = new JScrollPane();
        guiTable = new JXTable();
        guiJenksMessage = new JLabel();

        guiAddOne.setText(MessageBundle.getString("add_value")); // NOI18N
        guiAddOne.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                guiAddOneActionPerformed(evt);
            }
        });

        guiRemoveAll.setText(MessageBundle.getString("remove_all_values")); // NOI18N
        guiRemoveAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                guiRemoveAllActionPerformed(evt);
            }
        });

        jPanel1.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));

        guiNaN.setSelected(true);
        guiNaN.setText(MessageBundle.getString("style.rastercolormappane.nan")); // NOI18N
        guiNaN.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                guiNaNActionPerformed(evt);
            }
        });

        guiLblPalette.setText(MessageBundle.getString("style.rastercolormappane.palette")); // NOI18N

        guiGenerate.setText(MessageBundle.getString("generate")); // NOI18N
        guiGenerate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                guiGenerateActionPerformed(evt);
            }
        });

        guiInvert.setText(MessageBundle.getString("style.rastercolormappane.invert")); // NOI18N

        guiLblBand.setHorizontalAlignment(SwingConstants.RIGHT);
        guiLblBand.setText(MessageBundle.getString("style.rastercolormappane.band")); // NOI18N

        guiBand.setModel(new SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        guiLblStep.setText(MessageBundle.getString("style.rastersymbolizer.divisions")); // NOI18N

        guiNbStep.setModel(new SpinnerNumberModel(Integer.valueOf(3), Integer.valueOf(2), null, Integer.valueOf(1)));

        jLabel1.setText(MessageBundle.getString("method")); // NOI18N

        guiMethod.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                guiMethodItemStateChanged(evt);
            }
        });

        minLabel.setText(MessageBundle.getString("minimum")); // NOI18N

        guiMinSpinner.setModel(new SpinnerNumberModel(Double.valueOf(0.0d), null, null, Double.valueOf(0.0d)));

        maxLabel.setText(MessageBundle.getString("maximum")); // NOI18N

        guiMaxSpinner.setModel(new SpinnerNumberModel(Double.valueOf(0.0d), null, null, Double.valueOf(1.0d)));

        guiFitToData.setText(MessageBundle.getString("style.rastercolormappane.fittodata")); // NOI18N
        guiFitToData.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                guiFitToDataActionPerformed(evt);
            }
        });

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(guiLblPalette)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiPalette, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(guiFitToData)
                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(guiGenerate))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(minLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiMinSpinner, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(maxLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiMaxSpinner, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(guiLblStep)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiNbStep, GroupLayout.PREFERRED_SIZE, 62, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiLblBand)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiBand, GroupLayout.PREFERRED_SIZE, 51, GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiMethod, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addComponent(guiNaN)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(guiInvert))))
        );

        jPanel1Layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {guiBand, guiMaxSpinner, guiMinSpinner, guiNbStep});

        jPanel1Layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {guiInvert, guiLblBand, guiLblPalette, guiLblStep, guiNaN, jLabel1, maxLabel, minLabel});

        jPanel1Layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {guiFitToData, guiGenerate});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(guiPalette, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(guiLblPalette, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(guiMethod, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(guiNaN)
                    .addComponent(guiInvert))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(guiLblBand)
                    .addComponent(guiBand, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(guiLblStep)
                    .addComponent(guiNbStep, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(minLabel)
                    .addComponent(guiMinSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(maxLabel)
                    .addComponent(guiMaxSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(guiGenerate)
                    .addComponent(guiFitToData)))
        );

        guiTableScroll.setViewportView(guiTable);

        guiJenksMessage.setFont(guiJenksMessage.getFont().deriveFont((guiJenksMessage.getFont().getStyle() | Font.ITALIC) | Font.BOLD, guiJenksMessage.getFont().getSize()+1));
        guiJenksMessage.setHorizontalAlignment(SwingConstants.CENTER);
        guiJenksMessage.setText(MessageBundle.getString("jenks.notable")); // NOI18N

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(guiAddOne)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(guiRemoveAll)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(guiJenksMessage, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(guiTableScroll, Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(guiJenksMessage)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(guiTableScroll, GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(guiAddOne)
                    .addComponent(guiRemoveAll)))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * And or Remove NaN value in the model
     * @param evt 
     */
    private void guiNaNActionPerformed(ActionEvent evt) {//GEN-FIRST:event_guiNaNActionPerformed
        
        final boolean withNaN = guiNaN.isSelected();
        
        if(model instanceof CategorizeColorModel){
            final Map<Expression,Expression> values = new HashMap<>();
            final List<Entry<Expression, Expression>> ths = ((CategorizeColorModel)model).ths;
            for(int i=0,n=ths.size();i<n;i++){
                final Entry<Expression, Expression> entry = ths.get(i);
                
                final Object num = ((Literal)entry.getKey()).getValue();
                if(num instanceof Number && (Double.isNaN(((Number)num).doubleValue()) || Float.isNaN(((Number)num).floatValue()))){
                    if(withNaN){
                        //color model already has a NaN
                        return;
                    }else{
                        //remove it
                    }
                }else{
                    values.put(entry.getKey(), entry.getValue());
                }
            }
            
            if(withNaN){
                //add NaN entry
                values.put(new DefaultLiteral<Number>(Float.NaN), TRS);
            }
            
            model = new CategorizeColorModel(values);
            postParse();
            
        }else if(model instanceof InterpolateColorModel){
            //we need to convert from interpolate to categorize
            final List<InterpolationPoint> newPoints = new ArrayList<>();
            final List<InterpolationPoint> points = ((InterpolateColorModel)model).points;
            for(InterpolationPoint pt : points){
                final Number num = pt.getData();
                if(Double.isNaN(num.doubleValue()) || Float.isNaN(num.floatValue())){
                    if(withNaN){
                        //color model already has a NaN
                        return;
                    }else{
                        //remove it
                    }
                }else{
                    newPoints.add(pt);
                }
            }
            
            if(withNaN){
                //add NaN entry
                newPoints.add(SF.interpolationPoint(Float.NaN, TRS));
            }
            
            model = new InterpolateColorModel(newPoints);
            postParse();
        }
        
        
    }//GEN-LAST:event_guiNaNActionPerformed

    private void guiMethodItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_guiMethodItemStateChanged
        final Object method = guiMethod.getSelectedItem();
        
        if(Interpolate.class.equals(method)){
            if(model instanceof InterpolateColorModel){
                //nothing to do
            }else if(model instanceof CategorizeColorModel){
                //we need to convert from categorize thredholds to interpolation points.
                final List<InterpolationPoint> points = new ArrayList<>();
                final List<Entry<Expression, Expression>> ths = ((CategorizeColorModel)model).ths;
                for(int i=1,n=ths.size();i<n;i++){
                    final Entry<Expression, Expression> entry = ths.get(i);
                    points.add(SF.interpolationPoint(entry.getKey().evaluate(n, Number.class), entry.getValue()));
                }
                model = new InterpolateColorModel(points);
                postParse();
            }else{
                model = new InterpolateColorModel(Collections.EMPTY_LIST);
                postParse();
            }
        }else if(Categorize.class.equals(method)){
            if(model instanceof CategorizeColorModel){
                //nothing to do
            }else if(model instanceof InterpolateColorModel){
                //we need to convert from interpolate to categorize
                final Map<Expression, Expression> values = new HashMap<>();
                values.put( StyleConstants.CATEGORIZE_LESS_INFINITY, TRS);
                final List<InterpolationPoint> points = ((InterpolateColorModel)model).points;
                for(InterpolationPoint pt : points){
                    values.put(new DefaultLiteral(pt.getData()), pt.getValue());
                }
                model = new CategorizeColorModel(values);
                postParse();
            }else{
                final Map<Expression, Expression> values = new HashMap<>();
                values.put(StyleConstants.CATEGORIZE_LESS_INFINITY, TRS);
                values.put(new DefaultLiteral<Number>(0), TRS);
                model = new CategorizeColorModel(values);
                postParse();
            }
        }else{
            if(model instanceof JenksColorModel){
                //nothing to do
            }else{
                model = new JenksColorModel();
                postParse();
            }
        }
        
        //ensure the NaN is set as defined
        guiNaNActionPerformed(null);
    }//GEN-LAST:event_guiMethodItemStateChanged

    private void guiFitToDataActionPerformed(ActionEvent evt) {//GEN-FIRST:event_guiFitToDataActionPerformed
        initializeSpinners();
    }//GEN-LAST:event_guiFitToDataActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton guiAddOne;
    private JSpinner guiBand;
    private JButton guiFitToData;
    private JButton guiGenerate;
    private JCheckBox guiInvert;
    private JLabel guiJenksMessage;
    private JLabel guiLblBand;
    private JLabel guiLblPalette;
    private JLabel guiLblStep;
    private JSpinner guiMaxSpinner;
    private JComboBox guiMethod;
    private JSpinner guiMinSpinner;
    private JCheckBox guiNaN;
    private JSpinner guiNbStep;
    private JComboBox guiPalette;
    private JButton guiRemoveAll;
    private JXTable guiTable;
    private JScrollPane guiTableScroll;
    private JLabel jLabel1;
    private JPanel jPanel1;
    private JLabel maxLabel;
    private JLabel minLabel;
    // End of variables declaration//GEN-END:variables

    private void guiAddOneActionPerformed(final ActionEvent evt) {
        model.addValue(Float.NaN, Color.BLACK);
        model.fireTableDataChanged();
    }

    private void guiRemoveAllActionPerformed(final ActionEvent evt) {
        model.removeAll();
        model.fireTableDataChanged();
    }

    private void guiGenerateActionPerformed(final ActionEvent evt) {

        if(!(layer instanceof CoverageMapLayer)){
            return;
        }

        model.removeAll();

        //add the NaN if specified
        if(guiNaN.isSelected()){
            model.addValue(Float.NaN, new Color(0, 0, 0, 0));
        }

        boolean mustInterpolation = true;
        final Object paletteValue = (Object) guiPalette.getSelectedItem();
        List<Entry<Double, Color>> steps = new ArrayList<>();

        if (paletteValue instanceof Palette) {
            final Palette palette = (Palette) paletteValue;
            steps = palette.getSteps();
        } else if (paletteValue instanceof String) {
            try {
                final Color[] paletteColors = PF.getColors(String.valueOf(paletteValue));
                final double stepValue = 1.0f/(paletteColors.length-1);
                for (int i = 0; i < paletteColors.length; i++) {
                    final double fragment = i * stepValue;
                    steps.add(new AbstractMap.SimpleEntry<>(fragment, paletteColors[i]));
                }
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }

        for(int i=0,n=steps.size();i<n;i++){
            final double k = steps.get(i).getKey();
            if(k < -0.01 || k > 1.01){
                mustInterpolation = false;
            }
        }

        //recalculate number of steps
        final int nbStep = (Integer)guiNbStep.getValue();
        if(steps.size() != nbStep){
            //recalculate steps
            double min = steps.get(0).getKey();
            double max = min;
            final List<InterpolationPoint> points = new ArrayList<>();
            for(int i=0;i<steps.size();i++){
                points.add(new DefaultInterpolationPoint(steps.get(i).getKey(), SF.literal(steps.get(i).getValue())));
                min = Math.min(min, steps.get(i).getKey());
                max = Math.max(max, steps.get(i).getKey());
            }
            Interpolate inter = SF.interpolateFunction(DEFAULT_CATEGORIZE_LOOKUP, points,Method.COLOR, Mode.LINEAR, DEFAULT_FALLBACK);
            
            //rebuild steps
            steps.clear();
            for(int i=0;i<nbStep;i++){
                final double val = min + ( (max-min)/(nbStep-1) * i );
                final Color color = inter.evaluate(val, Color.class);
                steps.add(new AbstractMap.SimpleEntry<>(val,color));
            }
        }
        
        if(guiInvert.isSelected()){
            final List<Entry<Double, Color>> inverted = new ArrayList<>();
            for(int i=0,n=steps.size();i<n;i++){
                final double k = steps.get(i).getKey();
                inverted.add(new SimpleImmutableEntry<>(
                        k, steps.get(n-1-i).getValue()));
            }
            steps = inverted;
        }
        if(layer instanceof CoverageMapLayer){
            final CoverageMapLayer cml = (CoverageMapLayer)layer;
            final CoverageReference ref = cml.getCoverageReference();
            try {


                if(mustInterpolation){
                    final GridCoverageReader reader = ref.acquireReader();
                    final int imageIndex = ref.getImageIndex();

                    final List<MeasurementRange<?>> ranges = reader.getSampleValueRanges(imageIndex);
                    
                    double min = (Double)guiMinSpinner.getValue();
                    double max = (Double)guiMaxSpinner.getValue();
                    
                    if (ranges != null && !ranges.isEmpty()) {
                        final Integer index = (Integer) guiBand.getValue();
                        final MeasurementRange r = ranges.get(index);

                        min = Math.max(r.getMinDouble(), min);
                        max = Math.min(r.getMaxDouble(), max);

                        // search min/max
                        if (Double.isInfinite(min) || Double.isInfinite(max)) {
                            Map<String,Object> an = StatisticOp.analyze(reader,cml.getCoverageReference().getImageIndex());
                            final double[] minArray = (double[])an.get(StatisticOp.MINIMUM);
                            final double[] maxArray = (double[])an.get(StatisticOp.MAXIMUM);
                            min = findExtremum(minArray, true);
                            max = findExtremum(maxArray, false);
                        }
                    }
                    getInterpolationPoints(min, max, steps);                    
                    ref.recycle(reader);
                }else{
                    for(int s=0,l=steps.size();s<l;s++){
                        final Entry<Double, Color> step = steps.get(s);
                        model.addValue(step.getKey(), step.getValue());
                    }
                }

            } catch (CoverageStoreException ex) {
                LOGGER.log(Level.INFO, ex.getMessage(),ex);
            }
        }

        model.fireTableDataChanged();

    }

    /**
     * Find the min or max values in an array of double
     * @param data double array
     * @param min search min values or max values
     * @return min or max value.
     */
    private double findExtremum(final double[] data, final boolean min) {
        if (data.length > 0) {
            double extremum = data[0];
            if (min) {
                for (int i = 0; i < data.length; i++) {
                    extremum = Math.min(extremum, data[i]);
                }
            } else {
                for (int i = 0; i < data.length; i++) {
                    extremum = Math.max(extremum, data[i]);
                }
            }
            return extremum;
        }
        throw new IllegalArgumentException("Array of " + (min ? "min" : "max") + " values is empty.");
    }

    @Override
    public boolean canHandle(Object target) {
        return target instanceof MapLayer && !(target instanceof FeatureMapLayer);
    }

    @Override
    public void setTarget(final Object layer) {
        if(layer instanceof MapLayer){
            this.layer = (MapLayer)layer;
            parse();
        }else{
            this.layer = null;
        }
    }

    @Override
    public void apply() {
        if(layer == null) return;

        final Expression lookup = DEFAULT_CATEGORIZE_LOOKUP;
        final Literal fallback = DEFAULT_FALLBACK;

        final Function function = ((ColorMapModel)guiTable.getModel()).createFunction();

        final ChannelSelection selection = SF.channelSelection(
                SF.selectedChannelType(String.valueOf(guiBand.getValue()),DEFAULT_CONTRAST_ENHANCEMENT));

        final Expression opacity = LITERAL_ONE_FLOAT;
        final OverlapBehavior overlap = OverlapBehavior.LATEST_ON_TOP;
        final ColorMap colorMap = SF.colorMap(function);
        final ContrastEnhancement enchance = SF.contrastEnhancement(LITERAL_ONE_FLOAT,ContrastMethod.NONE);
        final ShadedRelief relief = SF.shadedRelief(LITERAL_ONE_FLOAT);
        final Symbolizer outline = null;
        final Unit uom = NonSI.PIXEL;
        final String geom = DEFAULT_GEOM;
        final Description desc = SF.description(this.desc, this.desc);

        final RasterSymbolizer symbol = SF.rasterSymbolizer(
                name,geom,desc,uom,opacity, selection, overlap, colorMap, enchance, relief, outline);

        if(parentRule!=null){
            parentRule.symbolizers().remove(parentIndex);
            parentRule.symbolizers().add(parentIndex,symbol);
        }else{
            //style did not exist, add a new feature type style for it
            final MutableFeatureTypeStyle fts = SF.featureTypeStyle(symbol);
            fts.setDescription(SF.description("analyze", "analyze"));
            layer.getStyle().featureTypeStyles().add(fts);
        }
    }

    @Override
    public void reset() {
        if(layer != null){
            parse();
        }
    }

    private void getInterpolationPoints(final double min, final double max, List<Entry<Double, Color>> steps) throws CoverageStoreException {
        for(int s=0,l=steps.size();s<l;s++){
            final Entry<Double, Color> step = steps.get(s);
            model.addValue(min + (step.getKey()*(max-min)), step.getValue());
        }
    }
    
    private void getInterpolationPoints(final GridCoverageReader reader, final CoverageMapLayer cml, List<Entry<Double, Color>> steps) throws CoverageStoreException {
        //we explore the image and try to find the min and max
        Map<String,Object> an = StatisticOp.analyze(reader,cml.getCoverageReference().getImageIndex());
        final double[] minArray = (double[])an.get(StatisticOp.MINIMUM);
        final double[] maxArray = (double[])an.get(StatisticOp.MAXIMUM);
        final double min = findExtremum(minArray, true);
        final double max = findExtremum(maxArray, false);
        getInterpolationPoints(min, max, steps);
    }


    private class DeleteRenderer extends DefaultTableCellRenderer{

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            DeleteRenderer.this.setIcon(IconBuilder.createIcon(FontAwesomeIcons.ICON_TRASH_O, 16, FontAwesomeIcons.DEFAULT_COLOR));
            return DeleteRenderer.this;
        }

    }

    private class DeleteEditor extends AbstractCellEditor implements TableCellEditor{

        private final JButton button = new JButton();
        private int row;

        public DeleteEditor() {
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setIcon(IconBuilder.createIcon(FontAwesomeIcons.ICON_TRASH_O, 16, FontAwesomeIcons.DEFAULT_COLOR));

            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    model.remove(row);
                    fireEditingCanceled();
                    model.fireTableDataChanged();
                }
            });

        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }

        @Override
        public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
            this.row = row;
            return button;
        }

    }

    private abstract class ColorMapModel extends AbstractTableModel{

        public abstract void addValue(Number value, Color color);

        public abstract void remove(int row);

        public abstract void removeAll();

        public abstract Function createFunction();

    }

    private class InterpolateColorModel extends ColorMapModel{

        final Comparator<InterpolationPoint> COMP = new Comparator<InterpolationPoint>() {
            @Override
            public int compare(InterpolationPoint o1, InterpolationPoint o2) {
                return (int)Math.signum( (o1.getData().doubleValue() - o2.getData().doubleValue()));
            }
        };

        private final List<InterpolationPoint> points = new ArrayList<>();

        public InterpolateColorModel(List<InterpolationPoint> points) {
            this.points.addAll(points);
            Collections.sort(this.points,COMP);
        }

        @Override
        public int getRowCount() {
            return points.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return true;
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            final InterpolationPoint pt = points.get(rowIndex);
            switch(columnIndex){
                case 0:
                    return pt.getData();
                case 1:
                    Color c = pt.getValue().evaluate(null, Color.class);
                    final boolean isNaN = Double.isNaN(pt.getData().doubleValue());
                    Color cprevious = c;
                    Color cnext = c;
                    
                    if(!isNaN && rowIndex!=0){
                        final InterpolationPoint ptprevious = points.get(rowIndex-1);
                        if(!Double.isNaN(ptprevious.getData().doubleValue())){
                            cprevious = ptprevious.getValue().evaluate(null, Color.class);
                            cprevious = DefaultInterpolate.interpolate(c, cprevious, 0.5);
                        }
                    }
                    
                    if(!isNaN && rowIndex<points.size()-1){
                        final InterpolationPoint ptnext = points.get(rowIndex+1);
                        if(!Double.isNaN(ptnext.getData().doubleValue())){
                            cnext = ptnext.getValue().evaluate(null, Color.class);
                            cnext = DefaultInterpolate.interpolate(c, cnext, 0.5);
                        }
                    }
                                       
                    return new Color[]{cprevious,c,cnext};
            }
            return "";
        }

        @Override
        public String getColumnName(final int columnIndex) {
            switch(columnIndex){
                case 0: return MessageBundle.getString("style.rastersymbolizer.value");
                case 1: return MessageBundle.getString("style.rastersymbolizer.color");
            }
            return "";
        }

        @Override
        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
            InterpolationPoint pt = points.get(rowIndex);
            switch(columnIndex){
                case 0:
                    Number n = Converters.convert(aValue, Number.class);
                    if(n == null){
                        n = Float.NaN;
                    }

                    pt = SF.interpolationPoint(n, pt.getValue());
                    break;
                case 1:
                    Color c = (Color) aValue;
                    if(c == null){
                        c = new Color(0, 0, 0, 0);
                    }
                    pt = SF.interpolationPoint(pt.getData(),SF.literal(c));
                    break;
            }

            points.set(rowIndex, pt);
            Collections.sort(points,COMP);
            fireTableDataChanged();
        }

        @Override
        public void addValue(Number value, Color color) {
            final InterpolationPoint pt = SF.interpolationPoint(value, SF.literal(color));
            points.add(pt);
            Collections.sort(points,COMP);
            fireTableDataChanged();
        }

        @Override
        public void removeAll() {
            points.clear();
            fireTableDataChanged();
        }

        @Override
        public void remove(int row) {
            points.remove(row);
            fireTableDataChanged();
        }

        @Override
        public Function createFunction() {
            return SF.interpolateFunction(DEFAULT_CATEGORIZE_LOOKUP, new ArrayList<>(points),
                    Method.COLOR, Mode.LINEAR, DEFAULT_FALLBACK);
        }

    }

    private class CategorizeColorModel extends ColorMapModel{

        final Comparator<Entry<Expression,Expression>> COMP = new Comparator<Entry<Expression, Expression>>() {

            @Override
            public int compare(Entry<Expression, Expression> o1, Entry<Expression, Expression> o2) {
                final Double d0 = o1.getKey().evaluate(null, Double.class);
                final Double d1 = o2.getKey().evaluate(null, Double.class);
                if(d0==null) return -1;
                if(d1==null) return +1;
                return d0.compareTo(d1);
            }
        };

        private final List<Entry<Expression,Expression>> ths = new ArrayList<>();

        public CategorizeColorModel(List<Entry<Expression,Expression>> map) {
            ths.addAll(map);
            Collections.sort(ths,COMP);
        }

        public CategorizeColorModel(Map<Expression,Expression> map) {
            ths.addAll(map.entrySet());
            Collections.sort(ths,COMP);
        }

        @Override
        public int getRowCount() {
            return ths.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            if(columnIndex==3){
                //first and last (infinite) rows can not be removed
                return rowIndex > 0 && rowIndex < ths.size()-1;
            }else if(columnIndex==2){
                //a copy of the next threholds value, can not be removed
                return false;
            }else if(columnIndex==1){
                //color column
                return true;
            }else{
                //first column
                return rowIndex>0;
            }
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if(columnIndex==2){
                //return next column value if any
                if(rowIndex<ths.size()-1){
                    return getValueAt(rowIndex+1, 0);
                }else{
                    //last line +infinity
                    return Double.POSITIVE_INFINITY;
                }
            }else if(columnIndex==3){
                //delete column
                return "";
            }else if(columnIndex==1){
                //color column
                final Entry<Expression, Expression> entry = ths.get(rowIndex);
                return entry.getValue().evaluate(null, Color.class);
            }else{
                //thresdhold value
                final Entry<Expression, Expression> entry = ths.get(rowIndex);
                Number n = entry.getKey().evaluate(null, Number.class);
                if(n==null) n = Double.NEGATIVE_INFINITY;
                return n;
            }
        }

        @Override
        public String getColumnName(final int columnIndex) {
            switch(columnIndex){
                case 0: return MessageBundle.getString("style.rastersymbolizer.lower");
                case 1: return MessageBundle.getString("style.rastersymbolizer.color");
                case 2: return MessageBundle.getString("style.rastersymbolizer.upper");
            }
            return "";
        }

        @Override
        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
            Entry<Expression,Expression> th = ths.get(rowIndex);
            switch(columnIndex){
                case 0:
                    Number n = Converters.convert(aValue, Number.class);
                    if(n == null){
                        n = Float.NaN;
                    }
                    th = new AbstractMap.SimpleEntry<>((Expression)FF.literal(n),th.getValue());
                    break;
                case 1:
                    Color c = (Color) aValue;
                    if(c == null){
                        c = new Color(0, 0, 0, 0);
                    }
                    th = new AbstractMap.SimpleEntry<>(th.getKey(),(Expression)SF.literal(c));
                    break;
            }

            ths.set(rowIndex, th);
            Collections.sort(ths,COMP);
            fireTableDataChanged();
        }

        @Override
        public void addValue(Number value, Color color) {
            Entry<Expression,Expression> th = new AbstractMap.SimpleEntry<>(
                    (Expression)FF.literal(value),(Expression)SF.literal(color));
            ths.add(th);
            Collections.sort(ths,COMP);
            fireTableDataChanged();
        }

        @Override
        public void removeAll() {
            ths.clear();
            Entry<Expression,Expression> th1 = new AbstractMap.SimpleEntry<>(
                    (Expression)StyleConstants.CATEGORIZE_LESS_INFINITY,(Expression)TRS);
//            Entry<Expression,Expression> th2 = new AbstractMap.SimpleEntry<>(
//                    (Expression)FF.literal(0d),(Expression)SF.literal(new Color(0f,0f,0f,0f)));
            ths.add(th1);
//            ths.add(th2);
            fireTableDataChanged();
        }

        @Override
        public void remove(int row) {
            ths.remove(row);
            fireTableDataChanged();
        }

        @Override
        public Function createFunction() {
            final Expression lookup = DEFAULT_CATEGORIZE_LOOKUP;
            final Literal fallback = DEFAULT_FALLBACK;

            final Map<Expression,Expression> map = new HashMap<>();
            for(Entry<Expression,Expression> exp : ths){
                map.put(exp.getKey(), exp.getValue());
            }

            return SF.categorizeFunction(lookup, map, ThreshholdsBelongTo.PRECEDING, fallback);
        }

    }

    private class JenksColorModel extends ColorMapModel{

        @Override
        public void addValue(Number value, Color color) {
            //do nothing, model is dynamic
        }

        @Override
        public void remove(int row) {
            //do nothing, model is dynamic
        }

        @Override
        public void removeAll() {
            //do nothing, model is dynamic
        }

        @Override
        public Function createFunction() {
            final Literal fallback = DEFAULT_FALLBACK;
            final Object item = guiPalette.getSelectedItem();
            String paletteName = "";
            if(item instanceof String){
                paletteName = (String)item;
            }
            return SF.jenksFunction(FF.literal(guiNbStep.getModel().getValue()), FF.literal(paletteName), fallback);
        }

        @Override
        public int getRowCount() {
            return 0;
        }

        @Override
        public int getColumnCount() {
            return 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return null;
        }
        
    }

}
