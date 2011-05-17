/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2011, Geomatys
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
package org.geotoolkit.internal.jaxb;

import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractSet;
import java.util.AbstractMap;
import java.util.NoSuchElementException;
import java.io.Serializable;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;

import org.geotoolkit.util.Utilities;
import org.geotoolkit.util.collection.XCollections;


/**
 * A map of identifiers which can be used as a helper class for
 * {@link org.geotoolkit.xml.IdentifiedObject} implementations.
 *
 * This class work as a wrapper around a collection of identifiers. Because all operations
 * are performed by an iteration over the collection elements, this implementation is
 * suitable only for small maps (less that 10 elements). Given that objects typically
 * have only one or two identifiers, this is considered acceptable.
 * <p>
 * The collection of identifiers shall not contains any null element. However, in order
 * to make the code more robust, any null element are skipped. Note however that it may
 * cause some inconsistency, for example {@link #isEmpty()} could returns {@code false}
 * while the more accurate {@link #size()} method returns 0.
 *
 * {@section Thread safety}
 * This class is thread safe if the underlying identifiers collection is thread safe.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.18
 *
 * @see org.geotoolkit.xml.IdentifiedObject
 *
 * @since 3.18
 * @module
 */
public final class IdentifierMap extends AbstractMap<Citation,String> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2661044384787218964L;

    /**
     * The identifiers to wrap in a map view.
     */
    private final Collection<Identifier> identifiers;

    /**
     * A view over the entries, created only when first needed.
     */
    private transient Set<Entry<Citation,String>> entries;

    /**
     * Creates a new map which will be a view over the given identifiers.
     *
     * @param identifiers The identifiers to wrap in a map view.
     */
    public IdentifierMap(final Collection<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * Removes every entries in the underlying collection.
     *
     * @throws UnsupportedOperationException If the collection of identifiers is unmodifiable.
     */
    @Override
    public void clear() throws UnsupportedOperationException {
        identifiers.clear();
    }

    /**
     * Returns {@code true} if the collection of identifiers contains at least one element.
     * This method does not verify if the collection contains null element (it should not).
     * Consequently, this method may return {@code false} even if the {@link #size()} method
     * returns 0.
     */
    @Override
    public boolean isEmpty() {
        return identifiers.isEmpty();
    }

    /**
     * Returns {@code true} if at least one identifier declares the given
     * {@linkplain Identifier#getCode() code}.
     *
     * @param  code The code to search, which should be an instance of {@link String}.
     * @return {@code true} if at least one identifier uses the given code.
     */
    @Override
    public boolean containsValue(final Object code) {
        if (code instanceof String) {
            for (final Identifier identifier : identifiers) {
                if (identifier != null && Utilities.equals(code, identifier.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if at least one identifier declares the given
     * {@linkplain Identifier#getAuthority() authority}.
     *
     * @param  authority The authority to search, which should be an instance of {@link Citation}.
     * @return {@code true} if at least one identifier uses the given authority.
     */
    @Override
    public boolean containsKey(final Object authority) {
        return get(authority, false) != null;
    }

    /**
     * Returns the identifier for the given key, or {@code null} if none. The
     * identifier found (if any) is removed if {@code remove} is {@code true}.
     */
    private Identifier get(final Object key, final boolean remove) {
        if (key instanceof Citation) {
            final Iterator<? extends Identifier> it = identifiers.iterator();
            while (it.hasNext()) {
                final Identifier identifier = it.next();
                if (identifier != null && Utilities.equals(key, identifier.getAuthority())) {
                    if (remove) {
                        it.remove();
                    }
                    return identifier;
                }
            }
        }
        return null;
    }

    /**
     * Returns the code of the first identifier associated with the given
     * {@linkplain Identifier#getAuthority() authority}, or {@code null} if no identifier was found.
     *
     * @param  authority The authority to search, which should be an instance of {@link Citation}.
     * @return The code of the identifier for the given authority, or {@code null} if none.
     */
    @Override
    public String get(final Object authority) {
        final Identifier identifier = get(authority, false);
        return (identifier != null) ? identifier.getCode() : null;
    }

    /**
     * Removes the first identifier associated with the given {@linkplain Identifier#getAuthority()
     * authority}, if one is found.
     *
     * @param  authority The authority to search, which should be an instance of {@link Citation}.
     * @return The code of the identifier for the given authority, or {@code null} if none.
     */
    @Override
    public String remove(final Object authority) {
        final Identifier identifier = get(authority, true);
        return (identifier != null) ? identifier.getCode() : null;
    }

    /**
     * Set the code of the identifier having the given authority to the given value.
     * If no identifier is found for the given authority, a new one is created.
     *
     * @param  authority The authority for which to set the code.
     * @param  code The new code for the given authority.
     * @return The previous code for the given authority, or {@code null} if none.
     */
    @Override
    public String put(final Citation authority, final String code) {
        String old = null;
        final Iterator<? extends Identifier> it = identifiers.iterator();
        while (it.hasNext()) {
            final Identifier identifier = it.next();
            if (identifier == null) {
                it.remove(); // Opportunist cleaning, but should not happen.
            } else if (Utilities.equals(authority, identifier.getAuthority())) {
                if (identifier instanceof IdentifierEntry) {
                    return ((IdentifierEntry) identifier).setValue(code);
                }
                if (old == null) {
                    old = identifier.getCode();
                }
                it.remove();
            }
        }
        identifiers.add(new IdentifierEntry(authority, code));
        return old;
    }

    /**
     * Returns a view over the collection of identifiers. This view supports removal operation
     * if the underlying collection of identifiers supports the {@link Iterator#remove()} method.
     *
     * @return A view over the collection of identifiers.
     */
    @Override
    public synchronized Set<Entry<Citation,String>> entrySet() {
        if (entries == null) {
            entries = new Entries(identifiers);
        }
        return entries;
    }

    /**
     * The view returned by {@link IdentifierMap#entrySet()}. If the backing identifier
     * collection contains null entries, those entries will be ignored. If the backing
     * collection contains many entries for the same authority, then only the first
     * occurrence is retained.
     *
     * @author Martin Desruisseaux (Geomatys)
     * @version 3.18
     *
     * @since 3.18
     * @module
     */
    private static final class Entries extends AbstractSet<Entry<Citation,String>> {
        /**
         * The identifiers to wrap in a set of entries view. This is a reference
         * to the same collection than {@link IdentifierMap#identifiers}.
         */
        private final Collection<? extends Identifier> identifiers;

        /**
         * Creates a new view over the collection of identifiers.
         *
         * @param identifiers The identifiers to wrap in a set of entries view.
         */
        Entries(final Collection<? extends Identifier> identifiers) {
            this.identifiers = identifiers;
        }

        /**
         * Same implementation than {@link IdentifierMap#clear()}.
         */
        @Override
        public void clear() throws UnsupportedOperationException {
            identifiers.clear();
        }

        /**
         * Same implementation than {@link IdentifierMap#isEmpty()}.
         */
        @Override
        public boolean isEmpty() {
            return identifiers.isEmpty();
        }

        /**
         * Counts the number of entries, ignoring null elements and duplicated authorities.
         * Because {@code null} elements are ignored, this method may return 0 even if
         * {@link #isEmpty()} returns {@code false}.
         */
        @Override
        public int size() {
            final HashMap<Citation,Boolean> done = new HashMap<Citation,Boolean>();
            for (final Identifier identifier : identifiers) {
                if (identifier != null) {
                    done.put(identifier.getAuthority(), null);
                }
            }
            return done.size();
        }

        /**
         * Returns an iterator over the (<var>citation</var>, <var>code</var>) entries.
         */
        @Override
        public Iterator<Entry<Citation, String>> iterator() {
            return new Iter(identifiers);
        }
    }

    /**
     * The iterator over the (<var>citation</var>, <var>code</var>) entries. This iterator is
     * created by the {@link IdentifierMap.Entries} collection. It extends {@link HashMap} as
     * an opportunist implementation strategy, but users don't need to know this detail.
     * <p>
     * This iterator supports the {@link #remove()} operation if the underlying collection
     * supports it.
     *
     * @author Martin Desruisseaux (Geomatys)
     * @version 3.18
     *
     * @since 3.18
     * @module
     */
    @SuppressWarnings("serial") // Not intended to be serialized.
    private static final class Iter extends HashMap<Citation,Boolean> implements Iterator<Entry<Citation,String>> {
        /**
         * An iterator over the {@link IdentifierMap#identifiers} collection,
         * or (@code null} if we have reached the iteration end.
         */
        private Iterator<? extends Identifier> identifiers;

        /**
         * The next entry to be returned by {@link #next()}, or {@code null} if not yet computed.
         * This field will be computed only when {@link #next()} or {@link #hasNext()} is invoked.
         */
        private transient IdentifierEntry next;

        /**
         * Creates a new iterator for the given collection of identifiers.
         */
        Iter(final Collection<? extends Identifier> identifiers) {
            super(XCollections.hashMapCapacity(identifiers.size()));
            this.identifiers = identifiers.iterator();
        }

        /**
         * Advance to the next non-null identifier, wraps it in an entry and stores the
         * result in the {@link #next} field. If this we reach the iteration end, then
         * this method set the {@link #identifiers} iterator to {@code null}.
         */
        private void toNext() {
            final Iterator<? extends Identifier> it = identifiers;
            if (it != null) {
                while (it.hasNext()) {
                    final Identifier identifier = it.next();
                    if (identifier != null) {
                        final Citation authority = identifier.getAuthority();
                        if (put(authority, Boolean.TRUE) == null) {
                            if (identifier instanceof IdentifierEntry) {
                                next = (IdentifierEntry) identifier;
                            } else {
                                next = new IdentifierEntry(authority, identifier.getCode());
                            }
                            return;
                        }
                    }
                }
                identifiers = null;
            }
        }

        /**
         * If we need to search for the next element, fetches it now.
         * Then returns {@code true} if we didn't reached the iteration end.
         */
        @Override
        public boolean hasNext() {
            if (next == null) {
                toNext();
            }
            return identifiers != null;
        }

        /**
         * If we need to search for the next element, searches it now. Then set {@link #next}
         * to {@code null} as a flag meaning that next invocations will need to search again
         * for an element, and returns the element that we got.
         */
        @Override
        public Entry<Citation,String> next() throws NoSuchElementException {
            IdentifierEntry entry = next;
            if (entry == null) {
                toNext();
                entry = next;
            }
            next = null;
            if (identifiers == null) {
                throw new NoSuchElementException();
            }
            return entry;
        }

        /**
         * Removes the last element returned by {@link #next()}. Note that if the {@link #next}
         * field is non-null, that would mean that the iteration has moved since the last call
         * to the {@link #next()} method, in which case the iterator is invalid.
         */
        @Override
        public void remove() throws IllegalStateException {
            final Iterator<? extends Identifier> it = identifiers;
            if (it == null || next != null) {
                throw new IllegalStateException();
            }
            it.remove();
        }
    }
}