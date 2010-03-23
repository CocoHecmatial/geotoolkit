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

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.geotoolkit.util.Utilities;
import org.geotoolkit.internal.sql.table.Database;
import org.geotoolkit.internal.sql.table.LocalCache;
import org.geotoolkit.internal.sql.table.QueryType;
import org.geotoolkit.internal.sql.table.SingletonTable;
import org.geotoolkit.internal.sql.table.NoSuchTableException;


/**
 * Connection to a table of {@linkplain Layer layers}.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.10
 *
 * @since 3.10 (derived from Seagis)
 * @module
 */
final class LayerTable extends SingletonTable<LayerEntry> {
    /**
     * Connection to the table of domains. Will be created when first needed.
     */
    private transient volatile DomainOfLayerTable domains;

    /**
     * Connection to the table of series. Will be created when first needed.
     */
    private transient volatile SeriesTable series;

    /**
     * A clone of this table used for fetching fallbacks. Will be created when first needed.
     */
    private transient volatile LayerTable fallbacks;

    /**
     * The names of all series found in the database.
     * Computed only when first needed.
     */
    private transient Set<String> names;

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
     * Creates a new instance having the same configuration than the given table.
     * This is a copy constructor used for obtaining a new instance to be used
     * concurrently with the original instance.
     *
     * @param table The table to use as a template.
     */
    private LayerTable(final LayerTable table) {
        super(table);
    }

    /**
     * Returns a copy of this table. This is a copy constructor used for obtaining
     * a new instance to be used concurrently with the original instance.
     */
    @Override
    protected LayerTable clone() {
        return new LayerTable(this);
    }

    /**
     * Returns the set of layers declared in the database.
     *
     * @return The list of laters.
     * @throws SQLException If an error occured while fetching the set.
     */
    public Set<String> getNames() throws SQLException {
        Set<String> names = this.names;
        if (names == null) {
            final Set<LayerEntry> entries = getEntries();
            names = new LinkedHashSet<String>(Utilities.hashMapCapacity(entries.size()));
            for (final LayerEntry entry : entries) {
                names.add(entry.getName());
            }
            names = Collections.unmodifiableSet(names);
            this.names = names;
        }
        return names;
    }

    /**
     * Creates a layer from the current row in the specified result set.
     *
     * @param  results The result set to read.
     * @param  identifier The identifier of the layer to create.
     * @return The entry for current row in the specified result set.
     * @throws SQLException if an error occured while reading the database.
     */
    @Override
    protected LayerEntry createEntry(final ResultSet results, final Comparable<?> identifier) throws SQLException {
        final LayerQuery query = (LayerQuery) super.query;
        double period = results.getDouble(indexOf(query.period));
        if (results.wasNull()) {
            period = Double.NaN;
        }
        final String fallback = results.getString(indexOf(query.fallback));
        final String comments = results.getString(indexOf(query.comments));
        final LayerEntry entry = new LayerEntry(this, identifier, period, fallback, comments);
        return entry;
    }

    /**
     * Creates a new layer if none exist for the given name.
     *
     * @param  name The name of the layer.
     * @return {@code true} if a new layer has been created, or {@code false} if it already exists.
     * @throws SQLException if an error occured while reading or writing the database.
     */
    final boolean createIfAbsent(final String name) throws SQLException {
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
                statement.setString(indexOf(query.name), name);
                success = updateSingleton(statement);
                ce.release();
            } finally {
                transactionEnd(success);
            }
        }
        return true;
    }

    /**
     * Returns the {@link DomainOfLayerTable} instance, creating it if needed. This method is
     * invoked only from {@link LayerEntry}, which is responsible for performing synchronization
     * on the returned table. Note that because the returned instance is used in only one place,
     * synchronization in that place is effective even if the {@code DomainOfLayerTable} methods
     * are not synchronized.
     */
    final DomainOfLayerTable getDomainOfLayerTable() throws NoSuchTableException {
        DomainOfLayerTable table = domains;
        if (table == null) {
            // Not a big deal if two instances are created concurrently.
            domains = table = getDatabase().getTable(DomainOfLayerTable.class);
        }
        return table;
    }

    /**
     * Returns the {@link SeriesTable} instance, creating it if needed.  This method is invoked
     * only from {@link LayerEntry}, which is responsible for performing synchronization on the
     * returned table. Note that because the returned instance is used in only one place,
     * synchronization in that place is effective even if the {@code SeriesTable} methods are not
     * synchronized.
     */
    final SeriesTable getSeriesTable() throws NoSuchTableException {
        SeriesTable table = series;
        if (table == null) {
            // Not a big deal if two instances are created concurrently.
            series = table = getDatabase().getTable(SeriesTable.class);
        }
        return table;
    }

    /**
     * Returns a clone of this table used for fetching fallbacks. This is created for the
     * same synchronization raison than the one discussed in {@link #getSeriesTable()}.
     */
    final LayerTable getLayerTable() {
        LayerTable table = fallbacks;
        if (table == null) {
            // Not a big deal if two instances are created concurrently.
            fallbacks = table = clone();
        }
        return table;
    }
}
