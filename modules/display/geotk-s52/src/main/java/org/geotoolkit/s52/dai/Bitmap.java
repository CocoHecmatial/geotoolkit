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
package org.geotoolkit.s52.dai;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contains one row of a raster image;
 * Each pixel is represented by a letter (ASCII>=64);
 * The letter represents a color token defined within the PCRF.CTOK subfield.
 * The letter '@'represents a 100% transparent pixel(TRNSP).
 * Note: PBTM and PVCT are mutual exclusive.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class Bitmap extends DAIField{

    /** A(1/15) : Raster Bit‑map Row ‑ maximal length 122 characters;
     * The length shall be identical to content of PAHL‑field. */
    public String BITR;

    protected Bitmap(String code) {
        super(code);
    }

    @Override
    public Map<String, Object> getSubFields() {
        final Map<String,Object> map = new LinkedHashMap<>();
        map.put("BITR", BITR);
        return map;
    }

    @Override
    protected void readSubFields(String str) {
        final int[] offset = new int[1];
        BITR = readStringByDelim(str, offset, DELIM_1F, true); //be tolerance, delimiter missing in some files
    }

}
