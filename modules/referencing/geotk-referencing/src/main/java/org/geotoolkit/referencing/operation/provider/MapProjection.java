/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 1999-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2010, Geomatys
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

import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import java.util.NoSuchElementException;

import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.Projection;

import org.geotoolkit.lang.Immutable;
import org.geotoolkit.referencing.NamedIdentifier;
import org.geotoolkit.referencing.operation.MathTransformProvider;
import org.geotoolkit.parameter.DefaultParameterDescriptor;
import org.geotoolkit.metadata.iso.citation.Citations;
import org.geotoolkit.internal.referencing.Identifiers;

import static org.geotoolkit.internal.referencing.Identifiers.createDescriptor;
import static org.geotoolkit.internal.referencing.Identifiers.createOptionalDescriptor;
/*
 * Do not import UnitaryProjection, and do not use it neither except as fully-qualified names
 * only in javadoc comments. As of Java 6 update 10, using UnitaryProjection seems to confuse
 * javac when it tries to compile the Parameters nested class with protected access.  I guess
 * this is related to cyclic dependency, which is nice to avoid anyway.
 */


/**
 * The base provider for {@linkplain org.geotoolkit.referencing.operation.projection map projections}.
 * This base class defines the descriptors for the most commonly used parameters. Subclasses will
 * declare the parameters they use in a {@linkplain ParameterDescriptorGroup descriptor group}
 * named {@code PARAMETERS}.
 *
 * @author Martin Desruisseaux (IRD)
 * @version 3.00
 *
 * @see <A HREF="http://mathworld.wolfram.com/MapProjection.html">Map projections on MathWorld</A>
 * @see <A HREF="http://atlas.gc.ca/site/english/learningresources/carto_corner/map_projections.html">Map projections on the atlas of Canada</A>
 *
 * @since 2.0
 * @module
 */
@Immutable
public abstract class MapProjection extends MathTransformProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6280666068007678702L;

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#semiMajor
     * semi major} parameter value. Valid values range is (0 &hellip; &infin;). This parameter
     * is mandatory and has no default value.
     */
    public static final ParameterDescriptor<Double> SEMI_MAJOR = createDescriptor(
            new NamedIdentifier[] {
                new NamedIdentifier(Citations.OGC,  "semi_major"),
                new NamedIdentifier(Citations.ESRI, "Semi_Major"),
                new NamedIdentifier(Citations.EPSG, "Semi-major axis")
                // EPSG does not specifically define the above parameter
            },
            Double.NaN, 0, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * The operation parameter descriptor for the {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#semiMinor
     * semi minor} parameter value. Valid values range is (0 &hellip; &infin;). This parameter
     * is mandatory and has no default value.
     */
    public static final ParameterDescriptor<Double> SEMI_MINOR = createDescriptor(
            new NamedIdentifier[] {
                new NamedIdentifier(Citations.OGC,  "semi_minor"),
                new NamedIdentifier(Citations.ESRI, "Semi_Minor"),
                new NamedIdentifier(Citations.EPSG, "Semi-minor axis")
                // EPSG does not specifically define the above parameter
            },
            Double.NaN, 0, Double.POSITIVE_INFINITY, SI.METRE);

    /**
     * The operation parameter descriptor for whatever the projection should roll longitude.
     * If {@code true}, then the value of (<var>longitude</var> - {@linkplain
     * org.geotoolkit.referencing.operation.projection.UnitaryProjection.Parameters#centralMeridian
     * central meridian}) will be rolled to the [-180 &hellip; 180)&deg; range before the projection
     * is applied. If {@code false}, then longitude rolling is never applied. If not provided, then
     * the default behavior is to roll longitude only if the central meridian is different than zero.
     * <p>
     * This is a Geotk-specific parameter.
     *
     * @since 3.00
     */
    public static final ParameterDescriptor<Boolean> ROLL_LONGITUDE = new DefaultParameterDescriptor<Boolean>(
            Citations.GEOTOOLKIT, "roll_longitude", Boolean.class, null, null, null, null, null, false);

    /**
     * The operation parameter descriptor for the ESRI {@code "X_Scale"} parameter value.
     * Valid values range is unrestricted. This parameter is optional and its default value is 1.
     * <p>
     * This is an ESRI-specific parameter, but its usage could be extended to any projection.
     * The choice to allow this parameter or not is taken on a projection-by-projection basis.
     *
     * @since 3.00
     */
    public static final ParameterDescriptor<Double> X_SCALE = createOptionalDescriptor(
            new NamedIdentifier[] {
                new NamedIdentifier(Citations.ESRI, "X_Scale")
            },
            1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Unit.ONE);

    /**
     * The operation parameter descriptor for the ESRI {@code "Y_Scale"} parameter value.
     * Valid values range is unrestricted. This parameter is optional and its default value is 1.
     * <p>
     * This is an ESRI-specific parameter, but its usage could be extended to any projection.
     * The choice to allow this parameter or not is taken on a projection-by-projection basis.
     *
     * @since 3.00
     */
    public static final ParameterDescriptor<Double> Y_SCALE = createOptionalDescriptor(
            new NamedIdentifier[] {
                new NamedIdentifier(Citations.ESRI, "Y_Scale")
            },
            1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Unit.ONE);

    /**
     * The operation parameter descriptor for the ESRI {@code "XY_Plane_Rotation"} parameter value.
     * The rotation is applied before the <cite>false easting</cite> and <cite>false northing</cite>
     * translation, if any. Valid values range is [-360 &hellip; 360]&deg;. This parameter is
     * optional and its default value is 0&deg;.
     * <p>
     * This is an ESRI-specific parameter, but its usage could be extended to any projections.
     * The choice to allow this parameter or not is taken on a projection-by-projection basis.
     *
     * @since 3.00
     */
    public static final ParameterDescriptor<Double> XY_PLANE_ROTATION = createOptionalDescriptor(
            /*
             * The descriptors defined in this class are ordinary descriptors instead than the
             * special Identifiers subclass because there is no need to manage aliases for them.
             * This is especially important for XY_PLANE_ROTATION because it shares the same name
             * than RECTIFIED_GRID_ANGLE (used in ObliqueMercator), and we don't want those
             * parameters to be confused. Defining this descriptor as an ordinary one force
             * UnitaryProjection.Parameters to check for strict equality when looking for
             * XY_PLANE_ROTATION, while the relaxed check (by name only) is allowed when
             * looking for RECTIFIED_GRID_ANGLE.
             */
            new NamedIdentifier[] {
                sameNameAs(Citations.ESRI, Identifiers.RECTIFIED_GRID_ANGLE)
            },
            0, -360, +360, NonSI.DEGREE_ANGLE);

    /**
     * Returns the name of the given authority declared in the given parameter descriptor.
     * This method is used only as a way to avoid creating many instances of the same name.
     */
    static NamedIdentifier sameNameAs(final Citation authority, final GeneralParameterDescriptor parameters) {
        for (final GenericName candidate : parameters.getAlias()) {
            if (candidate instanceof NamedIdentifier) {
                final NamedIdentifier name = (NamedIdentifier) candidate;
                if (name.getAuthority() == authority) {
                    return name;
                }
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Constructs a math transform provider from a set of parameters. The provider
     * {@linkplain #getIdentifiers identifiers} will be the same than the parameter
     * ones.
     *
     * @param parameters The set of parameters (never {@code null}).
     */
    protected MapProjection(final ParameterDescriptorGroup parameters) {
        super(2, 2, parameters);
    }

    /**
     * Returns the operation type for this map projection.
     */
    @Override
    public Class<? extends Projection> getOperationType() {
        return Projection.class;
    }

    /**
     * Creates a map projection from the specified group of parameter values.
     *
     * @param  values The group of parameter values.
     * @return The created map projection.
     * @throws ParameterNotFoundException if a required parameter was not found.
     */
    @Override
    protected abstract MathTransform2D createMathTransform(ParameterValueGroup values)
            throws ParameterNotFoundException;
}
