/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2013, Geomatys
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
package org.geotoolkit.db.h2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geotoolkit.db.JDBCFeatureStoreUtilities;
import org.geotoolkit.db.dialect.SQLDialect;
import org.geotoolkit.version.AbstractVersionControl;
import org.geotoolkit.version.Version;
import org.geotoolkit.version.VersioningException;
import org.geotoolkit.feature.type.ComplexType;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.PropertyDescriptor;

/**
 * Manage versioning for a given feature type.
 * 
 * @author Johann Sorel (Geomatys)
 */
public class H2VersionControl extends AbstractVersionControl{

    private final H2FeatureStore featureStore;
    private final FeatureType featureType;
    private final SQLDialect dialect;
    private Boolean isVersioned = null;
    
    
    public H2VersionControl(H2FeatureStore featureStore, FeatureType featureType) {
        this.featureStore = featureStore;
        this.featureType = featureType;
        this.dialect = featureStore.getDialect();
    }

    @Override
    public synchronized boolean isEditable() {
        return true;
    }

    @Override
    public synchronized void startVersioning() throws VersioningException {
        if(isVersioned()){
            //versioning already active, do nothing
            return;
        }
        
        //install history functions, won't do anything if already present
        featureStore.installHSFunctions();
        
        final String schemaName = featureStore.getDatabaseSchema();
        final Set<ComplexType> visited = new HashSet<ComplexType>();
        createVersioningTable(schemaName, featureType, visited);
                
        //clear cache
        isVersioned = null;
    }

    /**
     * Create versioning table for given table.
     * 
     * @param schemaName
     * @param tableName 
     * @param visited set of already visited types, there might be recursion 
     *                or multiple properties with the same type. 
     */
    private void createVersioningTable(final String schemaName, final ComplexType type, final Set<ComplexType> visited) throws VersioningException{
        
        if(visited.contains(type)) return;
        visited.add(type);
        
        final String tableName = type.getName().getLocalPart();
        
        final StringBuilder sb = new StringBuilder("SELECT \"HS_CreateHistory\"(");
        sb.append('\'');
        if(schemaName!=null && !schemaName.isEmpty()){
            sb.append(schemaName).append('.');
        }
        sb.append(tableName);
        sb.append('\'');
        sb.append(',');

        final List<String> hsColumnNames = new ArrayList<String>();
        for(PropertyDescriptor desc : type.getDescriptors()){
            if(desc.getType() instanceof ComplexType){
                //complex type, create sub table history
                createVersioningTable(schemaName, (ComplexType)desc.getType(), visited);
            }else{
                hsColumnNames.add("'"+desc.getName().getLocalPart()+"'");
            }
        }
        
        Connection cnx = null;
        Statement stmt = null;
        try{
            cnx = featureStore.getDataSource().getConnection();
            stmt = cnx.createStatement();  
            
            sb.append("array[");
            for(int i=0,n=hsColumnNames.size();i<n;i++){
                if(i!=0) sb.append(',');
                sb.append(hsColumnNames.get(i));
            }
            
            sb.append("]);");
            stmt.executeQuery(sb.toString());
            
        }catch(SQLException ex){
            throw new VersioningException(ex.getMessage(), ex);
        }finally{
            JDBCFeatureStoreUtilities.closeSafe(featureStore.getLogger(), cnx, stmt, null);
        }
    }
    
    @Override
    public synchronized void dropVersioning() throws VersioningException {
        if(!isVersioned()){
            //versioning not active, do nothing
            return;
        }
        
        //install history functions, won't do anything if already present
        featureStore.installHSFunctions();
        final String schemaName = featureStore.getDatabaseSchema();
        final Set<ComplexType> visited = new HashSet<ComplexType>();
        dropVersioning(schemaName, featureType, visited);
        
        //clear cache
        isVersioned = null;
    }
    
    private void dropVersioning(final String schemaName, final ComplexType type, final Set<ComplexType> visited) throws VersioningException{
        if(visited.contains(type)) return;
        visited.add(type);
        
        final String tableName = type.getName().getLocalPart();
        
        //drop complex properties versioning
        for(PropertyDescriptor desc : type.getDescriptors()){
            if(desc.getType() instanceof ComplexType){
                //complex type, drop sub table history
                dropVersioning(schemaName, (ComplexType)desc.getType(),visited);
            }
        }
        
        final StringBuilder sb = new StringBuilder("SELECT \"HS_DropHistory\"(");          
        sb.append('\'');
        if(schemaName!=null && !schemaName.isEmpty()){
            sb.append(schemaName).append('.');
        }
        sb.append(tableName);
        sb.append('\'');
        sb.append(");");
        
        Connection cnx = null;
        Statement stmt = null;
        try{       
            cnx = featureStore.getDataSource().getConnection();
            stmt = cnx.createStatement();  
            stmt.executeQuery(sb.toString());
            
        }catch(SQLException ex){
            throw new VersioningException(ex.getMessage(), ex);
        }finally{
            JDBCFeatureStoreUtilities.closeSafe(featureStore.getLogger(), cnx, stmt, null);
        }
    }
    
    @Override
    public synchronized void trim(Version version) throws VersioningException {
        trim(version.getDate());
    }

    @Override
    public void trim(Date date) throws VersioningException {
        if(!isVersioned()){
            return ;
        }
        
        final String schemaName = featureStore.getDatabaseSchema();
        final Set<ComplexType> visited = new HashSet<ComplexType>();
        trim(schemaName, featureType, date, visited);
    }
    
    private void trim(final String schemaName, final ComplexType type, final Date date, final Set<ComplexType> visited) throws VersioningException{
        if(visited.contains(type)) return;
        visited.add(type);
        
        final String tableName  = type.getName().getLocalPart();
        
        //trim complex properties versioning
        for(PropertyDescriptor desc : type.getDescriptors()){
            if(desc.getType() instanceof ComplexType){
                //complex type, trim sub table history
                trim(schemaName, (ComplexType)desc.getType(), date, visited);
            }
        }
        
        Connection cnx = null;
        Statement stmt = null;
        ResultSet rs   = null;
        try{
            cnx  = featureStore.getDataSource().getConnection();
            stmt = cnx.createStatement();  
            
            final StringBuilder sb = new StringBuilder("SELECT \"HSX_TrimHistory\"(");
            sb.append('\'');
            if(schemaName != null && !schemaName.isEmpty()){
                sb.append(schemaName).append('.');
            }
            sb.append(tableName);
            sb.append('\'');
            sb.append(", TIMESTAMP '");
            sb.append(new Timestamp(date.getTime()).toString());
            sb.append("');");
            rs = stmt.executeQuery(sb.toString());
            
        }catch(SQLException ex){
            throw new VersioningException(ex.getMessage(), ex);
        }finally{
            JDBCFeatureStoreUtilities.closeSafe(featureStore.getLogger(), cnx, stmt, null);
        }
    }
    
    @Override
    public synchronized void revert(Version version) throws VersioningException {
        revert(version.getDate());
    }

    @Override
    public void revert(final Date date) throws VersioningException {
        if(!isVersioned()){
            return ;
        }
        
        final String schemaName = featureStore.getDatabaseSchema();
        final Set<ComplexType> visited = new HashSet<ComplexType>();
        revert(schemaName, featureType, date, visited);
    }
    
    private void revert(final String schemaName, final ComplexType type, final Date date, final Set<ComplexType> visited) throws VersioningException{
        if(visited.contains(type)) return;
        visited.add(type);
        
        final String tableName  = type.getName().getLocalPart();
        
        //revert complex properties versioning
        for(PropertyDescriptor desc : type.getDescriptors()){
            if(desc.getType() instanceof ComplexType){
                //complex type, revert sub table history
                revert(schemaName, (ComplexType)desc.getType(), date, visited);
            }
        }
        
        Connection cnx = null;
        Statement stmt = null;
        ResultSet rs   = null;
        try{
            cnx  = featureStore.getDataSource().getConnection();
            stmt = cnx.createStatement();  
            
            final StringBuilder sb = new StringBuilder("SELECT \"HSX_RevertHistory\"(");
            sb.append('\'');
            if(schemaName != null && !schemaName.isEmpty()){
                sb.append(schemaName).append('.');
            }
            sb.append(tableName);
            sb.append('\'');
            sb.append(", TIMESTAMP '");
            sb.append(new Timestamp(date.getTime()).toString());
            sb.append("');");
            rs = stmt.executeQuery(sb.toString());
            
        }catch(SQLException ex){
            throw new VersioningException(ex.getMessage(), ex);
        }finally{
            JDBCFeatureStoreUtilities.closeSafe(featureStore.getLogger(), cnx, stmt, null);
        }
    }
    
    @Override
    public synchronized List<Version> list() throws VersioningException {
        if(!isVersioned()){
            return Collections.EMPTY_LIST;
        }
        
        final List<Version> versions = new ArrayList<Version>();
        
        final String schemaName = featureStore.getDatabaseSchema();
        final String tableName = getHSTableName();
        
        Connection cnx = null;
        Statement stmt = null;
        ResultSet rs = null;
        try{
            cnx = featureStore.getDataSource().getConnection();
            stmt = cnx.createStatement();  
            
            final StringBuilder sb = new StringBuilder("SELECT distinct(sub.date) as date FROM (");
            sb.append("SELECT \"HS_Begin\" AS date from ");
            dialect.encodeSchemaAndTableName(sb, schemaName, tableName);
            sb.append(" UNION ");
            sb.append("SELECT \"HS_End\" AS date from ");
            dialect.encodeSchemaAndTableName(sb, schemaName, tableName);
            sb.append(" WHERE \"HS_End\" IS NOT NULL");
            sb.append(") AS sub ORDER BY date ASC");
            
            rs = stmt.executeQuery(sb.toString());
            while(rs.next()){
                final Timestamp ts = rs.getTimestamp(1);
                final Version v = new Version(this, ts.toString(), ts);
                versions.add(v);
            }            
        }catch(SQLException ex){
            throw new VersioningException(ex.getMessage(), ex);
        }finally{
            JDBCFeatureStoreUtilities.closeSafe(featureStore.getLogger(), cnx, stmt, null);
        }
        
        return versions;
    }

    @Override
    public synchronized boolean isVersioned() throws VersioningException {
        boolean hasHS = featureStore.hasHSFunctions();
        if(!hasHS) return false;
        
        if(isVersioned!=null) return isVersioned;
        
        //search for the versioning table
        final String schemaName = featureStore.getDatabaseSchema();
        final String tableName = getHSTableName();
        
        Connection cnx = null;
        Statement stmt = null;
        ResultSet rs = null;
        try{
            cnx = featureStore.getDataSource().getConnection();
            stmt = cnx.createStatement();  
            
            final StringBuilder sb = new StringBuilder("SELECT count(*) from \"information_schema\".\"tables\" WHERE ");
            sb.append("\"table_schema\"=");
            dialect.encodeValue(sb, schemaName, String.class);
            sb.append(" AND \"table_name\"=");
            dialect.encodeValue(sb, tableName, String.class);
            rs = stmt.executeQuery(sb.toString());
            rs.next();
            final int nb = rs.getInt(1);
            isVersioned = nb>0;
            
        }catch(SQLException ex){
            throw new VersioningException(ex.getMessage(), ex);
        }finally{
            JDBCFeatureStoreUtilities.closeSafe(featureStore.getLogger(), cnx, stmt, null);
        }
        
        return isVersioned;
    }
    
    /**
     * Get the history derivated table name.
     * @return String
     */
    public String getHSTableName(){
        return "HS_TBL_"+featureType.getName().getLocalPart();
    }
    
}
