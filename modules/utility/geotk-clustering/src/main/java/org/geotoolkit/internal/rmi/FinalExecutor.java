/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2012, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.internal.rmi;

import java.rmi.RemoteException;
import org.geotoolkit.internal.Threads;


/**
 * A private class for {@link ClusterCommands} which exit the JVM after the shutdown.
 * It should be the very last executor to be run.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.00
 *
 * @since 3.00
 * @module
 */
final class FinalExecutor extends RemoteExecutor implements Runnable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4747059278529898416L;

    /**
     * Creates a new remote executor with the given master, which may be {@code null).
     *
     * @throws RemoteException If an error occurred while exporting this executor.
     */
    FinalExecutor(final TaskExecutor master) throws RemoteException {
        super(master);
    }

    /**
     * Exists the JVM after the shutdown.
     */
    @Override
    public void shutdown() throws RemoteException {
        super.shutdown();
        // This is not really a shutdown hook, but close
        // (more a kind of "pre-shutdown hook").
        new Thread(Threads.RESOURCE_DISPOSERS, this, "exit").start();
    }

    /**
     * Waits a little bit before to exit. This is needed in order to avoid
     * a {@link java.io.EOFException} on the caller side.
     */
    @Override
    public void run() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // Someone doesn't want to let us sleep.
        }
        System.exit(0);
    }
}
