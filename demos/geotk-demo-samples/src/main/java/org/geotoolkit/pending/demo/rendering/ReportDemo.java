
package org.geotoolkit.pending.demo.rendering;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import net.sf.jasperreports.engine.JasperReport;

import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.FeatureStoreFinder;
import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.data.memory.GenericMappingFeatureIterator;
import org.geotoolkit.data.memory.mapping.FeatureMapper;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.canvas.J2DCanvas;
import org.geotoolkit.display2d.ext.grid.DefaultGridTemplate;
import org.geotoolkit.display2d.ext.grid.GraphicGridJ2D;
import org.geotoolkit.display2d.ext.grid.GridTemplate;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.OutputDef;
import org.geotoolkit.display2d.service.PortrayalExtension;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.display2d.service.ViewDef;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.FeatureUtilities;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.pending.demo.Demos;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.geotoolkit.report.FeatureCollectionDataSource;
import org.geotoolkit.report.JasperReportService;
import org.geotoolkit.report.graphic.chart.ChartDef;
import org.geotoolkit.report.graphic.legend.LegendDef;
import org.geotoolkit.report.graphic.map.MapDef;
import org.geotoolkit.report.graphic.northarrow.NorthArrowDef;
import org.geotoolkit.report.graphic.scalebar.ScaleBarDef;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.style.RandomStyleBuilder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.FeatureFactory;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.Name;
import org.opengis.filter.FilterFactory;

public class ReportDemo {

    private static final FeatureFactory FF = FeatureFactory.LENIENT;
    private static final MutableStyleFactory SF = (MutableStyleFactory)FactoryFinder.getStyleFactory(
                 new Hints(Hints.STYLE_FACTORY,MutableStyleFactory.class));
    private static final FilterFactory FIF = FactoryFinder.getFilterFactory(null);


    public static void main(String[] args) throws Exception {
        Demos.init();

        final InputStream template = ReportDemo.class.getResourceAsStream("/data/report/complexReport.jrxml");

        final Entry<JasperReport,FeatureType> entry = JasperReportService.prepareTemplate(template);
        final JasperReport report = entry.getKey();
        final FeatureType type = entry.getValue();
        System.out.println(type);


        //source to make an atlas ----------------------------------------------------
        final FeatureStore store = FeatureStoreFinder.open(
                (Map)Collections.singletonMap("url",ReportDemo.class.getResource("/data/world/Countries.shp")));
        final Name name = store.getNames().iterator().next();
        final FeatureCollection countries =  store.createSession(true).getFeatureCollection(QueryBuilder.all(name));


        //Iterator over all the countries --------------------------------------------
        final FeatureIterator ite = countries.iterator();


        //We map the feature type to the report type ---------------------------------
        final GenericMappingFeatureIterator mapped = new GenericMappingFeatureIterator(ite, new FeatureMapper(){

            @Override
            public FeatureType getSourceType() {
                return countries.getFeatureType();
            }

            @Override
            public FeatureType getTargetType() {
                return type;
            }

            @Override
            public Feature transform(Feature feature) {
                final Feature modified = FeatureUtilities.defaultFeature(type, "id");

                //create the main map with a single feature ------------------
                final FeatureCollection col = FeatureStoreUtilities.collection(feature);
                final MapContext context = MapBuilder.createContext();
                final MutableStyle style = RandomStyleBuilder.createRandomVectorStyle(col.getFeatureType());
                context.layers().add(MapBuilder.createFeatureLayer(col, style));


                try{
                    //add a custom decoration on our map.
                    final GridTemplate gridTemplate = new DefaultGridTemplate(
                        DefaultGeographicCRS.WGS84,
                        new BasicStroke(1.5f),
                        new Color(120,120,120,200),

                        new BasicStroke(1,BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3, new float[]{5,5}, 0),
                        new Color(120,120,120,60),

                        new Font("serial", Font.BOLD, 12),
                        Color.GRAY,
                        0,
                        Color.WHITE,

                        new Font("serial", Font.ITALIC, 10),
                        Color.GRAY,
                        0,
                        Color.WHITE);
                    final PortrayalExtension ext = new PortrayalExtension() {
                        @Override
                        public void completeCanvas(J2DCanvas canvas) throws PortrayalException {
                            canvas.getContainer().getRoot().getChildren().add(new GraphicGridJ2D(canvas, gridTemplate));
                        }
                    };
                    final CanvasDef canvasDef = new CanvasDef(new Dimension(1, 1), Color.WHITE,false);
                    final SceneDef sceneDef = new SceneDef(context,null,ext);
                    final ViewDef viewDef = new ViewDef(CRS.transform(context.getBounds(), CRS.decode("EPSG:3395")),0);
                    final MapDef mapdef = new MapDef(canvasDef,sceneDef,viewDef,null);
                    modified.getProperty("map3").setValue(mapdef);
                }catch(Exception ex){
                    ex.printStackTrace();
                }


                //casual attributs -------------------
                modified.getProperty("CNTRY_NAME").setValue(feature.getProperty("CNTRY_NAME").getValue());
                modified.getProperty("POP_CNTRY").setValue(feature.getProperty("POP_CNTRY").getValue());

                //chart -------------------------
                final DefaultPieDataset pds = new DefaultPieDataset();
                pds.setValue((Comparable)feature.getProperty("SOVEREIGN").getValue(), Math.random());
                pds.setValue((Comparable)feature.getProperty("ISO_3DIGIT").getValue(), Math.random());
                final JFreeChart chart = ChartFactory.createPieChart("Info", pds, true, true, Locale.FRENCH);
                modified.getProperty("chart4").setValue(new ChartDef(chart));

                //legend --------------------------
                modified.getProperty("legend5").setValue(new LegendDef());

                //scale bar -------------------
                modified.getProperty("scalebar6").setValue(new ScaleBarDef());

                //north arow -------------------
                modified.getProperty("northarrow7").setValue(new NorthArrowDef());

                //subtable --------------
                final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
                ftb.setName("subdata");
                ftb.add("men", Integer.class);
                ftb.add("women",Integer.class);
                ftb.add("desc", String.class);
                final FeatureType subType = ftb.buildFeatureType();
                final FeatureCollection subcol = FeatureStoreUtilities.collection("sub", subType);
                try {
                    FeatureWriter fw = subcol.getSession().getFeatureStore().getFeatureWriterAppend(subType.getName());
                    for(int i=0,n=new Random().nextInt(20);i<n;i++){
                        Feature f =fw.next();
                        f.getProperty("men").setValue(new Random().nextInt());
                        f.getProperty("women").setValue(new Random().nextInt());
                        f.getProperty("desc").setValue("some text from attribut");
                        fw.write();
                    }
                    fw.close();

                } catch (DataStoreException ex) {
                    ex.printStackTrace();
                }
                modified.getProperty("table8").setValue(new FeatureCollectionDataSource(subcol));

                return modified;
            }
        });


        //Generate the report --------------------------------------------------------
        final OutputDef output = new OutputDef(JasperReportService.MIME_PDF, new File("atlas.pdf"));
        JasperReportService.generateReport(report, mapped, null, output);

    }


}
