
package org.geotoolkit.pending.demo.datamodel;

import java.util.Iterator;
import org.geotoolkit.data.FeatureStoreFactory;
import org.geotoolkit.data.FeatureStoreFinder;
import org.geotoolkit.pending.demo.Demos;
import org.opengis.parameter.ParameterDescriptorGroup;


public class ListAllFactoriesDemo {
    
    public static void main(String[] args) {
        Demos.init();
        
        // Listing or creating new datastores are made through the DataStoreFinder utility class        
        final Iterator<FeatureStoreFactory> ite = FeatureStoreFinder.getAllFactories(null).iterator();
        
        while(ite.hasNext()){
            
            final FeatureStoreFactory factory = ite.next();
            
            //display general informations about this factory
            System.out.println(factory.getDisplayName());
            System.out.println(factory.getDescription());
            
            //display the parameter requiered to open a new instance
            //of datastore of this type
            final ParameterDescriptorGroup description = factory.getParametersDescriptor();
            System.out.println(description);
            System.out.println("\n\n");
            
            
            //if we wanted to created a new datastore of this type we would proceed
            //like this :
            /*
            final ParameterValueGroup params = description.createValue();
            params.parameter("parameter_name_1").setValue("parameter value 1");
            params.parameter("parameter_name_2").setValue("parameter value 2");
            params.parameter("parameter_name_N").setValue("parameter value N");
            final Server server = factory.open(params);
            */
        }
        
        
    }
    
}
