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
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.geotoolkit.coverage.*;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.coverage.io.GridCoverageWriter;
import org.geotoolkit.storage.DataStoreException;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class XMlCoverageReference implements CoverageReference, PyramidalModel{

    private final XMLCoverageStore store;
    private final Name name;
    private final XMLPyramidSet set;


    public XMlCoverageReference(XMLCoverageStore store, Name name, XMLPyramidSet set) {
        this.store = store;
        this.name = name;
        this.set = set;
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public boolean isWritable() throws DataStoreException {
        //TODO
        return false;
    }

    @Override
    public int getImageIndex() {
        return 0;
    }
        
    @Override
    public GridCoverageWriter createWriter() throws DataStoreException {
        //TODO
        throw new DataStoreException("Not supported yet.");
    }

    @Override
    public XMLCoverageStore getStore() {
        return store;
    }

    @Override
    public GridCoverageReader createReader() throws CoverageStoreException {
        final PyramidalModelReader reader = new PyramidalModelReader();
        reader.setInput(this);
        return reader;
    }

    @Override
    public XMLPyramidSet getPyramidSet() throws DataStoreException {
        return set;
    }

    @Override
    public boolean isWriteable() {
        return true;
    }

    @Override
    public Pyramid createPyramid(CoordinateReferenceSystem crs) throws DataStoreException {
        final XMLPyramidSet set = getPyramidSet();
        save();
        return set.createPyramid(crs);
    }

    @Override
    public GridMosaic createMosaic(String pyramidId, Dimension gridSize,
    Dimension tilePixelSize, Point2D upperleft, double pixelscale) throws DataStoreException {
        final XMLPyramidSet set = getPyramidSet();
        final XMLPyramid pyramid = set.getPyramid(pyramidId);
        final XMLMosaic mosaic = pyramid.createMosaic(gridSize, tilePixelSize, upperleft, pixelscale);
        save();
        return mosaic;
    }

    @Override
    public void writeTile(String pyramidId, String mosaicId, int col, int row, RenderedImage image) throws DataStoreException {
        final XMLPyramidSet set = getPyramidSet();
        final XMLPyramid pyramid = set.getPyramid(pyramidId);
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
            Logger.getLogger(XMlCoverageReference.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    @Override
    public void writeTiles(String pyramidId, String mosaicId, RenderedImage image, boolean onlyMissing) throws DataStoreException {
        final XMLPyramidSet set = getPyramidSet();
        final XMLPyramid pyramid = set.getPyramid(pyramidId);
        final XMLMosaic mosaic = pyramid.getMosaic(mosaicId);
        mosaic.writeTiles(image,onlyMissing);
        save();
    }

    public Image getLegend() throws DataStoreException {
        return null;
    }

}
