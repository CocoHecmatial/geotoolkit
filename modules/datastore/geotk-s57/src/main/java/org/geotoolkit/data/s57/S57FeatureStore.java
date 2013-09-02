/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2013, Geomatys
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

package org.geotoolkit.data.s57;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.AbstractFeatureStore;
import org.geotoolkit.data.FeatureReader;
import org.geotoolkit.data.FeatureStoreFactory;
import org.geotoolkit.data.FeatureStoreFinder;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.data.query.DefaultQueryCapabilities;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryCapabilities;
import org.geotoolkit.data.s57.model.DataSetIdentification;
import org.geotoolkit.data.s57.model.DataSetParameter;
import org.geotoolkit.data.s57.model.FeatureRecord;
import org.geotoolkit.data.s57.model.S57FileReader;
import org.geotoolkit.data.s57.model.S57Object;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.feature.DefaultName;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;
import org.opengis.parameter.ParameterValueGroup;

import org.geotoolkit.data.s57.model.S57Reader;
import org.geotoolkit.data.s57.model.S57UpdatedReader;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.version.VersionControl;
import org.geotoolkit.version.VersioningException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * S-57 FeatureStore.
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class S57FeatureStore extends AbstractFeatureStore{

    public static final String S57TYPECODE = "S-57 Code";

    private class FileDef{
        File mainFile;
        List<Name> fileTypes;

        //S-57 metadata/description records
        DataSetIdentification datasetIdentification;
        DataSetParameter datasetParameter;
        CoordinateReferenceSystem crs;

        //versioning informations
        private S57VersionControl history;

        /**
         * Rebuild history.
         * @return VersionHistory
         * @throws DataStoreException
         */
        private synchronized S57VersionControl loadHistory(final File mainFile) throws DataStoreException{
            if(history!=null) return history;

            final String baseName = mainFile.getName().substring(0, mainFile.getName().length()-4);
            final List<File> updateFiles = findUpdateFiles(baseName, null, null);
            updateFiles.add(0, mainFile);
            history = new S57VersionControl(updateFiles);
            return history;
        }

    }

    private final QueryCapabilities capa = new DefaultQueryCapabilities(false, true);

    /**
     * Root file for S57 data. Can be a folder.
     */
    private final File file;
    /**
     * Indicate if types must be aggregate.
     */
    private final boolean aggregate;

    /**
     * List of main S57 files, files that finish with the ".000" extension.
     */
    private List<File> mainFiles;

    private final Map<File,FileDef> fileDefs = new HashMap<>();
    /**
     * Feature types known.
     */
    private Map<Name,FeatureType> types;

    public S57FeatureStore(final ParameterValueGroup params) throws DataStoreException{
        super(params);

        final URL url = (URL) params.parameter(S57FeatureStoreFactory.URLP.getName().toString()).getValue();
        try {
            this.file = new File(url.toURI());
        } catch (URISyntaxException ex) {
            throw new DataStoreException(ex);
        }
        aggregate = (Boolean) params.parameter(S57FeatureStoreFactory.AGGREGATE.getName().toString()).getValue();

    }

    @Override
    public FeatureStoreFactory getFactory() {
        return FeatureStoreFinder.getFactoryById(S57FeatureStoreFactory.NAME);
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        return capa;
    }

    @Override
    public VersionControl getVersioning(Name typeName) throws VersioningException {
        try {
            typeCheck(typeName);
            final File file = getMainFileForTypeName(typeName);

            if (file == null) {
                throw new VersioningException("No main file!");
            }
            final FileDef def = fileDefs.get(file);
            return def.loadHistory(file);
        } catch (DataStoreException ex) {
            throw new VersioningException(ex);
        }
    }

    @Override
    public void refreshMetaModel() {
        //do nothing, types are not dynamic like a database
    }

    @Override
    public Set<Name> getNames() throws DataStoreException {
        loadTypes();
        return types.keySet();
    }

    @Override
    public FeatureType getFeatureType(Name typeName) throws DataStoreException {
        loadTypes();
        for(FeatureType ft : types.values()){
            if(DefaultName.match(ft.getName(),typeName)){
                return ft;
            }
        }
        throw new DataStoreException("Type "+typeName+" does not exist.");
    }

    private synchronized void loadTypes()throws DataStoreException {
        if(types!= null) return;
        types = new HashMap<>();

        findMainFiles(null);

        for (File mainFile : mainFiles) {
            final FileDef def = new FileDef();
            fileDefs.put(mainFile,def);
            def.mainFile = mainFile;

            if(aggregate){
                FeatureType type = S57Constants.ABSTRACT_S57FEATURETYPE;
                final FeatureTypeBuilder ftBuilder = new FeatureTypeBuilder();
                ftBuilder.copy(S57Constants.ABSTRACT_S57FEATURETYPE);
                final DefaultName typeName = new DefaultName(type.getName().getNamespaceURI(),
                        mainFile.getName().substring(0, mainFile.getName().length() - 4) +"_"+ type.getName().getLocalPart());
                ftBuilder.setName(typeName);
                type = ftBuilder.buildFeatureType();
                types.put(type.getName(),type);
                def.fileTypes = Collections.singletonList(type.getName());

                //get the metadatas we need
                final S57Reader reader = getS57Reader(mainFile, null);
                try{
                    while(reader.hasNext()){
                        final S57Object obj = reader.next();
                        if(obj instanceof DataSetIdentification){
                            def.datasetIdentification = (DataSetIdentification) obj;
                        }else if(obj instanceof DataSetParameter){
                            def.datasetParameter = (DataSetParameter) obj;
                            def.crs = def.datasetParameter.buildCoordinateReferenceSystem();
                        }
                    }
                }catch(IOException ex){
                    throw new DataStoreException(ex);
                }finally{
                    try {
                        reader.dispose();
                    } catch (IOException ex) {
                        //we tryed
                        getLogger().log(Level.WARNING, ex.getMessage(), ex);
                    }
                }


            }else{
                //search all types
                final S57Reader reader = getS57Reader(mainFile, null);
                try{
                    final List<Name> fileNames = new ArrayList<>();
                    while(reader.hasNext()){
                        final S57Object obj = reader.next();
                        if(obj instanceof DataSetIdentification){
                            def.datasetIdentification = (DataSetIdentification) obj;
                        }else if(obj instanceof DataSetParameter){
                            def.datasetParameter = (DataSetParameter) obj;
                            def.crs = def.datasetParameter.buildCoordinateReferenceSystem();
                        }else if(obj instanceof FeatureRecord){
                            final FeatureRecord rec = (FeatureRecord) obj;
                            final int objlCode = rec.code;
                            FeatureType type = TypeBanks.getFeatureType(objlCode, def.crs);
                            final FeatureTypeBuilder ftBuilder = new FeatureTypeBuilder();
                            ftBuilder.copy(type);

                            final DefaultName typeName = new DefaultName(type.getName().getNamespaceURI(),
                                    mainFile.getName().substring(0, mainFile.getName().length() - 4) +"_"+ type.getName().getLocalPart());
                            ftBuilder.setName(typeName);
                            type = ftBuilder.buildFeatureType();

                            if(type == null){
                                throw new DataStoreException("Unknown feature type OBJL : "+objlCode);
                            }
                            type.getUserData().put(S57TYPECODE, rec.code);
                            types.put(type.getName(),type);
                            fileNames.add(type.getName());
                        }
                    }
                    def.fileTypes = fileNames;
                }catch(IOException ex){
                    throw new DataStoreException(ex);
                }finally{
                    try {
                        reader.dispose();
                    } catch (IOException ex) {
                        //we tryed
                        getLogger().log(Level.WARNING, ex.getMessage(), ex);
                    }
                }
            }

        }
    }

    @Override
    public FeatureReader getFeatureReader(Query query) throws DataStoreException {
        final FeatureType ft = getFeatureType(query.getTypeName());

        /** find the wanted version */
        final Date vdate = query.getVersionDate();
        final String vlabel = query.getVersionLabel();
        final File file = getMainFileForTypeName(query.getTypeName());
        final FileDef def = fileDefs.get(file);
        S57Version wantedVersion = null;
        if(vdate!=null || vlabel != null){
            try{
                if(vdate!=null){
                    wantedVersion = (S57Version) def.loadHistory(file).getVersion(vdate);
                }else{
                    wantedVersion = (S57Version) def.loadHistory(file).getVersion(vlabel);
                }
            }catch(VersioningException ex){
                throw new DataStoreException(ex);
            }
        }

        final S57Reader s57reader = getS57Reader(file, wantedVersion);
        final FeatureReader reader = new S57FeatureReader(this,ft,
                (Integer)ft.getUserData().get(S57TYPECODE),s57reader,
                def.datasetIdentification,def.datasetParameter);
        return handleRemaining(reader, query);
    }

    private S57Reader getS57Reader(final File file, final S57Version version) throws DataStoreException{
        final FileDef def = fileDefs.get(file);
        final List<S57Version> versions = def.loadHistory(file).list();

        S57Reader reader = null;
        for(S57Version v : versions){
            if(reader==null){
                //create base reader
                reader = new S57FileReader();
                ((S57FileReader)reader).setInput(v.getFile());
                reader.setDsid(def.datasetIdentification);
            }else{
                //encapsulated with an update file
                reader = new S57UpdatedReader(reader,v.getFile());
                reader.setDsid(def.datasetIdentification);
            }
            if(v.equals(version)) break;
        }
        return reader;
    }

    /**
     * Search for the main file, it must be somewhere in the given path and have the
     * extension .000
     * @return File if found, DataStoreException otherwise
     */
    private void findMainFiles(File root) throws DataStoreException {
        if (mainFiles == null) {
            mainFiles = new ArrayList<File>();
        }

        if (root == null) {
            root = file;
        }

        if (root.isDirectory()) {
            //search sub files
            for (File f : root.listFiles()) {
                findMainFiles(f);
            }
        }

        if (root.getName().endsWith(".000")) {
           mainFiles.add(root);
        }
    }

    /**
     * Search for update files.
     * @param updateFiles
     * @param root
     * @return List<File>, never null
     */
    private List<File> findUpdateFiles(String baseName, List<File> updateFiles, File root) throws DataStoreException {
        if (root == null) {
            root = file;
        }
        if (updateFiles == null) {
            updateFiles = new ArrayList<>();
        }

        if (root.isDirectory()) {
            //search sub files
            for (File f : root.listFiles()) {
                findUpdateFiles(baseName, updateFiles, f);
            }
        } else {
            if (root.getName().startsWith(baseName) && !root.getName().endsWith(".000")) {
                updateFiles.add(root);
            }
        }

        //sort files
        Collections.sort(updateFiles);
        return updateFiles;
    }


    private File getMainFileForTypeName(final Name typeName) {
        for (Entry<File,FileDef> entry : fileDefs.entrySet()) {
            final List<Name> names = entry.getValue().fileTypes;
            if (names.contains(typeName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    // WRITING OPERATIONS //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void createFeatureType(Name typeName, FeatureType featureType) throws DataStoreException {
        throw new DataStoreException("Writing not supported yet.");
    }

    @Override
    public void updateFeatureType(Name typeName, FeatureType featureType) throws DataStoreException {
        throw new DataStoreException("Writing not supported yet.");
    }

    @Override
    public void deleteFeatureType(Name typeName) throws DataStoreException {
        throw new DataStoreException("Writing not supported yet.");
    }

    @Override
    public List<FeatureId> addFeatures(Name groupName, Collection<? extends Feature> newFeatures, Hints hints) throws DataStoreException {
        throw new DataStoreException("Writing not supported yet.");
    }

    @Override
    public void updateFeatures(Name groupName, Filter filter, Map<? extends PropertyDescriptor, ? extends Object> values) throws DataStoreException {
        throw new DataStoreException("Writing not supported yet.");
    }

    @Override
    public void removeFeatures(Name groupName, Filter filter) throws DataStoreException {
        throw new DataStoreException("Writing not supported yet.");
    }

    @Override
    public FeatureWriter getFeatureWriter(Name typeName, Filter filter, Hints hints) throws DataStoreException {
        return handleWriter(typeName, filter, hints);
    }

}