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
package org.geotoolkit.metadata.landsat;

import java.io.File;
import java.io.IOException;
import org.geotoolkit.util.FileUtilities;

/**
 * Convinient methods to manipulate LandSat informations.
 * 
 * @author Johann Sorel (Geomatys)
 */
public final class LandSat {
    
    private LandSat(){}
    
    public static LandSatMetaNode parseMetadata(final File file) throws IOException{
        
        final String metaFile = FileUtilities.getStringFromFile(file);        
        final String[] lines = metaFile.split("\n");
        
        
        //rebuild the metadata tree
        LandSatMetaNode root = null;
        LandSatMetaNode node = null;
        
        for(int i=0; i<lines.length;i++){
            String line = lines[i];
            line = line.trim();
            if(line.isEmpty()) continue;
            
            final int separator = line.indexOf('=');
            
            if(separator < 0){
                //might be the END tag
                if("END".equalsIgnoreCase(line)){
                    //ok we have finish
                    break;
                }else{
                    //unexpected
                    throw new IOException("Line "+i+" does not match metadata pattern {key = value} : "+ line);
                }
            }
            
            final String key = line.substring(0, separator).trim();
            final String value = line.substring(separator+1).trim();
            
            if("GROUP".equalsIgnoreCase(key)){
                final LandSatMetaNode candidate = new LandSatMetaNode(key, value);
                if(node != null){
                    node.add(candidate);
                }else{
                    root = candidate;
                }
                node = candidate;
            }else if("END_GROUP".equalsIgnoreCase(key)){
                
                //end this node, check the name match, 
                //otherwise it means the file is incorrect
                if(!value.equalsIgnoreCase(String.valueOf(node.getValue()))){
                    throw new IOException("End Group line "+i+" does not match any previous group. "+ line);
                }
                node = (LandSatMetaNode) node.getParent();
            }else{
                //simple key=value pair
                final LandSatMetaNode candidate = new LandSatMetaNode(key, value);
                node.add(candidate);
            }
        }
        
        return root;
    }
    
    public static LandSatNomenclature parseNomenclature(final String name){
        if(name == null || name.length() < 24){
            throw new IllegalArgumentException("name is too short to match lansat naming convention");
        }
        
        //TODO
        throw new IllegalArgumentException("not coded yet.");
    }
    
}
