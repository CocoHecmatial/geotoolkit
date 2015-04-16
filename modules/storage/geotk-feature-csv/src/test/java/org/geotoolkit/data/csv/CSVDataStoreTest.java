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

import java.util.Map;
import java.util.Collections;
import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import org.geotoolkit.data.AbstractFileFeatureStoreFactory;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.FeatureStoreFinder;
import org.geotoolkit.data.FeatureReader;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.factory.HintsPending;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.apache.sis.referencing.CommonCRS;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.Name;
import org.geotoolkit.feature.type.PropertyDescriptor;
import static org.junit.Assert.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class CSVDataStoreTest {

    public CSVDataStoreTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreate() throws Exception{

        File f = File.createTempFile("test", ".csv");
        f.deleteOnExit();

        final FeatureStore ds = FeatureStoreFinder.open(
                (Map)Collections.singletonMap(AbstractFileFeatureStoreFactory.URLP.getName().getCode(),
                f.toURL()));
        assertNotNull(ds);
        
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test");
        ftb.add("integerProp", Integer.class);
        ftb.add("doubleProp", Double.class);
        ftb.add("stringProp", String.class);
        ftb.add("geometryProp", Geometry.class, CommonCRS.WGS84.normalizedGeographic());
        FeatureType sft = ftb.buildFeatureType();
        ds.createFeatureType(sft.getName(), sft);
        Name name = ds.getNames().iterator().next();

        assertEquals(1, ds.getNames().size());

        for(Name n : ds.getNames()){
            FeatureType ft = ds.getFeatureType(n);
            for(PropertyDescriptor desc : sft.getDescriptors()){
                PropertyDescriptor td = ft.getDescriptor(desc.getName().getLocalPart());
                assertNotNull(td);
                assertEquals(td.getType().getBinding(), desc.getType().getBinding());
            }
        }

        FeatureWriter fw = ds.getFeatureWriterAppend(name);
        try{
            Feature feature = fw.next();
            fw.write();
            feature = fw.next();
            fw.write();
            feature = fw.next();
            fw.write();
        }finally{
            fw.close();
        }

        FeatureReader reader = ds.getFeatureReader(QueryBuilder.all(name));
        int number = 0;
        try{
            while(reader.hasNext()){
                number++;
                reader.next();
            }
        }finally{
            reader.close();
        }

        assertEquals(3, number);


        //test with hint
        QueryBuilder qb = new QueryBuilder(name);
        qb.setHints(new Hints(HintsPending.FEATURE_DETACHED, Boolean.FALSE));
        reader = ds.getFeatureReader(qb.buildQuery());
        number = 0;
        try{
            while(reader.hasNext()){
                number++;
                reader.next();
            }
        }finally{
            reader.close();
        }

        assertEquals(3, number);

    }

}
