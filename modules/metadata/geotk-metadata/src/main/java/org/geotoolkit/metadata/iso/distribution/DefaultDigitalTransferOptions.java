/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2004-2009, Open Source Geospatial Foundation (OSGeo)
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
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package org.geotoolkit.metadata.iso.distribution;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.OnLineResource;
import org.opengis.metadata.distribution.DigitalTransferOptions;
import org.opengis.metadata.distribution.Medium;
import org.geotoolkit.metadata.iso.MetadataEntity;


/**
 * Technical means and media by which a resource is obtained from the distributor.
 *
 * @author Martin Desruisseaux (IRD)
 * @author Touraïvane (IRD)
 * @author Cédric Briançon (Geomatys)
 * @version 3.03
 *
 * @since 2.1
 * @module
 */
@XmlType(propOrder={
    "unitsOfDistribution",
    "transferSize",
    "onLines",
    "offLine"
})
@XmlRootElement(name = "MD_DigitalTransferOptions")
public class DefaultDigitalTransferOptions extends MetadataEntity implements DigitalTransferOptions {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -1533064478468754337L;

    /**
     * Tiles, layers, geographic areas, etc., in which data is available.
     */
    private InternationalString unitsOfDistribution;

    /**
     * Estimated size of a unit in the specified transfer format, expressed in megabytes.
     * The transfer size is &gt; 0.0.
     * Returns {@code null} if the transfer size is unknown.
     */
    private Double transferSize;

    /**
     * Information about online sources from which the resource can be obtained.
     */
    private Collection<OnLineResource> onLines;

    /**
     * Information about offline media on which the resource can be obtained.
     */
    private Medium offLines;

    /**
     * Constructs an initially empty digital transfer options.
     */
    public DefaultDigitalTransferOptions() {
    }

    /**
     * Constructs a metadata entity initialized with the values from the specified metadata.
     *
     * @param source The metadata to copy.
     *
     * @since 2.4
     */
    public DefaultDigitalTransferOptions(final DigitalTransferOptions source) {
        super(source);
    }

    /**
     * Returne tiles, layers, geographic areas, etc., in which data is available.
     */
    @Override
    @XmlElement(name = "unitsOfDistribution")
    public InternationalString getUnitsOfDistribution() {
        return unitsOfDistribution;
    }

    /**
     * Sets tiles, layers, geographic areas, etc., in which data is available.
     *
     * @param newValue The new units of distribution.
     */
    public synchronized void setUnitsOfDistribution(final InternationalString newValue) {
        checkWritePermission();
        unitsOfDistribution = newValue;
    }

    /**
     * Returns an estimated size of a unit in the specified transfer format, expressed in megabytes.
     * The transfer size is &gt; 0.0. Returns {@code null} if the transfer size is unknown.
     */
    @Override
    @XmlElement(name = "transferSize")
    public Double getTransferSize() {
        return transferSize;
    }

    /**
     * Sets an estimated size of a unit in the specified transfer format, expressed in megabytes.
     * The transfer size is &gt; 0.0.
     *
     * @param newValue The new transfer size.
     */
    public synchronized void setTransferSize(final Double newValue) {
        checkWritePermission();
        transferSize = newValue;
    }

    /**
     * Returns information about online sources from which the resource can be obtained.
     */
    @Override
    @XmlElement(name = "onLine")
    public synchronized Collection<OnLineResource> getOnLines() {
        return xmlOptional(onLines = nonNullCollection(onLines, OnLineResource.class));
    }

    /**
     * Sets information about online sources from which the resource can be obtained.
     *
     * @param newValues The new online sources.
     */
    public synchronized void setOnLines(final Collection<? extends OnLineResource> newValues) {
        onLines = copyCollection(newValues, onLines, OnLineResource.class);
    }

    /**
     * Returns information about offline media on which the resource can be obtained.
     */
    @Override
    @XmlElement(name = "offLine")
    public Medium getOffLine() {
        return offLines;
    }

    /**
     * Sets information about offline media on which the resource can be obtained.
     *
     * @param newValue The new offline media.
     */
    public synchronized void setOffLine(final Medium newValue) {
        checkWritePermission();
        offLines = newValue;
    }
}
