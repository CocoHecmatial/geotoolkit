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
import org.geotoolkit.data.iso8211.Field;
import org.geotoolkit.data.iso8211.SubField;
import static org.geotoolkit.data.s57.S57Constants.*;
import static org.geotoolkit.data.s57.model.S57ModelObject.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DataSetHistory extends S57ModelObject {
    
    //7.3.3 Data set history record structure
    public static final String DSHT = "DSHT";
    public static final String DSHT_RCNM = "RCNM";
    public static final String DSHT_RCID = "RCID";
    public static final String DSHT_PRCO = "PRCO";
    public static final String DSHT_ESDT = "ESDT";
    public static final String DSHT_LSDT = "LSDT";
    public static final String DSHT_DCRT = "DCRT";
    public static final String DSHT_CODT = "CODT";
    public static final String DSHT_COMT = "COMT";
    
    public RecordType type;
    public long id;
    public Agency agency;
    public String earliestSourceDate;
    public String latestSourceDate;
    public String collectionCriteria;
    public String compilationDate;
    public String comment;
    
    @Override
    public void read(Field isofield) throws IOException {
        for(SubField sf : isofield.getSubFields()){
            final String tag = sf.getType().getTag();
            final Object val = sf.getValue();
                 if (DSHT_RCNM.equalsIgnoreCase(tag)) type = RecordType.valueOf(val);
            else if (DSHT_RCID.equalsIgnoreCase(tag)) id = toLong(val);
            else if (DSHT_PRCO.equalsIgnoreCase(tag)) agency = Agency.valueOf(val);
            else if (DSHT_ESDT.equalsIgnoreCase(tag)) earliestSourceDate = toString(val);
            else if (DSHT_LSDT.equalsIgnoreCase(tag)) latestSourceDate = toString(val);
            else if (DSHT_DCRT.equalsIgnoreCase(tag)) collectionCriteria = toString(val);
            else if (DSHT_CODT.equalsIgnoreCase(tag)) compilationDate = toString(val);
            else if (DSHT_COMT.equalsIgnoreCase(tag)) comment = toString(val);
        }
    }
    
}
