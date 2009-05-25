/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
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
package org.geotoolkit.internal.jaxb.metadata;

import javax.xml.bind.annotation.XmlElement;
import org.opengis.metadata.citation.ResponsibleParty;
import org.geotoolkit.metadata.iso.citation.DefaultResponsibleParty;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author Cédric Briançon (Geomatys)
 * @version 3.00
 *
 * @since 2.5
 * @module
 */
public final class ResponsiblePartyAdapter
        extends MetadataAdapter<ResponsiblePartyAdapter,ResponsibleParty>
{
    /**
     * Empty constructor for JAXB only.
     */
    public ResponsiblePartyAdapter() {
    }

    /**
     * Wraps an ResponsibleParty value with a {@code CI_ResponsibleParty} element at marshalling time.
     *
     * @param metadata The metadata value to marshall.
     */
    private ResponsiblePartyAdapter(final ResponsibleParty metadata) {
        super(metadata);
    }

    /**
     * Returns the ResponsibleParty value wrapped by a {@code CI_ResponsibleParty} element.
     *
     * @param value The value to marshall.
     * @return The adapter which wraps the metadata value.
     */
    @Override
    protected ResponsiblePartyAdapter wrap(final ResponsibleParty value) {
        return new ResponsiblePartyAdapter(value);
    }

    /**
     * Returns the {@link DefaultResponsibleParty} generated from the metadata value.
     * This method is systematically called at marshalling time by JAXB.
     *
     * @return The metadata to be marshalled.
     */
    @XmlElement(name = "CI_ResponsibleParty")
    public DefaultResponsibleParty getResponsibleParty() {
        final ResponsibleParty metadata = this.metadata;
        return (metadata instanceof DefaultResponsibleParty) ?
            (DefaultResponsibleParty) metadata : new DefaultResponsibleParty(metadata);
    }

    /**
     * Sets the value for the {@link DefaultResponsibleParty}. This method
     * is systematically called at unmarshalling time by JAXB.
     *
     * @param metadata The unmarshalled metadata.
     */
    public void setResponsibleParty(final DefaultResponsibleParty metadata) {
        this.metadata = metadata;
    }
}
