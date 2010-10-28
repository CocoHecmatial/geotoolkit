/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007-2010, Open Source Geospatial Foundation (OSGeo)
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

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import javax.imageio.ImageReader;
import javax.imageio.IIOException;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.metadata.IIOMetadata;

import org.opengis.util.FactoryException;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.coverage.grid.RectifiedGrid;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;

import org.geotoolkit.util.Range;
import org.geotoolkit.util.XArrays;
import org.geotoolkit.util.Localized;
import org.geotoolkit.util.DateRange;
import org.geotoolkit.util.NumberRange;
import org.geotoolkit.util.converter.Classes;
import org.geotoolkit.image.io.mosaic.Tile;
import org.geotoolkit.image.io.metadata.MetadataHelper;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.geotoolkit.image.io.metadata.SampleDimension;
import org.geotoolkit.image.io.XImageIO;
import org.geotoolkit.internal.io.IOUtilities;
import org.geotoolkit.internal.image.io.DimensionAccessor;
import org.geotoolkit.internal.sql.table.SpatialDatabase;
import org.geotoolkit.internal.sql.table.NoSuchRecordException;
import org.geotoolkit.internal.coverage.TransferFunction;
import org.geotoolkit.metadata.iso.citation.Citations;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.referencing.crs.DefaultTemporalCRS;
import org.geotoolkit.referencing.AbstractIdentifiedObject;
import org.geotoolkit.referencing.factory.AbstractAuthorityFactory;
import org.geotoolkit.referencing.cs.DiscreteCoordinateSystemAxis;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.GridSampleDimension;
import org.geotoolkit.coverage.grid.ViewType;
import org.geotoolkit.coverage.Category;
import org.geotoolkit.resources.Errors;

import static org.geotoolkit.internal.image.io.DimensionAccessor.fixRoundingError;


/**
 * A structure which contain the information to be added in the {@linkplain CoverageDatabase
 * Coverage Database} for a new coverage reference. The information provided in this class
 * match closely the layout of the coverage database.
 * <p>
 * Instances of this class are created by {@link Layer#addCoverageReferences(Collection,
 * CoverageDatabaseController)} and dispatched to {@link CoverageDatabaseListener}s. The
 * listeners can modify the field values {@linkplain CoverageDatabaseEvent#isBefore() before}
 * the insertion in the database occurs.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.16
 *
 * @see CoverageDatabaseListener
 *
 * @since 3.12 (derived from Seagis)
 * @module
 */
public final class NewGridCoverageReference {
    /**
     * The authorities of {@link #horizontalSRID} and {@link #verticalSRID} codes, in preference
     * order. The SRID should be the primary keys in the {@code "spatial_ref_sys"} table. If we
     * failed to determine the primary key, we will rely on the observation that the primary key
     * values are often the EPSG codes (but not necessarily). We shall declare here only the
     * authority which are known to use numerical codes.
     */
    private static final Citation[] AUTHORITIES = {
        Citations.POSTGIS,
        Citations.EPSG
    };

    /**
     * The range of sample values to use if no transfer function is defined.
     * Note that the value 0 is reserved for "no data".
     */
    private static final NumberRange<Integer> PACKED_RANGE = NumberRange.create(1, 255);

    /**
     * The originating database.
     */
    private final SpatialDatabase database;

    /**
     * The path to the coverage file (not including the filename), or {@code null} if the filename
     * has no parent directory. The full path to the input file is
     * "{@linkplain #path}/{@linkplain #filename}.{@linkplain #extension}".
     *
     * @see #filename
     * @see #extension
     * @see #getFile()
     */
    public final File path;

    /**
     * The filename, not including the {@linkplain #path} and {@linkplain #extension}.
     *
     * @see #path
     * @see #extension
     * @see #getFile()
     */
    public final String filename;

    /**
     * The filename extension (not including the leading dot), or {@code null} if none.
     *
     * @see #path
     * @see #filename
     * @see #getFile()
     */
    public final String extension;

    /**
     * The zero-based index of the image to be inserted in the database. If there is many
     * images to insert for many different {@linkplain #dateRanges date ranges}, then this
     * is the index of the first image, i.e.:
     * <p>
     * <ul>
     *   <li>The temporal extent of the image at index {@code imageIndex} is
     *       <code>{@linkplain #dateRanges}[0]</code>.</li>
     *   <li>The temporal extent of the image at index {@code imageIndex + 1} is
     *       <code>{@linkplain #dateRanges}[1]</code>.</li>
     *   <li><i>etc.</i></li>
     *   <li>Finally the temporal extent of the image at index {@code imageIndex + n}
     *       is <code>{@linkplain #dateRanges}[n]</code> where <var>n</var> =
     *       {@code dateRanges.length - 1}.</li>
     * </ul>
     *
     * @since 3.16
     */
    public int imageIndex;

    /**
     * The name of the coverage format. It shall be one of the primary key values in the
     * {@code "Formats"} table. Note that this is not necessarily the same name than the
     * {@linkplain ImageReaderSpi#getFormatNames() image format name}.
     * <p>
     * This field is initialized to the format which seems the best fit. A list of
     * alternative formats can be obtained by {@link #getAlternativeFormats()}.
     *
     * @see #isFormatDefined()
     * @see #getAlternativeFormats()
     * @see #refresh()
     */
    public String format;

    /**
     * The sample dimensions for coverages associated with the {@linkplain #format}, or an empty
     * list if undefined. If non-empty, then the list size is equals to the number of bands.
     * <p>
     * Each {@code GridSampleDimension} specifies how to convert pixel values to geophysics values,
     * or conversely. Their type (geophysics or not) is format dependent. For example coverages
     * read from PNG files will typically store their data as integer values (non-geophysics),
     * while coverages read from ASCII files will often store their pixel values as real numbers
     * (geophysics values).
     * <p>
     * The content of this list can be modified in-place.
     *
     * @see #refresh()
     *
     * @since 3.13
     */
    public final List<GridSampleDimension> sampleDimensions;

    /**
     * The format entry which seems the best fit. The {@link #format} field is initialized
     * to the name of this format. The most interesting information from this field is the
     * list of sample dimensions.
     */
    final FormatEntry bestFormat;

    /**
     * Some formats which may be applicable as an alternative to {@code series.format}.
     * This list is created by {@link #getAlternativeFormats()} when first needed. The
     * content shall not be modified after creation.
     */
    private FormatEntry[] alternativeFormats;

    /**
     * The image reader provider.
     */
    private final ImageReaderSpi spi;

    /**
     * The image bounds. The rectangle {@linkplain Rectangle#width width} and
     * {@linkplain Rectangle#height height} must be set to the image size. The
     * ({@linkplain Rectangle#x x},{@linkplain Rectangle#y y}) origin is usually (0,0),
     * but different value are allowed. For example the origin can be set to
     * the {@linkplain Tile#getLocation location of a tile} in tiled images.
     * <p>
     * If the (x,y) origin is different than (0,0), then it will be interpreted as the
     * translation to apply on the grid <em>before</em> to apply the {@link #gridToCRS}
     * transform at reading time.
     * <p>
     * This field is never {@code null}. However users can modify it before the
     * new entry is inserted in the database.
     */
    public final Rectangle imageBounds;

    /**
     * The <cite>grid to CRS</cite> transform, which maps always the pixel
     * {@linkplain PixelOrientation#UPPER_LEFT upper left} corner. This transform
     * does <em>not</em> include the (x,y) translation of the {@link #imageBounds}.
     * <p>
     * This field is never {@code null}. However users can modify it before the
     * new entry is inserted in the database.
     */
    public final AffineTransform gridToCRS;

    /**
     * The horizontal CRS identifier. This shall be the value of a primary key
     * in the {@code "spatial_ref_sys"} PostGIS table. The value may be 0 if this
     * class found no information about the horizontal SRID.
     */
    public int horizontalSRID;

    /**
     * The vertical CRS identifier, ignored if {@link #verticalValues} is {@code null}.
     * When not ignored, this shall be the value of a primary key in the
     * {@code "spatial_ref_sys"} PostGIS table. The value may be 0 if this
     * class found no information about the vertical SRID.
     */
    public int verticalSRID;

    /**
     * The vertical coordinate values, or {@code null} if none.
     */
    public double[] verticalValues;

    /**
     * The date range, or {@code null} if none. This array usually contains only one element,
     * but more than one time range is allowed if the image file contains data at many times.
     * In the later case, the sequence of date ranges is associated to the sequence of
     * {@linkplain #imageIndex image indices}, i.e.:
     * <p>
     * <ul>
     *   <li>{@code dateRanges[0]} is the temporal extent of the image at index {@link #imageIndex}.</li>
     *   <li>{@code dateRanges[1]} is the temporal extent of the image at index {@link #imageIndex} + 1.</li>
     *   <li><i>etc.</i></li>
     *   <li>Finally, {@code dateRanges[n]} is the temporal extent of the image at index
     *       {@link #imageIndex} + n where <var>n</var> = {@code dateRanges.length - 1}.</li>
     * </ul>
     */
    public DateRange[] dateRanges;

    /**
     * Creates a new instance which is a copy of the given instance except for the input file,
     * image index and time range. This method is used only when iterating over the content of
     * an aggregate (typically a NcML file).
     * <p>
     * This constructor does not clone the references to mutable objects.
     * Consequently this instance is not allowed to be made visible through public API.
     *
     * {@section Not for implementors}
     * The {@link WritableGridCoverageTable#addEntries} method assumes that the instance created by
     * this method uses the same format and the same spatial extent than the master entry. If this
     * assumption doesn't hold anymore in a future version, then {@code WritableGridCoverageTable}
     * needs to be updated (see comments in its code).
     *
     * @param master     The reference to copy.
     * @param file       The path, filename and index to the new image file.
     * @param dateIndex  Index of the element to select in the {@code dateRanges} array.
     *
     * @since 3.16
     */
    NewGridCoverageReference(final NewGridCoverageReference master, final File file, final int dateIndex) {
        String filename  = file.getName();
        String extension = null;
        final int s = filename.lastIndexOf('.');
        if (s > 0) {
            extension = filename.substring(s+1);
            filename = filename.substring(0, s);
        }
        this.database           = master.database;
        this.path               = file.getParentFile();
        this.filename           = filename;
        this.extension          = extension;
        this.format             = master.format;
        this.sampleDimensions   = master.sampleDimensions;
        this.bestFormat         = master.bestFormat;
        this.alternativeFormats = master.alternativeFormats;
        this.spi                = master.spi;
        this.imageBounds        = master.imageBounds;
        this.gridToCRS          = master.gridToCRS;
        this.horizontalSRID     = master.horizontalSRID;
        this.verticalSRID       = master.verticalSRID;
        this.verticalValues     = master.verticalValues;
        this.dateRanges         = new DateRange[] {master.dateRanges[dateIndex]};
        // 'imageIndex' needs to be left to 0.
    }

    /**
     * Creates an entry for the given tile.
     *
     * @param  database The database where the new entry will be added.
     * @param  tile The tile to use for the entry.
     * @throws IOException if an error occurred while reading the image.
     */
    NewGridCoverageReference(final SpatialDatabase database, final Tile tile)
            throws SQLException, IOException, FactoryException
    {
        this(database, tile.getImageReader(), tile.getInput(), tile.getImageIndex(), tile, true);
    }

    /**
     * Creates en entry for the given reader. The {@linkplain ImageReader#setInput(Object)
     * reader input must be set} by the caller before to invoke this constructor.
     *
     * @param  database      The database where the new entry will be added.
     * @param  reader        The image reader with its input set.
     * @param  input         The original input. May not be the same than {@link ImageReader#getInput()}
     *                       because the later may have been transformed in an image input stream.
     * @param  imageIndex    Index of the image to read.
     * @param  disposeReader {@code true} if {@link ImageReader#dispose()} should be invoked on
     *                       the given {@code reader} after this method finished its work.
     * @throws IOException if an error occurred while reading the image.
     */
    NewGridCoverageReference(final SpatialDatabase database, final ImageReader reader,
            final Object input, final int imageIndex, final boolean disposeReader)
            throws SQLException, IOException, FactoryException
    {
        this(database, reader, input, imageIndex, null, disposeReader);
    }

    /**
     * Creates en entry for the given tile or reader. The reader argument is mandatory
     * and must have its {@linkplain ImageReader#setInput(Object) input set}. The tile
     * argument is optional.
     *
     * @param  database      The database where the new entry will be added.
     * @param  reader        The image reader with its input set.
     * @param  input         The original input. May not be the same than {@link ImageReader#getInput()}
     *                       because the later may have been transformed in an image input stream.
     * @param  imageIndex    Index of the image to read.
     * @param  tile          The tile for which a reference is created, or {@code null} if none.
     * @param  disposeReader {@code true} if {@link ImageReader#dispose()} should be invoked on
     *                       the given {@code reader} after this method finished its work.
     * @throws IOException if an error occurred while reading the image.
     */
    private NewGridCoverageReference(final SpatialDatabase database, final ImageReader reader,
            Object input, final int imageIndex, final Tile tile, final boolean disposeReader)
            throws SQLException, IOException, FactoryException
    {
        this.database = database;
        this.imageIndex = imageIndex;
        /*
         * Get the input, which must be an instance of File.
         * Split that input file into the path components.
         */
        input = IOUtilities.tryToFile(input);
        if (!(input instanceof File)) {
            throw new IIOException(Errors.format(Errors.Keys.ILLEGAL_CLASS_$2,
                    Classes.getShortClassName(input), File.class));
        }
        final File inputFile = (File) input;
        path = inputFile.getParentFile();
        final String name = inputFile.getName();
        final int split = name.lastIndexOf('.');
        if (split >= 0) {
            filename  = name.substring(0, split);
            extension = name.substring(split + 1);
        } else {
            filename  = name;
            extension = null;
        }
        /*
         * Get the metadata. We usually need the image metadata. However some formats may
         * store their information only in stream metadata, so we will fallback on stream
         * metadata if there is no image metadata.
         */
        spi = reader.getOriginatingProvider();
        String imageFormat = getFormatName(spi);
        if (imageFormat == null) {
            imageFormat = IOUtilities.extension(input);
        }
        if (imageFormat.length() == 0) {
            throw new IOException(Errors.format(Errors.Keys.UNDEFINED_FORMAT));
        }
        SpatialMetadata metadata = null;
        if (true) {
            IIOMetadata candidate = reader.getImageMetadata(imageIndex);
            if (candidate instanceof SpatialMetadata) {
                metadata = (SpatialMetadata) candidate;
            } else {
                candidate = reader.getStreamMetadata();
                if (candidate instanceof SpatialMetadata) {
                    metadata = (SpatialMetadata) candidate;
                }
            }
        }
        final MetadataHelper helper = (metadata != null) ? new MetadataHelper(
                (reader instanceof Localized) ? (Localized) reader : null) : null;
        /*
         * Get the geolocalization from the image, then complete with the tile information if
         * there is a Tile object. We avoid invoking Tile.getRegion() because it may create a
         * new ImageReader for fetching the image size. We will rather use the ImageReader that
         * we already have.
         */
        imageBounds = new Rectangle(reader.getWidth(imageIndex), reader.getHeight(imageIndex));
        AffineTransform gridToCRS = null;
        if (tile != null) {
            imageBounds.setLocation(tile.getLocation());
            gridToCRS = tile.getGridToCRS();
        }
        if (gridToCRS != null) {
            // Tile.getGridToCRS() returns an immutable AffineTransform.
            // We want to allow modifications.
            gridToCRS = new AffineTransform(gridToCRS);
        } else if (metadata != null) {
            gridToCRS = helper.getAffineTransform(metadata.getInstanceForType(RectifiedGrid.class), null);
        } else {
            gridToCRS = new AffineTransform();
        }
        this.gridToCRS = gridToCRS;
        /*
         * Get the CRS, then try to infer the horizontal and vertical SRID from it.
         * This code scan the "spatial_ref_sys" PostGIS table until matches are found,
         * or leaves the SRID to 0 if no match is found.
         */
        if (metadata != null) {
            final CoordinateReferenceSystem crs = metadata.getInstanceForType(CoordinateReferenceSystem.class);
            if (crs != null) {
                /*
                 * Horizontal CRS.
                 */
                final CRSAuthorityFactory crsFactory = database.getCRSAuthorityFactory();
                final CoordinateReferenceSystem horizontalCRS = CRS.getHorizontalCRS(crs);
                if (horizontalCRS != null) {
                    final Integer id = getIdentifier(horizontalCRS, crsFactory);
                    if (id != null) {
                        horizontalSRID = id;
                    }
                }
                /*
                 * Vertical CRS. Extract also the vertical ordinates, if any.
                 */
                final VerticalCRS verticalCRS = CRS.getVerticalCRS(crs);
                if (verticalCRS != null) {
                    final Integer id = getIdentifier(verticalCRS, crsFactory);
                    if (id != null) {
                        verticalSRID = id;
                    }
                    final CoordinateSystemAxis axis = verticalCRS.getCoordinateSystem().getAxis(0);
                    if (axis instanceof DiscreteCoordinateSystemAxis) {
                        final DiscreteCoordinateSystemAxis da = (DiscreteCoordinateSystemAxis) axis;
                        final int length = da.length();
                        for (int i=0; i<length; i++) {
                            final Comparable<?> value = da.getOrdinateAt(i);
                            if (value instanceof Number) {
                                if (verticalValues == null) {
                                    verticalValues = new double[length];
                                    Arrays.fill(verticalValues, Double.NaN);
                                }
                                verticalValues[i] = ((Number) value).doubleValue();
                            }
                        }
                    }
                }
                /*
                 * Temporal CRS. Extract also the time ordinate range, if any.
                 */
                final TemporalCRS temporalCRS = CRS.getTemporalCRS(crs);
                if (temporalCRS != null) {
                    final CoordinateSystemAxis axis = temporalCRS.getCoordinateSystem().getAxis(0);
                    if (axis instanceof DiscreteCoordinateSystemAxis) {
                        final DiscreteCoordinateSystemAxis da = (DiscreteCoordinateSystemAxis) axis;
                        DefaultTemporalCRS c = null; // To be created when first needed.
                        dateRanges = new DateRange[da.length()];
                        for (int i=0; i<dateRanges.length; i++) {
                            Range<?> r = da.getOrdinateRangeAt(i);
                            if (!(r instanceof DateRange)) {
                                if (c == null) {
                                    c = DefaultTemporalCRS.wrap(temporalCRS);
                                }
                                r = new DateRange(
                                        c.toDate(((Number) r.getMinValue()).doubleValue()), r.isMinIncluded(),
                                        c.toDate(((Number) r.getMaxValue()).doubleValue()), r.isMaxIncluded());
                            }
                            dateRanges[i] = (DateRange) r;
                        }
                    } else {
                        final DefaultTemporalCRS c = DefaultTemporalCRS.wrap(temporalCRS);
                        dateRanges = new DateRange[] {
                            new DateRange(c.toDate(axis.getMinimumValue()), c.toDate(axis.getMaximumValue()))
                        };
                    }
                }
            }
        }
        /*
         * Get the sample dimensions. This code extracts the SampleDimensions from the metadata,
         * converts them to GridSampleDimensions, then checks if the resulting sample dimensions
         * are native, packed or geophysics.
         */
        ViewType packMode = ViewType.NATIVE;
        List<GridSampleDimension> sampleDimensions = null;
        if (metadata != null) {
            final DimensionAccessor dimHelper = new DimensionAccessor(metadata);
            if (dimHelper.isScanSuggested(reader, imageIndex)) {
                dimHelper.scanValidSampleValue(reader, imageIndex);
            }
            sampleDimensions = helper.getGridSampleDimensions(metadata.getListForType(SampleDimension.class));
            if (sampleDimensions != null) {
                /*
                 * Replaces geophysics sample dimensions by new sample dimensions using the
                 * [1...255] range of packed values, and 0 for "no data".  We don't do that
                 * in the default MetadataHelper implementation because the choosen range is
                 * arbitrary. However this is okay to make such arbitrary choice in the particular
                 * case of NewGridCoverageReference, because the choosen range will be saved
                 * in the database (the user can also modify the range).
                 */
                final GridSampleDimension[] bands = sampleDimensions.toArray(new GridSampleDimension[sampleDimensions.size()]);
                for (int i=0; i<bands.length; i++) {
                    final GridSampleDimension band = bands[i];
                    final List<Category> categories = band.getCategories();
                    for (int j=categories.size(); --j>=0;) {
                        Category c = categories.get(j);
                        final TransferFunction tf = new TransferFunction(c, null);
                        if (tf.isQuantitative) {
                            if (tf.isGeophysics) {
                                /*
                                 * In the geophysics case, we are free to choose whatever upper
                                 * value please us.  We are using 255 here, the maximum allowed
                                 * for a 8-bits indexed image.
                                 */
                                c = new Category(c.getName(), c.getColors(), PACKED_RANGE, c.getRange());
                                bands[i] = packSampleDimension(band, c).geophysics(true);
                                packMode = ViewType.GEOPHYSICS;
                            } else if (tf.minimum < 0 && TransferFunctionType.LINEAR.equals(tf.type)) {
                                /*
                                 * In the signed integer values case, the offset applied here must
                                 * be consistent with the sample conversion applied by the image
                                 * reader when SampleConversionType.SHIFT_SIGNED_INTEGERS is set.
                                 */
                                // Upper sample value: Add 1 because value 0 is reserved for
                                // "no data", and add 1 again because 'upper' is exclusive.
                                final int upper = (tf.maximum - tf.minimum) + 2;
                                double offset = fixRoundingError(tf.offset - tf.scale * (1 - tf.minimum));
                                c = new Category(c.getName(), c.getColors(), 1, upper, tf.scale, offset);
                                bands[i] = packSampleDimension(band, c);
                                packMode = ViewType.PACKED;
                            }
                            /*
                             * MetadataHelper should have created at most one quantitative category
                             * (usually the last one) recognized by its non-null transfer function.
                             * In the uncommon case were there is more quantitative categories, it
                             * is the user responsibility to edit the fields (using the widget for
                             * instance). We stop the loop in order to avoid conflicts.
                             */
                            break;
                        }
                    }
                }
                sampleDimensions = Arrays.asList(bands);
            }
        }
        /*
         * Search if a format already exists in the database for the sample dimensions.
         * If no existing format is found, create a new FormatEntry but do not add it
         * in the database yet.
         */
        final FormatTable formatTable = database.getTable(FormatTable.class);
        FormatEntry candidate = formatTable.find(imageFormat, sampleDimensions);
        if (candidate == null) {
            GridSampleDimension[] bands = null;
            if (sampleDimensions != null) {
                bands = new GridSampleDimension[sampleDimensions.size()];
                for (int i=0; i<bands.length; i++) {
                    bands[i] = sampleDimensions.get(i).geophysics(false);
                }
            }
            candidate = new FormatEntry(formatTable.searchFreeIdentifier(imageFormat),
                    imageFormat, null, bands, packMode, null);
        }
        formatTable.release();
        bestFormat = candidate;
        format = candidate.getIdentifier();
        this.sampleDimensions = (candidate.sampleDimensions != null) ?
            new ArrayList<GridSampleDimension>(candidate.sampleDimensions) :
            new ArrayList<GridSampleDimension>();
        /*
         * Close the reader but do not dispose it (unless we were asked to),
         * since it may be used for the next entry.
         */
        XImageIO.close(reader);
        if (disposeReader) {
            reader.dispose();
        }
    }

    /**
     * Creates a new sample dimension defined as the "no data" category together with the
     * given category. The dimension name and units are copied from the old sample dimension.
     */
    private static GridSampleDimension packSampleDimension(final GridSampleDimension band, final Category category) {
        return new GridSampleDimension(band.getDescription(),
                new Category[] {Category.NODATA, category}, band.getUnits());
    }

    /**
     * Returns the identifier of the given CRS. This method search first in the PostGIS
     * {@code "spatial_ref_sys"} table. If no identifier is found in that table, then it
     * search for the EPSG code on the assumption that PostGIS codes are often the same
     * numerical values than EPSG codes (not that there is nothing enforcing that; this
     * is only an observed common practice).
     *
     * @param  crs The CRS for which the identifier is wanted.
     * @param  crsFactory The PostGIS CRS factory, or {@code null} if none.
     * @return The identifier, or {@code null} if none.
     * @throws FactoryException If an error occurred while searching for the identifier.
     */
    private static Integer getIdentifier(final CoordinateReferenceSystem crs, final CRSAuthorityFactory crsFactory)
            throws FactoryException
    {
        if (crsFactory instanceof AbstractAuthorityFactory) {
            IdentifiedObject identifiedCRS = ((AbstractAuthorityFactory) crsFactory)
                    .getIdentifiedObjectFinder(crs.getClass()).find(crs);
            if (identifiedCRS == null) {
                identifiedCRS = crs;
            }
            ReferenceIdentifier id = null;
            for (final Citation authority : AUTHORITIES) {
                id = AbstractIdentifiedObject.getIdentifier(identifiedCRS, authority);
                if (id != null) {
                    final String code = id.getCode();
                    if (id != null) try {
                        return Integer.valueOf(code);
                    } catch (NumberFormatException e) {
                        throw new FactoryException(Errors.format(Errors.Keys.UNPARSABLE_NUMBER_$1, id), e);
                    }
                }
            }
        }
        return CRS.lookupEpsgCode(crs, true);
    }

    /**
     * Returns the format name. Current implementation selects the longest name,
     * on the assumption that it is the most explicit name.
     *
     * @param  spi The image reader provider, or {@code null}.
     * @return The format name, or {@code null} if {@code spi} was null.
     * @throws IOException if the format can not be obtained.
     */
    private static String getFormatName(final ImageReaderSpi spi) throws IOException {
        if (spi == null) {
            return null;
        }
        String format = "";
        String[] formats = spi.getFormatNames();
        if (formats != null) {
            for (final String candidate : formats) {
                final int d = candidate.length() - format.length();
                if (d >= 0) {
                    if (d == 0) {
                        int na=0, nb=0;
                        for (int i=candidate.length(); --i>=0;) {
                            if (Character.isUpperCase(candidate.charAt(i))) na++;
                            if (Character.isUpperCase(format   .charAt(i))) nb++;
                        }
                        if (na <= nb) {
                            continue;
                        }
                    }
                    format = candidate;
                }
            }
        }
        if (format.length() == 0) {
            // No format found - fall back on mime types.
            formats = spi.getMIMETypes();
            if (formats != null) {
                for (final String candidate : formats) {
                    if (candidate.length() > format.length()) {
                        format = candidate;
                    }
                }
            }
            if (format.length() == 0) {
                return null;
            }
        }
        return format;
    }

    /**
     * Returns the path to the coverage file as
     * "{@linkplain #path}/{@linkplain #filename}.{@linkplain #extension}".
     *
     * @return The path to the image file, or {@code null} if {@link #filename} is null.
     *
     * @see #path
     * @see #filename
     * @see #extension
     */
    public File getFile() {
        String name = filename;
        if (name == null) {
            return null;
        }
        if (extension != null) {
            name = name + '.' + extension;
        }
        return new File(path, name);
    }

    /**
     * Returns a list of formats which may be used as an alternative to {@link #format}.
     * This method can be invoked from Graphical User Interface wanting to provide a
     * choice to user.
     *
     * @return A list of formats which may be used as an alternative to {@link #format}.
     * @throws CoverageStoreException If an error occurred while fetching the list of
     *         alternative formats from the database.
     *
     * @since 3.13
     */
    public String[] getAlternativeFormats() throws CoverageStoreException {
        FormatEntry[] alternativeFormats = this.alternativeFormats;
        if (alternativeFormats == null) {
            /*
             * Fetch the alternative formats from the database when first needed.
             */
            final Collection<FormatEntry> formats;
            try {
                final FormatTable table = database.getTable(FormatTable.class);
                table.setImageFormats(bestFormat.getImageFormats());
                formats = table.getEntries();
                table.release();
            } catch (SQLException e) {
                throw new CoverageStoreException(e);
            }
            alternativeFormats = formats.toArray(new FormatEntry[formats.size()]);
            /*
             * Retain only the formats having the same number of bands.
             */
            final int numBands = sampleDimensions.size();
            if (numBands != 0) {
                int count = 0;
                for (int i=0; i<alternativeFormats.length; i++) {
                    final FormatEntry candidate = alternativeFormats[i];
                    final List<GridSampleDimension> cb = candidate.sampleDimensions;
                    if (cb == null || cb.size() == numBands) {
                        alternativeFormats[count++] = candidate;
                    }
                }
                alternativeFormats = XArrays.resize(alternativeFormats, count);
            }
            this.alternativeFormats = alternativeFormats;
        }
        /*
         * Return the format names (not the format entries, which are not public API).
         */
        final String[] names = new String[alternativeFormats.length];
        for (int i=0; i<names.length; i++) {
            names[i] = alternativeFormats[i].identifier.toString();
        }
        return names;
    }

    /**
     * Returns {@code true} if the {@linkplain #format} is already defined in the database,
     * or {@code false} if this is a new format.
     *
     * @return {@code true} if the current format is defined in the database.
     * @throws CoverageStoreException If an error occurred while reading from the database.
     *
     * @since 3.13
     *
     * @see #format
     */
    public boolean isFormatDefined() throws CoverageStoreException {
        final boolean isDefined;
        try {
            final FormatTable table = database.getTable(FormatTable.class);
            isDefined = table.exists(format);
            table.release();
        } catch (SQLException e) {
            throw new CoverageStoreException(e);
        }
        return isDefined;
    }

    /**
     * Recomputes some attributes in this {@code NewGridCoverageReference}. This method can be
     * invoked after one of the following attributes changed:
     * <p>
     * <ul>
     *   <li>{@link #format}</li>
     * </ul>
     * <p>
     * The current implementation recomputes the following attributes. Note that this list
     * may be expanded in a future version.
     * <p>
     * <ul>
     *   <li>{@link #sampleDimensions}</li>
     * </ul>
     *
     * @throws CoverageStoreException If an error occurred while reading from the database.
     *
     * @since 3.13
     */
    public void refresh() throws CoverageStoreException {
        sampleDimensions.clear();
        final List<GridSampleDimension> newContent;
        if (bestFormat.getIdentifier().equals(format)) {
            newContent = bestFormat.sampleDimensions;
        } else {
            final FormatEntry entry;
            try {
                final FormatTable table = database.getTable(FormatTable.class);
                try {
                    entry = table.getEntry(format);
                } catch (NoSuchRecordException e) {
                    table.release();
                    return;
                }
                table.release();
            } catch (SQLException e) {
                throw new CoverageStoreException(e);
            }
            newContent = entry.sampleDimensions;
        }
        if (newContent != null) {
            sampleDimensions.addAll(newContent);
        }
    }

    /**
     * Returns a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder(Classes.getShortClassName(this)).append('[');
        boolean isNext = false;
fill:   for (int i=0; ;i++) {
            final String label;
            final Object value;
            switch (i) {
                case 0: label="format"; value=format;    break;
                case 1: label="file";   value=getFile(); break;
                default: break fill;
            }
            if (value != null) {
                if (isNext) {
                    buffer.append(", ");
                }
                buffer.append(label).append('=').append(value);
                isNext = true;
            }
        }
        return buffer.append(']').toString();
    }
}
