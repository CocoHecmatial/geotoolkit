/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2016, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.gml;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geotoolkit.gml.xml.v321.CurveSegmentArrayPropertyType;
import org.geotoolkit.gml.xml.v321.CurveType;
import org.geotoolkit.gml.xml.v321.DirectPositionListType;
import org.geotoolkit.gml.xml.v321.DirectPositionType;
import org.geotoolkit.gml.xml.v321.EnvelopeType;
import org.geotoolkit.gml.xml.v321.GeodesicStringType;
import org.geotoolkit.gml.xml.v321.LineStringType;
import org.geotoolkit.gml.xml.v321.LinearRingType;
import org.geotoolkit.gml.xml.v321.PointType;
import org.geotoolkit.gml.xml.v321.PolygonType;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class GeometrytoJTSTest extends org.geotoolkit.test.TestBase {

    static final GeometryFactory GF = new GeometryFactory();

    @Test
    public void gmlPolygonToJTSTest2D() throws Exception {
        GeometryFactory fact = GF;
        final Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(0, 0);
        coordinates[1] = new Coordinate(0, 1);
        coordinates[2] = new Coordinate(1, 1);
        coordinates[3] = new Coordinate(1, 0);
        coordinates[4] = new Coordinate(0, 0);

        LinearRing linear = GF.createLinearRing(coordinates);
        Polygon expected = new Polygon(linear, null, fact);
        expected.setSRID(2154);

        LinearRingType exterior = new LinearRingType();
        List<Double> coords = new ArrayList<>();
        coords.add(0.0); coords.add(0.0);
        coords.add(0.0); coords.add(1.0);
        coords.add(1.0); coords.add(1.0);
        coords.add(1.0); coords.add(0.0);
        coords.add(0.0); coords.add(0.0);

        exterior.setPosList(new DirectPositionListType(coords));
        PolygonType gml = new PolygonType(exterior, null);

        final Geometry result = GeometrytoJTS.toJTS((org.geotoolkit.gml.xml.Polygon)gml);

        Assert.assertEquals(expected, result);

    }

    @Test
    public void gmlPolygonToJTSTest3D() throws Exception {
        GeometryFactory fact = GF;
        final Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(0, 0, 1);
        coordinates[1] = new Coordinate(0, 1, 1);
        coordinates[2] = new Coordinate(1, 1, 1);
        coordinates[3] = new Coordinate(1, 0, 1);
        coordinates[4] = new Coordinate(0, 0, 1);

        LinearRing linear = GF.createLinearRing(coordinates);
        Polygon expected = new Polygon(linear, null, fact);
        expected.setSRID(2154);

        LinearRingType exterior = new LinearRingType();
        List<Double> coords = new ArrayList<>();
        coords.add(0.0); coords.add(0.0); coords.add(1.0);
        coords.add(0.0); coords.add(1.0); coords.add(1.0);
        coords.add(1.0); coords.add(1.0); coords.add(1.0);
        coords.add(1.0); coords.add(0.0); coords.add(1.0);
        coords.add(0.0); coords.add(0.0); coords.add(1.0);

        exterior.setPosList(new DirectPositionListType(coords));
        exterior.setSrsDimension(3);
        PolygonType gml = new PolygonType(exterior, null);


        final Geometry result = GeometrytoJTS.toJTS((org.geotoolkit.gml.xml.Polygon)gml);

        Assert.assertEquals(expected, result);

    }

    @Test
    public void gmlLineStringToJTSTest2D() throws Exception {

        final Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(0, 0);
        coordinates[1] = new Coordinate(0, 1);
        coordinates[2] = new Coordinate(1, 1);
        coordinates[3] = new Coordinate(1, 0);
        coordinates[4] = new Coordinate(0, 0);

        LineString expected = GF.createLineString(coordinates);
        expected.setSRID(2154);



        List<DirectPositionType> coords = new ArrayList<>();
        coords.add(new DirectPositionType(0.0, 0.0));
        coords.add(new DirectPositionType(0.0, 1.0));
        coords.add(new DirectPositionType(1.0, 1.0));
        coords.add(new DirectPositionType(1.0, 0.0));
        coords.add(new DirectPositionType(0.0, 0.0));
        LineStringType gml = new LineStringType("", coords);
        gml.setSrsName("EPSG:2154");

        final Geometry result = GeometrytoJTS.toJTS(gml);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void gmlLineStringToJTSTest3D() throws Exception {

        final Coordinate[] coordinates = new Coordinate[5];
        coordinates[0] = new Coordinate(0, 0, 1);
        coordinates[1] = new Coordinate(0, 1, 1);
        coordinates[2] = new Coordinate(1, 1, 1);
        coordinates[3] = new Coordinate(1, 0, 1);
        coordinates[4] = new Coordinate(0, 0, 1);

        LineString expected = GF.createLineString(coordinates);
        expected.setSRID(2154);



        List<DirectPositionType> coords = new ArrayList<>();
        coords.add(new DirectPositionType(0.0, 0.0, 1.0));
        coords.add(new DirectPositionType(0.0, 1.0, 1.0));
        coords.add(new DirectPositionType(1.0, 1.0, 1.0));
        coords.add(new DirectPositionType(1.0, 0.0, 1.0));
        coords.add(new DirectPositionType(0.0, 0.0, 1.0));
        LineStringType gml = new LineStringType("", coords);
        gml.setSrsName("EPSG:2154");

        final Geometry result = GeometrytoJTS.toJTS(gml);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void gmlPointToJTSTest2D() throws Exception {

        Point expected = GF.createPoint(new Coordinate(0, 1));
        expected.setSRID(2154);

        PointType gml = new PointType(new DirectPositionType(0.0, 1.0));

        final Geometry result = GeometrytoJTS.toJTS(gml);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void gmlPointToJTSTest3D() throws Exception {

        Point expected = GF.createPoint(new Coordinate(0, 1, 1));
        expected.setSRID(2154);

        PointType gml = new PointType(new DirectPositionType(0.0, 1.0, 1.0));

        final Geometry result = GeometrytoJTS.toJTS(gml);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void gmlGeodesicStringToJTSTest2D() throws Exception {

        final DirectPositionListType posLst = new DirectPositionListType(
                Arrays.asList(10.0,20.0,30.0,40.0,50.0,60.0));
        final GeodesicStringType s = new GeodesicStringType();
        s.setPosList(posLst);
        final CurveSegmentArrayPropertyType segments = new CurveSegmentArrayPropertyType();
        segments.setAbstractCurveSegment(s);
        final CurveType curve = new CurveType();
        curve.setSegments(segments);

        final LineString line = GF.createLineString(new Coordinate[]{
            new Coordinate(10, 20),
            new Coordinate(30, 40),
            new Coordinate(50, 60),
        });
        final MultiLineString expected = GF.createMultiLineString(new LineString[]{line});

        final Geometry result = GeometrytoJTS.toJTS(curve);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void gmlEnvelopeToJTSTest2D() throws Exception {
        final EnvelopeType env = new EnvelopeType(new DirectPositionType(2.0, 2.0), new DirectPositionType(4.0, 4.0), "EPSG:4326");
        final Geometry geom = GeometrytoJTS.toJTS(env);

        Coordinate[] expectedPoints = {
            new Coordinate(2.0, 2.0),
            new Coordinate(2.0, 4.0),
            new Coordinate(4.0, 4.0),
            new Coordinate(4.0, 2.0),
            new Coordinate(2.0, 2.0)
        };

        Assert.assertTrue(GF.createPolygon(expectedPoints).equalsTopo(geom));
    }

}
