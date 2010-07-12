/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007-2010, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.image.io.plugin;

import java.awt.image.DataBuffer;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.VariableIF;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dataset.EnhanceScaleMissing;

import org.geotoolkit.util.XArrays;


/**
 * Parses the offset, scale factor, minimum, maximum and fill values from a variable. This class
 * duplicate UCAR's {@code EnhanceScaleMissingImpl} functionality, but we have to do that because:
 * <p>
 * <ul>
 *   <li>I have not been able to find any method giving me directly the offset and scale factor.
 *       We can use some trick with {@link EnhanceScaleMissing#convertScaleOffsetMissing}, but
 *       they are subject to rounding errors and there is no efficient way I can see to take
 *       missing values in account.</li>
 *   <li>The {@link EnhanceScaleMissing} methods are available only if the variable is enhanced.
 *       Our variable is not, because we want raw (packed) data.</li>
 *   <li>We want minimum, maximum and fill values in packed units (as opposed to the geophysics
 *       values provided by the UCAR's API), because we check for missing values before to
 *       convert them.</li>
 * </ul>
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.14
 *
 * @since 3.08 (derived from 2.4)
 * @module
 */
final class NetcdfVariable {
    /**
     * NetCDF attributes processed by this class.
     */
    static final String
            VALID_MIN     = "valid_min",
            VALID_MAX     = "valid_max",
            VALID_RANGE   = "valid_range",
            SCALE_FACTOR  = "scale_factor",
            ADD_OFFSET    = "add_offset",
            MISSING_VALUE = "missing_value",
            FILL_VALUE    = "_FillValue";

    /**
     * The data type to accept in images. Used for automatic detection of which variables
     * to assign to images.
     *
     * @see #getRawDataType(VariableIF)
     */
    static final Set<DataType> VALID_TYPES;
    static {
        final Set<DataType> types = EnumSet.noneOf(DataType.class);
        types.add(DataType.BOOLEAN);
        types.add(DataType.BYTE);
        types.add(DataType.SHORT);
        types.add(DataType.INT);
        types.add(DataType.LONG);
        types.add(DataType.FLOAT);
        types.add(DataType.DOUBLE);
        VALID_TYPES = Collections.unmodifiableSet(types);
    }

    /**
     * Raw image type as one of {@link DataBuffer} constants.
     */
    public final int imageType;

    /**
     * The scale and and offset values, or {@link Double#NaN NaN} if none.
     */
    public final double scale, offset;

    /**
     * The minimal and maximal valid values in packed (not geophysics) units, or
     * infinity if none. They are converted from the geophysics values if needed.
     */
    public final double minimum, maximum;

    /**
     * The fill and missing values in <strong>packed</strong> units, or {@code null} if none.
     * Note that this is different from what UCAR does (they convert to geophysics values).
     * We keep packed values in order to avoir rounding error. This array contains both the
     * fill value and the missing values, without duplicated values.
     */
    public final double[] fillValues;

    /**
     * The units as an unparsed string, or {@code null} if unknown.
     */
    public final String units;

    /**
     * The widest type found in attributes scanned by the {@link #attribute} method
     * since the last time this field was set. This is a temporary variable used by
     * the constructor only.
     */
    private transient DataType widestType;

    /**
     * Extracts metadata from the specified variable using UCAR's API. This approach suffers
     * from rounding errors and is unable to get the missing values. Use this constructor
     * only for comparing our own results with the results from the UCAR's API.
     */
    public NetcdfVariable(final EnhanceScaleMissing variable) {
        if (variable instanceof VariableIF) {
            final VariableIF vif = (VariableIF) variable;
            imageType = getRawDataType(vif);
            units     = vif.getUnitsString();
        } else {
            imageType = DataBuffer.TYPE_DOUBLE;
            units     = null;
        }
        offset     =  variable.convertScaleOffsetMissing(0.0);
        scale      =  variable.convertScaleOffsetMissing(1.0) - offset;
        minimum    = (variable.getValidMin() - offset) / scale;
        maximum    = (variable.getValidMax() - offset) / scale;
        fillValues = null; // No way to get this information.
    }

    /**
     * Extracts metadata from the specified variable using our own method.
     * The <A HREF="http://www.cfconventions.org/">CF Metadata conventions</A> states that valid
     * ranges should be in packed units, but not every NetCDF files follow this advice in practice.
     * The UCAR NetCDF library applies the following heuristic rules (quoting from
     * {@link EnhanceScaleMissing}):
     *
     * <blockquote>
     * If {@code valid_range} is the same type as {@code scale_factor} (actually the wider of
     * {@code scale_factor} and {@code add_offset}) and this is wider than the external data,
     * then it will be interpreted as being in the units of the internal (unpacked) data.
     * Otherwise it is in the units of the external (packed) data.
     * </blockquote>
     *
     * @param variable The variable to extract metadata from.
     */
    public NetcdfVariable(VariableIF variable) {
        /*
         * If the variable is enhanced, get the original variable. This is necessary in order
         * to get access to the low-level attributes like "scale_factor", which are hidden by
         * VariableEnhanced.
         */
        while (variable instanceof VariableEnhanced) {
            final VariableIF candidate = ((VariableEnhanced) variable).getOriginalVariable();
            if (candidate == null) {
                break;
            }
            variable = candidate;
        }
        final DataType dataType, scaleType, rangeType;
        /*
         * Gets the scale factors, if present. Also remember its type
         * for the heuristic rule to be applied later on the valid range.
         */
        imageType  = getRawDataType(variable);
        dataType   = widestType = variable.getDataType();
        units      = variable.getUnitsString();
        scale      = attribute(variable, SCALE_FACTOR);
        offset     = attribute(variable, ADD_OFFSET);
        scaleType  = widestType;
        widestType = dataType; // Reset before we scan the other attributes.
        /*
         * Gets minimum and maximum. If a "valid_range" attribute is present, it has precedence
         * over "valid_min" and "valid_max" as specified in the UCAR documentation.
         */
        double minimum = Double.NaN;
        double maximum = Double.NaN;
        Attribute attribute = variable.findAttributeIgnoreCase(VALID_RANGE);
        if (attribute != null) {
            widestType = widest(attribute.getDataType(), widestType);
            Number value = attribute.getNumericValue(0);
            if (value != null) {
                minimum = value.doubleValue();
            }
            value = attribute.getNumericValue(1);
            if (value != null) {
                maximum = value.doubleValue();
            }
        }
        if (Double.isNaN(minimum)) {
            minimum = attribute(variable, VALID_MIN);
        }
        if (Double.isNaN(maximum)) {
            maximum = attribute(variable, VALID_MAX);
        }
        rangeType  = widestType;
        widestType = dataType; // Reset before we scan the other attributes.
        /*
         * Heuristic rule defined in UCAR documentation (see EnhanceScaleMissing interface):
         * if the type of the range is equals to the type of the scale, and the type of the
         * data is not wider, then assume that the minimum and maximum are geophysics values.
         */
        if (rangeType.equals(scaleType) && rangeType.equals(widest(rangeType, dataType))) {
            final double offset = Double.isNaN(this.offset) ? 0 : this.offset;
            final double scale  = Double.isNaN(this.scale ) ? 1 : this.scale;
            minimum = (minimum - offset) / scale;
            maximum = (maximum - offset) / scale;
            if (!isFloatingPoint(rangeType)) {
                if (!Double.isNaN(minimum) && !Double.isInfinite(minimum)) {
                    minimum = Math.round(minimum);
                }
                if (!Double.isNaN(maximum) && !Double.isInfinite(maximum)) {
                    maximum = Math.round(maximum);
                }
            }
        }
        if (Double.isNaN(minimum)) minimum = Double.NEGATIVE_INFINITY;
        if (Double.isNaN(maximum)) maximum = Double.POSITIVE_INFINITY;
        this.minimum = minimum;
        this.maximum = maximum;
        /*
         * Gets fill and missing values. According UCAR documentation, they are
         * always in packed units. We keep them "as-is" (as opposed to UCAR who
         * converts them to geophysics units), in order to avoid rounding errors.
         * Note that we merge missing and fill values in a single array, without
         * duplicated values.
         */
        widestType = dataType;
        attribute = variable.findAttributeIgnoreCase(MISSING_VALUE);
        final double fillValue    = attribute(variable, FILL_VALUE);
        final int    fillCount    = Double.isNaN(fillValue) ? 0 : 1;
        final int    missingCount = (attribute != null) ? attribute.getLength() : 0;
        final double[] missings   = new double[fillCount + missingCount];
        if (fillCount != 0) {
            missings[0] = fillValue;
        }
        int count = fillCount;
scan:   for (int i=0; i<missingCount; i++) {
            final Number number = attribute.getNumericValue(i);
            if (number != null) {
                final double value = number.doubleValue();
                if (!Double.isNaN(value)) {
                    for (int j=0; j<count; j++) {
                        if (value == missings[j]) {
                            // Current value duplicates a previous one.
                            continue scan;
                        }
                    }
                    missings[count++] = value;
                }
            }
        }
        fillValues = (count != 0) ? XArrays.resize(missings, count) : null;
    }

    /**
     * Returns {@code true} if at least one {@linkplain #fillValues fill value} is included
     * in the range of value values. In such case, we will consider the range of values as
     * invalid (the NetCDF library seems to set the range to maximal floating point values
     * when the range is actually not specified).
     *
     * @since 3.14
     */
    final boolean hasCollisions(final EnhanceScaleMissing variable) {
        if (fillValues != null) {
            for (final double fillValue : fillValues) {
                if (!variable.isInvalidData(fillValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the attribute value as a {@code double}. As a side effect, this method
     * updates the {@link #widestType} field if the given attribute has a wider side
     * than any previous attribute.
     *
     * @param  variable The variable from which to extract the attribute value.
     * @param  name The name (case-insensitive) of the attribute to fetch.
     * @return The attribute value, or {@code NaN} if none.
     */
    private double attribute(final VariableSimpleIF variable, final String name) {
        final Attribute attribute = variable.findAttributeIgnoreCase(name);
        if (attribute != null) {
            widestType = widest(attribute.getDataType(), widestType);
            final Number value = attribute.getNumericValue();
            if (value != null) {
                return value.doubleValue();
            }
        }
        return Double.NaN;
    }

    /**
     * Returns the widest of two data types.
     */
    private static DataType widest(final DataType type1, final DataType type2) {
        if (type1 == null) return type2;
        if (type2 == null) return type1;
        final int size1 = type1.getSize();
        final int size2 = type2.getSize();
        if (size1 > size2) return type1;
        if (size1 < size2) return type2;
        return isFloatingPoint(type2) ? type2 : type1;
    }

    /**
     * Returns {@code true} if the specified type is a floating point type.
     */
    private static boolean isFloatingPoint(final DataType type) {
        return DataType.FLOAT.equals(type) || DataType.DOUBLE.equals(type);
    }

    /**
     * Returns the data type which most closely represents the "raw" internal data
     * of the variable. This is the value returned by the default implementation of
     * {@link NetcdfImageReader#getRawDataType}.
     *
     * @param  variable The variable.
     * @return The data type, or {@link DataBuffer#TYPE_UNDEFINED} if unknown.
     *
     * @see NetcdfImageReader#getRawDataType
     */
    static int getRawDataType(final VariableIF variable) {
        final DataType type = variable.getDataType();
        if (type != null) switch (type) {
            case BOOLEAN: // Fall through
            case BYTE:    return DataBuffer.TYPE_BYTE;
            case CHAR:    return DataBuffer.TYPE_USHORT;
            case SHORT:   return variable.isUnsigned() ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_SHORT;
            case INT:     return DataBuffer.TYPE_INT;
            case FLOAT:   return DataBuffer.TYPE_FLOAT;
            case LONG:    // Fall through
            case DOUBLE:  return DataBuffer.TYPE_DOUBLE;
        }
        return DataBuffer.TYPE_UNDEFINED;
    }
}
