/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2005-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2010, Geomatys
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
package org.geotoolkit.openoffice;


/**
 * Information about a method to be exported as <A HREF="http://www.openoffice.org">OpenOffice</A>
 * add-in.
 *
 * @author Martin Desruisseaux (IRD)
 * @version 3.09
 *
 * @since 3.09 (derived from 2.2)
 * @module
 */
public final class MethodInfo {
    /** The category name. */
    final String category;

    /** The display name. */
    final String display;

    /** A description of the exported method. */
    final String description;

    /** Arguments names (even index) and descriptions (odd index). */
    final String[] arguments;

    /**
     * Constructs method informations.
     *
     * @param category    The category name.
     * @param display     The display name.
     * @param description A description of the exported method.
     * @param arguments   Arguments names (even index) and descriptions (odd index).
     */
    public MethodInfo(final String category,
                      final String display,
                      final String description,
                      final String[] arguments)
    {
        this.category    = category;
        this.display     = display;
        this.description = description;
        this.arguments   = arguments;
    }
}
