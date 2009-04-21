/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2005-2009, Open Source Geospatial Foundation (OSGeo)
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

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.CylindricalProjection;

import org.geotoolkit.resources.Vocabulary;
import org.geotoolkit.referencing.NamedIdentifier;
import org.geotoolkit.referencing.operation.projection.Equirectangular;
import org.geotoolkit.internal.referencing.Identifiers;
import org.geotoolkit.metadata.iso.citation.Citations;


/**
 * The provider for "<cite>Equidistant Cylindrical</cite>" projection (EPSG:9842, 9823).
 * EPSG defines two codes for this projection, 9823 being the spherical case and 9842 the
 * ellipsoidal case. However the formulas are the same in both cases, with an additional
 * adjustement of Earth radius in the ellipsoidal case. Concequently they are implemented
 * in Geotoolkit by the same class.
 * <p>
 * The programmatic names and parameters are enumerated at
 * <A HREF="http://www.remotesensing.org/geotiff/proj_list/equirectangular.html">Equirectangular
 * on RemoteSensing.org</A>. The math transform implementations instantiated by this provider may
 * be any of the following classes:
 * <p>
 * <ul>
 *   <li>{@link org.geotoolkit.referencing.operation.projection.Equirectangular}</li>
 * </ul>
 *
 * @author John Grange
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.0
 *
 * @since 2.2
 * @module
 */
public class EquidistantCylindrical extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -278288251842178001L;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#centralMeridian
     * central meridian} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is [-180 &hellip; 180]&deg; and default value is 0&deg;.
     */
    public static final ParameterDescriptor<Double> CENTRAL_MERIDIAN =
            Identifiers.CENTRAL_MERIDIAN.select(
                "central_meridian",               // OGC
                "Central_Meridian",               // ESRI
                "Longitude of natural origin",    // EPSG
                "ProjCenterLong");                // GeoTIFF

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#latitudeOfOrigin
     * latitude of origin} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is [-90 &hellip; 90]&deg; and default value is 0&deg;.
     *
     * {@note ESRI includes a "<code>standard_parallel_1</code>" parameter instead of
     *        "<cite>Latitude of natural origin</cite>". The ESRI name is also the one
     *        used by Synder, and is declared as an alias.}
     */
    public static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN =
            Identifiers.LATITUDE_OF_ORIGIN.select(
                "latitude_of_origin",           // OGC
                "Latitude of natural origin",   // EPSG
                "Standard_Parallel_1",          // ESRI
                "ProjCenterLat");               // GeoTIFF

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#falseEasting
     * false easting} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_EASTING = Mercator1SP.FALSE_EASTING;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#falseNorthing
     * false northing} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is unrestricted and default value is 0 metre.
     */
    public static final ParameterDescriptor<Double> FALSE_NORTHING = Mercator1SP.FALSE_NORTHING;

    /**
     * The parameters group.
     */
    public static final ParameterDescriptorGroup PARAMETERS = Identifiers.createDescriptorGroup(new NamedIdentifier[] {
            new NamedIdentifier(Citations.OGC,      "Equidistant_Cylindrical"),
            new NamedIdentifier(Citations.EPSG,     "Equidistant Cylindrical"),
            new NamedIdentifier(Citations.EPSG,     "Equidistant Cylindrical (Spherical)"),
            new NamedIdentifier(Citations.EPSG,     "9842"), // The ellipsoidal case
            new NamedIdentifier(Citations.EPSG,     "9823"), // The cylindrical case
            new NamedIdentifier(Citations.GEOTIFF,  "CT_Equirectangular"),
            new NamedIdentifier(Citations.GEOTIFF,  "17"),
            new NamedIdentifier(Citations.GEOTOOLKIT, Vocabulary.formatInternational(
                                Vocabulary.Keys.EQUIDISTANT_CYLINDRICAL_PROJECTION))
        }, new ParameterDescriptor[] {
            SEMI_MAJOR,       SEMI_MINOR, ROLL_LONGITUDE,
            CENTRAL_MERIDIAN, LATITUDE_OF_ORIGIN,
            FALSE_EASTING,    FALSE_NORTHING
        });

    /**
     * Constructs a new provider.
     */
    public EquidistantCylindrical() {
        super(PARAMETERS);
    }

    /**
     * Constructs a new provider for the given parameters.
     */
    EquidistantCylindrical(ParameterDescriptorGroup parameters) {
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
        return Equirectangular.create(getParameters(), values);
    }
}
