/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Johann Sorel
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

package org.geotoolkit.data.dbf;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geotoolkit.data.AbstractReadingTests;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.feature.AttributeDescriptorBuilder;
import org.geotoolkit.feature.AttributeTypeBuilder;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.apache.sis.storage.DataStoreException;
import static org.junit.Assert.assertNotNull;
import org.geotoolkit.feature.type.FeatureType;
import org.opengis.util.GenericName;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel
 * @module pending
 */
public class DBFReadingTest extends AbstractReadingTests{

    private final DbaseFileFeatureStore store;
    private final Set<GenericName> names = new HashSet<GenericName>();
    private final List<ExpectedResult> expecteds = new ArrayList<ExpectedResult>();

    public DBFReadingTest() throws DataStoreException, NoSuchAuthorityCodeException, FactoryException, IOException{

        final File file = new File("src/test/resources/org/geotoolkit/data/dbf/sample.dbf");
        final String ns = "http://test.com";
        store = new DbaseFileFeatureStore(file.toPath(), ns);

        for(GenericName n : store.getNames()){
            FeatureType ft = store.getFeatureType(n);
            assertNotNull(ft);
        }

        final FeatureTypeBuilder builder = new FeatureTypeBuilder();

        final AttributeTypeBuilder buildAtt = new AttributeTypeBuilder();
        final AttributeDescriptorBuilder buildDesc = new AttributeDescriptorBuilder();
        
        GenericName name = NamesExt.create("http://test.com", "sample");
        builder.reset();
        builder.setName(name);
        
        buildAtt.reset();
        buildAtt.setName(ns, "N1");
        buildAtt.setBinding(Double.class);
        buildAtt.setLength(5);
        buildDesc.reset();
        buildDesc.setName(ns, "N1");
        buildDesc.setNillable(true);
        buildDesc.setType(buildAtt.buildType());
        builder.add(buildDesc.buildDescriptor());
        
        buildAtt.reset();
        buildAtt.setName(ns, "N2");
        buildAtt.setBinding(Double.class);
        buildAtt.setLength(5);
        buildDesc.reset();
        buildDesc.setName(ns, "N2");
        buildDesc.setNillable(true);
        buildDesc.setType(buildAtt.buildType());
        builder.add(buildDesc.buildDescriptor());
        
        buildAtt.reset();
        buildAtt.setName(ns, "N3");
        buildAtt.setBinding(String.class);
        buildAtt.setLength(6);
        buildDesc.reset();
        buildDesc.setName(ns, "N3");
        buildDesc.setNillable(true);
        buildDesc.setType(buildAtt.buildType());
        builder.add(buildDesc.buildDescriptor());
        
        final FeatureType type3 = builder.buildFeatureType();
        
        names.add(name);
        expecteds.add(new ExpectedResult(name,type3,3,null));
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
