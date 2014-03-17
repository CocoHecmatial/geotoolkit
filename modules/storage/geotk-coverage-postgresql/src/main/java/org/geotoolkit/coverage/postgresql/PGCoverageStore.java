/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012 - 2013, Geomatys
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
package org.geotoolkit.coverage.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.coverage.AbstractCoverageStore;
import org.geotoolkit.coverage.CoverageReference;
import org.geotoolkit.coverage.CoverageStoreFactory;
import org.geotoolkit.coverage.CoverageStoreFinder;
import org.geotoolkit.coverage.CoverageType;
import org.geotoolkit.feature.DefaultName;
import org.geotoolkit.jdbc.ManageableDataSource;
import org.geotoolkit.referencing.factory.epsg.ThreadedEpsgFactory;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.storage.DataNode;
import org.geotoolkit.storage.DefaultDataNode;
import org.geotoolkit.version.Version;
import org.geotoolkit.version.VersionControl;
import org.geotoolkit.version.VersioningException;
import org.opengis.feature.type.Name;
import org.opengis.parameter.ParameterValueGroup;

/**
 * GeotoolKit Coverage Store using PostgreSQL Raster model.
 *
 * @author Johann Sorel (Geomatys)
 */
public class PGCoverageStore extends AbstractCoverageStore{

    private ThreadedEpsgFactory epsgfactory;
    private DataSource source;
    private int fetchSize;
    private String schema;

    public PGCoverageStore(final ParameterValueGroup params, final DataSource source){
        super(params);
        ArgumentChecks.ensureNonNull("source", source);
        this.source = source;

    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public void setDatabaseSchema(String schema) {
        this.schema = schema;
    }

    public String getDatabaseSchema() {
        return schema;
    }

    public DataSource getDataSource() {
        return source;
    }

    public synchronized ThreadedEpsgFactory getEPSGFactory() throws SQLException{
        if(epsgfactory == null){
            epsgfactory = new ThreadedEpsgFactory(source);
        }
        return epsgfactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CoverageStoreFactory getFactory() {
        return CoverageStoreFinder.getFactoryById(PGCoverageStoreFactory.NAME);
    }

    @Override
    public DataNode getRootNode() throws DataStoreException {
        final DataNode root = new DefaultDataNode();
        final String ns = getDefaultNamespace();

        final StringBuilder query = new StringBuilder();

        query.append("SELECT name FROM ");
        query.append(encodeTableName("Layer"));

        Connection cnx = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            cnx = source.getConnection();
            stmt = cnx.createStatement();
            rs = stmt.executeQuery(query.toString());
            while (rs.next()){
                final Name n = new DefaultName(ns,rs.getString(1));
                final CoverageReference ref = createCoverageReference(n, null);
                root.getChildren().add(ref);
            }
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        } finally {
            closeSafe(cnx,stmt,rs);
        }
        return root;
    }

    @Override
    public CoverageReference create(Name name) throws DataStoreException {

        final StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ");
        query.append(encodeTableName("Layer"));
        query.append("(name) VALUES ('");
        query.append(name.getLocalPart());
        query.append("')");

        Connection cnx = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            cnx = source.getConnection();
            cnx.setReadOnly(false);
            stmt = cnx.createStatement();
            stmt.executeUpdate(query.toString());
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        } finally {
            closeSafe(cnx,stmt,rs);
        }

        fireCoverageAdded(name);
        return getCoverageReference(new DefaultName(getDefaultNamespace(), name.getLocalPart()));
    }

    @Override
    public void delete(Name name) throws DataStoreException {
        final StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ");
        query.append(encodeTableName("Layer"));
        query.append(" WHERE name='");
        query.append(name.getLocalPart());
        query.append("'");

        Connection cnx = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            cnx = source.getConnection();
            cnx.setReadOnly(false);
            stmt = cnx.createStatement();
            stmt.execute(query.toString());
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        } finally {
            closeSafe(cnx,stmt,rs);
        }

        fireCoverageDeleted(name);
    }

    public void dropPostgresSchema(final String name) throws DataStoreException {
        Statement stmt = null;
        Connection cnx = null;
        String sql = null;
        try {
            cnx = getDataSource().getConnection();
            sql = "DROP SCHEMA \""+ name +"\" CASCADE;";
            stmt = cnx.createStatement();
            stmt.execute(sql);
        } catch (SQLException ex) {
            throw new DataStoreException("Failed to delete features : " + ex.getMessage() + "\nSQL Query :" + sql, ex);
        } finally {
            closeSafe(cnx, stmt, null);
        }
    }

    @Override
    public Logger getLogger() {
        return super.getLogger();
    }

    int getLayerId(Connection cnx, String name) throws SQLException {
        final StringBuilder query = new StringBuilder();
        query.append("SELECT id FROM ");
        query.append(encodeTableName("Layer"));
        query.append(" WHERE name='");
        query.append(name);
        query.append("'");

        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = cnx.createStatement();
            rs = stmt.executeQuery(query.toString());
            if(rs.next()){
                return rs.getInt(1);
            }else{
                throw new SQLException("No layer for name : "+name);
            }
        } finally {
            closeSafe(null,stmt,rs);
        }

    }

    ////////////////////////////////////////////////////////////////////////////
    // Versioning support //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean handleVersioning() {
        return true;
    }

    @Override
    public VersionControl getVersioning(Name typeName) throws VersioningException {
        try {
            typeCheck(typeName);
            return new PGVersionControl(this, typeName);
        } catch (DataStoreException ex) {
            throw new VersioningException(ex.getMessage(), ex);
        }
    }

    @Override
    public CoverageReference getCoverageReference(Name name, Version version) throws DataStoreException {
        typeCheck(name);
        return createCoverageReference(name, version);
    }

    private CoverageReference createCoverageReference(final Name name, Version version) throws DataStoreException {
        if(version == null){
            try {
                //grab the latest
                VersionControl vc = new PGVersionControl(this, name);
                final List<Version> versions = vc.list();
                if(versions.isEmpty()){
                    final Calendar cal = Calendar.getInstance(PGVersionControl.GMT0);
                    cal.setTimeInMillis(0);
                    version = vc.createVersion(cal.getTime());
                }else{
                    version = versions.get(versions.size()-1);
                }
            } catch (VersioningException ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            }
        }
        return new PGCoverageReference(this, name, version);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Connection utils ////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    String encodeTableName(String name){
        final String schema = getDatabaseSchema();
        if(schema == null){
            return "\""+name+"\"";
        }else{
            return "\""+schema+"\".\""+name+"\"";
        }
    }

    public void closeSafe(final Connection cx, final Statement st, final ResultSet rs){
        closeSafe(cx);
        closeSafe(st);
        closeSafe(rs);
    }

    public void closeSafe(final ResultSet rs) {
        if (rs == null) {
            return;
        }

        try {
            rs.close();
        } catch (SQLException e) {
            final String msg = "Error occurred closing result set";
            getLogger().warning(msg);

            if (getLogger().isLoggable(Level.FINER)) {
                getLogger().log(Level.FINER, msg, e);
            }
        }
    }

    public void closeSafe(final Statement st) {
        if (st == null) {
            return;
        }

        try {
            st.close();
        } catch (SQLException e) {
            final String msg = "Error occurred closing statement";
            getLogger().warning(msg);

            if (getLogger().isLoggable(Level.FINER)) {
                getLogger().log(Level.FINER, msg, e);
            }
        }
    }

    public void closeSafe(final Connection cx) {
        if (cx == null) {
            return;
        }

        try {
            cx.close();
            getLogger().fine("CLOSE CONNECTION");
        } catch (SQLException e) {
            final String msg = "Error occurred closing connection";
            getLogger().warning(msg);

            if (getLogger().isLoggable(Level.FINER)) {
                getLogger().log(Level.FINER, msg, e);
            }
        }
    }

    @Override
    public void close() {
        if (source instanceof ManageableDataSource) {
            try {
                final ManageableDataSource mds = (ManageableDataSource) source;
                source = null;
                mds.close();
            } catch (SQLException e) {
                // it's ok, we did our best..
                getLogger().log(Level.FINE, "Could not close dataSource", e);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (source != null) {
            getLogger().severe("There's code using JDBC based coverage store and " +
                    "not disposing them. This may lead to temporary loss of database connections. " +
                    "Please make sure all data access code calls CoverageStore.dispose() " +
                    "before freeing all references to it");
            close();
        }
        super.finalize();
    }

	@Override
	public CoverageType getType() {
		return CoverageType.PYRAMID;
	}
}
