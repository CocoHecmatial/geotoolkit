/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
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
package org.geotoolkit.storage.coverage;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.Set;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.util.LazySet;
import org.geotoolkit.lang.Static;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.parameter.ParameterValueGroup;


/**
 * Creates {@link CoverageStore} instances from a set of parameters.
 *
 * {@section Registration}
 * {@link CoverageStore} factories must implement the {@link CoverageStoreFactory} interface and declare their
 * fully qualified class name in a {@code META-INF/services/org.geotoolkit.coverage.CoverageStoreFactory}
 * file. See the {@link ServiceLoader} javadoc for more information.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @version 3.20
 *
 * @since 3.20
 * @module pending
 */
public final class CoverageStoreFinder extends Static {
    /**
     * The service loader. This loader and its iterator are not synchronized;
     * when doing an iteration, the iterator must be used inside synchronized blocks.
     */
    private static final ServiceLoader<CoverageStoreFactory> loader = ServiceLoader.load(CoverageStoreFactory.class);

    /**
     * Do not allow instantiation of this class.
     */
    private CoverageStoreFinder() {
    }

    /**
     * Returns the set of all factories, optionally filtered by type and availability.
     * This method ensures also that the iterator backing the set is properly synchronized.
     * <p>
     * Note that the iterator doesn't need to be thread-safe; this is the accesses to the
     * underlying {@linkplain #loader}, directly or indirectly through its iterator, which
     * need to be thread-safe.
     *
     * @param  <T>  The type of factories to be returned.
     * @param  type The type of factories to be returned, or {@code null} for all kind of factories.
     * @param  all  {@code true} for all factories, or {@code false} for only available factories.
     * @return The set of factories for the given conditions.
     */
    private static synchronized <T extends CoverageStoreFactory> Set<T> getFactories(final Class<T> type, final boolean all) {
        final Iterator<CoverageStoreFactory> factories = loader.iterator();
        return new LazySet<T>(new Iterator<T>() {
            /**
             * The next factory to be returned by the {@link #next()} method, or {@code null}
             * if not yet computed. This field is set by the {@link #hasNext()} method.
             */
            private T next;

            /**
             * Returns {@code true} if there is more factories to return.
             * This implementation fetches immediately the next factory.
             */
            @Override
            @SuppressWarnings("unchecked")
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }
                synchronized (CoverageStoreFinder.class) {
                    while (factories.hasNext()) {
                        final CoverageStoreFactory candidate = factories.next();
                        if (type == null || type.isInstance(candidate)) {
                            if (all || candidate.availability().pass()) {
                                next = (T) candidate;
                                return true;
                            }
                        }
                    }
                }
                return false;
            }

            /**
             * Returns the next element in the iteration.
             */
            @Override
            public T next() {
                if (hasNext()) {
                    final T n = next;
                    next = null; // Tells to hasNext() that it will need to fetch a new element.
                    return n;
                }
                throw new NoSuchElementException("No more elements.");
            }

            /**
             * Unsupported operation, since this iterator is read-only.
             */
            @Override
            public void remove() {
                throw new UnsupportedOperationException("Can not remove elements from this iterator.");
            }
        });
    }

    /**
     * Returns all factories of the given type, regardless of their
     * {@linkplain CoverageStoreFactory#availability() availability}.
     *
     * @param  <T>  The type of the factories to fetch.
     * @param  type The type of the factories to fetch, or {@code null} for fetching all of them.
     * @return The set of all factories of the given type.
     */
    public static <T extends CoverageStoreFactory> Set<T> getAllFactories(final Class<T> type) {
        return getFactories(type, true);
    }

    /**
     * Returns factories of the given type which are
     * {@linkplain CoverageStoreFactory#availability() available}.
     *
     * @param  <T>  The type of the factories to fetch.
     * @param  type The type of the factories to fetch, or {@code null} for fetching very types.
     * @return The set of available factories of the given type.
     */
    public static <T extends CoverageStoreFactory> Set<T> getAvailableFactories(final Class<T> type) {
        return getFactories(type, false);
    }

    /**
     * Returns a factory having an {@linkplain CoverageStoreFactory#getIdentification() identification}
     * equals (ignoring case) to the given string. If more than one factory is found, then this
     * method selects an arbitrary one. If no factory is found, then this method returns
     * {@code null}.
     *
     * @param  identifier The identifier of the factory to find.
     * @return A factory for the given identifier, or {@code null} if none.
     */
    public static synchronized CoverageStoreFactory getFactoryById(final String identifier) {
        for (final CoverageStoreFactory factory : loader) {
            if (Citations.identifierMatches(factory.getIdentification().getCitation(), identifier)) {
                return factory;
            }
        }
        return null;
    }

    /**
     * Creates a {@link CoverageStore} instance for the given map of parameter values. This method iterates
     * over all {@linkplain #getAvailableFactories(Class) available factories} until a factory
     * claiming to {@linkplain CoverageStoreFactory#canProcess(Map) be able to process} the given
     * parameters is found. This factory then {@linkplain CoverageStoreFactory#create(Map) create}
     * the coverage store.
     *
     * @param  parameters The configuration of the desired coverage store.
     * @return A coverage store created from the given parameters, or {@code null} if none.
     * @throws CoverageStoreException If a factory is found but can't create the coverage store.
     */
    public static CoverageStore open(final Map<String, Serializable> parameters) throws DataStoreException {
        ArgumentChecks.ensureNonNull("parameters", parameters);
        return open(null, parameters);
    }

    /**
     * Creates a {@link CoverageStore} instance for the given parameters group. This method iterates over
     * all {@linkplain #getAvailableFactories(Class) available factories} until a factory claiming
     * to {@linkplain CoverageStoreFactory#canProcess(ParameterValueGroup) be able to process} the given
     * parameters is found. This factory then {@linkplain CoverageStoreFactory#create(ParameterValueGroup)
     * create} the coverage store.
     *
     * @param  parameters The configuration of the desired coverage store.
     * @return A coverage store created from the given parameters, or {@code null} if none.
     * @throws CoverageStoreException If a factory is found but can't create the coverage store.
     */
    public static CoverageStore open(final ParameterValueGroup parameters) throws DataStoreException {
        ArgumentChecks.ensureNonNull("parameters", parameters);
        return open(parameters, null);
    }

    /**
     * Implementation of the public {@code create} method. Exactly one of the {@code parameters}
     * and {@code asMap} arguments shall be non-null.
     */
    private static synchronized CoverageStore open(final ParameterValueGroup parameters,
            final Map<String, Serializable> asMap) throws DataStoreException
    {
        CharSequence unavailable = null;
        for (final CoverageStoreFactory factory : loader) {
            if ((parameters != null) ? factory.canProcess(parameters) : factory.canProcess(asMap)) {
                if (factory.availability().pass()) {
                    return (parameters != null) ? factory.open(parameters) : factory.open(asMap);
                } else if (unavailable == null) {
                    unavailable = factory.getDisplayName();
                }
            }
        }
        if (unavailable != null) {
            throw new DataStoreException("The " + unavailable + " coverage store is not available. "
                    + "Are every required JAR files accessible on the classpath?");
        }
        return null;
    }

    /**
     * Scans for factory plug-ins on the application class path. This method is needed because the
     * application class path can theoretically change, or additional plug-ins may become available.
     * Rather than re-scanning the classpath on every invocation of the API, the class path is scanned
     * automatically only on the first invocation. Clients can call this method to prompt a re-scan.
     * Thus this method need only be invoked by sophisticated applications which dynamically make
     * new plug-ins available at runtime.
     */
    public static synchronized void scanForPlugins() {
        loader.reload();
    }

}
