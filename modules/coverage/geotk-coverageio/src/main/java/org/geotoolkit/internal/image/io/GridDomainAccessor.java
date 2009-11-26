/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Open Source Geospatial Foundation (OSGeo)
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
 */
package org.geotoolkit.internal.image.io;

import javax.imageio.metadata.IIOMetadata;

import org.opengis.metadata.spatial.CellGeometry;
import org.opengis.metadata.spatial.PixelOrientation;

import org.geotoolkit.resources.Errors;
import org.geotoolkit.image.io.metadata.MetadataAccessor;

import static org.geotoolkit.image.io.metadata.SpatialMetadataFormat.FORMAT_NAME;


/**
 * A convenience specialization of {@link MetadataAccessor} for nodes related to the
 * {@code "RectifiedGridDomain"} node. This class actually defines itself as an accessor
 * of {@code "RectifiedGridDomain/OffsetVectors"}, but provides also convenience methods
 * for the {@code "RectifiedGridDomain/Limits"} and the {@code "SpatialRepresentation"}
 * nodes.
 * <p>
 * For most usage, the following methods should be invoked exactly once.
 * They will take care of invoking the appropriate setter methods.
 * <p>
 * <ul>
 *   <li>{@link #setSpatialRepresentation setSpatialRepresentation}</li>
 *   <li>{@link #setRectifiedGridDomain   setRectifiedGridDomain}</li>
 * </ul>
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.06
 *
 * @since 3.06
 * @module
 */
public final class GridDomainAccessor extends MetadataAccessor {
    /**
     * Creates a new accessor for the given metadata.
     *
     * @param metadata The Image I/O metadata. An instance of the {@link SpatialMetadata}
     *                 sub-class is recommanded, but not mandatory.
     */
    public GridDomainAccessor(final IIOMetadata metadata) {
        super(metadata, FORMAT_NAME, "RectifiedGridDomain/OffsetVectors", "OffsetVector");
    }

    /**
     * Sets the {@code "low"} and {@code "high"} attributes of the
     * {@code "RectifiedGridDomain/Limits"} node to the given values.
     *
     * @param low  The value to be assigned to the {@code "low"} attribute.
     * @param high The value to be assigned to the {@code "high"} attribute.
     */
    public void setLimits(final int[] low, final int[] high) {
        final MetadataAccessor accessor = new MetadataAccessor(
                metadata, FORMAT_NAME, "RectifiedGridDomain/Limits", null);
        accessor.setAttribute("low",  low);
        accessor.setAttribute("high", high);
    }

    /**
     * Sets the {@code "origin"} attribute to the given value. This method shall be invoked only
     * when the {@linkplain #selectParent() parent node is selected}. This is typically the case
     * if this method is invoked before {@link #addOffsetVector(double[])}.
     *
     * @param values The value to be assigned to the {@code "origin"} attribute.
     */
    public void setOrigin(final double... values) {
        setAttribute("origin", values);
    }

    /**
     * Appends a new {@code "OffsetVector"} node with the {@code "values"}
     * attribute set to the given array.
     *
     * @param values The value to be assigned to the {@code "values"} attribute
     *        of a new {@code "OffsetVector"} node.
     */
    public void addOffsetVector(final double... values) {
        selectChild(appendChild());
        setAttribute("values", values);
    }

    /**
     * Checks the length of the given array against the expected value.
     * In case of mismatch, an {@link IllegalArgumentException} is thrown.
     */
    private static void checkDimension(final String name, final int length, final int expected) {
        if (length != expected) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.MISMATCHED_DIMENSION_$3, name, length, expected));
        }
    }

    /**
     * Sets the values of the {@code "SpatialRepresentation"} attributes. This method computes
     * the {@code "centerPoint"} attribute from the given {@code "origin"} and {"bounds"} because
     * this method is typically invoked together with the {@link #setRectifiedGridDomain} method.
     *
     * @param origin       The {@code origin} argument given to the {@code setRectifiedGridDomain} method.
     * @param bounds       The {@code bounds} argument given to the {@code setRectifiedGridDomain} method.
     * @param cellGeometry The value to assign to the {@code "cellGeometry"} attribute.
     * @param pointInPixel The value to assign to the {@code "pointInPixel"} attribute.
     */
    public void setSpatialRepresentation(final double[] origin, final double[] bounds,
            final CellGeometry cellGeometry, final PixelOrientation pointInPixel)
    {
        final int crsDim = origin.length;
        checkDimension("bounds", bounds.length, crsDim);
        final double[] center = new double[crsDim];
        for (int i=0; i<crsDim; i++) {
            center[i] = 0.5 * (origin[i] + bounds[i]);
        }
        final MetadataAccessor accessor = new MetadataAccessor(
                metadata, FORMAT_NAME, "SpatialRepresentation", null);
        accessor.setAttribute("numberOfDimensions", crsDim);
        accessor.setAttribute("centerPoint",  center);
        accessor.setAttribute("pointInPixel", pointInPixel);
        accessor.setAttribute("cellGeometry", cellGeometry);
    }

    /**
     * Sets the values of the {@code "RectifiedGridDomain"} attributes of offset vectors.
     * This convenience method invokes the following methods with values computes from
     * the arguments:
     * <p>
     * <ul>
     *   <li>{@link #setLimits}</li>
     *   <li>{@link #setOrigin}</li>
     *   <li>{@link #addOffsetVector}</li>
     * </ul>
     *
     * {@section Grid and CRS dimensions)
     * The dimension of the grid (named {@code gridDim} below) is typically equals to the dimension
     * of the CRS (named {@code crsDim} below). But in some cases the CRS dimension may be greater
     * than the grid dimension (the converse is not allowed however).
     * See {@link org.opengis.coverage.grid.RectifiedGrid} for more information.
     *
     * @param origin
     *          The coordinate of the pixel at the {@code low} index.
     *          The length of this array shall be equals to the {@code crsDim}.
     * @param bounds
     *          The coordinate of the pixel at the {@code high} index. The length of this array
     *          shall be equals to the {@code crsDim}. The ordinate values are often greater than
     *          {@code origin}, but not always. For example if the direction of the <var>y</var>
     *          axis in the CRS space has the opposite direction than the corresponding axis in
     *          the pixel space, then the {@code bounds} value is smaller than {@code origin}.
     * @param low
     *          The smaller pixel index, or {@code null} for an array filled with zeros.
     *          If non-null, the array length shall be equals to {@code gridDim}.
     * @param high
     *          The largest pixel index, <strong>inclusive</strong>.
     *          The array length shall be equals to {@code gridDim}.
     * @param gridToCrsDim
     *          If non-null, an array of length {@code gridDim} where, for the grid dimension
     *          {@code i}, the CRS dimension to use is {@code gridToCrsDim[i]}. For example if
     *          the first grid dimension is for the <var>y</var> axis in the CRS space and the
     *          second grid dimension is for the <var>x</var> axis in the CRS space (in other
     *          words if the axes shall be swapped), then this array shall be {@code {1,0}}.
     *          A null value is equivalent to an {@code {0,1,2,3...}} array.
     * @param pixelCenter
     *          {@code true} if the {@code origin} and {@code bounds} coordinates map pixel center
     *          (with both coordinates inclusive), or {@code false} if they are a bounding box
     *          which contains the totality of the grid.
     */
    public void setRectifiedGridDomain(final double[] origin, final double[] bounds, int[] low,
            final int[] high, final int[] gridToCrsDim, final boolean pixelCenter)
    {
        final int crsDim  = origin.length;
        final int gridDim = high.length;
        if (low == null) {
            low = new int[gridDim];
        }
        checkDimension("low",    low.length,   gridDim);
        checkDimension("bounds", bounds.length, crsDim);
        if (crsDim < gridDim) {
            checkDimension("origin", crsDim, gridDim);
        }
        if (gridToCrsDim != null) {
            checkDimension("gridToCrsDim", gridToCrsDim.length, gridDim);
        }
        setLimits(low, high);
        setOrigin(origin);
        final double[] vector = new double[crsDim];
        for (int i=0; i<gridDim; i++) {
            final int j = (gridToCrsDim != null) ? gridToCrsDim[i] : i;
            int span = high[i] - low[i];
            if (!pixelCenter) {
                span++;
            }
            vector[j] = MetadataHelper.nice((bounds[i] - origin[i]) / span);
            addOffsetVector(vector);
            vector[j] = 0;
        }
    }

    /**
     * Convenience method invoking {@link #setSpatialRepresentation setSpatialRepresentation} and
     * {@link #setRectifiedGridDomain setRectifiedGridDomain} for a two-dimensional bounding box.
     *
     * @param xOrigin The first  ordinate of the {@code origin} parameter.
     * @param yOrigin The second ordinate of the {@code origin} parameter.
     * @param xBound  The first  ordinate of the {@code bounds} parameter.
     * @param yBound  The second ordinate of the {@code bounds} parameter.
     * @param width   The number of pixels along the <var>x</var> axis.
     * @param height  The number of pixels along the <var>y</var> axis.
     * @param cellGeometry The value to assign to the {@code "cellGeometry"} attribute.
     * @param pixelCenter {@code true} if the {@code origin} and {@code bounds} coordinates map
     *          pixel center (with both coordinates inclusive), or {@code false} if they are a
     *          bounding box which contains the totality of the grid.
     */
    public void setAll(final double xOrigin, final double yOrigin, final double xBound,
            final double yBound, final int width, final int height, final boolean pixelCenter,
            final CellGeometry cellGeometry)
    {
        final double[] origin = new double[] {xOrigin, yOrigin};
        final double[] bounds = new double[] {xBound,  yBound};
        final int[]    high   = new int[]    {width-1, height-1};
        setRectifiedGridDomain(origin, bounds, null, high, null, true);
        setSpatialRepresentation(origin, bounds, cellGeometry,
                pixelCenter ? PixelOrientation.CENTER : PixelOrientation.UPPER_LEFT);
    }
}
