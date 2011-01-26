/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008-2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2011, Geomatys
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
import org.opengis.metadata.lineage.Lineage;
import org.geotoolkit.metadata.iso.lineage.DefaultLineage;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author Cédric Briançon (Geomatys)
 * @version 3.05
 *
 * @since 2.5
 * @module
 */
public final class LI_Lineage extends MetadataAdapter<LI_Lineage, Lineage> {
    /**
     * Empty constructor for JAXB only.
     */
    public LI_Lineage() {
    }

    /**
     * Wraps an Lineage value with a {@code LI_Lineage} element at marshalling time.
     *
     * @param metadata The metadata value to marshall.
     */
    private LI_Lineage(final Lineage metadata) {
        super(metadata);
    }

    /**
     * Returns the Lineage value wrapped by a {@code LI_Lineage} element.
     *
     * @param value The value to marshall.
     * @return The adapter which wraps the metadata value.
     */
    @Override
    protected LI_Lineage wrap(final Lineage value) {
        return new LI_Lineage(value);
    }

    /**
     * Returns the {@link DefaultLineage} generated from the metadata value.
     * This method is systematically called at marshalling time by JAXB.
     *
     * @return The metadata to be marshalled.
     */
    @Override
    @XmlElement(name = "LI_Lineage")
    public DefaultLineage getElement() {
        final Lineage metadata = this.metadata;
        return (metadata instanceof DefaultLineage) ?
            (DefaultLineage) metadata : new DefaultLineage(metadata);
    }

    /**
     * Sets the value for the {@link DefaultLineage}. This method is systematically
     * called at unmarshalling time by JAXB.
     *
     * @param metadata The unmarshalled metadata.
     */
    public void setElement(final DefaultLineage metadata) {
        this.metadata = metadata;
    }
}
