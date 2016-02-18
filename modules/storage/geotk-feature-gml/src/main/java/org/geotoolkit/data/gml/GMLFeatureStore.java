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
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.AbstractFeatureStore;
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
import org.geotoolkit.feature.type.ComplexType;
import org.geotoolkit.feature.type.FeatureType;
import org.opengis.util.GenericName;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.geotoolkit.feature.xml.jaxp.JAXPStreamFeatureReader;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.storage.DataFileStore;
import org.geotoolkit.util.collection.CloseableIterator;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;
import org.opengis.parameter.ParameterValueGroup;

/**
 * GML feature store.
 *
 * @author Johann Sorel (Geomatys)
 */
public class GMLFeatureStore extends AbstractFeatureStore implements DataFileStore {

    static final QueryCapabilities CAPABILITIES = new DefaultQueryCapabilities(false);

    private final Path file;
    private String name;
    private FeatureType featureType;
    private Boolean longitudeFirst;

    //all types
    private final Map<GenericName, Object> cache = new HashMap<>();

    /**
     * @deprecated use {@link #GMLFeatureStore(Path)} or {@link #GMLFeatureStore(ParameterValueGroup)} instead
     */
    @Deprecated
    public GMLFeatureStore(final File f) throws MalformedURLException, DataStoreException{
        this(f.toPath());
    }

    public GMLFeatureStore(final Path f) throws MalformedURLException, DataStoreException{
        this(toParameters(f));
    }

    public GMLFeatureStore(final ParameterValueGroup params) throws DataStoreException {
        super(params);

        final URI uri = (URI) params.parameter(GMLFeatureStoreFactory.PATH.getName().toString()).getValue();
        this.file = Paths.get(uri);

        final String path = uri.toString();
        final int slash = Math.max(0, path.lastIndexOf('/') + 1);
        int dot = path.indexOf('.', slash);
        if (dot < 0) {
            dot = path.length();
        }
        this.name = path.substring(slash, dot);
        this.longitudeFirst = (Boolean) params.parameter(GMLFeatureStoreFactory.LONGITUDE_FIRST.getName().toString()).getValue();
    }

    private static ParameterValueGroup toParameters(final Path f) throws MalformedURLException{
        final ParameterValueGroup params = GMLFeatureStoreFactory.PARAMETERS_DESCRIPTOR.createValue();
        Parameters.getOrCreate(GMLFeatureStoreFactory.PATH, params).setValue(f.toUri());
        return params;
    }

    @Override
    public FeatureStoreFactory getFactory() {
        return FeatureStoreFinder.getFactoryById(GMLFeatureStoreFactory.NAME);
    }

    @Override
    public synchronized Set<GenericName> getNames() throws DataStoreException {
        if(featureType==null){
            final JAXPStreamFeatureReader reader = new JAXPStreamFeatureReader();
            reader.getProperties().put(JAXPStreamFeatureReader.LONGITUDE_FIRST, longitudeFirst);
            reader.setReadEmbeddedFeatureType(true);
            try {
                FeatureReader ite = reader.readAsStream(file);
                featureType = ite.getFeatureType();
            } catch (IOException | XMLStreamException ex) {
                throw new DataStoreException(ex.getMessage(),ex);
            } finally{
                reader.dispose();
            }
        }
        return Collections.singleton(featureType.getName());
    }

    @Override
    public FeatureType getFeatureType(GenericName typeName) throws DataStoreException {
        typeCheck(typeName);
        return featureType;
    }

    @Override
    public List<ComplexType> getFeatureTypeHierarchy(GenericName typeName) throws DataStoreException {
        return super.getFeatureTypeHierarchy(typeName);
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public void refreshMetaModel() {
    }

    @Override
    public Path[] getDataFiles() throws DataStoreException {
        return new Path[]{file};
    }

    @Override
    public FeatureReader getFeatureReader(Query query) throws DataStoreException {
        typeCheck(query.getTypeName());

        final JAXPStreamFeatureReader reader = new JAXPStreamFeatureReader(featureType);
        reader.getProperties().put(JAXPStreamFeatureReader.LONGITUDE_FIRST, longitudeFirst);
        final CloseableIterator ite;
        try {
            ite = reader.readAsStream(file);
        } catch (IOException | XMLStreamException ex) {
            reader.dispose();
            throw new DataStoreException(ex.getMessage(),ex);
        } finally{
            //do not dispose, the iterator is closeable and will close the reader
            //reader.dispose();
        }

        final FeatureReader freader = GenericWrapFeatureIterator.wrapToReader(ite,featureType);
        return handleRemaining(freader, query);
    }

    // WRITING SUPPORT : TODO //////////////////////////////////////////////////

    @Override
    public void createFeatureType(GenericName typeName, FeatureType featureType) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public void updateFeatureType(GenericName typeName, FeatureType featureType) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public void deleteFeatureType(GenericName typeName) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public FeatureWriter getFeatureWriter(GenericName typeName, Filter filter, Hints hints) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public List<FeatureId> addFeatures(GenericName groupName, Collection<? extends Feature> newFeatures, Hints hints) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public void updateFeatures(GenericName groupName, Filter filter, Map<? extends PropertyDescriptor, ? extends Object> values) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

    @Override
    public void removeFeatures(GenericName groupName, Filter filter) throws DataStoreException {
        throw new DataStoreException("Writing not supported");
    }

}
