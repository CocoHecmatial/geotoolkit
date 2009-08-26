/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.opengis.util.InternationalString;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.distribution.Distributor;

import org.geotoolkit.lang.ThreadSafe;
import org.geotoolkit.metadata.iso.MetadataEntity;


/**
 * Description of the computer language construct that specifies the representation
 * of data objects in a record, file, message, storage device or transmission channel.
 *
 * @author Martin Desruisseaux (IRD)
 * @author Touraïvane (IRD)
 * @author Cédric Briançon (Geomatys)
 * @version 3.03
 *
 * @since 2.1
 * @module
 */
@ThreadSafe
@XmlType(propOrder={
    "name",
    "version",
    "amendmentNumber",
    "specification",
    "fileDecompressionTechnique",
    "formatDistributors"
})
@XmlRootElement(name = "MD_Format")
public class DefaultFormat extends MetadataEntity implements Format {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 6498897239493553607L;

    /**
     * Name of the data transfer format(s).
     */
    private InternationalString name;

    /**
     * Version of the format (date, number, etc.).
     */
    private InternationalString version;

    /**
     * Amendment number of the format version.
     */
    private InternationalString amendmentNumber;

    /**
     * Name of a subset, profile, or product specification of the format.
     */
    private InternationalString specification;

    /**
     * Recommendations of algorithms or processes that can be applied to read or
     * expand resources to which compression techniques have been applied.
     */
    private InternationalString fileDecompressionTechnique;

    /**
     * Provides information about the distributors format.
     */
    private Collection<Distributor> formatDistributors;

    /**
     * Constructs an initially empty format.
     */
    public DefaultFormat() {
    }

    /**
     * Constructs a metadata entity initialized with the values from the specified metadata.
     *
     * @param source The metadata to copy.
     *
     * @since 2.4
     */
    public DefaultFormat(final Format source) {
        super(source);
    }

    /**
     * Creates a format initialized to the given name.
     *
     * @param name    The name of the data transfer format(s).
     * @param version The version of the format (date, number, etc.).
     */
    public DefaultFormat(final InternationalString name, final InternationalString version) {
        setName   (name   );
        setVersion(version);
    }

    /**
     * Returns the name of the data transfer format(s).
     */
    @Override
    @XmlElement(name = "name", required = true)
    public synchronized InternationalString getName() {
        return name;
    }

    /**
     * Sets the name of the data transfer format(s).
     *
     * @param newValue The new name.
     */
    public synchronized void setName(final InternationalString newValue) {
         checkWritePermission();
         name = newValue;
     }

    /**
     * Returne the version of the format (date, number, etc.).
     */
    @Override
    @XmlElement(name = "version", required = true)
    public synchronized InternationalString getVersion() {
        return version;
    }

    /**
     * Sets the version of the format (date, number, etc.).
     *
     * @param newValue The new version.
     */
    public synchronized void setVersion(final InternationalString newValue) {
        checkWritePermission();
        version = newValue;
    }

    /**
     * Returns the amendment number of the format version.
     */
    @Override
    @XmlElement(name = "amendmentNumber")
    public synchronized InternationalString getAmendmentNumber() {
        return amendmentNumber;
    }

    /**
     * Sets the amendment number of the format version.
     *
     * @param newValue The new amendment number.
     */
    public synchronized void setAmendmentNumber(final InternationalString newValue) {
        checkWritePermission();
        amendmentNumber = newValue;
    }

    /**
     * Returns the name of a subset, profile, or product specification of the format.
     */
    @Override
    @XmlElement(name = "specification")
    public synchronized InternationalString getSpecification() {
        return specification;
    }

    /**
     * Sets the name of a subset, profile, or product specification of the format.
     *
     * @param newValue The new specification.
     */
    public synchronized void setSpecification(final InternationalString newValue) {
        checkWritePermission();
        specification = newValue;
    }

    /**
     * Returns recommendations of algorithms or processes that can be applied to read or
     * expand resources to which compression techniques have been applied.
     */
    @Override
    @XmlElement(name = "fileDecompressionTechnique")
    public synchronized InternationalString getFileDecompressionTechnique() {
        return fileDecompressionTechnique;
    }

    /**
     * Sets recommendations of algorithms or processes that can be applied to read or
     * expand resources to which compression techniques have been applied.
     *
     * @param newValue The new file decompression technique.
     */
    public synchronized void setFileDecompressionTechnique(final InternationalString newValue) {
        checkWritePermission();
        fileDecompressionTechnique = newValue;
    }

    /**
     * Provides information about the distributors format.
     */
    @Override
    @XmlElement(name = "FormatDistributor")
    public synchronized Collection<Distributor> getFormatDistributors() {
        return xmlOptional(formatDistributors = nonNullCollection(formatDistributors, Distributor.class));
    }

    /**
     * Sets information about the distributors format.
     *
     * @param newValues The new format distributors.
     */
    public synchronized void setFormatDistributors(
            final Collection<? extends Distributor> newValues)
    {
        formatDistributors = copyCollection(newValues, formatDistributors, Distributor.class);
    }
}
