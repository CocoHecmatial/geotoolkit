/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2005-2012, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2012, Geomatys
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
package org.geotoolkit.referencing.operation.provider;

import net.jcip.annotations.Immutable;

import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.CylindricalProjection;
import org.opengis.referencing.ReferenceIdentifier;

import org.geotoolkit.resources.Vocabulary;
import org.geotoolkit.referencing.NamedIdentifier;
import org.geotoolkit.internal.referencing.Identifiers;
import org.geotoolkit.metadata.iso.citation.Citations;

import static org.geotoolkit.internal.referencing.Identifiers.exclude;


/**
 * The provider for "<cite>Oblique Mercator</cite>" projection (EPSG:9815).
 * The programmatic names and parameters are enumerated at
 * <A HREF="http://www.remotesensing.org/geotiff/proj_list/oblique_mercator.html">Oblique Mercator
 * on RemoteSensing.org</A>. The math transform implementations instantiated by this provider may be
 * any of the following classes:
 * <p>
 * <ul>
 *   <li>{@link org.geotoolkit.referencing.operation.projection.ObliqueMercator}</li>
 * </ul>
 *
 * @author Rueben Schulz (UBC)
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.20
 *
 * @since 2.1
 * @module
 */
@Immutable
public class ObliqueMercator extends MapProjection {
    /**
     * For compatibility with different versions during deserialization.
     */
    private static final long serialVersionUID = 201776686002266891L;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#centralMeridian
     * central meridian} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is [-180 &hellip; 180]&deg; and default value is 0&deg;.
     */
    public static final ParameterDescriptor<Double> LONGITUDE_OF_CENTRE;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#latitudeOfOrigin
     * latitude of origin} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is [-90 &hellip; 90]&deg; and default value is 0&deg;.
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_CENTRE;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#azimuth azimuth}
     * parameter value. Valid values range is from -360 to -270, -90 to 90, and 270 to 360 degrees.
     * This parameter is mandatory and has no default value.
     */
    public static final ParameterDescriptor<Double> AZIMUTH;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.ObliqueMercator.Parameters#rectifiedGridAngle
     * rectifiedGridAngle} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">optional</a>.
     * Valid values rage is [-360 &hellip; 360]&deg; and default value is the azimuth.
     */
    public static final ParameterDescriptor<Double> RECTIFIED_GRID_ANGLE;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#scaleFactor
     * scale factor} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is (0 &hellip; &infin;) and default value is 1.
     */
    public static final ParameterDescriptor<Double> SCALE_FACTOR;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#falseEasting
     * false easting} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_EASTING;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#falseNorthing
     * false northing} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_NORTHING;

    /**
     * Parameters creation, which must be done before to initialize the {@link #PARAMETERS} field.
     */
    static {
        final Citation[] excludes = new Citation[] {Citations.NETCDF};
        LONGITUDE_OF_CENTRE = Identifiers.CENTRAL_MERIDIAN.select(excludes, null,
                "Longitude of projection centre",   // EPSG
                "longitude_of_center",              // OGC
                "Longitude_Of_Center",              // ESRI
                "CenterLong");                      // GeoTIFF
        LATITUDE_OF_CENTRE = Identifiers.LATITUDE_OF_ORIGIN.select(excludes, null,
                "Latitude of projection centre",    // EPSG
                "latitude_of_center",               // OGC
                "Latitude_Of_Center",               // ESRI
                "CenterLat");                       // GeoTIFF
        AZIMUTH = Identifiers.AZIMUTH.select(excludes, null,
                "Azimuth of initial line");         // EPSG
        RECTIFIED_GRID_ANGLE = Identifiers.RECTIFIED_GRID_ANGLE;
        SCALE_FACTOR = Identifiers.SCALE_FACTOR.select(excludes, null,
                "Scale factor on initial line",     // EPSG
                "ScaleAtCenter");                   // GeoTIFF
        FALSE_EASTING = Identifiers.FALSE_EASTING.select(excludes, null,
                "Easting at projection centre",     // EPSG
                "FalseEasting");                    // GeoTIFF
        FALSE_NORTHING = Identifiers.FALSE_NORTHING.select(excludes, null,
                "Northing at projection centre",    // EPSG
                "FalseNorthing");                   // GeoTIFF
    }

    /**
     * The parameters group.
     */
    public static final ParameterDescriptorGroup PARAMETERS = Identifiers.createDescriptorGroup(
        new ReferenceIdentifier[] {
            new NamedIdentifier(Citations.OGC,     "Oblique_Mercator"),
            new NamedIdentifier(Citations.EPSG,    "Hotine Oblique Mercator (variant B)"), // Starting from 7.6
            new NamedIdentifier(Citations.EPSG,    "Oblique Mercator"), // Prior to EPSG database version 7.6
            new NamedIdentifier(Citations.EPSG,    "Rectified Skew Orthomorphic (RSO)"),
            new IdentifierCode (Citations.EPSG,     9815),
            new NamedIdentifier(Citations.GEOTIFF, "CT_ObliqueMercator"),
            new IdentifierCode (Citations.GEOTIFF,  3), // Also used by CT_ObliqueMercator_Hotine
            new NamedIdentifier(Citations.ESRI,    "Hotine_Oblique_Mercator_Azimuth_Center"),
            new NamedIdentifier(Citations.ESRI,    "Rectified_Skew_Orthomorphic_Center"),
            new NamedIdentifier(Citations.PROJ4,   "omerc"),
            new NamedIdentifier(Citations.GEOTOOLKIT, Vocabulary.formatInternational(
                                Vocabulary.Keys.OBLIQUE_MERCATOR_PROJECTION))
        }, new ParameterDescriptor<?>[] {
            SEMI_MAJOR, SEMI_MINOR, ROLL_LONGITUDE,
            LONGITUDE_OF_CENTRE, LATITUDE_OF_CENTRE,
            AZIMUTH, RECTIFIED_GRID_ANGLE, SCALE_FACTOR,
            FALSE_EASTING, FALSE_NORTHING
        });

    /**
     * Constructs a new provider.
     */
    public ObliqueMercator() {
        super(PARAMETERS);
    }

    /**
     * Constructs a new provider for the given parameters.
     */
    ObliqueMercator(ParameterDescriptorGroup parameters) {
        super(parameters);
    }

    /**
     * Returns the operation type for this map projection.
     */
    @Override
    public Class<CylindricalProjection> getOperationType() {
        return CylindricalProjection.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MathTransform2D createMathTransform(ParameterValueGroup values) {
        return org.geotoolkit.referencing.operation.projection.ObliqueMercator.create(getParameters(), values);
    }




    /**
     * The provider for "<cite>Oblique Mercator</cite>" projection specified with two points
     * on the central line. This is different than the classical {@linkplain ObliqueMercator
     * Oblique Mercator}, which uses a central point and azimuth.
     *
     * @author Rueben Schulz (UBC)
     * @author Martin Desruisseaux (Geomatys)
     * @version 3.20
     *
     * @see org.geotoolkit.referencing.operation.projection.ObliqueMercator
     *
     * @since 2.1
     * @module
     */
    @Immutable
    public static class TwoPoint extends ObliqueMercator {
        /**
         * For compatibility with different versions during deserialization.
         */
        private static final long serialVersionUID = 7124258885016543889L;

        /**
         * The operation parameter descriptor for the {@code latitudeOf1stPoint} parameter value.
         * Valid values range is [-90 &hellip; 90]&deg;. This parameter is mandatory and has no
         * default value.
         */
        public static final ParameterDescriptor<Double> LAT_OF_1ST_POINT = Identifiers.LAT_OF_1ST_POINT;

        /**
         * The operation parameter descriptor for the {@code longitudeOf1stPoint} parameter value.
         * Valid values range is [-180 &hellip; 180]&deg;. This parameter is mandatory and has no
         * default value.
         */
        public static final ParameterDescriptor<Double> LONG_OF_1ST_POINT = Identifiers.LONG_OF_1ST_POINT;

        /**
         * The operation parameter descriptor for the {@code latitudeOf2ndPoint} parameter value.
         * Valid values range is [-90 &hellip; 90]&deg;. This parameter is mandatory and has no
         * default value.
         */
        public static final ParameterDescriptor<Double> LAT_OF_2ND_POINT = Identifiers.LAT_OF_2ND_POINT;

        /**
         * The operation parameter descriptor for the {@code longitudeOf2ndPoint} parameter value.
         * Valid values range is [-180 &hellip; 180]&deg;. This parameter is mandatory and has no
         * default value.
         */
        public static final ParameterDescriptor<Double> LONG_OF_2ND_POINT = Identifiers.LONG_OF_2ND_POINT;

        /**
         * The parameters group.
         */
        @SuppressWarnings("hiding")
        public static final ParameterDescriptorGroup PARAMETERS;
        static {
            final Citation[] excludes = new Citation[] {
                Citations.EPSG, Citations.OGC, Citations.NETCDF, Citations.GEOTIFF, Citations.PROJ4
            };
            PARAMETERS = Identifiers.createDescriptorGroup(
            new NamedIdentifier[] {
                new NamedIdentifier(Citations.ESRI, "Hotine_Oblique_Mercator_Two_Point_Center"),
                sameNameAs(Citations.GEOTOOLKIT, ObliqueMercator.PARAMETERS)
            }, new ParameterDescriptor<?>[] {
                exclude(SEMI_MAJOR, excludes),
                exclude(SEMI_MINOR, excludes),
                ROLL_LONGITUDE,
                LAT_OF_1ST_POINT,    LONG_OF_1ST_POINT,
                LAT_OF_2ND_POINT,    LONG_OF_2ND_POINT,
                exclude(LATITUDE_OF_CENTRE, excludes),
                exclude(SCALE_FACTOR,       excludes),
                exclude(FALSE_EASTING,      excludes),
                exclude(FALSE_NORTHING,     excludes)
            });
        }

        /**
         * Constructs a new provider.
         */
        public TwoPoint() {
            super(PARAMETERS);
        }
    }
}
