/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008-2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.internal.io;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import org.geotoolkit.internal.OS;
import org.geotoolkit.resources.Errors;
import org.geotoolkit.util.logging.Logging;


/**
 * Methods related to the Geotoolkit installation directory. This is provided for data that need
 * to be saved in a user-specified directory. If the user didn't specified any directory, they
 * will be saved in the temporary directory.
 * <p>
 * We try to keep the configuration options to a strict minimum, but we still need is some case
 * to specify in which directory are stored the data, for example the NADCON data used for datum
 * shift over United States.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.0
 *
 * @since 3.0
 * @module
 */
public enum Installation {
    /**
     * The root directory of Geotoolkit installation.
     */
    ROOT_DIRECTORY("org/geotoolkit", "Root directory", null),

    /**
     * The grid shift file location, used for datum shift like NADCOM.
     */
    NADCON("org/geotoolkit/referencing/operation/transform", "Grid location", "NADCON"),

    /**
     * The EPSG database, or parameters required for a connection to a distant EPSG database.
     */
    EPSG("org/geotoolkit/referencing/factory", "EPSG", "EPSG");

    /**
     * The preference node and key for storing the value of this configuration option.
     */
    private final String node, key;

    /**
     * The default subdirectory in the root directory, or
     * {@code null} if this key is for the root directory.
     */
    private final String directory;

    /**
     * The default root directory. Computed only once at class initialization time in
     * order to make sure that the value stay consistent during all the JVM execution.
     */
    private static final File DEFAULT_ROOT = root();

    /**
     * Creates a new configuration key.
     *
     * @param node The preference node where to store the configuration value.
     * @param key  The key where to store the value in the above node.
     * @param directory The default subdirectory in the root directory.
     */
    private Installation(final String node, final String key, final String directory) {
        this.node = node;
        this.key = key;
        this.directory = directory;
    }

    /**
     * Returns the preferences node.
     */
    private Preferences preference(final boolean userSpecific) {
        return (userSpecific ? Preferences.userRoot() : Preferences.systemRoot()).node(node);
    }

    /**
     * Sets the preference to the given value. If the preference is set for the current user,
     * then the system preference is left untouched. But if the preference is set for the system,
     * we assume that it applies to all users including the current one, so the current user
     * preference is removed.
     *
     * @param userSpecific {@code true} for user preference, or {@code false} for system preference.
     * @param value The preference value, or {@code null} for removing it.
     */
    public final void set(final boolean userSpecific, final String value) {
        final Preferences prefs = preference(userSpecific);
        if (value != null) {
            prefs.put(key, value);
        } else {
            prefs.remove(key);
        }
        if (!userSpecific) {
            preference(true).remove(key);
        }
    }

    /**
     * Returns the preference, or {@code null} if none.
     *
     * @param  userSpecific {@code true} for user preference, or {@code false} for system preference.
     * @return The preference value, or {@code null} if none.
     */
    public final String get(final boolean userSpecific) {
        return preference(userSpecific).get(key, null);
    }

    /**
     * Returns the default root directory, ignoring user's preferences.
     */
    private static File root() {
        try {
            final String directory = System.getProperty("user.home");
            if (directory != null) {
                File file = new File(directory);
                String name = ".geotoolkit";
                switch (OS.current()) {
                    case WINDOWS: {
                        file = new File(file, "Application Data");
                        name = "Geotoolkit";
                        break;
                    }
                    case MAC_OS: {
                        file = new File(file, "Library");
                        name = "Geotoolkit";
                        break;
                    }
                    // For Linux and unknown OS, keep the directory selected above.
                }
                if (file.isDirectory() && file.canWrite()) {
                    return new File(file, name);
                }
            }
        } catch (SecurityException e) {
            Logging.getLogger("org.geotoolkit").warning(e.toString());
        }
        return new File(System.getProperty("java.io.tmpdir"), "Geotoolkit");
    }

    /**
     * If the preference is defined, returns its value as a {@link File}. Otherwise returns a
     * sub-directory of the <cite>root directory</cite> where the later is defined as the first
     * of the following directories which is found suitable:
     * <p>
     * <ul>
     *   <li>{@link #ROOT_DIRECTORY} user preferences, if defined.</li>
     *   <li>{@link #ROOT_DIRECTORY} system preferences, if defined.</li>
     *   <li>{@code ".geotoolkit"} subdirectory in the user home directory,
     *       if the user home directory exists and is writable.</li>
     *   <li>{@code "Geotoolkit"} subdirectory in the temporary directory.</li>
     * </ul>
     *
     * @param  usePreferences Usually {@code true}. If {@code false}, the preferences
     *         are ignored and only the default directory is returned.
     * @return The directory (never {@code null}).
     */
    public File directory(final boolean usePreferences) {
        if (usePreferences) {
            boolean user = true;
            do {
                final String candidate = get(user);
                if (candidate != null) {
                    return new File(candidate);
                }
            } while ((user = !user) == false);
        }
        if (directory != null) {
            return new File(ROOT_DIRECTORY.directory(true), directory);
        } else {
            return DEFAULT_ROOT;
        }
    }

    /**
     * Same as {@link #directory}, but creates the directory if it doesn't already exist.
     *
     * @param  usePreferences Usually {@code true}. If {@code false}, the preferences
     *         are ignored and only the default directory is returned.
     * @return The default directory.
     * @throws IOException If the subdirectory can't be created.
     */
    public File validDirectory(final boolean usePreferences) throws IOException {
        final File directory = directory(usePreferences);
        if (!directory.isDirectory()) {
            if (!directory.mkdirs()) {
                throw new IOException(Errors.format(Errors.Keys.CANT_CREATE_FACTORY_$1, directory));
            }
        }
        return directory;
    }

    /**
     * Returns a {@link File} if possible, or an {@link URL} otherwise, from a resource name.
     * If the string is not an URL or an absolute path, then the file is searched on the classpath
     * first, or in the directory given by {@link #directory()} if not found on the classpath.
     *
     * @param  caller The class to use for fetching resources, typically the caller class.
     * @param  path   A string representation of a filename or a URL.
     * @return A File or URL created from the string representation.
     * @throws IOException if the URL cannot be created.
     */
    public Object toFileOrURL(final Class<?> caller, final String path) throws IOException {
        final Object uof = IOUtilities.toFileOrURL(path);
        if (uof instanceof URL) {
            return (URL) uof;
        }
        File file = (File) uof;
        if (!file.isAbsolute()) {
            if (directory != null && file.getParent() == null) {
                final URL url = caller.getResource(directory + '/' + file.getPath());
                if (url != null) {
                    return url;
                }
            }
            // Just a file name, prepend base location.
            file = new File(directory(true), file.getPath());
        }
        return file;
    }

    /**
     * Tests if the file returned by {@link #toFileOrURL toFileOrURL} exists.
     *
     * @param  caller The class to use for fetching resources, typically the caller class.
     * @param  path   A string representation of a filename or a URL.
     * @return A File or URL created from the string representation.
     * @throws IOException if the URL cannot be created.
     */
    public boolean exists(final Class<?> caller, final String path) throws IOException {
        final Object uof = toFileOrURL(caller, path);
        if (uof instanceof File) {
            return ((File) uof).isFile();
        } else {
            return (uof != null);
        }
    }
}
