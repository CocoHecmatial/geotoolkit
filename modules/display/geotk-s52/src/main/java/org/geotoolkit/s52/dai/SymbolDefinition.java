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
 * Symbol Definition.
 * Defines several symbol‑parameters.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SymbolDefinition extends DAIField{

    /** A(8) : name of the symbol; */
    public String SYNM;
    /** A(1) : type of symbol definition:
     * V  Vector definition
     * R  Raster definition */
    public String SYDF;
    /** I(5) : pivot‑point's column‑number;
     * SYCL is counted from the top, left corner of the vector/raster space to the right;
     * ‑9999(left)<= SYCL <= 32767(right) */
    public int SYCL;
    /** I(5) : pivot‑point's row‑number;
     * PROW is counted from the top, left corner of the vector/raster space to the bottom ;
     * -9999(top)<= SYRW <= 32767(bottom) */
    public int SYRW;
    /** I(5) : width of bounding box;
     * where 1<= PAHL <=128 for raster and where 1<= PAHL <=32767 for vector
     * Note: does not include vector line width */
    public int SYHL;
    /** I(5) : height of bounding box;
     * where 1<= PAVL <=128 for raster and where 1<= PAGL <=32767 for vector
     * Note: does not include vector line width */
    public int SYVL;
    /** I(5) : bounding box upper left column number;
     * where 1<= SBXC <=128 for raster and where 1<= SBXC <=32767 for vector */
    public int SBXC;
    /** I(5) : bounding box upper left row number;
     * where 1<= SBXR <=128 for raster and where 1<= SBXR <=32767 for vector */
    public int SBXR;

    public SymbolDefinition() {
        super("SYMD");
    }

    @Override
    protected void readSubFields(String str) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
