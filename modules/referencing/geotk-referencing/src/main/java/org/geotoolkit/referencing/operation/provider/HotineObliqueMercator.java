/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2005-2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.referencing.operation.provider;

import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;

import org.geotoolkit.referencing.NamedIdentifier;
import org.geotoolkit.internal.referencing.Identifiers;
import org.geotoolkit.metadata.iso.citation.Citations;


/**
 * The provider for "<cite>Hotine Oblique Mercator</cite>" projection (EPSG:9812).
 * The programmatic names and parameters are enumerated at
 * <A HREF="http://www.remotesensing.org/geotiff/proj_list/hotine_oblique_mercator.html">Hotine
 * Oblique Mercator on RemoteSensing.org</A>. The math transform implementations instantiated by
 * this provider may be any of the following classes:
 * <p>
 * <ul>
 *   <li>{@link org.geotoolkit.referencing.operation.projection.ObliqueMercator}</li>
 * </ul>
 * <p>
 * This projection is similar to the {@linkplain ObliqueMercator oblique mercator} projection,
 * except that coordinates start at the intersection of the central line and the equator
 * of the aposphere.
 *
 * @author Rueben Schulz (UBC)
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.00
 *
 * @since 2.4
 * @module
 */
public class HotineObliqueMercator extends ObliqueMercator {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5822488360988630419L;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#falseEasting
     * false easting} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    @SuppressWarnings("hiding")
    public static final ParameterDescriptor<Double> FALSE_EASTING = Mercator1SP.FALSE_EASTING;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#falseNorthing
     * false northing} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    @SuppressWarnings("hiding")
    public static final ParameterDescriptor<Double> FALSE_NORTHING = Mercator1SP.FALSE_NORTHING;

    /**
     * The parameters group.
     */
    @SuppressWarnings("hiding")
    public static final ParameterDescriptorGroup PARAMETERS = Identifiers.createDescriptorGroup(new NamedIdentifier[] {
            new NamedIdentifier(Citations.OGC,      "Hotine_Oblique_Mercator"),
            new NamedIdentifier(Citations.EPSG,     "Hotine Oblique Mercator"),
            new NamedIdentifier(Citations.EPSG,     "9812"),
            new NamedIdentifier(Citations.GEOTIFF,  "CT_ObliqueMercator_Hotine"),
            // Note: The GeoTIFF numerical code (3) is already used by CT_ObliqueMercator.
            new NamedIdentifier(Citations.ESRI,     "Hotine_Oblique_Mercator_Azimuth_Natural_Origin"),
            new NamedIdentifier(Citations.ESRI,     "Rectified_Skew_Orthomorphic_Natural_Origin"),
            sameNameAs(Citations.GEOTOOLKIT, ObliqueMercator.PARAMETERS)
        }, new ParameterDescriptor[] {
            SEMI_MAJOR,          SEMI_MINOR, ROLL_LONGITUDE,
            LONGITUDE_OF_CENTRE, LATITUDE_OF_CENTRE,
            AZIMUTH,             RECTIFIED_GRID_ANGLE,
            SCALE_FACTOR,
            FALSE_EASTING,       FALSE_NORTHING
        });

    /**
     * Constructs a new provider.
     */
    public HotineObliqueMercator() {
        super(PARAMETERS);
    }

    /**
     * Constructs a new provider for the given parameters.
     */
    HotineObliqueMercator(ParameterDescriptorGroup parameters) {
        super(parameters);
    }




    /**
     * The provider for "<cite>Hotine Oblique Mercator</cite>" projection specified with two points
     * on the central line. This is different than the classical {@linkplain HotineObliqueMercator
     * Hotine Oblique Mercator}, which uses a central point and azimuth.
     *
     * @author Rueben Schulz (UBC)
     * @author Martin Desruisseaux (Geomatys)
     * @version 3.00
     *
     * @see org.geotoolkit.referencing.operation.projection.ObliqueMercator
     *
     * @since 2.4
     * @module
     */
    public static class TwoPoint extends HotineObliqueMercator {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -3104452416276842816L;

        /**
         * The operation parameter descriptor for the {@code latitudeOf1stPoint} parameter value.
         * Valid values range is [-90 &hellip; 90]&deg;. This parameter is mandatory and has no
         * default value.
         */
        public static final ParameterDescriptor<Double> LAT_OF_1ST_POINT =
                ObliqueMercator.TwoPoint.LAT_OF_1ST_POINT;

        /**
         * The operation parameter descriptor for the {@code longitudeOf1stPoint} parameter value.
         * Valid values range is [-180 &hellip; 180]&deg;. This parameter is mandatory and has no
         * default value.
         */
        public static final ParameterDescriptor<Double> LONG_OF_1ST_POINT =
                ObliqueMercator.TwoPoint.LONG_OF_1ST_POINT;

        /**
         * The operation parameter descriptor for the {@code latitudeOf2ndPoint} parameter value.
         * Valid values range is [-90 &hellip; 90]&deg;. This parameter is mandatory and has no
         * default value.
         */
        public static final ParameterDescriptor<Double> LAT_OF_2ND_POINT =
                ObliqueMercator.TwoPoint.LAT_OF_2ND_POINT;

        /**
         * The operation parameter descriptor for the {@code longitudeOf2ndPoint} parameter value.
         * Valid values range is [-180 &hellip; 180]&deg;. This parameter is mandatory and has no
         * default value.
         */
        public static final ParameterDescriptor<Double> LONG_OF_2ND_POINT =
                ObliqueMercator.TwoPoint.LONG_OF_2ND_POINT;

        /**
         * The parameters group.
         */
        @SuppressWarnings("hiding")
        public static final ParameterDescriptorGroup PARAMETERS = Identifiers.createDescriptorGroup(new NamedIdentifier[] {
                new NamedIdentifier(Citations.ESRI, "Hotine_Oblique_Mercator_Two_Point_Natural_Origin"),
                sameNameAs(Citations.GEOTOOLKIT, HotineObliqueMercator.PARAMETERS)
            }, new ParameterDescriptor[] {
                SEMI_MAJOR,          SEMI_MINOR, ROLL_LONGITUDE,
                LAT_OF_1ST_POINT,    LONG_OF_1ST_POINT,
                LAT_OF_2ND_POINT,    LONG_OF_2ND_POINT,
                LATITUDE_OF_CENTRE,  SCALE_FACTOR,
                FALSE_EASTING,       FALSE_NORTHING
            });

        /**
         * Constructs a new provider.
         */
        public TwoPoint() {
            super(PARAMETERS);
        }
    }
}
