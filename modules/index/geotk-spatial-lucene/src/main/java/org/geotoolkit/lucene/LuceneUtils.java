/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Geomatys
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
package org.geotoolkit.lucene;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.converter.UnitConverter;
import javax.measure.unit.Unit;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.geotoolkit.geometry.Envelopes;
import org.geotoolkit.geometry.GeneralEnvelope;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import org.geotoolkit.measure.Units;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal
 */
public class LuceneUtils {
    private static final Logger LOGGER = Logging.getLogger(LuceneUtils.class);
    
    public static Directory getAppropriateDirectory(final File indexDirectory) throws IOException {
        
        // for windows
        if (System.getProperty("os.name", "").startsWith("Windows")) {
             return new SimpleFSDirectory(indexDirectory);
             
        // for unix     
        } else {
            final String archModel = System.getProperty("sun.arch.data.model");
            LOGGER.log(Level.FINER, "archmodel:{0}", archModel);
            if ("64".equals(archModel)) {
                return new MMapDirectory(indexDirectory);
            } else {
                return new NIOFSDirectory(indexDirectory);
            }
        }
    }
    
    public static GeneralEnvelope getExtendedReprojectedEnvelope(final Geometry geom, final CoordinateReferenceSystem treeCrs, final String strUnit, final double distance) {
        final GeneralEnvelope bound =  getReprojectedEnvelope(geom, treeCrs);
        
        // add the reprojected distance
        final Unit unit = Units.valueOf(strUnit);
        final UnitConverter converter = unit.getConverterTo(treeCrs.getCoordinateSystem().getAxis(0).getUnit());
        final double rdistance = converter.convert(distance);
        final double minx = bound.getLower(0) - rdistance;
        final double miny = bound.getLower(1) - rdistance;
        final double maxx = bound.getUpper(0) + rdistance;
        final double maxy = bound.getUpper(1) + rdistance;
        bound.setRange(0, minx, maxx);
        bound.setRange(1, miny, maxy);
        return bound;
    }
    
    /**
     * Extract the internal envelope from the geometry and reprojected it to the treeCRS.
     * 
     * @param geom
     * @param treeCrs
     * @return 
     */
    public static GeneralEnvelope getReprojectedEnvelope(final Geometry geom, final CoordinateReferenceSystem treeCrs) {
        final Envelope jtsBound = geom.getEnvelopeInternal();
        final String epsgCode = SRIDGenerator.toSRS(geom.getSRID(), SRIDGenerator.Version.V1);
        try {
            final CoordinateReferenceSystem geomCRS = CRS.decode(epsgCode);
            final GeneralEnvelope bound = new GeneralEnvelope(geomCRS);
            bound.setRange(0, jtsBound.getMinX(), jtsBound.getMaxX());
            bound.setRange(1, jtsBound.getMinY(), jtsBound.getMaxY());

            // reproject to cartesian CRS
            return (GeneralEnvelope) Envelopes.transform(bound, treeCrs);
        } catch (FactoryException ex) {
            LOGGER.log(Level.WARNING, "Factory exception while getting filter geometry crs", ex);
        } catch (TransformException ex) {
            LOGGER.log(Level.WARNING, "Transform exception while reprojecting filter geometry", ex);
        }
        return null;
    }
    
    /**
     * Reproject the envelope in the tree CRS.
     * @param env
     * @param treeCrs
     * @return 
     */
    public static org.opengis.geometry.Envelope getReprojectedEnvelope(final org.opengis.geometry.Envelope env, final CoordinateReferenceSystem treeCrs) {
        try {
            return Envelopes.transform(env, treeCrs);
        } catch (TransformException ex) {
            LOGGER.log(Level.WARNING, "Transform exception while reprojecting filter geometry", ex);
        }
        return null;
    }
    
}
