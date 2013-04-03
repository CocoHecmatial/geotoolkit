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
package org.geotoolkit.data.s57.iso8211;

import java.util.Arrays;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class SubField {
    
    private final SubFieldDescription type;
    private Object value;

    public SubField(SubFieldDescription type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public int setValue(byte[] buffer, int offset){
        Integer length = type.getLength();
        
        switch(type.getType()){
            case TEXT:
                if(length!=null){
                    value = new String(Arrays.copyOfRange(buffer, offset, offset+length));
                    return length;
                }else{
                    //find the end
                    if(length == null){
                        for(length=0;length+offset<buffer.length;length++){
                            if(   ISO8211Constants.FEND == buffer[length+offset] 
                               || ISO8211Constants.SFEND == buffer[length+offset]){
                                break;
                            }
                        }
                    }
                    value = new String(Arrays.copyOfRange(buffer, offset, offset+length));
                    return length+1; //+1 for the delimiter
                }
                
            case INTEGER:
                value = ISO8211Utilities.readSignedInteger(buffer, offset, 4);
                return 4;
            case REAL_FIXED:
                value = ISO8211Utilities.readUnsignedInteger(buffer, offset, 2);
                return 2;
            case REAL_FLOAT:
                value = ISO8211Utilities.readReal(buffer, offset, 4);
                return 4;
            case LOGICAL:
                value = (buffer[offset]!=0);
                return 1;
            case INTEGER_UNSIGNED:
                value = ISO8211Utilities.readUnsignedInteger(buffer, offset, length);
                return length;
            case INTEGER_SIGNED:
                value = ISO8211Utilities.readSignedInteger(buffer, offset, length);
                return length;
            case REAL:
                value = ISO8211Utilities.readReal(buffer, offset,length);
                return length;
            case BINARY:
                value = Arrays.copyOfRange(buffer, offset, offset+length);
                return length;
        }
        return 0;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(type.getTag()).append(" : ").append(value);
        return sb.toString();
    }
    
}
