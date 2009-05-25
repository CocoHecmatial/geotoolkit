/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Open Source Geospatial Foundation (OSGeo)
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

import org.geotoolkit.internal.io.ObjectStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


/**
 * Wraps a {@link ObjectStream} in an implementation that can be made available remotely.
 *
 * {@section Thread-safety}
 * This wrapper doesn't perform explicit synchronization, since it assumes that the
 * backing stream is thread-safe.
 *
 * @param <E> The type of elements returned by the stream.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.00
 *
 * @since 3.00
 * @module
 */
final class RemoteStream<E> extends UnicastRemoteObject implements ObjectStream<E> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 8406150195639660789L;

    /**
     * The wrapped stream.
     */
    private final ObjectStream<E> stream;

    /**
     * Creates a new wrapper for the given stream.
     *
     * @param stream The stream to wrap.
     * @throws RemoteException If an error occured while exporting the server object.
     */
    public RemoteStream(final ObjectStream<E> stream) throws RemoteException {
        this.stream = stream;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException If an I/O or RMI error occured.
     */
    @Override
    public E next() throws IOException {
        return stream.next();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException If an I/O or RMI error occured.
     */
    @Override
    public void close() throws IOException {
        stream.close();
    }
}
