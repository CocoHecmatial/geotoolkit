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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;

import org.geotoolkit.xml.XLink;
import org.geotoolkit.xml.IdentifierMap;
import org.geotoolkit.xml.IdentifierSpace;
import org.geotoolkit.util.logging.Logging;
import org.geotoolkit.util.Utilities;


/**
 * Wraps a {@link XLink}, {@link UUID} or other objects as an identifier
 * in the {@link IdentifierMap}.
 *
 * @param  <T> The value type, typically {@link XLink}, {@link UUID} or {@link String}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.19
 *
 * @since 3.19
 * @module
 */
public final class IdentifierAdapter<T> implements Identifier {
    /**
     * The authority.
     */
    final IdentifierSpace<T> authority;

    /**
     * The identifier value.
     */
    T value;

    /**
     * Creates a new adapter for the given authority and identifier value.
     *
     * @param authority The identifier authority.
     * @param value The identifier value.
     */
    public IdentifierAdapter(final IdentifierSpace<T> authority, final T value) {
        this.authority = authority;
        this.value = value;
    }

    /**
     * Creates an identifier from a text value.
     */
    static Identifier create(final Citation authority, final String code) {
        if (authority instanceof IdentifierAuthority) {
            switch (((IdentifierAuthority) authority).ordinal) {
                case IdentifierAuthority.ID: {
                    return new IdentifierAdapter<String>(IdentifierSpace.ID, code);
                }
                case IdentifierAuthority.UUID: {
                    try {
                        return new IdentifierAdapter<UUID>(IdentifierSpace.UUID, UUID.fromString(code));
                    } catch (IllegalArgumentException e) {
                        parseFailure(e);
                        break;
                    }
                }
                case IdentifierAuthority.HREF:
                case IdentifierAuthority.XLINK: {
                    final URI uri;
                    try {
                        uri = new URI(code);
                    } catch (URISyntaxException e) {
                        parseFailure(e);
                        break;
                    }
                    if (authority == IdentifierSpace.HREF) {
                        // TODO: Actually, should be stored as XLink using code below.
                        return new IdentifierAdapter<URI>(IdentifierSpace.HREF, uri);
                    }
                    final XLink xlink = new XLink();
                    xlink.setType(XLink.Type.SIMPLE);
                    xlink.setHRef(uri);
                    return new IdentifierAdapter<XLink>(IdentifierSpace.XLINK, xlink);
                }
            }
        }
        return new IdentifierEntry(authority, code);
    }

    /**
     * Invoked when a string can not be parsed in the identifier type to be stored in the map.
     */
    private static void parseFailure(final Exception e) {
        Logging.recoverableException(IdentifierMap.class, "putSpecialized", e);
    }

    /**
     * Returns the authority specified at construction time.
     */
    @Override
    public Citation getAuthority() {
        return authority;
    }

    /**
     * Returns a string representation of the identifier given at construction time.
     */
    @Override
    public String getCode() {
        return value.toString();
    }

    /**
     * Returns a hash code value for this identifier.
     */
    @Override
    public int hashCode() {
        return Utilities.hash(value, authority.hashCode());
    }

    /**
     * Compares this identifier with the given object for equality.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof IdentifierAdapter<?>) {
            final IdentifierAdapter<?> that = (IdentifierAdapter<?>) other;
            return Utilities.equals(authority, that.authority) &&
                   Utilities.equals(value, that.value);
        }
        return false;
    }

    /**
     * Returns a string representation of this identifier.
     */
    @Override
    public String toString() {
        final String code = String.valueOf(value);
        final boolean quote = (value != null) && (code instanceof CharSequence || code.indexOf('[') >= 1);
        final StringBuilder buffer = new StringBuilder("Identifier[\"").append(authority).append("\", ");
        if (quote) buffer.append('"');
        buffer.append(code);
        if (quote) buffer.append('"');
        return buffer.append(']').toString();
    }
}
