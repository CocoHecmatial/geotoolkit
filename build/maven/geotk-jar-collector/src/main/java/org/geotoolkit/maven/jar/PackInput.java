/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
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
package org.geotoolkit.maven.jar;

import java.util.Enumeration;
import java.util.jar.*;
import java.io.File;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;


/**
 * A JAR file to be used for input by {@link Packer}.
 * Those files will be open in read-only mode.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.00
 *
 * @since 3.00
 */
final class PackInput implements Closeable {
    /**
     * The {@code value} directory.
     */
    public static final String META_INF = "META-INF/";

    /**
     * The {@code value} directory.
     */
    public static final String SERVICES = META_INF + "services/";

    /**
     * The JAR file.
     */
    private JarFile file;

    /**
     * The main class obtained from the manifest, or {@code null} if none.
     */
    public final String mainClass;

    /**
     * An enumeration over the entries. We are going to iterate only once.
     */
    private Enumeration<JarEntry> entries;

    /**
     * The current entry under iteration.
     */
    private JarEntry entry;

    /**
     * Opens the given JAR file in read-only mode.
     *
     * @param  file The file to open.
     * @throws IOException if the file can't be open.
     */
    PackInput(final File file) throws IOException {
        this.file = new JarFile(file);
        final Manifest manifest = this.file.getManifest();
        if (manifest != null) {
            final Attributes attributes = manifest.getMainAttributes();
            if (attributes != null) {
                mainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
                return;
            }
        }
        mainClass = null;
    }

    /**
     * Returns the entries in the input JAR file.
     *
     * @return The next entry, or {@code null} if the iteration is finished.
     */
    JarEntry nextEntry() {
        if (entries == null) {
            entries = file.entries();
        }
        while (entries.hasMoreElements()) {
            entry = entries.nextElement();
            final String name = entry.getName();
            if (name.startsWith(META_INF)) {
                if (!name.startsWith(SERVICES)) {
                    continue;
                }
            }
            entry.setMethod(JarEntry.DEFLATED);
            entry.setCompressedSize(-1); // Change in method has changed the compression size.
            return entry;
        }
        return entry = null;
    }

    /**
     * Returns the input stream for the current entry.
     *
     * @param entry The entry for which to get an input stream.
     */
    InputStream getInputStream() throws IOException {
        return file.getInputStream(entry);
    }

    /**
     * Returns the input stream for the entry of the given name. This method must be invoked
     * before the first call to {@link #nextEntry}. Each entry can be requested only once.
     *
     * @param  name The name of the entry
     * @return The input stream for the requested entry, or {@code null} if none.
     * @throws IOException If the entry can not be read.
     * @throws IllegalStateException Programming error (pre-condition violated).
     */
    InputStream getInputStream(final String name) throws IOException {
        if (entries != null) {
            throw new IllegalStateException("Too late for this method.");
        }
        final JarEntry candidate = file.getJarEntry(name);
        if (candidate == null) {
            return null;
        }
        return file.getInputStream(candidate);
    }

    /**
     * Closes this input.
     *
     * @throws IOException if an error occured while closing the file.
     */
    @Override
    public void close() throws IOException {
        if (file != null) {
            file.close();
        }
        file    = null;
        entry   = null;
        entries = null;
    }
}
