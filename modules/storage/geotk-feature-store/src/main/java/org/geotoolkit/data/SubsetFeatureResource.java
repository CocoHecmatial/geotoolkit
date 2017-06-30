/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2017, Geomatys
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
package org.geotoolkit.data;

import java.util.stream.Stream;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryUtilities;
import org.geotoolkit.feature.FeatureTypeExt;
import org.geotoolkit.feature.ReprojectFeatureType;
import org.geotoolkit.feature.ViewFeatureType;
import org.geotoolkit.storage.AbstractResource;
import org.geotoolkit.storage.StorageListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.metadata.Metadata;

/**
 * Default subset feature resource.
 *
 * @author Johann Sorel (Geomatys)
 */
final class SubsetFeatureResource extends AbstractResource implements FeatureResource, FeatureStoreListener {

    private final FeatureStoreListener.Weak weakListener = new StorageListener.Weak(this);

    private final FeatureResource parent;
    private final Query query;
    private FeatureType type;

    public SubsetFeatureResource(FeatureResource parent, Query query) {
        super(parent.getIdentifier());
        this.parent = parent;
        this.query = query;
        weakListener.registerSource(parent);
    }

    @Override
    public synchronized FeatureType getType() throws DataStoreException {
        if (type==null) {
            type = parent.getType();
            final String[] properties = query.getPropertyNames();
            if (properties!=null && FeatureTypeExt.isAllProperties(type, properties)) {
                type = new ViewFeatureType(type, properties);
            }
            if(query.getCoordinateSystemReproject()!=null){
                type = new ReprojectFeatureType(type, query.getCoordinateSystemReproject());
            }
        }
        return type;
    }

    @Override
    public FeatureResource subset(Query query) throws DataStoreException {
        final Query merge = QueryUtilities.subQuery(this.query, query);
        return new SubsetFeatureResource(parent, merge);
    }

    @Override
    public Stream<Feature> features() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return parent.getMetadata();
    }

    @Override
    public void structureChanged(FeatureStoreManagementEvent event) {
        //forward events
        sendStructureEvent(event.copy(this));
    }

    @Override
    public void contentChanged(FeatureStoreContentEvent event) {
        //forward events
        sendContentEvent(event.copy(this));
    }

}
