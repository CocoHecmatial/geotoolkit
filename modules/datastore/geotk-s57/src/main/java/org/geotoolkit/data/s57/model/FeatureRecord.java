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
package org.geotoolkit.data.s57.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.geotoolkit.data.iso8211.Field;
import org.geotoolkit.data.iso8211.SubField;
import static org.geotoolkit.data.s57.S57Constants.*;
import org.geotoolkit.data.s57.S62Agency;
import static org.geotoolkit.data.s57.model.S57Object.*;
import org.geotoolkit.io.LEDataInputStream;

/**
 * S-57 Feature Record.
 * 
 * @author Johann Sorel (Geomatys)
 */
public class FeatureRecord extends S57Object {
 
    //7.5.3.2 Feature record identifier field structure
    public static final String FRID = "FRID";
    public static final String FRID_RCNM = "RCNM"; 
    public static final String FRID_RCID = "RCID"; 
    /** object geometric primitive*/
    public static final String FRID_PRIM = "PRIM"; 
    /** The “Group” [GRUP] subfield is used to separate feature objects into groups. The definition of groups is
     * dependent on the product specification (see Appendix B – Product Specifications). If a feature object does
     * not belong to a group, the subfield must be left empty (see clause 2.1). */
    public static final String FRID_GRUP = "GRUP"; 
    /** The numeric object label/code of the object class from the IHO Object Catalogue is encoded in the “Object
     * Label/Code” [OBJL] subfield. */
    public static final String FRID_OBJL = "OBJL"; 
    /** record version */
    public static final String FRID_RVER = "RVER"; 
    /** record update instruction */
    public static final String FRID_RUIN = "RUIN"; 
    
    /** RCNM */
    public Primitive primitiveType;
    /** GRUP */
    public int group;
    /** OBJL */
    public int code;
    /** RVER */
    public int version;
    /** RUIN */
    public UpdateInstruction updateInstruction;
    /** FOID */
    public Identifier identifier;
    /** ATTF */
    public final List<Attribute> attributes = new ArrayList<Attribute>();
    /** NATF */
    public final List<NationalAttribute> nattributes = new ArrayList<NationalAttribute>();
    /** FFPC */
    public ObjectPointerControl objectControl;
    /** FFPT */
    public final List<ObjectPointer> objectPointers = new ArrayList<ObjectPointer>();
    /** FSPC */
    public SpatialPointerControl spatialControl;
    /** FSPT */
    public final List<SpatialPointer> spatialPointers = new ArrayList<SpatialPointer>();
    
    public static class Identifier extends S57Object {
        //7.6.2 Feature object identifier field structure
        public static final String FRID_FOID = "FOID"; 
        public static final String FRID_FOID_AGEN = "AGEN"; 
        /** The “Feature Object Identification Number” ranges from 1 to (2^32)-2. The “Feature Object Identification
        * Subdivision” ranges from 1 to (2^16)-2. Both subfields are used to create an unique key for a feature object
        * produced by the agency encoded in the AGEN subfield. The usage of the FIDN and FIDS subfields is not
        * constrained and must be defined by the encoder. */
        public static final String FRID_FOID_FIDN = "FIDN"; 
        public static final String FRID_FOID_FIDS = "FIDS"; 
        
        public S62Agency agency;
        public int number;
        public int subdivision;
        
        @Override
        public void read(Field isofield) throws IOException {
            for(SubField sf : isofield.getSubFields()){
                final String tag = sf.getType().getTag();
                final Object value = sf.getValue();
                     if (FRID_FOID_AGEN.equals(tag)) agency = S62Agency.valueOf(value);
                else if (FRID_FOID_FIDN.equals(tag)) number = toInteger(value);
                else if (FRID_FOID_FIDS.equals(tag)) subdivision = toInteger(value);
            }
        }
        
    }
    
    public static class Attribute extends BaseAttribute {
        //7.6.3 Feature record attribute field structure
        /** 4.4
        * Attributes of feature objects must be encoded in the “Feature Record Attribute” [ATTF] field (see clause
        * 7.6.3). The numeric attribute label/code of the attribute from the IHO Object Catalogue is encoded in the
        * “Attribute Label/Code” [ATTL] subfield. In both the ASCII and binary implementations, the “Attribute Value”
        * subfield [ATVL] must be a string of characters terminated by the subfield terminator (1/15). Lexical level
        * 0 or 1 may be used for the general text in the ATTF field (see clause 2.4). */
        public static final String FRID_ATTF = "ATTF"; 
        public static final String FRID_ATTF_ATTL = "ATTL"; 
        public static final String FRID_ATTF_ATVL = "ATVL"; 
        
        @Override
        protected String getKeyTag() {
            return FRID_ATTF_ATTL;
        }

        @Override
        protected String getValueTag() {
            return FRID_ATTF_ATVL;
        }
    }

    public static class NationalAttribute extends BaseAttribute {
        //7.6.4 Feature record national attribute field structure
        /** 4.5
        * National attributes of feature objects must be encoded in the “Feature Record National Attribute” [NATF]
        * field (see clause 7.6.4). The numeric attribute label/code of the national attribute from the IHO Object
        * Catalogue is encoded in the “Attribute Label/Code” [ATTL] subfield. In both the ASCII and binary
        * implementations, the “Attribute Value” subfield [ATVL] must be a string of characters terminated by the
        * appropriate subfield terminator (see clause 2.5). All lexical levels may be used for the general text in the
        * NATF field (see clause 2.4). */
        public static final String FRID_NATF = "NATF"; 
        public static final String FRID_NATF_ATTL = "ATTL"; 
        public static final String FRID_NATF_ATVL = "ATVL"; 
                
        @Override
        protected String getKeyTag() {
            return FRID_NATF_ATTL;
        }

        @Override
        protected String getValueTag() {
            return FRID_NATF_ATVL;
        }
        
    }
    
    public static class ObjectPointerControl extends BaseControl {
        //7.6.5 Feature record to feature object pointer control field structure
        public static final String FRID_FFPC = "FFPC"; 
        public static final String FRID_FFPC_FFUI = "FFUI"; 
        public static final String FRID_FFPC_FFIX = "FFIX"; 
        public static final String FRID_FFPC_NFPT = "NFPT"; 
                
        @Override
        protected String getUpdateTag() {
            return FRID_FFPC_FFUI;
        }

        @Override
        protected String getIndexTag() {
            return FRID_FFPC_FFIX;
        }

        @Override
        protected String getNumberTag() {
            return FRID_FFPC_NFPT;
        }
        
    }
    
    public static class ObjectPointer extends S57Object {
        //7.6.6 Feature record to feature object pointer field structure
        /** 4.6
        * The “Feature Record to Feature Object Pointer” [FFPT] field is used to establish a relationship between
        * feature objects. Relationships between feature objects are discussed in detail in chapter 6.
        * The main element of the pointer field is the LNAM subfield (see clause 4.3). The LNAM subfield contains
        * the key of the feature object being referenced (foreign key). The “Relationship Indicator” [RIND] subfield
        * can be used to qualify a relationship (e.g. master or slave relationship) or to add a stacking order to a
        * relationship. */
        public static final String FRID_FFPT = "FFPT"; 
        /** LNAM is composed of AGEN + FIDN + FIDS */
        public static final String FRID_FFPT_LNAM = "LNAM"; 
        public static final String FRID_FFPT_RIND = "RIND"; 
        public static final String FRID_FFPT_COMT = "COMT"; 
                
        //reference id
        public S62Agency agency;
        public long refid;
        public int revision;
        //informations
        public RelationShip relationship;
        public String comment;
        
        @Override
        public void read(Field isofield) throws IOException {
            read(isofield.getSubFields());
        }
        
        public void read(List<SubField> subFields) throws IOException {
            for(SubField sf : subFields){
                final String tag = sf.getType().getTag();
                final Object val = sf.getValue();
                if(FRID_FFPT_LNAM.equals(tag)){
                    if(val instanceof byte[]){
                        final byte[] buffer = (byte[]) val;
                        agency = S62Agency.valueOf(LEDataInputStream.readUnsignedShort(buffer, 0));
                        refid = LEDataInputStream.readUnsignedInt(buffer, 2);
                        revision = LEDataInputStream.readUnsignedShort(buffer, 6);
                    }else{
                        //TODO
                        throw new IOException("ASCII Form for LNAM not supported yet");
                    }
                }
                else if(FRID_FFPT_RIND.equals(tag)) relationship = RelationShip.valueOf(val);
                else if(FRID_FFPT_COMT.equals(tag)) comment = toString(val);
            }
        }
        
    }
    
    public static class SpatialPointerControl extends BaseControl {
        //7.6.7 Feature record to spatial record pointer control field structure
        public static final String FRID_FSPC = "FSPC"; 
        public static final String FRID_FSPC_FSUI = "FSUI"; 
        public static final String FRID_FSPC_FSIX = "FSIX"; 
        public static final String FRID_FSPC_NSPT = "NSPT"; 
        
        @Override
        protected String getUpdateTag() {
            return FRID_FSPC_FSUI;
        }

        @Override
        protected String getIndexTag() {
            return FRID_FSPC_FSIX;
        }

        @Override
        protected String getNumberTag() {
            return FRID_FSPC_NSPT;
        }
        
    }
    
    public static class SpatialPointer extends Pointer {
        //7.6.8 Feature record to spatial record pointer field structure
        public static final String FRID_FSPT = "FSPT"; 
        public static final String FRID_FSPT_NAME = "NAME";
        public static final String FRID_FSPT_ORNT = "ORNT"; 
        public static final String FRID_FSPT_USAG = "USAG"; 
        public static final String FRID_FSPT_MASK = "MASK";
        
        //informations
        public Orientation orientation;
        public Usage usage;
        public Mask mask;
        
        @Override
        public void read(Field isofield) throws IOException {
            read(isofield.getSubFields());
        }
        
        public void read(List<SubField> subFields) throws IOException {
            for(SubField sf : subFields){
                final String tag = sf.getType().getTag();
                final Object val = sf.getValue();
                if(FRID_FSPT_NAME.equals(tag)){
                     if(val instanceof byte[]){
                        final byte[] buffer = (byte[]) val;
                        type = RecordType.valueOf(buffer[0] & 0xff);
                        refid = LEDataInputStream.readUnsignedInt(buffer, 1);
                    }else{
                        //TODO
                        throw new IOException("ASCII Form for NAME not supported yet");
                    }
                }
                else if(FRID_FSPT_ORNT.equals(tag)) orientation = Orientation.valueOf(val);
                else if(FRID_FSPT_USAG.equals(tag)) usage = Usage.valueOf(val);
                else if(FRID_FSPT_MASK.equals(tag)) mask = Mask.valueOf(val);
            }
        }
        
        @Override
        public String toString() {
            return "SP:"+type+","+refid+","+orientation+","+usage+","+mask;
        }
        
    }
    
    @Override
    public void read(Field isofield) throws IOException {
        for(SubField sf : isofield.getSubFields()){
            final String tag = sf.getType().getTag();
            final Object value = sf.getValue();
                 if (FRID_RCNM.equals(tag)) type = RecordType.valueOf(value);
            else if (FRID_RCID.equals(tag)) id = toLong(value);
            else if (FRID_PRIM.equals(tag)) primitiveType = Primitive.valueOf(value);
            else if (FRID_GRUP.equals(tag)) group = toInteger(value);
            else if (FRID_OBJL.equals(tag)) code = toInteger(value);
            else if (FRID_RVER.equals(tag)) version = toInteger(value);
            else if (FRID_RUIN.equals(tag)) updateInstruction = UpdateInstruction.valueOf(value);
        }
        for(Field f : isofield.getFields()){
            final String tag = f.getType().getTag();
            if(Identifier.FRID_FOID.equals(tag)){
                identifier = new Identifier();
                identifier.read(f);
            }else if(Attribute.FRID_ATTF.equals(tag)){
                final Iterator<SubField> sfite = f.getSubFields().iterator();
                while(sfite.hasNext()){
                    final Attribute candidate = new Attribute();
                    candidate.attfLexicalLevel = attfLexicalLevel;
                    candidate.natfLexicalLevel = natfLexicalLevel;
                    candidate.read(Arrays.asList(sfite.next(),sfite.next()));
                    attributes.add(candidate);
                }
            }else if(NationalAttribute.FRID_NATF.equals(tag)){
                final Iterator<SubField> sfite = f.getSubFields().iterator();
                while(sfite.hasNext()){
                    final NationalAttribute candidate = new NationalAttribute();
                    candidate.attfLexicalLevel = attfLexicalLevel;
                    candidate.natfLexicalLevel = natfLexicalLevel;
                    candidate.read(Arrays.asList(sfite.next(),sfite.next()));
                    nattributes.add(candidate);
                }
            }else if(ObjectPointerControl.FRID_FFPC.equals(tag)){
                objectControl = new ObjectPointerControl();
                objectControl.read(f);
            }else if(ObjectPointer.FRID_FFPT.equals(tag)){
                final Iterator<SubField> sfite = f.getSubFields().iterator();
                while(sfite.hasNext()){
                    final ObjectPointer candidate = new ObjectPointer();
                    candidate.read(Arrays.asList(sfite.next(),sfite.next(),sfite.next()));
                    objectPointers.add(candidate);
                }
            }else if(SpatialPointerControl.FRID_FSPC.equals(tag)){
                spatialControl = new SpatialPointerControl();
                spatialControl.read(f);
            }else if(SpatialPointer.FRID_FSPT.equals(tag)){
                final Iterator<SubField> sfite = f.getSubFields().iterator();
                while(sfite.hasNext()){
                    final SpatialPointer candidate = new SpatialPointer();
                    candidate.read(Arrays.asList(sfite.next(),sfite.next(),sfite.next(),sfite.next()));
                    spatialPointers.add(candidate);
                }
            }
        }
    }
    
}
