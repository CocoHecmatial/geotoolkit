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
package org.geotoolkit.internal.jaxb.code;

import javax.xml.bind.annotation.XmlElement;
import org.opengis.metadata.citation.Role;


/**
 * JAXB adapter for {@link Role}, in order to integrate the value in an element respecting
 * the ISO-19139 standard. See package documentation for more information about the handling
 * of {@code CodeList} in ISO-19139.
 *
 * @author Cédric Briançon (Geomatys)
 * @version 3.00
 *
 * @since 2.5
 * @module
 */
public final class RoleAdapter extends CodeListAdapter<RoleAdapter, Role> {
    /**
     * Ensures that the adapted code list class is loaded.
     */
    static {
        ensureClassLoaded(Role.class);
    }

    /**
     * Empty constructor for JAXB only.
     */
    public RoleAdapter() {
    }

    /**
     * Creates a new adapter for the given proxy.
     */
    private RoleAdapter(final CodeListProxy proxy) {
        super(proxy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RoleAdapter wrap(CodeListProxy proxy) {
        return new RoleAdapter(proxy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<Role> getCodeListClass() {
        return Role.class;
    }

    /**
     * Invoked by JAXB on marshalling.
     *
     * @return The value to be marshalled.
     */
    @XmlElement(name = "CI_RoleCode")
    public CodeListProxy getCodeListProxy() {
        return proxy;
    }

    /**
     * Invoked by JAXB on unmarshalling.
     *
     * @param proxy The unmarshalled value.
     */
    public void setCodeListProxy(final CodeListProxy proxy) {
        this.proxy = proxy;
    }
}
