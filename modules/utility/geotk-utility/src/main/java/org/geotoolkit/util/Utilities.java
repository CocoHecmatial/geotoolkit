/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2001-2012, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2012, Geomatys
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
package org.geotoolkit.util;

import java.util.Arrays;
import java.util.Objects;
import java.io.Serializable;
import org.apache.sis.util.ComparisonMode;

import org.geotoolkit.lang.Static;


/**
 * Miscellaneous methods.
 * <p>
 * This class also provides convenience methods for computing {@linkplain Object#hashCode hash code}
 * values. All those methods expect a {@code seed} argument, which is the hash code value computed
 * for previous fields in a class. For the initial seed (the one for the field for which to compute
 * an hash code), an arbitrary value must be provided. We suggest a different number for different
 * class in order to reduce the risk of collision between "empty" instances of different classes.
 * {@linkplain java.io.Serializable} classes can use {@code (int) serialVersionUID} for example.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.20
 *
 * @since 1.2
 * @module
 */
public final class Utilities extends Static {
    /**
     * A prime number used for hash code computation. Value 31 is often used because
     * some modern compilers can optimize {@code x*31} as {@code (x << 5) - x}
     * (Josh Bloch, <cite>Effective Java</cite>).
     */
    private static final int PRIME_NUMBER = 31;

    /**
     * Forbid object creation.
     */
    private Utilities() {
    }

    /**
     * Returns {@code true} if the given floats are equals. Positive and negative zero are
     * considered different, while a NaN value is considered equal to all other NaN values.
     *
     * @param o1 The first value to compare.
     * @param o2 The second value to compare.
     * @return {@code true} if both values are equal.
     *
     * @see Float#equals(Object)
     */
    public static boolean equals(final float o1, final float o2) {
        return Float.floatToIntBits(o1) == Float.floatToIntBits(o2);
    }

    /**
     * Returns {@code true} if the given doubles are equals. Positive and negative zero are
     * considered different, while a NaN value is considered equal to all other NaN values.
     *
     * @param o1 The first value to compare.
     * @param o2 The second value to compare.
     * @return {@code true} if both values are equal.
     *
     * @see Double#equals(Object)
     */
    public static boolean equals(final double o1, final double o2) {
        return Double.doubleToLongBits(o1) == Double.doubleToLongBits(o2);
    }

    /**
     * Convenience method for testing two objects for equality using the given level of strictness.
     * If at least one of the given objects implement the {@link LenientComparable} interface, then
     * the comparison is performed using the {@link LenientComparable#equals(Object, ComparisonMode)}
     * method. Otherwise this method performs the same work than the
     * {@link Objects#deepEquals(Object, Object)} convenience method.
     * <p>
     * If both arguments are arrays or collections, then the elements are compared recursively.
     *
     * @param object1 The first object to compare, or {@code null}.
     * @param object2 The second object to compare, or {@code null}.
     * @param mode The strictness level of the comparison.
     * @return {@code true} if both objects are equal for the given level of strictness.
     *
     * @see org.geotoolkit.referencing.CRS#equalsIgnoreMetadata(Object, Object)
     *
     * @since 3.18
     *
     * @deprecated Moved to Apache SIS {@link org.apache.sis.util.Utilities}.
     */
    @Deprecated
    public static boolean deepEquals(final Object object1, final Object object2, final ComparisonMode mode) {
        return org.apache.sis.util.Utilities.deepEquals(object1, object2, mode);
    }

    /**
     * Alters the given seed with the hash code value computed from the given value.
     *
     * @param  value The value whose hash code to compute.
     * @param  seed  The hash code value computed so far. If this method is invoked for the first
     *               field, then any arbitrary value (preferably different for each class) is okay.
     * @return An updated hash code value.
     */
    public static int hash(final boolean value, final int seed) {
        // Use the same values than Boolean.hashCode()
        return seed * PRIME_NUMBER + (value ? 1231 : 1237);
    }

    /**
     * Alters the given seed with the hash code value computed from the given value.
     *
     * @param  value The value whose hash code to compute.
     * @param  seed  The hash code value computed so far. If this method is invoked for the first
     *               field, then any arbitrary value (preferably different for each class) is okay.
     * @return An updated hash code value.
     */
    public static int hash(final char value, final int seed) {
        return seed * PRIME_NUMBER + (int) value;
    }

    /**
     * Alters the given seed with the hash code value computed from the given value.
     * {@code byte} and {@code short} primitive types are handled by this method as
     * well through implicit widening conversion.
     *
     * @param  value The value whose hash code to compute.
     * @param  seed  The hash code value computed so far. If this method is invoked for the first
     *               field, then any arbitrary value (preferably different for each class) is okay.
     * @return An updated hash code value.
     */
    public static int hash(final int value, final int seed) {
        return seed * PRIME_NUMBER + value;
    }

    /**
     * Alters the given seed with the hash code value computed from the given value.
     * {@code byte} and {@code short} primitive types are handled by this method as
     * well through implicit widening conversion.
     *
     * @param  value The value whose hash code to compute.
     * @param  seed  The hash code value computed so far. If this method is invoked for the first
     *               field, then any arbitrary value (preferably different for each class) is okay.
     * @return An updated hash code value.
     */
    public static int hash(final long value, final int seed) {
        return seed * PRIME_NUMBER + (((int) value) ^ ((int) (value >>> 32)));
    }

    /**
     * Alters the given seed with the hash code value computed from the given value.
     *
     * @param  value The value whose hash code to compute.
     * @param  seed  The hash code value computed so far. If this method is invoked for the first
     *               field, then any arbitrary value (preferably different for each class) is okay.
     * @return An updated hash code value.
     */
    public static int hash(final float value, final int seed) {
        return seed * PRIME_NUMBER + Float.floatToIntBits(value);
    }

    /**
     * Alters the given seed with the hash code value computed from the given value.
     *
     * @param  value The value whose hash code to compute.
     * @param  seed  The hash code value computed so far. If this method is invoked for the first
     *               field, then any arbitrary value (preferably different for each class) is okay.
     * @return An updated hash code value.
     */
    public static int hash(final double value, final int seed) {
        return hash(Double.doubleToLongBits(value), seed);
    }

    /**
     * Alters the given seed with the hash code value computed from the given value. The given
     * object may be null. This method do <strong>not</strong> iterates recursively in array
     * elements. If array needs to be hashed, use one of {@link Arrays} method or
     * {@link #deepHashCode deepHashCode} instead.
     * <p>
     * <b>Note on assertions:</b> There is no way to ensure at compile time that this method
     * is not invoked with an array argument, while doing so would usually be a program error.
     * Performing a systematic argument check would impose a useless overhead for correctly
     * implemented {@link Object#hashCode} methods. As a compromise we perform this check at
     * runtime only if assertions are enabled. Using assertions for argument check in a public
     * API is usually a deprecated practice, but we make an exception for this particular method.
     *
     * @param  value The value whose hash code to compute, or {@code null}.
     * @param  seed  The hash code value computed so far. If this method is invoked for the first
     *               field, then any arbitrary value (preferably different for each class) is okay.
     * @return An updated hash code value.
     * @throws AssertionError If assertions are enabled and the given value is an array.
     */
    public static int hash(final Object value, int seed) throws AssertionError {
        seed *= PRIME_NUMBER;
        if (value != null) {
            assert !value.getClass().isArray() : name(value);
            seed += value.hashCode();
        }
        return seed;
    }

    /**
     * Returns a hash code for the specified object, which may be an array.
     * This method returns one of the following values:
     * <p>
     * <ul>
     *   <li>If the supplied object is {@code null}, then this method returns 0.</li>
     *   <li>Otherwise if the object is an array of objects, then
     *       {@link Arrays#deepHashCode(Object[])} is invoked.</li>
     *   <li>Otherwise if the object is an array of primitive type, then the corresponding
     *       {@link Arrays#hashCode(double[]) Arrays.hashCode(...)} method is invoked.</li>
     *   <li>Otherwise {@link Object#hashCode()} is invoked.<li>
     * </ul>
     * <p>
     * This method should be invoked <strong>only</strong> if the object type is declared
     * exactly as {@code Object}, not as some subtype like {@code Object[]}, {@code String} or
     * {@code float[]}. In the later cases, use the appropriate {@link Arrays} method instead.
     *
     * @param object The object to compute hash code. May be {@code null}.
     * @return The hash code of the given object.
     *
     * @deprecated Moved to Apache SIS {@link org.apache.sis.util.Utilities}.
     */
    @Deprecated
    public static int deepHashCode(final Object object) {
        if (object == null) {
            return 0;
        }
        if (object instanceof Object[]) {
            return Arrays.deepHashCode((Object[]) object);
        }
        if (object instanceof double[]) {
            return Arrays.hashCode((double[]) object);
        }
        if (object instanceof float[]) {
            return Arrays.hashCode((float[]) object);
        }
        if (object instanceof long[]) {
            return Arrays.hashCode((long[]) object);
        }
        if (object instanceof int[]) {
            return Arrays.hashCode((int[]) object);
        }
        if (object instanceof short[]) {
            return Arrays.hashCode((short[]) object);
        }
        if (object instanceof byte[]) {
            return Arrays.hashCode((byte[]) object);
        }
        if (object instanceof char[]) {
            return Arrays.hashCode((char[]) object);
        }
        if (object instanceof boolean[]) {
            return Arrays.hashCode((boolean[]) object);
        }
        return object.hashCode();
    }

    /**
     * Returns a string representation of the specified object, which may be an array.
     * This method returns one of the following values:
     * <p>
     * <ul>
     *   <li>If the object is an array of objects, then
     *       {@link Arrays#deepToString(Object[])} is invoked.</li>
     *   <li>Otherwise if the object is an array of primitive type, then the corresponding
     *       {@link Arrays#toString(double[]) Arrays.toString(...)} method is invoked.</li>
     *   <li>Otherwise {@link String#valueOf(Object)} is invoked.</li>
     * </ul>
     * <p>
     * This method should be invoked <strong>only</strong> if the object type is declared
     * exactly as {@code Object}, not as some subtype like {@code Object[]}, {@code Number} or
     * {@code float[]}. In the later cases, use the appropriate {@link Arrays} method instead.
     *
     * @param object The object to format as a string. May be {@code null}.
     * @return A string representation of the given object.
     *
     * @deprecated Moved to Apache SIS {@link org.apache.sis.util.Utilities}.
     */
    @Deprecated
    public static String deepToString(final Object object) {
        if (object instanceof Object[]) {
            return Arrays.deepToString((Object[]) object);
        }
        if (object instanceof double[]) {
            return Arrays.toString((double[]) object);
        }
        if (object instanceof float[]) {
            return Arrays.toString((float[]) object);
        }
        if (object instanceof long[]) {
            return Arrays.toString((long[]) object);
        }
        if (object instanceof int[]) {
            return Arrays.toString((int[]) object);
        }
        if (object instanceof short[]) {
            return Arrays.toString((short[]) object);
        }
        if (object instanceof byte[]) {
            return Arrays.toString((byte[]) object);
        }
        if (object instanceof char[]) {
            return Arrays.toString((char[]) object);
        }
        if (object instanceof boolean[]) {
            return Arrays.toString((boolean[]) object);
        }
        return String.valueOf(object);
    }

    /**
     * Returns the class name of the given object.
     * Used in assertions only.
     */
    private static String name(final Object object) {
        return object.getClass().getSimpleName();
    }
}
