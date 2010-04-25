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
package org.geotoolkit.internal.sql;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * Runs the PostGIS installation scripts on a given database.
 * This runner assumes that the file encoding is {@code "ISO-8859-1"}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.11
 *
 * @since 3.11
 * @module
 */
public final class PostgisInstaller extends ScriptRunner {
    /**
     * The filename of the installation script.
     */
    public static final String INSTALL = "lwpostgis.sql";

    /**
     * The filename of the CRS definitions.
     */
    public static final String REF_SYS = "spatial_ref_sys.sql";

    /**
     * The default schema were to create PostGIS objects.
     */
    public static final String DEFAULT_SCHEMA = "postgis";

    /**
     * The schema where the PostGIS tables will be created, or {@code null} if none.
     * The default value is {@value #DEFAULT_SCHEMA}. A {@code null} value means that
     * there is no schema (i.e. tables are created in the public schema).
     */
    public String schema = DEFAULT_SCHEMA;

    /**
     * Creates a new runner which will execute the statements using the given connection.
     * It will be caller responsability to close this connection after the install.
     *
     * @param connection The connection to the database.
     * @throws SQLException If an error occured while executing a SQL statement.
     */
    public PostgisInstaller(final Connection connection) throws SQLException {
        super(connection);
        if (!Dialect.POSTGRESQL.equals(dialect)) {
            connection.close();
            throw new UnsupportedOperationException(dialect.toString());
        }
        setEncoding("ISO-8859-1");
    }

    /**
     * Run the SQL script read from the given directory. In case of failure, use
     * {@link #toString()} for information about the line which caused the error.
     *
     * @param  directory The directory of the script(s) to run.
     * @return The number of rows added or modified as a result of the script(s) execution.
     * @throws IOException If an error occured while reading the input.
     * @throws SQLException If an error occured while executing a SQL statement.
     */
    @Override
    public int run(final File directory) throws IOException, SQLException {
        if (directory.isFile()) {
            return super.run(directory);
        }
        int n = 0;
        if (schema != null) {
            final LineNumberReader reader = new LineNumberReader(
                    new StringReader("SET search_path = " + schema + END_OF_STATEMENT));
            n = run(reader);
            reader.close();
        }
        n += run(new File(directory, INSTALL));
        n += run(new File(directory, REF_SYS));
        return n;
    }
}
