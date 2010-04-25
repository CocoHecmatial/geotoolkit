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
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.SQLException;

import org.opengis.referencing.FactoryException;

import org.geotoolkit.util.Strings;
import org.geotoolkit.coverage.sql.CoverageDatabase;
import org.geotoolkit.referencing.factory.epsg.EpsgInstaller;


/**
 * Runs the Coverage database installation scripts on a given database.
 * This runner assumes that the file encoding is {@code "UTF-8"}.
 * <p>
 * Callers should set the public fields declared in this class before to
 * invoke {@link #install()}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.11
 *
 * @since 3.11
 * @module
 */
public class CoverageDatabaseInstaller extends ScriptRunner {
    /**
     * The default login used for read/write operations.
     */
    public static final String ADMINISTRATOR = "geoadmin";

    /**
     * The default login used for read only operations.
     */
    public static final String USER = "geouser";

    /**
     * The default schema.
     */
    public static final String SCHEMA = "coverages";

    /**
     * The directory of PostGIS installation scripts, or {@code null} if none.
     */
    public File postgisDir;

    /**
     * {@code true} if the "geoadmin" and "geouser" roles should be created.
     */
    public boolean createRoles;

    /**
     * {@code true} if the EPSG database should be copied.
     */
    public boolean createEPSG;

    /**
     * The coverages schema.
     */
    public String schema;

    /**
     * The name of the administrator role. This role should have read and write access.
     */
    public String admin;

    /**
     * The name of the user role. This role should have only read access.
     */
    public String user;

    /**
     * The runner under execution, or {@code null} for this runner. Used in order
     * to get information about the SQL instruction that failed.
     */
    private transient ScriptRunner runner;

    /**
     * Creates a new runner which will execute the statements using the given connection.
     *
     * @param connection The connection to the database.
     * @throws SQLException If an error occured while executing a SQL statement.
     */
    public CoverageDatabaseInstaller(final Connection connection) throws SQLException {
        super(connection);
        if (!Dialect.POSTGRESQL.equals(dialect)) {
            connection.close();
            throw new UnsupportedOperationException(dialect.toString());
        }
        setEncoding("UTF-8");
    }

    /**
     * Invoked after each step has been performed. The values range from 0 to 100 inclusive.
     * This is only a very approximative information. The default implementation does nothing.
     *
     * @param percent The progress as a value from 0 to 100 inclusive.
     * @param schema The name of the schema about to be created, or {@code null}.
     */
    protected void progress(int percent, String schema) {
    }

    /**
     * If the given value is null or empty, returns the default value.
     */
    private static String ensureNonNull(String value, String defaultValue) {
        if (value == null || (value = value.trim()).length() == 0) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Starts the installation. In case of failure, use {@link #toString()} for information
     * about the line which caused the error.
     *
     * @return The number of rows added or modified as a result of the script execution.
     * @throws IOException If an error occured while reading the input.
     * @throws SQLException If an error occured while executing a SQL statement.
     * @throws FactoryException If an error occured during the installation of the EPSG database.
     */
    public int install() throws IOException, SQLException, FactoryException {
        user   = ensureNonNull(user,   USER);
        admin  = ensureNonNull(admin,  ADMINISTRATOR);
        schema = ensureNonNull(schema, SCHEMA);
        progress(0, null);
        int n = run("prepare.sql");
        /*
         * Creates the postgis schema.
         */
        if (postgisDir != null) {
            progress(5, PostgisInstaller.DEFAULT_SCHEMA);
            final PostgisInstaller postgis = new PostgisInstaller(getConnection());
            runner = postgis;
            n += postgis.run(postgisDir);
            postgis.close(false); // Close the statement, not the connection.
            progress(30, PostgisInstaller.DEFAULT_SCHEMA);
            n += run("postgis-update.sql");
            runner = null;
        }
        /*
         * Creates the epsg schema.
         */
        if (createEPSG) {
            progress(40, EpsgInstaller.DEFAULT_SCHEMA);
            final EpsgInstaller epsg = new EpsgInstaller();
            epsg.setDatabase(getConnection());
            epsg.setSchema(EpsgInstaller.DEFAULT_SCHEMA);
            epsg.call();
        }
        /*
         * Creates the coverages schema.
         */
        progress(80, "coverages");
        n += run("coverages-create.sql");
        progress(100, null);
        return n;
    }

    /**
     * Runs the given resource file.
     *
     * @param  file The resource file to run.
     * @return The number of rows added or modified as a result of the script execution.
     * @throws IOException If an error occured while reading the input.
     * @throws SQLException If an error occured while executing a SQL statement.
     */
    private int run(final String file) throws IOException, SQLException {
        runner = null;
        final InputStream in = CoverageDatabase.class.getResourceAsStream(file);
        if (in == null) {
            throw new FileNotFoundException(file);
        }
        final int n = run(in);
        in.close();
        return n;
    }

    /**
     * Executes the given SQL statement.
     *
     * @param  sql The SQL statement to execute.
     * @return The number of rows added or modified as a result of the statement execution.
     * @throws SQLException If an error occured while executing the SQL statement.
     * @throws IOException If an I/O operation was required and failed.
     */
    @Override
    protected int execute(final StringBuilder sql) throws SQLException, IOException {
        if (!createRoles) {
            if (sql.indexOf("CREATE ROLE") >= 0) {
                return 0;
            }
        }
        Strings.replace(sql, USER,          user);
        Strings.replace(sql, ADMINISTRATOR, admin);
        Strings.replace(sql, SCHEMA,        schema);
        return super.execute(sql);
    }

    /**
     * Returns the current position (current file and current line in that file). The main purpose
     * of this method is to provides informations on the position where an exception occured.
     */
    @Override
    public String getCurrentPosition() {
        if (runner != null) {
            return runner.getCurrentPosition();
        }
        return super.getCurrentPosition();
    }

    /**
     * Returns a string representation of this runner for debugging purpose. This method
     * may be invoked after a {@link SQLException} occured in order to determine the line
     * in the SQL script that caused the error.
     */
    @Override
    public String toString() {
        if (runner != null) {
            return runner.toString();
        }
        return super.toString();
    }
}
