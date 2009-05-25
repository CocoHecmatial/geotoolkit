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
package org.geotoolkit.metadata.iso.citation;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opengis.metadata.citation.Series;
import org.opengis.util.InternationalString;
import org.geotoolkit.metadata.iso.MetadataEntity;
import org.geotoolkit.util.SimpleInternationalString;


/**
 * Information about the series, or aggregate dataset, to which a dataset belongs.
 *
 * @author Jody Garnett (Refractions)
 * @author Martin Desruisseaux (IRD)
 * @author Cédric Briançon (Geomatys)
 * @version 3.00
 *
 * @since 2.1
 * @module
 */
@XmlType(propOrder={
    "name",
    "issueIdentification",
    "page"
})
@XmlRootElement(name = "CI_Series")
public class DefaultSeries extends MetadataEntity implements Series {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 2784101441023323052L;

    /**
     * Name of the series, or aggregate dataset, of which the dataset is a part.
     */
    private InternationalString name;

    /**
     * Information identifying the issue of the series.
     */
    private String issueIdentification;

    /**
     * Details on which pages of the publication the article was published.
     */
    private String page;

    /**
     * Constructs a default series.
     */
    public DefaultSeries() {
    }

    /**
     * Constructs a metadata entity initialized with the values from the specified metadata.
     *
     * @param source The metadata to copy.
     *
     * @since 2.4
     */
    public DefaultSeries(final Series source) {
        super(source);
    }

    /**
     * Constructs a series with the specified name.
     *
     * @param name The name of the series, or aggregate dataset, of which the dataset is a part.
     */
    public DefaultSeries(final CharSequence name) {
        final InternationalString n;
        if (name instanceof InternationalString) {
            n = (InternationalString) name;
        } else {
            n = new SimpleInternationalString(name.toString());
        }
        setName(n);
    }

    /**
     * Returne the name of the series, or aggregate dataset, of which the dataset is a part.
     */
    @Override
    @XmlElement(name = "name")
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the name of the series, or aggregate dataset, of which the dataset is a part.
     *
     * @param newValue The new name.
     */
    public synchronized void setName(final InternationalString newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Returns information identifying the issue of the series.
     */
    @Override
    @XmlElement(name = "issueIdentification")
    public String getIssueIdentification() {
        return issueIdentification;
    }

    /**
     * Sets information identifying the issue of the series.
     *
     * @param newValue The new issue identification.
     */
    public synchronized void setIssueIdentification(final String newValue) {
        checkWritePermission();
        issueIdentification = newValue;
    }

    /**
     * Returns details on which pages of the publication the article was published.
     */
    @Override
    @XmlElement(name = "page")
    public String getPage() {
        return page;
    }

    /**
     * Sets details on which pages of the publication the article was published.
     *
     * @param newValue The new page.
     */
    public synchronized void setPage(final String newValue) {
        checkWritePermission();
        page = newValue;
    }
}
