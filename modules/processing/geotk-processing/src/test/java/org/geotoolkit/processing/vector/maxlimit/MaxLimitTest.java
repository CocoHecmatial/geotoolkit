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
package org.geotoolkit.processing.vector.maxlimit;

import org.geotoolkit.process.ProcessException;
import org.opengis.util.NoSuchIdentifierException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;


import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.FeatureBuilder;
import org.apache.sis.referencing.CRS;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.processing.vector.AbstractProcessTest;


import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;


import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit test of MaxLimit process
 *
 * @author Quentin Boileau @module pending
 */
public class MaxLimitTest extends AbstractProcessTest {

    private static FeatureBuilder sfb;
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static FeatureType type;

    public MaxLimitTest() {
        super("maxlimit");
    }

    @Test
    public void testLimit() throws ProcessException, NoSuchIdentifierException, FactoryException {

        // Inputs
        final FeatureCollection featureList = buildFeatureList();

        // Process
        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("vector", "maxlimit");

        ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("feature_in").setValue(featureList);
        in.parameter("max_in").setValue(5);
        org.geotoolkit.process.Process proc = desc.createProcess(in);

        //Features out
        final FeatureCollection featureListOut = (FeatureCollection) proc.call().parameter("feature_out").getValue();

        assertEquals(5, featureListOut.size());
    }

    private static FeatureType createSimpleResultType() throws NoSuchAuthorityCodeException, FactoryException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("MaxTest");
        ftb.add("name", String.class);
        ftb.add("geom1", Geometry.class, CRS.forCode("EPSG:3395"));

        ftb.setDefaultGeometry("geom1");
        final FeatureType sft = ftb.buildFeatureType();
        return sft;
    }

    private static FeatureCollection buildFeatureList() throws FactoryException {

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
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-01 U id-11");
        featureList.add(myFeature);

        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(3, 5),
                    new Coordinate(3, 7),
                    new Coordinate(4, 7),
                    new Coordinate(4, 5),
                    new Coordinate(3, 5)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature1");
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
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-03 U id-12");
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
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-03 U id-11");
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
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-03 U id-13");
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
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-02 U id-12");
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
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-02 U id-11");
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
        sfb.setPropertyValue("geom1", geometryFactory.createPolygon(ring, null));
        myFeature = sfb.buildFeature("id-04");
        featureList.add(myFeature);


        ring = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(7, 7),
                    new Coordinate(7, 8),
                    new Coordinate(9, 8),
                    new Coordinate(9, 4),
                    new Coordinate(8, 4),
                    new Coordinate(8, 5),
                    new Coordinate(8, 7),
                    new Coordinate(7, 7)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature12");
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
                    new Coordinate(4, 7),
                    new Coordinate(4, 8),
                    new Coordinate(7, 8),
                    new Coordinate(7, 7),
                    new Coordinate(6, 7),
                    new Coordinate(4, 7)
                });
        Polygon poly1 = geometryFactory.createPolygon(ring, null);
        Polygon poly2 = geometryFactory.createPolygon(ring2, null);
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "feature11");
        sfb.setPropertyValue("geom1", geometryFactory.createMultiPolygon(new Polygon[]{poly1, poly2}));
        myFeature = sfb.buildFeature("id-11");
        featureList.add(myFeature);

        return featureList;
    }
}
