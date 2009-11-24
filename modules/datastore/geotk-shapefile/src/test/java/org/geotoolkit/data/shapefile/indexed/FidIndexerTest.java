/*
 *    GeotoolKit - An Open source Java GIS Toolkit
 *    http://geotoolkit.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.data.shapefile.indexed;

import java.io.IOException;

import org.geotoolkit.data.FeatureSource;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.shapefile.ShpFiles;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class FidIndexerTest extends FIDTestCase {
    public  FidIndexerTest( ) throws IOException {
        super("FidIndexerTest");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'org.geotoolkit.index.fid.FidIndexer.generate(URL)'
     */
    public void testGenerate() throws Exception {
        ShpFiles shpFiles = new ShpFiles(backshp.toURL());
        FidIndexer.generate(shpFiles);

        IndexedShapefileDataStore ds = new IndexedShapefileDataStore(backshp
                .toURL(), null, false, false, IndexType.NONE);

        FeatureSource<SimpleFeatureType, SimpleFeature> fs = ds.getFeatureSource(ds.getTypeNames()[0]);
        int features = fs.getCount(QueryBuilder.all(fs.getName()));

        IndexedFidReader reader = new IndexedFidReader(shpFiles);

        try {
            assertEquals(features, reader.getCount());

            int i = 1;

            while (reader.hasNext()) {
                assertEquals(shpFiles.getTypeName() + "." + i, reader.next());
                assertEquals(shpFiles.getTypeName() + "." + i, i - 1, reader.currentSHXIndex());
                i++;
            }

            assertEquals(features, i - 1);
        } finally {
            reader.close();
        }
    }
}
