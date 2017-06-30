package org.geotoolkit.pending.demo.processing;

import org.geotoolkit.coverage.xmlstore.XMLCoverageStoreFactory;
import java.awt.Dimension;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;

import org.geotoolkit.coverage.filestore.*;
import org.geotoolkit.util.NamesExt;
import org.apache.sis.geometry.GeneralEnvelope;
import org.geotoolkit.gui.swing.ProgressWindow;
import org.geotoolkit.image.jai.Registry;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.pending.demo.Demos;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.coverage.CoverageStore;
import org.geotoolkit.style.DefaultStyleFactory;
import org.geotoolkit.style.MutableStyleFactory;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.referencing.CommonCRS;
import org.geotoolkit.storage.coverage.CoverageResource;

/**
 * Create a pyramid from a MapContext.
 */
public class MapTilingDemo {

    public static final MutableStyleFactory SF = new DefaultStyleFactory();

    public static void main(String[] args) throws Throwable {
        Demos.init();

        //reset values, only allow pure java readers
        for(String jn : ImageIO.getReaderFormatNames()){
            if(jn.toLowerCase().contains("png")){
                Registry.setNativeCodecAllowed(jn, ImageReaderSpi.class, false);
            }
        }

        //reset values, only allow pure java writers
        for(String jn : ImageIO.getWriterFormatNames()){
            if(jn.toLowerCase().contains("png")){
                Registry.setNativeCodecAllowed(jn, ImageWriterSpi.class, false);
            }
        }



        //create a map context
        final MapContext context = openData();


        //get the description of the process we want
        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("engine2d", "mapcontextpyramid");
        System.out.println(desc.getInputDescriptor());

        //create a coverage store where the pyramid wil be stored
        final XMLCoverageStoreFactory factory = new XMLCoverageStoreFactory();
        final CoverageStore store = (CoverageStore) factory.create(Collections.singletonMap(
                "path", new URL("file:/media/terra/GIS_DATA/wmts_bluemarble")));
        final GenericName name = NamesExt.create("bluemarble");
        final CoverageResource ref = store.create(name);


        //set the input parameters
        final ParameterValueGroup input = desc.getInputDescriptor().createValue();
//        Envelope env = context.getBounds();

        final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.defaultGeographic());
        env.setRange(0, -180, +180);
        env.setRange(1, -90, 90);
        final int nbscale = 20;
        double[] scales = new double[nbscale];
        scales[0] = env.getSpan(0) / 256 ;
        for(int i=1;i<nbscale;i++){
            scales[i] = scales[i-1] /2;
        }


        input.parameter("context").setValue(context);
        input.parameter("extent").setValue(env);
        input.parameter("tilesize").setValue(new Dimension(256, 256));
        input.parameter("scales").setValue(scales);
        input.parameter("container").setValue(ref);
        final org.geotoolkit.process.Process p = desc.createProcess(input);

        //use a small predefined dialog
        final ProgressWindow pw = new ProgressWindow(null);
        p.addListener(pw);

        //get the result
        final ParameterValueGroup result = p.call();

//        //display the tiled image
//        context.layers().clear();
//        for(final Name n : store.getNames()){
//            final CoverageReference covref = store.getCoverageResource(n);
//            final MapLayer layer = MapBuilder.createCoverageLayer(
//                    covref,
//                    new DefaultStyleFactory().style(StyleConstants.DEFAULT_RASTER_SYMBOLIZER),
//                    n.getLocalPart());
//
//            //display the generated pyramid
//            final PyramidalModel model = (PyramidalModel) covref;
//            System.out.println(model.getPyramidSet());
//
//            layer.setDescription(SF.description(n.getLocalPart(), n.getLocalPart()));
//            context.layers().add(layer);
//        }
//
//        JMap2DFrame.show(context);

    }

    private static MapContext openData() throws DataStoreException, URISyntaxException {

        final ParameterValueGroup params = FileCoverageStoreFactory.PARAMETERS_DESCRIPTOR.createValue();
        params.parameter(FileCoverageStoreFactory.PATH.getName().getCode()).setValue(new URI("file:/home/jsorel/temp/bluemarble/bluemarble"));

        final CoverageStore store = (CoverageStore) DataStores.open(params);

        final MapContext context = MapBuilder.createContext();

        for(GenericName n : store.getNames()){
            final CoverageMapLayer layer = MapBuilder.createCoverageLayer(store.getCoverageResource(n));
            context.layers().add(layer);
        }

        return context;
    }
}
