/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2004-2011, Open Source Geospatial Foundation (OSGeo)
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
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package org.geotoolkit.referencing.operation;

import java.util.Map;

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.Projection;
import org.opengis.referencing.operation.Transformation;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.ConicProjection;
import org.opengis.referencing.operation.PlanarProjection;
import org.opengis.referencing.operation.CylindricalProjection;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.geotoolkit.referencing.ComparisonMode;
import org.geotoolkit.referencing.AbstractIdentifiedObject;
import org.geotoolkit.referencing.operation.transform.Parameterized;
import org.geotoolkit.referencing.operation.transform.PassThroughTransform;
import org.geotoolkit.internal.referencing.ParameterizedAffine;
import org.geotoolkit.internal.referencing.Semaphores;
import org.geotoolkit.io.wkt.Formatter;
import org.geotoolkit.lang.Immutable;
import org.geotoolkit.util.converter.Classes;
import org.geotoolkit.util.UnsupportedImplementationException;

import static org.geotoolkit.util.Utilities.ensureNonNull;


/**
 * A parameterized mathematical operation on coordinates that transforms or converts
 * coordinates to another {@linkplain CoordinateReferenceSystem coordinate reference
 * system}. This coordinate operation thus uses an {@linkplain OperationMethod operation
 * method}, usually with associated parameter values.
 * <p>
 * In the Geotk implementation, the {@linkplain #getParameterValues parameter values}
 * are inferred from the {@linkplain #transform transform}. Other implementations may
 * have to override the {@link #getParameterValues} method.
 * <p>
 * This is a single (not {@linkplain DefaultConcatenatedOperation concatenated})
 * coordinate operation.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.16
 *
 * @since 2.0
 * @module
 */
@Immutable
public class DefaultSingleOperation extends AbstractCoordinateOperation implements SingleOperation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2635450075620911309L;

    /**
     * The operation method.
     */
    protected final OperationMethod method;

    /**
     * Constructs a new operation with the same values than the specified defining
     * conversion, together with the specified source and target CRS. This constructor
     * is used by {@link DefaultConversion} only.
     */
    DefaultSingleOperation(final Conversion               definition,
                           final CoordinateReferenceSystem sourceCRS,
                           final CoordinateReferenceSystem targetCRS,
                           final MathTransform             transform)
    {
        super(definition, sourceCRS, targetCRS, transform);
        method = definition.getMethod();
    }

    /**
     * Constructs an operation from a set of properties.
     * The properties given in argument follow the same rules than for the
     * {@linkplain AbstractCoordinateOperation#AbstractCoordinateOperation(Map,
     * CoordinateReferenceSystem, CoordinateReferenceSystem, MathTransform)
     * super-class constructor}.
     *
     * @param properties Set of properties. Should contains at least {@code "name"}.
     * @param sourceCRS  The source CRS.
     * @param targetCRS  The target CRS.
     * @param transform  Transform from positions in the {@linkplain #getSourceCRS source CRS}
     *                   to positions in the {@linkplain #getTargetCRS target CRS}.
     * @param method     The operation method.
     */
    public DefaultSingleOperation(final Map<String,?>             properties,
                                  final CoordinateReferenceSystem sourceCRS,
                                  final CoordinateReferenceSystem targetCRS,
                                  final MathTransform             transform,
                                  final OperationMethod           method)
    {
        super(properties, sourceCRS, targetCRS, transform);
        ensureNonNull("method", method);
        DefaultOperationMethod.checkDimensions(method, transform);
        this.method = method;
    }

    /**
     * Returns a coordinate operation of the specified class. This method constructs an instance
     * of {@link Transformation}, {@link ConicProjection}, {@link CylindricalProjection},
     * {@link PlanarProjection}, {@link Projection} or {@link Conversion}.
     *
     * @param properties Set of properties. Should contains at least {@code "name"}.
     * @param sourceCRS  The source CRS.
     * @param targetCRS  The target CRS.
     * @param transform  Transform from positions in the {@linkplain #getSourceCRS source CRS}
     *                   to positions in the {@linkplain #getTargetCRS target CRS}.
     * @param method     The operation method, or {@code null}.
     * @param type       The minimal type as <code>{@linkplain Conversion}.class</code>,
     *                   <code>{@linkplain Projection}.class</code>, <i>etc.</i>
     *                   This method may create an instance of a subclass of {@code type}.
     * @return A new coordinate operation of the given type.
     *
     * @see DefaultConversion#create(Map, CoordinateReferenceSystem, CoordinateReferenceSystem, MathTransform, OperationMethod, Class)
     */
    public static CoordinateOperation create(final Map<String,?>            properties,
                                             final CoordinateReferenceSystem sourceCRS,
                                             final CoordinateReferenceSystem targetCRS,
                                             final MathTransform             transform,
                                             final OperationMethod           method,
                                             Class<? extends CoordinateOperation> type)
    {
        if (method != null) {
            if (method instanceof MathTransformProvider) {
                final Class<? extends SingleOperation> candidate =
                        ((MathTransformProvider) method).getOperationType();
                if (candidate != null) {
                    if (type == null) {
                        type = candidate;
                    } else if (type.isAssignableFrom(candidate)) {
                        type = candidate.asSubclass(type);
                    }
                }
            }
            if (type != null) {
                if (Transformation.class.isAssignableFrom(type)) {
                    return new DefaultTransformation(
                               properties, sourceCRS, targetCRS, transform, method);
                }
                if (ConicProjection.class.isAssignableFrom(type)) {
                    return new DefaultConicProjection(
                               properties, sourceCRS, targetCRS, transform, method);
                }
                if (CylindricalProjection.class.isAssignableFrom(type)) {
                    return new DefaultCylindricalProjection(
                               properties, sourceCRS, targetCRS, transform, method);
                }
                if (PlanarProjection.class.isAssignableFrom(type)) {
                    return new DefaultPlanarProjection(
                               properties, sourceCRS, targetCRS, transform, method);
                }
                if (Projection.class.isAssignableFrom(type)) {
                    return new DefaultProjection(
                               properties, sourceCRS, targetCRS, transform, method);
                }
                if (Conversion.class.isAssignableFrom(type)) {
                    return new DefaultConversion(
                               properties, sourceCRS, targetCRS, transform, method);
                }
            }
            return new DefaultSingleOperation(
                       properties, sourceCRS, targetCRS, transform, method);
        }
        return new AbstractCoordinateOperation(properties, sourceCRS, targetCRS, transform);
    }

    /**
     * Returns the operation method.
     */
    @Override
    public OperationMethod getMethod() {
        return method;
    }

    /**
     * Returns the parameter values. The default implementation infers the parameter
     * values from the {@linkplain #transform transform}, if possible.
     *
     * @throws UnsupportedOperationException if the parameters values can't be determined
     *         for the current math transform implementation.
     *
     * @see DefaultMathTransformFactory#createParameterizedTransform(ParameterValueGroup)
     * @see Parameterized#getParameterValues()
     */
    @Override
    public ParameterValueGroup getParameterValues() throws UnsupportedOperationException {
        MathTransform mt = transform;
        while (mt != null) {
            if (mt instanceof Parameterized) {
                final ParameterValueGroup param;
                if (mt instanceof ParameterizedAffine) {
                    param = ((ParameterizedAffine) mt).parameters.getParameterValues();
                } else {
                    if (Semaphores.queryAndSet(Semaphores.PROJCS)) {
                        throw new AssertionError(); // Should never happen.
                    }
                    try {
                        param = ((Parameterized) mt).getParameterValues();
                    } finally {
                        Semaphores.clear(Semaphores.PROJCS);
                    }
                }
                if (param != null) {
                    return param;
                }
            }
            if (mt instanceof PassThroughTransform) {
                mt = ((PassThroughTransform) mt).getSubTransform();
            } else {
                break;
            }
        }
        throw new UnsupportedImplementationException(Classes.getClass(mt));
    }

    /**
     * Compares this operation method with the specified object for equality.
     * If {@code compareMetadata} is {@code true}, then all available properties
     * are compared including the {@linkplain DefaultOperationMethod#getFormula formula}.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final AbstractIdentifiedObject object, final ComparisonMode mode) {
        if (super.equals(object, mode)) {
            final DefaultSingleOperation that = (DefaultSingleOperation) object;
            if (mode.equals(ComparisonMode.STRICT)) {
                return equals(this.method, that.method, mode);
            }
            /*
             * We consider the operation method as metadata. We could argue that OperationMethod's
             * 'sourceDimension' and 'targetDimension' are not metadata,  but their values should
             * be identical to the 'sourceCRS' and 'targetCRS' dimensions, already checked by the
             * superclass. We could also argue that 'OperationMethod.parameters' are not metadata,
             * but their values should have been taken in account for the MathTransform creation,
             * which was compared by the superclass.
             *
             * Comparing the MathTransforms instead of parameters avoid the problem of implicit
             * parameters.  For example in a ProjectedCRS, the "semiMajor" and "semiMinor" axis
             * lengths are sometime provided as explicit parameters, and sometime inferred from
             * the geodetic datum.  The two cases would be different set of parameters from the
             * OperationMethod's point of view, but still result in the creation of identical
             * MathTransform.
             *
             * An other rational for treating OperationMethod as metadata is that Geotk
             * MathTransformProvider extends DefaultOperationMethod. Consequently there is
             * a wide range of subclasses, which make the comparisons more difficult. For
             * example Mercator1SP and Mercator2SP providers are two different ways to describe
             * the same projection. The SQL-backed EPSG factory uses yet an other implementation.
             *
             * NOTE: A previous Geotk implementation made this final check:
             *
             *     return nameMatches(this.method, that.method);
             *
             * but it was not strictly necessary since it was redundant with the comparisons of
             * MathTransforms. Actually it was preventing to detect that two CRS were equivalent
             * despite different method names (e.g. "Mercator (1SP)" and "Mercator (2SP)" when
             * the parameters are properly chosen).
             */
            return true;
        }
        return false;
    }

    /**
     * Returns a hash code value for this operation method.
     */
    @Override
    public int hashCode() {
        return super.hashCode() ^ method.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatWKT(final Formatter formatter) {
        final String name = super.formatWKT(formatter);
        append(formatter, method, "METHOD");
        return name;
    }
}
