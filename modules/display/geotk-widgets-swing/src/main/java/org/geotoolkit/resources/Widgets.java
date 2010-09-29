/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2010, Geomatys
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
package org.geotoolkit.resources;

import java.util.Locale;
import java.util.MissingResourceException;


/**
 * Locale-dependent resources for widgets messages.
 *
 * @author Cédric Briançon (Geomatys)
 * @version 3.03
 *
 * @since 3.03
 * @module
 */
public final class Widgets extends IndexedResourceBundle {
    /**
     * Resource keys. This class is used when compiling sources, but no dependencies to
     * {@code Keys} should appear in any resulting class files. Since the Java compiler
     * inlines final integer values, using long identifiers will not bloat the constant
     * pools of compiled classes.
     *
     * @author Cédric Briançon (Geomatys)
     * @version 3.03
     *
     * @since 3.03
     */
    public static final class Keys {
        private Keys() {
        }

        /**
         * Add all
         */
        public static final int ADD_ALL = 0;

        /**
         * Add selected elements
         */
        public static final int ADD_SELECTED_ELEMENTS = 1;

        /**
         * Confirm data addition
         */
        public static final int CONFIRM_ADD_DATA = 10;

        /**
         * Confirm delete
         */
        public static final int CONFIRM_DELETE = 6;

        /**
         * <html>Are you sure you want to delete "<cite>{0}</cite>"?< <strong>This action will remove
         * all references to raster data in that layer.</strong> However, the raster files will not be
         * deleted.</html>
         */
        public static final int CONFIRM_DELETE_LAYER_$1 = 7;

        /**
         * A {0,choice,0#horizontal|1#vertical} Coordinate Reference System must be specified.
         */
        public static final int CRS_REQUIRED_$1 = 16;

        /**
         * Domain of entries to list
         */
        public static final int DOMAIN_OF_ENTRIES = 8;

        /**
         * You can restrict the amount of images to be listed by specifying a smaller geographic area
         * or time range, or a larger resolution. Leave the values unchanged for listing every images
         * available in the layer.
         */
        public static final int EXPLAIN_DOMAIN_OF_ENTRIES = 11;

        /**
         * Incomplete form
         */
        public static final int INCOMPLETE_FORM = 17;

        /**
         * Elements of layer {0}
         */
        public static final int LAYER_ELEMENTS_$1 = 9;

        /**
         * New format (editable).
         */
        public static final int NEW_FORMAT = 13;

        /**
         * Raster sample values are geophysics.
         */
        public static final int RASTER_IS_GEOPHYSICS = 15;

        /**
         * Remove all
         */
        public static final int REMOVE_ALL = 2;

        /**
         * Remove selected elements
         */
        public static final int REMOVE_SELECTED_ELEMENTS = 3;

        /**
         * Rename this format if sample dimensions need to be edited.
         */
        public static final int RENAME_FORMAT_FOR_EDIT = 14;

        /**
         * Select a directory
         */
        public static final int SELECT_DIRECTORY = 4;

        /**
         * Select a file
         */
        public static final int SELECT_FILE = 12;

        /**
         * Select variables
         */
        public static final int SELECT_VARIABLES = 18;

        /**
         * <html><i>from</i> {0}<br><i>to</i> {1}</html>
         */
        public static final int TIME_RANGE_$2 = 5;
    }

    /**
     * Constructs a new resource bundle loading data from the given UTF file.
     *
     * @param filename The file or the JAR entry containing resources.
     */
    Widgets(final String filename) {
        super(filename);
    }

    /**
     * Returns resources in the given locale.
     *
     * @param  locale The locale, or {@code null} for the default locale.
     * @return Resources in the given locale.
     * @throws MissingResourceException if resources can't be found.
     */
    public static Widgets getResources(Locale locale) throws MissingResourceException {
        return getBundle(Widgets.class, locale);
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its parents.
     *
     * @param  key The key for the desired string.
     * @return The string for the given key.
     * @throws MissingResourceException If no object for the given key can be found.
     */
    public static String format(final int key) throws MissingResourceException {
        return getResources(null).getString(key);
    }
}
