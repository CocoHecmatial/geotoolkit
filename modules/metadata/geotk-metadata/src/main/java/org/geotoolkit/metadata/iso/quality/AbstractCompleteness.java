/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2004-2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.metadata.iso.quality;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import net.jcip.annotations.ThreadSafe;

import org.opengis.metadata.quality.Completeness;


/**
 * Presence and absence of features, their attributes and their relationships.
 *
 * @author Martin Desruisseaux (IRD)
 * @author Touraïvane (IRD)
 * @version 3.04
 *
 * @since 2.1
 * @module
 */
@ThreadSafe
@XmlType(name = "AbstractDQ_Completeness_Type")
@XmlRootElement(name = "DQ_Completeness")
@XmlSeeAlso({
    DefaultCompletenessCommission.class,
    DefaultCompletenessOmission.class
})
public class AbstractCompleteness extends AbstractElement implements Completeness {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7893993264874215741L;

    /**
     * Constructs an initially empty completeness.
     */
    public AbstractCompleteness() {
    }

    /**
     * Constructs a metadata entity initialized with the values from the specified metadata.
     *
     * @param source The metadata to copy, or {@code null} if none.
     *
     * @since 2.4
     */
    public AbstractCompleteness(final Completeness source) {
        super(source);
    }
}
