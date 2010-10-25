/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2001-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2010, Geomatys
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
 *
 *    This package contains formulas from the PROJ package of USGS.
 *    USGS's work is fully acknowledged here. This derived work has
 *    been relicensed under LGPL with Frank Warmerdam's permission.
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package org.geotoolkit.referencing.operation.transform;

import java.util.Arrays;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import javax.measure.converter.UnitConverter;

import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.geometry.DirectPosition;

import org.geotoolkit.lang.Immutable;
import org.geotoolkit.util.Utilities;
import org.geotoolkit.resources.Errors;
import org.geotoolkit.parameter.Parameter;
import org.geotoolkit.parameter.FloatParameter;
import org.geotoolkit.parameter.ParameterGroup;
import org.geotoolkit.geometry.GeneralDirectPosition;
import org.geotoolkit.referencing.operation.matrix.Matrix3;
import org.geotoolkit.referencing.operation.matrix.GeneralMatrix;
import org.geotoolkit.referencing.operation.provider.EllipsoidToGeocentric;
import org.geotoolkit.referencing.operation.provider.GeocentricToEllipsoid;

import static java.lang.Math.*;
import static java.lang.Double.doubleToLongBits;
import static org.geotoolkit.internal.referencing.MatrixUtilities.invert;


/**
 * Transforms three dimensional {@linkplain org.geotoolkit.referencing.crs.DefaultGeographicCRS
 * geographic} points to {@linkplain org.geotoolkit.referencing.crs.DefaultGeocentricCRS geocentric}
 * coordinate points. Input points must be longitudes, latitudes and heights above the ellipsoid.
 * <p>
 * See any of the following providers for a list of programmatic parameters:
 * <p>
 * <ul>
 *   <li>{@link org.geotoolkit.referencing.operation.provider.EllipsoidToGeocentric}</li>
 *   <li>{@link org.geotoolkit.referencing.operation.provider.GeocentricToEllipsoid}</li>
 * </ul>
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @author PROJ4 Project for formulas
 * @version 3.16
 *
 * @since 1.2
 * @module
 */
@Immutable
public class GeocentricTransform extends AbstractMathTransform implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3352045463953828140L;

    /**
     * Maximal error tolerance in metres during assertions. If assertions are enabled, then every
     * coordinates transformed with {@link #inverseTransform} will be transformed again with
     * {@link #mathTransform}. If the distance between the resulting position and the original
     * position is greater than {@code MAX_ERROR}, then a {@link AssertionError} is thrown.
     */
    private static final double MAX_ERROR = 0.01;

    /**
     * Cosine of 67.5 decimal degrees.
     */
    private static final double COS_67P5 = 0.38268343236508977;

    /**
     * Toms region 1 constant.
     */
    private static final double AD_C = 1.0026000;

    /**
     * Semi-major axis of ellipsoid in meters.
     */
    private final double a;

    /**
     * Semi-minor axis of ellipsoid in meters.
     */
    private final double b;

    /**
     * Square of semi-major axis (<var>a</var>²).
     */
    private final double a2;

    /**
     * Square of semi-minor axis (<var>b</var>²).
     */
    private final double b2;

    /**
     * Eccentricity squared.
     */
    private final double e2;

    /**
     * 2nd eccentricity squared.
     */
    private final double ep2;

    /**
     * {@code true} if geographic coordinates include an ellipsoidal
     * height (i.e. are 3-D), or {@code false} if they are strictly 2-D.
     */
    final boolean hasHeight;

    /**
     * The inverse of this transform. Will be created only when needed.
     */
    private transient MathTransform inverse;

    /**
     * Constructs a transform from the specified parameters.
     * <p>
     * <strong>WARNING:</strong> Current implementation expects longitude and latitude ordinates
     * in decimal degrees, but it may be changed to radians in a future version. The static factory
     * method will preserve the decimal degrees contract.
     *
     * @param semiMajor The semi-major axis length.
     * @param semiMinor The semi-minor axis length.
     * @param units     The axis units.
     * @param hasHeight {@code true} if geographic coordinates include an ellipsoidal
     *        height (i.e. are 3-D), or {@code false} if they are only 2-D.
     *
     * @see #create(double, double, Unit, boolean)
     */
    protected GeocentricTransform(final double  semiMajor,
                                  final double  semiMinor,
                                  final Unit<Length> units,
                                  final boolean hasHeight)
    {
        this.hasHeight = hasHeight;
        final UnitConverter converter = units.getConverterTo(SI.METRE);
        a   = converter.convert(semiMajor);
        b   = converter.convert(semiMinor);
        a2  = a * a;
        b2  = b * b;
        e2  = (a2 - b2) / a2;
        ep2 = (a2 - b2) / b2;
        checkArgument("a", a, Double.MAX_VALUE);
        checkArgument("b", b, a);
    }

    /**
     * Constructs a transform from the specified ellipsoid. The returned transform expects
     * (<var>longitude</var>, <var>latitude</var>, <var>height</var>) source coordinates
     * where the longitudes and latitudes are in <em>decimal degrees</em>, and the height
     * is optional (depending on the value of the {@code hasHeight} argument).
     *
     * @param ellipsoid The ellipsoid.
     * @param hasHeight {@code true} if geographic coordinates include an ellipsoidal
     *        height (i.e. are 3-D), or {@code false} if they are only 2-D.
     * @return The transform from geographic to geocentric coordinates.
     *
     * @since 3.15
     */
    public static MathTransform create(final Ellipsoid ellipsoid, final boolean hasHeight) {
        return create(ellipsoid.getSemiMajorAxis(),
                      ellipsoid.getSemiMinorAxis(),
                      ellipsoid.getAxisUnit(), hasHeight);
    }

    /**
     * Constructs a transform from the specified parameters. The returned transform expects
     * (<var>longitude</var>, <var>latitude</var>, <var>height</var>) source coordinates
     * where the longitudes and latitudes are in <em>decimal degrees</em>, and the height
     * is optional (depending on the value of the {@code hasHeight} argument).
     *
     * @param  semiMajor The semi-major axis length.
     * @param  semiMinor The semi-minor axis length.
     * @param  units     The axis units.
     * @param  hasHeight {@code true} if geographic coordinates include an ellipsoidal
     *         height (i.e. are 3-D), or {@code false} if they are only 2-D.
     * @return The transform from geographic to geocentric coordinates.
     *
     * @since 3.15
     */
    public static MathTransform create(final double semiMajor,
                                       final double semiMinor,
                                       final Unit<Length> units,
                                       final boolean hasHeight)
    {
        return new GeocentricTransform(semiMajor, semiMinor, units, hasHeight);
    }

    /**
     * Checks an argument value. The argument must be greater
     * than 0 and finite, otherwise an exception is thrown.
     *
     * @param name  The argument name.
     * @param value The argument value.
     * @param max   The maximal legal argument value.
     */
    private static void checkArgument(final String name, final double value, final double max)
            throws IllegalArgumentException
    {
        if (!(value >= 0 && value <= max)) {
            // Use '!' in order to trap NaN
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ILLEGAL_ARGUMENT_$2, name, value));
        }
    }

    /**
     * Returns the parameter descriptors for this math transform.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return EllipsoidToGeocentric.PARAMETERS;
    }

    /**
     * Returns the parameter values for this math transform.
     *
     * @return A copy of the parameter values for this math transform.
     */
    @Override
    public ParameterValueGroup getParameterValues() {
        return getParameterValues(getParameterDescriptors());
    }

    /**
     * Returns the parameter values using the specified descriptor.
     *
     * @param  descriptor The parameter descriptor.
     * @return A copy of the parameter values for this math transform.
     */
    private ParameterValueGroup getParameterValues(final ParameterDescriptorGroup descriptor) {
        final ParameterValue<?>[] parameters = new ParameterValue<?>[hasHeight ? 2 : 3];
        int index = 0;
        if (!hasHeight) {
            final ParameterValue<Integer> p = new Parameter<Integer>(EllipsoidToGeocentric.DIM);
            p.setValue(2);
            parameters[index++] = p;
        }
        parameters[index++] = new FloatParameter(EllipsoidToGeocentric.SEMI_MAJOR, a);
        parameters[index++] = new FloatParameter(EllipsoidToGeocentric.SEMI_MINOR, b);
        return new ParameterGroup(descriptor, parameters);
    }

    /**
     * Gets the dimension of input points, which is 2 or 3.
     */
    @Override
    public final int getSourceDimensions() {
        return hasHeight ? 3 : 2;
    }

    /**
     * Gets the dimension of output points, which is 3.
     */
    @Override
    public final int getTargetDimensions() {
        return 3;
    }

    /**
     * Converts geodetic coordinates (longitude, latitude, height) to geocentric
     * coordinates (x, y, z) according to the current ellipsoid parameters.
     */
    @Override
    protected void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff) {
        transform(srcPts, srcOff, dstPts, dstOff, 1, hasHeight);
    }

    /**
     * Converts geodetic coordinates (longitude, latitude, height) to geocentric
     * coordinates (x, y, z) according to the current ellipsoid parameters.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
        transform(srcPts, srcOff, dstPts, dstOff, numPts, hasHeight);
    }

    /**
     * Implementation of geodetic to geocentric conversion. This implementation allows the caller
     * to force usage of height in computation. The usual value for {@code hasHeight} argument is
     * the {@link #hasHeight} field. Passing {@code true} forces this method to use height ordinates
     * even if this transform is for two-dimensional input. This is used for assertion with
     * {@link #checkTransform}.
     */
    private void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts,
                           final boolean hasHeight)
    {
        final int dimSource = hasHeight ? 3 : 2; // May not be the same than getSourceDimension().
        int srcInc = 0;
        int dstInc = 0;
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, dimSource, dstOff, 3, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * dimSource;
                    dstOff += (numPts - 1) * 3; // Target dimension is fixed to 3.
                    srcInc = -2 * dimSource;
                    dstInc = -6;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*dimSource);
                    srcOff = 0;
                    break;
                }
            }
        }
        while (--numPts >= 0) {
            final double λ = toRadians(srcPts[srcOff++]);      // Longitude
            final double φ = toRadians(srcPts[srcOff++]);      // Latitude
            final double h = hasHeight ? srcPts[srcOff++] : 0; // Height above the ellipsoid (m)

            final double cosφ = cos(φ);
            final double sinφ = sin(φ);
            final double rn = a / sqrt(1 - e2 * (sinφ*sinφ));
            final double rcosφ = (rn + h) * cosφ;

            dstPts[dstOff++] = rcosφ * cos(λ);            // X: Toward prime meridian
            dstPts[dstOff++] = rcosφ * sin(λ);            // Y: Toward East
            dstPts[dstOff++] = (rn * (1-e2) + h) * sinφ;  // Z: Toward North

            srcOff += srcInc;
            dstOff += dstInc;
        }
    }

    /**
     * Converts geodetic coordinates (longitude, latitude, height) to geocentric
     * coordinates (x, y, z) according to the current ellipsoid parameters.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) {
        final int dimSource = getSourceDimensions();
        int srcInc = 0;
        int dstInc = 0;
        if (srcPts == dstPts) {
            switch (IterationStrategy.suggest(srcOff, dimSource, dstOff, 3, numPts)) {
                case ASCENDING: {
                    break;
                }
                case DESCENDING: {
                    srcOff += (numPts - 1) * dimSource;
                    dstOff += (numPts - 1) * 3; // Target dimension is fixed to 3.
                    srcInc = -2 * dimSource;
                    dstInc = -6;
                    break;
                }
                default: {
                    srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + numPts*dimSource);
                    srcOff = 0;
                    break;
                }
            }
        }
        while (--numPts >= 0) {
            final double λ = toRadians(srcPts[srcOff++]);       // Longitude
            final double φ = toRadians(srcPts[srcOff++]);       // Latitude
            final double h = hasHeight ? srcPts[srcOff++] : 0;  // Height above the ellipsoid (m)

            final double cosφ = cos(φ);
            final double sinφ = sin(φ);
            final double rn = a / sqrt(1 - e2 * (sinφ*sinφ));
            final double rcosφ = (rn + h) * cosφ;

            dstPts[dstOff++] = (float) (rcosφ * cos(λ));           // X: Toward prime meridian
            dstPts[dstOff++] = (float) (rcosφ * sin(λ));           // Y: Toward East
            dstPts[dstOff++] = (float) ((rn * (1-e2) + h) * sinφ); // Z: Toward North

            srcOff += srcInc;
            dstOff += dstInc;
        }
    }

    /**
     * Converts geodetic coordinates (longitude, latitude, height) to geocentric
     * coordinates (x, y, z) according to the current ellipsoid parameters.
     */
    @Override
    public void transform(float[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            final double λ = toRadians(srcPts[srcOff++]);       // Longitude
            final double φ = toRadians(srcPts[srcOff++]);       // Latitude
            final double h = hasHeight ? srcPts[srcOff++] : 0;  // Height above the ellipsoid (m)

            final double cosφ = cos(φ);
            final double sinφ = sin(φ);
            final double rn = a / sqrt(1 - e2 * (sinφ*sinφ));
            final double rcosφ = (rn + h) * cosφ;

            dstPts[dstOff++] = rcosφ * cos(λ);           // X: Toward prime meridian
            dstPts[dstOff++] = rcosφ * sin(λ);           // Y: Toward East
            dstPts[dstOff++] = (rn * (1-e2) + h) * sinφ; // Z: Toward North
        }
    }

    /**
     * Converts geodetic coordinates (longitude, latitude, height) to geocentric
     * coordinates (x, y, z) according to the current ellipsoid parameters.
     */
    @Override
    public void transform(double[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) {
        while (--numPts >= 0) {
            final double λ = toRadians(srcPts[srcOff++]);       // Longitude
            final double φ = toRadians(srcPts[srcOff++]);       // Latitude
            final double h = hasHeight ? srcPts[srcOff++] : 0;  // Height above the ellipsoid (m)

            final double cosφ = cos(φ);
            final double sinφ = sin(φ);
            final double rn = a / sqrt(1 - e2 * (sinφ*sinφ));
            final double rcosφ = (rn + h) * cosφ;

            dstPts[dstOff++] = (float) (rcosφ * cos(λ));           // X: Toward prime meridian
            dstPts[dstOff++] = (float) (rcosφ * sin(λ));           // Y: Toward East
            dstPts[dstOff++] = (float) ((rn * (1-e2) + h) * sinφ); // Z: Toward North
        }
    }

    /**
     * Converts geocentric coordinates (<var>x</var>, <var>y</var>, <var>z</var>) to geodetic
     * coordinates (<var>longitude</var>, <var>latitude</var>, <var>height</var>), according
     * to the current ellipsoid parameters. The method used here is derived from "<cite>An
     * Improved Algorithm for Geocentric to Geodetic Coordinate Conversion</cite>", by Ralph
     * Toms, Feb 1996.
     *
     * @param srcPts1 The array containing the source point coordinates in simple precision.
     * @param srcPts2 Same than {@code srcPts1} but in double precision. Exactly one of
     *                {@code srcPts1} or {@code srcPts2} can be non-null.
     * @param srcOff  The offset to the first point to be transformed in the source array.
     * @param dstPts1 The array into which the transformed point coordinates are returned.
     *                May be the same than {@code srcPts1} or {@code srcPts2}.
     * @param dstPts2 Same than {@code dstPts1} but in double precision. Exactly one of
     *                {@code dstPts1} or {@code dstPts2} can be non-null.
     * @param dstOff  The offset to the location of the first transformed point that is stored
     *                in the destination array.
     * @param numPts  The number of point objects to be transformed.
     * @param descending {@code true} if points should be iterated in descending order.
     */
    final void inverseTransform(final float[] srcPts1, final double[] srcPts2, int srcOff,
                                final float[] dstPts1, final double[] dstPts2, int dstOff,
                                int numPts, final boolean hasHeight, final boolean descending)
    {
        boolean computeHeight = hasHeight;
        assert (computeHeight=true) == true; // Force computeHeight to true if assertions are enabled.
        if (descending) {
            final int n = (numPts - 1) * 3;
            srcOff += n;
            dstOff += n;
            if (!computeHeight) {
                dstOff -= (numPts - 1);
            }
        }
        while (--numPts >= 0) {
            final double x, y, z;
            if (srcPts2 != null) {
                x = srcPts2[srcOff++]; // Toward prime meridian
                y = srcPts2[srcOff++]; // Toward East
                z = srcPts2[srcOff++]; // Toward North
            } else {
                x = srcPts1[srcOff++]; // Toward prime meridian
                y = srcPts1[srcOff++]; // Toward East
                z = srcPts1[srcOff++]; // Toward North
            }
            // Note: The Java version of 'atan2' work correctly for x==0.
            //       No need for special handling like in the C version.
            //       No special handling neither for latitude. Formulas
            //       below are generic enough, considering that 'atan'
            //       work correctly with infinities (1/0).

            // Note: Variable names follow the notation used in Toms, Feb 1996
            final double      W  = hypot(x, y);                     // distance from Z axis
            final double      T0 = z * AD_C;                        // initial estimate of vertical component
            final double      S0 = hypot(T0, W);                    // initial estimate of horizontal component
            final double  sin_B0 = T0 / S0;                         // sin(B0), B0 is estimate of Bowring aux variable
            final double  cos_B0 = W / S0;                          // cos(B0)
            final double sin3_B0 = sin_B0 * sin_B0 * sin_B0;        // cube of sin(B0)
            final double      T1 = z + b * ep2 * sin3_B0;           // corrected estimate of vertical component
            final double     sum = W - a*e2*(cos_B0*cos_B0*cos_B0); // numerator of cos(phi1)
            final double      S1 = hypot(T1, sum);                  // corrected estimate of horizontal component
            final double  sin_p1 = T1 / S1;                         // sin(phi1), phi1 is estimated latitude
            final double  cos_p1 = sum / S1;                        // cos(phi1)

            final double longitude = toDegrees(atan2(y     , x     ));
            final double  latitude = toDegrees(atan(sin_p1 / cos_p1));
            final double    height;

            if (dstPts2 != null) {
                dstPts2[dstOff++] = longitude;
                dstPts2[dstOff++] = latitude;
            } else {
                dstPts1[dstOff++] = (float) longitude;
                dstPts1[dstOff++] = (float) latitude;
            }
            if (computeHeight) {
                final double rn = a / sqrt(1 - e2*(sin_p1*sin_p1)); // Earth radius at location
                if      (cos_p1 >= +COS_67P5) height = W / +cos_p1 - rn;
                else if (cos_p1 <= -COS_67P5) height = W / -cos_p1 - rn;
                else                          height = z / sin_p1 + rn*(e2 - 1.0);
                if (hasHeight) {
                    if (dstPts2 != null) {
                        dstPts2[dstOff++] = height;
                    } else {
                        dstPts1[dstOff++] = (float) height;
                    }
                }
                // If assertion are enabled, then transform the
                // result and compare it with the input array.
                final double error;
                assert MAX_ERROR > (error = checkTransform(x,y,z, longitude, latitude, height)) : error;
            }
            if (descending) {
                srcOff -= 6;
                dstOff -= computeHeight ? 6 : 4;
            }
        }
    }

    /**
     * Transform the last half if the specified array and returns the distance
     * with the first half. Array {@code points} must have a length of 6.
     */
    private double checkTransform(final double... points) {
        transform(points, 3, points, 3, 1, true);
        final double dx = points[0] - points[3];
        final double dy = points[1] - points[4];
        final double dz = points[2] - points[5];
        return sqrt(dx*dx + dy*dy + dz*dz);
    }

    /**
     * Gets the derivative of this transform at a point.
     *
     * @param  point The coordinate point where to evaluate the derivative.
     * @return The derivative at the specified point as a 3&times;2 or 3&times;3 matrix.
     *
     * @since 3.16
     */
    @Override
    public Matrix derivative(final DirectPosition point) {
        return derivative(point, hasHeight);
    }

    /**
     * Implementation of {@link #derivative(DirectPosition)} with the possibility
     * to override the value of the {@link #hasHeight} by the given boolean value.
     */
    final Matrix derivative(final DirectPosition point, final boolean hasHeight) {
        final int dimSource = hasHeight ? 3 : 2;
        final int dimPoint = point.getDimension();
        if (dimPoint != dimSource) {
            throw new MismatchedDimensionException(constructMessage("point", dimPoint, dimSource));
        }
        final double λ = toRadians(point.getOrdinate(0));      // Longitude
        final double φ = toRadians(point.getOrdinate(1));      // Latitude
        final double h = hasHeight ? point.getOrdinate(2) : 0; // Height above the ellipsoid (m)
        final double cosλ = cos(λ);
        final double sinλ = sin(λ);
        final double cosφ = cos(φ);
        final double sinφ = sin(φ);
        final double ads  = 1 - e2 * (sinφ*sinφ);
        final double rn   = a / sqrt(ads);
        /*
         * Note: we multiply the radius by PI/180 despite (a + h) being a linear measure,
         * because we need that multiplication factor for all qualtities derivated by λ or
         * φ, since those quantities were converted from degrees to radians by this method.
         */
        final double sdλ = toRadians(rn + h);
        final double sdφ = toRadians((1-e2) * (rn/ads) + h);
        final double dXλ = -sdλ * (cosφ * sinλ);
        final double dXφ = -sdφ * (sinφ * cosλ);
        final double dYλ =  sdλ * (cosφ * cosλ);
        final double dYφ = -sdφ * (sinφ * sinλ);
        final double dZφ =  sdφ * cosφ;
        if (hasHeight) {
            return new Matrix3(dXλ, dXφ, cosφ*cosλ,
                               dYλ, dYφ, cosφ*sinλ,
                                 0, dZφ, sinφ);
        } else {
            return new GeneralMatrix(3, 2, new double[] {dXλ, dXφ, dYλ, dYφ, 0, dZφ});
        }
    }

    /**
     * Returns the inverse of this transform.
     */
    @Override
    public synchronized MathTransform inverse() {
        if (inverse == null) {
            inverse = new Inverse();
        }
        return inverse;
    }

    /**
     * Returns a hash value for this transform.
     */
    @Override
    public int hashCode() {
        final long code = doubleToLongBits( a ) +
                      31*(doubleToLongBits( b ) +
                      31*(doubleToLongBits( a2) +
                      31*(doubleToLongBits( b2) +
                      31*(doubleToLongBits( e2) +
                      31*(doubleToLongBits(ep2))))));
        int c = ((int) code) ^ (int) (code >>> 32);
        if (hasHeight) {
            c += 37;
        }
        return c ^ (int) serialVersionUID;
    }

    /**
     * Compares the specified object with this math transform for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            // Slight optimization
            return true;
        }
        if (super.equals(object)) {
            final GeocentricTransform that = (GeocentricTransform) object;
            return this.hasHeight == that.hasHeight    &&
                   Utilities.equals(this.a,   that.a)  &&
                   Utilities.equals(this.b,   that.b)  &&
                   Utilities.equals(this.a2,  that.a2) &&
                   Utilities.equals(this.b2,  that.b2) &&
                   Utilities.equals(this.e2,  that.e2) &&
                   Utilities.equals(this.ep2, that.ep2);
        }
        return false;
    }

    /**
     * Inverse of a geocentric transform. Converts geocentric coordinates (<var>x</var>,
     * <var>y</var>, <var>z</var>) to geodetic coordinates (<var>longitude</var>,
     * <var>latitude</var>, <var>height</var>), according to the current ellipsoid
     * parameters. The method used here is derived from "<cite>An Improved Algorithm
     * for Geocentric to Geodetic Coordinate Conversion</cite>", by Ralph Toms, Feb 1996.
     *
     * @author Martin Desruisseaux (IRD, Geomatys)
     * @version 3.16
     *
     * @since 2.0
     * @module
     */
    private final class Inverse extends AbstractMathTransform.Inverse implements Serializable {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 6942084702259211803L;

        /**
         * Default constructor.
         */
        public Inverse() {
            GeocentricTransform.this.super();
        }

        /**
         * Returns the parameter descriptors for this math transform.
         */
        @Override
        public ParameterDescriptorGroup getParameterDescriptors() {
            return GeocentricToEllipsoid.PARAMETERS;
        }

        /**
         * Returns the parameter values for this math transform.
         *
         * @return A copy of the parameter values for this math transform.
         */
        @Override
        public ParameterValueGroup getParameterValues() {
            return GeocentricTransform.this.getParameterValues(getParameterDescriptors());
        }

        /**
         * Inverse transform a single points.
         */
        @Override
        protected void transform(final double[] srcPts, final int srcOff,
                                 final double[] dstPts, final int dstOff)
        {
            inverseTransform(null, srcPts, srcOff, null, dstPts, dstOff, 1, hasHeight, false);
        }

        /**
         * Inverse transform an array of points.
         */
        @Override
        public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
            boolean descending = false;
            if (srcPts == dstPts) {
                switch (IterationStrategy.suggest(srcOff, getSourceDimensions(),
                                                  dstOff, getTargetDimensions(), numPts))
                {
                    case ASCENDING: {
                        break;
                    }
                    case DESCENDING: {
                        descending = true;
                        break;
                    }
                    default: {
                        srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + 3*numPts);
                        srcOff = 0;
                        break;
                    }
                }
            }
            inverseTransform(null, srcPts, srcOff, null, dstPts, dstOff, numPts, hasHeight, descending);
        }

        /**
         * Inverse transform an array of points.
         */
        @Override
        public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) {
            boolean descending = false;
            if (srcPts == dstPts) {
                switch (IterationStrategy.suggest(srcOff, getSourceDimensions(),
                                                  dstOff, getTargetDimensions(), numPts))
                {
                    case ASCENDING: {
                        break;
                    }
                    case DESCENDING: {
                        descending = true;
                        break;
                    }
                    default: {
                        srcPts = Arrays.copyOfRange(srcPts, srcOff, srcOff + 3*numPts);
                        srcOff = 0;
                        break;
                    }
                }
            }
            inverseTransform(srcPts, null, srcOff, dstPts, null, dstOff, numPts, hasHeight, descending);
        }

        /**
         * Inverse transform an array of points.
         */
        @Override
        public void transform(float [] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
            inverseTransform(srcPts, null, srcOff, null, dstPts, dstOff, numPts, hasHeight, false);
        }

        /**
         * Inverse transform an array of points.
         */
        @Override
        public void transform(double[] srcPts, int srcOff, float [] dstPts, int dstOff, int numPts) {
            inverseTransform(null, srcPts, srcOff, dstPts, null, dstOff, numPts, hasHeight, false);
        }

        /**
         * Gets the derivative of this transform at a point. If there is no height, then this
         * method drops the last row, which is the result of ellipsoidal height computation.
         * This is consistent with what the {@code transform} method does.
         *
         * @since 3.16
         */
        @Override
        public Matrix derivative(DirectPosition point) throws TransformException {
            if (hasHeight) {
                return super.derivative(point);
            }
            final double[] ordinates = point.getCoordinate();
            if (ordinates.length != 3) {
                throw new MismatchedDimensionException(constructMessage("point", ordinates.length, 3));
            }
            inverseTransform(null, ordinates, 0, null, ordinates, 0, 1, true, false);
            point = new GeneralDirectPosition(ordinates);
            Matrix m = invert(GeocentricTransform.this.derivative(point, true));
            assert m.getNumCol() == 3 && m.getNumRow() == 3;
            final double[] elements = new double[6];
            for (int i=0; i<elements.length; i++) {
                elements[i] = m.getElement(i / 3, i % 3);
            }
            m = new GeneralMatrix(2, 3, elements);
            return m;
        }

        /**
         * Restore reference to this object after deserialization.
         */
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            GeocentricTransform.this.inverse = this;
        }
    }
}
