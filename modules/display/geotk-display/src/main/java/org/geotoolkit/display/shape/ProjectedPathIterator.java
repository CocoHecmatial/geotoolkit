/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.display.shape;

import java.awt.geom.Point2D;
import java.awt.geom.PathIterator;
import java.awt.geom.IllegalPathStateException;

import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

import static java.lang.Math.*;
import static org.geotoolkit.math.XMath.SQRT2;


/**
 * A path iterator which applies a map projection on the fly. This iterator extends
 * {@link Point2D.Double} for opportunist reasons only - users should not rely on this
 * implementation details.
 * <p>
 * The point inherited by this class is the location of the last point of the last
 * call to a {@code currentSegment(...)} method. This location has been transformed
 * by the {@linkplain #projection}.
 *
 * @author Rémi Maréchal (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.20
 *
 * @since 3.20
 * @module
 */
@SuppressWarnings("serial")
final class ProjectedPathIterator extends Point2D.Double implements PathIterator {
    /**
     * Tolerance factor for determining when to replace a curve by a straight line.
     */
    private static final double TOLERANCE = 1E-2;

    /**
     * The original path iterator.
     */
    private final PathIterator iterator;

    /**
     * The transform to apply on the values returned by the path iterator.
     * This transform should be the concatenation of the map projection
     * given to the {@link ProjectedShape} constructor, together with the
     * affine transform given to the {@code getPathIterator(...)} method.
     */
    private final MathTransform2D projection;

    /**
     * The original ordinate values of the last iteration step, before transformation.
     * The values of the last iteration steps after transformation are (x,y).
     */
    private transient double λ, φ;

    /**
     * Derivative of the previous iteration step, or {@code null} if none.
     */
    private transient Matrix derivative;

    /**
     * Control points computed by {@link #transformSegment}.
     */
    private transient double ctrlx1, ctrly1, ctrlx2, ctrly2;

    /**
     * Creates a new path iterator which will apply the given transform on the
     * points returned by the given iterator.
     */
    ProjectedPathIterator(final PathIterator iterator, final MathTransform2D projection) {
        this.iterator   = iterator;
        this.projection = projection;
    }

    /**
     * Returns the winding rule specified by the original iterator.
     */
    @Override
    public int getWindingRule() {
        return iterator.getWindingRule();
    }

    /**
     * Returns {@code true} if there is no more points to iterate over.
     */
    @Override
    public boolean isDone() {
        return iterator.isDone();
    }

    /**
     * Moves the iterator to the next segment.
     */
    @Override
    public void next() {
        iterator.next();
    }

    /**
     * Transforms the given number of points in the given array. The last points is stored in the
     * (λ,φ) and (x,y) fields of this iterator, in order to be reused during the next iteration.
     */
    private void transform(final double[] coords, final int n) throws TransformException {
        final int i0 = (n-1) << 1;
        final int i1 = i0 + 1;
        λ = coords[i0];
        φ = coords[i1];
        projection.transform(coords, 0, coords, 0, n);
        x = coords[i0];
        y = coords[i1];
        derivative = null;
    }

    /**
     * Same method than above, but for floating point values. The last point is transformed
     * in a special way in order to keep the {@code double} precision.
     */
    private void transform(final float[] coords, int n) throws TransformException {
        if (--n != 0) {
            projection.transform(coords, 0, coords, 0, n);
        }
        final int i0 = n << 1;
        final int i1 = i0 + 1;
        x = λ = coords[i0];
        y = φ = coords[i1];
        projection.transform(this, this);
        coords[i0] = (float) x;
        coords[i1] = (float) y;
        derivative = null;
    }

    /**
     * Returns the coordinates and type of the current path segment in the iteration.
     * This method handles line segments in a special way, by attempting to replace
     * them by a curve computed from the derivative at the two end points.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public int currentSegment(final double[] coords) {
        int type = iterator.currentSegment(coords);
        try {
            switch (type) {
                case SEG_CLOSE:                         break;
                case SEG_MOVETO:  transform(coords, 1); break;
                case SEG_QUADTO:  transform(coords, 2); break;
                case SEG_CUBICTO: transform(coords, 3); break;
                case SEG_LINETO: {
                    final double x1 = x;
                    final double y1 = y;
                    final double Δλ = coords[0] - λ;
                    final double Δφ = coords[1] - φ;
                    if (derivative == null) {
                        x = λ;
                        y = φ;
                        derivative = projection.derivative(this);
                    }
                    final Matrix D1 = derivative;
                    x = λ = coords[0];
                    y = φ = coords[1];
                    derivative = projection.derivative(this);
                    projection.transform(this, this);
                    type = transformSegment(Δλ, Δφ, x1, y1, D1);
                    int i=0;
                    switch (type) {
                        case SEG_CUBICTO: coords[i++] = ctrlx1; coords[i++] = ctrly1; // Fallthrough
                        case SEG_QUADTO:  coords[i++] = ctrlx2; coords[i++] = ctrly2; // Fallthrough
                        case SEG_LINETO:  coords[i++] =     x;  coords[i++] =     y;
                    }
                    break;
                }
            }
        } catch (TransformException cause) {
            IllegalPathStateException ex = new IllegalPathStateException(cause.getLocalizedMessage());
            ex.initCause(cause);
            throw ex;
        }
        return type;
    }

    /**
     * A copy of the above method, adapted for {@code float} arrays.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public int currentSegment(final float[] coords) {
        // Following block is for debugging purpose only. Set the condition to 'true' in order
        // to delegates to the method using full 'double' precision. This is sometime useful
        // for making sure that the results are the same.
        if (false) {
            final double[] buffer = new double[min(coords.length, 6)];
            final int type = currentSegment(buffer);
            for (int i=0; i<buffer.length; i++) {
                coords[i] = (float) buffer[i];
            }
            return type;
        }
        // Copy of currentSegment(double[]). Note that this copy contains many implicit casts
        // from 'float' to 'double'. So while the source code looks close to identical to the
        // above method, the compiled code is actually not the same. The part which is really
        // the same has been factorized out in the transformSegment(...) method.
        int type = iterator.currentSegment(coords);
        try {
            switch (type) {
                case SEG_CLOSE:                         break;
                case SEG_MOVETO:  transform(coords, 1); break;
                case SEG_QUADTO:  transform(coords, 2); break;
                case SEG_CUBICTO: transform(coords, 3); break;
                case SEG_LINETO: {
                    final double x1 = x;
                    final double y1 = y;
                    final double Δλ = coords[0] - λ;
                    final double Δφ = coords[1] - φ;
                    if (derivative == null) {
                        x = λ;
                        y = φ;
                        derivative = projection.derivative(this);
                    }
                    final Matrix D1 = derivative;
                    x = λ = coords[0];
                    y = φ = coords[1];
                    derivative = projection.derivative(this);
                    projection.transform(this, this);
                    type = transformSegment(Δλ, Δφ, x1, y1, D1);
                    int i=0;
                    switch (type) {
                        case SEG_CUBICTO: coords[i++] = (float) ctrlx1; coords[i++] = (float) ctrly1; // Fallthrough
                        case SEG_QUADTO:  coords[i++] = (float) ctrlx2; coords[i++] = (float) ctrly2; // Fallthrough
                        case SEG_LINETO:  coords[i++] = (float)     x;  coords[i++] = (float)     y;
                    }
                    break;
                }
            }
        } catch (TransformException cause) {
            IllegalPathStateException ex = new IllegalPathStateException(cause.getLocalizedMessage());
            ex.initCause(cause);
            throw ex;
        }
        return type;
    }

    /**
     * Transforms a line segment from P1 to P2, which may result in a quadratic or cubic curve.
     * <p>
     * <b>Notation:</b>
     * <ul>
     *   <li>The (λ,φ) variable names are ordinates in the source space.</li>
     *   <li>The (x,y) variable names are ordinates in the target space.</li>
     *   <li>The 1 and 2 suffixes denote the starting and ending points respectively.</li>
     *   <li>The Δ prefix denote the difference between the target and the source points.</li>
     * </ul>
     */
    private int transformSegment(
            final double Δλ, final double Δφ,
            final double x1, final double y1, final Matrix D1)
    {
        final double x2 = x;
        final double y2 = y;
        final Matrix D2 = derivative;

        // Unitary vectors from the matrix at the starting point.
        double dx1_dλ = D1.getElement(0, 0);
        double dy1_dλ = D1.getElement(1, 0);
        double dx1_dφ = D1.getElement(0, 1);
        double dy1_dφ = D1.getElement(1, 1);
        double Lλ = hypot(dx1_dλ, dy1_dλ);
        double Lφ = hypot(dx1_dφ, dy1_dφ);
        dx1_dλ /= Lλ;
        dy1_dλ /= Lλ;
        dx1_dφ /= Lφ;
        dy1_dφ /= Lφ;

        // Unitary vectors from the matrix at the ending point.
        double dx2_dλ = D2.getElement(0, 0);
        double dy2_dλ = D2.getElement(1, 0);
        double dx2_dφ = D2.getElement(0, 1);
        double dy2_dφ = D2.getElement(1, 1);
        Lλ = hypot(dx2_dλ, dy2_dλ);
        Lφ = hypot(dx2_dφ, dy2_dφ);
        dx2_dλ /= Lλ;
        dy2_dλ /= Lλ;
        dx2_dφ /= Lφ;
        dy2_dφ /= Lφ;

        // (Δx,Δy) is the vector from P1 to P2 in the (x,y) space.
        final double Δx = (x2 - x1);
        final double Δy = (y2 - y1);
        final double L12 = hypot(Δx, Δy);

        // (Δx1,Δy1) is the (Δx,Δy) vector that we would have if the derivative was D1 everywhere.
        // (Δx2,Δy2) is the (Δx,Δy) vector that we would have if the derivative was D2 everywhere.
        final double Δx1 = (dx1_dλ*Δλ + dx1_dφ*Δφ);
        final double Δy1 = (dy1_dλ*Δλ + dy1_dφ*Δφ);
        final double Δx2 = (dx2_dλ*Δλ + dx2_dφ*Δφ);
        final double Δy2 = (dy2_dλ*Δλ + dy2_dφ*Δφ);
        final double L1  = hypot(Δx1, Δy1);
        final double L2  = hypot(Δx2, Δy2);
        final double s1  = (Δx*Δx1 + Δy*Δy1) / (L12*L1);
        final double s2  = (Δx*Δx2 + Δy*Δy2) / (L12*L2);
        if (!(abs(s1-s2) > TOLERANCE)) { // Use ! for catching NaN.
            if (!(abs(s1-1) > TOLERANCE)) {
                return SEG_LINETO;
            }
            if (s1 >= SQRT2/2) {
                /*
                 * Reference to Thomas W. Sederberg article : COMPUTER AIDED GEOMETRIC DESIGN
                 * Computes α as a weight coefficient of the delta to add to the starting point.
                 */
                final double α = abs((Δy2*Δx - Δx2*Δy) / (Δy2*Δx1 - Δx2*Δy1));
                ctrlx2 = x1 + Δx1*α;
                ctrly2 = y1 + Δy1*α;
                return SEG_QUADTO;
            }
        }
        final double α = L12 / (3 * L1);
        final double β = L12 / (3 * L2);
        ctrlx1 = x1 + Δx1*α;
        ctrly1 = y1 + Δy1*α;
        ctrlx2 = x2 - Δx2*β;
        ctrly2 = y2 - Δy2*β;
        return SEG_CUBICTO;
    }
}
