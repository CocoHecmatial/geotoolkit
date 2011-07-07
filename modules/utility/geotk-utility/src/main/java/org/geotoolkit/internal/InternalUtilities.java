/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2011, Geomatys
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
package org.geotoolkit.internal;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.text.NumberFormat;
import java.text.DecimalFormat;

import org.geotoolkit.lang.Static;
import org.geotoolkit.util.Utilities;
import org.geotoolkit.util.ComparisonMode;
import org.geotoolkit.util.converter.Classes;
import org.geotoolkit.util.collection.XCollections;
import org.geotoolkit.resources.Errors;

import static java.lang.Math.*;


/**
 * Various utility methods not to be put in public API.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.19
 *
 * @since 3.18 (derived from 3.00)
 * @module
 */
public final class InternalUtilities extends Static {
    /**
     * Relative difference tolerated when comparing floating point numbers using
     * {@link ComparisonMode#APPROXIMATIVE}.
     * <p>
     * Historically, this was the relative tolerance threshold for considering two matrixes
     * as {@linkplain org.geotoolkit.referencing.operation.matrix.XMatrix#equals(Object,
     * ComparisonMode) equal}. This value has been determined empirically in order to allow
     * {@link org.geotoolkit.referencing.operation.transform.ConcatenatedTransform} to detect the
     * cases where two {@link org.geotoolkit.referencing.operation.transform.LinearTransform}
     * are equal for practical purpose. This threshold can be used as below:
     *
     * {@preformat java
     *     Matrix m1 = ...;
     *     Matrix m2 = ...;
     *     if (MatrixUtilities.epsilonEqual(m1, m2, EQUIVALENT_THRESHOLD, true)) {
     *         // Consider that matrixes are equal.
     *     }
     * }
     *
     * By extension, the same threshold value is used for comparing other floating point values.
     *
     * @since 3.18
     */
    public static final double COMPARISON_THRESHOLD = 1E-14;

    /**
     * Do not allow instantiation of this class.
     */
    private InternalUtilities() {
    }

    /**
     * Returns an identity string for the given value. This method returns a string similar to
     * the one returned by the default implementation of {@link Object#toString()}, except that
     * a simple class name (without package name) is used instead than the fully-qualified name.
     *
     * @param  value The object for which to get the identity string, or {@code null}.
     * @return The identity string for the given object.
     *
     * @since 3.17
     */
    public static String identity(final Object value) {
        return Classes.getShortClassName(value) + '@' + Integer.toHexString(System.identityHashCode(value));
    }

    /**
     * Returns {@code true} if the given values are approximatively equal given the
     * comparison mode.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @param  mode The comparison mode to use for comparing the numbers.
     * @return {@code true} If both values are approximatively equal.
     *
     * @since 3.18
     */
    public static boolean epsilonEqual(final double v1, final double v2, final ComparisonMode mode) {
        return (mode == ComparisonMode.APPROXIMATIVE) ? epsilonEqual(v1, v2) : Utilities.equals(v1, v2);
    }

    /**
     * Returns {@code true} if the given values are approximatively equal, up to the
     * {@linkplain #COMPARISON_THRESHOLD comparison threshold}.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @return {@code true} If both values are approximatively equal.
     *
     * @since 3.18
     */
    public static boolean epsilonEqual(final double v1, final double v2) {
        final double threshold = COMPARISON_THRESHOLD * max(abs(v1), abs(v2));
        if (threshold == Double.POSITIVE_INFINITY || Double.isNaN(threshold)) {
            return Double.doubleToLongBits(v1) == Double.doubleToLongBits(v2);
        }
        return abs(v1 - v2) <= threshold;
    }

    /**
     * Returns {@code true} if the following objects are floating point numbers ({@link Float} or
     * {@link Double} types) and approximatively equal. If the given object are not floating point
     * numbers, then this method returns {@code false} unconditionally on the assumption that
     * strict equality has already been checked before this method call.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @return {@code true} If both values are real number and approximatively equal.
     *
     * @since 3.18
     */
    public static boolean floatEpsilonEqual(final Object v1, final Object v2) {
        return (v1 instanceof Float || v1 instanceof Double) &&
               (v2 instanceof Float || v2 instanceof Double) &&
               epsilonEqual(((Number) v1).doubleValue(), ((Number) v2).doubleValue());
    }

    /**
     * Returns a copy of the given array as a non-empty immutable set.
     * If the given array is empty, then this method returns {@code null}.
     * <p>
     * This method is not public provided in the public API because the recommended
     * practice is usually to return an empty collection rather than {@code null}.
     *
     * @param  <T> The type of elements.
     * @param  elements The elements to copy in a set.
     * @return An unmodifiable set which contains all the given elements.
     *
     * @since 3.17
     */
    public static <T> Set<T> nonEmptySet(final T... elements) {
        final Set<T> asSet = XCollections.immutableSet(elements);
        return (asSet != null && asSet.isEmpty()) ? null : asSet;
    }

    /**
     * Returns an unmodifiable map which contains a copy of the given map, only for the given keys.
     * The value for the given keys shall be of the given type. Other values can be of any types,
     * since they will be ignored.
     *
     * @param  <K>  The type of keys in the map.
     * @param  <V>  The type of values in the map.
     * @param  map  The map to copy, or {@code null}.
     * @param  valueType The base type of retained values.
     * @param  keys The keys of values to retain.
     * @return A copy of the given map containing only the given keys, or {@code null}
     *         if the given map was null.
     * @throws ClassCastException If at least one retained value is not of the expected type.
     *
     * @since 3.17
     */
    public static <K,V> Map<K,V> subset(final Map<?,?> map, final Class<V> valueType, final K... keys)
            throws ClassCastException
    {
        Map<K,V> copy = null;
        if (map != null) {
            copy = new HashMap<K,V>(XCollections.hashMapCapacity(Math.min(map.size(), keys.length)));
            for (final K key : keys) {
                final V value = valueType.cast(map.get(key));
                if (value != null) {
                    copy.put(key, value);
                }
            }
            copy = XCollections.unmodifiableMap(copy);
        }
        return copy;
    }

    /**
     * Returns the separator to use between numbers. Current implementation returns the coma
     * character, unless the given number already use the coma as the decimal separator.
     *
     * @param  format The format used for formatting numbers.
     * @return The character to use as a separator between numbers.
     *
     * @since 3.11
     */
    public static char getSeparator(final NumberFormat format) {
        if (format instanceof DecimalFormat) {
            final char c = ((DecimalFormat) format).getDecimalFormatSymbols().getDecimalSeparator();
            if (c == ',') {
                return ';';
            }
        }
        return ',';
    }

    /**
     * Gets the ARGB values for the given hexadecimal value. If the given code begins with the
     * {@code '#'} character, then this method accepts the following hexadecimal patterns:
     * <p>
     * <ul>
     *   <li>{@code "#AARRGGBB"}: an explicit ARGB code used verbatim.</li>
     *   <li>{@code "#RRGGBB"}: a fully opaque RGB color.</li>
     *   <li>{@code "#ARGB"}: an abbreviation for {@code "#AARRGGBB"}.</li>
     *   <li>{@code "#RGB"}: an abbreviation for {@code "#RRGGBB"}.
     *       For example #0BC means #00BBCC.</li>
     * </ul>
     *
     * @param  color The color code to parse.
     * @throws NumberFormatException If the given code can not be parsed.
     * @return The ARGB code.
     *
     * @see java.awt.Color#decode(String)
     *
     * @since 3.19
     */
    @SuppressWarnings("fallthrough")
    public static int parseColor(String color) throws NumberFormatException {
        color = color.trim();
        if (color.startsWith("#")) {
            color = color.substring(1);
            int value = Integer.parseInt(color, 16);
            switch (color.length()) {
                case 3: value |= 0xF000; // Fallthrough
                case 4: {
                    /*
                     * Color shortcut for hexadecimal value.
                     * Example: #0BC means #00BBCC.
                     */
                    int t;
                    value = (((t=(value & 0xF000)) | (t << 4)) << 24) |
                            (((t=(value & 0x0F00)) | (t << 4)) << 16) |
                            (((t=(value & 0x00F0)) | (t << 4)) <<  8) |
                            (((t=(value & 0x000F)) | (t << 4)));
                    break;
                }
                case 6: value |= 0xFF000000; // Fallthrough
                case 8: break;
                default: {
                    throw new NumberFormatException(Errors.format(
                            Errors.Keys.ILLEGAL_ARGUMENT_$2, "color", color));
                }
            }
            return value;
        }
        /*
         * Parses the string as an opaque color unless an alpha value was provided.
         * This matches closing the default java.awt.Color.decode(String) behavior,
         * which considers every colors as opaque. We relax slightly the condition
         * by allowing non-zero alpha values. The inconvenient is that specifying
         * a fully transparent color is not possible with syntax - please use the
         * above "#" syntax instead.
         */
        int value = Integer.decode(color);
        if ((value & 0xFF000000) == 0) {
            value |= 0xFF000000;
        }
        return value;
    }
}
