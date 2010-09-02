/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2010, Geomatys
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
package org.geotoolkit.referencing.cs;

import org.geotoolkit.util.Range;


/**
 * Interface for coordinate systems axes having a finite number of discrete ordinate values.
 * This interface is sometime used for axes associated to a grid coverage, for example in the
 * NetCDF file format.
 *
 * {@note The services provided by <code>DiscreteCoordinateSystemAxis</code> are redundant with
 * the service provided by the <cite>grid to CRS</cite> transform associated with grid geometries.
 * However this interface is defined as a more convenient way to access irregular ordinate values
 * on independent axes, for example a list of time instants on the temporal axis.}
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.15
 *
 * @see org.opengis.coverage.grid.GridGeometry#getGridToCRS()
 *
 * @since 3.15
 * @module
 */
public interface DiscreteCoordinateSystemAxis {
    /**
     * Returns the number of ordinate values.
     *
     * @return The number of ordinate values.
     */
    int length();

    /**
     * Returns the ordinate value at the given index. The returned value is typically
     * an instance of {@link Number} or {@link java.util.Date}.
     *
     * @param  index The index at which to return the ordinate value.
     * @return The ordinate value at the given index as a {@link Number},
     *         {@link java.util.Date} or {@link String}.
     * @throws IndexOutOfBoundsException If the given index is outside the
     *         [0 &hellip; {@linkplain #length() length}-1] range.
     */
    Comparable<?> getOrdinateAt(int index) throws IndexOutOfBoundsException;

    /**
     * Returns the range of ordinate values at the given index. The {@linkplain Range#getMinValue()
     * range minimum} and {@linkplain Range#getMaxValue() range maximum} are the values where the
     * underlying grid element switches from "belonging to" the ordinate value referenced by
     * <code>index&plusmn;1</code> to "belonging to" the ordinate value referenced by {@code index}.
     * <p>
     * The value returned by {@link #getOrdinateAt(int)} is typically in the middle of the range
     * returned by this method for the same index, but not necessarily.
     *
     * @param  index The index at which to return the range of ordinate values.
     * @return The range of ordinate values at the given index.
     * @throws IndexOutOfBoundsException If the given index is outside the
     *         [0 &hellip; {@linkplain #length() length}-1] range.
     * @throws UnsupportedOperationException if the axis is not numeric.
     */
    Range<?> getOrdinateRangeAt(int index) throws IndexOutOfBoundsException, UnsupportedOperationException;
}
