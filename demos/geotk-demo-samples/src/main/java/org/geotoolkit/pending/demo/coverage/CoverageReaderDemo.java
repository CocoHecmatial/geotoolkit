
package org.geotoolkit.pending.demo.coverage;

import java.awt.geom.Rectangle2D;
import java.io.File;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.io.CoverageIO;
import org.geotoolkit.coverage.io.GridCoverageReadParam;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.gui.swing.render2d.JMap2DFrame;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.pending.demo.Demos;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.geotoolkit.style.DefaultStyleFactory;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.style.StyleConstants;
import org.opengis.metadata.Metadata;


public class CoverageReaderDemo {

    public static final MutableStyleFactory SF = new DefaultStyleFactory();

    public static void main(String[] args) throws Exception {
        Demos.init();

//        final File input = new File("data/clouds.jpg");
//        final GridCoverageReader reader = CoverageIO.createSimpleReader(input);
                
//        //print the iso 19115 metadata
//        final Metadata metadata = reader.getMetadata();
//        System.out.println(metadata);
//
//        //read a piece of coverage
//        final GridCoverageReadParam param = new GridCoverageReadParam();
//        param.setResolution(1,1);
//        param.setEnvelope(new Rectangle2D.Double(0, 0, 100, 100), DefaultGeographicCRS.WGS84);
//
//        final GridCoverage2D coverage = (GridCoverage2D) reader.read(0, param);
//        coverage.show();


        //create a mapcontext
        final MapContext context = MapBuilder.createContext();
        final CoverageMapLayer cl = MapBuilder.createCoverageLayer(new File("data/clouds.jpg"));
        context.layers().add(cl);

        //display it
        JMap2DFrame.show(context);

    }

}
