/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2005-2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.referencing.operation.matrix;

import javax.vecmath.SingularMatrixException;
import org.opengis.referencing.operation.Matrix;
import org.geotoolkit.util.Cloneable;
import org.geotoolkit.util.ComparisonMode;
import org.geotoolkit.util.LenientComparable;


/**
 * A matrix capables to perform some operations. The GeoAPI {@link Matrix} interface is
 * basically a two dimensional array of numbers. The {@code XMatrix} interface adds
 * {@linkplain #invert inversion} and {@linkplain #multiply multiplication} capabilities
 * among others. It is used as a bridge across various matrix implementations in Java3D
 * ({@link javax.vecmath.Matrix3f}, {@link javax.vecmath.Matrix3d}, {@link javax.vecmath.Matrix4f},
 * {@link javax.vecmath.Matrix4d}, {@link javax.vecmath.GMatrix}).
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @author Simone Giannecchini (Geosolutions)
 * @version 3.18
 *
 * @see MatrixFactory#toXMatrix(Matrix)
 *
 * @since 2.2
 * @module
 */
public interface XMatrix extends Matrix, LenientComparable, Cloneable {
    /**
     * Sets all the values in this matrix to zero.
     */
    void setZero();

    /**
     * Sets this matrix to the identity matrix.
     */
    void setIdentity();

    /**
     * Returns {@code true} if this matrix is an identity matrix using the provided tolerance.
     * This method is equivalent to computing the difference between this matrix and an identity
     * matrix of identical size, and returning {@code true} if and only if all differences are
     * smaller than or equal to {@code tolerance}.
     *
     * @param tolerance The tolerance value.
     * @return {@code true} if this matrix is close enough to the identity matrix
     *         given the tolerance value.
     *
     * @since 2.4
     */
    boolean isIdentity(double tolerance);

    /**
     * Returns {@code true} if this matrix is an affine transform.
     * A transform is affine if the matrix is square and last row contains
     * only zeros, except in the last column which contains 1.
     *
     * @return {@code true} if this matrix is affine.
     */
    boolean isAffine();

    /**
     * Negates the value of this matrix: {@code this} = {@code -this}.
     */
    void negate();

    /**
     * Sets the value of this matrix to its transpose.
     */
    void transpose();

    /**
     * Inverts this matrix in place.
     *
     * @throws SingularMatrixException if this matrix is not invertible.
     */
    void invert() throws SingularMatrixException;

    /**
     * Sets the value of this matrix to the result of multiplying itself with the specified matrix.
     * In other words, performs {@code this} = {@code this} &times; {@code matrix}. In the context
     * of coordinate transformations, this is equivalent to
     * {@link java.awt.geom.AffineTransform#concatenate AffineTransform.concatenate}:
     * first transforms by the supplied transform and then transform the result by
     * the original transform.
     *
     * @param matrix The matrix to multiply to this matrix.
     */
    void multiply(Matrix matrix);

    /**
     * Compares the element values regardless the object class. This is similar to a call to
     * {@link javax.vecmath.GMatrix#epsilonEquals GMatrix.epsilonEquals}. The method name is
     * intentionally different in order to avoid ambiguities at compile-time.
     *
     * @param matrix    The matrix to compare.
     * @param tolerance The tolerance value.
     * @return {@code true} if this matrix is close enough to the given matrix
     *         given the tolerance value.
     *
     * @since 2.5
     */
    boolean equals(Matrix matrix, double tolerance);

    /**
     * Compares this matrix with the given object for equality. To be considered equal, the two
     * objects must meet the following conditions, which depend on the {@code mode} argument:
     * <p>
     * <ul>
     *   <li><b>{@link ComparisonMode#STRICT STRICT}:</b> the two matrixes must be of the same
     *       class, have the same size and the same element values.</li>
     *   <li><b>{@link ComparisonMode#BY_CONTRACT BY_CONTRACT}/{@link ComparisonMode#IGNORE_METADATA
     *       IGNORE_METADATA}:</b> the two matrixes must have the same size and the same element
     *       values, but are not required to be the same implementation class (any {@link Matrix}
     *       is okay).</li>
     *   <li><b>{@link ComparisonMode#APPROXIMATIVE APPROXIMATIVE}:</b> the two matrixes must have
     *       the same size, but the element values can differ up to some threshold. The threshold
     *       value is determined empirically and may change in future Geotk versions.</li>
     * </ul>
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal.
     *
     * @since 3.18
     */
    @Override
    boolean equals(Object object, ComparisonMode mode);

    /**
     * Returns a clone of this matrix.
     */
    @Override
    XMatrix clone();
}
