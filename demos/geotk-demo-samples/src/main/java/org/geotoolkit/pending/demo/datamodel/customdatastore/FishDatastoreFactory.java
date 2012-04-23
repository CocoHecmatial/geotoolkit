

package org.geotoolkit.pending.demo.datamodel.customdatastore;

import java.util.Collections;
import org.geotoolkit.data.AbstractDataStoreFactory;
import org.geotoolkit.data.AbstractFileDataStoreFactory;
import org.geotoolkit.data.DataStore;
import org.geotoolkit.metadata.iso.DefaultIdentifier;
import org.geotoolkit.metadata.iso.citation.DefaultCitation;
import org.geotoolkit.metadata.iso.identification.DefaultServiceIdentification;
import org.geotoolkit.parameter.DefaultParameterDescriptor;
import org.geotoolkit.parameter.DefaultParameterDescriptorGroup;
import org.geotoolkit.storage.DataStoreException;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.Identification;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

public class FishDatastoreFactory extends AbstractFileDataStoreFactory{

    /** factory identification **/
    public static final String NAME = "fish";
    public static final DefaultServiceIdentification IDENTIFICATION;
    static {
        IDENTIFICATION = new DefaultServiceIdentification();
        final Identifier id = new DefaultIdentifier(NAME);
        final DefaultCitation citation = new DefaultCitation(NAME);
        citation.setIdentifiers(Collections.singleton(id));
        IDENTIFICATION.setCitation(citation);
    }
    
    public static final ParameterDescriptor<String> IDENTIFIER = new DefaultParameterDescriptor<String>(
                    AbstractDataStoreFactory.IDENTIFIER.getName().getCode(),
                    AbstractDataStoreFactory.IDENTIFIER.getRemarks(), String.class,NAME,true);
    
    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            new DefaultParameterDescriptorGroup("FishParameters",
                IDENTIFIER,URLP,NAMESPACE);

    @Override
    public Identification getIdentification() {
        return IDENTIFICATION;
    }

    @Override
    public String getDescription() {
        return "Scientific fish files (*.fsh)";
    }

    @Override
    public ParameterDescriptorGroup getParametersDescriptor() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public DataStore create(ParameterValueGroup params) throws DataStoreException {
        checkCanProcessWithError(params);
        return new FishDataStore(params);
    }

    @Override
    public DataStore createNew(ParameterValueGroup params) throws DataStoreException {
        throw new DataStoreException("Not supported.");
    }

    @Override
    public String[] getFileExtensions() {
        return new String[]{".fsh"};
    }

}
