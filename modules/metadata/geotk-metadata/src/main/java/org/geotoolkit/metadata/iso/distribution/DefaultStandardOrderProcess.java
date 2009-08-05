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

import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opengis.util.InternationalString;
import org.opengis.metadata.distribution.StandardOrderProcess;

import org.geotoolkit.metadata.iso.MetadataEntity;
import org.geotoolkit.internal.jaxb.uom.DateTimeAdapter;


/**
 * Common ways in which the resource may be obtained or received, and related instructions
 * and fee information.
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
    "fees",
    "plannedAvailableDateTime",
    "orderingInstructions",
    "turnaround"
})
@XmlRootElement(name = "MD_StandardOrderProcess")
public class DefaultStandardOrderProcess extends MetadataEntity implements StandardOrderProcess {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -6503378937452728631L;

    /**
     * Fees and terms for retrieving the resource.
     * Include monetary units (as specified in ISO 4217).
     */
    private InternationalString fees;

    /**
     * Date and time when the dataset will be available,
     * in milliseconds ellapsed since January 1st, 1970.
     */
    private long plannedAvailableDateTime = Long.MIN_VALUE;

    /**
     * General instructions, terms and services provided by the distributor.
     */
    private InternationalString orderingInstructions;

    /**
     * Typical turnaround time for the filling of an order.
     */
    private InternationalString turnaround;

    /**
     * Constructs an initially empty standard order process.
     */
    public DefaultStandardOrderProcess() {
    }

    /**
     * Constructs a metadata entity initialized with the values from the specified metadata.
     *
     * @param source The metadata to copy.
     *
     * @since 2.4
     */
    public DefaultStandardOrderProcess(final StandardOrderProcess source) {
        super(source);
    }

    /**
     * Returns fees and terms for retrieving the resource.
     * Include monetary units (as specified in ISO 4217).
     */
    @Override
    @XmlElement(name = "fees")
    public synchronized InternationalString getFees() {
        return fees;
    }

    /**
     * Sets fees and terms for retrieving the resource.
     * Include monetary units (as specified in ISO 4217).
     *
     * @param newValue The new fees.
     */
    public synchronized void setFees(final InternationalString newValue) {
        checkWritePermission();
        fees = newValue;
    }

    /**
     * Returns the date and time when the dataset will be available.
     */
    @Override
    @XmlJavaTypeAdapter(DateTimeAdapter.class)
    @XmlElement(name = "plannedAvailableDateTime")
    public synchronized Date getPlannedAvailableDateTime() {
        return (plannedAvailableDateTime!=Long.MIN_VALUE) ?
                new Date(plannedAvailableDateTime) : null;
    }

    /**
     * Sets the date and time when the dataset will be available.
     *
     * @param newValue The new planned available time.
     */
    public synchronized void setPlannedAvailableDateTime(final Date newValue) {
        checkWritePermission();
        plannedAvailableDateTime = (newValue!=null) ? newValue.getTime() : Long.MIN_VALUE;
    }

    /**
     * Returns general instructions, terms and services provided by the distributor.
     */
    @Override
    @XmlElement(name = "orderingInstructions")
    public synchronized InternationalString getOrderingInstructions() {
        return orderingInstructions;
    }

    /**
     * Sets general instructions, terms and services provided by the distributor.
     *
     * @param newValue The new ordering instructions.
     */
    public synchronized void setOrderingInstructions(final InternationalString newValue) {
        checkWritePermission();
        orderingInstructions = newValue;
    }

    /**
     * Returns typical turnaround time for the filling of an order.
     */
    @Override
    @XmlElement(name = "turnaround")
    public synchronized InternationalString getTurnaround() {
        return turnaround;
    }

    /**
     * Sets typical turnaround time for the filling of an order.
     *
     * @param newValue The new turnaround.
     */
    public synchronized void setTurnaround(final InternationalString newValue) {
        checkWritePermission();
        turnaround = newValue;
    }
}
