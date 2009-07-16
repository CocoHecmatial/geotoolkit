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
package org.geotoolkit.util.converter;


/**
 * Holds explicit {@link #sourceClass} and {@link #targetClass} values. Used as key in a hash
 * map of converters. Also used as the base class for subclasses working on explicit source and
 * target class. We allows this opportunist leveraging of implementation because those classes
 * are not public (otherwise a separated hierarchy may have been preferable).
 *
 * @param <S> The base type of source objects.
 * @param <T> The base type of converted objects.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.00
 *
 * @since 3.00
 * @module
 */
class ClassPair<S,T> {
    /**
     * The source class.
     */
    protected final Class<? super S> sourceClass;

    /**
     * The target class.
     */
    protected final Class<? extends T> targetClass;

    /**
     * Creates an entry for the given converter.
     *
     * @param converter The converter.
     */
    ClassPair(final ObjectConverter<? super S, ? extends T> converter) {
        this(converter.getSourceClass(), converter.getTargetClass());
    }

    /**
     * Creates an entry for the given source and target classes.
     *
     * @param source The {@link ObjectConverter#getSourceClass source class}.
     * @param target The {@link ObjectConverter#getTargetClass target class}.
     */
    ClassPair(final Class<? super S> source, final Class<? extends T> target) {
        sourceClass = source;
        targetClass = target;
    }

    /**
     * Returns a key for the parent source, or {@code null} if none.
     *
     * @return A key for the parent source, or {@code null}.
     */
    public final ClassPair<? super S,T> parentSource() {
        final Class<? super S> source = sourceClass.getSuperclass();
        return (source != null) ? new ClassPair<S,T>(source, targetClass) : null;
    }

    /**
     * Casts the given converter to the source and target classes of this {@code ClassPair}. This
     * method is not public because the checks are performed using assertions only. If this method
     * was to goes public, the assertions would need to be replaced by inconditional checks.
     * <p>
     * This method is used by {@link ConverterRegistry} after fetching a value from a hash
     * map using this {@code ClassPair} as a key. In this context, the cast should never fail
     * (assuming that the converters do not change their source and target classes).
     */
    @SuppressWarnings("unchecked")
    final ObjectConverter<S,T> cast(final ObjectConverter<?,?> converter) {
        assert converter.getSourceClass().isAssignableFrom(sourceClass) : sourceClass;
        assert targetClass.isAssignableFrom(converter.getTargetClass()) : targetClass;
        return (ObjectConverter<S,T>) converter;
    }

    /**
     * Returns {@code true} if the given converter is defining the conversion for this key. A
     * converter is "defining" if its source and target classes are exactly the ones declared
     * for this key.
     *
     * @param  The converter to check.
     * @return {@code true} if the given converter is defining the conversion.
     */
    final boolean isDefining(final ObjectConverter<?,?> converter) {
        if (converter instanceof FallbackConverter<?,?>) {
            final FallbackConverter<?,?> fc = (FallbackConverter<?,?>) converter;
            final ObjectConverter<?,?> primary, fallback;
            synchronized (fc) {
                primary  = fc.converter(true);
                fallback = fc.converter(false);
            }
            return isDefining(primary) || isDefining(fallback);
        }
        return sourceClass.equals(converter.getSourceClass()) &&
               targetClass.equals(converter.getTargetClass());
    }

    /**
     * Compares the given object with this entry for equality. Two entries are considered
     * equals if they have the same source and target classes. This is required for use
     * as {@link java.util.HashMap} keys in {@link ConverterRegistry}.
     *
     * @param  other The object to compare with this entry.
     * @return {@code true} if the given object is a entry having the same source and target classes.
     */
    @Override
    public final boolean equals(final Object other) {
        if (other instanceof ClassPair<?,?>) {
            final ClassPair<?,?> that = (ClassPair<?,?>) other;
            return sourceClass.equals(that.sourceClass) &&
                   targetClass.equals(that.targetClass);
        }
        return false;
    }

    /**
     * Returns a hash code value for this entry.
     */
    @Override
    public final int hashCode() {
        return sourceClass.hashCode() + 31*targetClass.hashCode();
    }

    /**
     * Returns a string representation for this entry.
     * Used for formatting error messages.
     */
    @Override
    public String toString() {
        return sourceClass.getSimpleName() + "\u00A0\u21E8\u00A0" + targetClass.getSimpleName();
    }
}
