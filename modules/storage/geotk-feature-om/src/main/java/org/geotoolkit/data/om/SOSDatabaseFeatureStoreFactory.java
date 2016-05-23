/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2010, Geomatys
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

package org.geotoolkit.data.om;

import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultServiceIdentification;
import org.opengis.metadata.Identifier;
import java.util.Collections;
import org.geotoolkit.parameter.Parameters;
import org.opengis.metadata.identification.Identification;
import org.opengis.parameter.ParameterDescriptor;
import org.apache.commons.dbcp.BasicDataSource;

import java.io.IOException;

import org.geotoolkit.data.AbstractFeatureStoreFactory;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.jdbc.ManageableDataSource;
import org.geotoolkit.jdbc.DBCPDataSource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.metadata.iso.quality.DefaultConformanceResult;
import org.apache.sis.parameter.ParameterBuilder;

import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

import static org.geotoolkit.db.AbstractJDBCFeatureStoreFactory.*;
import org.geotoolkit.storage.DataType;
import org.geotoolkit.storage.DefaultFactoryMetadata;
import org.geotoolkit.storage.FactoryMetadata;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class SOSDatabaseFeatureStoreFactory extends AbstractFeatureStoreFactory {

    /** factory identification **/
    public static final String NAME = "om";
    public static final DefaultServiceIdentification IDENTIFICATION;
    static {
        IDENTIFICATION = new DefaultServiceIdentification();
        final Identifier id = new DefaultIdentifier(NAME);
        final DefaultCitation citation = new DefaultCitation(NAME);
        citation.setIdentifiers(Collections.singleton(id));
        IDENTIFICATION.setCitation(citation);
    }
    
    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);
    
    /**
     * Parameter for database port
     */
    public static final ParameterDescriptor<Integer> PORT = new ParameterBuilder()
            .addName("port")
            .setRemarks("Port")
            .setRequired(false)
            .create(Integer.class,5432);

    /**
     * Parameter identifying the OM datastore
     */
    public static final ParameterDescriptor<String> DBTYPE = new ParameterBuilder()
            .addName("dbtype")
            .setRemarks("DbType")
            .setRequired(true)
            .create(String.class, "OM");

    /**
     * Parameter for database type (postgres, derby, ...)
     */
    public static final ParameterDescriptor<String> SGBDTYPE = new ParameterBuilder()
            .addName("sgbdtype")
            .setRequired(true)
            .createEnumerated(String.class, new String[]{"derby","postgres"},null);

    /**
     * Parameter for database url for derby database
     */
    public static final ParameterDescriptor<String> DERBYURL = new ParameterBuilder()
            .addName("derbyurl")
            .setRemarks("DerbyURL")
            .setRequired(false)
            .create(String.class, null);

    /**
     * Parameter for database host
     */
    public static final ParameterDescriptor<String> HOST = new ParameterBuilder()
            .addName("host")
            .setRemarks("Host")
            .setRequired(false)
            .create(String.class, "localhost");

    /**
     * Parameter for database name
     */
    public static final ParameterDescriptor<String> DATABASE = new ParameterBuilder()
            .addName("database")
            .setRemarks("Database")
            .setRequired(false)
            .create(String.class, null);

    /**
     * Parameter for database user name
     */
    public static final ParameterDescriptor<String> USER = new ParameterBuilder()
            .addName("user")
            .setRemarks("User")
            .setRequired(false)
            .create(String.class, null);

    /**
     * Parameter for database user password
     */
    public static final ParameterDescriptor<String> PASSWD = new ParameterBuilder()
            .addName("password")
            .setRemarks("Password")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            new ParameterBuilder().addName("SOSDBParameters").createGroup(
                IDENTIFIER,DBTYPE,HOST,PORT,DATABASE,USER,PASSWD,NAMESPACE, SGBDTYPE, DERBYURL);

    @Override
    public Identification getIdentification() {
        return IDENTIFICATION;
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public CharSequence getDescription() {
        return Bundle.formatInternational(Bundle.Keys.SOSdatastoreDescription);
    }

    @Override
    public CharSequence getDisplayName() {
        return Bundle.formatInternational(Bundle.Keys.SOSdatastoreTitle);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ParameterDescriptorGroup getParametersDescriptor() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public boolean canProcess(final ParameterValueGroup params) {
        boolean valid = super.canProcess(params);
        if(valid){
            Object value = params.parameter(DBTYPE.getName().toString()).getValue();
            if("OM".equals(value)){
                Object sgbdtype = Parameters.value(SGBDTYPE, params);

                if("derby".equals(sgbdtype)){
                    //check the url is set
                    Object derbyurl = Parameters.value(DERBYURL, params);
                    return derbyurl != null;
                }else{
                    return true;
                }

            }else{
                return false;
            }
        }else{
            return false;
        }
    }

    @Override
    public SOSDatabaseFeatureStore open(final ParameterValueGroup params) throws DataStoreException {
        ensureCanProcess(params);
        try{
            //create a datasource
            final BasicDataSource dataSource = new BasicDataSource();

            // some default data source behaviour
            dataSource.setPoolPreparedStatements(true);

            // driver
            final String driver = getDriverClassName(params);
            dataSource.setDriverClassName(driver);
            final boolean isPostgres = driver.startsWith("org.postgresql");
            
            // url
            dataSource.setUrl(getJDBCUrl(params));

            // username
            final String user = (String) params.parameter(USER.getName().toString()).getValue();
            dataSource.setUsername(user);

            // password
            final String passwd = (String) params.parameter(PASSWD.getName().toString()).getValue();
            if (passwd != null) {
                dataSource.setPassword(passwd);
            }

            // some datastores might need this
            dataSource.setAccessToUnderlyingConnectionAllowed(true);

            final ManageableDataSource source = new DBCPDataSource(dataSource);
            return new SOSDatabaseFeatureStore(params,source,isPostgres);
        } catch (IOException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public SOSDatabaseFeatureStore create(final ParameterValueGroup params) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private String getDriverClassName(final ParameterValueGroup params){
        final String type  = (String) params.parameter(SGBDTYPE.getName().toString()).getValue();
        if (type.equals("derby")) {
            return "org.apache.derby.jdbc.EmbeddedDriver";
        } else {
            return "org.postgresql.Driver";
        }
    }

    private String getJDBCUrl(final ParameterValueGroup params) throws IOException {
        final String type  = (String) params.parameter(SGBDTYPE.getName().toString()).getValue();
        if (type.equals("derby")) {
            final String derbyURL = (String) params.parameter(DERBYURL.getName().toString()).getValue();
            return derbyURL;
        } else {
            final String host  = (String) params.parameter(HOST.getName().toString()).getValue();
            final Integer port = (Integer) params.parameter(PORT.getName().toString()).getValue();
            final String db    = (String) params.parameter(DATABASE.getName().toString()).getValue();
            return "jdbc:postgresql" + "://" + host + ":" + port + "/" + db;
        }
    }

    @Override
    public ConformanceResult availability() {
        DefaultConformanceResult result =  new DefaultConformanceResult();
        result.setPass(true);
        return result;
    }
    
    @Override
    public FactoryMetadata getMetadata() {
        return new DefaultFactoryMetadata(DataType.VECTOR, true, false, false, false, GEOMS_ALL);
    }
}
