/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2001-2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.referencing.crs;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import java.io.ObjectInputStream;
import net.jcip.annotations.Immutable;

import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.Datum;

import org.geotoolkit.referencing.cs.DefaultCompoundCS;
import org.geotoolkit.referencing.AbstractReferenceSystem;
import org.geotoolkit.util.collection.UnmodifiableArrayList;
import org.geotoolkit.util.collection.CheckedCollection;
import org.geotoolkit.util.ComparisonMode;
import org.geotoolkit.util.Utilities;
import org.geotoolkit.io.wkt.Formatter;
import org.geotoolkit.resources.Errors;

import static org.geotoolkit.util.Utilities.deepEquals;
import static org.geotoolkit.util.ArgumentChecks.ensureNonNull;


/**
 * A coordinate reference system describing the position of points through two or more
 * independent coordinate reference systems. Thus it is associated with two or more
 * {@linkplain CoordinateSystem coordinate systems} and {@linkplain Datum datums} by
 * defining the compound CRS as an ordered set of two or more instances of
 * {@link CoordinateReferenceSystem}.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.18
 *
 * @since 1.2
 * @module
 */
@Immutable
public class DefaultCompoundCRS extends AbstractCRS implements CompoundCRS {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -2656710314586929286L;

    /**
     * The coordinate reference systems in this compound CRS.
     * May actually be a list of {@link SingleCRS}.
     */
    private final List<? extends CoordinateReferenceSystem> crs;

    /**
     * A decomposition of the CRS list into the single elements. Computed
     * by {@link #getElements} on construction or deserialization.
     */
    private transient List<SingleCRS> singles;

    /**
     * Constructs a new object in which every attributes are set to a default value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultCompoundCRS() {
        this(org.geotoolkit.internal.referencing.NullReferencingObject.INSTANCE);
    }

    /**
     * Constructs a new compound CRS with the same values than the specified one.
     * This copy constructor provides a way to wrap an arbitrary implementation into a
     * Geotk one or a user-defined one (as a subclass), usually in order to leverage
     * some implementation-specific API. This constructor performs a shallow copy,
     * i.e. the properties are not cloned.
     *
     * @param crs The coordinate reference system to copy.
     *
     * @since 2.2
     */
    public DefaultCompoundCRS(final CompoundCRS crs) {
        super(crs);
        if (crs instanceof DefaultCompoundCRS) {
            final DefaultCompoundCRS that = (DefaultCompoundCRS) crs;
            this.crs     = that.crs;
            this.singles = that.singles;
        } else {
            this.crs = copy(crs.getComponents());
            // 'singles' is computed by the above method call.
        }
    }

    /**
     * Constructs a coordinate reference system from a name and two CRS.
     *
     * @param name The name.
     * @param head The head CRS.
     * @param tail The tail CRS.
     */
    public DefaultCompoundCRS(final String name,
                              final CoordinateReferenceSystem head,
                              final CoordinateReferenceSystem tail)
    {
        this(name, new CoordinateReferenceSystem[] {head, tail});
    }

    /**
     * Constructs a coordinate reference system from a name and three CRS.
     *
     * @param name The name.
     * @param head The head CRS.
     * @param middle The middle CRS.
     * @param tail The tail CRS.
     */
    public DefaultCompoundCRS(final String name,
                              final CoordinateReferenceSystem head,
                              final CoordinateReferenceSystem middle,
                              final CoordinateReferenceSystem tail)
    {
        this(name, new CoordinateReferenceSystem[] {head, middle, tail});
    }

    /**
     * Constructs a coordinate reference system from a name.
     *
     * @param name The name.
     * @param crs The array of coordinate reference system making this compound CRS.
     */
    public DefaultCompoundCRS(final String name, final CoordinateReferenceSystem[] crs) {
        this(Collections.singletonMap(NAME_KEY, name), crs);
    }

    /**
     * Constructs a coordinate reference system from a set of properties.
     * The properties are given unchanged to the
     * {@linkplain AbstractReferenceSystem#AbstractReferenceSystem(Map) super-class constructor}.
     *
     * @param properties Set of properties. Should contains at least {@code "name"}.
     * @param crs The array of coordinate reference system making this compound CRS.
     */
    public DefaultCompoundCRS(final Map<String,?> properties, CoordinateReferenceSystem... crs) {
        super(properties, createCoordinateSystem(crs));
        this.crs = copy(Arrays.asList(crs));
        // 'singles' is computed by the above method call.
    }

    /**
     * Returns a compound coordinate system for the specified array of CRS objects.
     * This method is a work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    private static CoordinateSystem createCoordinateSystem(final CoordinateReferenceSystem[] crs) {
        ensureNonNull("crs", crs);
        if (crs.length < 2) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.MISSING_PARAMETER_$1, "crs[" + crs.length + ']'));
        }
        final CoordinateSystem[] cs = new CoordinateSystem[crs.length];
        for (int i=0; i<crs.length; i++) {
            ensureNonNull("crs", i, crs);
            cs[i] = crs[i].getCoordinateSystem();
        }
        return new DefaultCompoundCS(cs);
    }

    /**
     * Returns an unmodifiable copy of the given list. As a side effect, this method computes the
     * {@linkplain singles} list. If it appears that the list of {@code SingleCRS} is equal to the
     * given list, then it is returned in other to share the same list in both {@link #crs} and
     * {@link #singles} references.
     * <p>
     * <strong>WARNING:</strong> this method is invoked by constructors <em>before</em>
     * the {@linkplain #crs} field is set. Do not use this field.
     */
    private List<? extends CoordinateReferenceSystem> copy(
            List<? extends CoordinateReferenceSystem> crs)
    {
        if (computeSingleCRS(crs)) {
            crs = singles; // Shares the same list.
        } else {
            crs = UnmodifiableArrayList.wrap(crs.toArray(new CoordinateReferenceSystem[crs.size()]));
        }
        return crs;
    }

    /**
     * The ordered list of coordinate reference systems.
     *
     * @return The coordinate reference systems as an unmodifiable list.
     */
    @Override
    @SuppressWarnings("unchecked") // We are safe if the list is read-only.
    public List<CoordinateReferenceSystem> getComponents() {
        return (List<CoordinateReferenceSystem>) crs;
    }

    /**
     * Returns the ordered list of single coordinate reference systems. If this compound CRS
     * contains other compound CRS, all of them are expanded in an array of {@code SingleCRS}
     * objects.
     *
     * @return The single coordinate reference systems as an unmodifiable list.
     */
    public List<SingleCRS> getSingleCRS() {
        return singles;
    }

    /**
     * Returns the ordered list of single coordinate reference systems for the specified CRS.
     * The specified CRS doesn't need to be a Geotk implementation.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The single coordinate reference systems, or an empty list if the
     *         given CRS is neither a {@link SingleCRS} or a {@link CompoundCRS}.
     */
    public static List<SingleCRS> getSingleCRS(final CoordinateReferenceSystem crs) {
        final List<SingleCRS> singles;
        if (crs instanceof DefaultCompoundCRS) {
            singles = ((DefaultCompoundCRS) crs).getSingleCRS();
        } else if (crs instanceof CompoundCRS) {
            final List<CoordinateReferenceSystem> elements =
                ((CompoundCRS) crs).getComponents();
            singles = new ArrayList<SingleCRS>(elements.size());
            getSingleCRS(elements, singles);
        } else if (crs instanceof SingleCRS) {
            singles = Collections.singletonList((SingleCRS) crs);
        } else {
            singles = Collections.emptyList();
        }
        return singles;
    }

    /**
     * Recursively adds all {@link SingleCRS} in the specified list.
     *
     * @throws ClassCastException if a CRS is neither a {@link SingleCRS} or a {@link CompoundCRS}.
     */
    private static boolean getSingleCRS(
            final List<? extends CoordinateReferenceSystem> source, final List<SingleCRS> target)
    {
        boolean identical = true;
        for (final CoordinateReferenceSystem candidate : source) {
            if (candidate instanceof CompoundCRS) {
                getSingleCRS(((CompoundCRS) candidate).getComponents(), target);
                identical = false;
            } else {
                target.add((SingleCRS) candidate);
            }
        }
        return identical;
    }

    /**
     * Computes the {@link #singles} field from the given CRS list and returns {@code true}
     * if it has the same content.
     */
    private boolean computeSingleCRS(List<? extends CoordinateReferenceSystem> crs) {
        singles = new ArrayList<SingleCRS>(crs.size());
        final boolean identical = getSingleCRS(crs, singles);
        singles = UnmodifiableArrayList.wrap(singles.toArray(new SingleCRS[singles.size()]));
        return identical;
    }

    /**
     * Computes the single CRS on deserialization.
     */
    @SuppressWarnings("unchecked")
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (crs instanceof CheckedCollection<?>) {
            final Class<?> type = ((CheckedCollection<?>) crs).getElementType();
            if (SingleCRS.class.isAssignableFrom(type)) {
                singles = (List<SingleCRS>) crs;
                return;
            }
        }
        computeSingleCRS(crs);
    }

    /**
     * Compares this coordinate reference system with the specified object for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    final DefaultCompoundCRS that = (DefaultCompoundCRS) object;
                    return Utilities.equals(this.crs, that.crs);
                }
                default: {
                    final CompoundCRS that = (CompoundCRS) object;
                    return deepEquals(getComponents(), that.getComponents(), mode);
                }
            }
        }
        return false;
    }

    /**
     * Returns a hash value for this compound CRS.
     *
     * @return The hash code value. This value doesn't need to be the same
     *         in past or future versions of this class.
     */
    @Override
    public int hashCode() {
        // Don't call superclass method since 'coordinateSystem' and 'datum' may be null.
        return crs.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Formats the inner part of a
     * <A HREF="http://www.geoapi.org/snapshot/javadoc/org/opengis/referencing/doc-files/WKT.html#COMPD_CS"><cite>Well
     * Known Text</cite> (WKT)</A> element.
     *
     * @param  formatter The formatter to use.
     * @return The name of the WKT element type, which is {@code "COMPD_CS"}.
     */
    @Override
    public String formatWKT(final Formatter formatter) {
        for (final CoordinateReferenceSystem element : crs) {
            formatter.append(element);
        }
        return "COMPD_CS";
    }
}
