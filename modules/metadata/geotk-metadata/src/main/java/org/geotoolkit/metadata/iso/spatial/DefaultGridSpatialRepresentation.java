/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2004-2009, Open Source Geospatial Foundation (OSGeo)
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

import java.util.List;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opengis.metadata.spatial.Dimension;
import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.spatial.GridSpatialRepresentation;


/**
 * Basic information required to uniquely identify a resource or resources.
 *
 * @author Martin Desruisseaux (IRD)
 * @author Touraïvane (IRD)
 * @author Cédric Briançon (Geomatys)
 * @version 3.0
 *
 * @since 2.1
 * @module
 */
@XmlType(name = "MD_GridSpatialRepresentation", propOrder={
    "numberOfDimensions",
    "axisDimensionsProperties",
    "cellGeometry",
    "transformationParameterAvailable"
})
@XmlSeeAlso({DefaultGeorectified.class, DefaultGeoreferenceable.class})
@XmlRootElement(name = "MD_GridSpatialRepresentation")
public class DefaultGridSpatialRepresentation extends AbstractSpatialRepresentation
        implements GridSpatialRepresentation
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -8400572307442433979L;

    /**
     * Number of independent spatial-temporal axes.
     */
    private Integer numberOfDimensions;

    /**
     * Information about spatial-temporal axis properties.
     */
    private List<Dimension> axisDimensionsProperties;

    /**
     * Identification of grid data as point or cell.
     */
    private CellGeometry cellGeometry;

    /**
     * Indication of whether or not parameters for transformation exists.
     */
    private boolean transformationParameterAvailable;

    /**
     * Constructs an initially empty grid spatial representation.
     */
    public DefaultGridSpatialRepresentation() {
    }

    /**
     * Constructs a metadata entity initialized with the values from the specified metadata.
     *
     * @param source
     * @since 2.4
     */
    public DefaultGridSpatialRepresentation(final GridSpatialRepresentation source) {
        super(source);
    }

    /**
     * Creates a grid spatial representation initialized to the given values.
     * This is a convenience constructor. The argument types don't need to
     * match exactly the types expected by getters and setters.
     *
     * @param numberOfDimensions The number of independent spatial-temporal axes.
     * @param axisDimensionsProperties Information about spatial-temporal axis properties.
     * @param cellGeometry Identification of grid data as point or cell.
     * @param transformationParameterAvailable Indication of whether or not parameters for
     *          transformation exists.
     */
    public DefaultGridSpatialRepresentation(final int numberOfDimensions,
                                            final List<? extends Dimension> axisDimensionsProperties,
                                            final CellGeometry cellGeometry,
                                            final boolean transformationParameterAvailable)
    {
        setNumberOfDimensions(numberOfDimensions);
        setAxisDimensionsProperties(axisDimensionsProperties);
        setCellGeometry(cellGeometry);
        setTransformationParameterAvailable(transformationParameterAvailable);
    }

    /**
     * Returns the number of independent spatial-temporal axes.
     */
    @Override
    @XmlElement(name = "numberOfDimensions", required = true)
    public Integer getNumberOfDimensions() {
        return numberOfDimensions;
    }

    /**
     * Sets the number of independent spatial-temporal axes.
     *
     * @param newValue The new number of dimension.
     */
    public synchronized void setNumberOfDimensions(final Integer newValue) {
        checkWritePermission();
        numberOfDimensions = newValue;
    }

    /**
     * Returns information about spatial-temporal axis properties.
     */
    @Override
    @XmlElement(name = "axisDimensionProperties", required = true)
    public synchronized List<Dimension> getAxisDimensionsProperties() {
        return axisDimensionsProperties = nonNullList(axisDimensionsProperties, Dimension.class);
    }

    /**
     * Sets the information about spatial-temporal axis properties.
     *
     * @param newValues The new axis dimension properties.
     */
    public synchronized void setAxisDimensionsProperties(final List<? extends Dimension> newValues) {
        checkWritePermission();
        axisDimensionsProperties = (List<Dimension>)
                copyCollection(newValues, axisDimensionsProperties, Dimension.class);
    }

    /**
     * Returns the identification of grid data as point or cell.
     */
    @Override
    @XmlElement(name = "cellGeometry", required = true)
    public CellGeometry getCellGeometry() {
        return cellGeometry;
    }

    /**
     * Sets identification of grid data as point or cell.
     *
     * @param newValue The new cell geometry.
     */
    public synchronized void setCellGeometry(final CellGeometry newValue) {
        checkWritePermission();
        cellGeometry = newValue;
    }

    /**
     * Returns indication of whether or not parameters for transformation exists.
     */
    @Override
    @XmlElement(name = "transformationParameterAvailability", required = true)
    public boolean isTransformationParameterAvailable() {
        return transformationParameterAvailable;
    }

    /**
     * Sets indication of whether or not parameters for transformation exists.
     *
     * @param newValue {@code true} if the transformation parameters are available.
     */
    public synchronized void setTransformationParameterAvailable(final boolean newValue) {
        checkWritePermission();
        transformationParameterAvailable = newValue;
    }
}
