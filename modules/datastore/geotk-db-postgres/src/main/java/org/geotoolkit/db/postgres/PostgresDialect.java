/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011-2013, Geomatys
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
package org.geotoolkit.db.postgres;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.geotoolkit.db.DefaultJDBCFeatureStore;
import org.geotoolkit.db.dialect.SQLDialect;
import static org.geotoolkit.db.JDBCFeatureStoreUtilities.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class PostgresDialect implements SQLDialect{

    private static final Map<Integer,Class> TYPE_TO_CLASS = new HashMap<Integer, Class>();
    private static final Map<String,Class> TYPENAME_TO_CLASS = new HashMap<String, Class>();
    private static final Map<Class,String> CLASS_TO_TYPENAME = new HashMap<Class,String>();
    private static final Set<String> IGNORE_TABLES = new HashSet<String>();
    
    static {
        //fill base types
        TYPE_TO_CLASS.put(Types.VARCHAR,        String.class);
        TYPE_TO_CLASS.put(Types.CHAR,           String.class);
        TYPE_TO_CLASS.put(Types.LONGVARCHAR,   String.class);
        TYPE_TO_CLASS.put(Types.NVARCHAR,       String.class);
        TYPE_TO_CLASS.put(Types.NCHAR,          String.class);
        TYPE_TO_CLASS.put(Types.BIT,            Boolean.class);
        TYPE_TO_CLASS.put(Types.BOOLEAN,        Boolean.class);
        TYPE_TO_CLASS.put(Types.TINYINT,        Short.class);
        TYPE_TO_CLASS.put(Types.SMALLINT,       Short.class);
        TYPE_TO_CLASS.put(Types.INTEGER,        Integer.class);
        TYPE_TO_CLASS.put(Types.BIGINT,         Long.class);
        TYPE_TO_CLASS.put(Types.REAL,           Float.class);
        TYPE_TO_CLASS.put(Types.FLOAT,          Double.class);
        TYPE_TO_CLASS.put(Types.DOUBLE,         Double.class);
        TYPE_TO_CLASS.put(Types.DECIMAL,        BigDecimal.class);
        TYPE_TO_CLASS.put(Types.NUMERIC,        BigDecimal.class);
        TYPE_TO_CLASS.put(Types.DATE,           Date.class);
        TYPE_TO_CLASS.put(Types.TIME,           Time.class);
        TYPE_TO_CLASS.put(Types.TIMESTAMP,      Timestamp.class);     
        TYPE_TO_CLASS.put(Types.BLOB,           byte[].class);
        TYPE_TO_CLASS.put(Types.BINARY,         byte[].class);
        TYPE_TO_CLASS.put(Types.CLOB,           String.class);   
        TYPE_TO_CLASS.put(Types.VARBINARY,      byte[].class);
        TYPE_TO_CLASS.put(Types.ARRAY,          Array.class);
        
        
//NAME IN CREATE QUERY          SQL TYPE     SQL TPE NAME
/*serial                            4           serial      */ TYPENAME_TO_CLASS.put("serial", Integer.class);
/*bigserial                         -5          bigserial   */ TYPENAME_TO_CLASS.put("bigserial", Long.class);
/*abstime                           1111        abstime     */ TYPENAME_TO_CLASS.put("abstime", Object.class);
/*aclitem                           1111        aclitem     */ TYPENAME_TO_CLASS.put("aclitem", Object.class);
/*bigint                            -5          int8        */ TYPENAME_TO_CLASS.put("int8", Long.class);
/*bit(1)                            -7          bit         */ TYPENAME_TO_CLASS.put("bit", Boolean.class);
/*bit varying                       1111        varbit      */ TYPENAME_TO_CLASS.put("varbit", Object.class);
/*boolean                           -7          bool        */ TYPENAME_TO_CLASS.put("bool", Boolean.class);
/*box                               1111        box         */ TYPENAME_TO_CLASS.put("box", Object.class);
/*byte                              -2          bytea       */ TYPENAME_TO_CLASS.put("bytea", Object.class);
/*char                              1           char        */ TYPENAME_TO_CLASS.put("char", String.class);
/*character(1)                      1           bpchar      */ TYPENAME_TO_CLASS.put("bpchar", String.class);
/*character varying                 12          varchar     */ TYPENAME_TO_CLASS.put("varchar", String.class);
/*cid                               1111        cid         */ TYPENAME_TO_CLASS.put("cid", Object.class);
/*cidr                              1111        cidr        */ TYPENAME_TO_CLASS.put("cidr", Object.class);
/*circle                            1111        circle      */ TYPENAME_TO_CLASS.put("circle", Object.class);
/*date                              91          date        */ TYPENAME_TO_CLASS.put("date", Date.class);
/*double precision                  8           float8      */ TYPENAME_TO_CLASS.put("float8", Double.class);
/*gtsvector                         1111        gtsvector   */ TYPENAME_TO_CLASS.put("gtsvector", Object.class);
/*inet                              1111        inet        */ TYPENAME_TO_CLASS.put("inet", Object.class);
/*int2vector                        1111        int2vector  */ TYPENAME_TO_CLASS.put("int2vector", Object.class);
/*integer                           4           int4        */ TYPENAME_TO_CLASS.put("int4", Integer.class);
/*interval                          1111        interval    */ TYPENAME_TO_CLASS.put("interval", Object.class);
/*line                              1111        line        */ TYPENAME_TO_CLASS.put("line", Object.class);
/*lseg                              1111        lseg        */ TYPENAME_TO_CLASS.put("lseg", Object.class);
/*macaddr                           1111        macaddr     */ TYPENAME_TO_CLASS.put("macaddr", Object.class);
/*money                             8           money       */ TYPENAME_TO_CLASS.put("money", Double.class);
/*name                              12          name        */ TYPENAME_TO_CLASS.put("name", String.class);
/*numeric                           2           numeric     */ TYPENAME_TO_CLASS.put("numeric", BigDecimal.class);
/*oid                               -5          oid         */ TYPENAME_TO_CLASS.put("oid", Long.class);
/*oidvector                         1111        oidvector   */ TYPENAME_TO_CLASS.put("oidvector", Object.class);
/*path                              1111        path        */ TYPENAME_TO_CLASS.put("path", Object.class);
/*pg_node_tree                      1111        pg_node_tree*/ TYPENAME_TO_CLASS.put("pg_node_tree", Object.class);
/*point                             1111        point       */ TYPENAME_TO_CLASS.put("point", Object.class);
/*polygon                           1111        polygon     */ TYPENAME_TO_CLASS.put("polygon", Object.class);
/*real                              7           float4      */ TYPENAME_TO_CLASS.put("float4", Float.class);
/*refcursor                         1111        refcursor   */ TYPENAME_TO_CLASS.put("refcursor", Object.class);
/*regclass                          1111        regclass    */ TYPENAME_TO_CLASS.put("regclass", Object.class);
/*regconfig                         1111        regconfig   */ TYPENAME_TO_CLASS.put("regconfig", Object.class);
/*regdictionary                     1111       regdictionary*/ TYPENAME_TO_CLASS.put("regdictionary", Object.class);
/*regoper                           1111        regoper     */ TYPENAME_TO_CLASS.put("regoper", Object.class);
/*regoperator                       1111        regoperator */ TYPENAME_TO_CLASS.put("regoperator", Object.class);
/*regproc                           1111        regproc     */ TYPENAME_TO_CLASS.put("regproc", Object.class);
/*regprocedure                      1111        regprocedure*/ TYPENAME_TO_CLASS.put("regprocedure", Object.class);
/*regtype                           1111        regtype     */ TYPENAME_TO_CLASS.put("regtype", Object.class);
/*reltime                           1111        reltime     */ TYPENAME_TO_CLASS.put("reltime", Object.class);
/*smallint                          8           int2        */ TYPENAME_TO_CLASS.put("int2", Short.class);
/*smgr                              1111        smgr        */ TYPENAME_TO_CLASS.put("smgr", Object.class);
/*text                              12          text        */ TYPENAME_TO_CLASS.put("text", String.class);
/*tid                               1111        tid         */ TYPENAME_TO_CLASS.put("tid", Object.class);
/*timestamp without time zone       93          timestamp   */ TYPENAME_TO_CLASS.put("timestamp", Timestamp.class);
/*timestamp with time zone          93          timestamptz */ TYPENAME_TO_CLASS.put("timestamptz", Timestamp.class);
/*time without time zone            92          time        */ TYPENAME_TO_CLASS.put("time", Time.class);
/*time with time zone               92          timetz      */ TYPENAME_TO_CLASS.put("timetz", Time.class);
/*tinterval                         1111        tinterval   */ TYPENAME_TO_CLASS.put("tinterval", Object.class);
/*tsquery                           1111        tsquery     */ TYPENAME_TO_CLASS.put("tsquery", Object.class);
/*tsvector                          1111        tsvector    */ TYPENAME_TO_CLASS.put("tsvector", Object.class);
/*txid_snapshot                     1111       txid_snapshot*/ TYPENAME_TO_CLASS.put("txid_snapshot", Object.class);
/*uuid                              1111        uuid        */ TYPENAME_TO_CLASS.put("uuid", Object.class);
/*xid                               1111        xid         */ TYPENAME_TO_CLASS.put("xid", Object.class);
/*xml                               2009        xml         */ TYPENAME_TO_CLASS.put("xml", String.class);
/*box2d                             1111        box2d       */ TYPENAME_TO_CLASS.put("box2d", Object.class);
/*box3d                             1111        box3d       */ TYPENAME_TO_CLASS.put("box3d", Object.class);
/*box3d_extent                      1111        box3d_extent*/ TYPENAME_TO_CLASS.put("box3d_extent", Object.class);
/*chip                              1111        chip        */ TYPENAME_TO_CLASS.put("chip", Object.class);
/*geography                         1111        geography   */ TYPENAME_TO_CLASS.put("geography", Object.class);
/*geometry_dump                     2002       geometry_dump*/ TYPENAME_TO_CLASS.put("geometry_dump", Object.class);
/*gidx                              1111        gidx        */ TYPENAME_TO_CLASS.put("gidx", Object.class);
/*pgis_abs                          1111        pgis_abs    */ TYPENAME_TO_CLASS.put("pgis_abs", Object.class);
/*spheroid                          1111        spheroid    */ TYPENAME_TO_CLASS.put("spheroid", Object.class);
        CLASS_TO_TYPENAME.put(String.class, "varchar");
        CLASS_TO_TYPENAME.put(Boolean.class, "bool");
        CLASS_TO_TYPENAME.put(boolean.class, "bool");
        CLASS_TO_TYPENAME.put(Short.class, "int2");
        CLASS_TO_TYPENAME.put(short.class, "int2");
        CLASS_TO_TYPENAME.put(Integer.class, "int4");
        CLASS_TO_TYPENAME.put(int.class, "int4");
        CLASS_TO_TYPENAME.put(Long.class, "int8");
        CLASS_TO_TYPENAME.put(long.class, "int8");
        CLASS_TO_TYPENAME.put(Float.class, "float4");
        CLASS_TO_TYPENAME.put(float.class, "float4");
        CLASS_TO_TYPENAME.put(Double.class, "float8");
        CLASS_TO_TYPENAME.put(double.class, "float8");
        CLASS_TO_TYPENAME.put(BigDecimal.class, "");
        CLASS_TO_TYPENAME.put(Date.class, "date");
        CLASS_TO_TYPENAME.put(Time.class, "time");
        CLASS_TO_TYPENAME.put(java.util.Date.class, "timestamp");
        CLASS_TO_TYPENAME.put(Timestamp.class, "timestamp");
        CLASS_TO_TYPENAME.put(byte[].class, "blob");



        //POSTGIS extension
        TYPENAME_TO_CLASS.put("GEOMETRY", Geometry.class);
        TYPENAME_TO_CLASS.put("GEOGRAPHY", Geometry.class);
        TYPENAME_TO_CLASS.put("POINT", Point.class);
        TYPENAME_TO_CLASS.put("POINTM", Point.class);
        TYPENAME_TO_CLASS.put("LINESTRING", LineString.class);
        TYPENAME_TO_CLASS.put("LINESTRINGM", LineString.class);
        TYPENAME_TO_CLASS.put("POLYGON", Polygon.class);
        TYPENAME_TO_CLASS.put("POLYGONM", Polygon.class);
        TYPENAME_TO_CLASS.put("MULTIPOINT", MultiPoint.class);
        TYPENAME_TO_CLASS.put("MULTIPOINTM", MultiPoint.class);
        TYPENAME_TO_CLASS.put("MULTILINESTRING", MultiLineString.class);
        TYPENAME_TO_CLASS.put("MULTILINESTRINGM", MultiLineString.class);
        TYPENAME_TO_CLASS.put("MULTIPOLYGON", MultiPolygon.class);
        TYPENAME_TO_CLASS.put("MULTIPOLYGONM", MultiPolygon.class);
        TYPENAME_TO_CLASS.put("GEOMETRYCOLLECTION", GeometryCollection.class);
        TYPENAME_TO_CLASS.put("GEOMETRYCOLLECTIONM", GeometryCollection.class);
        
        CLASS_TO_TYPENAME.put(Geometry.class, "GEOMETRY");
        CLASS_TO_TYPENAME.put(Point.class, "POINT");
        CLASS_TO_TYPENAME.put(LineString.class, "LINESTRING");
        CLASS_TO_TYPENAME.put(Polygon.class, "POLYGON");
        CLASS_TO_TYPENAME.put(MultiPoint.class, "MULTIPOINT");
        CLASS_TO_TYPENAME.put(MultiLineString.class, "MULTILINESTRING");
        CLASS_TO_TYPENAME.put(MultiPolygon.class, "MULTIPOLYGON");
        CLASS_TO_TYPENAME.put(GeometryCollection.class, "GEOMETRYCOLLECTION");

        //postgis 1+ geometry and referencing
        IGNORE_TABLES.add("spatial_ref_sys");
        IGNORE_TABLES.add("geometry_columns");
        IGNORE_TABLES.add("geography_columns");
        //postgis 2 raster
        IGNORE_TABLES.add("raster_columns");
        IGNORE_TABLES.add("raster_overviews");
        
    }
        
    private final DefaultJDBCFeatureStore datastore;

    public PostgresDialect(DefaultJDBCFeatureStore datastore) {
        this.datastore = datastore;
    }
    
    @Override
    public String getTableEscape() {
        return "\"";
    }

    @Override
    public Class getJavaType(int sqlType, String sqlTypeName) {
        
        Class c = null;
        
        if(sqlType == Types.ARRAY){
            //special case for array types
            if(sqlTypeName.startsWith("_")){
                sqlTypeName = sqlTypeName.substring(1);
            }
            c = TYPENAME_TO_CLASS.get(sqlTypeName);
            if(c == null){
                c = Object.class;
            }
            c = Array.newInstance(c, 0).getClass();
        }else{
            c = TYPENAME_TO_CLASS.get(sqlTypeName);
            if(c == null){
                //try relying on base type.
                c = TYPE_TO_CLASS.get(sqlType);
            }
        }
        
        if(c == null){
            datastore.getLogger().log(Level.INFO, "No definied mapping for type : {0} {1}", new Object[]{sqlType, sqlTypeName});
            c = Object.class;
        }
        return c;
    }

    @Override
    public String getSQLType(Class javaType) throws SQLException{
        final String sqlName = CLASS_TO_TYPENAME.get(javaType);
        if(sqlName == null) throw new SQLException("No database mapping for type "+ javaType);
        return sqlName;
    }

    @Override
    public String getColumnSequence(Connection cx, String schemaName, String tableName, String columnName) throws SQLException {
        final Statement st = cx.createStatement();
        try {
            final StringBuilder sb = new StringBuilder();
            sb.append("SELECT pg_get_serial_sequence('\"");
            if (schemaName != null && !schemaName.isEmpty()){
                sb.append(schemaName).append("\".\"");
            }
            sb.append(tableName).append("\"', '");
            sb.append(columnName).append("')");
            final String sql = sb.toString();
            datastore.getLogger().fine(sql);
            final ResultSet rs = st.executeQuery(sql);
            try {
                if (rs.next()) {
                    return rs.getString(1);
                }
            } finally {
                closeSafe(datastore.getLogger(),rs);
            }
        } finally {
            closeSafe(datastore.getLogger(),st);
        }
        return null;
    }

    @Override
    public boolean ignoreTable(String name) {
        return IGNORE_TABLES.contains(name.toLowerCase());
    }
    
}
