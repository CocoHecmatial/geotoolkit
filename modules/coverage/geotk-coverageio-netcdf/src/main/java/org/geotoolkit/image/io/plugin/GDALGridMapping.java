/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.image.io.plugin;

import java.util.logging.Level;
import java.awt.geom.AffineTransform;
import javax.vecmath.MismatchedSizeException;

import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.geotoolkit.util.Strings;
import org.geotoolkit.util.Localized;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.resources.Errors;
import org.geotoolkit.image.io.WarningProducer;
import org.geotoolkit.internal.image.io.Warnings;


/**
 * Utilities methods specific to the GDAL library.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.19
 *
 * @since 3.19
 * @module
 */
final class GDALGridMapping {
    /**
     * The Coordinate Reference System parsed by the constructor, or {@code null} if none.
     */
    CoordinateReferenceSystem crs;

    /**
     * The <cite>grid to CRS</cite> parsed by the constructor, or {@code null} if none.
     */
    AffineTransform gridToCRS;

    /**
     * Parses the given WKT and "grid to CRS" transform, if non-null.
     *
     * @param  caller The caller (can not be null).
     * @param  The CRs Well Known Text, or {@code null}.
     * @param  geoTransform The GDAL "GeoTransform", or {@code null}.
     */
    GDALGridMapping(final WarningProducer caller, final String wkt, final String geoTransform) {
        if (wkt != null) try {
            crs = CRS.parseWKT(wkt);
        } catch (FactoryException e) {
            Warnings.log(caller, Level.WARNING, caller.getClass(), "parseWKT", e);
        }
        if (geoTransform != null) try {
            gridToCRS = getGeoTransform(caller, Strings.parseDoubles(geoTransform, ' '));
        } catch (RuntimeException e) { // NumberFormatException & MismatchedSizeException.
            Warnings.log(caller, Level.WARNING, caller.getClass(), "getGeoTransform", e);
        }
    }

    /**
     * Creates an affine transform from the given GDAL GeoTransform coefficients.
     * Those coefficients are not in the usual order expected by matrix, affine
     * transforms or TFW files. The relationship from pixel/line (P,L) coordinates
     * to CRS are:
     *
     * {@preformat math
     *     X = c[0] + P*c[1] + L*c[2];
     *     Y = c[3] + P*c[4] + L*c[5];
     * }
     *
     * @param  caller The caller (can not be null).
     * @param  c The GDAL coefficients as an array of length 6.
     * @return The affine transform for the given coefficients.
     */
    private static AffineTransform getGeoTransform(final Localized caller, final double... c) {
        if (c.length != 6) {
            throw new MismatchedSizeException(Errors.getResources(caller.getLocale())
                    .getString(Errors.Keys.MISMATCHED_ARRAY_LENGTH));
        }
        return new AffineTransform(c[1], c[4], c[2], c[5], c[0], c[3]);
    }
}
