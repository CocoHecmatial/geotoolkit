/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010-2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2010-2011, Geomatys
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
package org.geotoolkit.internal.jaxb.gmi;

import java.util.Collection;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.Metadata;
import org.geotoolkit.metadata.iso.DefaultMetadata;


/**
 * A wrapper for a metadata using the {@code "gmi"} namespace.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.17
 *
 * @since 3.17
 * @module
 */
@XmlType(name = "MI_Metadata_Type")
@XmlRootElement(name = "MI_Metadata")
public class MI_Metadata extends DefaultMetadata {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5472890635961139724L;

    /**
     * Creates an initially empty metadata.
     * This is also the default constructor used by JAXB.
     */
    public MI_Metadata() {
    }

    /**
     * Creates a new metadata as a copy of the given one.
     * This is a shallow copy constructor.
     *
     * @param original The original metadata to copy.
     */
    public MI_Metadata(final Metadata original) {
        super(original);
    }

    /**
     * Returns {@code true} if the given collection is null or empty.
     */
    static boolean isEmpty(final Collection<?> collection) {
        return (collection == null) || collection.isEmpty();
    }

    /**
     * Wraps the given metadata into a Geotk implementation that can be marshalled,
     * using the {@code "gmi"} namespace if necessary.
     *
     * @param  original The original metadata provided by the user.
     * @return The metadata to marshall.
     */
    public static DefaultMetadata wrap(final Metadata original) {
        if (original != null && !(original instanceof MI_Metadata)) {
            if (!isEmpty(original.getAcquisitionInformation())) {
                return new MI_Metadata(original);
            }
            if (!(original instanceof DefaultMetadata)) {
                return new DefaultMetadata(original);
            }
        }
        return (DefaultMetadata) original;
    }
}
