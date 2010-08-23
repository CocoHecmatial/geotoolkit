/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2010, Geomatys
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
package org.geotoolkit.coverage.io;

import java.util.Locale;
import java.util.logging.LogRecord;
import javax.imageio.spi.ImageReaderWriterSpi;
import java.awt.geom.AffineTransform;
import java.awt.Dimension;

import org.opengis.util.InternationalString;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import org.geotoolkit.lang.Static;
import org.geotoolkit.resources.Loggings;
import org.geotoolkit.resources.Vocabulary;
import org.geotoolkit.util.converter.Classes;
import org.geotoolkit.internal.image.io.Formats;
import org.geotoolkit.coverage.AbstractCoverage;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.referencing.AbstractIdentifiedObject;

import static org.geotoolkit.coverage.io.GridCoverageStore.LOGGER;
import static org.geotoolkit.coverage.io.GridCoverageStore.X_DIMENSION;
import static org.geotoolkit.coverage.io.GridCoverageStore.Y_DIMENSION;
import static org.geotoolkit.coverage.io.GridCoverageStore.fixRoundingError;


/**
 * Static utilities methods for use by {@link ImageCoverageReader} and {@link ImageCoverageStore}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.15
 *
 * @since 3.15
 * @module
 */
@Static
final class ImageCoverageStore {
    /**
     * Do not allow instantiation of this class.
     */
    private ImageCoverageStore() {
    }

    /**
     * Logs a "Created encoder|decoder of class Foo" message. This method can
     * be invoked from {@code setInput} or {@code setOutput} methods only.
     *
     * @param store  The object which is invoking this method.
     * @param caller The caller class (not necessarily {@code object.getClass()}.
     * @param codec  The object for which to write the class name.
     * @param The provider (for image reader/writer), or {@code null}.
     */
    static void logCodecCreation(final GridCoverageStore store, final Class<?> caller,
            final Object codec, final ImageReaderWriterSpi spi)
    {
        assert caller.isInstance(store) : caller;
        final boolean write = (store instanceof GridCoverageWriter);
        final Locale locale = store.locale;
        String message = Loggings.getResources(locale).getString(
                Loggings.Keys.CREATED_CODEC_OF_CLASS_$2, write ? 1 : 0, codec.getClass().getName());
        if (spi != null) {
            final StringBuilder buffer = new StringBuilder(message).append('\n');
            Formats.formatDescription(spi, locale, buffer);
            message = buffer.toString();
        }
        final LogRecord record = new LogRecord(store.level, message);
        record.setLoggerName(LOGGER.getName());
        record.setSourceClassName(caller.getName());
        record.setSourceMethodName(write ? "setOutput" : "setInput");
        LOGGER.log(record);
    }

    /**
     * Logs a read or write operation. This method can be invoked from
     * {@code read} or {@code write} methods only.
     *
     * @param store      The object which is invoking this method.
     * @param caller     The caller class (not necessarily {@code object.getClass()}.
     * @param coverage   The coverage read or written.
     * @param actualSize The actual image size (may be different than the coverage grid envelope),
     *                   or {@code null} to compute it from the grid envelope.
     * @param crs        The coordinate reference system (may be different than the coverage CRS),
     *                   or {@code null} for the coverage CRS.
     * @param timeNanos  The elapsed execution time, in nanoseconds.
     * @param destToExtractedGrid The transform from the destination grid to the extracted source
     *                   grid, or {@code null}.
     */
    static void logOperation(
            final GridCoverageStore   store,
            final Class<?>            caller,
            final GridCoverage        coverage,
            final Dimension           actualSize,
            CoordinateReferenceSystem crs,
            final MathTransform2D     destToExtractedGrid,
            final long                timeNanos)
    {
        assert caller.isInstance(store) : caller;
        final Locale locale = store.locale;
        /*
         * Get the coverage name, or "untitled" if unknown.
         */
        InternationalString name = null;
        if (coverage instanceof AbstractCoverage) {
            name = ((AbstractCoverage) coverage).getName();
        }
        if (name == null) {
            name = Vocabulary.formatInternational(Vocabulary.Keys.UNTITLED);
        }
        /*
         * Get the view types.
         */
        final String viewTypes;
        if (coverage instanceof GridCoverage2D) {
            viewTypes = ((GridCoverage2D) coverage).getViewTypes().toString();
        } else {
            viewTypes = Vocabulary.getResources(locale).getString(Vocabulary.Keys.NONE);
        }
        /*
         * Get the coverage dimension.
         */
        final GridEnvelope ge = coverage.getGridGeometry().getGridRange();
        final int dimension = ge.getDimension();
        final StringBuilder buffer = new StringBuilder();
        for (int i=0; i<dimension; i++) {
            int span = ge.getSpan(i);
            if (actualSize != null) {
                switch (i) {
                    case X_DIMENSION: span = actualSize.width;  break;
                    case Y_DIMENSION: span = actualSize.height; break;
                }
            }
            if (i != 0) {
                buffer.append(" \u00D7 ");
            }
            buffer.append(span);
        }
        final String size = buffer.toString();
        /*
         * Get the coordinate reference system.
         */
        String crsName = null;
        if (crs == null) {
            crs = coverage.getCoordinateReferenceSystem();
        }
        if (crs != null) {
            buffer.setLength(0);
            String t = AbstractIdentifiedObject.getName(crs, null);
            if (t != null) {
                buffer.append(t);
            }
            final ReferenceIdentifier id = AbstractIdentifiedObject.getIdentifier(crs, null);
            if (id != null) {
                buffer.append(" (").append(id).append(')');
            }
            if (buffer.length() != 0) {
                crsName = buffer.toString();
            }
        }
        if (crsName == null) {
            crsName = Vocabulary.getResources(locale).getString(Vocabulary.Keys.UNDEFINED);
        }
        /*
         * Get the "source to destination" transform. We will format affine transform
         * in a special way. Usually, we have only scale and translation terms.
         */
        String transform = null;
        if (destToExtractedGrid != null && !destToExtractedGrid.isIdentity()) try {
            if (destToExtractedGrid instanceof AffineTransform) {
                final AffineTransform tr = (AffineTransform) destToExtractedGrid.inverse();
                if (tr.getShearX() == 0 && tr.getShearY() == 0) {
                    buffer.setLength(0);
                    transform = buffer.append("AffineTransform[scale=(")
                            .append(fixRoundingError(tr.getScaleX())).append(", ")
                            .append(fixRoundingError(tr.getScaleY())).append("), translation=(")
                            .append(fixRoundingError(tr.getTranslateX())).append(", ")
                            .append(fixRoundingError(tr.getTranslateY())).append(")]").toString();
                } else {
                    // The 'new' is for avoiding AffineTransform2D.toString().
                    transform = new AffineTransform(tr).toString();
                }
            } else {
                transform = Classes.getShortClassName(destToExtractedGrid);
            }
        } catch (NoninvertibleTransformException e) {
            transform = e.toString();
        }
        if (transform == null) {
            transform = Vocabulary.getResources(locale).getString(Vocabulary.Keys.NONE);
        }
        /*
         * Put everything in a log record.
         */
        final boolean write = (store instanceof GridCoverageWriter);
        final LogRecord record = Loggings.getResources(locale).getLogRecord(
                store.level, Loggings.Keys.COVERAGE_STORE_$7, new Object[] {
                        write ? 1 : 0, name.toString(locale), viewTypes,
                        size, crsName, transform, timeNanos / 1E+6
        });
        record.setLoggerName(LOGGER.getName());
        record.setSourceClassName(caller.getName());
        record.setSourceMethodName(write ? "write" : "read");
        LOGGER.log(record);
    }
}
