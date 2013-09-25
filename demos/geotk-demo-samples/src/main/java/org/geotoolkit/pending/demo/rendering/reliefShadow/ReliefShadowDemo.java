/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geotoolkit.pending.demo.rendering.reliefShadow;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import org.apache.sis.geometry.GeneralEnvelope;
import org.geotoolkit.coverage.io.CoverageIO;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.gui.swing.go2.JMap2DFrame;
import org.geotoolkit.image.interpolation.Interpolation;
import org.geotoolkit.image.interpolation.InterpolationCase;
import org.geotoolkit.image.interpolation.Resample;
import org.geotoolkit.image.io.XImageIO;
import org.geotoolkit.image.iterator.PixelIterator;
import org.geotoolkit.image.iterator.PixelIteratorFactory;
import org.geotoolkit.lang.Setup;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.ElevationModel;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.pending.demo.Demos;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.geotoolkit.referencing.operation.transform.AffineTransform2D;
import org.geotoolkit.style.DefaultStyleFactory;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.style.StyleConstants;
import static org.geotoolkit.style.StyleConstants.DEFAULT_DESCRIPTION;
import static org.geotoolkit.style.StyleConstants.LITERAL_ONE_FLOAT;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.style.ChannelSelection;
import org.opengis.style.ColorMap;
import org.opengis.style.ContrastEnhancement;
import org.opengis.style.Description;
import org.opengis.style.OverlapBehavior;
import org.opengis.style.RasterSymbolizer;
import org.opengis.style.ShadedRelief;
import org.opengis.style.Symbolizer;
import org.openide.util.Exceptions;

/**
 * Show how to use {@link ElevationModel} to add shadow on image in renderer. 
 *
 * @author Remi Marechal (Geomatys).
 */
public class ReliefShadowDemo {
    public static final MutableStyleFactory SF = new DefaultStyleFactory();
    protected static final FilterFactory FF = FactoryFinder.getFilterFactory(null);
    
    /**
     * Relief path of Digital Elevation Model (DEM).
     */
    final static File reliefPath = new File("data/cloudsRelief.tiff");
    /**
     * Create {@link GridCoverageReader} which will be return by {@link ElevationModel} to read DEM.
     */
    
    public static void main(String[] args) throws Exception {
        Demos.init();
        ImageReader covPath = XImageIO.getReaderByFormatName("tiff-wf", reliefPath, Boolean.FALSE, false);
            covPath.setInput(reliefPath);
        final GridCoverageReader  demGCR = CoverageIO.createSimpleReader(covPath);
        /*
         * Coverage which will be shadowed.
         */
        final File input = new File("data/clouds.jpg");
        final GridCoverageReader reader = CoverageIO.createSimpleReader(input);
                
        //create a mapcontext
        final MapContext context = MapBuilder.createContext();        
        final CoverageMapLayer cl = MapBuilder.createCoverageLayer(reader, 0, SF.style(StyleConstants.DEFAULT_RASTER_SYMBOLIZER), "raster");
        
        /*
         * Define Elevation Model object to get informations necessary to compute shadow on coverage. 
         */
        cl.setElevationModel(new ElevationModel() {

            /*
             * Define in per cent the lenght of shadow in function of maximum amplitude lenght from all DEM values.
             * It is a factor to control shadow lenght propagation.
             */
            @Override
            public double getAmplitudeScale() {
                return 55;//55%
            }

            /*
             * Return grid coverage use to read Digital Elevation Model.
             */
            @Override
            public GridCoverageReader getCoverageReader() {
                return demGCR;
            }

            /*
             * Define angle of the light from Origine North axis.
             * Angle is define positive in clockwise.
             */
            @Override
            public double getAzimuth() {
                return -80;
            }

            /*
             * Define angle formed by light direction and ground surface. 
             */
            @Override
            public double getAltitude() {
                return 2;
            }
        });
        
        MutableStyle style = customRaster();
        cl.setStyle(style);
        final GeneralEnvelope env = new GeneralEnvelope(DefaultGeographicCRS.WGS84);
        env.setEnvelope(-180, -90, 180, 90);
        context.setAreaOfInterest(env);
        context.layers().add(cl);
        JMap2DFrame.show(context);
    }
    
    /*
     * Define style.
     */
    public static MutableStyle customRaster() {

        final String name = "mySymbol";
        final Description desc = DEFAULT_DESCRIPTION;
        final String geometry = null; //use the default geometry of the feature
        final Unit unit = NonSI.PIXEL;
        final Expression opacity = LITERAL_ONE_FLOAT;
        final ChannelSelection channels = null;
        final OverlapBehavior overlap = null;
        final ColorMap colormap = null;
        final ContrastEnhancement enhance = null;
        /*
         * Define if we want shadow.
         * First argument define in percent the dimming of shadow pixel value.
         * Second argument define if we want increase sunny pixel value.
         */
        final ShadedRelief relief = SF.shadedRelief(FF.literal(60),true);
        final Symbolizer outline = null;

        final RasterSymbolizer symbol = SF.rasterSymbolizer(
                name,(String)null,desc,unit,opacity,
                channels,overlap,colormap,enhance,relief,outline);
        final MutableStyle style = SF.style(symbol);
        return style;
    }    
}
