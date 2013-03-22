package org.geotoolkit.data.mapinfo.mif;

import org.geotoolkit.data.*;
import org.geotoolkit.data.query.DefaultQueryCapabilities;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryCapabilities;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.feature.DefaultName;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.storage.DataStoreException;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;
import org.opengis.parameter.ParameterValueGroup;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A featureStore for MapInfo exchange format MIF-MID.
 *
 * @author Alexis Manin (Geomatys)
 *         Date : 21/02/13
 */
public class MIFFeatureStore extends AbstractFeatureStore {

    private final MIFManager manager;

    private final QueryCapabilities queryCapabilities = new DefaultQueryCapabilities(false);

    /**
     * Creates a new instance of MIFFeatureStore.
     *
     * @param url The URL of the MIF file to use for this DataStore.
     *
     * @throws DataStoreException If we got a problem getting needed files.
     */
    public MIFFeatureStore(final URL url) throws DataStoreException {
        this(url, null);
    }

    /**
     * This sets the datastore's namespace during construction (so the schema -
     * FeatureType - will have the correct value) You can call this with
     * namespace = null, but I suggest you give it an actual namespace.
     *
     * @param url
     * @param namespace
     */
    public MIFFeatureStore(final URL url, final String namespace) throws DataStoreException {
        this(toParameter(url, namespace));
    }

    public MIFFeatureStore(final ParameterValueGroup params) throws DataStoreException {
        super(params);

        final URL filePath = (URL) params.parameter(MIFFeatureStoreFactory.URLP.getName().toString()).getValue();
        try {
            manager = new MIFManager(filePath);
        } catch (Exception e) {
            throw new DataStoreException("Datastore can't reach target data.", e);
        }
    }

    private static ParameterValueGroup toParameter(final URL url, final String namespace) {
        final ParameterValueGroup params = MIFFeatureStoreFactory.PARAMETERS_DESCRIPTOR.createValue();
        Parameters.getOrCreate(MIFFeatureStoreFactory.URLP, params).setValue(url);
        Parameters.getOrCreate(MIFFeatureStoreFactory.NAMESPACE, params).setValue(namespace);
        return params;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureStoreFactory getFactory() {
        return FeatureStoreFinder.getFactoryById(MIFFeatureStoreFactory.NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Name> getNames() throws DataStoreException {
        return manager.getTypeNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createSchema(Name typeName, FeatureType featureType) throws DataStoreException {
        try {
            manager.addSchema(typeName, featureType);
        } catch (URISyntaxException e) {
            throw new DataStoreException("We're unable to add a schema because we can't access source files.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSchema(Name typeName, FeatureType featureType) throws DataStoreException {
        throw new DataStoreException("Can not update MIF schema.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSchema(Name typeName) throws DataStoreException {
        manager.deleteSchema(typeName);
        removeFeatures(typeName, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureType getFeatureType(Name typeName) throws DataStoreException {
        return manager.getType(typeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryCapabilities getQueryCapabilities() {
        return queryCapabilities;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FeatureId> addFeatures(Name groupName, Collection<? extends Feature> newFeatures, Hints hints) throws DataStoreException {
        return handleAddWithFeatureWriter(groupName, newFeatures, hints);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFeatures(Name groupName, Filter filter, Map<? extends PropertyDescriptor, ? extends Object> values) throws DataStoreException {
        handleUpdateWithFeatureWriter(groupName, filter, values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFeatures(Name groupName, Filter filter) throws DataStoreException {
        handleRemoveWithFeatureWriter(groupName, filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureReader getFeatureReader(Query query) throws DataStoreException {
        typeCheck(query.getTypeName());
        return handleRemaining(new MIFFeatureReader(manager, query.getTypeName()), query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureWriter getFeatureWriter(Name typeName, Filter filter, Hints hints) throws DataStoreException {
        typeCheck(typeName);
        final MIFFeatureReader reader = new MIFFeatureReader(manager, typeName);
        final MIFFeatureWriter writer = new MIFFeatureWriter(manager, reader);
        return  writer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshMetaModel() {
        manager.refreshMetaModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshMetaModel(DefaultName name) throws DataStoreException {
        refreshMetaModel();
    }

    public void setDelimiter(char newDelimiter) {
        manager.setDelimiter(newDelimiter);
    }

}
