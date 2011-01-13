/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008-2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.referencing;

import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;

import org.geotoolkit.test.Assert;
import org.geotoolkit.io.wkt.FormattableObject;
import org.geotoolkit.referencing.operation.transform.LinearTransform;

import static org.geotoolkit.test.Commons.*;


/**
 * Inherits JUnit assertions methods, and add Geotk-specific assertion methods. The methods
 * defined in this class requires Geotk-specific API (otherwise they would be defined in the
 * {@code geotk-test} module).
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.16
 *
 * @since 3.16 (derived from 3.00)
 */
public final class ReferencingAssert extends Assert {
    /**
     * Small tolerance for comparisons of floating point values.
     */
    private static final double EPS = 1E-7;

    /**
     * Do not allow instantiation of this class.
     */
    private ReferencingAssert() {
    }

    /**
     * Asserts that the given transform is represented by diagonal matrix where every elements
     * on the diagonal have the given values. The matrix doesn't need to be square. The last
     * row is handled especially if the {@code affine} argument is {@code true}.
     *
     * @param tr     The transform.
     * @param affine If {@code true}, then the last row is expected to contains the value 1
     *               in the last column, and all other columns set to 0.
     * @param values The values which are expected on the diagonal. If this array length is
     *               smaller than the diagonal length, then the last element in the array
     *               is repeated for all remaining diagonal elements.
     *
     * @since 3.07
     */
    public static void assertDiagonalMatrix(final MathTransform tr, final boolean affine, final double... values) {
        assertTrue("The transform shall be linear.", tr instanceof LinearTransform);
        final Matrix matrix = ((LinearTransform) tr).getMatrix();
        final int numRows = matrix.getNumRow();
        final int numCols = matrix.getNumCol();
        for (int j=0; j<numRows; j++) {
            for (int i=0; i<numCols; i++) {
                final double expected;
                if (affine && j == numRows-1) {
                    expected = (i == numCols-1) ? 1 : 0;
                } else if (i == j) {
                    expected = values[Math.min(values.length-1, i)];
                } else {
                    expected = 0;
                }
                assertEquals("matrix(" + j + ',' + i + ')', expected, matrix.getElement(j, i), EPS);
            }
        }
    }

    /**
     * Asserts that the WKT of the given object is equal to the expected one.
     *
     * @param object The object to format in <cite>Well Known Text</cite> format.
     * @param expected The expected text, or {@code null} if {@code object} is expected to be null.
     *        If non-null, the expected text can use the format produced by
     *        {@link org.geotoolkit.test.Tools#printAsJavaCode} for easier reading.
     */
    public static void assertWktEquals(final IdentifiedObject object, final String expected) {
        if (expected == null) {
            assertNull(object);
        } else {
            assertNotNull(object);
            final String wkt;
            if (isSingleLine(expected) && (object instanceof FormattableObject)) {
                wkt = ((FormattableObject) object).toWKT(FormattableObject.SINGLE_LINE);
            } else {
                wkt = object.toWKT();
            }
            assertMultilinesEquals(object.getName().getCode(), decodeQuotes(expected), wkt);
        }
    }

    /**
     * Returns {@code true} if the following string has no carriage return or line feed.
     *
     * @param  text The text to check.
     * @return {@code true} if the given text is a single line.
     */
    private static boolean isSingleLine(final String text) {
        return text.lastIndexOf('\n') < 0 && text.lastIndexOf('\r') < 0;
    }
}
