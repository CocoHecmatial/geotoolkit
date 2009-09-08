/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.metadata;

import java.util.Map;
import java.util.Set;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;


/**
 * The base class of {@link Map} views.
 *
 * @param <V> The type of values in the map.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.04
 *
 * @since 3.04
 * @module
 */
abstract class MetadataMap<V> extends AbstractMap<String,V> {
    /**
     * The accessor to use for the metadata.
     */
    final PropertyAccessor accessor;

    /**
     * Determines the string representation of keys in the map.
     */
    final KeyNamePolicy keyNames;

    /**
     * A view of the mappings contained in this map.
     */
    private transient Set<Map.Entry<String,V>> entrySet;

    /**
     * Creates a new map backed by the given accessor.
     */
    MetadataMap(final PropertyAccessor accessor, final KeyNamePolicy keyNames) {
        this.accessor = accessor;
        this.keyNames = keyNames;
    }

    /**
     * Returns the number of elements in this map.
     */
    @Override
    public abstract int size();

    /**
     * Returns a view of the mappings contained in this map.
     */
    @Override
    public final synchronized Set<Map.Entry<String,V>> entrySet() {
        if (entrySet == null) {
            entrySet = entries();
        }
        return entrySet;
    }

    /**
     * Creates the entry set.
     */
    abstract Set<Map.Entry<String,V>> entries();

    /**
     * Returns an iterator over the entries in this map.
     */
    abstract Iterator<Map.Entry<String,V>> iterator();




    /**
     * The iterator over the elements contained in a {@link Entries} set.
     *
     * @author Martin Desruisseaux (Geomatys)
     * @version 3.04
     *
     * @since 3.04
     */
    abstract class Iter implements Iterator<Map.Entry<String,V>> {
        /**
         * Creates a new iterator.
         */
        Iter() {
        }

        /**
         * Assumes that the underlying map is unmodifiable.
         * Only {@link PropertyMap} supports this method.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }




    /**
     * Base class of views of the entries contained in the map.
     *
     * @author Martin Desruisseaux (Geomatys)
     * @version 3.04
     *
     * @since 3.04
     */
    class Entries extends AbstractSet<Map.Entry<String,V>> {
        /**
         * Creates a new entries set.
         */
        Entries() {
        }

        /**
         * Returns true if this collection contains no elements.
         */
        @Override
        public final boolean isEmpty() {
            return MetadataMap.this.isEmpty();
        }

        /**
         * Returns the number of elements in this collection.
         */
        @Override
        public final int size() {
            return MetadataMap.this.size();
        }

        /**
         * Returns an iterator over the elements contained in this collection.
         */
        @Override
        public final Iterator<Map.Entry<String,V>> iterator() {
            return MetadataMap.this.iterator();
        }
    }
}
