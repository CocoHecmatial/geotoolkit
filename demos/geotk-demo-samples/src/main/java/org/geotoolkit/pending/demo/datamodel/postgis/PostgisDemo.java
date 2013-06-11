package org.geotoolkit.pending.demo.datamodel.postgis;

import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.FeatureStoreFinder;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.memory.MemoryFeatureStore;
import org.geotoolkit.data.postgis.PostgisNGDataStoreFactory;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.gui.swing.go2.JMap2DFrame;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.pending.demo.Demos;
import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.style.RandomStyleBuilder;
import org.opengis.feature.type.Name;
import org.opengis.parameter.ParameterValueGroup;

public class PostgisDemo {

    public static void main(String[] args) throws DataStoreException {
        Demos.init();
        
        System.out.println(PostgisNGDataStoreFactory.PARAMETERS_DESCRIPTOR);

        final ParameterValueGroup parameters = PostgisNGDataStoreFactory.PARAMETERS_DESCRIPTOR.createValue();
        Parameters.getOrCreate(PostgisNGDataStoreFactory.HOST, parameters).setValue("hote");
        Parameters.getOrCreate(PostgisNGDataStoreFactory.PORT, parameters).setValue(5432);
        Parameters.getOrCreate(PostgisNGDataStoreFactory.DATABASE, parameters).setValue("base");
        Parameters.getOrCreate(PostgisNGDataStoreFactory.USER, parameters).setValue("user");
        Parameters.getOrCreate(PostgisNGDataStoreFactory.PASSWORD, parameters).setValue("secret");
        
        final FeatureStore store = FeatureStoreFinder.open(parameters);
        
        final MapContext context = MapBuilder.createContext();
        
        for(Name n : store.getNames()){
            System.out.println(store.getFeatureType(n));
            
            final FeatureCollection col = store.createSession(true).getFeatureCollection(QueryBuilder.all(n));
            context.layers().add(MapBuilder.createFeatureLayer(col, RandomStyleBuilder.createRandomVectorStyle(col.getFeatureType())));
        }
        
        
        JMap2DFrame.show(context);
        
    }

}
