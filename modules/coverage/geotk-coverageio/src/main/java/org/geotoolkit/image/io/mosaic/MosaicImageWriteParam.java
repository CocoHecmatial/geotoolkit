/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008-2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.image.io.mosaic;

import java.io.File;
import java.awt.Color;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import javax.imageio.ImageWriteParam;

import org.geotoolkit.resources.Errors;
import org.geotoolkit.image.ImageWorker;
import org.geotoolkit.internal.image.SimpleBufferedImageOp;


/**
 * The parameters for {@link MosaicImageWriter}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.00
 *
 * @since 2.5
 * @module
 */
public class MosaicImageWriteParam extends ImageWriteParam {
    /**
     * The index of the {@linkplain TileManager tile manager} to use in the array returned by
     * {@link MosaicImageWriter#getOutput}.
     */
    private int outputIndex = 0;

    /**
     * Controls the way {@link MosaicImageWriter} writes the tiles.
     */
    private TileWritingPolicy policy = TileWritingPolicy.OVERWRITE;

    /**
     * An optional operation to apply on source tiles before to create the mosaic.
     */
    private BufferedImageOp sourceTileFilter;

    /**
     * Constructs an empty set of parameters.
     */
    public MosaicImageWriteParam() {
    }

    /**
     * Creates a new set of parameters with the same mosaic-specific parameters than the given
     * one, and the default value for all other parameters.
     * <p>
     * This method is not public because for a public API, it would be cleaner to copy all
     * parameters (as we usually expect from a copy constructor), while {@link MosaicImageWriter}
     * really needs to copy only the mosaic-specific parameters and left the other ones to their
     * default value.
     *
     * @param copy The parameters to copy.
     */
    MosaicImageWriteParam(final MosaicImageWriteParam copy) {
        outputIndex = copy.outputIndex;
        policy      = copy.policy;
    }

    /**
     * Returns the index of the image to be written. This is the index of the
     * {@linkplain TileManager tile manager} to use in the array returned by
     * {@link MosaicImageWriter#getOutput}. The default value is 0.
     *
     * @return The index of the image to be written.
     */
    public int getOutputIndex() {
        return outputIndex;
    }

    /**
     * Sets the index of the image to be written. This is the index of the
     * {@linkplain TileManager tile manager} to use in the array returned by
     * {@link MosaicImageWriter#getOutput}. The default value is 0.
     *
     * @param index The index of the image to be written.
     */
    public void setOutputIndex(final int index) {
        if (index < 0 || index > Tile.MASK) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.VALUE_OUT_OF_BOUNDS_$3, index, 0, Tile.MASK));
        }
        outputIndex = index;
    }

    /**
     * Returns whatever existings {@linkplain File files} should be skipped or overwritten.
     * The default value is {@link TileWritingPolicy#OVERWRITE OVERWRITE}.
     *
     * @return The policy to apply when writting tiles.
     */
    public TileWritingPolicy getTileWritingPolicy() {
        return policy;
    }

    /**
     * Sets whatever existings {@linkplain File files} should be skipped. The default behavior
     * is to {@linkplain TileWritingPolicy#OVERWRITE overwrite} every files inconditionnaly.
     * Settings the policy to {@link TileWritingPolicy#WRITE_NEWS_ONLY WRITE_NEWS_ONLY} may
     * speedup {@link MosaicImageWriter} when the process of writting tiles is started again
     * after a previous partial failure, by skipping the tiles that were successfully generated
     * in the previous run.
     *
     * @param policy The policy to apply when writting tiles.
     */
    public void setTileWritingPolicy(final TileWritingPolicy policy) {
        Tile.ensureNonNull("policy", policy);
        this.policy = policy;
    }

    /**
     * Returns an optional operation to apply on source tiles before to create the mosaic.
     *
     * @return The operation to apply on source tiles, or {@code null} if none.
     *
     * @since 3.00
     */
    public BufferedImageOp getSourceTileFilter() {
        return sourceTileFilter;
    }

    /**
     * Sets the operation to apply on source tiles before to create the mosaic. If an operation
     * is given, then that operation will be applied on every source tiles and the result will
     * be saved as RAW images in temporary files. The temporary files will be removed when the
     * mosaic creation is finihed.
     *
     * @param filter The operation to apply on source tiles, or {@code null} if none.
     *
     * @since 3.00
     */
    public void setSourceTileFilter(final BufferedImageOp filter) {
        sourceTileFilter = filter;
    }

    /**
     * Sets the {@linkplain #setSourceTileFilter source tile filter} to an operation that
     * replace the given set of opaque colors by fully transparent pixels. Only the colors
     * in the neighbor of the tile borders are replaced, as described in the
     * {@link org.geotoolkit.image.jai.SilhouetteMask} operation.
     *
     * @param colors The border colors to make transparent. The colors are presumed opaque,
     *        i.e. alpha values are ignored.
     *
     * @since 3.00
     */
    public void setOpaqueBorderFilter(final Color[] colors) {
        setSourceTileFilter(new BorderFilter(colors));
    }

    /**
     * An operation that replace opaque color by fully transparent pixels.
     */
    private static final class BorderFilter extends SimpleBufferedImageOp {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 890880010471839924L;

        /**
         * The opaque border color to make transparent.
         */
        private final Color[] colors;

        /**
         * Creates a new filter for the given opaque border colors to make transparent.
         */
        BorderFilter(final Color[] colors) {
            this.colors = colors.clone();
        }

        /**
         * Performs the filter, which replace opaque border colors by fully transparent pixels.
         */
        @Override
        public RenderedImage filter(final BufferedImage src) {
            final double[] RGB = new double[3];
            final ImageWorker worker = new ImageWorker(src);
            for (int i=0; i<colors.length; i++) {
                final Color color = colors[i];
                RGB[0] = color.getRed();
                RGB[1] = color.getGreen();
                RGB[2] = color.getBlue();
                worker.maskBackground(RGB, null);
            }
            return worker.getRenderedImage();
        }
    }
}
