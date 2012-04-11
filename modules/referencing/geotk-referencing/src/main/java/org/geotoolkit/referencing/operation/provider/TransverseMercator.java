/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 1999-2012, Open Source Geospatial Foundation (OSGeo)
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
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.CylindricalProjection;

import org.geotoolkit.resources.Vocabulary;
import org.geotoolkit.referencing.NamedIdentifier;
import org.geotoolkit.internal.referencing.Identifiers;
import org.geotoolkit.metadata.iso.citation.Citations;


/**
 * The provider for "<cite>Transverse Mercator</cite>" projection (EPSG:9807).
 * The math transform implementations instantiated by this provider may be any of the following classes:
 * <p>
 * <ul>
 *   <li>{@link org.geotoolkit.referencing.operation.projection.TransverseMercator}</li>
 * </ul>
 *
 * <!-- PARAMETERS TransverseMercator -->
 * <p>The following table summarizes the parameters recognized by this provider.
 * For a more detailed parameter list, see the {@link #PARAMETERS} constant.</p>
 * <blockquote><p><b>Operation name:</b> {@code Transverse_Mercator}</p>
 * <table class="geotk">
 *   <tr><th>Parameter Name</th><th>Default value</th></tr>
 *   <tr><td>{@code semi_major}</td><td></td></tr>
 *   <tr><td>{@code semi_minor}</td><td></td></tr>
 *   <tr><td>{@code roll_longitude}</td><td>false</td></tr>
 *   <tr><td>{@code central_meridian}</td><td>0°</td></tr>
 *   <tr><td>{@code latitude_of_origin}</td><td>0°</td></tr>
 *   <tr><td>{@code scale_factor}</td><td>1</td></tr>
 *   <tr><td>{@code false_easting}</td><td>0 metres</td></tr>
 *   <tr><td>{@code false_northing}</td><td>0 metres</td></tr>
 * </table></blockquote>
 * <!-- END OF PARAMETERS -->
 *
 * @author Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author Rueben Schulz (UBC)
 * @version 3.20
 *
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/transverse_mercator.html">Transverse Mercator on RemoteSensing.org</A>
 * @see <a href="{@docRoot}/../modules/referencing/operation-parameters.html">Geotk coordinate operations matrix</a>
 *
 * @since 2.1
 * @module
 */
@Immutable
public class TransverseMercator extends MapProjection {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3386587506686432398L;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#centralMeridian
     * central meridian} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is [-180 &hellip; 180]&deg; and default value is 0&deg;.
     *
     * @deprecated Invoke <code>{@linkplain #PARAMETERS}.{@linkplain ParameterDescriptorGroup#descriptor(String)
     * descriptor(String)}</code> instead.
     */
    @Deprecated
    public static final ParameterDescriptor<Double> CENTRAL_MERIDIAN =
            Identifiers.CENTRAL_MERIDIAN.select(null,
                "Longitude of natural origin",    // EPSG
                "central_meridian",               // OGC
                "Central_Meridian",               // ESRI
                "longitude_of_central_meridian",  // NetCDF
                "NatOriginLong");                 // GeoTIFF

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#latitudeOfOrigin
     * latitude of origin} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is [-90 &hellip; 90]&deg; and default value is 0&deg;.
     *
     * @deprecated Invoke <code>{@linkplain #PARAMETERS}.{@linkplain ParameterDescriptorGroup#descriptor(String)
     * descriptor(String)}</code> instead.
     */
    @Deprecated
    public static final ParameterDescriptor<Double> LATITUDE_OF_ORIGIN = Mercator2SP.LATITUDE_OF_ORIGIN;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#scaleFactor
     * scale factor} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is (0 &hellip; &infin;) and default value is 1.
     *
     * @deprecated Invoke <code>{@linkplain #PARAMETERS}.{@linkplain ParameterDescriptorGroup#descriptor(String)
     * descriptor(String)}</code> instead.
     */
    @Deprecated
    public static final ParameterDescriptor<Double> SCALE_FACTOR =
            Identifiers.SCALE_FACTOR.select(null,
                "Scale factor at natural origin",   // EPSG
                "scale_factor_at_central_meridian", // NetCDF
                "ScaleAtNatOrigin");                // GeoTIFF

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#falseEasting
     * false easting} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is unrestricted and default value is 0 metre.
     *
     * @deprecated Invoke <code>{@linkplain #PARAMETERS}.{@linkplain ParameterDescriptorGroup#descriptor(String)
     * descriptor(String)}</code> instead.
     */
    @Deprecated
    public static final ParameterDescriptor<Double> FALSE_EASTING = Mercator2SP.FALSE_EASTING;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#falseNorthing
     * false northing} parameter value.
     *
     * This parameter is <a href="package-summary.html#Obligation">mandatory</a>.
     * Valid values range is unrestricted and default value is 0 metre.
     *
     * @deprecated Invoke <code>{@linkplain #PARAMETERS}.{@linkplain ParameterDescriptorGroup#descriptor(String)
     * descriptor(String)}</code> instead.
     */
    @Deprecated
    public static final ParameterDescriptor<Double> FALSE_NORTHING = Mercator2SP.FALSE_NORTHING;

    /**
     * The group of all parameters expected by this coordinate operation.
     * The following table lists the operation names and the parameters recognized by Geotk:
     * <p>
     * <!-- GENERATED PARAMETERS - inserted by ProjectionParametersJavadoc -->
     * <table class="geotk" border="1">
     *   <tr><th colspan="2">
     *     <table class="compact">
     *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code Transverse_Mercator}</td></tr>
     *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Transverse Mercator}</td></tr>
     *       <tr><td></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Gauss-Kruger}</td></tr>
     *       <tr><td></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Gauss-Boaga}</td></tr>
     *       <tr><td></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code TM}</td></tr>
     *       <tr><td></td><td class="onright">{@code ESRI}:</td><td class="onleft">{@code Transverse_Mercator}</td></tr>
     *       <tr><td></td><td class="onright">{@code ESRI}:</td><td class="onleft">{@code Gauss_Kruger}</td></tr>
     *       <tr><td></td><td class="onright">{@code NetCDF}:</td><td class="onleft">{@code TransverseMercator}</td></tr>
     *       <tr><td></td><td class="onright">{@code GeoTIFF}:</td><td class="onleft">{@code CT_TransverseMercator}</td></tr>
     *       <tr><td></td><td class="onright">{@code PROJ4}:</td><td class="onleft">{@code tmerc}</td></tr>
     *       <tr><td></td><td class="onright">{@code Geotk}:</td><td class="onleft">{@code Transverse Mercator projection}</td></tr>
     *       <tr><td><b>Identifier:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code 9807}</td></tr>
     *       <tr><td></td><td class="onright">{@code GeoTIFF}:</td><td class="onleft">{@code 1}</td></tr>
     *     </table>
     *   </th></tr>
     *   <tr><td>
     *     <table class="compact">
     *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code semi_major}</td></tr>
     *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Semi-major axis}</td></tr>
     *       <tr><td></td><td class="onright">{@code ESRI}:</td><td class="onleft">{@code Semi_Major}</td></tr>
     *       <tr><td></td><td class="onright">{@code GeoTIFF}:</td><td class="onleft">{@code SemiMajor}</td></tr>
     *       <tr><td></td><td class="onright">{@code PROJ4}:</td><td class="onleft">{@code a}</td></tr>
     *     </table>
     *   </td><td>
     *     <table class="compact">
     *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
     *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
     *       <tr><td><b>Value range:</b></td><td>[0…∞) metres</td></tr>
     *     </table>
     *   </td></tr>
     *   <tr><td>
     *     <table class="compact">
     *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code semi_minor}</td></tr>
     *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Semi-minor axis}</td></tr>
     *       <tr><td></td><td class="onright">{@code ESRI}:</td><td class="onleft">{@code Semi_Minor}</td></tr>
     *       <tr><td></td><td class="onright">{@code GeoTIFF}:</td><td class="onleft">{@code SemiMinor}</td></tr>
     *       <tr><td></td><td class="onright">{@code PROJ4}:</td><td class="onleft">{@code b}</td></tr>
     *     </table>
     *   </td><td>
     *     <table class="compact">
     *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
     *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
     *       <tr><td><b>Value range:</b></td><td>[0…∞) metres</td></tr>
     *     </table>
     *   </td></tr>
     *   <tr><td>
     *     <table class="compact">
     *       <tr><td><b>Name:</b></td><td class="onright">{@code Geotk}:</td><td class="onleft">{@code roll_longitude}</td></tr>
     *     </table>
     *   </td><td>
     *     <table class="compact">
     *       <tr><td><b>Type:</b></td><td>{@code Boolean}</td></tr>
     *       <tr><td><b>Obligation:</b></td><td>optional</td></tr>
     *       <tr><td><b>Default value:</b></td><td>false</td></tr>
     *     </table>
     *   </td></tr>
     *   <tr><td>
     *     <table class="compact">
     *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code central_meridian}</td></tr>
     *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Longitude of natural origin}</td></tr>
     *       <tr><td></td><td class="onright">{@code ESRI}:</td><td class="onleft">{@code Central_Meridian}</td></tr>
     *       <tr><td></td><td class="onright">{@code NetCDF}:</td><td class="onleft">{@code longitude_of_central_meridian}</td></tr>
     *       <tr><td></td><td class="onright">{@code GeoTIFF}:</td><td class="onleft">{@code NatOriginLong}</td></tr>
     *       <tr><td></td><td class="onright">{@code PROJ4}:</td><td class="onleft">{@code lon_0}</td></tr>
     *     </table>
     *   </td><td>
     *     <table class="compact">
     *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
     *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
     *       <tr><td><b>Value range:</b></td><td>[-180 … 180]°</td></tr>
     *       <tr><td><b>Default value:</b></td><td>0°</td></tr>
     *     </table>
     *   </td></tr>
     *   <tr><td>
     *     <table class="compact">
     *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code latitude_of_origin}</td></tr>
     *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Latitude of natural origin}</td></tr>
     *       <tr><td></td><td class="onright">{@code ESRI}:</td><td class="onleft">{@code Latitude_Of_Origin}</td></tr>
     *       <tr><td></td><td class="onright">{@code NetCDF}:</td><td class="onleft">{@code latitude_of_projection_origin}</td></tr>
     *       <tr><td></td><td class="onright">{@code GeoTIFF}:</td><td class="onleft">{@code NatOriginLat}</td></tr>
     *       <tr><td></td><td class="onright">{@code PROJ4}:</td><td class="onleft">{@code lat_0}</td></tr>
     *     </table>
     *   </td><td>
     *     <table class="compact">
     *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
     *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
     *       <tr><td><b>Value range:</b></td><td>[-90 … 90]°</td></tr>
     *       <tr><td><b>Default value:</b></td><td>0°</td></tr>
     *     </table>
     *   </td></tr>
     *   <tr><td>
     *     <table class="compact">
     *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code scale_factor}</td></tr>
     *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Scale factor at natural origin}</td></tr>
     *       <tr><td></td><td class="onright">{@code ESRI}:</td><td class="onleft">{@code Scale_Factor}</td></tr>
     *       <tr><td></td><td class="onright">{@code NetCDF}:</td><td class="onleft">{@code scale_factor_at_central_meridian}</td></tr>
     *       <tr><td></td><td class="onright">{@code GeoTIFF}:</td><td class="onleft">{@code ScaleAtNatOrigin}</td></tr>
     *       <tr><td></td><td class="onright">{@code PROJ4}:</td><td class="onleft">{@code k}</td></tr>
     *     </table>
     *   </td><td>
     *     <table class="compact">
     *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
     *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
     *       <tr><td><b>Value range:</b></td><td>[0…∞)</td></tr>
     *       <tr><td><b>Default value:</b></td><td>1</td></tr>
     *     </table>
     *   </td></tr>
     *   <tr><td>
     *     <table class="compact">
     *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code false_easting}</td></tr>
     *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code False easting}</td></tr>
     *       <tr><td></td><td class="onright">{@code ESRI}:</td><td class="onleft">{@code False_Easting}</td></tr>
     *       <tr><td></td><td class="onright">{@code NetCDF}:</td><td class="onleft">{@code false_easting}</td></tr>
     *       <tr><td></td><td class="onright">{@code GeoTIFF}:</td><td class="onleft">{@code FalseEasting}</td></tr>
     *       <tr><td></td><td class="onright">{@code PROJ4}:</td><td class="onleft">{@code x_0}</td></tr>
     *     </table>
     *   </td><td>
     *     <table class="compact">
     *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
     *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
     *       <tr><td><b>Value range:</b></td><td>(-∞ … ∞) metres</td></tr>
     *       <tr><td><b>Default value:</b></td><td>0 metres</td></tr>
     *     </table>
     *   </td></tr>
     *   <tr><td>
     *     <table class="compact">
     *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code false_northing}</td></tr>
     *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code False northing}</td></tr>
     *       <tr><td></td><td class="onright">{@code ESRI}:</td><td class="onleft">{@code False_Northing}</td></tr>
     *       <tr><td></td><td class="onright">{@code NetCDF}:</td><td class="onleft">{@code false_northing}</td></tr>
     *       <tr><td></td><td class="onright">{@code GeoTIFF}:</td><td class="onleft">{@code FalseNorthing}</td></tr>
     *       <tr><td></td><td class="onright">{@code PROJ4}:</td><td class="onleft">{@code y_0}</td></tr>
     *     </table>
     *   </td><td>
     *     <table class="compact">
     *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
     *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
     *       <tr><td><b>Value range:</b></td><td>(-∞ … ∞) metres</td></tr>
     *       <tr><td><b>Default value:</b></td><td>0 metres</td></tr>
     *     </table>
     *   </td></tr>
     * </table>
     */
    public static final ParameterDescriptorGroup PARAMETERS = Identifiers.createDescriptorGroup(
        new ReferenceIdentifier[] {
            new NamedIdentifier(Citations.OGC,      "Transverse_Mercator"),
            new NamedIdentifier(Citations.EPSG,     "Transverse Mercator"),
            new NamedIdentifier(Citations.EPSG,     "Gauss-Kruger"),
            new NamedIdentifier(Citations.EPSG,     "Gauss-Boaga"),
            new NamedIdentifier(Citations.EPSG,     "TM"),
            new IdentifierCode (Citations.EPSG,      9807),
            new NamedIdentifier(Citations.ESRI,     "Transverse_Mercator"),
            new NamedIdentifier(Citations.ESRI,     "Gauss_Kruger"),
            new NamedIdentifier(Citations.NETCDF,   "TransverseMercator"),
            new NamedIdentifier(Citations.GEOTIFF,  "CT_TransverseMercator"),
            new IdentifierCode (Citations.GEOTIFF,   1),
            new NamedIdentifier(Citations.PROJ4,    "tmerc"),
            new NamedIdentifier(Citations.GEOTOOLKIT, Vocabulary.formatInternational(
                                Vocabulary.Keys.TRANSVERSE_MERCATOR_PROJECTION)),
        }, null, new ParameterDescriptor<?>[] {
            SEMI_MAJOR, SEMI_MINOR, ROLL_LONGITUDE,
            CENTRAL_MERIDIAN, LATITUDE_OF_ORIGIN,
            SCALE_FACTOR, FALSE_EASTING, FALSE_NORTHING
        });

    /**
     * Constructs a new provider.
     */
    public TransverseMercator() {
        super(PARAMETERS);
    }

    /**
     * Constructs a new provider with the specified parameters.
     */
    TransverseMercator(final ParameterDescriptorGroup descriptor) {
        super(descriptor);
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
        return org.geotoolkit.referencing.operation.projection.TransverseMercator.create(getParameters(), values);
    }




    /**
     * The provider for <cite>Mercator Transverse (South Orientated)</cite> projection
     * (EPSG:9808). The coordinate axes are called <cite>Westings</cite> and <cite>Southings</cite>
     * and increment to the West and South from the origin respectively.
     * <p>
     * The terms <cite>false easting</cite> (FE) and <cite>false northing</cite> (FN) increase
     * the Westing and Southing value at the natural origin. In other words they are effectively
     * <cite>false westing</cite> (FW) and <cite>false southing</cite> (FS) respectively.
     *
     * <!-- PARAMETERS SouthOrientated -->
     * <p>The following table summarizes the parameters recognized by this provider.
     * For a more detailed parameter list, see the {@link #PARAMETERS} constant.</p>
     * <blockquote><p><b>Operation name:</b> {@code Transverse Mercator (South Orientated)}</p>
     * <table class="geotk">
     *   <tr><th>Parameter Name</th><th>Default value</th></tr>
     *   <tr><td>{@code semi_major}</td><td></td></tr>
     *   <tr><td>{@code semi_minor}</td><td></td></tr>
     *   <tr><td>{@code roll_longitude}</td><td>false</td></tr>
     *   <tr><td>{@code central_meridian}</td><td>0°</td></tr>
     *   <tr><td>{@code latitude_of_origin}</td><td>0°</td></tr>
     *   <tr><td>{@code scale_factor}</td><td>1</td></tr>
     *   <tr><td>{@code false_easting}</td><td>0 metres</td></tr>
     *   <tr><td>{@code false_northing}</td><td>0 metres</td></tr>
     * </table></blockquote>
     * <!-- END OF PARAMETERS -->
     *
     * @author Martin Desruisseaux (MPO, IRD, Geomatys)
     * @version 3.20
     *
     * @see org.geotoolkit.referencing.operation.projection.TransverseMercator
     * @see <a href="{@docRoot}/../modules/referencing/operation-parameters.html">Geotk coordinate operations matrix</a>
     *
     * @since 2.2
     * @module
     */
    @Immutable
    public static class SouthOrientated extends TransverseMercator {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5938929136350638347L;

        /**
         * The group of all parameters expected by this coordinate operation.
         * The following table lists the operation names and the parameters recognized by Geotk.
         * Note that the terms <cite>false easting</cite> (FE) and <cite>false northing</cite> (FN)
         * increase the Westing and Southing value at the natural origin. In other words they are
         * effectively <cite>false westing</cite> (FW) and <cite>false southing</cite> (FS) respectively.
         * <p>
         * <!-- GENERATED PARAMETERS - inserted by ProjectionParametersJavadoc -->
         * <table class="geotk" border="1">
         *   <tr><th colspan="2">
         *     <table class="compact">
         *       <tr><td><b>Name:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Transverse Mercator (South Orientated)}</td></tr>
         *       <tr><td><b>Alias:</b></td><td class="onright">{@code Geotk}:</td><td class="onleft">{@code Transverse Mercator projection}</td></tr>
         *       <tr><td><b>Identifier:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code 9808}</td></tr>
         *     </table>
         *   </th></tr>
         *   <tr><td>
         *     <table class="compact">
         *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code semi_major}</td></tr>
         *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Semi-major axis}</td></tr>
         *     </table>
         *   </td><td>
         *     <table class="compact">
         *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
         *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
         *       <tr><td><b>Value range:</b></td><td>[0…∞) metres</td></tr>
         *     </table>
         *   </td></tr>
         *   <tr><td>
         *     <table class="compact">
         *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code semi_minor}</td></tr>
         *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Semi-minor axis}</td></tr>
         *     </table>
         *   </td><td>
         *     <table class="compact">
         *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
         *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
         *       <tr><td><b>Value range:</b></td><td>[0…∞) metres</td></tr>
         *     </table>
         *   </td></tr>
         *   <tr><td>
         *     <table class="compact">
         *       <tr><td><b>Name:</b></td><td class="onright">{@code Geotk}:</td><td class="onleft">{@code roll_longitude}</td></tr>
         *     </table>
         *   </td><td>
         *     <table class="compact">
         *       <tr><td><b>Type:</b></td><td>{@code Boolean}</td></tr>
         *       <tr><td><b>Obligation:</b></td><td>optional</td></tr>
         *       <tr><td><b>Default value:</b></td><td>false</td></tr>
         *     </table>
         *   </td></tr>
         *   <tr><td>
         *     <table class="compact">
         *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code central_meridian}</td></tr>
         *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Longitude of natural origin}</td></tr>
         *     </table>
         *   </td><td>
         *     <table class="compact">
         *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
         *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
         *       <tr><td><b>Value range:</b></td><td>[-180 … 180]°</td></tr>
         *       <tr><td><b>Default value:</b></td><td>0°</td></tr>
         *     </table>
         *   </td></tr>
         *   <tr><td>
         *     <table class="compact">
         *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code latitude_of_origin}</td></tr>
         *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Latitude of natural origin}</td></tr>
         *     </table>
         *   </td><td>
         *     <table class="compact">
         *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
         *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
         *       <tr><td><b>Value range:</b></td><td>[-90 … 90]°</td></tr>
         *       <tr><td><b>Default value:</b></td><td>0°</td></tr>
         *     </table>
         *   </td></tr>
         *   <tr><td>
         *     <table class="compact">
         *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code scale_factor}</td></tr>
         *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code Scale factor at natural origin}</td></tr>
         *     </table>
         *   </td><td>
         *     <table class="compact">
         *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
         *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
         *       <tr><td><b>Value range:</b></td><td>[0…∞)</td></tr>
         *       <tr><td><b>Default value:</b></td><td>1</td></tr>
         *     </table>
         *   </td></tr>
         *   <tr><td>
         *     <table class="compact">
         *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code false_easting}</td></tr>
         *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code False easting}</td></tr>
         *     </table>
         *   </td><td>
         *     <table class="compact">
         *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
         *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
         *       <tr><td><b>Value range:</b></td><td>(-∞ … ∞) metres</td></tr>
         *       <tr><td><b>Default value:</b></td><td>0 metres</td></tr>
         *     </table>
         *   </td></tr>
         *   <tr><td>
         *     <table class="compact">
         *       <tr><td><b>Name:</b></td><td class="onright">{@code OGC}:</td><td class="onleft">{@code false_northing}</td></tr>
         *       <tr><td><b>Alias:</b></td><td class="onright">{@code EPSG}:</td><td class="onleft">{@code False northing}</td></tr>
         *     </table>
         *   </td><td>
         *     <table class="compact">
         *       <tr><td><b>Type:</b></td><td>{@code Double}</td></tr>
         *       <tr><td><b>Obligation:</b></td><td>mandatory</td></tr>
         *       <tr><td><b>Value range:</b></td><td>(-∞ … ∞) metres</td></tr>
         *       <tr><td><b>Default value:</b></td><td>0 metres</td></tr>
         *     </table>
         *   </td></tr>
         * </table>
         */
        @SuppressWarnings("hiding")
        public static final ParameterDescriptorGroup PARAMETERS;
        static {
            final Citation[] excludes = {
                Citations.ESRI, Citations.NETCDF, Citations.GEOTIFF, Citations.PROJ4
            };
            PARAMETERS = Identifiers.createDescriptorGroup(
                new ReferenceIdentifier[] {
                    new NamedIdentifier(Citations.EPSG, "Transverse Mercator (South Orientated)"),
                    new IdentifierCode (Citations.EPSG,  9808),
                    sameNameAs(Citations.GEOTOOLKIT, TransverseMercator.PARAMETERS)
            }, excludes, new ParameterDescriptor<?>[] {
                sameParameterAs(PseudoMercator.PARAMETERS, "semi_major"),
                sameParameterAs(PseudoMercator.PARAMETERS, "semi_minor"),
                ROLL_LONGITUDE,
                sameParameterAs(PseudoMercator.PARAMETERS, "central_meridian"),
                sameParameterAs(PseudoMercator.PARAMETERS, "latitude_of_origin"),
                SCALE_FACTOR,
                sameParameterAs(PseudoMercator.PARAMETERS, "false_easting"),
                sameParameterAs(PseudoMercator.PARAMETERS, "false_northing")
            });
        }

        /**
         * Constructs a new provider.
         */
        public SouthOrientated() {
            super(PARAMETERS);
        }
    }
}
