/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.internal.image.io;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import javax.imageio.ImageWriter;
import javax.imageio.IIOImage;
import javax.imageio.IIOParam;
import javax.imageio.metadata.IIOMetadata;

import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.coverage.grid.RectifiedGrid;
import org.opengis.metadata.spatial.Georectified;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.MathTransform;

import org.geotoolkit.internal.image.ImageUtilities;
import org.geotoolkit.image.io.ImageMetadataException;
import org.geotoolkit.image.io.metadata.MetadataHelper;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.geotoolkit.image.io.metadata.SpatialMetadataFormat;
import org.geotoolkit.metadata.iso.spatial.PixelTranslation;
import org.geotoolkit.referencing.cs.DefaultCartesianCS;
import org.geotoolkit.resources.Errors;

import static org.geotoolkit.image.io.MultidimensionalImageStore.*;


/**
 * A helper class for processing the {@link IIOImage} and {@link IIOParam} information before to
 * write an image.
 * <p>
 * This class is defined in the NetCDF module because the NetCDF writer is currently the only
 * one to use it. However we may move it to an other module if it happen to be useful for other
 * writers too.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.20
 *
 * @since 3.20
 * @module
 */
public class IIOImageHelper {
    /**
     * The number of bands in the source image.
     * Shall be equals or greater than the length of the {@link #sourceBands} array.
     */
    private final int numBands;

    /**
     * The smallest rectangle that fully encompass the region to read from the source (never
     * {@code null}). This rectangle is computed as the intersection of the user-supplied region
     * (if any) and the image bounds.  If a sub-sampling has been specified, it will be taken in
     * account in order to ensure that the first and last row and column are included in the set
     * of pixels coordinates to iterate.
     *
     * @see #getSourceRegionCenter()
     */
    public final Rectangle sourceRegion;

    /**
     * The bands to read, or {@code null} for reading all bands.
     * The length of this array shall be equals or less than {@link #numBands}.
     *
     * @see #hasSourceBands()
     * @see #getSourceBands()
     */
    private final int[] sourceBands;

    /**
     * The sub-sampling, or 1 if none.
     *
     * @see #hasSubsampling()
     */
    public final int sourceXSubsampling, sourceYSubsampling;


    // ---- The above variables were working on standard image parameters only. ----------------
    // ---- The remaining fields below work on spatial metadata.                ----------------

    /**
     * The spatial metadata about the image to be written, or {@code null} if none.
     */
    public final SpatialMetadata metadata;

    /**
     * The coordinate system of the image to write, or {@code null} if not yet computed.
     * Note that the CS may have more than 2 dimensions.
     *
     * @see #getCoordinateSystem()
     */
    private CoordinateSystem coordinateSystem;

    /**
     * {@code true} if already checked for {@link RectifiedGrid} information in the
     * {@linkplain #metadata}. If {@code true}, then the value of {@link #gridToCRS}
     * and {@link #gridDomain} are considered final, even if they are null.
     */
    private boolean isGridGeometryComputed;

    /**
     * The conversion from grid to CRS coordinates, or {@code null} if not yet computed.
     * This transform is <strong>not</strong> adjusted for user parameters (sub-region,
     * sub-sampling).
     */
    private MathTransform gridToCRS;

    /**
     * The lower and upper bounds of the grid, or {@code null} if not yet computed.
     * This extent is <strong>not</strong> adjusted for user parameters (sub-region,
     * sub-sampling).
     */
    private GridEnvelope gridDomain;

    /**
     * Coordinates in the center of the image to write, or {@code null} if not yet computed.
     */
    private double[] gridCenter;

    /**
     * Creates a helper for the specified image and parameters.
     *
     * @param writer The writer which is preparing the image to write, or {@code null} if unknown.
     * @param image  The image or raster to be read or written.
     * @param param  The parameters that control the writing process, or {@code null} if none.
     */
    public IIOImageHelper(final ImageWriter writer, final IIOImage image, final IIOParam param) {
        /*
         * This code is a duplication of the SpatialImageWriter.createRectIter(...) method,
         * except that we unconditionally compute the intersection with the image bounds.
         */
        final Rectangle bounds;
        if (image.hasRaster()) {
            final Raster raster = image.getRaster();
            bounds   = raster.getBounds(); // Needs to be a clone.
            numBands = raster.getNumBands();
        } else {
            final RenderedImage raster = image.getRenderedImage();
            bounds   = ImageUtilities.getBounds(raster);
            numBands = raster.getSampleModel().getNumBands();
        }
        /*
         * Examines the parameters for subsampling in lines, columns and bands. If a subsampling
         * is specified, the source region will be translated by the subsampling offset (if any).
         */
        if (param != null) {
            Rectangle region   = param.getSourceRegion();
            sourceXSubsampling = param.getSourceXSubsampling();
            sourceYSubsampling = param.getSourceYSubsampling();
            if (region == null) {
                region = bounds;
            } else {
                region = region.intersection(bounds);
            }
            if (hasSubsampling()) {
                final int xOffset = param.getSubsamplingXOffset();
                final int yOffset = param.getSubsamplingYOffset();
                region.x      += xOffset;
                region.y      += yOffset;
                region.width  -= xOffset;
                region.height -= yOffset;
                // Fits to the smallest bounding box, which is
                // required by SubsampledRectIter implementation.
                region.width  -= (region.width  - 1) % sourceXSubsampling;
                region.height -= (region.height - 1) % sourceYSubsampling;
            }
            sourceRegion = region;
            sourceBands  = param.getSourceBands();
        } else {
            sourceRegion       = bounds;
            sourceBands        = null;
            sourceXSubsampling = 1;
            sourceYSubsampling = 1;
        }
        /*
         * The code below this point were working on standard image parameters only.
         * The code below this point work on Geotk-specific metadata.
         */
        final IIOMetadata md = image.getMetadata();
        metadata = (md == null || md instanceof SpatialMetadata) ? (SpatialMetadata) md :
                    new SpatialMetadata(SpatialMetadataFormat.getImageInstance(null), writer, md);
    }

    /**
     * Returns {@code true} if iteration over the pixel values will perform sub-sampling.
     *
     * @return {@code true} if the parameters given at construction time specify a
     *         sub-sampling, either horizontal, vertical or both.
     */
    public final boolean hasSubsampling() {
        return sourceXSubsampling != 1 || sourceYSubsampling != 1;
    }

    /**
     * Returns {@code true} if the {@link #sourceBands} array is non-null and is different
     * than the default array of bands. The default source bands is all bands from the source,
     * so this method returns {@code true} if the user asked either for a bands subset, or for
     * bands in a different order.
     *
     * @return {@code true} if the source bands are different than the default set of bands to read.
     */
    public final boolean hasSourceBands() {
        if (sourceBands != null) {
            if (sourceBands.length != numBands) {
                return true;
            }
            for (int i=0; i<numBands; i++) {
                if (sourceBands[i] != i) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the source bands as a non-null array. If the user didn't specified explicitely
     * the source bands, then this method returns the default sequence for all bands.
     *
     * @return The source bands (never {@code null}). <strong>Do not modify</strong>,
     *         since this method does not clone the internal array.
     */
    public final int[] getSourceBands() {
        if (sourceBands != null) {
            return sourceBands;
        }
        final int[] sequence = new int[numBands];
        for (int i=0; i<sequence.length; i++) {
            sequence[i] = i;
        }
        return sequence;
    }

    /**
     * Returns the coordinate system of the image to write.
     * Note that the CS may have more than 2 dimensions.
     * <p>
     * This method never returns null - if no coordinate system can be inferred from the
     * {@linkplain #metadata}, then the default is {@link DefaultCartesianCS#GRID}.
     *
     * @return The image coordinate system (never {@code null}).
     * @throws ImageMetadataException If an error occurred while computing the coordinate system.
     */
    public final CoordinateSystem getCoordinateSystem() throws ImageMetadataException {
        CoordinateSystem cs = coordinateSystem;
        if (cs == null) {
            if (metadata != null) {
                final CoordinateReferenceSystem crs = metadata.getInstanceForType(CoordinateReferenceSystem.class);
                if (crs != null) {
                    cs = crs.getCoordinateSystem();
                }
            }
            if (cs == null) {
                cs = DefaultCartesianCS.GRID;
            } else {
                final int dim = cs.getDimension();
                if (dim < 2) {
                    throw new ImageMetadataException(Errors.format(Errors.Keys.ILLEGAL_CS_DIMENSION_$1, dim));
                }
            }
            coordinateSystem = cs;
        }
        return cs;
    }

    /**
     * Computes the {@link #gridToCRS}, {@link #gridDomain} and {@link #gridCenter} fields. Any of
     * those fields may still be {@code null} after this method call if they couldn't be computed.
     *
     * @throws ImageMetadataException If an error occurred while computing the grid geometry.
     */
    private void computeGridGeometry() throws ImageMetadataException {
        /*
         * Maybe the coordinate system contains itself the information we are looking for.
         * This happen for example if the CS has been created by NetcdfImageReader.
         */
        final CoordinateSystem cs = getCoordinateSystem();
        if (cs instanceof GridGeometry) {
            final GridGeometry gridGeometry = (GridGeometry) cs;
            gridToCRS  = gridGeometry.getGridToCRS();
            gridDomain = gridGeometry.getExtent();
        } else {
            final RectifiedGrid domain = metadata.getInstanceForType(RectifiedGrid.class);
            if (domain != null) {
                gridDomain = domain.getExtent();
                gridToCRS  = MetadataHelper.INSTANCE.getGridToCRS(domain);
                final Georectified rectified = metadata.getInstanceForType(Georectified.class);
                if (rectified != null) {
                    final PixelOrientation orientation = rectified.getPointInPixel();
                    if (orientation != null) {
                        gridToCRS = PixelTranslation.translate(gridToCRS, orientation,
                                PixelOrientation.CENTER, X_DIMENSION, Y_DIMENSION);
                    }
                }
            }
        }
        isGridGeometryComputed = true;
    }

    /**
     * Returns the conversion from grid to CRS coordinates, or {@code null} if not yet computed.
     * This transform is <strong>not</strong> adjusted for user parameters (sub-region, sub-sampling).
     *
     * @return The conversion from grid to CRS coordinates, or {@code null} if none.
     * @throws ImageMetadataException If an error occurred while computing the grid geometry.
     */
    public MathTransform getGridToCRS() throws ImageMetadataException {
        if (!isGridGeometryComputed) {
            computeGridGeometry();
        }
        return gridToCRS;
    }

    /**
     * Returns the center of the source region. This method returns an array of length 2 or greater
     * since the grid extent may have more than 2 dimensions. Note that this method returns a direct
     * reference to the internal array; do not modify.
     * <p>
     * This method does not make any adjustment for sub-sampling. If the returned array needs to be
     * transformed using a <cite>grid to CRS</cite> transform, use a transform <strong>without</strong>
     * adjustment for sub-sampling.
     *
     * @return The center of the source region in grid coordinates units,
     *         as a direct reference to the internal array.
     * @throws ImageMetadataException If an error occurred while computing the grid geometry.
     */
    public final double[] getSourceRegionCenter() throws ImageMetadataException {
        if (gridCenter == null) {
            if (!isGridGeometryComputed) {
                computeGridGeometry();
            }
            gridCenter = new double[(gridDomain != null) ? gridDomain.getDimension() : coordinateSystem.getDimension()];
            for (int i=0; i<gridCenter.length; i++) {
                final int low, span;
                switch (i) {
                    case X_DIMENSION: low=sourceRegion.x; span=sourceRegion.width;  break;
                    case Y_DIMENSION: low=sourceRegion.y; span=sourceRegion.height; break;
                    default: {
                        if (gridDomain == null) {
                            continue; // Let the gridCenter[i] ordinate to zero.
                        }
                        low  = gridDomain.getLow(i);
                        span = gridDomain.getSpan(i);
                        break;
                    }
                }
                gridCenter[i] = low + 0.5*span;
            }
        }
        return gridCenter;
    }
}
