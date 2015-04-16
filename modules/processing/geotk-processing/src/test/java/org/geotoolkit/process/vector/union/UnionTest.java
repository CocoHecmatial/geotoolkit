/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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
package org.geotoolkit.process.vector.union;

import org.geotoolkit.process.ProcessException;
import org.opengis.util.NoSuchIdentifierException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.FeatureBuilder;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.process.vector.AbstractProcessTest;
import org.geotoolkit.referencing.CRS;

import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit test of Union process
 *
 * @author Quentin Boileau @module pending
 */
public class UnionTest extends AbstractProcessTest {

    private static FeatureBuilder sfb;
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static FeatureType type;

    public UnionTest() {
        super("union");
    }

    @Test
    public void testIntersection() throws ProcessException, NoSuchIdentifierException, FactoryException {

        // Inputs
        final FeatureCollection featureList = buildFeatureList();
        final FeatureCollection featureUnionList = buildFeatureUnionList();

        // Process
        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("vector", "union");

        ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("feature_in").setValue(featureList);
        in.parameter("feature_union").setValue(featureUnionList);
        in.parameter("input_geometry_name").setValue("geom1");
        org.geotoolkit.process.Process proc = desc.createProcess(in);

        //Features out
        final FeatureCollection featureListOut = (FeatureCollection) proc.call().parameter("feature_out").getValue();

        //Expected Features out
        final FeatureCollection featureListResult = buildResultList();
        
        assertEquals(featureListOut.getFeatureType(), featureListResult.getFeatureType());
        assertEquals(featureListOut.getID(), featureListResult.getID());
        assertEquals(featureListOut.size(), featureListResult.size());
        assertTrue(featureListOut.containsAll(featureListResult));
    }

    private static FeatureType createSimpleType() throws NoSuchAuthorityCodeException, FactoryException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("UnionTest");
        ftb.add("name", String.class);
        ftb.add("geom1", Geometry.class, CRS.decode("EPSG:3395"));
        ftb.add("geom2", Geometry.class, CRS.decode("EPSG:3395"));

        ftb.setDefaultGeometry("geom1");
        final FeatureType sft = ftb.buildFeatureType();
        return sft;
    }

    private static FeatureType createSimpleType2() throws NoSuchAuthorityCodeException, FactoryException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("UnionTest");
        ftb.add("name", String.class);
        ftb.add("color", String.class);
        ftb.add("geom3", Geometry.class, CRS.decode("EPSG:3395"));
        ftb.add("att", Integer.class);

        ftb.setDefaultGeometry("geom3");
        final FeatureType sft = ftb.buildFeatureType();
        return sft;
    }

    private static FeatureType createSimpleResultType() throws NoSuchAuthorityCodeException, FactoryException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("UnionTest-UnionTest");
        ftb.add("name", String.class);
        ftb.add("color", String.class);
        ftb.add("att", Integer.class);
        ftb.add("geom1", Geometry.class, CRS.decode("EPSG:3395"));

        ftb.setDefaultGeometry("geom1");
        final FeatureType sft = ftb.buildFeatureType();
        return sft;
    }

    private static FeatureCollection buildFeatureList() throws FactoryException {

        type = createSimpleType();
        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);


        Feature myFeature1;
        LinearRing ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(3.0, 5.0),
                    new Coordinate(3.0, 7.0),
                    new Coordinate(6.0, 7.0),
                    new Coordinate(6.0, 5.0),
                    new Coordinate(3.0, 5.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature1");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature1 = sfb.buildFeature("id-01");
        featureList.add(myFeature1);

        Feature myFeature2;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(6.0, 5.0),
                    new Coordinate(6.0, 7.0),
                    new Coordinate(8.0, 7.0),
                    new Coordinate(8.0, 5.0),
                    new Coordinate(6.0, 5.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature2");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature2 = sfb.buildFeature("id-02");
        featureList.add(myFeature2);

        Feature myFeature3;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(6.0, 2.0),
                    new Coordinate(6.0, 5.0),
                    new Coordinate(8.0, 5.0),
                    new Coordinate(8.0, 2.0),
                    new Coordinate(6.0, 2.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature3");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        //sfb.set("geom2", line);
        myFeature3 = sfb.buildFeature("id-03");
        featureList.add(myFeature3);

        Feature myFeature4;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(2.0, 3.0),
                    new Coordinate(2.0, 4.0),
                    new Coordinate(3.0, 4.0),
                    new Coordinate(3.0, 3.0),
                    new Coordinate(2.0, 3.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature4");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature4 = sfb.buildFeature("id-04");
        featureList.add(myFeature4);

        return featureList;
    }

    private static FeatureCollection buildFeatureUnionList() throws FactoryException {

        type = createSimpleType2();

        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);


        Feature myFeature1;
        LinearRing ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(4.0, 4.0),
                    new Coordinate(4.0, 8.0),
                    new Coordinate(7.0, 8.0),
                    new Coordinate(7.0, 4.0),
                    new Coordinate(4.0, 4.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature11");
        sfb.setPropertyValue("color", "red");
        sfb.setPropertyValue("geom3", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("att", 20);
        myFeature1 = sfb.buildFeature("id-11");
        featureList.add(myFeature1);

        Feature myFeature2;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(7.0, 4.0),
                    new Coordinate(7.0, 8.0),
                    new Coordinate(9.0, 8.0),
                    new Coordinate(9.0, 4.0),
                    new Coordinate(7.0, 4.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature12");
        sfb.setPropertyValue("color", "blue");
        sfb.setPropertyValue("geom3", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("att", 20);
        myFeature2 = sfb.buildFeature("id-12");
        featureList.add(myFeature2);

        Feature myFeature3;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(6.0, 2.0),
                    new Coordinate(6.0, 4.0),
                    new Coordinate(9.0, 4.0),
                    new Coordinate(9.0, 2.0),
                    new Coordinate(6.0, 2.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature13");
        sfb.setPropertyValue("color", "grey");
        sfb.setPropertyValue("geom3", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("att", 10);
        myFeature3 = sfb.buildFeature("id-13");
        featureList.add(myFeature3);

        Feature myFeature4;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(4.0, 2.0),
                    new Coordinate(4.0, 3.0),
                    new Coordinate(5.0, 3.0),
                    new Coordinate(5.0, 2.0),
                    new Coordinate(4.0, 2.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature14");
        sfb.setPropertyValue("color", "grey");
        sfb.setPropertyValue("geom3", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("att", 12);
        myFeature4 = sfb.buildFeature("id-14");
        featureList.add(myFeature4);

        Feature myFeature5;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(2.0, 5.0),
                    new Coordinate(2.0, 6.0),
                    new Coordinate(3.0, 6.0),
                    new Coordinate(3.0, 5.0),
                    new Coordinate(2.0, 5.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature15");
        sfb.setPropertyValue("color", "grey");
        sfb.setPropertyValue("geom3", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("att", 12);
        myFeature5 = sfb.buildFeature("id-15");
        featureList.add(myFeature5);

        return featureList;
    }

    private static FeatureCollection buildResultList() throws FactoryException {

        type = createSimpleResultType();

        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);


        Feature myFeature;
        LinearRing ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(4, 7),
                    new Coordinate(6, 7),
                    new Coordinate(6, 5),
                    new Coordinate(4, 5),
                    new Coordinate(4, 7)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature1");
        sfb.setPropertyValue("color", "red");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("att", 20);
        myFeature = sfb.buildFeature("id-01-id-11");
        featureList.add(myFeature);

        LineString str = geometryFactory.createLineString(
                new Coordinate[]{
                    new Coordinate(3, 5),
                    new Coordinate(3, 6)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature1");
        sfb.setPropertyValue("color", "grey");
        sfb.setPropertyValue("geom1", str);
        sfb.setPropertyValue("att", 12);
        myFeature = sfb.buildFeature("id-01-id-15");
        featureList.add(myFeature);

        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(3, 5),
                    new Coordinate(3, 6),
                    new Coordinate(3, 7),
                    new Coordinate(4, 7),
                    new Coordinate(4, 5),
                    new Coordinate(3, 5)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature1");
        sfb.setPropertyValue("color", null);
        sfb.setPropertyValue("att", null);
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-01");
        featureList.add(myFeature);

        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(7, 5),
                    new Coordinate(8, 5),
                    new Coordinate(8, 4),
                    new Coordinate(7, 4),
                    new Coordinate(7, 5)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature3");
        sfb.setPropertyValue("color", "blue");
        sfb.setPropertyValue("att", 20);
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-03-id-12");
        featureList.add(myFeature);

        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(6, 4),
                    new Coordinate(6, 5),
                    new Coordinate(7, 5),
                    new Coordinate(7, 4),
                    new Coordinate(6, 4)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature3");
        sfb.setPropertyValue("color", "red");
        sfb.setPropertyValue("att", 20);
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-03-id-11");
        featureList.add(myFeature);


        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(6, 2),
                    new Coordinate(6, 4),
                    new Coordinate(8, 4),
                    new Coordinate(8, 2),
                    new Coordinate(6, 2)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature3");
        sfb.setPropertyValue("color", "grey");
        sfb.setPropertyValue("att", 10);
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-03-id-13");
        featureList.add(myFeature);

        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(7, 7),
                    new Coordinate(8, 7),
                    new Coordinate(8, 5),
                    new Coordinate(7, 5),
                    new Coordinate(7, 7)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature2");
        sfb.setPropertyValue("color", "blue");
        sfb.setPropertyValue("att", 20);
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-02-id-12");
        featureList.add(myFeature);

        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(6, 5),
                    new Coordinate(6, 7),
                    new Coordinate(7, 7),
                    new Coordinate(7, 5),
                    new Coordinate(6, 5)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature2");
        sfb.setPropertyValue("color", "red");
        sfb.setPropertyValue("att", 20);
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-02-id-11");
        featureList.add(myFeature);

        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(2, 3),
                    new Coordinate(2, 4),
                    new Coordinate(3, 4),
                    new Coordinate(3, 3),
                    new Coordinate(2, 3)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature4");
        sfb.setPropertyValue("color", null);
        sfb.setPropertyValue("att", null);
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-04");
        featureList.add(myFeature);


        //POLYGON ((8 5, 8 7, 7 7, 7 8, 9 8, 9 4, 8 4, 8 5))
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(8, 5),
                    new Coordinate(8, 7),
                    new Coordinate(7, 7),
                    new Coordinate(7, 8),
                    new Coordinate(9, 8),
                    new Coordinate(9, 4),
                    new Coordinate(8, 4),
                    new Coordinate(8, 5)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature12");
        sfb.setPropertyValue("color", "blue");
        sfb.setPropertyValue("att", 20);
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-12");
        featureList.add(myFeature);

        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(8, 4),
                    new Coordinate(9, 4),
                    new Coordinate(9, 2),
                    new Coordinate(8, 2),
                    new Coordinate(8, 4)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature13");
        sfb.setPropertyValue("color", "grey");
        sfb.setPropertyValue("att", 10);
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-13");
        featureList.add(myFeature);

        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(4, 2),
                    new Coordinate(4, 3),
                    new Coordinate(5, 3),
                    new Coordinate(5, 2),
                    new Coordinate(4, 2)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature14");
        sfb.setPropertyValue("color", "grey");
        sfb.setPropertyValue("att", 12);
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-14");
        featureList.add(myFeature);

        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(4, 4),
                    new Coordinate(4, 5),
                    new Coordinate(6, 5),
                    new Coordinate(6, 4),
                    new Coordinate(4, 4)
                });
        LinearRing ring2 = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(6, 7),
                    new Coordinate(4, 7),
                    new Coordinate(4, 8),
                    new Coordinate(7, 8),
                    new Coordinate(7, 7),
                    new Coordinate(6, 7)
                });
        Polygon poly1 = geometryFactory.createPolygon(ring, null);
        Polygon poly2 = geometryFactory.createPolygon(ring2, null);
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature11");
        sfb.setPropertyValue("color", "red");
        sfb.setPropertyValue("att", 20);
        sfb.setPropertyValue("geom1", geometryFactory.createMultiPolygon(new Polygon[]{poly1, poly2}));
        myFeature = sfb.buildFeature("id-11");
        featureList.add(myFeature);

        Feature myFeature5;
        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(2.0, 5.0),
                    new Coordinate(2.0, 6.0),
                    new Coordinate(3.0, 6.0),
                    new Coordinate(3.0, 5.0),
                    new Coordinate(2.0, 5.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature15");
        sfb.setPropertyValue("color", "grey");
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        sfb.setPropertyValue("att", 12);
        myFeature5 = sfb.buildFeature("id-15");
        featureList.add(myFeature5);

        return featureList;
    }
}
