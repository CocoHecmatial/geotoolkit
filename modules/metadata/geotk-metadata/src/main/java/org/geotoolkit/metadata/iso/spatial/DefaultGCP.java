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
package org.geotoolkit.metadata.iso.spatial;

import java.util.Collection;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.spatial.GCP;

import org.geotoolkit.lang.ThreadSafe;
import org.geotoolkit.metadata.iso.MetadataEntity;
import org.geotoolkit.xml.Namespaces;


/**
 * Information on ground control point.
 *
 * @author Cédric Briançon (Geomatys)
 * @version 3.07
 *
 * @since 3.03
 * @module
 */
@ThreadSafe
@XmlType(name = "MI_GCP_Type", propOrder={
    //"geographicCoordinates",
    "accuracyReports"
})
@XmlRootElement(name = "MI_GCP", namespace = Namespaces.GMI)
public class DefaultGCP extends MetadataEntity implements GCP {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5517470507848931237L;

    /**
     * Geographic or map position of the control point, in either two or three dimensions.
     */
    private DirectPosition geographicCoordinates;

    /**
     * Accuracy of a ground control point.
     */
    private Collection<Element> accuracyReports;

    /**
     * Constructs an initially empty ground control point.
     */
    public DefaultGCP() {
    }

    /**
     * Constructs a metadata entity initialized with the values from the specified metadata.
     *
     * @param source The metadata to copy, or {@code null} if none.
     */
    public DefaultGCP(final GCP source) {
        super(source);
    }

    /**
     * Returns the geographic or map position of the control point, in either two or three
     * dimensions.
     *
     * @todo finish the annotation on the referencing module before
     */
    @Override
    //@XmlElement(name = "geographicCoordinates")
    public synchronized DirectPosition getGeographicCoordinates() {
        return geographicCoordinates;
    }

    /**
     * Sets the geographic or map position of the control point, in either two or three dimensions.
     *
     * @param newValue The new geographic coordinates values.
     */
    public synchronized void setGeographicCoordinates(final DirectPosition newValue) {
        checkWritePermission();
        geographicCoordinates = newValue;
    }

    /**
     * Get the accuracy of a ground control point.
     */
    @Override
    @XmlElement(name = "accuracyReport", namespace = Namespaces.GMI)
    public synchronized Collection<Element> getAccuracyReports() {
        return xmlOptional(accuracyReports = nonNullCollection(accuracyReports, Element.class));
    }

    /**
     * Sets the accuracy of a ground control point.
     *
     * @param newValues The new accuracy report values.
     */
    public synchronized void setAccuracyReports(final Collection<? extends Element> newValues) {
        accuracyReports = copyCollection(newValues, accuracyReports, Element.class);
    }
}
