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
package org.geotoolkit.metadata.iso.spatial;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.DimensionNameType;
import org.geotoolkit.metadata.iso.MetadataEntity;
import org.geotoolkit.internal.jaxb.uom.MeasureAdapter;

import org.geotoolkit.lang.ThreadSafe;
import org.geotoolkit.lang.ValueRange;


/**
 * Axis properties.
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
    "dimensionName", "dimensionSize", "resolution"
})
@XmlRootElement(name = "MD_Dimension")
public class DefaultDimension extends MetadataEntity implements Dimension {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -2572515000574007266L;

    /**
     * Name of the axis.
     */
    private DimensionNameType dimensionName;

    /**
     * Number of elements along the axis.
     */
    private Integer dimensionSize;

    /**
     * Degree of detail in the grid dataset.
     */
    private Double resolution;

    /**
     * Constructs an initially empty dimension.
     */
    public DefaultDimension() {
    }

    /**
     * Constructs a metadata entity initialized with the values from the specified metadata.
     *
     * @param source The metadata to copy.
     *
     * @since 2.4
     */
    public DefaultDimension(final Dimension source) {
        super(source);
    }

    /**
     * Creates a dimension initialized to the given type.
     *
     * @param dimensionName The name of the axis.
     * @param dimensionSize The number of elements along the axis.
     */
    public DefaultDimension(final DimensionNameType dimensionName, final int dimensionSize) {
        setDimensionName(dimensionName);
        setDimensionSize(dimensionSize);
    }

    /**
     * Returns the name of the axis.
     */
    @Override
    @XmlElement(name = "dimensionName", required = true)
    public synchronized DimensionNameType getDimensionName() {
        return dimensionName;
    }

    /**
     * Sets the name of the axis.
     *
     * @param newValue The new dimension name.
     */
    public synchronized void setDimensionName(final DimensionNameType newValue) {
        checkWritePermission();
        dimensionName = newValue;
    }

    /**
     * Returns the number of elements along the axis.
     */
    @Override
    @ValueRange(minimum=0)
    @XmlElement(name = "dimensionSize", required = true)
    public synchronized Integer getDimensionSize() {
        return dimensionSize;
    }

    /**
     * Sets the number of elements along the axis.
     *
     * @param newValue The new dimension size.
     */
    public synchronized void setDimensionSize(final Integer newValue) {
        checkWritePermission();
        dimensionSize = newValue;
    }

    /**
     * Returns the degree of detail in the grid dataset.
     */
    @Override
    @ValueRange(minimum=0, isMinIncluded=false)
    @XmlJavaTypeAdapter(MeasureAdapter.class)
    @XmlElement(name = "resolution")
    public synchronized Double getResolution() {
        return resolution;
    }

    /**
     * Sets the degree of detail in the grid dataset.
     *
     * @param newValue The new resolution.
     */
    public synchronized void setResolution(final Double newValue) {
        checkWritePermission();
        resolution = newValue;
    }
}
