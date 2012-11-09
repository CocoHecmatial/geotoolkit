/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Geomatys
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

package org.geotoolkit.gml.xml;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class GMLXmlFactory {
 
    public Point buildPoint(final String version, final org.opengis.geometry.DirectPosition pos) {
        if ("3.2.1".equals(version)) {
            return new org.geotoolkit.gml.xml.v321.PointType(pos);
        } else if ("3.1.1".equals(version)) {
            return new org.geotoolkit.gml.xml.v311.PointType(pos);
        } else {
            throw new IllegalArgumentException("unexpected gml version number:" + version);
        }
    }

    public MultiPoint buildMultiPoint(final String version, final List<Point> points, final String srsName) {
        if ("3.2.1".equals(version)) {
            final List<org.geotoolkit.gml.xml.v321.PointPropertyType> pointList = new ArrayList<org.geotoolkit.gml.xml.v321.PointPropertyType>();
            for (Point pt : points) {
                pointList.add(new org.geotoolkit.gml.xml.v321.PointPropertyType((org.geotoolkit.gml.xml.v321.PointType)pt));
            }
            return new org.geotoolkit.gml.xml.v321.MultiPointType(srsName, pointList);
        } else if ("3.1.1".equals(version)) {
            final List<org.geotoolkit.gml.xml.v311.PointPropertyType> pointList = new ArrayList<org.geotoolkit.gml.xml.v311.PointPropertyType>();
            for (Point pt : points) {
                pointList.add(new org.geotoolkit.gml.xml.v311.PointPropertyType((org.geotoolkit.gml.xml.v311.PointType)pt));
            }
            return new org.geotoolkit.gml.xml.v311.MultiPointType(srsName, pointList);
        } else {
            throw new IllegalArgumentException("unexpected gml version number:" + version);
        }
    }
    
    public LineString buildLineString(final String version, final List<org.opengis.geometry.DirectPosition> pos) {
        if ("3.2.1".equals(version)) {
            return new org.geotoolkit.gml.xml.v321.LineStringType(pos);
        } else if ("3.1.1".equals(version)) {
            return new org.geotoolkit.gml.xml.v311.LineStringType(pos);
        } else {
            throw new IllegalArgumentException("unexpected gml version number:" + version);
        }
    }
    
    public AbstractGeometricAggregate buildMultiLineString(final String version, final List<LineString> lines, final String srsName) {
        if ("3.2.1".equals(version)) {
            final List<org.geotoolkit.gml.xml.v321.CurvePropertyType> lineList = new ArrayList<org.geotoolkit.gml.xml.v321.CurvePropertyType>();
            for (LineString ls : lines) {
                lineList.add(new org.geotoolkit.gml.xml.v321.CurvePropertyType((org.geotoolkit.gml.xml.v321.LineStringType)ls));
            }
            return new org.geotoolkit.gml.xml.v321.MultiCurveType(srsName, lineList);
        } else if ("3.1.1".equals(version)) {
            final List<org.geotoolkit.gml.xml.v311.LineStringPropertyType> lineList = new ArrayList<org.geotoolkit.gml.xml.v311.LineStringPropertyType>();
            for (LineString ls : lines) {
                lineList.add(new org.geotoolkit.gml.xml.v311.LineStringPropertyType((org.geotoolkit.gml.xml.v311.LineStringType)ls));
            }
            return new org.geotoolkit.gml.xml.v311.MultiLineStringType(srsName, lineList);
        } else {
            throw new IllegalArgumentException("unexpected gml version number:" + version);
        }
    }
    
    public AbstractGeometricAggregate buildMultiPolygon(final String version, final List<Polygon> polygons, final String srsName) {
        if ("3.2.1".equals(version)) {
            final List<org.geotoolkit.gml.xml.v321.SurfacePropertyType> polyList = new ArrayList<org.geotoolkit.gml.xml.v321.SurfacePropertyType>();
            for (Polygon p : polygons) {
                polyList.add(new org.geotoolkit.gml.xml.v321.SurfacePropertyType((org.geotoolkit.gml.xml.v321.PolygonType) p));
            }
            return new org.geotoolkit.gml.xml.v321.MultiSurfaceType(srsName, polyList);
        } else if ("3.1.1".equals(version)) {
            final List<org.geotoolkit.gml.xml.v311.PolygonPropertyType> polyList = new ArrayList<org.geotoolkit.gml.xml.v311.PolygonPropertyType>();
            for (Polygon p : polygons) {
                polyList.add(new org.geotoolkit.gml.xml.v311.PolygonPropertyType((org.geotoolkit.gml.xml.v311.PolygonType)p));
            }
            return new org.geotoolkit.gml.xml.v311.MultiPolygonType(srsName, polyList);
        } else {
            throw new IllegalArgumentException("unexpected gml version number:" + version);
        }
    }
    
    public LinearRing buildLinearRing(final String version,  final List<Double> coordList, final String srsName) {
        if ("3.2.1".equals(version)) {
    
            final org.geotoolkit.gml.xml.v321.DirectPositionListType dpList = new org.geotoolkit.gml.xml.v321.DirectPositionListType(coordList);
            return new org.geotoolkit.gml.xml.v321.LinearRingType(srsName, dpList);
        } else if ("3.1.1".equals(version)) {
            final org.geotoolkit.gml.xml.v311.DirectPositionListType dpList = new org.geotoolkit.gml.xml.v311.DirectPositionListType(coordList);
            return new org.geotoolkit.gml.xml.v311.LinearRingType(srsName, dpList);
        } else {
            throw new IllegalArgumentException("unexpected gml version number:" + version);
        }
    }
    
    public Polygon buildPolygon(final String version, final AbstractRing gmlExterior, final List<AbstractRing> gmlInterior, final String srsName) {
        if ("3.2.1".equals(version)) {
            final List<org.geotoolkit.gml.xml.v321.AbstractRingType> interiors = new ArrayList<org.geotoolkit.gml.xml.v321.AbstractRingType>();
            if (gmlInterior != null) {
                for (AbstractRing ar : gmlInterior) {
                    if (ar != null && !(ar instanceof org.geotoolkit.gml.xml.v321.AbstractRingType)) {
                        throw new IllegalArgumentException("unexpected gml version for interior ring.(" + ar.getClass().getName()+ ")");
                    } else if (ar != null) {
                        interiors.add((org.geotoolkit.gml.xml.v321.AbstractRingType) ar);
                    }
                }
            }
            return new org.geotoolkit.gml.xml.v321.PolygonType(srsName, (org.geotoolkit.gml.xml.v321.AbstractRingType) gmlExterior, interiors);
        } else if ("3.1.1".equals(version)) {
            final List<org.geotoolkit.gml.xml.v311.AbstractRingType> interiors = new ArrayList<org.geotoolkit.gml.xml.v311.AbstractRingType>();
            if (gmlInterior != null) {
                for (AbstractRing ar : gmlInterior) {
                    if (ar != null && !(ar instanceof org.geotoolkit.gml.xml.v311.AbstractRingType)) {
                        throw new IllegalArgumentException("unexpected gml version for interior ring.");
                    } else if (ar != null) {
                        interiors.add((org.geotoolkit.gml.xml.v311.AbstractRingType)ar);
                    }
                }
            }
            return new org.geotoolkit.gml.xml.v311.PolygonType(srsName, (org.geotoolkit.gml.xml.v311.AbstractRingType)gmlExterior, interiors);
        } else {
            throw new IllegalArgumentException("unexpected gml version number:" + version);
        }
    }
    

}
