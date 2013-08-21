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

/**
 * Contains a vector image definition;
 * Colors are identified by a letter (ASCII>=64);
 * The letter represents a color token defined within the PCRF.CTOK subfield.
 * The letter '@'identifies a fully transparent color.
 * Note: PVCT and PBTM are mutual exclusive.
 *
 * @author Johann Sorel (Geomatys)
 */
public class Vector extends DAIField{

    /** A(1/15) : String of vector commands; */
    public String VECD;

    public Vector(String code) {
        super(code);
    }

    @Override
    protected void readSubFields(String str) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
