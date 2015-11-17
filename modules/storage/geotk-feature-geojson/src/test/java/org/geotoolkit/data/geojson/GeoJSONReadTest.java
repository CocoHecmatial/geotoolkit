package org.geotoolkit.data.geojson;

import org.geotoolkit.util.NamesExt;
import org.opengis.util.GenericName;
import com.vividsolutions.jts.geom.*;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.*;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.*;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.apache.sis.referencing.CommonCRS;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;

import java.net.URL;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.geotoolkit.data.geojson.GeoJSONFeatureStoreFactory.*;

/**
 * @author Quentin Boileau (Geomatys)
 */
public class GeoJSONReadTest {

    @Test
    public void readPointTest() throws DataStoreException {
        URL pointFile = GeoJSONReadTest.class.getResource("/org/geotoolkit/geojson/point.json");

        ParameterValueGroup param = PARAMETERS_DESCRIPTOR.createValue();
        param.parameter(URLP.getName().getCode()).setValue(pointFile);
        FeatureStore store = FeatureStoreFinder.open(param);
        assertNotNull(store);

        assertEquals(1, store.getNames().size());
        GenericName name = store.getNames().iterator().next();
        assertEquals(NamesExt.create("point"), name);

        FeatureType ft = store.getFeatureType(name);
        testFeatureTypes(buildGeometryFeatureType("point", Point.class), ft);

        Session session = store.createSession(false);
        FeatureCollection fcoll = session.getFeatureCollection(QueryBuilder.all(name));
        assertEquals(1, fcoll.size());
    }

    @Test
    public void readMultiPointTest() throws DataStoreException {
        URL pointFile = GeoJSONReadTest.class.getResource("/org/geotoolkit/geojson/multipoint.json");

        ParameterValueGroup param = PARAMETERS_DESCRIPTOR.createValue();
        param.parameter(URLP.getName().getCode()).setValue(pointFile);
        FeatureStore store = FeatureStoreFinder.open(param);
        assertNotNull(store);

        assertEquals(1, store.getNames().size());
        GenericName name = store.getNames().iterator().next();
        assertEquals(NamesExt.create("multipoint"), name);

        FeatureType ft = store.getFeatureType(name);
        testFeatureTypes(buildGeometryFeatureType("multipoint", MultiPoint.class), ft);

        Session session = store.createSession(false);
        FeatureCollection fcoll = session.getFeatureCollection(QueryBuilder.all(name));
        assertEquals(1, fcoll.size());
    }

    @Test
    public void readLineStringTest() throws DataStoreException {
        URL pointFile = GeoJSONReadTest.class.getResource("/org/geotoolkit/geojson/linestring.json");

        ParameterValueGroup param = PARAMETERS_DESCRIPTOR.createValue();
        param.parameter(URLP.getName().getCode()).setValue(pointFile);
        FeatureStore store = FeatureStoreFinder.open(param);
        assertNotNull(store);

        assertEquals(1, store.getNames().size());
        GenericName name = store.getNames().iterator().next();
        assertEquals(NamesExt.create("linestring"), name);

        FeatureType ft = store.getFeatureType(name);
        testFeatureTypes(buildGeometryFeatureType("linestring", LineString.class), ft);

        Session session = store.createSession(false);
        FeatureCollection fcoll = session.getFeatureCollection(QueryBuilder.all(name));
        assertEquals(1, fcoll.size());
    }

    @Test
    public void readMultiLineStringTest() throws DataStoreException {
        URL pointFile = GeoJSONReadTest.class.getResource("/org/geotoolkit/geojson/multilinestring.json");

        ParameterValueGroup param = PARAMETERS_DESCRIPTOR.createValue();
        param.parameter(URLP.getName().getCode()).setValue(pointFile);
        FeatureStore store = FeatureStoreFinder.open(param);
        assertNotNull(store);

        assertEquals(1, store.getNames().size());
        GenericName name = store.getNames().iterator().next();
        assertEquals(NamesExt.create("multilinestring"), name);

        FeatureType ft = store.getFeatureType(name);
        testFeatureTypes(buildGeometryFeatureType("multilinestring", MultiLineString.class), ft);

        Session session = store.createSession(false);
        FeatureCollection fcoll = session.getFeatureCollection(QueryBuilder.all(name));
        assertEquals(1, fcoll.size());
    }

    @Test
    public void readPolygonTest() throws DataStoreException {
        URL pointFile = GeoJSONReadTest.class.getResource("/org/geotoolkit/geojson/polygon.json");

        ParameterValueGroup param = PARAMETERS_DESCRIPTOR.createValue();
        param.parameter(URLP.getName().getCode()).setValue(pointFile);
        FeatureStore store = FeatureStoreFinder.open(param);
        assertNotNull(store);

        assertEquals(1, store.getNames().size());
        GenericName name = store.getNames().iterator().next();
        assertEquals(NamesExt.create("polygon"), name);

        FeatureType ft = store.getFeatureType(name);
        testFeatureTypes(buildGeometryFeatureType("polygon", Polygon.class), ft);

        Session session = store.createSession(false);
        FeatureCollection fcoll = session.getFeatureCollection(QueryBuilder.all(name));
        assertEquals(1, fcoll.size());
    }

    @Test
    public void readMultiPolygonTest() throws DataStoreException {
        URL pointFile = GeoJSONReadTest.class.getResource("/org/geotoolkit/geojson/multipolygon.json");

        ParameterValueGroup param = PARAMETERS_DESCRIPTOR.createValue();
        param.parameter(URLP.getName().getCode()).setValue(pointFile);
        FeatureStore store = FeatureStoreFinder.open(param);
        assertNotNull(store);

        assertEquals(1, store.getNames().size());
        GenericName name = store.getNames().iterator().next();
        assertEquals(NamesExt.create("multipolygon"), name);

        FeatureType ft = store.getFeatureType(name);
        testFeatureTypes(buildGeometryFeatureType("multipolygon", MultiPolygon.class), ft);

        Session session = store.createSession(false);
        FeatureCollection fcoll = session.getFeatureCollection(QueryBuilder.all(name));
        assertEquals(1, fcoll.size());
    }

    @Test
    public void readGeometryCollectionTest() throws DataStoreException {
        URL pointFile = GeoJSONReadTest.class.getResource("/org/geotoolkit/geojson/geometrycollection.json");

        ParameterValueGroup param = PARAMETERS_DESCRIPTOR.createValue();
        param.parameter(URLP.getName().getCode()).setValue(pointFile);
        FeatureStore store = FeatureStoreFinder.open(param);
        assertNotNull(store);

        assertEquals(1, store.getNames().size());
        GenericName name = store.getNames().iterator().next();
        assertEquals(NamesExt.create("geometrycollection"), name);

        FeatureType ft = store.getFeatureType(name);
        testFeatureTypes(buildGeometryFeatureType("geometrycollection", GeometryCollection.class), ft);

        Session session = store.createSession(false);
        FeatureCollection fcoll = session.getFeatureCollection(QueryBuilder.all(name));
        assertEquals(1, fcoll.size());

    }

    @Test
    public void readFeatureTest() throws DataStoreException {
        URL pointFile = GeoJSONReadTest.class.getResource("/org/geotoolkit/geojson/feature.json");

        ParameterValueGroup param = PARAMETERS_DESCRIPTOR.createValue();
        param.parameter(URLP.getName().getCode()).setValue(pointFile);
        FeatureStore store = FeatureStoreFinder.open(param);
        assertNotNull(store);

        assertEquals(1, store.getNames().size());
        GenericName name = store.getNames().iterator().next();
        assertEquals(NamesExt.create("feature"), name);

        FeatureType ft = store.getFeatureType(name);
        testFeatureTypes(buildSimpleFeatureType("feature"), ft);

        Session session = store.createSession(false);
        FeatureCollection fcoll = session.getFeatureCollection(QueryBuilder.all(name));
        assertEquals(1, fcoll.size());
    }

    @Test
    public void readFeatureCollectionTest() throws DataStoreException {
        URL pointFile = GeoJSONReadTest.class.getResource("/org/geotoolkit/geojson/featurecollection.json");

        ParameterValueGroup param = PARAMETERS_DESCRIPTOR.createValue();
        param.parameter(URLP.getName().getCode()).setValue(pointFile);
        FeatureStore store = FeatureStoreFinder.open(param);
        assertNotNull(store);

        assertEquals(1, store.getNames().size());
        GenericName name = store.getNames().iterator().next();
        assertEquals(NamesExt.create("featurecollection"), name);

        FeatureType ft = store.getFeatureType(name);
        testFeatureTypes(buildFCFeatureType("featurecollection"), ft);

        Session session = store.createSession(false);
        FeatureCollection fcoll = session.getFeatureCollection(QueryBuilder.all(name));
        assertEquals(7, fcoll.size());
    }

    /**
     * Test reading of Features with array as properties value
     * @throws DataStoreException
     */
    @Test
    public void readPropertyArrayTest() throws DataStoreException {
        URL arrayFile = GeoJSONReadTest.class.getResource("/org/geotoolkit/geojson/f_prop_array.json");

        ParameterValueGroup param = PARAMETERS_DESCRIPTOR.createValue();
        param.parameter(URLP.getName().getCode()).setValue(arrayFile);
        FeatureStore store = FeatureStoreFinder.open(param);
        assertNotNull(store);

        assertEquals(1, store.getNames().size());
        GenericName name = store.getNames().iterator().next();
        assertEquals(NamesExt.create("f_prop_array"), name);

        FeatureType ft = store.getFeatureType(name);
        testFeatureTypes(buildPropertyArrayFeatureType("f_prop_array", Geometry.class), ft);

        Session session = store.createSession(false);
        FeatureCollection fcoll = session.getFeatureCollection(QueryBuilder.all(name));
        assertEquals(2, fcoll.size());

        Double[][] array1 = new Double[5][5];
        Double[][] array2 = new Double[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                array1[i][j] = (double) (i + j);
                array2[i][j] = (double) (i - j);
            }
        }

        FeatureIterator ite = fcoll.iterator();
        Feature feat1 = ite.next();
        assertArrayEquals(array1, (Double[][]) feat1.getProperty("array").getValue());

        Feature feat2 = ite.next();
        assertArrayEquals(array2, (Double[][]) feat2.getProperty("array").getValue());

    }

    /**
     * This test ensure that properties fields with null value doesn't rise NullPointerException
     * @throws DataStoreException
     */
    @Test
    public void readNullPropsTest() throws DataStoreException {
        URL jsonFile = GeoJSONReadTest.class.getResource("/org/geotoolkit/geojson/sample_with_null_properties.json");

        ParameterValueGroup param = PARAMETERS_DESCRIPTOR.createValue();
        param.parameter(URLP.getName().getCode()).setValue(jsonFile);
        FeatureStore store = FeatureStoreFinder.open(param);
        assertNotNull(store);

        assertEquals(1, store.getNames().size());
        GenericName name = store.getNames().iterator().next();

        FeatureType ft = store.getFeatureType(name);
        Session session = store.createSession(false);
        FeatureCollection fcoll = session.getFeatureCollection(QueryBuilder.all(name));
        assertEquals(15, fcoll.size());
    }

    private FeatureType buildPropertyArrayFeatureType(String name, Class geomClass) {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName(name);
        ftb.add("array", Double[][].class);
        ftb.add(BasicFeatureTypes.GEOMETRY_ATTRIBUTE_NAME, geomClass, CommonCRS.WGS84.normalizedGeographic());
        return ftb.buildSimpleFeatureType();
    }

    private FeatureType buildGeometryFeatureType(String name, Class geomClass) {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName(name);
        ftb.add(BasicFeatureTypes.GEOMETRY_ATTRIBUTE_NAME, geomClass, CommonCRS.WGS84.normalizedGeographic());
        return ftb.buildSimpleFeatureType();
    }

    private FeatureType buildSimpleFeatureType(String name) {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName(name);
        ftb.add(BasicFeatureTypes.GEOMETRY_ATTRIBUTE_NAME, Polygon.class, CommonCRS.WGS84.normalizedGeographic());
        ftb.add("name", String.class);
        return ftb.buildSimpleFeatureType();
    }

    private FeatureType buildFCFeatureType(String name) {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName(name);
        ftb.add(BasicFeatureTypes.GEOMETRY_ATTRIBUTE_NAME, Geometry.class, CommonCRS.WGS84.normalizedGeographic());
        ftb.add("name", String.class);
        ftb.add("address", String.class);
        return ftb.buildSimpleFeatureType();
    }

    private void testFeatureTypes(FeatureType expected, FeatureType result) {
        for(PropertyDescriptor desc : expected.getDescriptors()){
            PropertyDescriptor td = result.getDescriptor(desc.getName().tip().toString());
            assertNotNull(td);
            assertEquals(td.getType().getBinding(), desc.getType().getBinding());
        }
    }
}
