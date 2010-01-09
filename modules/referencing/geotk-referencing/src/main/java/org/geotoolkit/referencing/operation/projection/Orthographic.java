/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2000-2010, Open Source Geospatial Foundation (OSGeo)
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
 */
package org.geotoolkit.referencing.operation.projection;

import static java.lang.Math.*;
import java.awt.geom.AffineTransform;

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;

import org.geotoolkit.lang.Immutable;
import org.geotoolkit.resources.Errors;


/**
 * Orthographic Projection. See the
 * <A HREF="http://mathworld.wolfram.com/OrthographicProjection.html">Orthographic projection on
 * MathWorld</A> for an overview. See any of the following providers for a list of programmatic
 * parameters:
 * <p>
 * <ul>
 *   <li>{@link org.geotoolkit.referencing.operation.provider.Orthographic}</li>
 * </ul>
 *
 * {@section Description}
 * This is a perspective azimuthal (planar) projection that is neither conformal nor equal-area.
 * It resembles a globe and only one hemisphere can be seen at a time, since it is a perspective
 * projection from infinite distance. While not useful for accurate measurements, this projection
 * is useful for pictorial views of the world. Only the spherical form is given here.
 *
 * {@section References}
 * <ul>
 *   <li>Proj-4.4.7 available at <A HREF="http://www.remotesensing.org/proj">www.remotesensing.org/proj</A>.<br>
 *       Relevant files are: {@code PJ_ortho.c}, {@code pj_fwd.c} and {@code pj_inv.c}.</li>
 *   <li>John P. Snyder (Map Projections - A Working Manual,<br>
 *       U.S. Geological Survey Professional Paper 1395, 1987)</li>
 * </ul>
 *
 * @author Rueben Schulz (UBC)
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.00
 *
 * @since 2.0
 * @module
 */
@Immutable
public class Orthographic extends UnitaryProjection {
    /**
     * For compatibility with different versions during deserialization.
     */
    private static final long serialVersionUID = 5036668705538661686L;

    /**
     * Maximum difference allowed when comparing real numbers.
     */
    private static final double EPSILON = 1E-6;

    /**
     * 0 if equatorial, 1 if polar, any other value if oblique. In the equatorial case,
     * {@link #latitudeOfOrigin} is zero, {@link #sinphi0} is zero and {@link #cosphi0}
     * is one.
     */
    private final byte type;

    /**
     * The latitude of origin, in radians.
     */
    private final double latitudeOfOrigin;

    /**
     * The sine of the {@link #latitudeOfOrigin}.
     */
    private final double sinphi0;

    /**
     * The cosine of the {@link #latitudeOfOrigin}.
     */
    private final double cosphi0;

    /**
     * Creates an Orthographic projection from the given parameters. The descriptor argument is
     * usually {@link org.geotoolkit.referencing.operation.provider.Orthographic#PARAMETERS}, but
     * is not restricted to. If a different descriptor is supplied, it is user's responsability
     * to ensure that it is suitable to an Orthographic projection.
     *
     * @param  descriptor Typically {@code Orthographic.PARAMETERS}.
     * @param  values The parameter values of the projection to create.
     * @return The map projection.
     *
     * @since 3.00
     */
    public static MathTransform2D create(final ParameterDescriptorGroup descriptor,
                                         final ParameterValueGroup values)
    {
        final Parameters parameters = new Parameters(descriptor, values);
        final Orthographic projection = new Orthographic(parameters);
        return projection.createConcatenatedTransform();
    }

    /**
     * Constructs a new map projection from the supplied parameters.
     *
     * @param parameters The parameters of the projection to be created.
     */
    protected Orthographic(final Parameters parameters) {
        super(parameters);
        double latitudeOfOrigin = toRadians(parameters.latitudeOfOrigin);
        boolean north=false, south=false;
        /*
         * Detect the special cases (equtorial or polar). In the polar case, we use the
         * same formulas for the North pole than the ones for the South pole, with only
         * the sign of y reversed.
         */
        if (abs(abs(latitudeOfOrigin) - PI/2) <= ANGLE_TOLERANCE) {
            // Polar case. The latitude of origin must be set to a positive value even for the
            // South case because the "normalize" affine transform will reverse the sign of phi.
            if (latitudeOfOrigin >= 0) {
                north = true;
            } else {
                south = true;
            }
            latitudeOfOrigin = PI/2;
            type = 1;
        } else if (latitudeOfOrigin == 0) {
            type = 0; // Equatorial case
        } else {
            type = 2; // Oblique case.
        }
        this.latitudeOfOrigin = latitudeOfOrigin;
        sinphi0 = sin(latitudeOfOrigin);
        cosphi0 = cos(latitudeOfOrigin);
        /*
         * At this point, all parameters have been processed. Now process to their
         * validation and the initialization of (de)normalize affine transforms.
         */
        if (south) {
            parameters.normalize(true).scale(1, -1);
        }
        parameters.validate();
        final AffineTransform denormalize = parameters.normalize(false);
        if (!parameters.isSpherical()) {
            /*
             * In principle the elliptical case is not supported. If nevertheless the user gave
             * an ellipsoid, use the same Earth radius than the one computed in Equirectangular.
             */
            double p = sin(abs(latitudeOfOrigin));
            p = sqrt(1 - excentricitySquared) / (1 - (p*p)*excentricitySquared);
            denormalize.scale(p, p);
        }
        if (north) {
            denormalize.scale(1, -1);
        }
        finish();
    }

    /**
     * Returns {@code true} since this projection is implemented using spherical formulas.
     */
    @Override
    boolean isSpherical() {
        return true;
    }

    /**
     * Returns the parameter descriptors for this unitary projection. Note that
     * the returned descriptor is about the unitary projection, not the full one.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return org.geotoolkit.referencing.operation.provider.Orthographic.PARAMETERS;
    }

    /**
     * Transforms the specified (<var>&lambda;</var>,<var>&phi;</var>) coordinates
     * (units in radians) and stores the result in {@code dstPts} (linear distance
     * on a unit sphere).
     */
    @Override
    protected void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff)
            throws ProjectionException
    {
        final double lam = rollLongitude(srcPts[srcOff]);
        final double phi = srcPts[srcOff + 1];
        final double cosphi = cos(phi);
        final double coslam = cos(lam);
        final double threshold, y;
        switch (type) {
            default: { // Oblique
                final double sinphi = sin(phi);
                threshold = sinphi0*sinphi + cosphi0*cosphi*coslam;
                y = cosphi0*sinphi - sinphi0*cosphi*coslam;
                break;
            }
            case 0: { // Equatorial
                threshold = cosphi * coslam;
                y = sin(phi);
                break;
            }
            case 1: { // Polar (South case, applicable to North because of (de)normalize transforms)
                threshold = phi;
                y = cosphi * coslam;
                break;
            }
        }
        if (threshold < -EPSILON) {
            throw new ProjectionException(Errors.Keys.POINT_OUTSIDE_HEMISPHERE);
        }
        dstPts[dstOff  ] = cosphi * sin(lam);
        dstPts[dstOff+1] = y;
    }

    /**
     * Transforms the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code dstPts} (angles in radians).
     */
    @Override
    protected void inverseTransform(double[] srcPts, int srcOff, double[] dstPts, int dstOff)
            throws ProjectionException
    {
        double x = srcPts[srcOff  ];
        double y = srcPts[srcOff+1];
        final double rho = hypot(x, y);
        double sinc = rho;
        if (sinc > 1) {
            if (sinc - 1 > ANGLE_TOLERANCE) {
                throw new ProjectionException(Errors.Keys.POINT_OUTSIDE_HEMISPHERE);
            }
            sinc = 1;
        }
        double phi;
        if (rho <= EPSILON) {
            phi = latitudeOfOrigin;
            x = 0;
        } else {
            if (type != 1) {
                final double cosc = sqrt(1 - sinc * sinc);
                if (type != 0) {
                    // Oblique case
                    phi = (cosc * sinphi0) + (y * sinc * cosphi0 / rho);
                    x  *= sinc * cosphi0;
                    y   = (cosc - sinphi0 * phi) * rho; // equivalent to part of (20-15)
                } else {
                    // Equatorial case
                    phi = y * sinc / rho;
                    x  *= sinc;
                    y   = cosc * rho;
                }
                phi = (abs(phi) >= 1) ? copySign(PI/2, phi) : asin(phi);
            } else {
                // South pole case, applicable to North case because of (de)normalize transforms.
                phi = acos(sinc); // equivalent to asin(cos(c)) over the range [0:1]
            }
            x = atan2(x, y);
        }
        dstPts[dstOff  ] = unrollLongitude(x);
        dstPts[dstOff+1] = phi;
    }

    /**
     * Compares the given object with this transform for equivalence.
     */
    @Override
    public boolean equivalent(final MathTransform object, final boolean strict) {
        if (super.equivalent(object, strict)) {
            final Orthographic that = (Orthographic) object;
            return equals(latitudeOfOrigin, that.latitudeOfOrigin, strict);
            // All other fields are derived from the latitude of origin.
        }
        return false;
    }
}
