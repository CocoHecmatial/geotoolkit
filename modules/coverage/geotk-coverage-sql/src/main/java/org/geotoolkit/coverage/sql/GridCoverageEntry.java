/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2005-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2007-2010, Geomatys
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
package org.geotoolkit.coverage.sql;

import java.awt.Dimension;
import java.awt.geom.Dimension2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Date;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.Matrix;

import org.geotoolkit.lang.Immutable;
import org.geotoolkit.image.io.IIOListeners;
import org.geotoolkit.coverage.GridSampleDimension;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.grid.GridGeometry2D;
import org.geotoolkit.coverage.grid.GeneralGridEnvelope;
import org.geotoolkit.referencing.crs.DefaultTemporalCRS;
import org.geotoolkit.referencing.operation.transform.ProjectiveTransform;
import org.geotoolkit.util.Utilities;
import org.geotoolkit.util.DateRange;
import org.geotoolkit.util.NumberRange;
import org.geotoolkit.util.logging.Logging;
import org.geotoolkit.internal.sql.table.Entry;
import org.geotoolkit.internal.sql.table.IllegalRecordException;
import org.geotoolkit.resources.Errors;


/**
 * Implementation of {@linkplain GridCoverageReference coverage reference}.
 * This implementation is immutable and thread-safe.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @author Sam Hiatt
 * @version 3.10
 *
 * @since 3.10 (derived from Seagis)
 * @module
 */
@Immutable
final class GridCoverageEntry extends Entry implements GridCoverageReference {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5725249398707248625L;

    /**
     * The grid geometry. Include the image size (in pixels), the horizontal envelope
     * and the vertical ordinate values.
     */
    private final GridGeometryEntry geometry;

    /**
     * The grid geometry as a {@link GridGeometry2D} object.
     * Will be created only when first needed.
     */
    private transient GridGeometry2D geometry2D;

    /**
     * Image start time, inclusive.
     */
    private final long startTime;

    /**
     * Image end time, exclusive.
     */
    private final long endTime;

    /**
     * Creates an entry containing coverage information (but not yet the coverage itself).
     *
     * @param  identifier The identifier of this grid geometry.
     * @param  geometry   The geometry of the grid, including CRS informations.
     * @param  startTime  The coverage start time, or {@code null} if none.
     * @param  endTime    The coverage end time, or {@code null} if none.
     * @param  comments   Optional remarks, or {@code null} if none.
     */
    protected GridCoverageEntry(final GridCoverageIdentifier identifier, final GridGeometryEntry geometry,
            final Date startTime, final Date endTime, final String comments) throws SQLException
    {
        super(identifier, comments);
        this.geometry  = geometry;
        this.startTime = (startTime != null) ? startTime.getTime() : Long.MIN_VALUE;
        this.  endTime = (  endTime != null) ?   endTime.getTime() : Long.MAX_VALUE;
        if (geometry.isEmpty() || this.startTime > this.endTime) {
            throw new IllegalRecordException(Errors.format(Errors.Keys.EMPTY_ENVELOPE));
        }
    }

    /**
     * Returns the identifier of this {@code GridCoverageReference}.
     */
    final GridCoverageIdentifier getIdentifier() {
        return (GridCoverageIdentifier) identifier;
    }

    /**
     * Returns a name for the coverage, for use in graphical user interfaces.
     */
    @Override
    public String getName() {
        final GridCoverageIdentifier identifier = getIdentifier();
        final StringBuilder buffer = new StringBuilder(identifier.filename);
        final int index = identifier.imageIndex;
        if (index != 0) {
            buffer.append(':').append(index);
        }
        return buffer.toString();
    }

    /**
     * Returns the path to the image file as an object of the given type.
     */
    @Override
    public <T> T getFile(final Class<T> type) throws IOException {
        final Object input;
        final GridCoverageIdentifier identifier = getIdentifier();
        if (type.isAssignableFrom(File.class)) {
            input = identifier.file();
        } else {
            final boolean isURL = type.isAssignableFrom(URL.class);
            if (isURL || type.isAssignableFrom(URI.class)) try {
                final URI uri = identifier.uri();
                input = isURL ? uri.toURL() : uri;
            } catch (URISyntaxException e) {
                throw new IOException(e);
            } else {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UNKNOWN_TYPE_$1, type));
            }
        }
        return type.cast(input);
    }

    /**
     * Returns the source as a {@link File} or an {@link URI}, in this preference order.
     * This method never returns {@code null}; if the URI can not be created, then an
     * exception is thrown.
     */
    final Object getInput() throws URISyntaxException {
        final GridCoverageIdentifier identifier = getIdentifier();
        final File file = identifier.file();
        if (file.isAbsolute()) {
            return file;
        }
        return identifier.uri();
    }

    /**
     * Returns the image format.
     */
    @Override
    public String getImageFormat() {
        return getIdentifier().series.format.imageFormat;
    }

    /**
     * Returns the native Coordinate Reference System of the coverage.
     * The returned CRS may be up to 4-dimensional.
     */
    @Override
    public CoordinateReferenceSystem getSpatioTemporalCRS(final boolean includeTime) {
        return geometry.getSpatioTemporalCRS(includeTime);
    }

    /**
     * Returns the geographic bounding box of the {@linkplain #getEnvelope coverage envelope}.
     */
    @Override
    public GeographicBoundingBox getGeographicBoundingBox() {
        try {
            return geometry.getGeographicBoundingBox();
        } catch (TransformException e) {
            // Returning 'null' is allowed by the method contract.
            Logging.recoverableException(GridCoverageReference.class, "getGeographicBoundingBox", e);
            return null;
        }
    }

    /**
     * Returns the spatio-temporal envelope of the coverage.
     */
    @Override
    public Envelope getEnvelope() {
        return getGridGeometry().getEnvelope();
    }

    /**
     * Returns the range of values in the third dimension, which may be vertical or temporal.
     * This method returns the range in units of the database vertical or temporal CRS, which
     * may not be the same than the vertical or temporal CRS of the coverage.
     */
    @Override
    public NumberRange<Double> getZRange() {
        double min = geometry.standardMinZ;
        double max = geometry.standardMaxZ;
        if (!(min <= max)) { // Use '!' for catching NaN values.
            min = Double.NEGATIVE_INFINITY;
            max = Double.POSITIVE_INFINITY;
            final DefaultTemporalCRS temporalCRS = geometry.getTemporalCRS();
            if (temporalCRS != null) {
                if (startTime != Long.MIN_VALUE) min = temporalCRS.toValue(new Date(startTime));
                if (  endTime != Long.MAX_VALUE) max = temporalCRS.toValue(new Date(  endTime));
            }
        }
        return NumberRange.create(min, true, max, false);
    }

    /**
     * Returns the temporal part of the {@linkplain #getEnvelope coverage envelope}.
     */
    @Override
    public DateRange getTimeRange() {
        return new DateRange((startTime != Long.MIN_VALUE) ? new Date(startTime) : null, true,
                               (endTime != Long.MAX_VALUE) ? new Date(  endTime) : null, false);
    }

    /**
     * Returns the coverage grid geometry.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public synchronized GridGeometry2D getGridGeometry() {
        if (geometry2D == null) {
            /*
             * If the grid coverage has a temporal dimension, we need to set the scale and offset
             * coefficients for it. Those coefficients need to be set on a coverage-by-coverage
             * basis since they are typically different for each coverage even if they share the
             * same GridGeometryEntry.
             */
            double min = Double.NEGATIVE_INFINITY;
            double max = Double.POSITIVE_INFINITY;
            final DefaultTemporalCRS temporalCRS = geometry.getTemporalCRS();
            if (temporalCRS != null) {
                if (startTime != Long.MIN_VALUE) min = temporalCRS.toValue(new Date(startTime));
                if (  endTime != Long.MAX_VALUE) max = temporalCRS.toValue(new Date(  endTime));
            }
            final boolean hasTime = !Double.isInfinite(min) || !Double.isInfinite(max);
            final CoordinateReferenceSystem crs = geometry.getSpatioTemporalCRS(hasTime);
            final int dimension = crs.getCoordinateSystem().getDimension();
            final Matrix gridToCRS = geometry.getGridToCRS(dimension,
                    geometry.indexOfNearestAltitude(getIdentifier().z));
            if (hasTime) {
                gridToCRS.setElement(dimension-1, dimension-1, max - min);
                gridToCRS.setElement(dimension-1, dimension, min);
            }
            /*
             * At this point, the 'gridToCRS' matrix has been built.
             * Now, compute the GridEnvelope.
             */
            final Dimension size = geometry.getImageSize();
            final int[] lower = new int[dimension];
            final int[] upper = new int[dimension];
            switch (dimension) {
                default: Arrays.fill(upper, 2, dimension, 1); // Fall through for every cases.
                case 2:  upper[1] = size.height;
                case 1:  upper[0] = size.width;
                case 0:  break;
            }
            geometry2D = new GridGeometry2D(new GeneralGridEnvelope(lower, upper, false),
                    geometry.getPixelInCell(), ProjectiveTransform.create(gridToCRS), crs, null);
        }
        return geometry2D;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GridSampleDimension[] getSampleDimensions() {
        final List<GridSampleDimension> sd;
        try {
            sd = getIdentifier().series.format.getSampleDimensions();
        } catch (SQLException e) {
            // Returning 'null' is allowed by the method contract.
            Logging.recoverableException(GridCoverageReference.class, "getSampleDimensions", e);
            return null;
        }
        final GridSampleDimension[] bands = sd.toArray(new GridSampleDimension[sd.size()]);
        for (int i=0; i<bands.length; i++) {
            bands[i] = bands[i].geophysics(true);
        }
        return bands;
    }

    /**
     * Loads the data and returns the coverage.
     *
     * @todo Not yet implemented.
     */
    @Override
    public GridCoverage2D getCoverage(IIOListeners listeners) throws IOException, CancellationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Abort the image reading.
     *
     * @todo Not yet implemented.
     */
    @Override
    public void abort() {
    }

    /**
     * Returns {@code true} if the coverage represented by this entry has enough resolution
     * compared to the requested one. If this method doesn't have suffisient information,
     * then it conservatively returns {@code true}.
     *
     * @param requested The requested resolution in units of the database horizontal CRS.
     */
    final boolean hasEnoughResolution(final Dimension2D requested) {
        if (requested != null) try {
            final Dimension2D resolution = geometry.getStandardResolution();
            if (resolution != null) {
                return resolution.getWidth()  <= requested.getWidth()  + GridGeometryEntry.EPS &&
                       resolution.getHeight() <= requested.getHeight() + GridGeometryEntry.EPS;
            }
        } catch (TransformException e) {
            Logging.recoverableException(GridCoverageEntry.class, "hasEnoughResolution", e);
        }
        return true;
    }

    /**
     * If two grid coverages have the same spatio-temporal envelope, return the one having the
     * coarsest resolution. If this method can not select an entry, it returns {@code null}.
     */
    final GridCoverageEntry selectCoarseResolution(final GridCoverageEntry that) {
        if (startTime == that.startTime && endTime == that.endTime && geometry.sameEnvelope(that.geometry)) {
            final Dimension size1 = this.geometry.getImageSize();
            final Dimension size2 = that.geometry.getImageSize();
            if (size1.width <= size2.width && size1.height <= size2.height) return this;
            if (size1.width >= size2.width && size1.height >= size2.height) return that;
        }
        return null;
    }

    /**
     * Compares two entries on the same criterion than the one used in the SQL {@code "ORDER BY"}
     * statement of {@link GridCoverageTable}). Entries without date are treated as unordered.
     */
    final boolean equalsAsSQL(final GridCoverageEntry other) {
        if (startTime == Long.MIN_VALUE && endTime == Long.MAX_VALUE) {
            return false;
        }
        return endTime == other.endTime;
    }

    /**
     * Compares this entry with the given object for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (super.equals(object)) {
            final GridCoverageEntry that = (GridCoverageEntry) object;
            return this.startTime == that.startTime &&
                   this.endTime   == that.endTime   &&
                   Utilities.equals(this.geometry, that.geometry);
        }
        return false;
    }
}
