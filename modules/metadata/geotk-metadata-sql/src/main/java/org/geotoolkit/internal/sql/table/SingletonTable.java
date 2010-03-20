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
package org.geotoolkit.internal.sql.table;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.Set;
import java.util.LinkedHashSet;

import org.geotoolkit.lang.ThreadSafe;
import org.geotoolkit.util.collection.Cache;
import org.geotoolkit.internal.sql.TypeMapper;
import org.geotoolkit.resources.Errors;


/**
 * Base class for tables with a {@code getEntry(...)} method returning at most one entry.
 * The entries are uniquely identified by an identifier, which may be a string or an integer.
 * <p>
 * {@code SingletonTable} defines the {@link #getEntries()}, {@link #getEntry(String)} and
 * {@link #getEntry(int)} methods. Subclasses shall provide implementation for the following
 * methods:
 * <p>
 * <ul>
 *   <li>{@link #configure(QueryType, PreparedStatement)} (optional)</li>
 *   <li>{@link #createEntry(ResultSet)}: Creates an entry for the current row.</li>
 * </ul>
 * <p>
 * The entries created by this class are cached for faster access the next time a
 * {@code getEntry(...)} method is invoked again.
 *
 * @param <E> The kind of entries to be created by this table.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.10
 *
 * @since 3.09 (derived from Seagis)
 * @module
 */
@ThreadSafe(concurrent = true)
public abstract class SingletonTable<E extends Entry> extends Table {
    /**
     * The main parameters to use for the identification of an entry, or an empty array if none.
     */
    private final Parameter[] pkParam;

    /**
     * The entries created up to date. The keys shall be {@link Integer}, {@link String} or
     * {@link MultiColumnsIdentifier} instances only. Note that this field is shared between
     * different {@code Table} instances of the same kind created for the same database.
     */
    private final Cache<Comparable<?>,E> cache;

    /**
     * Returns {@code true} if the subclass overrides the
     * {@link #createIdentifier(ResultSet, int[])} method.
     */
    private final boolean invokeCreateIdentifier;

    /**
     * Creates a new table using the specified query. The optional {@code pkParam} argument
     * defines the parameters to use for looking an element by identifier. This is usually the
     * parameter for the value to search in the primary key column. This information is needed
     * for {@link #getEntry(String)} execution.
     *
     * @param  query The query to use for this table.
     * @param  pkParam The parameters for looking an element by name.
     * @throws IllegalArgumentException if the specified parameters are not one of those
     *         declared for {@link QueryType#SELECT}.
     */
    protected SingletonTable(final Query query, final Parameter... pkParam) {
        super(query);
        this.pkParam = pkParam.clone();
        cache = new Cache<Comparable<?>,E>();
        invokeCreateIdentifier = isOverriden("createIdentifier", ResultSet.class, int[].class);
    }

    /**
     * Creates a new table connected to the same {@linkplain #getDatabase database} and using
     * the same {@linkplain #query query} than the specified table. Subclass constructors should
     * not modify the query, since it is shared.
     * <p>
     * This constructor shares also the cache. This is okay if the entries created by the
     * table does not depend on the table configuration.
     *
     * @param table The table to use as a template.
     */
    protected SingletonTable(final SingletonTable<E> table) {
        super(table);
        pkParam = table.pkParam;
        cache   = table.cache;
        invokeCreateIdentifier = table.invokeCreateIdentifier;
    }

    /**
     * Returns {@code true} if the given method has been overriden. This method does not assume
     * that the method is public (otherwise we would have used a more efficient approach).
     */
    private boolean isOverriden(final String methodName, final Class<?>... parameterTypes) {
        for (Class<?> c=getClass(); !c.equals(SingletonTable.class); c=c.getSuperclass()) {
            try {
                c.getDeclaredMethod(methodName, parameterTypes);
                return true;
            } catch (NoSuchMethodException e) {
                // Ignore and check the super-class.
            }
        }
        return false;
    }

    /**
     * Returns the 1-based column indices of the primary keys. Note that some elements in the
     * returned array may be 0 if the corresponding parameter is not applicable to the current
     * query type.
     * <p>
     * This method infers the "<cite>primary keys</cite>" from the {@code pkParam} argument
     * given to the constructor. This is usually the primary key defined in the database,
     * but this is not verified.
     *
     * @return The indices of the primary key columns.
     */
    private int[] getPrimaryKeyColumns() {
        final QueryType type = getQueryType();
        final int[] indices = new int[pkParam.length];
        for (int i=0; i<indices.length; i++) {
            indices[i] = pkParam[i].column.indexOf(type);
        }
        return indices;
    }

    /**
     * Returns the first value of {@link #getPrimaryKeyColumns()} which is different than 0,
     * or 0 if none. This is a convenience method used only for formatting exception messages.
     *
     * @return The index of the first primary key column, or 0 if none.
     */
    private int getPrimaryKeyColumn() {
        return getPrimaryKeyColumn(getPrimaryKeyColumns());
    }

    /**
     * Returns the first value of the given array which is different than zero.
     * If none is found, returns zero.
     */
    private static int getPrimaryKeyColumn(final int[] pkIndices) {
        for (final int column : pkIndices) {
            if (column != 0) {
                return column;
            }
        }
        return 0;
    }

    /**
     * Sets the value of the parameters associated to the primary key columns.
     *
     * @param  statement The statement in which to set the parameter value.
     * @param  identifier The identifier to set in the statement.
     * @throws SQLException If the parameter can not be set.
     */
    private void setPrimaryKeyParameter(final PreparedStatement statement, final Comparable<?> identifier)
            throws SQLException
    {
        final Comparable<?>[] identifiers;
        if (identifier instanceof MultiColumnIdentifier<?>) {
            identifiers = ((MultiColumnIdentifier<?>) identifier).getIdentifiers();
        } else {
            identifiers = new Comparable<?>[] {identifier};
        }
        if (identifiers.length != pkParam.length) {
            throw new CatalogException(errors().getString(Errors.Keys.MISMATCHED_ARRAY_LENGTH));
        }
        for (int i=0; i<identifiers.length; i++) {
            final int pkIndex = indexOf(pkParam[i]);
            if (identifier instanceof Number) {
                statement.setInt(pkIndex, ((Number) identifier).intValue());
            } else {
                statement.setString(pkIndex, identifier.toString());
            }
        }
    }

    /**
     * Returns {@code true} if the given column in the result set is numeric.
     *
     * @param  results The result set.
     * @param  pkIndex The index of the column to inspect (typically the primary key), or 0 if none.
     * @return {@code true} If the given column in the given result set is numeric.
     * @throws SQLException If an error occured while fetching the metadata.
     */
    private static boolean isNumeric(final ResultSet results, final int pkIndex) throws SQLException {
        if (pkIndex != 0) {
            final Class<?> type = TypeMapper.toJavaType(results.getMetaData().getColumnType(pkIndex));
            if (type != null) {
                return Number.class.isAssignableFrom(type);
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the prepared statement to be created by
     * {@link #getStatement(String)} should be able to return auto-generated keys.
     */
    @Override
    final boolean wantsAutoGeneratedKeys() {
        return QueryType.INSERT.equals(getQueryType());
    }

    /**
     * Creates an identifier for the current row in the given result set. This method needs to
     * be overriden by subclasses using {@link MultiColumnIdentifier}. Other subclasses don't
     * need to override this method: a {@link String} or {@link Integer} identifier will be
     * used as needed.
     *
     * @param  results The result set.
     * @param  pkIndices The indices of the column to inspect (typically the primary keys).
     * @return The {@linkplain MultiColumnIdentifier multi-column identifier}.
     * @throws SQLException If an error occured while fetching the data.
     *
     * @since 3.10
     */
    protected Comparable<?> createIdentifier(ResultSet results, int[] pkIndices) throws SQLException {
        results.close();
        throw new CatalogException(errors().getString(Errors.Keys.UNSUPPORTED_OPERATION_$1, getQueryType()));
    }

    /**
     * Creates an {@link Element} object for the current {@linkplain ResultSet result set} row.
     * This method is invoked automatically by {@link #getEntry(String)} and {@link #getEntries()}.
     *
     * @param  results  The result set to use for fetching data. Only the current row should be
     *                  used, i.e. {@link ResultSet#next} should <strong>not</strong> be invoked.
     * @param  identifier The identifier of the entry being created.
     * @return The element for the current row in the specified {@code results}.
     * @throws CatalogException if a logical error has been detected in the database content.
     * @throws SQLException if an error occured will reading from the database.
     */
    protected abstract E createEntry(final ResultSet results, final Comparable<?> identifier)
            throws CatalogException, SQLException;

    /**
     * Invokes the user's {@link #createEntry(ResultSet)} method, but wraps {@link SQLException}
     * into {@link CatalogException} because the later provides more informations.
     *
     * @throws CatalogException If an error occured during {@link #createEntry(ResultSet)}.
     * @throws SQLException If an error occured during {@link CatalogException#setMetadata}.
     *         Note that this is not an error occuring during normal execution, but rather
     *         an error occuring while querying database metadata for building the exception.
     */
    private E createEntryCatchSQL(final ResultSet results, final Comparable<?> identifier)
            throws CatalogException, SQLException
    {
        CatalogException exception;
        try {
            return createEntry(results, identifier);
        } catch (CatalogException cause) {
            if (cause.isMetadataInitialized()) {
                throw cause;
            }
            exception = cause;
        } catch (SQLException cause) {
            exception = new CatalogException(cause);
        }
        exception.setMetadata(this, results, getPrimaryKeyColumn(), identifier);
        exception.clearColumnName();
        throw exception;
    }

    /**
     * Returns an element for the given identifier.
     *
     * @param  identifier The name or numeric identifier of the element to fetch.
     * @return The element for the given identifier, or {@code null} if {@code identifier} was null.
     * @throws NoSuchRecordException if no record was found for the specified key.
     * @throws SQLException if an error occured will reading from the database.
     */
    public E getEntry(final Comparable<?> identifier) throws NoSuchRecordException, SQLException {
        if (identifier == null) {
            return null;
        }
        E entry = cache.peek(identifier);
        if (entry == null) {
            final Cache.Handler<E> handler = cache.lock(identifier);
            try {
                entry = handler.peek();
                if (entry == null) synchronized (getLock()) {
                    final LocalCache.Stmt ce = getStatement(QueryType.SELECT);
                    final PreparedStatement statement = ce.statement;
                    setPrimaryKeyParameter(statement, identifier);
                    final ResultSet results = statement.executeQuery();
                    while (results.next()) {
                        final E candidate = createEntryCatchSQL(results, identifier);
                        if (entry == null) {
                            entry = candidate;
                        } else if (!entry.equals(candidate)) {
                            // The ResultSet will be closed by the constructor below.
                            throw new DuplicatedRecordException(this, results, getPrimaryKeyColumn(), identifier);
                        }
                    }
                    if (entry == null) {
                        // The ResultSet will be closed by the constructor below.
                        throw new NoSuchRecordException(this, results, getPrimaryKeyColumn(), identifier);
                    }
                    results.close();
                    ce.release();
                }
            } finally {
                handler.putAndUnlock(entry);
            }
        }
        return entry;
    }

    /**
     * Returns all entries available in the database. The returned set may or may not be
     * serializable or modifiable, at implementation choice. If allowed, modification in
     * the returned set will not alter this table.
     *
     * @return The set of entries. May be empty, but neven {@code null}.
     * @throws SQLException if an error occured will reading from the database.
     */
    public Set<E> getEntries() throws SQLException {
        final Set<E> entries = new LinkedHashSet<E>();
        synchronized (getLock()) {
            final LocalCache.Stmt ce = getStatement(QueryType.LIST);
            final int[] pkIndices = getPrimaryKeyColumns();
            final int pkIndex = getPrimaryKeyColumn(pkIndices);
            final ResultSet results = ce.statement.executeQuery();
            final boolean isNumeric = !invokeCreateIdentifier && isNumeric(results, pkIndex);
            while (results.next()) {
                final Comparable<?> identifier;
                if (invokeCreateIdentifier) {
                    identifier = createIdentifier(results, pkIndices);
                } else if (isNumeric) {
                    identifier = results.getInt(pkIndex);
                } else {
                    identifier = results.getString(pkIndex);
                }
                E entry = cache.peek(identifier);
                if (entry == null) {
                    final Cache.Handler<E> handler = cache.lock(identifier);
                    try {
                        entry = handler.peek();
                        if (entry == null) {
                            entry = createEntryCatchSQL(results, identifier);
                        }
                    } finally {
                        handler.putAndUnlock(entry);
                    }
                }
                if (!entries.add(entry)) {
                    // The ResultSet will be closed by the constructor below.
                    throw new DuplicatedRecordException(this, results, pkIndex, identifier);
                }
            }
            results.close();
            ce.release();
        }
        return entries;
    }

    /**
     * Checks if an entry exists for the given name. This method do not attempt to create
     * the entry and doesn't check if the entry is valid.
     *
     * @param  identifier The identifier of the entry to fetch.
     * @return {@code true} if an entry of the given identifier was found.
     * @throws SQLException if an error occured will reading from the database.
     */
    public boolean exists(final Comparable<?> identifier) throws SQLException {
        if (identifier == null) {
            return false;
        }
        if (cache.containsKey(identifier)) {
            return true;
        }
        final boolean hasNext;
        synchronized (getLock()) {
            final LocalCache.Stmt ce = getStatement(QueryType.EXISTS);
            final PreparedStatement statement = ce.statement;
            setPrimaryKeyParameter(statement, identifier);
            final ResultSet results = statement.executeQuery();
            hasNext = results.next();
            results.close();
            ce.release();
        }
        return hasNext;
    }

    /**
     * Deletes the entry for the given identifier.
     *
     * @param  identifier The identifier of the entry to delete.
     * @return The number of entries deleted.
     * @throws SQLException if an error occured will reading from or writting to the database.
     */
    public int delete(final Comparable<?> identifier) throws SQLException {
        if (identifier == null) {
            return 0;
        }
        final int count;
        boolean success = false;
        synchronized (getLock()) {
            transactionBegin();
            try {
                final LocalCache.Stmt ce = getStatement(QueryType.DELETE);
                final PreparedStatement statement = ce.statement;
                setPrimaryKeyParameter(statement, identifier);
                count = update(statement);
                ce.release();
                success = true;
            } finally {
                transactionEnd(success);
            }
        }
        // Update the cache only on successfuly deletion.
        cache.remove(identifier);
        return count;
    }

    /**
     * Deletes many elements. "Many" depends on the configuration set by {@link #configure}.
     * It may be the whole table. Note that this action may be blocked if the user doesn't
     * have the required database authorisations, or if some records are still referenced in
     * foreigner tables.
     *
     * @return The number of elements deleted.
     * @throws SQLException if an error occured will reading from or writting to the database.
     */
    public int deleteAll() throws SQLException {
        final int count;
        boolean success = false;
        synchronized (getLock()) {
            transactionBegin();
            try {
                final LocalCache.Stmt ce = getStatement(QueryType.DELETE_ALL);
                count = update(ce.statement);
                ce.release();
                success = true;
            } finally {
                transactionEnd(success);
            }
        }
        // Update the cache only on successfuly deletion.
        cache.clear();
        return count;
    }

    /**
     * Executes the specified SQL {@code INSERT}, {@code UPDATE} or {@code DELETE} statement.
     * As a special case, this method does not execute the statement during testing and debugging
     * phases. In the later case, this method rather prints the statement to the stream specified
     * to {@link Database#setUpdateSimulator}.
     *
     * @param  statement The statement to execute.
     * @return The number of elements updated.
     * @throws SQLException if an error occured.
     */
    private int update(final PreparedStatement statement) throws SQLException {
        final Database database = getDatabase();
        database.ensureOngoingTransaction();
        final PrintWriter out = database.getUpdateSimulator();
        if (out != null) {
            out.println(statement);
            return 0;
        } else {
            return statement.executeUpdate();
        }
    }

    /**
     * Executes the specified SQL {@code INSERT}, {@code UPDATE} or {@code DELETE} statement,
     * which is expected to insert exactly one record. As a special case, this method does not
     * execute the statement during testing and debugging phases. In the later case, this method
     * rather prints the statement to the stream specified to {@link Database#setUpdateSimulator}.
     *
     * @param  statement The statement to execute.
     * @return {@code true} if the singleton has been found and updated.
     * @throws IllegalUpdateException if more than one elements has been updated.
     * @throws SQLException if an error occured.
     */
    protected final boolean updateSingleton(final PreparedStatement statement)
            throws IllegalUpdateException, SQLException
    {
        final int count = update(statement);
        if (count > 1) {
            throw new IllegalUpdateException(getLocale(), count);
        }
        return count != 0;
    }
}
