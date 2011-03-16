/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2011, Open Source Geospatial Foundation (OSGeo)
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
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package org.geotoolkit.metadata.iso.lineage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.lineage.Algorithm;
import org.opengis.util.InternationalString;

import org.geotoolkit.lang.ThreadSafe;
import org.geotoolkit.metadata.iso.MetadataEntity;
import org.geotoolkit.xml.Namespaces;


/**
 * Details of the methodology by which geographic information was derived from the instrument
 * readings.
 *
 * @author Cédric Briançon (Geomatys)
 * @author Guilhem Legal (Geomatys)
 * @version 3.17
 *
 * @since 3.03
 * @module
 */
@ThreadSafe
@XmlType(name = "LE_Algorithm_Type", propOrder={
    "citation",
    "description"
})
@XmlRootElement(name = "LE_Algorithm", namespace = Namespaces.GMI)
public class DefaultAlgorithm extends MetadataEntity implements Algorithm {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6343760610092069341L;

    /**
     * Information identifying the algorithm and version or date.
     */
    private Citation citation;

    /**
     * Information describing the algorithm used to generate the data.
     */
    private InternationalString description;

    /**
     * Constructs an initially empty algorithm.
     */
    public DefaultAlgorithm() {
    }

    /**
     * Constructs a metadata entity initialized with the values from the specified metadata.
     *
     * @param source The metadata to copy, or {@code null} if none.
     */
    public DefaultAlgorithm(final Algorithm source) {
        super(source);
    }

    /**
     * Returns the information identifying the algorithm and version or date.
     */
    @Override
    @XmlElement(name = "citation", namespace = Namespaces.GMI, required = true)
    public synchronized Citation getCitation() {
        return citation;
    }

    /**
     * Sets the information identifying the algorithm and version or date.
     *
     * @param newValue The new citation value.
     */
    public synchronized void setCitation(final Citation newValue) {
        checkWritePermission();
        citation = newValue;
    }

    /**
     * Returns the information describing the algorithm used to generate the data.
     */
    @Override
    @XmlElement(name = "description", namespace = Namespaces.GMI, required = true)
    public synchronized InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the information describing the algorithm used to generate the data.
     *
     * @param newValue The new description value.
     */
    public synchronized void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }
}
