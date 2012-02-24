/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2006-2012, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2012, Geomatys
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
package org.geotoolkit.coverage.processing.operation;

import javax.media.jai.operator.MinFilterShape;
import javax.media.jai.operator.MinFilterDescriptor;
import net.jcip.annotations.Immutable;

import org.geotoolkit.coverage.processing.FilterOperation;


/**
 * For each position of the mask, replaces the center pixel by the minimum of the pixel
 * values covered by the mask.
 *
 * <P><b>Name:</b>&nbsp;{@code "MinFilter"}<BR>
 *    <b>JAI operator:</b>&nbsp;<CODE>"{@linkplain MinFilterDescriptor MinFilter}"</CODE><BR>
 *    <b>Parameters:</b></P>
 * <table border='3' cellpadding='6' bgcolor='F4F8FF'>
 *   <tr bgcolor='#B9DCFF'>
 *     <th>Name</th>
 *     <th>Class</th>
 *     <th>Default value</th>
 *     <th>Minimum value</th>
 *     <th>Maximum value</th>
 *   </tr>
 *   <tr>
 *     <td>{@code "Source"}</td>
 *     <td>{@link org.geotoolkit.coverage.grid.GridCoverage2D}</td>
 *     <td align="center">N/A</td>
 *     <td align="center">N/A</td>
 *     <td align="center">N/A</td>
 *   </tr>
 *   <tr>
 *     <td>{@code "maskShape"}</td>
 *     <td>{@link MinFilterShape}</td>
 *     <td>{@link MinFilterDescriptor#MIN_MASK_SQUARE}</td>
 *     <td align="center">N/A</td>
 *     <td align="center">N/A</td>
 *   </tr>
 *   <tr>
 *     <td>{@code "maskSize"}</td>
 *     <td>{@link Integer}</td>
 *     <td>{@code 3}</td>
 *     <td align="center">N/A</td>
 *     <td align="center">N/A</td>
 *   </tr>
 * </table>
 *
 * @author Martin Desruisseaux (IRD)
 * @version 3.00
 *
 * @since 2.3
 * @module
 */
@Immutable
public class MinFilter extends FilterOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -45487721305059086L;

    /**
     * Constructs a default {@code "MinFilter"} operation.
     */
    public MinFilter() {
        super("MinFilter");
    }
}
