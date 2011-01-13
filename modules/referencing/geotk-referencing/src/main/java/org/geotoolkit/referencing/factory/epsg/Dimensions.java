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
package org.geotoolkit.referencing.factory.epsg;

import org.geotoolkit.util.Utilities;


/**
 * A counter for source and target dimensions (to be kept together).
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.03
 *
 * @since 2.1
 * @module
 */
final class Dimensions {
    /**
     * The source and target dimensions.
     */
    Integer sourceDimensions, targetDimensions;

    /**
     * The occurrences of this pair of dimensions.
     */
    int occurrences;

    /**
     * Creates an uninitialized {@code Dimensions}.
     */
    Dimensions() {
    }

    /**
     * Creates a dimensions initialized to the same value than the given one.
     */
    Dimensions(final Dimensions other) {
        sourceDimensions = other.sourceDimensions;
        targetDimensions = other.targetDimensions;
    }

    /**
     * Returns a hash code for this object.
     */
    @Override
    public int hashCode() {
        // MUST ignore 'occurrences'.
        int code = 0;
        if (sourceDimensions != null) code  = sourceDimensions;
        if (targetDimensions != null) code += targetDimensions * 31;
        return code;
    }

    /**
     * Compares this object wirh the given one for equality.
     */
    @Override
    public boolean equals(final Object object) {
        // MUST ignore 'occurrences'.
        if (object instanceof Dimensions) {
            final Dimensions that = (Dimensions) object;
            return Utilities.equals(sourceDimensions, that.sourceDimensions) &&
                   Utilities.equals(targetDimensions, that.targetDimensions);
        }
        return false;
    }

    /**
     * For debugging purpose only.
     */
    @Override
    public String toString() {
        return "[(" + sourceDimensions + ',' + targetDimensions + ")\u00D7" + occurrences + ']';
    }
}
