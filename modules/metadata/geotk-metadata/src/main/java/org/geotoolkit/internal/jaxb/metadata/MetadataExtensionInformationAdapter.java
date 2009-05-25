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
import org.opengis.metadata.MetadataExtensionInformation;
import org.geotoolkit.metadata.iso.DefaultMetadataExtensionInformation;


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
public final class MetadataExtensionInformationAdapter
        extends MetadataAdapter<MetadataExtensionInformationAdapter,MetadataExtensionInformation>
{
    /**
     * Empty constructor for JAXB only.
     */
    public MetadataExtensionInformationAdapter() {
    }

    /**
     * Wraps an MetadataExtensionInformation value with a {@code MD_MetadataExtensionInformation}
     * element at marshalling time.
     *
     * @param metadata The metadata value to marshall.
     */
    private MetadataExtensionInformationAdapter(final MetadataExtensionInformation metadata) {
        super(metadata);
    }

    /**
     * Returns the MetadataExtensionInformation value wrapped by a
     * {@code MD_MetadataExtensionInformation} element.
     *
     * @param value The value to marshall.
     * @return The adapter which wraps the metadata value.
     */
    @Override
    protected MetadataExtensionInformationAdapter wrap(final MetadataExtensionInformation value) {
        return new MetadataExtensionInformationAdapter(value);
    }

    /**
     * Returns the {@link DefaultMetadataExtensionInformation} generated from the metadata value.
     * This method is systematically called at marshalling time by JAXB.
     *
     * @return The metadata to be marshalled.
     */
    @XmlElement(name = "MD_MetadataExtensionInformation")
    public DefaultMetadataExtensionInformation getMetadataExtensionInformation() {
        final MetadataExtensionInformation metadata = this.metadata;
        return (metadata instanceof DefaultMetadataExtensionInformation) ?
            (DefaultMetadataExtensionInformation) metadata :
            new DefaultMetadataExtensionInformation(metadata);
    }

    /**
     * Sets the value for the {@link DefaultMetadataExtensionInformation}. This method is systematically
     * called at unmarshalling time by JAXB.
     *
     * @param metadata The unmarshalled metadata.
     */
    public void setMetadataExtensionInformation(final DefaultMetadataExtensionInformation metadata) {
        this.metadata = metadata;
    }
}
