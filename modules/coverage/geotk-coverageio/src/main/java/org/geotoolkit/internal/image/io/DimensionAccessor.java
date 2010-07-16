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
package org.geotoolkit.internal.image.io;

import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.measure.unit.Unit;

import org.opengis.metadata.content.TransferFunctionType;

import org.geotoolkit.util.NumberRange;
import org.geotoolkit.internal.image.ImageUtilities;
import org.geotoolkit.image.io.metadata.MetadataAccessor;

import static org.geotoolkit.image.io.metadata.SpatialMetadataFormat.FORMAT_NAME;


/**
 * A convenience specialization of {@link MetadataAccessor} for the
 * {@code "ImageDescription/Dimensions"} node. Example:
 *
 * {@preformat java
 *     SpatialMetadata metadata = new SpatialMetadata(SpatialMetadataFormat.IMAGE);
 *     DimensionAccessor accessor = new DimensionAccessor(metadata);
 *     accessor.selectChild(accessor.appendChild());
 *     accessor.setValueRange(-100, 2000);
 * }
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.14
 *
 * @since 3.06
 * @module
 */
public final class DimensionAccessor extends MetadataAccessor {
    /**
     * Creates a new accessor for the given metadata.
     *
     * @param metadata The Image I/O metadata. An instance of the
     *        {@link org.geotoolkit.image.io.metadata.SpatialMetadata}
     *        sub-class is recommanded, but not mandatory.
     */
    public DimensionAccessor(final IIOMetadata metadata) {
        super(metadata, FORMAT_NAME, "ImageDescription/Dimensions", "Dimension");
    }

    /**
     * Sets the {@code "descriptor"} attribute to the given value.
     *
     * @param descriptor The descriptor, or {@code null} if none.
     */
    public void setDescriptor(final String descriptor) {
        setAttribute("descriptor", descriptor);
    }

    /**
     * Sets the {@code "units"} attribute to the given value.
     *
     * @param units The units, or {@code null} if none.
     */
    public void setUnits(final String units) {
        setAttribute("units", units);
    }

    /**
     * Sets the {@code "units"} attribute to the given value.
     *
     * @param units The units, or {@code null} if none.
     */
    public void setUnits(final Unit<?> units) {
        setAttribute("units", units);
    }

    /**
     * Sets the {@code "minValue"} and {@code "maxValue"} attributes to the given range.
     * They are the geophysical value, already transformed by the transfert function if
     * there is one.
     *
     * @param minimum The value to be assigned to the {@code "minValue"} attribute.
     * @param maximum The value to be assigned to the {@code "maxValue"} attribute.
     */
    public void setValueRange(final float minimum, final float maximum) {
        setAttribute("minValue", minimum);
        setAttribute("maxValue", maximum);
    }

    /**
     * Sets the {@code "minValue"} and {@code "maxValue"} attributes to the given range.
     * They are the geophysical value, already transformed by the transfert function if
     * there is one.
     *
     * @param minimum The value to be assigned to the {@code "minValue"} attribute.
     * @param maximum The value to be assigned to the {@code "maxValue"} attribute.
     */
    public void setValueRange(final double minimum, final double maximum) {
        setAttribute("minValue", minimum);
        setAttribute("maxValue", maximum);
    }

    /**
     * Sets the {@code "validSampleValues"} attribute to the given range. This is the range of
     * values encoded in the file, before the transformation by the transfert function if there
     * is one.
     * <p>
     * This method does nothing if the given range is infinite.
     *
     * @param minimum The minimal sample value, inclusive.
     * @param maximum The maximal sample value, inclusive.
     */
    public void setValidSampleValue(final double minimum, final double maximum) {
        if (minimum <= maximum && !Double.isInfinite(minimum) && !Double.isInfinite(maximum)) {
            setValidSampleValue(NumberRange.createBestFit(minimum, true, maximum, true));
        }
    }

    /**
     * Sets the {@code "validSampleValues"} attribute to the given range. This is the range of
     * values encoded in the file, before the transformation by the transfert function if there
     * is one.
     *
     * @param range The value to be assigned to the {@code "validSampleValues"} attribute.
     */
    public void setValidSampleValue(final NumberRange<?> range) {
        setAttribute("validSampleValues", range);
    }

    /**
     * Sets the {@code "fillSampleValues"} attribute to the given value.
     *
     * @param value The value to be assigned to the {@code "fillSampleValues"} attribute.
     */
    public void setFillSampleValues(final int value) {
        setAttribute("fillSampleValues", value);
    }

    /**
     * Sets the {@code "fillSampleValues"} attribute to the given array.
     *
     * @param values The values to be assigned to the {@code "fillSampleValues"} attribute.
     */
    public void setFillSampleValues(final int... values) {
        setAttribute("fillSampleValues", values);
    }

    /**
     * Sets the {@code "fillSampleValues"} attribute to the given value.
     *
     * @param value The value to be assigned to the {@code "fillSampleValues"} attribute.
     */
    public void setFillSampleValues(final float value) {
        setAttribute("fillSampleValues", value);
    }

    /**
     * Sets the {@code "fillSampleValues"} attribute to the given array.
     *
     * @param values The values to be assigned to the {@code "fillSampleValues"} attribute.
     */
    public void setFillSampleValues(final float... values) {
        setAttribute("fillSampleValues", values);
    }

    /**
     * Sets the {@code "fillSampleValues"} attribute to the given value.
     *
     * @param value The value to be assigned to the {@code "fillSampleValues"} attribute.
     */
    public void setFillSampleValues(final double value) {
        setAttribute("fillSampleValues", value);
    }

    /**
     * Sets the {@code "fillSampleValues"} attribute to the given array.
     *
     * @param values The values to be assigned to the {@code "fillSampleValues"} attribute.
     */
    public void setFillSampleValues(final double... values) {
        setAttribute("fillSampleValues", values);
    }

    /**
     * Sets the {@code "scaleFactor"}, {@code "offset"} and {@code "transferFunctionType"}
     * attributes to the given values.
     *
     * @param scale  The value to be assigned to the {@code "scaleFactor"} attribute.
     * @param offset The value to be assigned to the {@code "offset"} attribute.
     * @param type   The value to be assigned to the {@code "transferFunctionType"} attribute.
     */
    public void setTransfertFunction(final double scale, final double offset, final TransferFunctionType type) {
        setAttribute("scaleFactor", scale);
        setAttribute("offset", offset);
        setAttribute("transferFunctionType", type);
    }

    /**
     * Sets the minimum and maximum values from the pixel values. This method is costly
     * and should be invoked only for relatively small images, after we checked that the
     * extremums are not already declared in the metadata.
     *
     * @param  reader The image reader to use for reading the pixel values.
     * @param  imageIndex The index of the image to read (usually 0).
     * @throws IOException If an error occured while reading the image.
     *
     * @since 3.14
     */
    public void scanValidSampleValue(final ImageReader reader, final int imageIndex) throws IOException {
        int bandIndex = 0;
        final RectIter iter = RectIterFactory.create(reader.readAsRenderedImage(imageIndex, null), null);
        iter.startBands();
        if (!iter.finishedBands()) do {
            if (bandIndex >= childCount()) {
                bandIndex = appendChild();
            }
            selectChild(bandIndex);
            setAttribute("minValue", Double.NaN);
            setAttribute("maxValue", Double.NaN);
            final double[] padValues = getAttributeAsDoubles("fillSampleValues", true);
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            iter.startLines();
            if (!iter.finishedLines()) do {
                iter.startPixels();
                if (!iter.finishedPixels()) {
nextPixel:          do {
                        final double sample = iter.getSampleDouble();
                        if (padValues != null) {
                            for (final double v : padValues) {
                                if (sample == v) {
                                    continue nextPixel;
                                }
                            }
                        }
                        if (sample < min) min = sample;
                        if (sample > max) max = sample;
                    } while (!iter.nextPixelDone());
                }
            } while (!iter.nextLineDone());
            setValidSampleValue(min, max);
            // Do not invoke setValueRange(min, max) because the
            // later is about geophysics values, not sample values.
            bandIndex++;
        } while (!iter.nextBandDone());
    }

    /**
     * Returns {@code true} if a call to {@link #scanValidSampleValue(ImageReader, int)} is
     * recommanded. This method uses heuristic rules that may be changed in any future version.
     *
     * @param  reader The image reader to use for reading information.
     * @param  imageIndex The index of the image to query (usually 0).
     * @return {@code true} if a call to {@code scanValidSampleValue} is recommanded.
     * @throws IOException If an error occured while querying the image.
     *
     * @since 3.14
     */
    public boolean scanSuggested(final ImageReader reader, final int imageIndex) throws IOException {
        final int numChilds = childCount();
        for (int i=0; i<numChilds; i++) {
            selectChild(i);
            if (getAttribute("validSampleValues") == null) {
                final Double minValue = getAttributeAsDouble("minValue");
                final Double maxValue = getAttributeAsDouble("maxValue");
                if (minValue == null || maxValue == null || !(minValue <= maxValue)) { // Une '!' for catching NaN.
                    /*
                     * Stop the band scanning whatever happen: if a scan is recommanded for at least
                     * one band, do the scan. If we don't have float type, we don't need to continue
                     * since this method will never returns 'true' in such case.
                     */
                    return ImageUtilities.isFloatType(reader.getRawImageType(imageIndex).getSampleModel().getDataType());
                }
            }
        }
        return false;
    }
}
