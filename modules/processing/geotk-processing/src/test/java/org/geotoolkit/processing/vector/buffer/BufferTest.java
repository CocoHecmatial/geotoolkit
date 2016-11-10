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
package org.geotoolkit.processing.vector.buffer;

import org.geotoolkit.process.ProcessException;
import org.opengis.util.NoSuchIdentifierException;
import java.util.ArrayList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import javax.measure.quantity.Length;
import javax.measure.Unit;
import org.apache.sis.measure.Units;
import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.FeatureBuilder;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.processing.vector.AbstractProcessTest;
import org.apache.sis.referencing.CRS;
import org.geotoolkit.feature.Feature;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;


import org.junit.Test;
import org.geotoolkit.feature.Property;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.GeometryDescriptor;
import static org.junit.Assert.*;

/**
 * JUnit test douglas peucker simplification on FeatureCollection
 *
 * @author Quentin Boileau @module pending
 */
public class BufferTest extends AbstractProcessTest {

    private static FeatureBuilder sfb;
    private static GeometryFactory geometryFactory;
    private static FeatureType type;
    private static final Double distance = new Double(5);

    public BufferTest() {
        super("buffer");
    }

    /**
     * Test Buffer process Tests realized : - Same FeatureType between the output FeatureCollection and a generated
     * FeatureCollection - Same Features ID - Same FeatureCollection size - Output FeatureCollection geometry contains
     * input FeatureCollection geometry
     */
    @Test
    public void testBuffer() throws ProcessException, NoSuchIdentifierException, FactoryException {

        // Inputs
        final FeatureCollection featureList = buildFeatureCollectionInput1();
        Unit<Length> unit = Units.METRE;

        // Process
        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("vector", "buffer");

        ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter("feature_in").setValue(featureList);
        in.parameter("distance_in").setValue(distance);
        //in.parameter("unit_in").setValue(unit);
        in.parameter("lenient_transform_in").setValue(true);
        org.geotoolkit.process.Process proc = desc.createProcess(in);


        //Features out
        final FeatureCollection featureListOut = (FeatureCollection) proc.call().parameter("feature_out").getValue();

        //Expected Features out
        final FeatureCollection featureListResult = buildFeatureCollectionResult();

        assertEquals(featureListResult.getFeatureType(), featureListOut.getFeatureType());
        assertEquals(featureListResult.getID(), featureListOut.getID());
        assertEquals(featureListResult.size(), featureListOut.size());

        double precision = 0.01;
        //geometry out list
        FeatureIterator iteratorOut = featureListOut.iterator();
        ArrayList<Geometry> geomsOut = new ArrayList<Geometry>();
        int itOut = 0;
        while (iteratorOut.hasNext()) {
            Feature featureOut = iteratorOut.next();

            for (Property propertyOut : featureOut.getProperties()) {
                if (propertyOut.getDescriptor() instanceof GeometryDescriptor) {
                    geomsOut.add(itOut++, (Geometry) propertyOut.getValue());
                }
            }
        }
        //geometry input list
        FeatureIterator listIterator = featureList.iterator();
        ArrayList<Geometry> geomsInput = new ArrayList<Geometry>();
        int itResult = 0;
        while (listIterator.hasNext()) {
            Feature feature = listIterator.next();

            for (Property property : feature.getProperties()) {
                if (property.getDescriptor() instanceof GeometryDescriptor) {
                    geomsInput.add(itResult++, (Geometry) property.getValue());
                }
            }
        }

        assertEquals(geomsInput.size(), geomsOut.size());
        for (int i = 0; i < geomsInput.size(); i++) {
            Geometry gOut = geomsOut.get(i);
            Geometry gInput = geomsInput.get(i);

            assertTrue(gOut.contains(gInput));
        }
    }

    private static FeatureType createSimpleType() throws NoSuchAuthorityCodeException, FactoryException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("BufferTest");
        ftb.add("name", String.class);
        ftb.add("position", Geometry.class, CRS.forCode("EPSG:3395"));

        ftb.setDefaultGeometry("position");
        final FeatureType sft = ftb.buildFeatureType();
        return sft;
    }

    private static FeatureCollection buildFeatureCollectionInput1() throws FactoryException {
        type = createSimpleType();

        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);

        geometryFactory = new GeometryFactory();

        Feature myFeature1;

        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "Point");
        sfb.setPropertyValue("position", geometryFactory.createPoint(new Coordinate(-10.0, 10.0)));
        myFeature1 = sfb.buildFeature("id-01");
        featureList.add(myFeature1);


        Feature myFeature2;
        LineString line = geometryFactory.createLineString(
                new Coordinate[]{
                    new Coordinate(30.0, 40.0),
                    new Coordinate(50.0, 60.0),
                    new Coordinate(60.0, 50.0),
                    new Coordinate(70.0, 40.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "LineString");
        sfb.setPropertyValue("position", line);
        myFeature2 = sfb.buildFeature("id-02");
        featureList.add(myFeature2);


        Feature myFeature3;
        LinearRing ring2 = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(-10.0, -10.0),
                    new Coordinate(0.0, -30.0),
                    new Coordinate(-20.0, -20.0),
                    new Coordinate(-30.0, 10.0),
                    new Coordinate(-20.0, 30.0),
                    new Coordinate(0.0, 20.0),
                    new Coordinate(10.0, 10.0),
                    new Coordinate(20.0, -20.0),
                    new Coordinate(10.0, -20.0),
                    new Coordinate(-10.0, -10.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "Polygone");
        sfb.setPropertyValue("position", geometryFactory.createPolygon(ring2, null));
        myFeature3 = sfb.buildFeature("id-03");
        featureList.add(myFeature3);

        return featureList;
    }

    private static FeatureCollection buildFeatureCollectionResult() throws FactoryException {
        type = createSimpleType();
        final FeatureCollection featureList = FeatureStoreUtilities.collection("", type);

        geometryFactory = new GeometryFactory();

        Feature myFeature1;

        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "Point");
        sfb.setPropertyValue("position", geometryFactory.createPoint(new Coordinate(-10.0, 10.0)).buffer(distance));
        myFeature1 = sfb.buildFeature("id-01");
        featureList.add(myFeature1);


        Feature myFeature2;
        LineString line = geometryFactory.createLineString(
                new Coordinate[]{
                    new Coordinate(30.0, 40.0),
                    new Coordinate(50.0, 60.0),
                    new Coordinate(60.0, 50.0),
                    new Coordinate(70.0, 40.0)
                });
        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "LineString");
        sfb.setPropertyValue("position", line.buffer(distance));
        myFeature2 = sfb.buildFeature("id-02");
        featureList.add(myFeature2);


        Feature myFeature3;
        LinearRing ring2 = geometryFactory.createLinearRing(
                new Coordinate[]{
                    new Coordinate(-10.0, -10.0),
                    new Coordinate(0.0, -30.0),
                    new Coordinate(-20.0, -20.0),
                    new Coordinate(-30.0, 10.0),
                    new Coordinate(-20.0, 30.0),
                    new Coordinate(0.0, 20.0),
                    new Coordinate(10.0, 10.0),
                    new Coordinate(20.0, -20.0),
                    new Coordinate(10.0, -20.0),
                    new Coordinate(-10.0, -10.0)
                });

        sfb = new FeatureBuilder(type);
        sfb.setPropertyValue("name", "Polygone");
        sfb.setPropertyValue("position", geometryFactory.createPolygon(ring2, null).buffer(distance));
        myFeature3 = sfb.buildFeature("id-03");
        featureList.add(myFeature3);

        return featureList;

    }
}
