/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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
package org.geotoolkit.wmts.model;

import java.util.logging.Level;
import org.geotoolkit.storage.coverage.DefaultPyramid;
import org.apache.sis.referencing.CRS;
import org.geotoolkit.wmts.xml.v100.*;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.util.logging.Logging;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class WMTSPyramid extends DefaultPyramid{

    private final TileMatrixSetLink link;
    private final TileMatrixSet matrixset;
    private CoordinateReferenceSystem crs;

    public WMTSPyramid(final WMTSPyramidSet set, final TileMatrixSetLink link){
        super(set, null);
        this.link = link;
        matrixset = set.getCapabilities().getContents().getTileMatrixSetByIdentifier(link.getTileMatrixSet());

        final String crsstr = matrixset.getSupportedCRS();
        try {
            // WMTS is made for display like WMS, so longitude is expected to be on the X axis.
            // Note : this is not written in the spec.
            crs = AbstractCRS.castOrCopy(CRS.forCode(crsstr)).forConvention(AxesConvention.RIGHT_HANDED);
        } catch (NoSuchAuthorityCodeException ex) {
            try {
                crs = CRS.forCode("EPSG:"+crsstr);
            } catch (Exception e) {
                e.addSuppressed(ex);
                Logging.getLogger("org.geotoolkit.wmts.model").log(Level.WARNING, null, e);
            }
        } catch (FactoryException ex) {
            Logging.getLogger("org.geotoolkit.wmts.model").log(Level.WARNING, null, ex);
        }

        final TileMatrixSetLimits limits = link.getTileMatrixSetLimits();

        for (final TileMatrix matrix : matrixset.getTileMatrix()) {

            TileMatrixLimits limit = null;
            if(limits != null){
                for(TileMatrixLimits li : limits.getTileMatrixLimits()){
                    if(li.getTileMatrix().equals(matrix.getIdentifier().getValue())){
                        limit = li;
                        break;
                    }
                }
            }

            final WMTSMosaic mosaic = new WMTSMosaic(this, matrix, limit);
            getMosaicsInternal().add(mosaic);
        }

    }

    public TileMatrixSet getMatrixset() {
        return matrixset;
    }

    @Override
    public WMTSPyramidSet getPyramidSet() {
        return (WMTSPyramidSet) super.getPyramidSet();
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

}
