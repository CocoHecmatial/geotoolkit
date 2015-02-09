/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2015, Geomatys
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
package org.geotoolkit.data.gml;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.AbstractFeatureStore;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureReader;
import org.geotoolkit.data.FeatureStoreFactory;
import org.geotoolkit.data.FeatureStoreFinder;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.data.memory.GenericWrapFeatureIterator;
import org.geotoolkit.data.query.DefaultQueryCapabilities;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryCapabilities;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.Name;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.geotoolkit.feature.xml.jaxp.JAXPStreamFeatureReader;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.storage.DataFileStore;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;
import org.opengis.parameter.ParameterValueGroup;

/**
 * GML feature store.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GMLFeatureStore extends AbstractFeatureStore implements DataFileStore {

    private static final QueryCapabilities CAPABILITIES = new DefaultQueryCapabilities(false);

    static final String BUNDLE_PATH = "org/geotoolkit/gml/bundle";

    private final File file;
    private String name;
    private FeatureType featureType;

    public GMLFeatureStore(final File f) throws MalformedURLException, DataStoreException{
        this(toParameters(f));
    }

    public GMLFeatureStore(final ParameterValueGroup params) throws DataStoreException {
        super(params);

        final URL url = (URL) params.parameter(GMLFeatureStoreFactory.URLP.getName().toString()).getValue();
        try {
            this.file = new File(url.toURI());
        } catch (URISyntaxException ex) {
            throw new DataStoreException(ex);
        }

        final String path = url.toString();
        final int slash = Math.max(0, path.lastIndexOf('/') + 1);
        int dot = path.indexOf('.', slash);
        if (dot < 0) {
            dot = path.length();
        }
        this.name = path.substring(slash, dot);
    }

    private static ParameterValueGroup toParameters(final File f) throws MalformedURLException{
        final ParameterValueGroup params = GMLFeatureStoreFactory.PARAMETERS_DESCRIPTOR.createValue();
        Parameters.getOrCreate(GMLFeatureStoreFactory.URLP, params).setValue(f.toURL());
        return params;
    }

    @Override
    public FeatureStoreFactory getFactory() {
        return FeatureStoreFinder.getFactoryById(GMLFeatureStoreFactory.NAME);
    }

    @Override
    public synchronized Set<Name> getNames() throws DataStoreException {
        if(featureType==null){
            final JAXPStreamFeatureReader reader = new JAXPStreamFeatureReader();
            reader.setReadEmbeddedFeatureType(true);
            try {
                Object obj = reader.read(file);
                if(obj instanceof FeatureCollection){
                    featureType = ((FeatureCollection)obj).getFeatureType();
                }else{
                    throw new DataStoreException("File does not contain a FeatureCollection, found a "+obj);
                }
            } catch (IOException | XMLStreamException ex) {
                throw new DataStoreException(ex.getMessage(),ex);
            } finally{
                reader.dispose();
            }
        }
        return Collections.singleton(featureType.getName());
    }

    @Override
    public FeatureType getFeatureType(Name typeName) throws DataStoreException {
        typeCheck(typeName);
        return featureType;
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public void refreshMetaModel() {
    }

    @Override
    public File[] getDataFiles() throws DataStoreException {
        return new File[]{file};
    }

    @Override
    public FeatureReader getFeatureReader(Query query) throws DataStoreException {
        typeCheck(query.getTypeName());

        final JAXPStreamFeatureReader reader = new JAXPStreamFeatureReader(featureType);
        final FeatureCollection col;
        try {
            Object obj = reader.read(file);
            if(obj instanceof FeatureCollection){
                col = (FeatureCollection)obj;
            }else{
                throw new DataStoreException("File does not contain a FeatureCollection, found a "+obj);
            }
        } catch (IOException | XMLStreamException ex) {
            throw new DataStoreException(ex.getMessage(),ex);
        } finally{
            reader.dispose();
        }

        final FeatureReader freader = GenericWrapFeatureIterator.wrapToReader(col.iterator(),featureType);
        return handleRemaining(freader, query);
    }

    // WRITING SUPPORT : TODO //////////////////////////////////////////////////

    @Override
    public void createFeatureType(Name typeName, FeatureType featureType) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public void updateFeatureType(Name typeName, FeatureType featureType) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public void deleteFeatureType(Name typeName) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public FeatureWriter getFeatureWriter(Name typeName, Filter filter, Hints hints) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public List<FeatureId> addFeatures(Name groupName, Collection<? extends Feature> newFeatures, Hints hints) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public void updateFeatures(Name groupName, Filter filter, Map<? extends PropertyDescriptor, ? extends Object> values) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public void removeFeatures(Name groupName, Filter filter) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

}
