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
 */
package org.geotoolkit.geometry;

import java.util.Arrays;
import java.io.Serializable;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;

import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.geometry.MismatchedReferenceSystemException;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.spatial.PixelOrientation;

import org.geotoolkit.resources.Errors;
import org.geotoolkit.util.Cloneable;
import org.geotoolkit.util.converter.Classes;
import org.geotoolkit.display.shape.XRectangle2D;
import org.geotoolkit.internal.InternalUtilities;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.geotoolkit.metadata.iso.spatial.PixelTranslation;

import static org.geotoolkit.util.ArgumentChecks.*;


/**
 * A minimum bounding box or rectangle. Regardless of dimension, an {@code Envelope} can
 * be represented without ambiguity as two {@linkplain DirectPosition direct positions}
 * (coordinate points). To encode an {@code Envelope}, it is sufficient to encode these
 * two points.
 *
 * {@note <code>Envelope</code> uses an arbitrary <cite>Coordinate Reference System</cite>, which
 * doesn't need to be geographic. This is different than the <code>GeographicBoundingBox</code>
 * class provided in the metadata package, which can be used as a kind of envelope restricted to
 * a Geographic CRS having Greenwich prime meridian.}
 *
 * This particular implementation of {@code Envelope} is said "General" because it
 * uses coordinates of an arbitrary dimension. This is in contrast with {@link Envelope2D},
 * which can use only two-dimensional coordinates.
 * <p>
 * A {@code GeneralEnvelope} can be created in various ways:
 * <p>
 * <ul>
 *   <li>{@linkplain #GeneralEnvelope(int) From a given number of dimension}, with all ordinates initialized to 0.</li>
 *   <li>{@linkplain #GeneralEnvelope(GeneralDirectPosition, GeneralDirectPosition) From two coordinate points}.</li>
 *   <li>{@linkplain #GeneralEnvelope(Envelope) From a an other envelope} (copy constructor).</li>
 *   <li>{@linkplain #GeneralEnvelope(GeographicBoundingBox) From a geographic bounding box}
 *       or a {@linkplain #GeneralEnvelope(Rectangle2D) Java2D rectangle}.</li>
 *   <li>{@linkplain #GeneralEnvelope(GridEnvelope, PixelInCell, MathTransform, CoordinateReferenceSystem)
 *       From a grid envelope} together with a <cite>Grid to CRS</cite> transform.</li>
 *   <li>{@linkplain #GeneralEnvelope(String) From a string} representing a {@code BBOX} in
 *       <cite>Well Known Text</cite> format.</li>
 * </ul>
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @author Simone Giannecchini (Geosolutions)
 * @version 3.20
 *
 * @see Envelope2D
 * @see org.geotoolkit.geometry.jts.ReferencedEnvelope
 * @see org.geotoolkit.metadata.iso.extent.DefaultGeographicBoundingBox
 *
 * @since 1.2
 * @module
 */
public class GeneralEnvelope extends ArrayEnvelope implements Cloneable, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1752330560227688940L;

    /**
     * Used for setting the {@link #ordinates} field during a {@link #clone()} operation only.
     * Will be fetch when first needed.
     */
    private static volatile Field ordinatesField;

    /**
     * Constructs an empty envelope of the specified dimension. All ordinates
     * are initialized to 0 and the coordinate reference system is undefined.
     *
     * @param dimension The envelope dimension.
     */
    public GeneralEnvelope(final int dimension) {
        super(dimension);
    }

    /**
     * Constructs one-dimensional envelope defined by a range of values.
     *
     * @param min The minimal value.
     * @param max The maximal value.
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point.
     */
    public GeneralEnvelope(final double min, final double max) throws IllegalArgumentException {
        super(min, max);
        ensureValidRanges(crs, ordinates);
    }

    /**
     * Constructs a envelope defined by two positions.
     *
     * @param  minDP Minimum ordinate values.
     * @param  maxDP Maximum ordinate values.
     * @throws MismatchedDimensionException if the two positions don't have the same dimension.
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point.
     */
    public GeneralEnvelope(final double[] minDP, final double[] maxDP) throws IllegalArgumentException {
        super(minDP, maxDP);
        ensureValidRanges(crs, ordinates);
    }

    /**
     * Constructs a envelope defined by two positions. The coordinate
     * reference system is inferred from the supplied direct position.
     *
     * @param  minDP Point containing minimum ordinate values.
     * @param  maxDP Point containing maximum ordinate values.
     * @throws MismatchedDimensionException if the two positions don't have the same dimension.
     * @throws MismatchedReferenceSystemException if the two positions don't use the same CRS.
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point
     *         (except for {@linkplain RangeMeaning#WRAPAROUND wraparound} axis).
     */
    public GeneralEnvelope(final GeneralDirectPosition minDP, final GeneralDirectPosition maxDP)
            throws MismatchedReferenceSystemException, IllegalArgumentException
    {
//  Uncomment next lines if Sun fixes RFE #4093999
//      ensureNonNull("minDP", minDP);
//      ensureNonNull("maxDP", maxDP);
        super(minDP.ordinates, maxDP.ordinates);
        crs = getCoordinateReferenceSystem(minDP, maxDP);
        AbstractDirectPosition.checkCoordinateReferenceSystemDimension(crs, ordinates.length >>> 1);
        ensureValidRanges(crs, ordinates);
    }

    /**
     * Constructs an empty envelope with the specified coordinate reference system.
     * All ordinates are initialized to 0.
     *
     * @param crs The coordinate reference system.
     *
     * @since 2.2
     */
    public GeneralEnvelope(final CoordinateReferenceSystem crs) {
//  Uncomment next line if Sun fixes RFE #4093999
//      ensureNonNull("crs", crs);
        super(crs.getCoordinateSystem().getDimension());
        this.crs = crs;
    }

    /**
     * Constructs a new envelope with the same data than the specified envelope.
     *
     * @param envelope The envelope to copy.
     */
    public GeneralEnvelope(final Envelope envelope) {
        super(envelope);
        ensureValidRanges(crs, ordinates);
    }

    /**
     * Constructs a new envelope with the same data than the specified
     * geographic bounding box. The coordinate reference system is set
     * to {@linkplain DefaultGeographicCRS#WGS84 WGS84}.
     *
     * @param box The bounding box to copy.
     *
     * @since 2.4
     */
    public GeneralEnvelope(final GeographicBoundingBox box) {
        super(box);
        ensureValidRanges(crs, ordinates);
    }

    /**
     * Constructs two-dimensional envelope defined by a {@link Rectangle2D}.
     * The coordinate reference system is initially undefined.
     *
     * @param rect The rectangle to copy.
     */
    public GeneralEnvelope(final Rectangle2D rect) {
        super(rect);
        ensureValidRanges(crs, ordinates);
    }

    /**
     * Constructs a georeferenced envelope from a grid envelope transformed using the specified
     * math transform. The <cite>grid to CRS</cite> transform should map either the
     * {@linkplain PixelInCell#CELL_CENTER cell center} (as in OGC convention) or
     * {@linkplain PixelInCell#CELL_CORNER cell corner} (as in Java2D/JAI convention)
     * depending on the {@code anchor} value. This constructor creates an envelope
     * containing entirely all pixels on a <cite>best effort</cite> basis - usually
     * accurate for affine transforms.
     * <p>
     * <b>Note:</b> The convention is specified as a {@link PixelInCell} code instead than
     * the more detailed {@link PixelOrientation}, because the later is restricted to the
     * two-dimensional case while the former can be used for any number of dimensions.
     * <p>
     * <b>Note:</b> The envelope created by this constructor is subject to rounding errors.
     * Consider invoking {@link #roundIfAlmostInteger(double, int)} after construction for
     * fixing them.
     *
     * @param gridEnvelope The grid envelope in integer coordinates.
     * @param anchor       Whatever grid coordinates map to pixel center or pixel corner.
     * @param gridToCRS    The transform (usually affine) from grid envelope to the CRS.
     * @param crs          The CRS for the envelope to be created, or {@code null} if unknown.
     *
     * @throws MismatchedDimensionException If one of the supplied object doesn't have
     *         a dimension compatible with the other objects.
     * @throws IllegalArgumentException if an argument is illegal for some other reason,
     *         including failure to use the provided math transform.
     *
     * @since 2.3
     *
     * @see org.geotoolkit.referencing.operation.builder.GridToEnvelopeMapper
     * @see org.geotoolkit.coverage.grid.GeneralGridEnvelope#GeneralGridEnvelope(Envelope,PixelInCell,boolean)
     */
    public GeneralEnvelope(final GridEnvelope  gridEnvelope,
                           final PixelInCell   anchor,
                           final MathTransform gridToCRS,
                           final CoordinateReferenceSystem crs)
            throws IllegalArgumentException
    {
//  Uncomment next line if Sun fixes RFE #4093999
//      ensureNonNull("gridEnvelope", gridEnvelope);
        super(gridEnvelope.getDimension());
        ensureNonNull("gridToCRS", gridToCRS);
        final int dimension = getDimension();
        ensureSameDimension(dimension, gridToCRS.getSourceDimensions());
        ensureSameDimension(dimension, gridToCRS.getTargetDimensions());
        final double offset = PixelTranslation.getPixelTranslation(anchor) + 0.5;
        for (int i=0; i<dimension; i++) {
            /*
             * According OpenGIS specification, GridGeometry maps pixel's center. We want a bounding
             * box for all pixels, not pixel's centers. Offset by 0.5 (use -0.5 for maximum too, not
             * +0.5, since maximum is exclusive).
             *
             * Note: the offset of 1 after getHigh(i) is because high values are inclusive according
             *       ISO specification, while our algorithm and Java usage expect exclusive values.
             */
            setRange(i, gridEnvelope.getLow(i) - offset, gridEnvelope.getHigh(i) - (offset - 1));
        }
        final ArrayEnvelope transformed;
        try {
            transformed = Envelopes.transform(gridToCRS, this);
        } catch (TransformException exception) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.BAD_TRANSFORM_$1,
                    Classes.getClass(gridToCRS)), exception);
        }
        assert transformed.ordinates.length == this.ordinates.length;
        System.arraycopy(transformed.ordinates, 0, this.ordinates, 0, ordinates.length);
        setCoordinateReferenceSystem(crs);
    }

    /**
     * Constructs a new envelope initialized to the values parsed from the given string in
     * <cite>Well Known Text</cite> (WKT) format. The given string is typically a {@code BOX}
     * element like below:
     *
     * {@preformat wkt
     *     BOX(-180 -90, 180 90)
     * }
     *
     * However this constructor is lenient to other geometry types like {@code POLYGON}.
     * Actually this constructor ignores the geometry type and just applies the following
     * simple rules:
     * <p>
     * <ul>
     *   <li>Character sequences complying to the rules of Java identifiers are skipped.</li>
     *   <li>Coordinates are separated by a coma ({@code ,}) character.</li>
     *   <li>The ordinates in a coordinate are separated by a space.</li>
     *   <li>Ordinate numbers are assumed formatted in US locale.</li>
     *   <li>The coordinate having the highest dimension determines the dimension of this envelope.</li>
     * </ul>
     * <p>
     * This constructor does not check the consistency of the provided WKT. For example it doesn't
     * check that every points in a {@code LINESTRING} have the same dimension. However this
     * constructor ensures that the parenthesis are balanced, in order to catch some malformed WKT.
     * <p>
     * The following examples can be parsed by this constructor in addition of the standard
     * {@code BOX} element. This constructor creates the bounding box of those geometries:
     * <p>
     * <ul>
     *   <li>{@code POINT(6 10)}</li>
     *   <li>{@code MULTIPOLYGON(((1 1, 5 1, 1 5, 1 1),(2 2, 3 2, 3 3, 2 2)))}</li>
     *   <li>{@code GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(3 8,7 10))}</li>
     * </ul>
     *
     * @param  wkt The {@code BOX}, {@code POLYGON} or other kind of element to parse.
     * @throws NumberFormatException If a number can not be parsed.
     * @throws IllegalArgumentException If the parenthesis are not balanced.
     *
     * @see Envelopes#parseWKT(String)
     * @see Envelopes#toWKT(Envelope)
     *
     * @since 3.09
     */
    public GeneralEnvelope(final String wkt) throws NumberFormatException, IllegalArgumentException {
        super(wkt);
        ensureValidRanges(crs, ordinates);
    }

    /**
     * Returns the given envelope as a {@code GeneralEnvelope} instance. If the given envelope
     * is already an instance of {@code GeneralEnvelope}, then it is returned unchanged.
     * Otherwise the coordinate values and the CRS of the given envelope are
     * {@linkplain #GeneralEnvelope(Envelope) copied} in a new {@code GeneralEnvelope}.
     *
     * @param  envelope The envelope to cast, or {@code null}.
     * @return The values of the given envelope as a {@code GeneralEnvelope} instance.
     *
     * @since 3.19
     */
    public static GeneralEnvelope castOrCopy(final Envelope envelope) {
        if (envelope == null || envelope instanceof GeneralEnvelope) {
            return (GeneralEnvelope) envelope;
        }
        return new GeneralEnvelope(envelope);
    }

    /**
     * Returns the number of dimensions.
     */
    @Override
    public final int getDimension() {
        return super.getDimension();
    }

    /**
     * Returns the coordinate reference system in which the coordinates are given.
     *
     * @return The coordinate reference system, or {@code null}.
     */
    @Override
    public final CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return super.getCoordinateReferenceSystem();
    }

    /**
     * Sets the coordinate reference system in which the coordinate are given.
     * This method <strong>does not</strong> reproject the envelope, and do not
     * check if the envelope is contained in the new domain of validity. The
     * later can be enforced by a call to {@link #reduceToDomain(boolean)}.
     * <p>
     * If the envelope coordinates need to be transformed to the new CRS, consider
     * using {@link Envelopes#transform(Envelope, CoordinateReferenceSystem)} instead.
     *
     * @param  crs The new coordinate reference system, or {@code null}.
     * @throws MismatchedDimensionException if the specified CRS doesn't have the expected
     *         number of dimensions.
     */
    public void setCoordinateReferenceSystem(final CoordinateReferenceSystem crs)
            throws MismatchedDimensionException
    {
        AbstractDirectPosition.checkCoordinateReferenceSystemDimension(crs, getDimension());
        this.crs = crs;
    }

    /**
     * Restricts this envelope to the CS or CRS
     * {@linkplain CoordinateReferenceSystem#getDomainOfValidity domain of validity}.
     * This method performs two steps:
     *
     * <ol>
     *   <li><p>Ensure that the envelope is contained in the {@linkplain CoordinateSystem
     *   coordinate system} domain. If some ordinates are out of range, then there is a choice
     *   depending on the {@linkplain CoordinateSystemAxis#getRangeMeaning range meaning}:
     *   <ul>
     *     <li>If {@linkplain RangeMeaning#EXACT EXACT} (typically <em>latitudes</em> ordinates),
     *     values greater than the {@linkplain CoordinateSystemAxis#getMaximumValue maximum value}
     *     are replaced by the maximum, and values smaller than the
     *     {@linkplain CoordinateSystemAxis#getMinimumValue minimum value}
     *     are replaced by the minimum.</li>
     *
     *     <li>If {@linkplain RangeMeaning#WRAPAROUND WRAPAROUND} (typically <em>longitudes</em>
     *     ordinates), a multiple of the range (e.g. 360° for longitudes) is added or subtracted.
     *     If a value stay out of range after this correction, then the ordinates are set to the
     *     full [{@linkplain CoordinateSystemAxis#getMinimumValue minimum} &hellip;
     *     {@linkplain CoordinateSystemAxis#getMaximumValue maximum}] range.
     *     See the example below.</li>
     *   </ul>
     *   </p></li>
     *   <li><p>If {@code crsDomain} is {@code true}, then the envelope from the previous step
     *   is intersected with the CRS {@linkplain CoordinateReferenceSystem#getDomainOfValidity
     *   domain of validity}, if any.
     *   </p></li>
     * </ol>
     *
     * <b>Example:</b> A longitude range of [185° &hellip; 190°] is equivalent to [-175° &hellip; -170°].
     * But [175° &hellip; 185°] would be equivalent to [175° &hellip; -175°], which is likely to mislead
     * <code>Envelope</code> users since the lower bounds is numerically greater than the upper bounds.
     * Reordering as [-175° &hellip; 175°] would interchange the meaning of what is "inside" and "outside"
     * the envelope. So this implementation conservatively expands the range to [-180° &hellip; 180°]
     * in order to ensure that the validated envelope fully contains the original envelope.
     *
     * @param  useDomainOfCRS {@code true} if the envelope should be restricted to
     *         the CRS <cite>domain of validity</cite> in addition to the CS domain.
     * @return {@code true} if this envelope has been modified, or {@code false} if no change
     *         was done.
     *
     * @since 3.11 (derived from 2.5)
     */
    public boolean reduceToDomain(final boolean useDomainOfCRS) {
        boolean changed = false;
        if (crs != null) {
            final int dimension = ordinates.length >>> 1;
            final CoordinateSystem cs = crs.getCoordinateSystem();
            for (int i=0; i<dimension; i++) {
                final int j = i + dimension;
                final CoordinateSystemAxis axis = cs.getAxis(i);
                final double  minimum = axis.getMinimumValue();
                final double  maximum = axis.getMaximumValue();
                final RangeMeaning rm = axis.getRangeMeaning();
                if (RangeMeaning.EXACT.equals(rm)) {
                    if (ordinates[i] < minimum) {ordinates[i] = minimum; changed = true;}
                    if (ordinates[j] > maximum) {ordinates[j] = maximum; changed = true;}
                } else if (RangeMeaning.WRAPAROUND.equals(rm)) {
                    final double length = maximum - minimum;
                    if (length > 0 && length < Double.POSITIVE_INFINITY) {
                        final double offset = Math.floor((ordinates[i] - minimum) / length) * length;
                        if (offset != 0) {
                            ordinates[i] -= offset;
                            ordinates[j] -= offset;
                            changed = true;
                        }
                        if (ordinates[j] > maximum) {
                            ordinates[i] = minimum; // See method Javadoc
                            ordinates[j] = maximum;
                            changed = true;
                        }
                    }
                }
            }
            if (useDomainOfCRS) {
                final Envelope domain = Envelopes.getDomainOfValidity(crs);
                if (domain != null) {
                    final CoordinateReferenceSystem domainCRS = domain.getCoordinateReferenceSystem();
                    if (domainCRS == null) {
                        intersect(domain);
                    } else {
                        /*
                         * The domain may have fewer dimensions than this envelope (typically only
                         * the ones relative to horizontal dimensions).  We can rely on directions
                         * for matching axis since CRS.getEnvelope(crs) should have transformed the
                         * domain to this envelope CRS.
                         */
                        final CoordinateSystem domainCS = domainCRS.getCoordinateSystem();
                        final int domainDimension = domainCS.getDimension();
                        for (int i=0; i<domainDimension; i++) {
                            final double minimum = domain.getMinimum(i);
                            final double maximum = domain.getMaximum(i);
                            final AxisDirection direction = domainCS.getAxis(i).getDirection();
                            for (int j=0; j<dimension; j++) {
                                if (direction.equals(cs.getAxis(j).getDirection())) {
                                    final int k = j + dimension;
                                    if (ordinates[j] < minimum) ordinates[j] = minimum;
                                    if (ordinates[k] > maximum) ordinates[k] = maximum;
                                }
                            }
                        }
                    }
                }
            }
        }
        return changed;
    }

    /**
     * Fixes rounding errors up to a given tolerance level. For each value {@code ordinates[i]}
     * at dimension <var>i</var>, this method multiplies the ordinate value by the given factor,
     * then round the result only if the product is close to an integer value. The threshold is
     * defined by the {@code maxULP} argument in ULP units (<cite>Unit in the Last Place</cite>).
     * If and only if the product has been rounded, it is divided by the factor and stored in this
     * envelope in place of the original ordinate.
     * <p>
     * This method is useful after envelope calculations subject to rounding errors, like the
     * {@link #GeneralEnvelope(GridEnvelope, PixelInCell, MathTransform, CoordinateReferenceSystem)}
     * constructor.
     *
     * @param factor The factor by which to multiply ordinates before rounding
     *               and divide after rounding. A recommended value is 360.
     * @param maxULP The maximal change allowed in ULPs (Unit in the Last Place).
     *
     * @since 3.11
     */
    public void roundIfAlmostInteger(final double factor, final int maxULP) {
        ensureStrictlyPositive("factor", factor);
        for (int i=0; i<ordinates.length; i++) {
            ordinates[i] = InternalUtilities.adjustForRoundingError(ordinates[i], factor, maxULP);
        }
    }

    /**
     * Returns the minimal ordinate along the specified dimension.
     *
     * @param  dimension The dimension to query.
     * @return The minimal ordinate value along the given dimension.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     */
    @Override
    public final double getMinimum(final int dimension) throws IndexOutOfBoundsException {
        return super.getMinimum(dimension);
    }

    /**
     * Returns the maximal ordinate along the specified dimension.
     *
     * @param  dimension The dimension to query.
     * @return The maximal ordinate value along the given dimension.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     */
    @Override
    public final double getMaximum(final int dimension) throws IndexOutOfBoundsException {
        return super.getMaximum(dimension);
    }

    /**
     * Sets the envelope range along the specified dimension.
     *
     * @param  dimension The dimension to set.
     * @param  minimum   The minimum value along the specified dimension.
     * @param  maximum   The maximum value along the specified dimension.
     * @throws IndexOutOfBoundsException If the given index is out of bounds.
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point
     *         (except for {@linkplain RangeMeaning#WRAPAROUND wraparound} axis).
     */
    public void setRange(final int dimension, final double minimum, final double maximum)
            throws IndexOutOfBoundsException, IllegalArgumentException
    {
        ensureValidIndex(ordinates.length >>> 1, dimension);
        ensureValidRange(crs, dimension, minimum, maximum);
        ordinates[dimension + (ordinates.length >>> 1)] = maximum;
        ordinates[dimension]                            = minimum;
    }

    /**
     * Sets the envelope to the specified values, which must be the lower corner coordinates
     * followed by upper corner coordinates. The number of arguments provided shall be twice
     * this {@linkplain #getDimension envelope dimension}, and minimum shall not be greater
     * than maximum.
     * <p>
     * <b>Example:</b>
     * (<var>x</var><sub>min</sub>, <var>y</var><sub>min</sub>, <var>z</var><sub>min</sub>,
     *  <var>x</var><sub>max</sub>, <var>y</var><sub>max</sub>, <var>z</var><sub>max</sub>)
     *
     * @param ordinates The new ordinate values.
     * @throws IllegalArgumentException if an ordinate value in the minimum point is not
     *         less than or equal to the corresponding ordinate value in the maximum point
     *         (except for {@linkplain RangeMeaning#WRAPAROUND wraparound} axis).
     *
     * @since 2.5
     */
    public void setEnvelope(final double... ordinates) throws IllegalArgumentException {
        if ((ordinates.length & 1) != 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.ODD_ARRAY_LENGTH_$1, ordinates.length));
        }
        final int dimension  = ordinates.length >>> 1;
        final int check = this.ordinates.length >>> 1;
        if (dimension != check) {
            throw new MismatchedDimensionException(Errors.format(
                    Errors.Keys.MISMATCHED_DIMENSION_$3, "ordinates", dimension, check));
        }
        ensureValidRanges(crs, ordinates);
        System.arraycopy(ordinates, 0, this.ordinates, 0, ordinates.length);
    }

    /**
     * Sets this envelope to the same coordinate values than the specified envelope.
     * If the given envelope has a non-null Coordinate Reference System (CRS), then
     * the CRS of this envelope will be set to the CRS of the given envelope.
     *
     * @param  envelope The envelope to copy coordinates from.
     * @throws MismatchedDimensionException if the specified envelope doesn't have the expected
     *         number of dimensions.
     *
     * @since 2.2
     */
    public void setEnvelope(final Envelope envelope) throws MismatchedDimensionException {
        ensureNonNull("envelope", envelope);
        final int dimension = ordinates.length >>> 1;
        AbstractDirectPosition.ensureDimensionMatch("envelope", envelope.getDimension(), dimension);
        if (envelope instanceof GeneralEnvelope) {
            System.arraycopy(((GeneralEnvelope) envelope).ordinates, 0, ordinates, 0, ordinates.length);
        } else {
            for (int i=0; i<dimension; i++) {
                ordinates[i]           = envelope.getMinimum(i);
                ordinates[i+dimension] = envelope.getMaximum(i);
            }
        }
        final CoordinateReferenceSystem envelopeCRS = envelope.getCoordinateReferenceSystem();
        if (envelopeCRS != null) {
            crs = envelopeCRS;
            assert crs.getCoordinateSystem().getDimension() == getDimension() : crs;
            assert envelope.getClass() != getClass() || equals(envelope) : envelope;
        }
    }

    /**
     * Sets a sub-domain of this envelope to the same coordinate values than the specified envelope.
     * This method copies the ordinate values of all dimensions from the given envelope to some
     * dimensions of this envelope. The target dimensions in this envelope range from {@code lower}
     * inclusive to <code>lower + {@linkplain #getDimension()}</code> exclusive.
     * <p>
     * This method ignores the Coordinate Reference System of {@code this} and the given envelope.
     *
     * @param  envelope The envelope to copy coordinates from.
     * @param  offset Index of the first dimension to write in this envelope.
     * @throws IndexOutOfBoundsException If the given offset is negative, or is greater than
     *         <code>getDimension() - envelope.getDimension()</code>.
     *
     * @since 3.16
     */
    public void setSubEnvelope(final Envelope envelope, int offset) throws IndexOutOfBoundsException {
        ensureNonNull("envelope", envelope);
        final int subDim = envelope.getDimension();
        final int dimension = ordinates.length >>> 1;
        if (offset < 0 || offset + subDim > dimension) {
            throw new IndexOutOfBoundsException(Errors.format(
                    Errors.Keys.ILLEGAL_ARGUMENT_$2, "lower", offset));
        }
        for (int i=0; i<subDim; i++) {
            ordinates[offset]           = envelope.getMinimum(i);
            ordinates[offset+dimension] = envelope.getMaximum(i);
            offset++;
        }
    }

    /**
     * Returns a new envelope that encompass only some dimensions of this envelope.
     * This method copy this envelope ordinates into a new envelope, beginning at
     * dimension {@code lower} and extending to dimension {@code upper-1}.
     * Thus the dimension of the sub-envelope is {@code upper-lower}.
     *
     * @param  lower The first dimension to copy, inclusive.
     * @param  upper The last  dimension to copy, exclusive.
     * @return The sub-envelope.
     * @throws IndexOutOfBoundsException if an index is out of bounds.
     */
    public GeneralEnvelope getSubEnvelope(final int lower, final int upper)
            throws IndexOutOfBoundsException
    {
        final int curDim = ordinates.length >>> 1;
        final int newDim = upper-lower;
        if (lower<0 || lower>curDim) {
            throw new IndexOutOfBoundsException(Errors.format(
                    Errors.Keys.ILLEGAL_ARGUMENT_$2, "lower", lower));
        }
        if (newDim<0 || upper>curDim) {
            throw new IndexOutOfBoundsException(Errors.format(
                    Errors.Keys.ILLEGAL_ARGUMENT_$2, "upper", upper));
        }
        final GeneralEnvelope envelope = new GeneralEnvelope(newDim);
        System.arraycopy(ordinates, lower,        envelope.ordinates, 0,      newDim);
        System.arraycopy(ordinates, lower+curDim, envelope.ordinates, newDim, newDim);
        return envelope;
    }

    /**
     * Returns a new envelope with the same values than this envelope minus the
     * specified range of dimensions.
     *
     * @param  lower The first dimension to omit, inclusive.
     * @param  upper The last  dimension to omit, exclusive.
     * @return The sub-envelope.
     * @throws IndexOutOfBoundsException if an index is out of bounds.
     */
    public GeneralEnvelope getReducedEnvelope(final int lower, final int upper)
            throws IndexOutOfBoundsException
    {
        final int curDim = ordinates.length >>> 1;
        final int rmvDim = upper-lower;
        if (lower<0 || lower>curDim) {
            throw new IndexOutOfBoundsException(Errors.format(
                    Errors.Keys.ILLEGAL_ARGUMENT_$2, "lower", lower));
        }
        if (rmvDim<0 || upper>curDim) {
            throw new IndexOutOfBoundsException(Errors.format(
                    Errors.Keys.ILLEGAL_ARGUMENT_$2, "upper", upper));
        }
        final GeneralEnvelope envelope = new GeneralEnvelope(curDim - rmvDim);
        System.arraycopy(ordinates, 0,     envelope.ordinates, 0,            lower);
        System.arraycopy(ordinates, lower, envelope.ordinates, upper, curDim-upper);
        return envelope;
    }

    /**
     * Sets the lower corner to {@linkplain Double#NEGATIVE_INFINITY negative infinity}
     * and the upper corner to {@linkplain Double#POSITIVE_INFINITY positive infinity}.
     * The {@linkplain #getCoordinateReferenceSystem coordinate reference system} (if any)
     * stay unchanged.
     *
     * @since 2.2
     */
    public void setToInfinite() {
        final int mid = ordinates.length >>> 1;
        Arrays.fill(ordinates, 0,   mid,              Double.NEGATIVE_INFINITY);
        Arrays.fill(ordinates, mid, ordinates.length, Double.POSITIVE_INFINITY);
        assert isInfinite() : this;
    }

    /**
     * Returns {@code true} if at least one ordinate has an
     * {@linkplain Double#isInfinite infinite} value.
     *
     * @return {@code true} if this envelope has infinite value.
     *
     * @since 2.2
     */
    public boolean isInfinite() {
        for (int i=0; i<ordinates.length; i++) {
            if (Double.isInfinite(ordinates[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets all ordinate values to {@linkplain Double#NaN NaN}. The
     * {@linkplain #getCoordinateReferenceSystem coordinate reference system} (if any) stay
     * unchanged.
     *
     * @since 2.2
     */
    public void setToNull() {
        Arrays.fill(ordinates, Double.NaN);
        assert isNull() : this;
    }

    /**
     * Returns {@code false} if at least one ordinate value is not {@linkplain Double#NaN NaN}. The
     * {@code isNull()} check is a little bit different than {@link #isEmpty()} since it returns
     * {@code false} for a partially initialized envelope, while {@code isEmpty()} returns
     * {@code false} only after all dimensions have been initialized. More specifically, the
     * following rules apply:
     * <p>
     * <ul>
     *   <li>If <code>isNull() == true</code>, then <code>{@linkplain #isEmpty()} == true</code></li>
     *   <li>If <code>{@linkplain #isEmpty()} == false</code>, then <code>isNull() == false</code></li>
     *   <li>The converse of the above-cited rules are not always true.</li>
     * </ul>
     *
     * @return {@code true} if this envelope has NaN values.
     *
     * @since 2.2
     */
    public boolean isNull() {
        for (int i=0; i<ordinates.length; i++) {
            if (!Double.isNaN(ordinates[i])) {
                return false;
            }
        }
        assert isEmpty() : this;
        return true;
    }

    /**
     * Determines whether or not this envelope is empty. An envelope is non-empty only if it has
     * at least one {@linkplain #getDimension() dimension}, and the {@linkplain #getSpan(int) span}
     * is greater than 0 along all dimensions. Note that a non-empty envelope is always
     * non-{@linkplain #isNull() null}, but the converse is not always true.
     *
     * @return {@code true} if this envelope is empty.
     */
    public boolean isEmpty() {
        final int dimension = ordinates.length >>> 1;
        if (dimension == 0) {
            return true;
        }
        for (int i=0; i<dimension; i++) {
            if (!(ordinates[i] < ordinates[i+dimension])) { // Use '!' in order to catch NaN
                return true;
            }
        }
        assert !isNull() : this;
        return false;
    }

    /**
     * Returns {@code true} if at least one ordinate in the given position
     * is {@link Double#NaN}. This is used for assertions only.
     */
    private static boolean hasNaN(final DirectPosition position) {
        for (int i=position.getDimension(); --i>=0;) {
            if (Double.isNaN(position.getOrdinate(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if at least one ordinate in the given envelope
     * is {@link Double#NaN}. This is used for assertions only.
     */
    private static boolean hasNaN(final Envelope envelope) {
        return hasNaN(envelope.getLowerCorner()) || hasNaN(envelope.getUpperCorner());
    }

    /**
     * Adds to this envelope a point of the given array.
     *
     * @param  array The array which contains the ordinate values.
     * @param  offset Index of the first valid ordinate value in the given array.
     */
    final void add(final double[] array, final int offset) {
        final int dim = ordinates.length >>> 1;
        for (int i=0; i<dim; i++) {
            final double value = array[offset + i];
            if (value < ordinates[i    ]) ordinates[i    ] = value;
            if (value > ordinates[i+dim]) ordinates[i+dim] = value;
        }
    }

    /**
     * Adds a point to this envelope. The resulting envelope is the smallest envelope that
     * contains both the original envelope and the specified point.
     * <p>
     * After adding a point, a call to {@link #contains(DirectPosition) contains(DirectPosition)}
     * with the added point as an argument will return {@code true}, except if one of the point
     * ordinates was {@link Double#NaN} (in which case the corresponding ordinate have been ignored).
     *
     * {@note This method assumes that the specified point uses the same CRS than this envelope.
     *        For performance reason, it will no be verified unless Java assertions are enabled.}
     *
     * @param  position The point to add.
     * @throws MismatchedDimensionException if the specified point doesn't have
     *         the expected dimension.
     */
    public void add(final DirectPosition position) throws MismatchedDimensionException {
        ensureNonNull("position", position);
        final int dim = ordinates.length >>> 1;
        AbstractDirectPosition.ensureDimensionMatch("position", position.getDimension(), dim);
        assert equalsIgnoreMetadata(crs, position.getCoordinateReferenceSystem()) : position;
        for (int i=0; i<dim; i++) {
            final double value = position.getOrdinate(i);
            if (value < ordinates[i    ]) ordinates[i    ] = value;
            if (value > ordinates[i+dim]) ordinates[i+dim] = value;
        }
        assert isEmpty() || contains(position) || hasNaN(position) : position;
    }

    /**
     * Adds an envelope object to this envelope. The resulting envelope is the union of the
     * two {@code Envelope} objects.
     *
     * {@note This method assumes that the specified envelope uses the same CRS than this envelope.
     *        For performance reason, it will no be verified unless Java assertions are enabled.}
     *
     * @param  envelope the {@code Envelope} to add to this envelope.
     * @throws MismatchedDimensionException if the specified envelope doesn't
     *         have the expected dimension.
     */
    public void add(final Envelope envelope) throws MismatchedDimensionException {
        ensureNonNull("envelope", envelope);
        final int dim = ordinates.length >>> 1;
        AbstractDirectPosition.ensureDimensionMatch("envelope", envelope.getDimension(), dim);
        assert equalsIgnoreMetadata(crs, envelope.getCoordinateReferenceSystem()) : envelope;
        for (int i=0; i<dim; i++) {
            final double min = envelope.getMinimum(i);
            final double max = envelope.getMaximum(i);
            if (min < ordinates[i    ]) ordinates[i    ] = min;
            if (max > ordinates[i+dim]) ordinates[i+dim] = max;
        }
        assert isEmpty() || contains(envelope, true) || hasNaN(envelope) : envelope;
    }

    /**
     * Returns {@code true} if this envelope completely encloses the specified envelope.
     * If one or more edges from the specified envelope coincide with an edge from this
     * envelope, then this method returns {@code true} only if {@code edgesInclusive}
     * is {@code true}.
     *
     * {@note This method assumes that the specified envelope uses the same CRS than this envelope.
     *        For performance reason, it will no be verified unless Java assertions are enabled.}
     *
     * @param  envelope The envelope to test for inclusion.
     * @param  edgesInclusive {@code true} if this envelope edges are inclusive.
     * @return {@code true} if this envelope completely encloses the specified one.
     * @throws MismatchedDimensionException if the specified envelope doesn't have
     *         the expected dimension.
     *
     * @see #intersects(Envelope, boolean)
     * @see #equals(Envelope, double, boolean)
     *
     * @since 2.2
     */
    public boolean contains(final Envelope envelope, final boolean edgesInclusive)
            throws MismatchedDimensionException
    {
        ensureNonNull("envelope", envelope);
        final int dim = ordinates.length >>> 1;
        AbstractDirectPosition.ensureDimensionMatch("envelope", envelope.getDimension(), dim);
        assert equalsIgnoreMetadata(crs, envelope.getCoordinateReferenceSystem()) : envelope;
        for (int i=0; i<dim; i++) {
            double inner = envelope.getMinimum(i);
            double outer = ordinates[i];
            if (!(edgesInclusive ? inner >= outer : inner > outer)) { // ! is for catching NaN.
                return false;
            }
            inner = envelope.getMaximum(i);
            outer = ordinates[i+dim];
            if (!(edgesInclusive ? inner <= outer : inner < outer)) { // ! is for catching NaN.
                return false;
            }
        }
        assert intersects(envelope, edgesInclusive) || hasNaN(envelope) : envelope;
        return true;
    }

    /**
     * Returns {@code true} if this envelope intersects the specified envelope.
     * If one or more edges from the specified envelope coincide with an edge from this
     * envelope, then this method returns {@code true} only if {@code edgesInclusive}
     * is {@code true}.
     *
     * {@note This method assumes that the specified envelope uses the same CRS than this envelope.
     *        For performance reason, it will no be verified unless Java assertions are enabled.}
     *
     * @param  envelope The envelope to test for intersection.
     * @param  edgesInclusive {@code true} if this envelope edges are inclusive.
     * @return {@code true} if this envelope intersects the specified one.
     * @throws MismatchedDimensionException if the specified envelope doesn't have
     *         the expected dimension.
     *
     * @see #contains(Envelope, boolean)
     * @see #equals(Envelope, double, boolean)
     *
     * @since 2.2
     */
    public boolean intersects(final Envelope envelope, final boolean edgesInclusive)
            throws MismatchedDimensionException
    {
        ensureNonNull("envelope", envelope);
        final int dim = ordinates.length >>> 1;
        AbstractDirectPosition.ensureDimensionMatch("envelope", envelope.getDimension(), dim);
        assert equalsIgnoreMetadata(crs, envelope.getCoordinateReferenceSystem()) : envelope;
        for (int i=0; i<dim; i++) {
            double inner = envelope.getMaximum(i);
            double outer = ordinates[i];
            if (!(edgesInclusive ? inner >= outer : inner > outer)) { // ! is for catching NaN.
                return false;
            }
            inner = envelope.getMinimum(i);
            outer = ordinates[i+dim];
            if (!(edgesInclusive ? inner <= outer : inner < outer)) { // ! is for catching NaN.
                return false;
            }
        }
        return true;
    }

    /**
     * Sets this envelope to the intersection if this envelope with the specified one.
     *
     * {@note This method assumes that the specified envelope uses the same CRS than this envelope.
     *        For performance reason, it will no be verified unless Java assertions are enabled.}
     *
     * @param  envelope the {@code Envelope} to intersect to this envelope.
     * @throws MismatchedDimensionException if the specified envelope doesn't
     *         have the expected dimension.
     */
    public void intersect(final Envelope envelope) throws MismatchedDimensionException {
        ensureNonNull("envelope", envelope);
        final int dim = ordinates.length >>> 1;
        AbstractDirectPosition.ensureDimensionMatch("envelope", envelope.getDimension(), dim);
        assert equalsIgnoreMetadata(crs, envelope.getCoordinateReferenceSystem()) : envelope;
        for (int i=0; i<dim; i++) {
            double min = Math.max(ordinates[i    ], envelope.getMinimum(i));
            double max = Math.min(ordinates[i+dim], envelope.getMaximum(i));
            if (min > max) {
                // Make an empty envelope (min==max)
                // while keeping it legal (min<=max).
                min = max = 0.5*(min+max);
            }
            ordinates[i    ] = min;
            ordinates[i+dim] = max;
        }
    }

    /**
     * Returns a {@link Rectangle2D} with the same bounds as this {@code Envelope}.
     * This envelope must be two-dimensional before this method is invoked.
     * This is a convenience method for inter-operability with Java2D.
     *
     * @return This envelope as a two-dimensional rectangle.
     * @throws IllegalStateException if this envelope is not two-dimensional.
     */
    public Rectangle2D toRectangle2D() throws IllegalStateException {
        /*
         * NOTE: if the type created below is changed to something else than XRectangle2D, then we
         *       must perform a usage search  because some client code cast the returned object to
         *       XRectangle2D when this envelope is known to not be a subclass of GeneralEnvelope.
         */
        if (ordinates.length == 4) {
            return XRectangle2D.createFromExtremums(ordinates[0], ordinates[1],
                                                    ordinates[2], ordinates[3]);
        } else {
            throw new IllegalStateException(Errors.format(
                    Errors.Keys.NOT_TWO_DIMENSIONAL_$1, getDimension()));
        }
    }

    /**
     * Returns a deep copy of this envelope.
     *
     * @return A clone of this envelope.
     */
    @Override
    public GeneralEnvelope clone() {
        try {
            Field field = ordinatesField;
            if (field == null) {
                field = ArrayEnvelope.class.getDeclaredField("ordinates");
                field.setAccessible(true);
                ordinatesField = field;
            }
            GeneralEnvelope e = (GeneralEnvelope) super.clone();
            field.set(e, ordinates.clone());
            return e;
        } catch (CloneNotSupportedException | ReflectiveOperationException exception) {
            // Should not happen, since we are cloneable, the
            // field is known to exist and we made it accessible.
            throw new AssertionError(exception);
        }
    }
}
