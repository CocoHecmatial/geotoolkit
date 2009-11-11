/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009, Geomatys
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
package org.geotoolkit.referencing;

import java.util.Set;
import java.awt.geom.Rectangle2D;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.geotoolkit.test.Depend;
import org.geotoolkit.geometry.GeneralEnvelope;
import org.geotoolkit.display.shape.XRectangle2D;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.geotoolkit.referencing.crs.CoordinateReferenceSystemTest;

import org.junit.*;
import static org.junit.Assert.*;
import static org.geotoolkit.test.Commons.decodeQuotes;


/**
 * Tests the {@link CRS} class. This is actually an indirect way to test many referencing
 * service (WKT parsing, object comparisons, <cite>etc.</cite>).
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.02
 *
 * @since 3.00
 */
@Depend(CoordinateReferenceSystemTest.class)
public final class CRS_Test {
    /**
     * Tests the {@link CRS#getSupportedAuthorities} method.
     */
    @Test
    public void testSupportedAuthorities() {
        final Set<String> withoutAlias = CRS.getSupportedAuthorities(false);
        assertTrue (withoutAlias.contains("CRS"));
        assertTrue (withoutAlias.contains("AUTO2"));
        assertTrue (withoutAlias.contains("urn:ogc:def"));
        assertTrue (withoutAlias.contains("http://www.opengis.net"));
        assertFalse(withoutAlias.contains("AUTO"));
        assertFalse(withoutAlias.contains("urn:x-ogc:def"));

        final Set<String> withAlias = CRS.getSupportedAuthorities(true);
        assertTrue (withAlias.containsAll(withoutAlias));
        assertFalse(withoutAlias.containsAll(withAlias));
        assertTrue (withAlias.contains("AUTO"));
        assertTrue (withAlias.contains("urn:x-ogc:def"));
    }

    /**
     * Tests simple decode.
     *
     * @throws FactoryException Should never happen.
     */
    @Test
    public void testDecode() throws FactoryException {
        assertSame(DefaultGeographicCRS.WGS84, CRS.decode("WGS84(DD)"));
    }

    /**
     * Tests an ESRI code.
     *
     * @throws Exception Should never happen.
     *
     * @todo Not yet working.
     */
    @Test
    @Ignore
    public void testESRICode() throws Exception {
        String wkt = "PROJCS[\"Albers_Conic_Equal_Area\",\n"                  +
                     "  GEOGCS[\"GCS_North_American_1983\",\n"                +
                     "    DATUM[\"D_North_American_1983\",\n"                 +
                     "    SPHEROID[\"GRS_1980\",6378137.0,298.257222101]],\n" +
                     "    PRIMEM[\"Greenwich\",0.0],\n"                       +
                     "    UNIT[\"Degree\",0.0174532925199433]],\n"            +
                     "  PROJECTION[\"Equidistant_Conic\"],\n"                 +
                     "  PARAMETER[\"False_Easting\",0.0],\n"                  +
                     "  PARAMETER[\"False_Northing\",0.0],\n"                 +
                     "  PARAMETER[\"Central_Meridian\",-96.0],\n"             +
                     "  PARAMETER[\"Standard_Parallel_1\",33.0],\n"           +
                     "  PARAMETER[\"Standard_Parallel_2\",45.0],\n"           +
                     "  PARAMETER[\"Latitude_Of_Origin\",39.0],\n"            +
                     "  UNIT[\"Meter\",1.0]]";
        CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
        final CoordinateReferenceSystem WGS84  = DefaultGeographicCRS.WGS84;
        final MathTransform crsTransform = CRS.findMathTransform(WGS84, crs, true);
        assertFalse(crsTransform.isIdentity());
    }

    /**
     * Tests the comparisons of two objects which should be equivalent despite their
     * different representation of the math transform.
     *
     * @throws FactoryException Should never happen.
     *
     * @see http://jira.codehaus.org/browse/GEOT-1268
     */
    @Test
    public void testEquivalence() throws FactoryException {
        final CoordinateReferenceSystem crs1 = CRS.parseWKT(decodeQuotes(
                "PROJCS[“NAD_1983_StatePlane_Massachusetts_Mainland_FIPS_2001”, \n" +
                "  GEOGCS[“GCS_North_American_1983”, \n" +
                "    DATUM[“D_North_American_1983”, \n" +
                "      SPHEROID[“GRS_1980”, 6378137.0, 298.257222101]], \n" +
                "    PRIMEM[“Greenwich”, 0.0], \n" +
                "    UNIT[“degree”, 0.017453292519943295], \n" +
                "    AXIS[“Longitude”, EAST], \n" +
                "    AXIS[“Latitude”, NORTH]], \n" +
                "  PROJECTION[“Lambert_Conformal_Conic”], \n" +
                "  PARAMETER[“central_meridian”, -71.5], \n" +
                "  PARAMETER[“latitude_of_origin”, 41.0], \n" +
                "  PARAMETER[“standard_parallel_1”, 41.7166666666666667], \n" +
                "  PARAMETER[“scale_factor”, 1.0], \n" +
                "  PARAMETER[“false_easting”, 200000.0], \n" +
                "  PARAMETER[“false_northing”, 750000.0], \n" +
                "  PARAMETER[“standard_parallel_2”, 42.6833333333333333], \n" +
                "  UNIT[“m”, 1.0], \n" +
                "  AXIS[“x”, EAST], \n" +
                "  AXIS[“y”, NORTH]]"));

        assertEquals("NAD_1983_StatePlane_Massachusetts_Mainland_FIPS_2001", CRS.getDeclaredIdentifier(crs1));

        final CoordinateReferenceSystem crs2 = CRS.parseWKT(decodeQuotes(
                "PROJCS[“NAD83 / Massachusetts Mainland”, \n" +
                "  GEOGCS[“NAD83”, \n" +
                "    DATUM[“North American Datum 1983”, \n" +
                "      SPHEROID[“GRS 1980”, 6378137.0, 298.257222101, AUTHORITY[“EPSG”,“7019”]], \n" +
                "      TOWGS84[1.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0], \n" +
                "      AUTHORITY[“EPSG”,“6269”]], \n" +
                "    PRIMEM[“Greenwich”, 0.0, AUTHORITY[“EPSG”,“8901”]], \n" +
                "    UNIT[“degree”, 0.017453292519943295], \n" +
                "    AXIS[“Geodetic longitude”, EAST], \n" +
                "    AXIS[“Geodetic latitude”, NORTH], \n" +
                "    AUTHORITY[“EPSG”,“4269”]], \n" +
                "  PROJECTION[“Lambert Conic Conformal (2SP)”, AUTHORITY[“EPSG”,“9802”]], \n" +
                "  PARAMETER[“central_meridian”, -71.5], \n" +
                "  PARAMETER[“latitude_of_origin”, 41.0], \n" +
                "  PARAMETER[“standard_parallel_1”, 42.6833333333333333], \n" +
                "  PARAMETER[“false_easting”, 200000.0], \n" +
                "  PARAMETER[“false_northing”, 750000.0], \n" +
                "  PARAMETER[“standard_parallel_2”, 41.7166666666666667], \n" +
                "  UNIT[“m”, 1.0], \n" +
                "  AXIS[“Easting”, EAST], \n" +
                "  AXIS[“Northing”, NORTH], \n" +
                "  AUTHORITY[“EPSG”,“26986”]]"));

        assertEquals("EPSG:26986", CRS.getDeclaredIdentifier(crs2));

        assertTrue(CRS.equalsIgnoreMetadata(crs1, crs2));
    }

    /**
     * Tests the transformations of an envelope.
     *
     * @throws FactoryException Should never happen.
     * @throws TransformException Should never happen.
     */
    @Test
    public void testEnvelopeTransformation() throws FactoryException, TransformException {
        final CoordinateReferenceSystem mapCRS = CRS.parseWKT(WKT.PROJCS_UTM_10N);
        final CoordinateReferenceSystem WGS84  = DefaultGeographicCRS.WGS84;
        final MathTransform crsTransform = CRS.findMathTransform(WGS84, mapCRS, true);
        assertFalse(crsTransform.isIdentity());

        final GeneralEnvelope firstEnvelope, transformedEnvelope, oldEnvelope;
        firstEnvelope = new GeneralEnvelope(new double[] {-124, 42}, new double[] {-122, 43});
        firstEnvelope.setCoordinateReferenceSystem(WGS84);

        transformedEnvelope = CRS.transform(crsTransform, firstEnvelope);
        transformedEnvelope.setCoordinateReferenceSystem(mapCRS);

        oldEnvelope = CRS.transform(crsTransform.inverse(), transformedEnvelope);
        oldEnvelope.setCoordinateReferenceSystem(WGS84);

        assertTrue(oldEnvelope.contains(firstEnvelope, true));
        assertTrue(oldEnvelope.equals  (firstEnvelope, 0.02, true));
    }

    /**
     * Tests the transformations of a rectangle using a coordinate operation.
     * With assertions enabled, this also test the transformation of an envelope.
     *
     * @throws FactoryException Should never happen.
     * @throws TransformException Should never happen.
     */
    @Test
    public void testTransformationOverPole() throws FactoryException, TransformException {
        final CoordinateReferenceSystem mapCRS = CRS.parseWKT(WKT.PROJCS_POLAR_STEREOGRAPHIC);
        final CoordinateReferenceSystem WGS84  = DefaultGeographicCRS.WGS84;
        final CoordinateOperation operation =
                CRS.getCoordinateOperationFactory(false).createOperation(mapCRS, WGS84);
        final MathTransform transform = operation.getMathTransform();
        assertTrue(transform instanceof MathTransform2D);
        /*
         * The rectangle to test, which contains the South pole.
         */
        Rectangle2D envelope = XRectangle2D.createFromExtremums(
                -3943612.4042124213, -4078471.954436003,
                 3729092.5890516187,  4033483.085688618);
        /*
         * This is what we get without special handling of singularity point.
         * Note that is doesn't include the South pole as we would expect.
         */
        Rectangle2D expected = XRectangle2D.createFromExtremums(
                -178.49352310409273, -88.99136583196398,
                 137.56220967463082, -40.905775004205864);
        /*
         * Tests what we actually get.
         */
        Rectangle2D actual = CRS.transform((MathTransform2D) transform, envelope, null);
        assertTrue(XRectangle2D.equalsEpsilon(expected, actual));
        /*
         * Using the transform(CoordinateOperation, ...) method,
         * the singularity at South pole is taken in account.
         */
        expected = XRectangle2D.createFromExtremums(-180, -90, 180, -40.905775004205864);
        actual = CRS.transform(operation, envelope, actual);
        assertTrue(XRectangle2D.equalsEpsilon(expected, actual));
        /*
         * The rectangle to test, which contains the South pole, but this time the south
         * pole is almost in a corner of the rectangle
         */
        envelope = XRectangle2D.createFromExtremums(-4000000, -4000000, 300000, 30000);
        expected = XRectangle2D.createFromExtremums(-180, -90, 180, -41.03163170198091);
        actual = CRS.transform(operation, envelope, actual);
        assertTrue(XRectangle2D.equalsEpsilon(expected, actual));
    }
}
