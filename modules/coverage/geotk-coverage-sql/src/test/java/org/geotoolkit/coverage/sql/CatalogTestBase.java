/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007-2010, Open Source Geospatial Foundation (OSGeo)
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.postgresql.ds.PGSimpleDataSource;

import org.geotoolkit.internal.io.Installation;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;


/**
 * Base classe for every tests requerying a connection to a coverage database.
 * This test requires a connection to a PostgreSQL database.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.09
 *
 * @since 3.09 (derived from Seagis)
 * @module
 */
public class CatalogTestBase {
    /**
     * The connection to the database.
     */
    private static Database database;

    /**
     * Creates the database when first needed.
     *
     * @return The database.
     */
    protected static synchronized Database getDatabase() {
        if (database == null) {
            final File pf = new File(Installation.root(), "Tests/coverage-sql.properties");
            assumeTrue(pf.isFile()); // All tests will be skipped if the above resources is not found.
            final Properties properties = new Properties();
            try {
                final InputStream in = new BufferedInputStream(new FileInputStream(pf));
                properties.load(in);
                in.close();
            } catch (IOException e) {
                throw new AssertionError(e); // This will cause a JUnit test failure.
            }
            final PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setServerName(properties.getProperty("server"));
            ds.setDatabaseName(properties.getProperty("database"));
            database = new Database(ds, properties);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override public void run() {
                    final Database db = database;
                    database = null;
                    try {
                        db.getConnection().close();
                    } catch (SQLException e) {
                        // Not a recommanded practice, but we can hardly do better.
                        // We can not log neither since we are shuting down the JVM.
                        e.printStackTrace();
                    }
                }
            });
        }
        return database;
    }

    /**
     * Tests the database connection.
     *
     * @throws SQLException if an SQL error occured.
     */
    @Test
    public void testConnection() throws SQLException {
        final Connection connection = getDatabase().getConnection();
        assertNotNull(connection);
        assertFalse(connection.isClosed());
    }

    /**
     * Tries to executes the specified query statement and to read one row.
     *
     * @param  query the statement to test.
     * @throws SQLException if an query error occured.
     */
    protected static void trySelectStatement(final String query) throws SQLException {
        final Statement s = getDatabase().getConnection().createStatement();
        final ResultSet r = s.executeQuery(query);
        if (r.next()) {
            final ResultSetMetaData metadata = r.getMetaData();
            final int num = metadata.getColumnCount();
            for (int i=1; i<=num; i++) {
                final String value = r.getString(i);
                if (metadata.isNullable(i) == ResultSetMetaData.columnNoNulls) {
                    assertNotNull(value);
                }
            }
        }
        r.close();
        s.close();
    }
}
