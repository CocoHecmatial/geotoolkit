/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010-2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2010-2011, Geomatys
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
package org.geotoolkit.util.collection;

import java.util.Iterator;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.LinkedHashMap;
import java.io.Serializable;

import org.geotoolkit.lang.Static;


/**
 * Static methods working on {@link Collection} objects. This is an extension to the
 * Java {@link Collections} utility class.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.18
 *
 * @since 3.10 (derived from 3.00)
 * @module
 */
public final class XCollections extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private XCollections() {
    }

    /**
     * Clears the given collection, if non-null. If the collection is null, then this method does
     * nothing. This is a convenience method when a null collection is a synonymous of empty.
     *
     * @param collection The collection to clear, or {@code null}.
     *
     * @since 3.18
     */
    public static void clear(final Collection<?> collection) {
        if (collection != null) {
            collection.clear();
        }
    }

    /**
     * Clears the given map, if non-null. If the map is null, then this method does nothing.
     * This is a convenience method when a null map is a synonymous of empty.
     *
     * @param map The map to clear, or {@code null}.
     *
     * @since 3.18
     */
    public static void clear(final Map<?,?> map) {
        if (map != null) {
            map.clear();
        }
    }

    /**
     * Returns {@code true} if the given collection is either null or
     * {@linkplain Collection#isEmpty() empty}. If this method returns {@code false},
     * then the given collection is guaranteed to be non-null and to contain at least
     * one element.
     *
     * @param collection The collection to test, or {@code null}.
     * @return {@code true} if the given collection is null or empty, or {@code false} otherwise.
     *
     * @since 3.18
     */
    public static boolean isNullOrEmpty(final Collection<?> collection) {
        return (collection == null) || collection.isEmpty();
    }

    /**
     * Returns {@code true} if the given map is either null or {@linkplain Map#isEmpty() empty}.
     * If this method returns {@code false}, then the given map is guaranteed to be non-null and
     * to contain at least one element.
     *
     * @param map The map to test, or {@code null}.
     * @return {@code true} if the given map is null or empty, or {@code false} otherwise.
     *
     * @since 3.18
     */
    public static boolean isNullOrEmpty(final Map<?,?> map) {
        return (map == null) || map.isEmpty();
    }

    /**
     * Returns a {@linkplain Queue queue} which is always empty and accepts no element.
     *
     * @param <E> The type of elements in the empty collection.
     * @return An empty collection.
     *
     * @see Collections#emptyList()
     * @see Collections#emptySet()
     */
    @SuppressWarnings({"unchecked","rawtype"})
    public static <E> Queue<E> emptyQueue() {
        return EmptyQueue.INSTANCE;
    }

    /**
     * Returns a {@linkplain SortedSet sorted set} which is always empty and accepts no element.
     *
     * @param <E> The type of elements in the empty collection.
     * @return An empty collection.
     *
     * @see Collections#emptyList()
     * @see Collections#emptySet()
     */
    @SuppressWarnings({"unchecked","rawtype"})
    public static <E> SortedSet<E> emptySortedSet() {
        return EmptySortedSet.INSTANCE;
    }

    /**
     * Returns the specified array as an immutable set, or {@code null} if the array is null.
     *
     * @param  <E> The type of array elements.
     * @param  array The array to copy in a set. May be {@code null}.
     * @return A set containing the array elements, or {@code null} if the given array was null.
     *
     * @see Collections#unmodifiableSet(Set)
     *
     * @since 3.17
     */
    @SafeVarargs
    public static <E> Set<E> immutableSet(final E... array) {
        if (array == null) {
            return null;
        }
        switch (array.length) {
            case 0:  return Collections.emptySet();
            case 1:  return Collections.singleton(array[0]);
            default: return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(array)));
        }
    }

    /**
     * Returns a unmodifiable version of the given map. This method is different than the
     * standard {@link Collections#unmodifiableMap(Map)} in that it tries to returns a more
     * efficient object when there is zero or one element. <em>The map returned by this
     * method may or may not be a view of the given map</em>. Consequently this method
     * shall be used <strong>only</strong> if the given map will not be modified after this
     * method call. In case of doubt, use the standard {@link Collections#unmodifiableMap(Map)}.
     *
     * @param  <K>  The type of keys in the map.
     * @param  <V>  The type of values in the map.
     * @param  map  The map to make unmodifiable, or {@code null}.
     * @return A unmodifiable version of the given map, or {@code null} if the given map was null.
     *
     * @since 3.18 (derived from 3.17)
     */
    public static <K,V> Map<K,V> unmodifiableMap(Map<K,V> map) {
        if (map != null) switch (map.size()) {
            case 0: {
                map = Collections.emptyMap();
                break;
            }
            case 1: {
                final Map.Entry<K,V> entry = map.entrySet().iterator().next();
                map = Collections.singletonMap(entry.getKey(), entry.getValue());
                break;
            }
            default: {
                map = Collections.unmodifiableMap(map);
                break;
            }
        }
        return map;
    }

    /**
     * The comparator to be returned by {@code #listComparator} and similar methods. Can not be
     * public because of parameterized types: we need a method for casting to the expected type.
     * This is the same trick than {@link Collections#emptySet()} for example.
     *
     * @since 3.18 (derived from 2.5)
     */
    @SuppressWarnings("rawtypes")
    private static final class Compare implements Comparator<Collection<Comparable>>, Serializable {
        /**
         * The unique instance.
         */
        static final Comparator<Collection<Comparable>> INSTANCE = new Compare();

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -8926770873102046405L;

        /**
         * Compares to collections of comparable objects.
         */
        @Override
        @SuppressWarnings("unchecked")
        public int compare(final Collection<Comparable> c1, final Collection<Comparable> c2) {
            final Iterator<Comparable> i1 = c1.iterator();
            final Iterator<Comparable> i2 = c2.iterator();
            int c;
            do {
                final boolean h1 = i1.hasNext();
                final boolean h2 = i2.hasNext();
                if (!h1) return h2 ? -1 : 0;
                if (!h2) return +1;
                final Comparable e1 = i1.next();
                final Comparable e2 = i2.next();
                c = e1.compareTo(e2);
            } while (c == 0);
            return c;
        }
    };

    /**
     * Returns a comparator for lists of comparable elements. The first element of each list
     * are {@linkplain Comparable#compareTo compared}. If one is <cite>greater than</cite> or
     * <cite>less than</cite> the other, the result of that comparison is returned. Otherwise
     * the second element are compared, and so on until either non-equal elements are found,
     * or end-of-list are reached. In the later case, the shortest list is considered
     * <cite>less than</cite> the longest one.
     * <p>
     * If both lists have the same length and equal elements in the sense of
     * {@link Comparable#compareTo}, then the comparator returns 0.
     *
     * @param <T> The type of elements in both lists.
     * @return The ordering between two lists.
     *
     * @since 3.18 (derived from 2.5)
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T extends Comparable<T>> Comparator<List<T>> listComparator() {
        return (Comparator) Compare.INSTANCE;
    }

    /**
     * Returns a comparator for sorted sets of comparable elements. The elements are compared in
     * iteration order as for the {@linkplain #listComparator list comparator}.
     *
     * @param <T> The type of elements in both sets.
     * @return The ordering between two sets.
     *
     * @since 3.18 (derived from 2.5)
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T extends Comparable<T>> Comparator<SortedSet<T>> sortedSetComparator() {
        return (Comparator) Compare.INSTANCE;
    }

    /**
     * Returns a comparator for arbitrary collections of comparable elements. The elements are
     * compared in iteration order as for the {@linkplain #listComparator list comparator}.
     *
     * <em>This comparator make sense only for collections having determinist order</em>
     * like {@link java.util.TreeSet}, {@link java.util.LinkedHashSet} or queues.
     * Do <strong>not</strong> use it with {@link java.util.HashSet}.
     *
     * @param <T> The type of elements in both collections.
     * @return The ordering between two collections.
     *
     * @since 3.18 (derived from 2.5)
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T extends Comparable<T>> Comparator<Collection<T>> collectionComparator() {
        return (Comparator) Compare.INSTANCE;
    }

    /**
     * Returns the capacity to be given to the {@link java.util.HashMap#HashMap(int) HashMap}
     * constructor for holding the given number of elements. This method computes the capacity
     * for the default <cite>load factor</cite>, which is 0.75.
     * <p>
     * The same calculation can be used for {@link java.util.LinkedHashMap} and
     * {@link java.util.HashSet} as well, which are built on top of {@code HashMap}.
     *
     * @param elements The number of elements to be put into the hash map or hash set.
     * @return The optimal initial capacity to be given to the hash map constructor.
     */
    public static int hashMapCapacity(int elements) {
        final int r = elements >>> 2;
        if (elements != (r << 2)) {
            elements++;
        }
        return elements + r;
    }

    /**
     * Copies the content of the given collection to a standard Java collection. This method can be
     * used when a in-memory, unsynchronized and modifiable copy of a collection is desired without
     * prior knowledge of the collection type. The following table gives the type mapping applied
     * by the method:
     * <p>
     * <table border="1" cellspacing="0" cellpadding="2">
     * <tr bgcolor="lightblue"><th>Input type</th><th>Output type</th></tr>
     * <tr><td>{@link SortedSet}</td><td>{@link TreeSet}</td></tr>
     * <tr><td>{@link HashSet}</td><td>{@link HashSet}</td></tr>
     * <tr><td>Other {@link Set}</td><td>{@link LinkedHashSet}</td></tr>
     * <tr><td>{@link Queue}</td><td>{@link LinkedList}</td></tr>
     * <tr><td>{@link List} or other {@link Collection}</td><td>{@link ArrayList}</td></tr>
     * </table>
     *
     * @param  <E> The type of elements in the collection.
     * @param  collection The collection to copy, or {@code null}.
     * @return A copy of the given collection, or {@code null} if the given collection was null.
     *
     * @since 3.18 (derived from 3.00)
     */
    @SuppressWarnings("unchecked")
    public static <E> Collection<E> copy(final Collection<E> collection) {
        if (collection == null) {
            return null;
        }
        /*
         * We will use the clone() method when possible because they are
         * implemented in a more efficient way than the copy constructors.
         */
        final Class<?> type = collection.getClass();
        if (collection instanceof Set<?>) {
            if (collection instanceof SortedSet<?>) {
                if (type == TreeSet.class) {
                    return (Collection<E>) ((TreeSet<E>) collection).clone();
                }
                return new TreeSet<>(collection);
            }
            if (type == HashSet.class || type == LinkedHashSet.class) {
                return (Collection<E>) ((HashSet<E>) collection).clone();
            }
            return new LinkedHashSet<>(collection);
        }
        if (collection instanceof Queue<?>) {
            if (type == LinkedList.class) {
                return (Collection<E>) ((LinkedList<E>) collection).clone();
            }
            return new LinkedList<>(collection);
        }
        if (type == ArrayList.class) {
            return (Collection<E>) ((ArrayList<E>) collection).clone();
        }
        return new ArrayList<>(collection);
    }

    /**
     * Copies the content of the given map to a standard Java map. This method can be used when a
     * in-memory, unsynchronized and modifiable copy of a map is desired without prior knowledge
     * of the map type. The following table gives the type mapping applied by the method:
     * <p>
     * <table border="1" cellspacing="0" cellpadding="2">
     * <tr bgcolor="lightblue"><th>Input type</th><th>Output type</th></tr>
     * <tr><td>{@link SortedMap}</td><td>{@link TreeMap}</td></tr>
     * <tr><td>{@link HashMap}</td><td>{@link HashMap}</td></tr>
     * <tr><td>Other {@link Map}</td><td>{@link LinkedHashMap}</td></tr>
     * </table>
     *
     * @param  <K> The type of keys in the map.
     * @param  <V> The type of values in the map.
     * @param  map The map to copy, or {@code null}.
     * @return A copy of the given map, or {@code null} if the given map was null.
     *
     * @since 3.18 (derived from 3.00)
     */
    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> copy(final Map<K,V> map) {
        if (map == null) {
            return null;
        }
        /*
         * We will use the clone() method when possible because they are
         * implemented in a more efficient way than the copy constructors.
         */
        final Class<?> type = map.getClass();
        if (map instanceof SortedMap<?,?>) {
            if (type == TreeMap.class) {
                return (Map<K,V>) ((TreeMap<K,V>) map).clone();
            }
            return new TreeMap<>(map);
        }
        if (type == HashMap.class || type == LinkedHashMap.class) {
            return (Map<K,V>) ((HashMap<K,V>) map).clone();
        }
        return new LinkedHashMap<>(map);
    }
}
