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
import java.util.List;
import org.geotoolkit.data.iso8211.Field;
import org.geotoolkit.data.iso8211.SubField;
import static org.geotoolkit.data.s57.S57Constants.*;
import static org.geotoolkit.data.s57.model.S57ModelObject.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class VectorRecord extends S57ModelObject {
    
    //7.7.1.1 Vector record identifier field structure
    public static final String VRID = "VRID";
    public static final String VRID_RCNM = "RCNM"; 
    public static final String VRID_RCID = "RCID"; 
    public static final String VRID_RVER = "RVER"; 
    public static final String VRID_RUIN = "RUIN"; 
    
    public RCNM type;
    public long id;
    public int version;
    public String updateInstruction;
    
    public List<Attribute> attributes;
    public RecordPointerControl recordPointerControl;
    public List<RecordPointer> records;
    public CoordinateControl coordinateControl;
    public List<Coordinate2D> coords2D;
    public List<Coordinate3D> coords3D;
    public List<Arc> arcs;
    
    public static class Attribute extends S57ModelObject {
        //7.7.1.2 Vector record attribute field structure
        public static final String VRID_ATTV = "ATTV"; 
        public static final String VRID_ATTV_ATTL = "ATTL"; 
        public static final String VRID_ATTV_ATVL = "ATVL";
        
        public int code;
        public String value;
        
        @Override
        public void read(Field isofield) throws IOException {
            for(SubField sf : isofield.getSubFields()){
                final String tag = sf.getType().getTag();
                final Object val = sf.getValue();
                     if(VRID_ATTV_ATTL.equalsIgnoreCase(tag)) code = toInteger(val);
                else if(VRID_ATTV_ATVL.equalsIgnoreCase(tag)) value = toString(val);
            }
        }
        
    }
    
    public static class RecordPointerControl extends S57ModelObject {
        //7.7.1.3 Vector record pointer control field structure
        public static final String VRID_VRPC = "VRPC"; 
        public static final String VRID_VRPC_VPUI = "VPUI"; 
        public static final String VRID_VRPC_VPIX = "VPIX";
        public static final String VRID_VRPC_NVPT = "NVPT";
    }
    
    public static class RecordPointer extends S57ModelObject {
        //7.7.1.4 Vector record pointer field structure
        public static final String VRID_VRPT = "VRPT"; 
        public static final String VRID_VRPT_NAME = "NAME"; 
        public static final String VRID_VRPT_ORNT = "ORNT"; 
        public static final String VRID_VRPT_USAG = "USAG"; 
        public static final String VRID_VRPT_TOPI = "TOPI"; 
        public static final String VRID_VRPT_MASK = "MASK"; 
        
        public String name;
        public String orientation;
        public String usage;
        public String topology;
        public String masking;
        
        @Override
        public void read(Field isofield) throws IOException {
            for(SubField sf : isofield.getSubFields()){
                final String tag = sf.getType().getTag();
                final Object value = sf.getValue();
                     if(VRID_VRPT_NAME.equalsIgnoreCase(tag)) name = toString(value);
                else if(VRID_VRPT_ORNT.equalsIgnoreCase(tag)) orientation = toString(value);
                else if(VRID_VRPT_USAG.equalsIgnoreCase(tag)) usage = toString(value);
                else if(VRID_VRPT_TOPI.equalsIgnoreCase(tag)) topology = toString(value);
                else if(VRID_VRPT_MASK.equalsIgnoreCase(tag)) masking = toString(value);
            }
        }
        
    }
    
    public static class CoordinateControl extends S57ModelObject {
        //7.7.1.5 Coordinate control field structure
        public static final String VRID_SGCC = "SGCC"; 
        public static final String VRID_SGCC_CCUI = "CCUI"; 
        public static final String VRID_SGCC_CCIX = "CCIX"; 
        public static final String VRID_SGCC_CCNG = "CCNG"; 
    }
    
    public static class Coordinate2D extends S57ModelObject {
        //7.7.1.6 2-D Coordinate field structure
        public static final String VRID_SG2D = "SG2D"; 
        public static final String VRID_SG2D_YCOO = "YCOO"; 
        public static final String VRID_SG2D_XCOO = "XCOO";
        
        public double x;
        public double y;

        @Override
        public void read(Field isofield) throws IOException {
            for(SubField sf : isofield.getSubFields()){
                final String tag = sf.getType().getTag();
                final Object value = sf.getValue();
                     if(VRID_SG2D_YCOO.equalsIgnoreCase(tag)) y = toDouble(value);
                else if(VRID_SG2D_XCOO.equalsIgnoreCase(tag)) x = toDouble(value);
            }
        }
        
    }
    
    public static class Coordinate3D extends S57ModelObject {
        //7.7.1.7 3-D Coordinate field structure
        public static final String VRID_SG3D = "SG3D";
        public static final String VRID_SG3D_YCOO = "YCOO";
        public static final String VRID_SG3D_XCOO = "XCOO";
        public static final String VRID_SG3D_VE3D = "VE3D";
        
        public double x;
        public double y;
        public double z;
        @Override
        public void read(Field isofield) throws IOException {
            for(SubField sf : isofield.getSubFields()){
                final String tag = sf.getType().getTag();
                final Object value = sf.getValue();
                     if (VRID_SG3D_YCOO.equalsIgnoreCase(tag)) y = toDouble(value);
                else if (VRID_SG3D_XCOO.equalsIgnoreCase(tag)) x = toDouble(value);
                else if (VRID_SG3D_VE3D.equalsIgnoreCase(tag)) z = toDouble(value);
            }
        }
        
    }
    
    public static class Arc extends S57ModelObject {
        //7.7.1.8 Arc/Curve definition field structure
        public static final String VRID_ARCC = "ARCC";
        public static final String VRID_ARCC_ATYP = "ATYP";
        public static final String VRID_ARCC_SURF = "SURF";
        public static final String VRID_ARCC_ORDR = "ORDR";
        public static final String VRID_ARCC_RESO = "RESO";
        public static final String VRID_ARCC_FPMF = "FPMF";
        
        public String type;
        public String surface;
        public int order;
        public double resolution;
        public int factor;
        
        @Override
        public void read(Field isofield) throws IOException {
            for(SubField sf : isofield.getSubFields()){
                final String tag = sf.getType().getTag();
                final Object value = sf.getValue();
                     if (VRID_ARCC_ATYP.equalsIgnoreCase(tag)) type = toString(value);
                else if (VRID_ARCC_SURF.equalsIgnoreCase(tag)) surface = toString(value);
                else if (VRID_ARCC_ORDR.equalsIgnoreCase(tag)) order = toInteger(value);
                else if (VRID_ARCC_RESO.equalsIgnoreCase(tag)) resolution = toDouble(value);
                else if (VRID_ARCC_FPMF.equalsIgnoreCase(tag)) factor = toInteger(value);
            }
        }
        
        public static class Arc2D extends S57ModelObject {
            //7.7.1.9 Arc coordinates field structure
            public static final String VRID_ARCC_AR2D = "AR2D";
            public static final String VRID_ARCC_AR2D_ENPT = "STPT";
            public static final String VRID_ARCC_AR2D_CDPM = "CTPT";
            public static final String VRID_ARCC_AR2D_CDPR = "ENPT";
            public static final String VRID_ARCC_AR2D_YCOO = "YCOO";
            public static final String VRID_ARCC_AR2D_XCOO = "XCOO";
        }

        public static class Ellipse2D extends S57ModelObject {
            //7.7.1.10 Ellipse coordinates field structure
            public static final String VRID_ARCC_EL2D = "EL2D";
            public static final String VRID_ARCC_EL2D_STPT = "STPT";
            public static final String VRID_ARCC_EL2D_CTPT = "CTPT";
            public static final String VRID_ARCC_EL2D_ENPT = "ENPT";
            public static final String VRID_ARCC_EL2D_CDPM = "CDPM";
            public static final String VRID_ARCC_EL2D_CDPR = "CDPR";
            public static final String VRID_ARCC_EL2D_YCOO = "YCOO";
            public static final String VRID_ARCC_EL2D_XCOO = "XCOO";
        }

        public static class Curve2D extends S57ModelObject {
            //7.7.1.11 Curve coordinates field structure
            public static final String VRID_ARCC_CT2D = "CT2D";
            public static final String VRID_ARCC_CT2D_YCOO = "YCOO";
            public static final String VRID_ARCC_CT2D_XCOO = "XCOO";
        }
        
    }
    
    @Override
    public void read(Field isofield) throws IOException {
        for(SubField sf : isofield.getSubFields()){
            final String tag = sf.getType().getTag();
            final Object value = sf.getValue();
                 if (VRID_RCNM.equalsIgnoreCase(tag)) type = RCNM.read(value);
            else if (VRID_RCID.equalsIgnoreCase(tag)) id = toLong(value);
            else if (VRID_RVER.equalsIgnoreCase(tag)) version = toInteger(value);
            else if (VRID_RUIN.equalsIgnoreCase(tag)) updateInstruction = toString(value);
        }
        for(Field f : isofield.getFields()){
            final String tag = f.getType().getTag();
            if(Attribute.VRID_ATTV.equalsIgnoreCase(tag)){
                if(attributes==null) attributes = new ArrayList<Attribute>();
                final Attribute candidate = new Attribute();
                candidate.read(f);
                attributes.add(candidate);
            }else if(RecordPointerControl.VRID_VRPC.equalsIgnoreCase(tag)){
                recordPointerControl = new RecordPointerControl();
                recordPointerControl.read(f);
            }else if(RecordPointer.VRID_VRPT.equalsIgnoreCase(tag)){
                if(records==null) records = new ArrayList<RecordPointer>();
                final RecordPointer candidate = new RecordPointer();
                candidate.read(f);
                records.add(candidate);
            }else if(CoordinateControl.VRID_SGCC.equalsIgnoreCase(tag)){
                coordinateControl = new CoordinateControl();
                coordinateControl.read(f);
            }else if(Coordinate2D.VRID_SG2D.equalsIgnoreCase(tag)){
                if(coords2D==null) coords2D = new ArrayList<Coordinate2D>();                
                final Coordinate2D candidate = new Coordinate2D();
                candidate.read(f);
                coords2D.add(candidate);
            }else if(Coordinate3D.VRID_SG3D.equalsIgnoreCase(tag)){
                if(coords3D==null) coords3D = new ArrayList<Coordinate3D>();
                final Coordinate3D candidate = new Coordinate3D();
                candidate.read(f);
                coords3D.add(candidate);
            }else if(Arc.VRID_ARCC.equalsIgnoreCase(tag)){
                if(arcs==null) arcs = new ArrayList<Arc>();
                final Arc candidate = new Arc();
                candidate.read(f);
                arcs.add(candidate);
            }
        }
    }
        
}
