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
package org.geotoolkit.coverage.sql;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import org.geotoolkit.lang.Immutable;
import org.geotoolkit.util.Utilities;
import org.geotoolkit.internal.sql.table.MultiColumnIdentifier;


/**
 * The identifier of a {@link GridCoverageEntry}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.10
 *
 * @see GridCoverageTable#createIdentifier
 *
 * @since 3.10
 * @module
 */
@Immutable
final class GridCoverageIdentifier extends MultiColumnIdentifier<GridCoverageIdentifier> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    static final long serialVersionUID = -6775081539771641953L;

    /**
     * The series in which the {@link GridCoverageEntry} is defined.
     */
    final SeriesEntry series;

    /**
     * The grid coverage filename, not including the extension.
     */
    final String filename;

    /**
     * The index of the image to read.
     */
    final short imageIndex;

    /**
     * The altitude in database units, or {@code NaN} if none.
     */
    final float z;

    /**
     * Creates a new identifier.
     */
    GridCoverageIdentifier(final SeriesEntry series, final String filename, final short imageIndex, final float z) {
        this.series     = series;
        this.filename   = filename;
        this.imageIndex = imageIndex;
        this.z          = z;
    }

    /**
     * Returns the image file. The returned file should be
     * {@linkplain File#isAbsolute absolute}. If it is not, then there is probably no
     * {@linkplain org.constellation.catalog.ConfigurationKey#ROOT_DIRECTORY root directory}
     * set and consequently the file is probably not accessible locally.
     * In such case, consider using {@link #uri()} instead.
     */
    public File file() {
        return series.file(filename);
    }

    /**
     * Returns the image URI.
     *
     * @throws URISyntaxException if the URI can not be created from the informations
     *         provided in the database.
     */
    public URI uri() throws URISyntaxException {
        return series.uri(filename);
    }

    /**
     * Returns the identifiers. This method intentionally exclude the {@link #z} value from
     * the identifiers, because it doesn't appear explicitly as a column in the database.
     */
    @Override
    public Comparable<?>[] getIdentifiers() {
        return new Comparable<?>[] {
            series.identifier,
            filename,
            imageIndex
        };
    }

    /**
     * Returns a hash code value for this identifier.
     */
    @Override
    public int hashCode() {
        return super.hashCode() ^ Float.floatToIntBits(z);
    }

    /**
     * Returns {@code true} if this object is equals to the given object.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (super.equals(other)) {
            return Utilities.equals(z, ((GridCoverageIdentifier) other).z);
        }
        return false;
    }

    /**
     * Compares this identifier with the given one for order.
     */
    @Override
    public int compareTo(final GridCoverageIdentifier that) {
        int d = super.compareTo(that);
        if (d == 0) {
            d = Float.compare(z, that.z);
        }
        return d;
    }
}
