/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotoolkit.gml.xml;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.temporal.object.ISODateParser;
import org.opengis.temporal.TemporalPosition;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlTransient
public abstract class AbstractTimePosition {

    protected static final Logger LOGGER = Logging.getLogger("org.geotoolkit.gml.xml");

    protected static final List<DateFormat> FORMATTERS = new ArrayList<DateFormat>();

    static {
        FORMATTERS.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS"));
        FORMATTERS.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
        FORMATTERS.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        FORMATTERS.add(new SimpleDateFormat("yyyy-MM-dd"));
        FORMATTERS.add(new SimpleDateFormat("yyyy"));
    }

    public TemporalPosition anyOther() {
        return null;
    }

    public abstract Date getDate();

    protected Date parseDate(final String value) {
        if (value != null && !value.isEmpty()) {

            //test iso date first
            final ISODateParser parser = new ISODateParser();
            try {
                return parser.parseToDate(value);
            }catch(NumberFormatException ex){
            }
            //fallback types
            for (DateFormat df : FORMATTERS) {
                try {
                    synchronized (df) {
                        return df.parse(value);
                    }
                } catch (ParseException ex) {
                    continue;
                }
            }
        }
        LOGGER.log(Level.WARNING, "Unable to parse date value:{0}", value);
        return null;
    }

    public abstract TimeIndeterminateValueType getIndeterminatePosition();
}
