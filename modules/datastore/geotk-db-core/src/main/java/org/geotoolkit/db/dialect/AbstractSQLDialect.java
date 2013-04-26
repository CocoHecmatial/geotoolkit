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
package org.geotoolkit.db.dialect;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.SQLException;
import org.geotoolkit.feature.AttributeTypeBuilder;
import org.geotoolkit.filter.capability.DefaultFilterCapabilities;
import org.geotoolkit.filter.visitor.CapabilitiesFilterSplitter;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractSQLDialect implements SQLDialect {

    @Override
    public boolean ignoreTable(String name) {
        return false;
    }

    /**
     * Default implementation handles no filter.
     * Everything will be added in the post filter.
     */
    @Override
    public Filter[] splitFilter(Filter filter, FeatureType type) {
        final CapabilitiesFilterSplitter splitter = new CapabilitiesFilterSplitter(
                (DefaultFilterCapabilities)getFilterCapabilities(), type);
        filter.accept(splitter, null);
        return new Filter[]{splitter.getPreFilter(),splitter.getPostFilter()};
    }

    @Override
    public void encodeColumnName(StringBuilder sql, String name) {
        sql.append(getTableEscape()).append(name).append(getTableEscape());
    }
    
    @Override
    public void encodeColumnAlias(StringBuilder sql, String name) {
        sql.append(" as ");
        encodeColumnName(sql, name);
    }
    
    @Override
    public void encodeSchemaName(StringBuilder sql, String name) {
        sql.append(getTableEscape()).append(name).append(getTableEscape());
    }

    @Override
    public void encodeTableName(StringBuilder sql, String name) {
        sql.append(getTableEscape()).append(name).append(getTableEscape());
    }
    
    @Override
    public void encodeSchemaAndTableName(StringBuilder sql, String databaseSchema, String tableName) {
        if (databaseSchema != null && !databaseSchema.isEmpty()) {
            encodeSchemaName(sql, databaseSchema);
            sql.append('.');
        }
        encodeTableName(sql, tableName);
    }
    
    @Override
    public void encodePostColumnCreateTable(StringBuilder sql, AttributeDescriptor att) {
    }
    
    @Override
    public void postCreateTable(String schemaName, SimpleFeatureType featureType,
                                Connection cx) throws SQLException {
    }
    
    @Override
    public void encodePostCreateTable(StringBuilder sql, String tableName) {
    }
    
    @Override
    public void decodeColumnType(final AttributeTypeBuilder atb, final Connection cx,
            final String typeName, final int datatype, final String schemaName,
            final String tableName, final String columnName) throws SQLException {

        final Class binding = getJavaType(datatype, typeName);
        atb.setName(columnName);
        atb.setBinding(binding);
    }

    
}
