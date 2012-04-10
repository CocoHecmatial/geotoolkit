/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010-2012, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2010-2012, Geomatys
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
package org.geotoolkit.referencing.operation.provider;

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Transformation;

import org.geotoolkit.internal.referencing.Identifiers;
import org.geotoolkit.metadata.iso.citation.Citations;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.parameter.DefaultParameterDescriptor;
import org.geotoolkit.referencing.NamedIdentifier;
import org.geotoolkit.referencing.operation.transform.NTv2Transform;
import org.geotoolkit.referencing.operation.MathTransformProvider;
import org.geotoolkit.resources.Errors;


/**
 * The provider for "<cite>France geocentric interpolation</cite>" (ESPG:9655).
 * The current implementation delegates to the emulation based on NTv2 method.
 *
 * <!-- PARAMETERS RGF93 -->
 * <p>The following table summarizes the parameters recognized by this provider.
 * For a more detailed parameter list, see the {@link #PARAMETERS} constant.</p>
 * <p><b>Operation name:</b> France geocentric interpolation</p>
 * <table bgcolor="#F4F8FF" cellspacing="0" cellpadding="0">
 *   <tr bgcolor="#B9DCFF"><th>Parameter Name</th><th>Default value</th></tr>
 *   <tr><td>Geocentric translation file</td><td>&nbsp;&nbsp;<code>"gr3df97a.txt"</code></td></tr>
 * </table>
 * <!-- END OF PARAMETERS -->
 *
 * @author Simon Reynard (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.20
 *
 * @since 3.12
 * @module
 */
public class RGF93 extends MathTransformProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4049217192968903800L;

    /**
     * The operation parameter descriptor for the <cite>Geocentric translation file</cite>
     * parameter value. The default value is {@code "gr3df97a.txt"}.
     *
     * @deprecated Invoke <code>{@linkplain #PARAMETERS}.{@linkplain ParameterDescriptorGroup#descriptor(String)
     * descriptor(String)}</code> instead.
     */
    @Deprecated
    public static final ParameterDescriptor<String> TRANSLATION_FILE = new DefaultParameterDescriptor<String>(
            Citations.EPSG, "Geocentric translation file", String.class, null, "gr3df97a.txt", null, null, null, true);

    /**
     * The group of all parameters expected by this coordinate operation.
     * The following table lists the operation names and the parameters recognized by Geotk:
     * <p>
     * <!-- GENERATED PARAMETERS - inserted by ProjectionParametersJavadoc -->
     * <table bgcolor="#F4F8FF" border="1" cellspacing="0" cellpadding="6">
     *   <tr bgcolor="#B9DCFF" valign="top"><td colspan="2">
     *     <table border="0" cellspacing="0" cellpadding="0">
     *       <tr><th align="left">Name:&nbsp;&nbsp;</th><td><code>EPSG:</code></td><td><code>France geocentric interpolation</code></td></tr>
     *       <tr><th align="left">Identifier:&nbsp;&nbsp;</th><td><code>EPSG:</code></td><td><code>9655</code></td></tr>
     *     </table>
     *   </td></tr>
     *   <tr valign="top"><td>
     *     <table border="0" cellspacing="0" cellpadding="0">
     *       <tr><th align="left">Name:&nbsp;&nbsp;</th><td><code>EPSG:</code></td><td><code>Geocentric translation file</code></td></tr>
     *     </table>
     *   </td><td>
     *     <table border="0" cellspacing="0" cellpadding="0">
     *       <tr><th align="left">Type:&nbsp;&nbsp;</th><td><code>String</code></td></tr>
     *       <tr><th align="left">Obligation:&nbsp;&nbsp;</th><td>mandatory</td></tr>
     *       <tr><th align="left">Default value:&nbsp;&nbsp;</th><td><code>"gr3df97a.txt"</code></td></tr>
     *     </table>
     *   </td></tr>
     * </table>
     */
    public static final ParameterDescriptorGroup PARAMETERS = Identifiers.createDescriptorGroup(
        new ReferenceIdentifier[] {
            new NamedIdentifier(Citations.EPSG, "France geocentric interpolation"),
            new IdentifierCode (Citations.EPSG,  9655)
        }, null, new ParameterDescriptor<?>[] {
            TRANSLATION_FILE
        });

    /**
     * Constructs a provider.
     */
    public RGF93() {
        super(2, 2, PARAMETERS);
    }

    /**
     * Returns the operation type.
     */
    @Override
    public Class<Transformation> getOperationType() {
        return Transformation.class;
    }

    /**
     * Creates a math transform from the specified group of parameter values.
     *
     * @throws FactoryException If the grid files can not be loaded.
     */
    @Override
    protected MathTransform createMathTransform(final ParameterValueGroup values) throws FactoryException {
        final String file = Parameters.stringValue(TRANSLATION_FILE, values);
        if (!"gr3df97a.txt".equals(file)) {
            throw new FactoryException(Errors.format(Errors.Keys.CANT_READ_FILE_$1, file));
        }
        return new NTv2Transform(NTv2Transform.RGF93);
    }
}
