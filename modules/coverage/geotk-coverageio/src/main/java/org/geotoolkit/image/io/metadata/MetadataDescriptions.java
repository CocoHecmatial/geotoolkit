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
package org.geotoolkit.image.io.metadata;

import java.util.Map;
import java.util.Locale;


/**
 * The descriptions of attribute in a metadata element.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.05
 *
 * @since 3.05
 * @module
 */
final class MetadataDescriptions {
    /**
     * The last value returned by {@link SpatialMetadata#getDescriptions}, cached on the
     * assumption that the description of different attributes of the same element are
     * likely to be asked a few consecutive time.
     */
    final Map<String,String> descriptions;

    /**
     * The name of the element requested when we fetched the {@link #descriptions} map.
     */
    final String elementName;

    /**
     * The locale requested when we fetched the {@link #descriptions} map.
     * This is not necessarly the same than {@code DescriptionMap.getLocale()}.
     */
    final Locale locale;

    /**
     * Creates a new {@code MetadataDescriptions} for the given standard, element name
     * and locale.
     */
    MetadataDescriptions(final Map<String,String> descriptions, final String elementName, final Locale locale) {
        this.descriptions = descriptions;
        this.elementName  = elementName;
        this.locale       = locale;
    }
}
