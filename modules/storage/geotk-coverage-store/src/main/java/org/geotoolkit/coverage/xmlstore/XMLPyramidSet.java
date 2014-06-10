/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012-2014, Geomatys
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
package org.geotoolkit.coverage.xmlstore;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.xml.bind.annotation.*;

import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.Classes;
import org.geotoolkit.coverage.AbstractPyramidSet;
import org.geotoolkit.coverage.Pyramid;
import org.geotoolkit.coverage.grid.ViewType;
import org.geotoolkit.gui.swing.tree.Trees;
import org.geotoolkit.image.io.XImageIO;
import org.geotoolkit.referencing.IdentifiedObjects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XMLPyramidSet extends AbstractPyramidSet{

    public static final String GEOPHYSICS = "geophysics";
    public static final String NATIVE = "native";
    

    @XmlElement(name="Pyramid")
    private List<XMLPyramid> pyramids;

    @XmlTransient
    private ImageReaderSpi spi;
    @XmlTransient
    private XMLCoverageReference ref;

    public XMLPyramidSet() {
    }

    public String getFormatName() {
        final String format = ref.getPreferredFormat();
        if(format!=null && !format.isEmpty()) return format;
        return ref.getPackMode().equals(ViewType.GEOPHYSICS) ? "PostGISWKBraster" : "PNG";
    }

    public XMLCoverageReference getRef() {
        return ref;
    }

    public void setRef(XMLCoverageReference ref) {
        this.ref = ref;
    }

    public ImageReaderSpi getReaderSpi() throws DataStoreException{
        if(spi == null){
            try {
                final ImageReader reader = XImageIO.getReaderByFormatName(getFormatName(), null, Boolean.TRUE, Boolean.TRUE);
                spi = reader.getOriginatingProvider();
                reader.dispose();
            } catch (IOException ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            }
        }
        return spi;
    }

    public List<XMLPyramid> pyramids() {
        if(pyramids == null){
            pyramids = new ArrayList<>();
        }
        return pyramids;
    }

    @Override
    public String getId() {
        return ref.getId();
    }

    @Override
    public Collection<Pyramid> getPyramids() {
        return (Collection)pyramids();
    }

    @Override
    public List<String> getFormats() {
        final String format = getFormatName();
        switch (format) {
            case "JPEG": return Arrays.asList("image/jpeg");
            case "PNG": return Arrays.asList("image/png");
            case "PostGISWKBraster" : return Arrays.asList("application/wkb"); // better mime type ?
            default : throw new IllegalStateException("unexpected pyramid format");
        }
    }

    @Override
    public String toString(){
        return Trees.toString(Classes.getShortClassName(this)+" "+getId(), getPyramids());
    }

    /**
     * Create and register a new pyramid in the set.
     *
     * @param crs The {@link org.opengis.referencing.crs.CoordinateReferenceSystem} for the image data of the pyramid.
     * @return The newly created pyramid.
     * @throws org.apache.sis.storage.DataStoreException If the given CRS is null or invalid.
     */
    Pyramid createPyramid(CoordinateReferenceSystem crs) throws DataStoreException {
        final XMLPyramid pyramid = new XMLPyramid(crs);
        try {
            pyramid.id = URLEncoder.encode(IdentifiedObjects.getIdentifier(crs),"UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new DataStoreException("No valid identifier can be created from given CRS.", ex);
        }
        pyramid.initialize(this);
        pyramids().add(pyramid);
        return pyramid;
    }

}
