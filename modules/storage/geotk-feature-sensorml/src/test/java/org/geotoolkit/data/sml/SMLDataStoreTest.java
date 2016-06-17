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

package org.geotoolkit.data.sml;


import com.vividsolutions.jts.geom.Point;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotoolkit.data.AbstractReadingTests;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.apache.sis.geometry.GeneralEnvelope;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.ScriptRunner;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.feature.type.ComplexType;

import org.opengis.util.GenericName;

import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.storage.DataStores;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class SMLDataStoreTest extends AbstractReadingTests{


    private static final String SML_NAMESPACE = "http://www.opengis.net/sml/1.0";
    private static final String GML_NAMESPACE = "http://www.opengis.net/gml";

    //root types
    private final static GenericName SML_TN_SYSTEM         = NamesExt.create(SML_NAMESPACE, "System");
    private final static GenericName SML_TN_COMPONENT      = NamesExt.create(SML_NAMESPACE, "Component");
    private final static GenericName SML_TN_PROCESSCHAIN   = NamesExt.create(SML_NAMESPACE, "ProcessChain");
    private final static GenericName SML_TN_PROCESSMODEL   = NamesExt.create(SML_NAMESPACE, "ProcessModel");
    private final static GenericName SML_TN_DATASOURCETYPE = NamesExt.create(SML_NAMESPACE, "DataSourceType");

    //subTypes
    private final static GenericName SML_KEYWORD_LIST      = NamesExt.create(SML_NAMESPACE, "KeywordList");
    private final static GenericName SML_INPUT_LIST        = NamesExt.create(SML_NAMESPACE, "InputList");
    private final static GenericName SML_OUTPUT_LIST       = NamesExt.create(SML_NAMESPACE, "OutputList");
    private final static GenericName SML_INPUT             = NamesExt.create(SML_NAMESPACE, "Input");
    private final static GenericName SML_OUTPUT            = NamesExt.create(SML_NAMESPACE, "Output");

    // Shared attributes
    private static final GenericName ATT_DESC        = NamesExt.create(GML_NAMESPACE, "description");
    private static final GenericName ATT_NAME        = NamesExt.create(GML_NAMESPACE, "name");
    private static final GenericName ATT_CODESPACE   = NamesExt.create(GML_NAMESPACE, "codespace");
    private static final GenericName ATT_KEYWORDS    = NamesExt.create(SML_NAMESPACE, "keywords");
    private static final GenericName ATT_KEYWORD     = NamesExt.create(SML_NAMESPACE, "keyword");
    private static final GenericName ATT_LOCATION    = NamesExt.create(SML_NAMESPACE, "location");
    private static final GenericName ATT_PHENOMENONS = NamesExt.create(SML_NAMESPACE, "phenomenons");
    private static final GenericName ATT_SMLTYPE     = NamesExt.create(SML_NAMESPACE, "smltype");
    private static final GenericName ATT_SMLREF      = NamesExt.create(SML_NAMESPACE, "smlref");
    private static final GenericName ATT_INPUTS      = NamesExt.create(SML_NAMESPACE, "inputs");
    private static final GenericName ATT_INPUT       = NamesExt.create(SML_NAMESPACE, "input");
    private static final GenericName ATT_OUTPUTS     = NamesExt.create(SML_NAMESPACE, "outputs");
    private static final GenericName ATT_OUTPUT      = NamesExt.create(SML_NAMESPACE, "output");
    // attribute for sml:System or sml:ProcessChain
    private static final GenericName ATT_PRODUCER = NamesExt.create(SML_NAMESPACE, "producer");
    private static final GenericName ATT_COMPONENTS = NamesExt.create(SML_NAMESPACE, "components");
    // attribute for sml:ProccessModel
    private static final GenericName ATT_METHOD = NamesExt.create(SML_NAMESPACE, "method");
    // attribute for sml:DatasourceType
    private static final GenericName ATT_CHARACTERISTICS = NamesExt.create(SML_NAMESPACE, "characteristics");


    private static DefaultDataSource ds;
    private static FeatureStore store;
    private static Set<GenericName> names = new HashSet<GenericName>();
    private static List<ExpectedResult> expecteds = new ArrayList<ExpectedResult>();
    static{
        try{
            final String url = "jdbc:derby:memory:TestSML;create=true";
            ds = new DefaultDataSource(url);

            Connection con = ds.getConnection();

            final ScriptRunner exec = new ScriptRunner(con);
            exec.run(SMLDataStoreTest.class.getResourceAsStream("/org/geotoolkit/sql/structure-mdweb.sql"));
            exec.run(SMLDataStoreTest.class.getResourceAsStream("/org/geotoolkit/sql/mdweb-base-data.sql"));
            exec.run(SMLDataStoreTest.class.getResourceAsStream("/org/geotoolkit/sql/ISO19115-base-data.sql"));
            exec.run(SMLDataStoreTest.class.getResourceAsStream("/org/geotoolkit/sql/mdweb-user-data.sql"));
            exec.run(SMLDataStoreTest.class.getResourceAsStream("/org/geotoolkit/sql/sml-schema.sql"));
            exec.run(SMLDataStoreTest.class.getResourceAsStream("/org/geotoolkit/sql/sml-data.sql"));

            final Map params = new HashMap<String, Object>();
            params.put("dbtype", "SML");
            params.put(SMLFeatureStoreFactory.SGBDTYPE.getName().toString(), "derby");
            params.put(SMLFeatureStoreFactory.DERBYURL.getName().toString(), url);
            store = (FeatureStore) DataStores.open(params);


            final FeatureTypeBuilder featureTypeBuilder = new FeatureTypeBuilder();

            //subType KeywordList
            featureTypeBuilder.reset();
            featureTypeBuilder.setName(SML_KEYWORD_LIST);
            featureTypeBuilder.add(ATT_CODESPACE,  String.class, 0, 1, true, null); // TODO xml attribute ?
            featureTypeBuilder.add(ATT_KEYWORD,    List.class, 0, Integer.MAX_VALUE, true, null);

            final ComplexType kwList = featureTypeBuilder.buildType();

            //subType Input
            featureTypeBuilder.reset();
            featureTypeBuilder.setName(SML_INPUT);
            featureTypeBuilder.add(ATT_NAME,     String.class, 0, 1, true, null); // TODO xml attribute ?
            featureTypeBuilder.add(ATT_DESC,     String.class, 0, 1, true, null);

            final ComplexType input = featureTypeBuilder.buildType();

            //subType InputList
            featureTypeBuilder.reset();
            featureTypeBuilder.setName(SML_INPUT_LIST);
            featureTypeBuilder.add(input, ATT_INPUT,    null, 0, Integer.MAX_VALUE, true, null);

            final ComplexType inList = featureTypeBuilder.buildType();

            //subType Output
            featureTypeBuilder.reset();
            featureTypeBuilder.setName(SML_OUTPUT);
            featureTypeBuilder.add(ATT_NAME,     String.class, 0, 1, true, null); // TODO xml attribute ?
            featureTypeBuilder.add(ATT_DESC,     String.class, 0, 1, true, null);

            final ComplexType output = featureTypeBuilder.buildType();

            //subType OutputList
            featureTypeBuilder.reset();
            featureTypeBuilder.setName(SML_OUTPUT_LIST);
            featureTypeBuilder.add(output, ATT_OUTPUT,    null, 0, Integer.MAX_VALUE, true, null);

            final ComplexType outList = featureTypeBuilder.buildType();

            // Feature type sml:System
            featureTypeBuilder.reset();
            featureTypeBuilder.setName(SML_TN_SYSTEM);
            featureTypeBuilder.add(ATT_DESC,        String.class, 0, 1, true, null);
            featureTypeBuilder.add(ATT_NAME,        String.class, 1, 1, false, null);
            featureTypeBuilder.add(kwList, ATT_KEYWORDS,    null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_LOCATION,    Point.class, 1, 1, false, null);
            featureTypeBuilder.add(ATT_PHENOMENONS, List.class, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_SMLTYPE,     String.class, 1, 1, true, null);
            featureTypeBuilder.add(ATT_SMLREF,      String.class, 1, 1, true, null);
            featureTypeBuilder.add(inList, ATT_INPUTS,      null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(outList, ATT_OUTPUTS,     null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_PRODUCER,    Map.class, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_COMPONENTS,  Map.class, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.setDefaultGeometry(ATT_LOCATION.tip().toString());
            final FeatureType typeSystem = featureTypeBuilder.buildFeatureType();

            // Feature type sml:Component
            featureTypeBuilder.reset();
            featureTypeBuilder.setName(SML_TN_COMPONENT);
            featureTypeBuilder.add(ATT_DESC,        String.class, 0, 1, true, null);
            featureTypeBuilder.add(ATT_NAME,        String.class, 1, 1, false, null);
            featureTypeBuilder.add(kwList, ATT_KEYWORDS,    null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_LOCATION,    Point.class, 1, 1, false, null);
            featureTypeBuilder.add(ATT_PHENOMENONS, List.class, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_SMLTYPE,     String.class, 1, 1, true, null);
            featureTypeBuilder.add(ATT_SMLREF,      String.class, 1, 1, true, null);
            featureTypeBuilder.add(inList, ATT_INPUTS,      null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(outList, ATT_OUTPUTS,     null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.setDefaultGeometry(ATT_LOCATION.tip().toString());
            final FeatureType typeComponent = featureTypeBuilder.buildFeatureType();

            // Feature type sml:ProcessChain
            featureTypeBuilder.reset();
            featureTypeBuilder.setName(SML_TN_PROCESSCHAIN);
            featureTypeBuilder.add(ATT_DESC,        String.class, 0, 1, true, null);
            featureTypeBuilder.add(ATT_NAME,        String.class, 1, 1, false, null);
            featureTypeBuilder.add(kwList, ATT_KEYWORDS,    null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_LOCATION,    Point.class, 1, 1, false, null);
            featureTypeBuilder.add(ATT_PHENOMENONS, List.class, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_SMLTYPE,     String.class, 1, 1, true, null);
            featureTypeBuilder.add(ATT_SMLREF,      String.class, 1, 1, true, null);
            featureTypeBuilder.add(inList, ATT_INPUTS,      null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(outList, ATT_OUTPUTS,     null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_PRODUCER,    Map.class, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_COMPONENTS,  Map.class, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.setDefaultGeometry(ATT_LOCATION.tip().toString());
            final FeatureType typeProcessChain = featureTypeBuilder.buildFeatureType();

            // Feature type sml:ProcessModel
            featureTypeBuilder.reset();
            featureTypeBuilder.setName(SML_TN_PROCESSMODEL);
            featureTypeBuilder.add(ATT_DESC,        String.class, 0, 1, true, null);
            featureTypeBuilder.add(ATT_NAME,        String.class, 1, 1, false, null);
            featureTypeBuilder.add(kwList, ATT_KEYWORDS,    null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_LOCATION,    Point.class, 1, 1, false, null);
            featureTypeBuilder.add(ATT_PHENOMENONS, List.class, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_SMLTYPE,     String.class, 1, 1, true, null);
            featureTypeBuilder.add(ATT_SMLREF,      String.class, 1, 1, true, null);
            featureTypeBuilder.add(inList, ATT_INPUTS,      null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(outList, ATT_OUTPUTS,     null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_METHOD,      String.class, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.setDefaultGeometry(ATT_LOCATION.tip().toString());
            final FeatureType typeProcess = featureTypeBuilder.buildFeatureType();

            // Feature type sml:DataSourceType
            featureTypeBuilder.reset();
            featureTypeBuilder.setName(SML_TN_DATASOURCETYPE);
            featureTypeBuilder.add(ATT_DESC,        String.class, 0, 1, true, null);
            featureTypeBuilder.add(ATT_NAME,        String.class, 1, 1, false, null);
            featureTypeBuilder.add(kwList, ATT_KEYWORDS,    null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_LOCATION,    Point.class, 1, 1, false, null);
            featureTypeBuilder.add(ATT_PHENOMENONS, List.class, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_SMLTYPE,     String.class, 1, 1, true, null);
            featureTypeBuilder.add(ATT_SMLREF,      String.class, 1, 1, true, null);
            featureTypeBuilder.add(inList, ATT_INPUTS,      null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(outList, ATT_OUTPUTS,     null, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.add(ATT_CHARACTERISTICS,Map.class, 0, Integer.MAX_VALUE, true, null);
            featureTypeBuilder.setDefaultGeometry(ATT_LOCATION.tip().toString());
            final FeatureType typeDataSource = featureTypeBuilder.buildFeatureType();

            names.add(typeSystem.getName());
            names.add(typeComponent.getName());
            names.add(typeProcessChain.getName());
            names.add(typeDataSource.getName());
            names.add(typeProcess.getName());


            //expected results -------------------------------------------------
            int size; GeneralEnvelope env;

            //System
            size = 1;
            env = new GeneralEnvelope(CRS.decode("EPSG:27582"));
            env.setRange(0, 65400, 65400);
            env.setRange(1, 1731368, 1731368);
            expecteds.add(new ExpectedResult(typeSystem.getName(),typeSystem, size, env));

            //Component
            //unvalide test, a feature has a null geometry even while the type define
            //todo must be fixed somehow later
            //a non-null property for it.
//            size = 1;
//            env = new GeneralEnvelope(CRS.decode("EPSG:27582"));
//            env.setRange(0, 65400, 65400);
//            env.setRange(1, 1731368, 1731368);
//            expecteds.add(new ExpectedResult(typeComponent.getName(),typeComponent, size, env));

            //ProcessChain
            size = 0;
            expecteds.add(new ExpectedResult(typeProcessChain.getName(),typeProcessChain, size, null));

            //ProcessModel
            size = 0;
            expecteds.add(new ExpectedResult(typeProcess.getName(),typeProcess, size, null));

            //DataSource
            size = 0;
            expecteds.add(new ExpectedResult(typeDataSource.getName(),typeDataSource, size, null));

        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    protected FeatureStore getDataStore() {
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
