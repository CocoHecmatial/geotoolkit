/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2005-2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.coverage.processing.operation;

import org.geotoolkit.util.NumberRange;
import org.geotoolkit.coverage.processing.OperationJAI;
import javax.media.jai.operator.MultiplyConstDescriptor;


/**
 * Multiplies every sample values of the source coverage by constants (one for each band).
 * If the number of constants supplied is less than the number of bands of the destination,
 * then the constant from entry 0 is applied to all the bands. Otherwise, a constant from a
 * different entry is applied to each band.
 *
 * <P><b>Name:</b>&nbsp;{@code "MultiplyConst"}<BR>
 *    <b>JAI operator:</b>&nbsp;<CODE>"{@linkplain MultiplyConstDescriptor MultiplyConst}"</CODE><BR>
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
 *     <td>{@code "constants"}</td>
 *     <td>{@code double[]}</td>
 *     <td align="center">1.0</td>
 *     <td align="center">N/A</td>
 *     <td align="center">N/A</td>
 *   </tr>
 * </table>
 *
 * @author Martin Desruisseaux (IRD)
 * @version 3.00
 *
 * @see org.geotoolkit.coverage.processing.Operations#multiply
 * @see MultiplyConstDescriptor
 *
 * @since 2.2
 * @module
 *
 * @todo Should operates on {@code sampleToGeophysics} transform when possible.
 *       See <A HREF="http://jira.codehaus.org/browse/GEOT-610">GEOT-610</A>.
 */
public class MultiplyConst extends OperationJAI {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -1077713495023498436L;

    /**
     * Constructs a default {@code "MultiplyConst"} operation.
     */
    public MultiplyConst() {
        super("MultiplyConst");
    }

    /**
     * Returns the expected range of values for the resulting image.
     */
    @Override
    protected NumberRange<?> deriveRange(final NumberRange<?>[] ranges, final Parameters parameters) {
        final double[] constants = (double[]) parameters.parameters.getObjectParameter("constants");
        if (constants.length == 1) {
            final double c = constants[0];
            final NumberRange<?> range = ranges[0];
            final double min = range.getMinimum() * c;
            final double max = range.getMaximum() * c;
            return (max < min) ? NumberRange.create(max, min) : NumberRange.create(min, max);
        }
        return super.deriveRange(ranges, parameters);
    }
}
