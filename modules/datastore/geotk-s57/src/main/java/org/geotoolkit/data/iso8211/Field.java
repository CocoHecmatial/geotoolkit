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
package org.geotoolkit.data.iso8211;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.sis.io.TableAppender;
import org.geotoolkit.gui.swing.tree.Trees;
import org.geotoolkit.data.iso8211.ISO8211Constants.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class Field {
    
    private final FieldDescription type;    
    private final List<Field> fields = new ArrayList<Field>();
    private final List<SubField> subfields = new ArrayList<SubField>();

    public Field(FieldDescription type) {
        this.type = type;
    }
    
    /**
     * @return the type
     */
    public FieldDescription getType() {
        return type;
    }

    public List<Field> getFields() {
        return fields;
    }

    public List<SubField> getSubFields(){
        return subfields;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getType().getTag());
        final FieldDataStructure structure = getType().getStructure();
        
        final List lst = new  ArrayList();
        if(structure == FieldDataStructure.ELEMENTARY){
            sb.append(" : ").append(getSubFields().get(0).getValue());
        }else if(structure == FieldDataStructure.LINEAR){
            lst.addAll(subfields);
        }else if(structure == FieldDataStructure.CARTESIAN){
            final List<String[]> names = getType().getSubfieldNames();
            final Iterator<SubField> sfite = subfields.iterator();
            if(names.size()==2){
                //we can make a nice table for display
                final TableAppender tw = new TableAppender();
                tw.writeHorizontalSeparator();
                if(names.get(0).length != 0){
                    //fixed named NxM size
                    //header
                    tw.append("");
                    for(String colname : names.get(1)){
                        tw.append('\t').append(colname);
                    }
                    tw.writeHorizontalSeparator();
                    
                    final Iterator<String> rownames = Arrays.asList(names.get(0)).iterator();
                    for(String rowname : names.get(0)){
                        tw.append(rowname);
                        for(String colname : names.get(1)){
                            tw.append('\t').append(String.valueOf(sfite.next().getValue()));
                        }
                        tw.writeHorizontalSeparator();
                    }
                    
                }else{
                    //infinite XxM unnamed size
                    for(int c=0;c<names.get(1).length;c++){
                        if(c!=0) tw.append('\t');
                        tw.append(names.get(1)[c]);
                    }
                    tw.writeHorizontalSeparator();
                    
                    while(sfite.hasNext()){
                        for(int c=0;c<names.get(1).length;c++){
                            if(c!=0) tw.append('\t');
                            tw.append(String.valueOf(sfite.next().getValue()));
                        }
                        tw.append('\n');
                    }
                    
                }
                tw.writeHorizontalSeparator();
                lst.add(tw.toString());
            }else{
                lst.addAll(subfields);
            }
            
            
        }else{
            //concatenated, how to display this correctly ?
            lst.addAll(subfields);
        }
        
        if(!fields.isEmpty() || subfields.size()>1){
            lst.addAll(fields);
            sb.append(Trees.toString("", lst));
        }
        return sb.toString();
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // IO operations ///////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Parse the given bytearray and rebuild subfields.
     * @param byteValues 
     */
    public void readValues(final byte[] byteValues) throws IOException{
        subfields.clear();
        
        final FieldDataStructure structure = type.getStructure();
        final List<SubFieldDescription> subdescs = type.getSubFieldTypes();
        
        if(structure == FieldDataStructure.ELEMENTARY){
            //parse a single field
            final SubFieldDescription desc = type.getSubFieldTypes().get(0);
            final SubField sf = new SubField(desc);
            sf.readValue(byteValues, 0);
            subfields.add(sf);
            
        }else if(structure == FieldDataStructure.LINEAR){
            //parse subfields
            int offset = 0;
            for(SubFieldDescription desc : subdescs){
                final SubField sf = new SubField(desc);
                int length = sf.readValue(byteValues, offset);
                offset += length;
                subfields.add(sf);
            }
        }else if(structure == FieldDataStructure.CARTESIAN){
            final List<String[]> names = getType().getSubfieldNames();
            if(names.size()>2){
                throw new IOException("Only 2D cartesian array fields supported");
            }
            int offset = 0;
            while(offset<(byteValues.length-1)){
                for(SubFieldDescription desc : subdescs){
                    final SubField sf = new SubField(desc);
                    int length = sf.readValue(byteValues, offset);
                    offset += length;
                    subfields.add(sf);
                }
            }
        }else if(structure == FieldDataStructure.CONCATENATED){
            throw new IOException("Concatenate field value reading not supported ");
        }
        
    }
    
    /**
     * 
     * @param out 
     * @return int : number of bytes written
     */
    public int writeValues(final DataOutput out){
        throw new RuntimeException("No supported yet.");
    }
    
}
