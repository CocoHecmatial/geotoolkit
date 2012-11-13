/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Geomatys
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

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.geotoolkit.coverage.DefaultPyramid;
import org.geotoolkit.coverage.DefaultPyramidSet;
import org.geotoolkit.coverage.Pyramid;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.geometry.GeneralDirectPosition;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.referencing.cs.DiscreteReferencingFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class PGPyramidSet extends DefaultPyramidSet{
    
    private final PGCoverageReference ref;
    private boolean updated = false;

    public PGPyramidSet(PGCoverageReference ref) {
        this.ref = ref;
    }

    void mustUpdate(){
        updated = false;
    }
    
    @Override
    public Collection<Pyramid> getPyramids() {
        if(!updated){
            updateModel();
        }
        return super.getPyramids();
    }
    
    /**
     * Explore pyramids and rebuild model
     */
    private synchronized void updateModel(){
        if(updated) return;
        super.getPyramids().clear();
        final PGCoverageStore store = ref.getStore();
        
        Connection cnx = null;
        Statement stmt = null;
        ResultSet rs = null;
        try{
            cnx = store.getDataSource().getConnection();
            stmt = cnx.createStatement();
            final int layerId = store.getLayerId(ref.getName().getLocalPart());
            
            final StringBuilder query = new StringBuilder();        
            query.append("SELECT p.id, p.epsg FROM ");
            query.append(store.encodeTableName("Pyramid"));
            query.append(" as p ");
            query.append(" WHERE p.\"layerId\" = ");
            query.append(layerId);
            
            final Map<Integer,String> map = new HashMap<Integer, String>();
            rs = stmt.executeQuery(query.toString());
            while(rs.next()){
                map.put(rs.getInt(1),rs.getString(2));
            }
            store.closeSafe(null,stmt,rs);
            for(Entry<Integer,String> entry : map.entrySet()){
                super.getPyramids().add(readPyramid(cnx, entry.getKey(), entry.getValue()));
            }
            
        }catch(SQLException ex){
            store.getLogger().log(Level.WARNING, ex.getMessage(),ex);
        }catch(FactoryException ex){
            store.getLogger().log(Level.WARNING, ex.getMessage(),ex);
        }finally{
            store.closeSafe(cnx, stmt, rs);
        }
        
        updated = true;
    }
    
    private Pyramid readPyramid(final Connection cnx, final int pyramidId, final String epsgcode) throws SQLException, FactoryException{
        
        final CoordinateReferenceSystem crs = ref.getStore().getEPSGFactory().createCoordinateReferenceSystem(epsgcode);
        DefaultPyramid pyramid = new DefaultPyramid(String.valueOf(pyramidId), this, crs);
        final PGCoverageStore store = ref.getStore();
        
        Statement stmt = null;
        ResultSet rs = null;
        try{            
            stmt = cnx.createStatement();
            
            //get mosaic additional axis values to recreate discret axis
            final SortedSet[] discretValues = new SortedSet[crs.getCoordinateSystem().getDimension()];
            for(int i=0;i<discretValues.length;i++){
                discretValues[i] = new TreeSet();
            }
            
            StringBuilder query = new StringBuilder();        
            query.append("SELECT ma.\"indice\", ma.\"value\" FROM ");
            query.append(store.encodeTableName("Mosaic"));
            query.append(" AS m , ");
            query.append(store.encodeTableName("MosaicAxis"));
            query.append(" AS ma WHERE m.\"pyramidId\" = ");
            query.append(pyramidId);
            query.append(" AND ma.\"mosaicId\" = m.\"id\"");
            
            rs = stmt.executeQuery(query.toString());
            while(rs.next()){
                final int indice = rs.getInt(1);
                final double value = rs.getDouble(2);
                discretValues[indice].add(value);
            }
            store.closeSafe(rs);
            
            final double[][] table = new double[discretValues.length][0];
            for(int i=0;i<discretValues.length;i++){
                final Object[] ds = discretValues[i].toArray();
                final double[] vals = new double[ds.length];
                for(int k=0;k<ds.length;k++){
                    vals[k] = (Double)ds[k];
                }
                table[i] = vals;
            }
            
            final CoordinateReferenceSystem dcrs = DiscreteReferencingFactory.createDiscreteCRS(crs, table);
            pyramid = new DefaultPyramid(String.valueOf(pyramidId), this, dcrs);
            
            query = new StringBuilder();        
            query.append("SELECT \"id\",\"upperCornerX\",\"upperCornerY\",\"gridWidth\",\"gridHeight\",\"scale\",\"tileWidth\",\"tileHeight\" FROM ");
            query.append(store.encodeTableName("Mosaic"));
            query.append(" WHERE \"pyramidId\" = ");
            query.append(pyramidId);
            
            rs = stmt.executeQuery(query.toString());
            while(rs.next()){
                final long mosaicId = rs.getLong(1);
                final double cornerX = rs.getDouble(2);
                final double cornerY = rs.getDouble(3);
                final int gridWidth = rs.getInt(4);
                final int gridHeight = rs.getInt(5);
                final double scale = rs.getDouble(6);
                final int tileWidth = rs.getInt(7);
                final int tileHeight = rs.getInt(8);
                
                final GeneralDirectPosition position = new GeneralDirectPosition(crs);
                position.setOrdinate(0, cornerX);
                position.setOrdinate(1, cornerY);
                
                final PGGridMosaic mosaic = new PGGridMosaic(ref, 
                        mosaicId, pyramid, position, 
                        new Dimension(gridWidth, gridHeight),
                        new Dimension(tileWidth, tileHeight), scale);
                pyramid.getMosaics().put(scale, mosaic);
            }
            
        }catch(SQLException ex){
            store.getLogger().log(Level.WARNING, ex.getMessage(),ex);
        }finally{
            store.closeSafe(null, stmt, rs);
        }
        
        return pyramid;
    }
        
}
