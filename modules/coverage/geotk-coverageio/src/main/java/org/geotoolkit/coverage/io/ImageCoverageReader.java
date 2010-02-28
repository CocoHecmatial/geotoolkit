/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2010, Geomatys
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

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.MissingResourceException;
import java.io.Closeable;
import java.io.IOException;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.RenderedImage;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.opengis.geometry.Envelope;
import org.opengis.util.InternationalString;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.RectifiedGrid;
import org.opengis.metadata.spatial.Georectified;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.geotoolkit.factory.Hints;
import org.geotoolkit.coverage.Category;
import org.geotoolkit.coverage.CoverageFactoryFinder;
import org.geotoolkit.coverage.GridSampleDimension;
import org.geotoolkit.coverage.grid.GeneralGridEnvelope;
import org.geotoolkit.coverage.grid.GridCoverageFactory;
import org.geotoolkit.coverage.grid.GridGeometry2D;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.grid.InvalidGridGeometryException;
import org.geotoolkit.display.shape.DoubleDimension2D;
import org.geotoolkit.display.shape.XRectangle2D;
import org.geotoolkit.image.io.NamedImageStore;
import org.geotoolkit.image.io.XImageIO;
import org.geotoolkit.image.io.metadata.MetadataHelper;
import org.geotoolkit.image.io.metadata.SampleDimension;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.geotoolkit.image.io.metadata.SpatialMetadataFormat;
import org.geotoolkit.internal.io.IOUtilities;
import org.geotoolkit.resources.Errors;
import org.geotoolkit.resources.Vocabulary;
import org.geotoolkit.util.XArrays;
import org.geotoolkit.util.NumberRange;
import org.geotoolkit.util.collection.BackingStoreException;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.referencing.crs.DefaultImageCRS;
import org.geotoolkit.referencing.operation.matrix.XMatrix;
import org.geotoolkit.referencing.operation.matrix.MatrixFactory;
import org.geotoolkit.referencing.operation.matrix.XAffineTransform;
import org.geotoolkit.referencing.operation.transform.IdentityTransform;
import org.geotoolkit.referencing.operation.transform.ProjectiveTransform;
import org.geotoolkit.referencing.operation.transform.ConcatenatedTransform;


/**
 * A {@link GridCoverageReader} implementation which use an {@link ImageReader} for reading
 * sample values. This implementation stores the sample values in a {@link RenderedImage},
 * and consequently is targeted toward two-dimensional slices of data.
 * <p>
 * {@code ImageCoverageReader} basically works as a layer which converts <cite>geodetic
 * coordinates</cite> (for example the region to read) to <cite>pixel coordinates</cite>
 * before to pass them to the wrapped {@code ImageReader}, and conversely: from pixel
 * coordinates to geodetic coordinates. The later conersion is called "<cite>grid to CRS</cite>"
 * and is determined from the {@link SpatialMetadata} provided by the {@code ImageReader}.
 *
 * {@section Default metadata value}
 * If no {@linkplain CoordinateReferenceSystem Coordinate Reference System} or no
 * <cite>grid to CRS</cite> {@linkplain MathTransform math transform} can be created
 * from the {@link SpatialMetadata}, then the default values listed below are used:
 * <p>
 * <table border="1" cellspacing="0">
 *   <tr bgcolor="lightblue"><th>Type</th><th>Default value</th></tr>
 *   <tr><td>&nbsp;{@link CoordinateReferenceSystem}&nbsp;</td>
 *       <td>&nbsp;{@link DefaultImageCRS#GRID_2D}&nbsp;</td></tr>
 *   <tr><td>&nbsp;{@link MathTransform}&nbsp;</td>
 *       <td>&nbsp;{@link IdentityTransform#create(int)} with the CRS dimension&nbsp;</td></tr>
 * </table>
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @author Johann Sorel (Geomatys)
 * @version 3.09
 *
 * @since 3.09 (derived from 2.2)
 * @module
 */
public class ImageCoverageReader extends GridCoverageReader {
    /**
     * Small values for rounding errors in floating point calculations. This value shall not be
     * too small, otherwise {@link #computeBounds} fails to correct for rounding errors and we
     * get a region to read bigger than necessary. Experience suggests that 1E-6 is too small,
     * while 1E-5 seems okay.
     */
    private static final double EPS = 1E-5;

    /**
     * Minimal image width and height, in pixels. If the user requests a smaller image,
     * then the request will be expanded to that size. The current setting is the minimal
     * size required for allowing bicubic interpolations.
     */
    private static final int MIN_SIZE = 4;

    /**
     * The name of metadata nodes we are interrested in. Some implementations of
     * {@link ImageReader} may use this information for reading only the metadata
     * we are interrested in.
     *
     * @see SpatialMetadataFormat
     */
    private static final Set<String> METADATA_NODES;
    static {
        final Set<String> s = new HashSet<String>(25);
                                                // geotk-coverageio
                                                // ├───ImageDescription
        s.add("Dimensions");                    // │   ├───Dimensions
        s.add("Dimension");                     // │   │   └───Dimension
        s.add("RangeElementDescriptions");      // │   └───RangeElementDescriptions
        s.add("RangeElementDescription");       // │       └───RangeElementDescription
        s.add("SpatialRepresentation");         // ├───SpatialRepresentation
        s.add("RectifiedGridDomain");           // └───RectifiedGridDomain
        s.add("Limits");                        //     ├───Limits
        s.add("OffsetVectors");                 //     ├───OffsetVectors
        s.add("OffsetVector");                  //     │   └───OffsetVector
        s.add("CoordinateReferenceSystem");     //     └───CoordinateReferenceSystem
        s.add("CoordinateSystem");              //         ├───CoordinateSystem
        s.add("Axes");                          //         │   └───Axes
        s.add("CoordinateSystemAxis");          //         │       └───CoordinateSystemAxis
        s.add("Datum");                         //         ├───Datum
        s.add("Ellipsoid");                     //         │   ├───Ellipsoid
        s.add("PrimeMeridian");                 //         │   └───PrimeMeridian
        s.add("Conversion");                    //         └───Conversion
        s.add("Parameters");                    //             └───Parameters
        s.add("ParameterValue");                //                 └───ParameterValue
        METADATA_NODES = Collections.unmodifiableSet(s);
    }

    /**
     * The {@link ImageReader} to use for decoding {@link RenderedImage}s. This reader is
     * initially {@code null} and lazily created the first time {@link #setInput(Object)}
     * is invoked. Once created, it is reused for subsequent inputs if possible.
     */
    protected ImageReader imageReader;

    /**
     * Optional parameter to be given (if non-null) to the
     * <code>{@linkplain #imageReader}.{@linkplain ImageReader#setInput(Object, boolean, boolean)
     * setInput}(&hellip;, seekForwardOnly, &hellip;)</code> method.
     *
     * If {@code TRUE}, images and metadata may only be read in ascending order from the input
     * source. If {@code FALSE}, they may be read in any order. If {@code null}, then this
     * parameter is not given to the {@linkplain #imageReader image reader} which is free to
     * use a plugin-dependent default (usually {@code false}).
     */
    protected Boolean seekForwardOnly;

    /**
     * Optional parameter to be given (if non-null) to the
     * <code>{@linkplain #imageReader}.{@linkplain ImageReader#setInput(Object, boolean, boolean)
     * setInput}(&hellip;, &hellip;, ignoreMetadata)</code> method.
     *
     * If {@code TRUE}, metadata may be ignored during reads. If {@code FALSE}, metadata will be
     * parsed. If {@code null}, then this parameter is not given to the {@linkplain #imageReader
     * image reader} which is free to use a plugin-dependent default (usually {@code false}).
     */
    protected Boolean ignoreMetadata;

    /**
     * The names of coverages, or {@code null} if not yet determined.
     * This is created by {@link #getCoverageNames()} when first needed.
     */
    private List<String> coverageNames;

    /**
     * Helper utilities for parsing metadata. Created only when needed.
     */
    private transient MetadataHelper helper;

    /**
     * The grid coverage factory to use.
     */
    private final GridCoverageFactory factory;

    /**
     * Creates a new instance using the default
     * {@linkplain GridCoverageFactory grid coverage factory}.
     */
    public ImageCoverageReader() {
        this(null);
    }

    /**
     * Creates a new instance using the {@linkplain GridCoverageFactory grid coverage factory}
     * specified by the given set of hints.
     *
     * @param hints The hints to use for fetching a {@link GridCoverageFactory},
     *        or {@code null} for the default hints.
     */
    public ImageCoverageReader(final Hints hints) {
        factory = CoverageFactoryFinder.getGridCoverageFactory(hints);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The given locale will also be given to the wrapped {@linkplain #imageReader image reader},
     * providing that the image reader supports the locale language. If it doesn't, then the image
     * reader locale is set to {@code null}.
     */
    @Override
    public void setLocale(final Locale locale) {
        super.setLocale(locale);
        setLocale(imageReader, locale);
        helper = null;
    }

    /**
     * Sets the given locale to the given {@link ImageReader}, provided that the image reader
     * supports the language of that locale. Otherwise set the reader locale to {@code null}.
     *
     * @see ImageReader#setLocale(Locale)
     */
    private static void setLocale(final ImageReader reader, final Locale locale) {
        if (reader != null && locale != null) {
            final Locale[] list = reader.getAvailableLocales();
            for (int i=list.length; --i>=0;) {
                if (locale.equals(list[i])) {
                    reader.setLocale(locale);
                    return;
                }
            }
            final String language = getISO3Language(locale);
            if (language != null) {
                for (int i=list.length; --i>=0;) {
                    final Locale candidate = list[i];
                    if (language.equals(getISO3Language(candidate))) {
                        reader.setLocale(candidate);
                        return;
                    }
                }
            }
            reader.setLocale(null);
        }
    }

    /**
     * Returns the ISO language code for the specified locale, or {@code null} if not available.
     * This is used for finding a match when the locale given to the {@link #setLocale(Locale)}
     * method does not match exactly the locale supported by the image reader. In such case, we
     * will pickup a locale for the same language even if it is not the same country.
     */
    private static String getISO3Language(final Locale locale) {
        try {
            return locale.getISO3Language();
        } catch (MissingResourceException exception) {
            return null;
        }
    }

    /**
     * Sets the input source to the given object. The input is typically a
     * {@link java.io.File}, {@link java.net.URL} or {@link String} object,
     * but other types (especially {@link ImageInputStream}) may be accepted
     * as well depending on the {@linkplain #imageReader image reader} implementation.
     * <p>
     * The given input can also be an {@link ImageReader} instance with its input initialized,
     * in which case it is used directly as the {@linkplain #imageReader image reader} wrapped
     * by this {@code ImageCoverageReader}.
     *
     * {@section Overriding in subclasses}
     * Subclasses wanting a different behavior should consider overriding the following
     * methods before to override this {@code setInput(Object)} method:
     * <p>
     * <ul>
     *   <li>{@link #canReuseImageReader(ImageReaderSpi, Object)} - for determining if the
     *       current {@linkplain #imageReader image reader} can be reused.</li>
     *   <li>{@link #createImageReader(Object)} - for creating a new
     *       {@linkplain #imageReader image reader} for the given input.</li>
     * </ul>
     */
    @Override
    public void setInput(final Object input) throws CoverageStoreException {
        final ImageReader oldReader = imageReader;
        final boolean disposeReader = (oldReader != null && oldReader != this.input);
        // The above must be determined before the call to close(),
        // because the above-cited method set this.input to null.
        try {
            close();
            assert (oldReader == null) || (oldReader.getInput() == null) : oldReader;
            if (input != null) {
                ImageReader newReader = null;
                if (input instanceof ImageReader) {
                    newReader = (ImageReader) input;
                    // The old reader will be disposed and the locale will be set below.
                } else {
                    /*
                     * First, check if the current reader can be reused. If the user
                     * didn't overriden the canReuseImageReader(...) method, then the
                     * default implementation is to look at the file extension.
                     */
                    if (oldReader != null) {
                        final ImageReaderSpi provider = oldReader.getOriginatingProvider();
                        if (provider != null && canReuseImageReader(provider, input)) {
                            newReader = oldReader;
                        }
                    }
                    /*
                     * If we can't reuse the old reader, create a new one. If the user didn't
                     * overriden the createImageReader(...) method, then the default behavior
                     * is to get an image reader by the extension.
                     */
                    if (newReader == null) {
                        newReader = createImageReader(input);
                    }
                    /*
                     * Set the input if it was not already done. In the default implementation,
                     * this is done by 'createImageReader' but not by 'canReuseImageReader'.
                     * However the user could have overriden the above-cited methods with a
                     * different behavior.
                     */
                    if (newReader != input && newReader.getInput() == null) {
                        if (seekForwardOnly != null) {
                            if (ignoreMetadata != null) {
                                newReader.setInput(input, seekForwardOnly, ignoreMetadata);
                            } else {
                                newReader.setInput(input, seekForwardOnly);
                            }
                        } else {
                            newReader.setInput(input);
                        }
                    }
                }
                if (newReader != oldReader) {
                    if (disposeReader) {
                        oldReader.dispose();
                    }
                    setLocale(newReader, locale);
                }
                imageReader = newReader;
            }
        } catch (IOException e) {
            throw new CoverageStoreException(error(input, e), e);
        }
        super.setInput(input);
    }

    /**
     * Returns {@code true} if the image reader created by the given provider can be reused.
     * This method is invoked automatically by {@link #setInput(Object)} for determining if
     * the current {@linkplain #imageReader image reader} can be reused for reading the given
     * input.
     * <p>
     * The default implementation checks if the suffix of the given input is one of the
     * {@linkplain ImageReaderSpi#getFileSuffixes() file suffixes known to the provider}.
     * If the given object has no suffix (for example if it is an instance of
     * {@link javax.imageio.stream.ImageInputStream}), then this method fallbacks on
     * {@link ImageReaderSpi#canDecodeInput(Object)}.
     * <p>
     * Subclasses can override this method if they want to determine in another way
     * whatever the {@linkplain #imageReader image reader} can be reused. Subclasses
     * don't need to set the image reader input; this will be done by the caller.
     *
     * @param  provider The provider of the image reader.
     * @param  input The input to set to the image reader.
     * @return {@code true} if the image reader can be reused.
     * @throws IOException If an error occured while determining if the current
     *         image reader can use the given input.
     */
    protected boolean canReuseImageReader(final ImageReaderSpi provider, final Object input) throws IOException {
        if (IOUtilities.canProcessAsPath(input)) {
            final String[] suffixes = provider.getFileSuffixes();
            return suffixes != null && XArrays.containsIgnoreCase(suffixes, IOUtilities.extension(input));
        } else {
            return provider.canDecodeInput(input);
        }
    }

    /**
     * Creates an {@link ImageReader} that claim to be able to decode the given input.
     * This method is invoked automatically by {@link #setInput(Object)} for creating
     * a new {@linkplain #imageReader image reader}. The image reader input must be set.
     * <p>
     * The default implementation delegates to {@link XImageIO#getReaderBySuffix(Object,
     * Boolean, Boolean)}. Subclasses can override this method if they want to create a
     * new {@linkplain #imageReader image reader} in another way.
     *
     * @param  input The input source.
     * @return An initialized image reader for reading the given input.
     * @throws IOException If no suitable image reader has been found, or if an error occured
     *         while creating it.
     */
    protected ImageReader createImageReader(final Object input) throws IOException {
        return XImageIO.getReaderBySuffix(input, seekForwardOnly, ignoreMetadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getCoverageNames() throws CoverageStoreException {
        if (coverageNames == null) {
            final ImageReader imageReader = this.imageReader; // Protect from changes.
            if (imageReader == null) {
                throw new IllegalStateException(error(Errors.Keys.NO_IMAGE_INPUT));
            }
            try {
                if (imageReader instanceof NamedImageStore) {
                    coverageNames = ((NamedImageStore) imageReader).getImageNames();
                } else {
                    coverageNames = new NameList(getInputName(), imageReader.getNumImages(true));
                }
            } catch (IOException e) {
                throw new CoverageStoreException(error(e), e);
            }
        }
        return coverageNames;
    }

    /**
     * Gets the spatial metadata from the given image reader, or return {@code null}
     * if none were found. This method asks only for the metadata nodes listed in the
     * {@link #METADATA_NODES} collection. Note however that most {@link ImageReader}
     * implementations will return all metadata anyway.
     *
     * @param  imageReader The image reader from which to get the metadata.
     * @param  index The index of the image to be queried.
     * @return The metadata of the given index, or {@code null} if none.
     * @throws IOException If an error occured while reading the metadata.
     */
    private static SpatialMetadata getSpatialMetadata(final ImageReader imageReader, final int index)
            throws IOException
    {
        final IIOMetadata metadata = imageReader.getImageMetadata(index,
                SpatialMetadataFormat.FORMAT_NAME, METADATA_NODES);
        if (metadata instanceof SpatialMetadata) {
            return (SpatialMetadata) metadata;
        } else if (metadata != null) {
            return new SpatialMetadata(SpatialMetadataFormat.IMAGE, imageReader, metadata);
        } else {
            return null;
        }
    }

    /**
     * Returns a helper object for parsing metadata.
     */
    private MetadataHelper getMetadataHelper() {
        if (helper == null) {
            helper = new MetadataHelper(this);
        }
        return helper;
    }

    /**
     * Returns the grid geometry for the {@link GridCoverage2D} to be read at the given index.
     * The default implementation performs the following:
     * <p>
     * <ul>
     *   <li>The {@link org.opengis.coverage.grid.GridEnvelope} is determined from the image
     *       {@linkplain ImageReader#getWidth(int) width} and
     *       {@linkplain ImageReader#getHeight(int) height}.</li>
     *   <li>The {@link CoordinateReferenceSystem} and the "<cite>grid to CRS</cite>" conversion
     *       are determined from the {@link SpatialMetadata} if any.</li>
     * </ul>
     */
    @Override
    public GridGeometry2D getGridGeometry(final int index) throws CoverageStoreException {
        final ImageReader imageReader = this.imageReader; // Protect from changes.
        if (imageReader == null) {
            throw new IllegalStateException(error(Errors.Keys.NO_IMAGE_INPUT));
        }
        /*
         * Get the required information from the SpatialMetadata, if any.
         * For now we just collect them - they will be processed later.
         */
        CoordinateReferenceSystem crs = null;
        MathTransform       gridToCRS = null;
        PixelOrientation pointInPixel = null;
        final int width, height;
        try {
            width  = imageReader.getWidth(index);
            height = imageReader.getHeight(index);
            final SpatialMetadata metadata = getSpatialMetadata(imageReader, index);
            if (metadata != null) {
                crs = metadata.getInstanceForType(CoordinateReferenceSystem.class);
                final RectifiedGrid grid = metadata.getInstanceForType(RectifiedGrid.class);
                if (grid != null) {
                    gridToCRS = getMetadataHelper().getGridToCRS(grid);
                }
                final Georectified georect = metadata.getInstanceForType(Georectified.class);
                if (georect != null) {
                    pointInPixel = georect.getPointInPixel();
                }
            }
        } catch (IOException e) {
            throw new CoverageStoreException(error(e), e);
        }
        /*
         * If any metadata are still null, replace them by their default values. Those default
         * values are selected in order to be as neutral as possible: An ImageCRS which is not
         * convertible to GeodeticCRS, an identity "grid to CRS" conversion, a PixelOrientation
         * equivalent to performing no shift at all in the "grid to CRS" conversion.
         */
        if (crs == null) {
            crs = DefaultImageCRS.GRID_2D;
        }
        final int dimension = crs.getCoordinateSystem().getDimension();
        if (gridToCRS == null) {
            gridToCRS = IdentityTransform.create(dimension);
        }
        if (pointInPixel == null) {
            pointInPixel = PixelOrientation.CENTER;
        }
        /*
         * Now build the grid geometry.
         */
        final int[] lower = new int[dimension];
        final int[] upper = new int[dimension];
        Arrays.fill(upper, 1);
        upper[0] = width;
        upper[1] = height;
        final GeneralGridEnvelope gridRange = new GeneralGridEnvelope(lower, upper, false);
        return new GridGeometry2D(gridRange, pointInPixel, gridToCRS, crs, null);
    }

    /**
     * Returns the "<cite>Grid to CRS</cite>" conversion as an affine transform.
     * The conversion will map upper-left corner, as in Java2D conventions.
     *
     * @param  gridGeometry The grid geometry from which to extract the conversion.
     * @return The "<cite>grid to CRS</cite>" conversion.
     * @throws InvalidGridGeometryException If the conversion is not affine.
     */
    private AffineTransform getGridToCRS(final GridGeometry2D gridGeometry) throws InvalidGridGeometryException {
        final MathTransform gridToCRS = gridGeometry.getGridToCRS2D(PixelOrientation.UPPER_LEFT);
        if (gridToCRS instanceof AffineTransform) {
            return (AffineTransform) gridToCRS;
        }
        throw new InvalidGridGeometryException(error(Errors.Keys.NOT_AN_AFFINE_TRANSFORM));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GridSampleDimension> getSampleDimensions(final int index) throws CoverageStoreException {
        final ImageReader imageReader = this.imageReader; // Protect from changes.
        if (imageReader == null) {
            throw new IllegalStateException(error(Errors.Keys.NO_IMAGE_INPUT));
        }
        /*
         * Get the required information from the SpatialMetadata, if any.
         * For now we just collect them - they will be processed later.
         */
        List<SampleDimension> sampleDimensions = null;
        try {
            final SpatialMetadata metadata = getSpatialMetadata(imageReader, index);
            if (metadata != null) {
                sampleDimensions = metadata.getListForType(SampleDimension.class);
            }
        } catch (IOException e) {
            throw new CoverageStoreException(error(e), e);
        }
        if (sampleDimensions == null || sampleDimensions.isEmpty()) {
            return null;
        }
        /*
         * Now convert the SampleDimension instances to GridSampleDimension instances.
         * For each sample dimension, we create a qualitative category for each fill
         * values (if any) and a single quantitative category for the range of sample
         * values.
         */
        InternationalString untitled = null; // To be created only if needed.
        final MetadataHelper hlp = getMetadataHelper();
        final List<Category> categories = new ArrayList<Category>();
        final GridSampleDimension[] bands = new GridSampleDimension[sampleDimensions.size()];
        boolean hasSampleDimensions = false;
        for (int i=0; i<bands.length; i++) {
            final SampleDimension sd = sampleDimensions.get(i);
            if (sd != null) {
                /*
                 * Get a name for the sample dimensions. This name will be given both to the
                 * GridSampleDimension object and to the single qualitative Category. If no
                 * name can be found, "Untitled" will be used.
                 */
                InternationalString dimensionName = sd.getDescriptor();
                if (dimensionName == null) {
                    if (untitled == null) {
                        untitled = Vocabulary.formatInternational(Vocabulary.Keys.UNTITLED);
                    }
                    dimensionName = untitled;
                }
                /*
                 * Create a qualitative category for each fill value.
                 */
                final double[] fillValues = sd.getFillSampleValues();
                if (fillValues != null) {
                    final CharSequence name = Category.NODATA.getName();
                    for (int j=0; j<fillValues.length; j++) {
                        final double fv = fillValues[i];
                        final int ifv = (int) fv;
                        final Category c;
                        if (ifv == fv) {
                            c = new Category(name, null, ifv);
                        } else {
                            c = new Category(name, null, fv);
                        }
                        categories.add(c);
                    }
                }
                /*
                 * Create a quantitative category for the range of valid sample values.
                 */
                final NumberRange<?> range = hlp.getValidSampleValues(sd, fillValues);
                if (range != null) {
                    final Double scale = sd.getScaleFactor();
                    if (scale != null) {
                        Double offset = sd.getOffset();
                        if (offset == null) {
                            offset = 0.0;
                        }
                        final TransferFunctionType type = sd.getTransferFunctionType();
                        if (type == null || type.equals(TransferFunctionType.LINEAR)) {
                            categories.add(new Category(dimensionName, null, range, scale, offset));
                        } else {
                            /*
                             * TODO: We need to support exponential and logarithmic transforms
                             * here. For doing a good job, we should use a MathTransformFactory
                             * (see CategoryTable for inspiration). In order to be consistent, we
                             * should use that factory for the transform implicitly created by
                             * the above new Category(...) constructor call, and the transform
                             * implicitly created by MetadataHelper.createGridToCRS(...).
                             */
                            throw new CoverageStoreException(Errors.getResources(locale)
                                    .getString(Errors.Keys.UNSUPPORTED_OPERATION_$1, type));
                        }
                    }
                }
                /*
                 * Create the GridSampleDimension instance.
                 */
                if (!categories.isEmpty()) {
                    bands[i] = new GridSampleDimension(dimensionName,
                            categories.toArray(new Category[categories.size()]), sd.getUnits());
                    categories.clear();
                    hasSampleDimensions = true;
                }
            }
        }
        return hasSampleDimensions ? Arrays.asList(bands) : null;
    }

    /**
     * Reads the grid coverage. The default implementation creates a grid coverage using the
     * information provided by {@link #getGridGeometry(int)} and {@link #getSampleDimensions(int)}.
     */
    @Override
    public GridCoverage2D read(final int index, final GridCoverageReadParam param)
            throws CoverageStoreException
    {
        final ImageReader imageReader = this.imageReader; // Protect from changes.
        if (imageReader == null) {
            throw new IllegalStateException(error(Errors.Keys.NO_IMAGE_INPUT));
        }
        GridGeometry2D gridGeometry = getGridGeometry(index);
        final AffineTransform change; // The change in the 'gridToCRS' transform.
        final ImageReadParam imageParam;
        if (param != null) {
            /*
             * Convert geodetic envelope and resolution to pixel coordinates.
             */
            final Envelope envelope = param.getEnvelope();
            final double[] resolution = param.getResolution();
            double sx=0, sy=0;
            if (resolution != null) {
                final int length = resolution.length;
                if (gridGeometry.axisDimensionX < length) sx = resolution[gridGeometry.axisDimensionX];
                if (gridGeometry.axisDimensionY < length) sy = resolution[gridGeometry.axisDimensionY];
            }
            final Dimension2D subsampling = (sx > 0 || sy > 0) ? new DoubleDimension2D(sx, sy) : null;
            final Rectangle imageBounds;
            try {
                imageBounds = computeBounds(gridGeometry, envelope, subsampling);
            } catch (Exception e) { // There is many different exceptions thrown by the above.
                throw new CoverageStoreException(error(e), e);
            }
            if (imageBounds == null) {
                throw new CoverageStoreException(error(Errors.Keys.EMPTY_ENVELOPE));
            }
            /*
             * Store the result of the above conversions in the ImageReadParam object.
             * Also keep trace of the change that will need to be applied on the gridToCRS
             * transform.
             */
            change = AffineTransform.getTranslateInstance(imageBounds.x, imageBounds.y);
            imageParam = imageReader.getDefaultReadParam();
            imageParam.setSourceRegion(imageBounds);
            if (subsampling != null) {
                sx = subsampling.getWidth();
                sy = subsampling.getHeight();
                imageParam.setSourceSubsampling((int) sx, (int) sy, 0, 0);
                change.scale(sx, sy);
            }
        } else {
            imageParam = null;
            change     = null;
        }
        /*
         * Read the image.
         */
        final List<GridSampleDimension> bands = getSampleDimensions(index);
        final Map<?,?> properties = getProperties(index);
        final String name;
        final RenderedImage image;
        try {
            try {
                name = getCoverageNames().get(index);
            } catch (BackingStoreException e) {
                throw e.unwrapOrRethrow(IOException.class);
            }
            image = imageReader.readAsRenderedImage(index, imageParam);
        } catch (IOException e) {
            throw new CoverageStoreException(error(e), e);
        }
        /*
         * If the grid geometry changed as a result of subsampling or reading a smaller region,
         * update the grid geometry. The (xmin, ymin) values are usually (0,0), but we take
         * them in account anyway since we have read an image using 'readAsRenderedImage(...)',
         * which could have shifted the image.
         */
        if (change != null) {
            final int xmin = image.getMinX();
            final int ymin = image.getMinY();
            final int xi = gridGeometry.gridDimensionX;
            final int yi = gridGeometry.gridDimensionY;
            final MathTransform gridToCRS = gridGeometry.getGridToCRS();
            MathTransform newGridToCRS = gridToCRS;
            if (!change.isIdentity()) {
                final int gridDimension = gridToCRS.getSourceDimensions();
                final XMatrix matrix = MatrixFactory.create(gridDimension + 1);
                matrix.setElement(xi, xi, change.getScaleX());
                matrix.setElement(yi, yi, change.getScaleY());
                matrix.setElement(xi, gridDimension, change.getTranslateX() - xmin);
                matrix.setElement(yi, gridDimension, change.getTranslateY() - ymin);
                newGridToCRS = ConcatenatedTransform.create(ProjectiveTransform.create(matrix), gridToCRS);
            }
            final GridEnvelope gridRange = gridGeometry.getGridRange();
            final int[] low  = gridRange.getLow ().getCoordinateValues();
            final int[] high = gridRange.getHigh().getCoordinateValues();
            low[xi] = xmin; high[xi] = xmin + image.getWidth();
            low[yi] = ymin; high[yi] = ymin + image.getHeight();
            final GridEnvelope newGridRange = new GeneralGridEnvelope(low, high, false);
            if (newGridToCRS != gridToCRS || !newGridRange.equals(gridRange)) {
                gridGeometry = new GridGeometry2D(newGridRange, newGridToCRS,
                        gridGeometry.getCoordinateReferenceSystem());
            }
        }
        return factory.create(name, image, gridGeometry,
                (bands != null) ? bands.toArray(new GridSampleDimension[bands.size()]) : null,
                null, properties);
    }

    /**
     * Computes the region to read in pixel coordinates. The main purpose of this method is to
     * be invoked just before an image is read, but it could also be invoked by some informative
     * methods like {@code getGridGeometry(GridCoverageReadParam)} (if we decide to add such method).
     *
     * @param gridGeometry  The grid geometry for the whole coverage,
     *                      as provided by {@link #getGridGeometry(int)}.
     * @param envelope      The region to read in "real world" coordinates, or {@code null}.
     *                      The CRS of this envelope doesn't need to be the coverage CRS.
     * @param resolution    On input, the requested resolution or {@code null}. On output
     *                      (if non-null), the subsampling to use for reading the image.
     * @return The region to be read in pixel coordinates, or {@code null} if the coverage
     *         can't be read because the region to read is empty.
     */
    private Rectangle computeBounds(final GridGeometry2D gridGeometry, Envelope envelope, final Dimension2D resolution)
            throws InvalidGridGeometryException, NoninvertibleTransformException, TransformException
    {
        final Rectangle gridRange = gridGeometry.getGridRange2D();
        final int width  = gridRange.width;
        final int height = gridRange.height;
        final AffineTransform gridToCRS = getGridToCRS(gridGeometry);
        final AffineTransform crsToGrid = gridToCRS.createInverse();
        /*
         * Get the full coverage envelope in the coverage CRS. The returned shape is likely
         * (but not garanteed) to be an instance of Rectangle2D. It can be freely modified.
         */
        Shape shapeToRead = XAffineTransform.transform(gridToCRS, gridRange, false); // Will be clipped later.
        Rectangle2D geodeticBounds = (shapeToRead instanceof Rectangle2D) ?
                (Rectangle2D) shapeToRead : shapeToRead.getBounds2D();
        if (geodeticBounds.isEmpty()) {
            return null;
        }
        /*
         * Check if the requested region (requestEnvelope) intersects the coverage region (shapeToRead).
         */
        XRectangle2D requestRect = null;
        if (envelope != null) {
            envelope = CRS.transform(envelope, gridGeometry.getCoordinateReferenceSystem2D());
            requestRect = XRectangle2D.createFromExtremums(
                    envelope.getMinimum(0), envelope.getMinimum(1),
                    envelope.getMaximum(0), envelope.getMaximum(1));
            if (requestRect.isEmpty() || !XRectangle2D.intersectInclusive(requestRect, geodeticBounds)) {
                return null;
            }
            /*
             * If the requested envelope contains fully the coverage bounds, we can ignore it
             * (we will read the full coverage). Otherwise if the coverage contains full the
             * requested region, the requested region become the new bounds. Otherwise we need
             * to compute the intersection.
             */
            if (!requestRect.contains(geodeticBounds)) {
                if (shapeToRead.contains(requestRect)) {
                    shapeToRead = geodeticBounds = requestRect;
                } else {
                    final Area area = new Area(shapeToRead);
                    area.intersect(new Area(requestRect));
                    shapeToRead = area;
                    geodeticBounds = shapeToRead.getBounds2D();
                    if (geodeticBounds.isEmpty()) {
                        return null;
                    }
                }
            }
        }
        /*
         * Transforms ["real world" envelope] --> [region in pixel coordinates] and computes the
         * subsampling from the desired resolution.  Note that we transform 'shapeToRead' (which
         * is a generic shape) rather than Rectangle2D instances, because operating on Shape can
         * give a smaller envelope when the transform contains rotation terms.
         */
        double sx = geodeticBounds.getWidth();  // "Real world" size of the region to be read.
        double sy = geodeticBounds.getHeight(); // Need to be extracted before the line below.
        shapeToRead = XAffineTransform.transform(crsToGrid, shapeToRead, shapeToRead != gridRange);
        final RectangularShape imageRegion = (shapeToRead instanceof RectangularShape) ?
                (RectangularShape) shapeToRead : shapeToRead.getBounds2D();
        sx = imageRegion.getWidth()  / sx;  // (sx,sy) are now conversion factors
        sy = imageRegion.getHeight() / sy;  // from "real world" to pixel coordinates.
        final int xSubsampling;
        final int ySubsampling;
        if (resolution != null) {
            sx *= resolution.getWidth();
            sy *= resolution.getHeight();
            xSubsampling = Math.max(1, Math.min(width /MIN_SIZE, (int) (sx + EPS)));
            ySubsampling = Math.max(1, Math.min(height/MIN_SIZE, (int) (sy + EPS)));
            resolution.setSize(xSubsampling, ySubsampling);
        } else {
            xSubsampling = 1;
            ySubsampling = 1;
        }
        /*
         * Cast the imageRegion in a Rectangle and make sure that it is contained inside the
         * RenderedImage valid bounds. We need to ensure that in order to prevent ImageReader
         * to do its own clipping (at least for the minimal X and Y values), which would cause
         * the 'gridToCRS' transform to be wrong.
         */
        final double xmin = Math.floor(imageRegion.getMinX() + EPS);
        final double ymin = Math.floor(imageRegion.getMinY() + EPS);
        final double xmax = Math.ceil (imageRegion.getMaxX() - EPS);
        final double ymax = Math.ceil (imageRegion.getMaxY() - EPS);
        final Rectangle imageBounds = new Rectangle();
        imageBounds.setRect(xmin, ymin, xmax - xmin, ymax - ymin);
        if (imageBounds.width <  MIN_SIZE) {
            imageBounds.x    -= (MIN_SIZE - imageBounds.width)/2;
            imageBounds.width =  MIN_SIZE;
        }
        if (imageBounds.height < MIN_SIZE) {
            imageBounds.y     -= (MIN_SIZE - imageBounds.height)/2;
            imageBounds.height = MIN_SIZE;
        }
        final int maxX = Math.min(width,  imageBounds.width  + imageBounds.x);
        final int maxY = Math.min(height, imageBounds.height + imageBounds.y);
        if (imageBounds.x < 0) imageBounds.x = 0;
        if (imageBounds.y < 0) imageBounds.y = 0;
        imageBounds.width  = maxX - imageBounds.x;
        imageBounds.height = maxY - imageBounds.y;
        return imageBounds;
    }

    /**
     * Returns an error message for the given exception. If the {@linkplain #input input} is
     * known, this method returns "<cite>Can't read 'the name'</cite>" followed by the cause
     * message. Otherwise it returns the localized message of the given exception.
     */
    private String error(final Exception e) {
        return error(input, e);
    }

    /**
     * Same than {@link #error(Exception)}, but with an explicitly specified input.
     */
    private String error(final Object input, final Exception e) {
        String message = e.getLocalizedMessage();
        if (IOUtilities.canProcessAsPath(input)) {
            final String cause = message;
            message = Errors.getResources(locale).getString(Errors.Keys.CANT_READ_$1, IOUtilities.name(input));
            if (cause != null && cause.indexOf(' ') > 0) { // Append only if we have a sentence.
                message = message + '\n' + cause;
            }
        }
        return message;
    }

    /**
     * Closes the input used by the {@link ImageReader}, provided that this is not the input
     * object explicitly given by the user. The {@link ImageReader} is not disposed, so it
     * can be reused for the next image to read.
     *
     * @throws IOException if an error occurs while closing the input.
     */
    private void close() throws IOException {
        coverageNames = null;
        final ImageReader imageReader = this.imageReader; // Protect from changes.
        final Object oldInput = input;
        input = null; // Clear now in case the code below fails.
        if (imageReader != null) {
            final Object readerInput = imageReader.getInput();
            imageReader.setInput(null);
            if (readerInput != oldInput) {
                if (readerInput instanceof Closeable) {
                    ((Closeable) readerInput).close();
                } else if (readerInput instanceof ImageInputStream) {
                    ((ImageInputStream) readerInput).close();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method {@linkplain ImageReader#dispose() disposes} the {@linkplain #imageReader
     * image reader} if and only if it was not an instance provided explicitly by the user.
     *
     * @see ImageReader#dispose()
     */
    @Override
    public void reset() throws CoverageStoreException {
        final ImageReader reader = imageReader;
        final boolean disposeReader = (reader != null && reader != input);
        try {
            close();
        } catch (IOException e) {
            throw new CoverageStoreException(e);
        }
        if (disposeReader) {
            reader.dispose();
        }
        imageReader = null;
        helper = null;
        super.reset();
    }
}
