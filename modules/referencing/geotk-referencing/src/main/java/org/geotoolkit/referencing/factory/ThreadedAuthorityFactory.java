/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2005-2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.referencing.factory;

import java.util.Map;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.awt.RenderingHints;

import org.opengis.referencing.FactoryException;

import org.geotoolkit.factory.Hints;
import org.geotoolkit.resources.Errors;
import org.geotoolkit.internal.FactoryUtilities;


/**
 * A caching authority factory which delegates to different instances of a backing store for
 * concurrency in multi-thread environment. This factory delays the {@linkplain #createBackingStore
 * creation of a backing store} until first needed, and {@linkplain AbstractAuthorityFactory#dispose
 * dispose} it after some timeout. This approach allows to etablish a connection to a database (for
 * example) and keep it only for a relatively short amount of time.
 *
 * {@section Multi-threading}
 * If two or more threads are accessing this factory in same time, then two or more instances
 * of the backing store may be created. The maximal amount of instances to create is specified
 * at {@code ThreadedAuthorityFactory} construction time. If more backing store instances are
 * needed, some of the threads will block until an instance become available.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.00
 *
 * @since 2.1
 * @module
 */
public abstract class ThreadedAuthorityFactory extends CachingAuthorityFactory {
    /**
     * A backing store used by {@link ThreadedAuthorityFactory}. A new instance is created
     * everytime a backing factory is {@linkplain ThreadedAuthorityFactory#release released}.
     * In a mono-thread application, there is typically only one instance at a given time.
     * However if more than one than one thread are requesting new objects concurrently,
     * than many instances may exist for the same {@code ThreadedAuthorityFactory}.
     */
    private static final class Store {
        /**
         * The factory used as a backing store, which has just been released
         * and made available for reuse.
         */
        final AbstractAuthorityFactory factory;

        /**
         * The timestamp at the time this object has been created. Because instances of
         * {@code Store} are created when backing stores are released, this is the time
         * when we finished using that {@linkplain #factory}.
         */
        final long timestamp;

        /**
         * Creates a new instance wrapping the given factory.
         * The factory must be already available for reuse.
         */
        Store(final AbstractAuthorityFactory factory) {
            this.factory = factory;
            timestamp = System.currentTimeMillis();
        }
    }

    /**
     * The backing store instances previously created and released for future reuse.
     * Last used factories must be {@linkplain Deque#addLast added last}. This is
     * used as a LIFO stack.
     */
    private final Deque<Store> stores;

    /**
     * The amount of backing stores that can still be created. This number is decremented
     * in a synchronized block every time a backing store is in use, and incremented once
     * released.
     */
    private int remainingBackingStores;

    /**
     * Counts how many time a factory has been used in the current thread. This is used in order to
     * reuse the same factory (instead than creating new instance) when an {@code AuthorityFactory}
     * implementation invokes itself indirectly through the {@link CachingAuthorityFactory}. This
     * assumes that factory implementations are reentrant.
     */
    private static final class Usage {
        /**
         * The factory used as a backing store.
         */
        AbstractAuthorityFactory factory;

        /**
         * Incremented on every call to {@link #getBackingStore()} and decremented on every call
         * to {@link #release}. When this value reach zero, the factory is really released.
         */
        int count;
    }

    /**
     * The factory currently in use by the current thread.
     */
    private final ThreadLocal<Usage> current = new ThreadLocal<Usage>() {
        @Override protected Usage initialValue() {
            return new Usage();
        }
    };

    /**
     * The delay of inactivity (in milliseconds) before to close a backing store.
     * The default value is one day, which is long enough to be like "no timeout"
     * for a normal working day while keeping a safety limit. Subclasses will set
     * a shorter value more suitable to server environment.
     * <p>
     * Every access to this field must be performed in a synchronized block.
     */
    private long timeout = 24 * 60 * 60 * 1000L;

    /**
     * The maximal difference between the scheduled time and the actual time in order to
     * perform the factory disposal. This is used as a tolerance value for possible wait
     * time inaccuracy.
     */
    static final long TIMEOUT_RESOLUTION = 100;

    /**
     * The delay (in milliseconds) after which to make a check even if no factory has been
     * reported as expired. This check is performed in case of bug and should not be costly.
     */
    private static final long SAFETY_CHECK_DELAY = 60 * 60 * 1000L;

    /**
     * {@code true} if this factory contains at least one active backing stores.
     * Note that a {@code ThreadedAuthorityFactory} may be active while having an
     * empty queue of {@linkplain #stores} if all backing stores are currently in
     * use.
     * <p>
     * Every access to this field must be performed in a synchronized block.
     */
    private boolean isActive;

    /**
     * Tells if {@link ReferencingFactoryContainer#hints} has been invoked. It must be
     * invoked exactly once. We will initialize the hints as late as possible because
     * it implies the creation of a backing factory, which may be costly.
     */
    private volatile boolean hintsInitialized;

    /**
     * Constructs an instance using the default setting. Subclasses are responsible for
     * creating an appropriate backing store when the {@link #createBackingStore} method
     * is invoked.
     *
     * @param userHints An optional set of hints, or {@code null} for the default ones.
     *
     * @since 2.2
     */
    protected ThreadedAuthorityFactory(final Hints userHints) {
        this(userHints, DEFAULT_MAX, 16);
        /*
         * NOTE: if the default maximum number of backing stores (currently 16) is augmented,
         * make sure to augment the number of runner threads in the "StressTest" class to a
         * greater amount.
         */
    }

    /**
     * Constructs an instance using the given setting. Subclasses are responsible for
     * creating an appropriate backing store when the {@link #createBackingStore} method
     * is invoked.
     *
     * @param userHints An optional set of hints, or {@code null} for the default ones.
     * @param maxStrongReferences The maximum number of objects to keep by strong reference.
     * @param maxBackingStores The maximal amount of backing stores to create. This is the
     *        maximal amount of threads that can use this factory without blocking each other
     *        when the requested objects are not in the cache.
     *
     * @since 3.00
     */
    protected ThreadedAuthorityFactory(final Hints userHints,
            final int maxStrongReferences, final int maxBackingStores)
    {
        super(userHints, maxStrongReferences);
        ensureNotSmaller("maxBackingStores", maxBackingStores, 1);
        stores = new LinkedList<Store>();
        remainingBackingStores = maxBackingStores;
    }

    /**
     * Returns the implementation hints. At the opposite of most factories that delegate their work
     * to an other factory (like the {@code CachingAuthorityFactory} parent class), this method does
     * <strong>not</strong> set {@link Hints#CRS_AUTHORITY_FACTORY} and its friends to the backing
     * store. This is because the backing stores may be created and destroyed at any time, while the
     * implementation hints are expected to be stable. Instead, the implementation hints of a
     * backing store are copied straight in this {@code ThreadedAuthorityFactory} hint map.
     */
    @Override
    public Map<RenderingHints.Key, ?> getImplementationHints() {
        if (!hintsInitialized) {
            try {
                final AbstractAuthorityFactory factory = getBackingStore();
                try {
                    final Map<RenderingHints.Key, ?> toAdd;
                    toAdd = factory.getImplementationHints();
                    /*
                     * Double-check locking: was a deprecated practice before Java 5, but is okay
                     * since Java 5 provided that 'hintsInitialized' is volatile. It is important
                     * to invoke factory.getImplementationHints()  outside the synchronized block
                     * in order to reduce the risk of deadlock. It is not a big deal if its value
                     * is computed twice.
                     */
                    synchronized (this) {
                        if (!hintsInitialized) {
                            hintsInitialized = true;
                            hints.putAll(toAdd);
                        }
                    }
                } finally {
                    release();
                }
            } catch (FactoryException exception) {
                synchronized (this) {
                    unavailable(exception);
                    hintsInitialized = true; // For preventing other tries.
                }
            }
        }
        return super.getImplementationHints();
    }

    /**
     * Returns the number of backing stores. This count does not include the backing stores
     * that are currently under execution. This method is used only for testing purpose.
     */
    final synchronized int countBackingStores() {
        return stores.size();
    }

    /**
     * Creates the backing store authority factory. This method is invoked the first time a
     * {@code createXXX(...)} method is invoked. It may also be invoked again if additional
     * factories are needed in different threads, or if all factories have been disposed
     * after the timeout.
     *
     * @return The backing store to uses in {@code createXXX(...)} methods.
     * @throws NoSuchFactoryException if the backing store has not been found.
     * @throws FactoryException if the creation of backing store failed for an other reason.
     */
    protected abstract AbstractAuthorityFactory createBackingStore()
            throws NoSuchFactoryException, FactoryException;

    /**
     * Returns a backing store authority factory. This method <strong>must</strong>
     * be used together with {@link #release} in a {@code try ... finally} block.
     *
     * @return The backing store to uses in {@code createXXX(...)} methods.
     * @throws FactoryException if the creation of backing store failed.
     */
    @Override
    final AbstractAuthorityFactory getBackingStore() throws FactoryException {
        /*
         * First checks if the current thread is already using a factory. If yes, we will
         * avoid creating new factories on the assumption that factories are reentrant.
         */
        final Usage usage = current.get();
        AbstractAuthorityFactory factory = usage.factory;
        if (factory == null) synchronized (this) {
            /**
             * If we have reached the maximal amount of backing stores allowed, wait for a backing
             * store to become available. In theory the 2 seconds timeout is not necessary, but we
             * put it as a safety in case we fail to invoke a notify() matching this wait(), for
             * example someone else is waiting on this monitor or because the release(...) method
             * threw an exception.
             */
            while (remainingBackingStores == 0) try {
                wait(2000);
            } catch (InterruptedException e) {
                // Someone doesn't want to let us sleep. Checks again the status.
            }
            /*
             * Reuses the most recently used factory, if available. If there is no factory
             * available for reuse, creates a new one. We don't add it to the queue now;
             * it will be done by the release(...) method.
             */
            Store store = stores.pollLast();
            if (store != null) {
                factory = store.factory;
            } else {
                factory = createBackingStore();
                if (factory == null) {
                    throw new NoSuchFactoryException(Errors.format(Errors.Keys.NO_DATA_SOURCE));
                }
                /*
                 * If the backing store we just created is the first one, awake the
                 * disposer thread which was waiting for an indefinite amount of time.
                 */
                if (!isActive) {
                    isActive = true;
                    StoreDisposer.INSTANCE.schedule(this,
                            System.currentTimeMillis() + Math.min(timeout, SAFETY_CHECK_DELAY));
                }
            }
            assert usage.count == 0;
            usage.factory = factory;
            remainingBackingStores--; // Must be done last when we are sure to not fail.
        }
        // Increment below is safe even if outside the synchronized block,
        // because each thread own exclusively its Usage instance
        usage.count++;
        return factory;
    }

    /**
     * Releases the backing store previously obtained with {@link #getBackingStore}.
     * This method marks the factory as available for reuse by other threads.
     */
    @Override
    final synchronized void release() {
        final Usage usage = current.get();
        if (--usage.count == 0) {
            remainingBackingStores++; // Must be done first in case an exception happen after this point.
            final AbstractAuthorityFactory factory = usage.factory;
            usage.factory = null;
            notify(); // We released only one backing store, so awake only one thread - not all of them.
            if (!stores.offerLast(new Store(factory))) {
                /*
                 * We were unable to add the factory to the queue. It may be because the queue is full,
                 * which could happen if there is too much factories created recently (this behavior is
                 * enabled only if the queue is some implementation having a limited capacity). This is
                 * probably not worth to keep yet more factories, so dispose the current one immediatly.
                 */
                dispose(factory, false);
            }
        }
        assert usage.count >= 0 && (usage.factory == null) == (usage.count == 0) : usage.count;
    }

    /**
     * Returns {@code true} if this factory contains at least one active backing store.
     * A backing store is "active" if it has been created for a previous request and not
     * yet disposed after a period of inactivity equals to the {@linkplain #getTimeout()
     * timeout}.
     * <p>
     * A return value of {@code false} typically implies that every connection to the
     * underlying database (if any) used by this factory have been closed.
     *
     * @return {@code true} if this factory contains at least one active backing store.
     *
     * @since 3.00
     */
    public synchronized boolean isActive() {
        return isActive;
    }

    /**
     * Returns the current timeout.
     *
     * @return The current timeout.
     *
     * @since 3.00
     */
    public synchronized long getTimeout() {
        return timeout;
    }

    /**
     * Sets a timer for disposing the backing store after the specified amount of milliseconds
     * of inactivity. If a new backing store is needed after the disposal of the current one,
     * then the {@link #createBackingStore} method will be invoked again.
     * <p>
     * Note that the backing store disposal can be vetoed if {@link #canDisposeBackingStore}
     * returns {@code false}.
     *
     * @param delay The delay of inactivity (in milliseconds) before to close a backing store.
     */
    public synchronized void setTimeout(final long delay) {
        if (delay <= 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.NOT_GREATER_THAN_ZERO_$1, delay));
        }
        timeout = delay; // Will be taken in account after the next factory to dispose.
    }

    /**
     * Returns {@code true} if the given backing store can be disposed now. This method is invoked
     * automatically after the amount of time specified by {@link #setTimeout}, providing that the
     * factory was not used during that time. The default implementation always returns {@code true}.
     * Subclasses should override this method and returns {@code false} if they want to prevent the
     * backing store disposal under some circonstances.
     *
     * @param backingStore The backing store in process of being disposed.
     * @return {@code true} if the backing store can be disposed now.
     */
    protected boolean canDisposeBackingStore(final AbstractAuthorityFactory backingStore) {
        return true;
    }

    /**
     * Disposes the given backing store in a background thread. We use a background thread
     * in part for avoiding {@link #disposeExpired()} to be blocked  if there is a problem
     * with a factory, because {@code disposeExpired()} is run in a thread which is shared
     * by all {@code ThreadedAuthorityFactory} instances. An other effect is to avoid to
     * run the user-code while we hold a synchronization lock on this factory.
     */
    private void dispose(final AbstractAuthorityFactory factory, final boolean shutdown) {
        final Thread thread = new Thread(FactoryUtilities.DISPOSAL_GROUP, (Runnable) null) {
            @Override public void run() {
                if (shutdown || canDisposeBackingStore(factory)) {
                    factory.dispose(shutdown);
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Releases resources immediately instead of waiting for the garbage collector.
     * This method disposes all backing stores.
     *
     * @param shutdown {@code false} for normal disposal, or {@code true} if
     *        this method is invoked during the process of a JVM shutdown.
     */
    @Override
    protected synchronized void dispose(final boolean shutdown) {
        StoreDisposer.INSTANCE.cancel(this);
        Store store;
        // Dispose from least recent to most recent.
        while ((store = stores.pollFirst()) != null) {
            dispose(store.factory, shutdown);
        }
        super.dispose(shutdown);
    }

    /**
     * Disposes the expired entries. This method should be invoked from the
     * {@link StoreDisposer} thread only.
     *
     * @return When (in milliseconds) to run the next check for disposal.
     */
    final synchronized long disposeExpired() {
        final long currentTimeMillis = System.currentTimeMillis();
        final Iterator<Store> it = stores.iterator();
        while (it.hasNext()) {
            final Store store = it.next();
            /*
             * Computes how much time we need to wait again before we can dispose the factory.
             * If this time is greater than some arbitrary amount, do not dispose the factory
             * and wait again.
             */
            long delay = timeout - (currentTimeMillis - store.timestamp);
            if (delay > TIMEOUT_RESOLUTION) {
                // Found a factory which is not expired. Stop the search,
                // since the iteration is expected to be ordered.
                return currentTimeMillis + Math.min(delay, SAFETY_CHECK_DELAY);
            }
            // Found an expired factory. Dispose it and
            // search for other factories to dispose.
            it.remove();
            dispose(store.factory, false);
        }
        // If we reach this point, then all factories have been disposed.
        isActive = false;
        return currentTimeMillis + SAFETY_CHECK_DELAY;
    }
}
