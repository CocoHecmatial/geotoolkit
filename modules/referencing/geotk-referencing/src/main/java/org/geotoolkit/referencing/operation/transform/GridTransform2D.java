/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2011, Geomatys
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
package org.geotoolkit.referencing.operation.transform;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import org.geotoolkit.lang.Immutable;
import org.geotoolkit.resources.Errors;
import org.geotoolkit.geometry.DirectPosition2D;


/**
 * A specialization of {@link GridTransform} in the two-dimensional case. The default implementation
 * is invertible for the {@link GridType#OFFSET OFFSET}, {@link GridType#NADCON NADCON} and
 * {@link GridType#NTv2 NTv2} grid types (assuming that the offsets are small), but not for the
 * {@linkplain GridType#LOCALIZATION LOCALIZATION} type. For an invertible localization grid,
 * see the {@link org.geotoolkit.referencing.operation.builder.LocalizationGrid} builder.
 *
 * @author Rueben Schulz (UBC)
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.00
 *
 * @since 3.00
 * @module
 */
@Immutable
public class GridTransform2D extends GridTransform implements MathTransform2D {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5797129125459758798L;

    /**
     * The inverse of this math transform. Will be created only when first needed.
     */
    private transient MathTransform2D inverse;

    /**
     * Constructs a grid using the specified data.
     *
     * @param type   Whatever the grid values are directly the target coordinates or offsets
     *               to apply on source coordinates.
     * @param grid   The grid of values. It must complies with the conditions documented in the
     *               {@link #grid} field.
     * @param size   Number of columns ({@linkplain Dimension#width width}) and rows
     *               ({@linkplain Dimension#height height}) in the grid.
     * @param area   Grid envelope in "real world" coordinates, or {@code null} if none. The
     *               minimal (<var>x</var>,<var>y</var>) coordinate will maps the (0,0) grid
     *               coordinate, and the maximal (<var>x</var>,<var>y</var>) coordinate will
     *               maps the ({@linkplain #width width}, {@linkplain #height height}) grid
     *               coordinate.
     */
    protected GridTransform2D(final GridType type,  final DataBuffer grid,
                              final Dimension size, final Rectangle2D area)
    {
        super(type, grid, size, area);
        final int n = grid.getNumBanks();
        if (n != 2) {
            throw new MismatchedDimensionException(Errors.format(
                    Errors.Keys.MISMATCHED_DIMENSION_$3, "grid", n, 2));
        }
    }

    /**
     * Gets an estimation of the derivative of this transform at a point.
     */
    @Override
    public Matrix derivative(final Point2D point) {
        final DirectPosition position;
        if (point instanceof DirectPosition) {
            position = (DirectPosition) point;
        } else {
            position = new DirectPosition2D(point);
        }
        return derivative(position);
    }

    /**
     * Returns the inverse of this transform.
     *
     * @throws NoninvertibleTransformException If this transform is not invertible.
     */
    @Override
    public synchronized MathTransform2D inverse() throws NoninvertibleTransformException {
        if (inverse == null) {
            switch (type) {
                case LOCALIZATION: {
                    // Actually throws an exception since this transform is not invertible.
                    inverse = (MathTransform2D) super.inverse();
                    break;
                }
                default: {
                    inverse = new Inverse();
                    break;
                }
            }
        }
        return inverse;
    }

    /**
     * Transforms target coordinates to source coordinates. This is done by iteratively
     * finding a target coordinate that shifts to the input coordinate. The input coordinate
     * is used as the first approximation.
     * <p>
     * This method is not applicable in the {@link GridType#LOCALIZATION} case.
     *
     * @author Rueben Schulz (UBC)
     * @author Martin Desruisseaux (IRD, Geomatys)
     * @version 3.00
     *
     * @since 3.00
     * @module
     */
    @Immutable
    private final class Inverse extends AbstractMathTransform.Inverse implements MathTransform2D {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = -6779719408779847014L;

        /**
         * Difference allowed in iterative computations. This is half the value
         * used in the NGS fortran code (so all tests pass).
         */
        private static final double ITERATION_TOLERANCE = 5E-10;

        /**
         * Maximum number of iterations for iterative computations.
         */
        private static final int MAXIMUM_ITERATIONS = 10;

        /**
         * Creates an inverse transform.
         */
        Inverse() {
            GridTransform2D.this.super();
        }

        /**
         * Transforms a single coordinate point in an array.
         *
         * @throws TransformException If there is no convergence.
         */
        @Override
        protected void transform(final double[] srcPts, final int srcOff,
                                 final double[] dstPts, final int dstOff)
                throws TransformException
        {
            double xi, yi;
            final double x, y;
            dstPts[dstOff  ] = xi = x = srcPts[srcOff  ];
            dstPts[dstOff+1] = yi = y = srcPts[srcOff+1];
            int i = MAXIMUM_ITERATIONS;
            do {
                GridTransform2D.this.transform(dstPts, dstOff, dstPts, dstOff);
                final double dx = dstPts[dstOff  ] - x;
                final double dy = dstPts[dstOff+1] - y;
                dstPts[dstOff  ] = (xi -= dx);
                dstPts[dstOff+1] = (yi -= dy);
                if (Math.abs(dx) <= ITERATION_TOLERANCE && Math.abs(dy) <= ITERATION_TOLERANCE) {
                    return;
                }
            } while (--i >= 0);
            throw new TransformException(Errors.format(Errors.Keys.NO_CONVERGENCE));
        }

        /**
         * Returns the inverse of this transform.
         */
        @Override
        public MathTransform2D inverse() {
            return GridTransform2D.this;
        }

        /**
         * Restores reference to this object after deserialization.
         */
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            GridTransform2D.this.inverse = this;
        }
    }
}
