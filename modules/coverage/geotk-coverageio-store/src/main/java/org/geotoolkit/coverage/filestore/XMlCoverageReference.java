/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Geomatys
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
package org.geotoolkit.coverage.filestore;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.coverage.AbstractPyramidalModel;
import org.geotoolkit.coverage.GridMosaic;
import org.geotoolkit.coverage.GridSampleDimension;
import org.geotoolkit.coverage.Pyramid;

import org.opengis.feature.type.Name;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class XMLCoverageReference extends AbstractPyramidalModel {

    private final XMLPyramidSet set;


    public XMLCoverageReference(XMLCoverageStore store, Name name, XMLPyramidSet set) {
        super(store,name,0);
        this.set = set;
    }

    @Override
    public boolean isWritable() throws DataStoreException {
        return true;
    }

    @Override
    public XMLPyramidSet getPyramidSet() throws DataStoreException {
        return set;
    }

    @Override
    public Pyramid createPyramid(CoordinateReferenceSystem crs) throws DataStoreException {
        final XMLPyramidSet set = getPyramidSet();
        save();
        return set.createPyramid(crs);
    }

    @Override
    public void deletePyramid(String pyramidId) throws DataStoreException {
        throw new DataStoreException("Not supported yet.");
    }
    
    @Override
    public GridMosaic createMosaic(String pyramidId, Dimension gridSize,
    Dimension tilePixelSize, DirectPosition upperleft, double pixelscale) throws DataStoreException {
        final XMLPyramidSet set = getPyramidSet();
        final XMLPyramid pyramid = (XMLPyramid) set.getPyramid(pyramidId);
        final XMLMosaic mosaic = pyramid.createMosaic(gridSize, tilePixelSize, upperleft, pixelscale);
        save();
        return mosaic;
    }

    @Override
    public void deleteMosaic(String pyramidId, String mosaicId) throws DataStoreException {
        throw new DataStoreException("Not supported yet.");
    }
    
    @Override
    public void writeTile(String pyramidId, String mosaicId, int col, int row, RenderedImage image) throws DataStoreException {
        final XMLPyramidSet set = getPyramidSet();
        final XMLPyramid pyramid = (XMLPyramid) set.getPyramid(pyramidId);
        final XMLMosaic mosaic = pyramid.getMosaic(mosaicId);
        mosaic.createTile(col,row,image);
        save();
    }

    /**
     * Save the pyramid set in the file
     * @throws DataStoreException
     */
    synchronized void save() throws DataStoreException{
        final XMLPyramidSet set = getPyramidSet();
        try {
            set.write();
        } catch (JAXBException ex) {
            Logger.getLogger(XMLCoverageReference.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    @Override
    public void writeTiles(String pyramidId, String mosaicId, RenderedImage image, boolean onlyMissing) throws DataStoreException {
        final XMLPyramidSet set = getPyramidSet();
        final XMLPyramid pyramid = (XMLPyramid) set.getPyramid(pyramidId);
        final XMLMosaic mosaic = pyramid.getMosaic(mosaicId);
        mosaic.writeTiles(image,onlyMissing);
        save();
    }

    @Override
    public void deleteTile(String pyramidId, String mosaicId, int col, int row) throws DataStoreException {
        throw new DataStoreException("Not supported yet.");
    }

    @Override
    public List<GridSampleDimension> getSampleDimensions(int index) throws DataStoreException {
        return null;
    }

    @Override
    public void createSampleDimension(List<GridSampleDimension> dimensions, final Map<String, Object> analyse) throws DataStoreException {
        throw new DataStoreException("Writing not supported.");
    }

    

}
