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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.geotoolkit.lang.ThreadSafe;
import org.geotoolkit.internal.sql.table.CatalogException;
import org.geotoolkit.internal.sql.table.Database;
import org.geotoolkit.internal.sql.table.LocalCache;
import org.geotoolkit.internal.sql.table.QueryType;
import org.geotoolkit.internal.sql.table.SingletonTable;


/**
 * Connection to a table of {@linkplain Layer layers}.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.10
 *
 * @since 3.10 (derived from Seagis)
 * @module
 */
@ThreadSafe(concurrent = true)
final class LayerTable extends SingletonTable<LayerEntry> {
    /**
     * Connection to the table of domains. Will be created when first needed. Will be shared
     * by all {@code LayerTable}, since it is independent of {@code LayerTable} settings.
     */
    private volatile DomainOfLayerTable domains;

    /**
     * Connection to the table of series. Will be created when first needed. Will be shared
     * by all {@code LayerTable}.
     */
    private volatile SeriesTable series;

    /**
     * Creates a layer table.
     *
     * @param database Connection to the database.
     */
    public LayerTable(final Database database) {
        this(new LayerQuery(database));
    }

    /**
     * Constructs a new {@code LayerTable} from the specified query.
     */
    private LayerTable(final LayerQuery query) {
        super(query, query.byName);
    }

    /**
     * Creates a layer from the current row in the specified result set.
     *
     * @param  results The result set to read.
     * @return The entry for current row in the specified result set.
     * @throws CatalogException if an inconsistent record is found in the database.
     * @throws SQLException if an error occured while reading the database.
     */
    @Override
    protected LayerEntry createEntry(final ResultSet results) throws CatalogException, SQLException {
        final LayerQuery query = (LayerQuery) super.query;
        final String name = results.getString(indexOf(query.name));
        double period = results.getDouble(indexOf(query.period));
        if (results.wasNull()) {
            period = Double.NaN;
        }
        final String fallback = results.getString(indexOf(query.fallback));
        final String comments = results.getString(indexOf(query.comments));
        final LayerEntry entry = new LayerEntry(this, name, period, fallback, comments);
        return entry;
    }

    /**
     * Returns the {@link DomainOfLayerTable} instance, creating it if needed.
     */
    final DomainOfLayerTable getDomainOfLayerTable() throws CatalogException {
        DomainOfLayerTable table = domains;
        if (table == null) {
            // This is not a big deal if the following line is invoked twice,
            // since getTable is synchronized and caches its returned values.
            domains = table = getDatabase().getTable(DomainOfLayerTable.class);
        }
        return table;
    }

    /**
     * Returns the {@link SeriesTable} instance, creating it if needed.
     */
    final SeriesTable getSeriesTable() throws CatalogException {
        SeriesTable table = series;
        if (table == null) {
            // This is not a big deal if the following line is invoked twice,
            // since getTable is synchronized and caches its returned values.
            series = table = getDatabase().getTable(SeriesTable.class);
        }
        return table;
    }

    /**
     * Creates a new layer if none exist for the given name.
     *
     * @param  name The name of the layer.
     * @return {@code true} if a new layer has been created, or {@code false} if it already exists.
     * @throws CatalogException if a logical error occured.
     * @throws SQLException if an error occured while reading or writing the database.
     */
    final boolean createIfAbsent(final String name) throws SQLException, CatalogException {
        ensureNonNull("name", name);
        if (exists(name)) {
            return false;
        }
        final LayerQuery query = (LayerQuery) super.query;
        synchronized (getLock()) {
            boolean success = false;
            transactionBegin();
            try {
                final LocalCache.Stmt ce = getStatement(QueryType.INSERT);
                final PreparedStatement statement = ce.statement;
                statement.setString(indexOf(query.byName), name);
                success = updateSingleton(statement);
                ce.release();
            } finally {
                transactionEnd(success);
            }
        }
        return true;
    }
}
