
package org.geotoolkit.pending.demo.coverage;

import com.sun.java.swing.plaf.gtk.GTKLookAndFeel;
import javax.swing.UIManager;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.grid.GridCoverageBuilder;
import org.geotoolkit.geometry.GeneralEnvelope;
import org.geotoolkit.gui.swing.go2.JMap2DFrame;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.pending.demo.Demos;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.geotoolkit.style.DefaultStyleFactory;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.style.StyleConstants;


public class CustomCoverage1SDemo {
    
    public static final MutableStyleFactory SF = new DefaultStyleFactory();
    
    public static void main(String[] args) throws Exception {
        Demos.init();
        
        UIManager.setLookAndFeel(new GTKLookAndFeel());
        
        //first create a matrix table
        final float[][] matrix = new float[100][100];
        for(int x=0;x<100;x++){
            for(int y=0;y<100;y++){
                matrix[x][y] = x+y;
            }
        }
        
        final GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setRenderedImage(matrix);
        
        //set it's envelope
        final GeneralEnvelope env = new GeneralEnvelope(DefaultGeographicCRS.WGS84);
        env.setRange(0, 0, 100);
        env.setRange(1, 0, 100);
        gcb.setEnvelope(env);
        
        //create the coverage
        final GridCoverage2D coverage = gcb.getGridCoverage2D();
                        
        //display it
        final MapContext context = MapBuilder.createContext();
        final CoverageMapLayer cl = MapBuilder.createCoverageLayer(coverage, SF.style(StyleConstants.DEFAULT_RASTER_SYMBOLIZER), "coverage");
        context.layers().add(cl);
        JMap2DFrame.show(context);
    }
    
}
