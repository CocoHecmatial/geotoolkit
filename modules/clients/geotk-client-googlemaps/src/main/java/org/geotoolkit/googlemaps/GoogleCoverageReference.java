/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Geomatys
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
package org.geotoolkit.googlemaps;

import org.geotoolkit.googlemaps.model.GoogleMapsPyramidSet;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.feature.type.Name;
import org.geotoolkit.storage.coverage.AbstractPyramidalCoverageReference;
import org.geotoolkit.storage.coverage.PyramidSet;

/**
 * GoogleMaps coverage reference.
 * 
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class GoogleCoverageReference extends AbstractPyramidalCoverageReference {

    private final GoogleMapsPyramidSet set;

    GoogleCoverageReference(final StaticGoogleMapsClient server, final Name name, boolean cacheImage) throws DataStoreException{
        super(server,name,0);
        this.set = new GoogleMapsPyramidSet(this,cacheImage);
    }

    public GetMapRequest createGetMap() {
        return new DefaultGetMap( (StaticGoogleMapsClient)store, name.tip().toString());
    }

    @Override
    public PyramidSet getPyramidSet() throws DataStoreException {
        return set;
    }

}
