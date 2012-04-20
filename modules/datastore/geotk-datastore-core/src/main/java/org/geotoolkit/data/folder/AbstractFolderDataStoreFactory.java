/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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
package org.geotoolkit.data.folder;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geotoolkit.data.AbstractDataStoreFactory;
import org.geotoolkit.data.AbstractFileDataStoreFactory;
import org.geotoolkit.data.DataStore;
import org.geotoolkit.data.FileDataStoreFactory;
import org.geotoolkit.metadata.iso.DefaultIdentifier;
import org.geotoolkit.metadata.iso.citation.DefaultCitation;
import org.geotoolkit.metadata.iso.identification.DefaultServiceIdentification;
import org.geotoolkit.parameter.DefaultParameterDescriptor;
import org.geotoolkit.parameter.DefaultParameterDescriptorGroup;
import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.util.ArgumentChecks;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.Identification;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Factory to create a datastore from a folder of specific file types.
 * 
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public abstract class AbstractFolderDataStoreFactory extends AbstractDataStoreFactory{

    /**
     * url to the folder.
     */
    public static final ParameterDescriptor<URL> URLFOLDER =
            new DefaultParameterDescriptor<URL>("url","url to a folder with extension, example : file:/home/user/data/*.shp",
            URL.class,null,true);
    
    /**
     * recursively search folder.
     */
    public static final ParameterDescriptor<Boolean> RECURSIVE =
            new DefaultParameterDescriptor<Boolean>("recursive","Recursively explore the given folder. default is true.",
            Boolean.class,true,true);
    
    private ParameterDescriptorGroup paramDesc = null;
    
    public AbstractFolderDataStoreFactory(final ParameterDescriptorGroup desc){
        ArgumentChecks.ensureNonNull("desc", desc);
        paramDesc = desc;
    }
    
    public abstract FileDataStoreFactory getSingleFileFactory();
    
    /**
     * {@inheritDoc }
     */
    @Override
    public boolean canProcess(final ParameterValueGroup params) {
        boolean valid = super.canProcess(params);

        if(valid){
            final FileDataStoreFactory dsf = getSingleFileFactory();
            final Object obj = params.parameter(URLFOLDER.getName().toString()).getValue();
            if(obj != null && obj instanceof URL){
                final String path = ((URL)obj).toString().toLowerCase();
                for(String ext : dsf.getFileExtensions()){
                    ext = "*"+ext;
                    if(path.endsWith(ext)){
                        return true;
                    }
                }
                return false;
            }else{
                return false;
            }
        }else{
            return false;
        }

    }
    
    @Override
    public ParameterDescriptorGroup getParametersDescriptor() {
        return paramDesc;
    }

    @Override
    public DataStore create(ParameterValueGroup params) throws DataStoreException {
        final FolderDataStore store = new FolderDataStore(params,this);
        return store;
    }

    @Override
    public DataStore createNew(ParameterValueGroup params) throws DataStoreException {
        //we can create an empty datastore of this type
        //the create datastore will always work, it will just be empty if there are no files in it.
        return create(params);
    }
     
    
    /**
     * Derivate a folder factory identification from original single file factory.
     */
    protected static DefaultServiceIdentification derivateIdentification(DefaultServiceIdentification identification){
        final String name = String.valueOf(identification.getCitation().getTitle())+"-folder";
        final DefaultServiceIdentification ident = new DefaultServiceIdentification();
        final Identifier id = new DefaultIdentifier(name);
        final DefaultCitation citation = new DefaultCitation(name);
        citation.setIdentifiers(Collections.singleton(id));
        ident.setCitation(citation);
        return ident;
    }
    
    /**
     * Create a Folder datastore descriptor group based on the single file factory
     * parameters.
     * 
     * @return ParameterDescriptorGroup
     */
    protected static ParameterDescriptorGroup derivateDescriptor(
            final ParameterDescriptor identifierParam,final ParameterDescriptorGroup sd){

        final List<GeneralParameterDescriptor> params = new ArrayList<GeneralParameterDescriptor>(sd.descriptors());
        for(int i=0;i<params.size();i++){
            if(params.get(i).getName().getCode().equals(AbstractDataStoreFactory.IDENTIFIER.getName().getCode())){
                params.remove(i);
                break;
            }
        }
        params.remove(AbstractFileDataStoreFactory.URLP);
        params.add(0,identifierParam);        
        params.add(1,URLFOLDER);
        params.add(2,RECURSIVE);

        return new DefaultParameterDescriptorGroup(sd.getName().getCode()+"Folder",
                params.toArray(new GeneralParameterDescriptor[params.size()]));
    }
    
}
