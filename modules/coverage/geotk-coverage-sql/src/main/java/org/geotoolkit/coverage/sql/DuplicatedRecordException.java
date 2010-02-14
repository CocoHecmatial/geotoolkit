/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2005-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2007-2010, Geomatys
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

import java.sql.ResultSet;
import java.sql.SQLException;
import org.geotoolkit.resources.Errors;


/**
 * Thrown when more than one value were found for a given key.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.09
 *
 * @since 3.09 (derived from Seagis)
 * @module
 */
public class DuplicatedRecordException extends IllegalRecordException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8782377096878595182L;

    /**
     * Creates an exception from the specified result set. The table and column names are
     * obtained from the {@code results} argument if non-null. <strong>Note that the result
     * set will be closed</strong>, because this exception is always thrown when an error
     * occured while reading this result set.
     *
     * @param table   The table that produced the result set, or {@code null} if unknown.
     * @param results The result set which contains duplicated values.
     * @param column  The column index of the primary key (first column index is 1).
     * @param key     The key value for the record that was duplicated.
     * @throws SQLException if the metadata can't be read from the result set.
     */
    public DuplicatedRecordException(final Table table, final ResultSet results, final int column, final String key)
            throws SQLException
    {
        setMetadata(table, results, column, key);
    }

    /**
     * Returns a localized message created from the information provided at construction time.
     */
    @Override
    public String getLocalizedMessage() {
        return Errors.format(Errors.Keys.DUPLICATED_RECORD_$1, getPrimaryKey());
    }
}
