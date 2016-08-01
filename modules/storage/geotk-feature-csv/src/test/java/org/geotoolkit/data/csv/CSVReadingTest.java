/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Geomatys
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

package org.geotoolkit.data.csv;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotoolkit.data.AbstractReadingTests;
import org.geotoolkit.data.FeatureStore;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.apache.sis.geometry.GeneralEnvelope;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.apache.sis.referencing.CRS;

import org.opengis.util.GenericName;
import org.opengis.util.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class CSVReadingTest extends AbstractReadingTests{

    private final CSVFeatureStore store;
    private final Set<GenericName> names = new HashSet<GenericName>();
    private final List<ExpectedResult> expecteds = new ArrayList<ExpectedResult>();

    public CSVReadingTest() throws DataStoreException, NoSuchAuthorityCodeException, FactoryException, IOException{

        final File file = File.createTempFile("temp2", "csv");
        file.deleteOnExit();
        store = new CSVFeatureStore(file, "http://test.com",';');

        final GeometryFactory gf = new GeometryFactory();
        final FeatureTypeBuilder builder = new FeatureTypeBuilder();

        final String namespace = "http://test.com";

        GenericName name = NamesExt.create("http://test.com", "TestSchema3");
        builder.reset();
        builder.setName(name);
        builder.add(NamesExt.create(namespace, "geometry"), Geometry.class, CRS.forCode("EPSG:27582"));
        builder.add(NamesExt.create(namespace, "stringProp"), String.class);
        builder.add(NamesExt.create(namespace, "intProp"), Integer.class);
        builder.add(NamesExt.create(namespace, "doubleProp"), Double.class);
        final FeatureType type3 = builder.buildFeatureType();
        store.createFeatureType(name,type3);

        //create a few features
        FeatureWriter writer = store.getFeatureWriterAppend(name);
        try{
            Feature f = writer.next();
            f.setPropertyValue("geometry", gf.createPoint(new Coordinate(10, 11)));
            f.setPropertyValue("stringProp", "hop1");
            f.setPropertyValue("intProp", 15);
            f.setPropertyValue("doubleProp", 32.2);
            writer.write();

            f = writer.next();
            f.setPropertyValue("geometry", gf.createPoint(new Coordinate(-5, -1)));
            f.setPropertyValue("stringProp", "hop3");
            f.setPropertyValue("intProp", 18);
            f.setPropertyValue("doubleProp", 412.10);
            writer.write();


        }finally{
            writer.close();
        }

        GeneralEnvelope env = new GeneralEnvelope(CRS.forCode("EPSG:27582"));
        env.setRange(0, -5, 10);
        env.setRange(1, -1, 11);

        names.add(name);
        expecteds.add(new ExpectedResult(name,type3,2,env));
    }

    @Override
    protected synchronized FeatureStore getDataStore() {
        return store;
    }

    @Override
    protected Set<GenericName> getExpectedNames() {
        return names;
    }

    @Override
    protected List<ExpectedResult> getReaderTests() {
        return expecteds;
    }

}
