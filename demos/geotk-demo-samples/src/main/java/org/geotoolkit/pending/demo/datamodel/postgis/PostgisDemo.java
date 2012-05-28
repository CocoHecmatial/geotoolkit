package org.geotoolkit.pending.demo.datamodel.postgis;

import org.geotoolkit.data.DataStore;
import org.geotoolkit.data.DataStoreFinder;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.memory.MemoryDataStore;
import org.geotoolkit.data.postgis.PostgisNGDataStoreFactory;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.gui.swing.go2.JMap2DFrame;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.pending.demo.Demos;
import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.util.RandomStyleFactory;
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
        Parameters.getOrCreate(PostgisNGDataStoreFactory.PASSWD, parameters).setValue("secret");
        
        final DataStore store = DataStoreFinder.get(parameters);
        
        final MapContext context = MapBuilder.createContext();
        
        for(Name n : store.getNames()){
            System.out.println(store.getFeatureType(n));
            
            final FeatureCollection col = store.createSession(true).getFeatureCollection(QueryBuilder.all(n));
            context.layers().add(MapBuilder.createFeatureLayer(col, RandomStyleFactory.createRandomVectorStyle(col)));
        }
        
        
        JMap2DFrame.show(context);
        
    }

}
