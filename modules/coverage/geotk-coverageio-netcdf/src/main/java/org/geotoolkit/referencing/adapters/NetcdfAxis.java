/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2010, Geomatys
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
package org.geotoolkit.referencing.adapters;

import javax.measure.unit.Unit;

import ucar.nc2.constants.CF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;

import org.opengis.util.InternationalString;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.RangeMeaning;

import org.geotoolkit.util.Strings;
import org.geotoolkit.measure.Units;
import org.geotoolkit.util.SimpleInternationalString;


/**
 * Wraps a NetCDF {@link CoordinateAxis1D} as an implementation of GeoAPI interfaces.
 * All methods in this class delegate their work to the wrapped NetCDF axis. Consequently
 * any change in the wrapped axis is immediately reflected in this {@code NetcdfAxis} instance.
 * However users are encouraged to not change the wrapped axis after construction, since GeoAPI
 * referencing objects are expected to be immutable.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.08
 *
 * @since 3.08
 * @module
 */
public class NetcdfAxis extends NetcdfIdentifiedObject implements CoordinateSystemAxis {
    /**
     * The NetCDF coordinate axis wrapped by this {@code NetcdfCRS} instance.
     */
    private final CoordinateAxis1D axis;

    /**
     * Creates a new {@code NetcdfAxis} object wrapping the given NetCDF coordinate axis.
     *
     * @param axis The NetCDF coordinate axis to wrap.
     */
    public NetcdfAxis(final CoordinateAxis1D axis) {
        ensureNonNull("axis", axis);
        this.axis = axis;
    }

    /**
     * Returns the wrapped NetCDF axis.
     */
    @Override
    public CoordinateAxis1D delegate() {
        return axis;
    }

    /**
     * Returns the axis name. The default implementation delegates to
     * {@link CoordinateAxis1D#getName()}.
     *
     * @see CoordinateAxis1D#getName()
     */
    @Override
    public String getCode() {
        return axis.getName();
    }

    /**
     * Returns the axis abbreviation. The default implementation returns
     * an acronym of the value returned by {@link CoordinateAxis1D#getName()}.
     *
     * @see CoordinateAxis1D#getName()
     */
    @Override
    public String getAbbreviation() {
        final String name = axis.getName().trim();
        if (name.equalsIgnoreCase("longitude")) return "\u03BB";
        if (name.equalsIgnoreCase("latitude"))  return "\u03C6";
        return Strings.camelCaseToAcronym(name).toLowerCase();
    }

    /**
     * Returns the axis direction. The default implementation delegates to
     * {@link #getDirection(CoordinateAxis)}.
     *
     * @see CoordinateAxis1D#getAxisType()
     * @see CoordinateAxis1D#getPositive()
     */
    @Override
    public AxisDirection getDirection() {
        return getDirection(axis);
    }

    /**
     * Returns the direction of the given axis. This method infers the direction from
     * {@link CoordinateAxis#getAxisType()} and {@link CoordinateAxis#getPositive()}.
     * If the direction can not be determined, then this method returns
     * {@link AxisDirection#OTHER}.
     *
     * @param  axis The axis for which to get the direction.
     * @return The direction of the given axis.
     */
    public static AxisDirection getDirection(final CoordinateAxis axis) {
        final AxisType type = axis.getAxisType();
        final boolean down = CF.POSITIVE_DOWN.equals(axis.getPositive());
        if (type != null) {
            switch (type) {
                case Time: return down ? AxisDirection.PAST : AxisDirection.FUTURE;
                case Lon:
                case GeoX: return down ? AxisDirection.WEST : AxisDirection.EAST;
                case Lat:
                case GeoY: return down ? AxisDirection.SOUTH : AxisDirection.NORTH;
                case Pressure:
                case Height:
                case GeoZ: return down ? AxisDirection.DOWN : AxisDirection.UP;
            }
        }
        return AxisDirection.OTHER;
    }

    /**
     * Returns the axis minimal value. The default implementation delegates
     * to {@link CoordinateAxis1D#getMinValue()}.
     *
     * @see CoordinateAxis1D#getMinValue()
     */
    @Override
    public double getMinimumValue() {
        return axis.getMinValue();
    }

    /**
     * Returns the axis maximal value. The default implementation delegates
     * to {@link CoordinateAxis1D#getMaxValue()}.
     *
     * @see CoordinateAxis1D#getMaxValue()
     */
    @Override
    public double getMaximumValue() {
        return axis.getMaxValue();
    }

    /**
     * Returns {@code null} since the range meaning is unspecified.
     */
    @Override
    public RangeMeaning getRangeMeaning() {
        return null;
    }

    /**
     * Returns the units as a string. If the axis direction or the time epoch
     * was appended to the units, then this part of the string is removed.
     */
    final String getUnitsString() {
        String symbol = axis.getUnitsString();
        if (symbol != null) {
            int i = symbol.lastIndexOf('_');
            if (i > 0) {
                final String direction = getDirection().name();
                if (symbol.regionMatches(true, i+1, direction, 0, direction.length())) {
                    symbol = symbol.substring(0, i).trim();
                }
            }
            i = symbol.indexOf(" since ");
            if (i > 0) {
                symbol = symbol.substring(0, i);
            }
            symbol = symbol.trim();
        }
        return symbol;
    }

    /**
     * Returns the units, or {@code null} if unknown.
     *
     * @see CoordinateAxis1D#getUnitsString()
     * @see Units#valueOf(String)
     */
    @Override
    public Unit<?> getUnit() {
        final String symbol = getUnitsString();
        return (symbol != null) ? Units.valueOf(symbol) : null;
    }

    /**
     * Returns the NetCDF description, or {@code null} if none.
     * The default implementation delegates to {@link CoordinateAxis1D#getDescription()}.
     *
     * @see CoordinateAxis1D#getDescription()
     */
    @Override
    public InternationalString getRemarks() {
        final String description = axis.getDescription();
        if (description != null) {
            return new SimpleInternationalString(description);
        }
        return super.getRemarks();
    }
}
