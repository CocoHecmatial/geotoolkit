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
 */
package org.geotoolkit.internal.jaxb.referencing;

import java.util.Map;
import javax.xml.bind.annotation.XmlElement;

import javax.measure.unit.Unit;
import javax.measure.quantity.Length;

import org.opengis.referencing.datum.Ellipsoid;

import org.geotoolkit.referencing.datum.DefaultEllipsoid;
import org.geotoolkit.referencing.AbstractIdentifiedObject;
import org.geotoolkit.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter for {@link Ellipsoid}, in order to integrate the value in an element
 * complying with OGC/ISO standard.
 *
 * @author Cédric Briançon (Geomatys)
 * @version 3.05
 *
 * @since 3.05
 * @module
 */
public final class CD_Ellipsoid extends PropertyType<CD_Ellipsoid, Ellipsoid> {
    /**
     * Empty constructor for JAXB only.
     */
    public CD_Ellipsoid() {
    }

    /**
     * Wraps an ellipsoid value with a {@code gml:Ellipsoid} element at marshalling-time.
     *
     * @param metadata The metadata value to marshall.
     */
    private CD_Ellipsoid(final Ellipsoid metadata) {
        super(metadata);
    }

    /**
     * Returns the ellipsoid value wrapped by a {@code gml:Ellipsoid} element.
     *
     * @param  value The value to marshall.
     * @return The adapter which wraps the metadata value.
     */
    @Override
    protected CD_Ellipsoid wrap(final Ellipsoid value) {
        return new CD_Ellipsoid(value);
    }

    /**
     * Returns the {@link DefaultEllipsoid} generated from the metadata value.
     * This method is systematically called at marshalling-time by JAXB.
     *
     * @return The metadata to be marshalled.
     */
    @Override
    @XmlElement(name = "Ellipsoid")
    public DefaultEllipsoid getElement() {
        if (skip()) return null;
        final Ellipsoid metadata = this.metadata;
        if (metadata instanceof DefaultEllipsoid) {
            return (DefaultEllipsoid) metadata;
        }
        final Map<String,?> properties = AbstractIdentifiedObject.getProperties(metadata);
        final double semiMajor = metadata.getSemiMajorAxis();
        final Unit<Length> unit = metadata.getAxisUnit();
        return metadata.isIvfDefinitive() ?
            DefaultEllipsoid.createFlattenedSphere(properties, semiMajor, metadata.getInverseFlattening(), unit) :
            DefaultEllipsoid.createEllipsoid(properties, semiMajor, metadata.getSemiMinorAxis(), unit);
    }

    /**
     * Sets the value for the {@link DefaultEllipsoid}.
     * This method is systematically called at unmarshalling-time by JAXB.
     *
     * @param metadata The unmarshalled metadata.
     */
    public void setElement(final DefaultEllipsoid metadata) {
        this.metadata = metadata;
    }
}
