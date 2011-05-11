/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2011, Geomatys
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
package org.geotoolkit.internal.jaxb;

import java.util.AbstractMap;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;


/**
 * An entry in the {@link IdentifierMap}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.18
 *
 * @since 3.18
 * @module
 */
final class IdentifierEntry extends AbstractMap.SimpleEntry<Citation,String> implements Identifier {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5484541090753985572L;

    /**
     * Creates a new entry for the given authority and code.
     */
    IdentifierEntry(final Citation authority, final String code) {
        super(authority, code);
    }

    /**
     * Returns the identifier namespace, which is the key of this entry.
     */
    @Override
    public Citation getAuthority() {
        return getKey();
    }

    /**
     * Returns the identifier code, which is the value of this entry.
     */
    @Override
    public String getCode() {
        return getValue();
    }
}
