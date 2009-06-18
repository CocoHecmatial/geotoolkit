/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
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
package org.geotoolkit.referencing;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.RenderingHints;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opengis.geometry.*;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.opengis.metadata.extent.*;
import org.opengis.metadata.citation.Citation;

import org.geotoolkit.lang.Static;
import org.geotoolkit.util.Version;
import org.geotoolkit.util.logging.Logging;
import org.geotoolkit.util.UnsupportedImplementationException;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.factory.Factory;
import org.geotoolkit.factory.Factories;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.factory.AuthorityFactoryFinder;
import org.geotoolkit.factory.FactoryNotFoundException;
import org.geotoolkit.factory.FactoryRegistryException;
import org.geotoolkit.display.shape.XRectangle2D;
import org.geotoolkit.geometry.Envelope2D;
import org.geotoolkit.geometry.GeneralEnvelope;
import org.geotoolkit.geometry.GeneralDirectPosition;
import org.geotoolkit.metadata.iso.citation.Citations;
import org.geotoolkit.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.geotoolkit.referencing.cs.DefaultEllipsoidalCS;
import org.geotoolkit.referencing.cs.DefaultCoordinateSystemAxis;
import org.geotoolkit.referencing.factory.AbstractAuthorityFactory;
import org.geotoolkit.referencing.factory.IdentifiedObjectFinder;
import org.geotoolkit.referencing.operation.projection.UnitaryProjection;
import org.geotoolkit.referencing.operation.transform.IdentityTransform;
import org.geotoolkit.referencing.operation.transform.AbstractMathTransform;
import org.geotoolkit.internal.referencing.CRSUtilities;
import org.geotoolkit.naming.DefaultNameSpace;
import org.geotoolkit.resources.Errors;


/**
 * Simple utility class for making use of the {@linkplain CoordinateReferenceSystem
 * Coordinate Reference System} and associated {@linkplain org.opengis.referencing.Factory}
 * implementations. This utility class is made up of static final functions. This class is
 * not a factory or a builder. It makes use of the GeoAPI factory interfaces provided by
 * {@link FactoryFinder}.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @author Jody Garnett (Refractions)
 * @author Andrea Aime (TOPP)
 * @version 3.01
 *
 * @since 2.1
 * @module
 */
@Static
public final class CRS {
    /**
     * The CRS factory to use for parsing WKT. Will be fetched when first needed
     * are stored for avoiding indirect synchronization lock in {@link #parseWKT}.
     */
    private static volatile CRSFactory crsFactory;

    /**
     * A factory for CRS creation as specified by the authority, which may have
     * (<var>latitude</var>, <var>longitude</var>) axis order. Will be created
     * only when first needed.
     */
    private static volatile CRSAuthorityFactory standardFactory;

    /**
     * A factory for CRS creation with (<var>longitude</var>, <var>latitude</var>) axis order.
     * Will be created only when first needed.
     */
    private static volatile CRSAuthorityFactory xyFactory;

    /**
     * A factory for default (non-lenient) operations.
     */
    private static volatile CoordinateOperationFactory strictFactory;

    /**
     * A factory for default lenient operations.
     */
    private static volatile CoordinateOperationFactory lenientFactory;

    /**
     * The default value for {@link Hints#FORCE_LONGITUDE_FIRST_AXIS_ORDER},
     * or {@code null} if not yet determined.
     */
    private static volatile Boolean defaultOrder;

    /**
     * The default value for {@link Hints#LENIENT_DATUM_SHIFT},
     * or {@code null} if not yet determined.
     */
    private static volatile Boolean defaultLenient;

    /**
     * Registers a listener automatically invoked when the system-wide configuration changed.
     */
    static {
        Factories.addChangeListener(new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) {
                synchronized (CRS.class) {
                    crsFactory      = null;
                    standardFactory = null;
                    xyFactory       = null;
                    strictFactory   = null;
                    lenientFactory  = null;
                    defaultOrder    = null;
                    defaultLenient  = null;
                }
            }
        });
    }

    /**
     * Do not allow instantiation of this class.
     */
    private CRS() {
    }


    //////////////////////////////////////////////////////////////
    ////                                                      ////
    ////        FACTORIES, CRS CREATION AND INSPECTION        ////
    ////                                                      ////
    //////////////////////////////////////////////////////////////

    /**
     * Returns the CRS factory. This is used mostly for WKT parsing.
     */
    private static CRSFactory getCRSFactory() {
        CRSFactory factory = crsFactory;
        if (factory == null) {
            synchronized (CRS.class) {
                // Double-checked locking - was a deprecated practice before Java 5.
                // Is okay since Java 5 provided that the variable is volatile.
                factory = crsFactory;
                if (factory == null) {
                    crsFactory = factory = FactoryFinder.getCRSFactory(null);
                }
            }
        }
        return factory;
    }

    /**
     * Returns the CRS authority factory used by the {@link #decode(String,boolean) decode} methods.
     * This factory {@linkplain org.geotoolkit.referencing.factory.CachingAuthorityFactory uses a cache},
     * scans over {@linkplain org.geotoolkit.referencing.factory.AllAuthoritiesFactory all factories} and
     * uses additional factories as {@linkplain org.geotoolkit.referencing.factory.FallbackAuthorityFactory
     * fallbacks} if there is more than one {@linkplain AuthorityFactoryFinder#getCRSAuthorityFactories
     * registered factory} for the same authority.
     * <p>
     * This factory can be used as a kind of <cite>system-wide</cite> factory for all authorities.
     * However for more determinist behavior, consider using a more specific factory (as returned
     * by {@link AuthorityFactoryFinder#getCRSAuthorityFactory}) when the authority is known.
     *
     * @param  longitudeFirst {@code true} if axis order should be forced to
     *         (<var>longitude</var>, <var>latitude</var>), {@code false} if no order should be
     *         forced (i.e. the standard specified by the authority is respected), or {@code null}
     *         for the {@linkplain Hints#getSystemDefault system default}.
     * @return The CRS authority factory.
     * @throws FactoryRegistryException if the factory can't be created.
     *
     * @see Hints#FORCE_LONGITUDE_FIRST_AXIS_ORDER
     *
     * @since 2.3
     */
    public static CRSAuthorityFactory getAuthorityFactory(Boolean longitudeFirst)
            throws FactoryRegistryException
    {
        // No need to synchronize; this is not a big deal if 'defaultOrder' is computed twice.
        if (longitudeFirst == null) {
            longitudeFirst = defaultOrder;
            if (longitudeFirst == null) {
                longitudeFirst = Boolean.TRUE.equals(Hints.getSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER));
                defaultOrder = longitudeFirst;
            }
        }
        CRSAuthorityFactory factory = (longitudeFirst) ? xyFactory : standardFactory;
        if (factory == null) synchronized (CRS.class) {
            // Double-checked locking - was a deprecated practice before Java 5.
            // Is okay since Java 5 provided that the variables are volatile.
            factory = (longitudeFirst) ? xyFactory : standardFactory;
            if (factory == null) try {
                factory = DefaultAuthorityFactory.create(longitudeFirst);
                if (longitudeFirst) {
                    xyFactory = factory;
                } else {
                    standardFactory = factory;
                }
            } catch (NoSuchElementException exception) {
                // No factory registered in FactoryFinder.
                throw new FactoryNotFoundException(null, exception);
            }
        }
        return factory;
    }

    /**
     * Returns the coordinate operation factory used by
     * {@link #findMathTransform(CoordinateReferenceSystem, CoordinateReferenceSystem)
     * findMathTransform} convenience methods.
     *
     * @param lenient {@code true} if the coordinate operations should be created
     *        even when there is no information available for a datum shift.
     * @return The coordinate operation factory used for finding math transform in this class.
     *
     * @since 2.4
     */
    public static CoordinateOperationFactory getCoordinateOperationFactory(final boolean lenient) {
        CoordinateOperationFactory factory = (lenient) ? lenientFactory : strictFactory;
        if (factory == null) synchronized (CRS.class) {
            // Double-checked locking - was a deprecated practice before Java 5.
            // Is okay since Java 5 provided that the variables are volatile.
            factory = (lenient) ? lenientFactory : strictFactory;
            if (factory == null) {
                final Hints hints = new Hints(); // Get the system-width default hints.
                if (lenient) {
                    hints.put(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);
                }
                factory = FactoryFinder.getCoordinateOperationFactory(hints);
                if (lenient) {
                    lenientFactory = factory;
                } else {
                    strictFactory = factory;
                }
            }
        }
        return factory;
    }

    /**
     * Returns the version number of the specified authority database, or {@code null} if
     * not available.
     *
     * @param  authority The authority name (typically {@code "EPSG"}).
     * @return The version number of the authority database, or {@code null} if unknown.
     * @throws FactoryRegistryException if no {@link CRSAuthorityFactory} implementation
     *         was found for the specified authority.
     *
     * @since 2.4
     */
    public static Version getVersion(final String authority) throws FactoryRegistryException {
        Object candidate = AuthorityFactoryFinder.getCRSAuthorityFactory(authority, null);
        final Set<Factory> guard = new HashSet<Factory>();
        while (candidate instanceof Factory) {
            final Factory factory = (Factory) candidate;
            if (!guard.add(factory)) {
                break; // Safety against never-ending recursivity.
            }
            final Map<RenderingHints.Key,?> hints = factory.getImplementationHints();
            final Object version = hints.get(Hints.VERSION);
            if (version instanceof Version) {
                return (Version) version;
            }
            candidate = hints.get(Hints.CRS_AUTHORITY_FACTORY);
        }
        return null;
    }

    /**
     * Gets the list of the codes that are supported by the given authority. For example
     * {@code getSupportedCodes("EPSG")} may returns {@code "EPSG:2000"}, {@code "EPSG:2001"},
     * {@code "EPSG:2002"}, <cite>etc</cite>. It may also returns {@code "2000"}, {@code "2001"},
     * {@code "2002"}, <cite>etc.</cite> without the {@code "EPSG:"} prefix. Whatever the authority
     * name is prefixed or not is factory implementation dependent.
     * <p>
     * If there is more than one factory for the given authority, then this method merges the
     * code set of all of them. If a factory fails to provide a set of supported code, then
     * this particular factory is ignored. Please be aware of the following potential issues:
     * <p>
     * <ul>
     *   <li>If there is more than one EPSG databases (for example an Access and a PostgreSQL ones),
     *       then this method will connect to all of them even if their content are identical.</li>
     *
     *   <li>If two factories format their codes differently (e.g. {@code "4326"} and
     *       {@code "EPSG:4326"}), then the returned set will contain a lot of synonymous
     *       codes.</li>
     *
     *   <li>For any code <var>c</var> in the returned set, there is no warranty that
     *       <code>{@linkplain #decode decode}(c)</code> will use the same authority
     *       factory than the one that formatted <var>c</var>.</li>
     *
     *   <li>This method doesn't report connection problems since it doesn't throw any exception.
     *       {@link FactoryException}s are logged as warnings and otherwise ignored.</li>
     * </ul>
     * <p>
     * If a more determinist behavior is wanted, consider the code below instead.
     * The following code exploit only one factory, the "preferred" one.
     *
     * {@preformat java
     *     factory = AuthorityFactoryFinder.getCRSAuthorityFactory(authority, null);
     *     Set<String> codes = factory.getAuthorityCodes(CoordinateReferenceSystem.class);
     *     String code = ...  // Choose a code here.
     *     CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem(code);
     * }
     *
     * @param  authority The authority name (for example {@code "EPSG"}).
     * @return The set of supported codes. May be empty, but never null.
     */
    public static Set<String> getSupportedCodes(final String authority) {
        return DefaultAuthorityFactory.getSupportedCodes(authority);
    }

    /**
     * Returns the set of the authority identifiers supported by registered authority factories.
     * This method search only for {@linkplain CRSAuthorityFactory CRS authority factories}.
     *
     * @param  returnAliases If {@code true}, the set will contain all identifiers for each
     *         authority. If {@code false}, only the first one
     * @return The set of supported authorities. May be empty, but never null.
     *
     * @since 2.3.1
     */
    public static Set<String> getSupportedAuthorities(final boolean returnAliases) {
        return DefaultAuthorityFactory.getSupportedAuthorities(returnAliases);
    }

    /**
     * Returns a Coordinate Reference System for the specified code.
     * Note that the code needs to mention the authority. Examples:
     * <p>
     * <ul>
     *   <li>{@code EPSG:1234}</li>
     *   <li>{@code AUTO:42001, ..., ..., ...}</li>
     * </ul>
     * <p>
     * If there is more than one factory implementation for the same authority, then all additional
     * factories are {@linkplain org.geotoolkit.referencing.factory.FallbackAuthorityFactory fallbacks}
     * to be used only when the first acceptable factory failed to create the requested CRS object.
     * <p>
     * CRS objects created by previous calls to this method are
     * {@linkplain org.geotoolkit.referencing.factory.CachingAuthorityFactory cached}
     * using {@linkplain java.lang.ref.WeakReference weak references}. Subsequent calls to this
     * method with the same authority code should be fast, unless the CRS object has been garbage
     * collected.
     *
     * @param  code The Coordinate Reference System authority code.
     * @return The Coordinate Reference System for the provided code.
     * @throws NoSuchAuthorityCodeException If the code could not be understood.
     * @throws FactoryException if the CRS creation failed for an other reason.
     *
     * @see #getSupportedCodes
     * @see org.geotoolkit.referencing.factory.AllAuthoritiesFactory#createCoordinateReferenceSystem
     */
    public static CoordinateReferenceSystem decode(String code)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        code = code.trim();
        return getAuthorityFactory(null).createCoordinateReferenceSystem(code);
    }

    /**
     * Returns a Coordinate Reference System for the specified code, maybe forcing the axis order
     * to (<var>longitude</var>, <var>latitude</var>). The {@code code} argument value is parsed
     * as in <code>{@linkplain #decode(String) decode}(code)</code>. The {@code longitudeFirst}
     * argument is the value to be given to the {@link Hints#FORCE_LONGITUDE_FIRST_AXIS_ORDER
     * FORCE_LONGITUDE_FIRST_AXIS_ORDER} hint.
     *
     * @param  code The Coordinate Reference System authority code.
     * @param  longitudeFirst {@code true} if axis order should be forced to
     *         (<var>longitude</var>, <var>latitude</var>), {@code false} if no order should
     *         be forced (i.e. the standard specified by the authority is respected).
     * @return The Coordinate Reference System for the provided code.
     * @throws NoSuchAuthorityCodeException If the code could not be understood.
     * @throws FactoryException if the CRS creation failed for an other reason.
     *
     * @see Hints#FORCE_LONGITUDE_FIRST_AXIS_ORDER
     *
     * @since 2.3
     */
    public static CoordinateReferenceSystem decode(String code, final boolean longitudeFirst)
            throws NoSuchAuthorityCodeException, FactoryException
    {
        code = code.trim();
        return getAuthorityFactory(longitudeFirst).createCoordinateReferenceSystem(code);
    }

    /**
     * Parses a
     * <A HREF="http://geoapi.sourceforge.net/snapshot/javadoc/org/opengis/referencing/doc-files/WKT.html"><cite>Well
     * Known Text</cite></A> (WKT) into a CRS object. This convenience method is a
     * shorthand for the following:
     *
     * {@preformat java
     *     FactoryFinder.getCRSFactory(null).createFromWKT(wkt);
     * }
     *
     * @param wkt The WKT string to parse.
     * @return The parsed coordinate reference system.
     * @throws FactoryException if the given WKT can't be parsed.
     */
    public static CoordinateReferenceSystem parseWKT(final String wkt) throws FactoryException {
        return getCRSFactory().createFromWKT(wkt);
    }

    /**
     * Returns the domain of validity for the specified coordinate reference system,
     * or {@code null} if unknown.
     *
     * This method fetches the {@linkplain CoordinateReferenceSystem#getDomainOfValidity domain
     * of validity} associated with the given CRS. Only {@linkplain GeographicExtent geographic
     * extents} of kind {@linkplain BoundingPolygon bounding polygon} are taken in account. If
     * none are found, then the {@linkplain #getGeographicBoundingBox geographic bounding boxes}
     * are used as a fallback.
     * <p>
     * The returned envelope is expressed in terms of the specified CRS.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The envelope in terms of the specified CRS, or {@code null} if none.
     *
     * @see #getGeographicBoundingBox
     * @see org.geotoolkit.geometry.GeneralEnvelope#normalize
     *
     * @since 2.2
     */
    public static Envelope getEnvelope(final CoordinateReferenceSystem crs) {
        Envelope envelope = null;
        GeneralEnvelope merged = null;
        if (crs != null) {
            final Extent domainOfValidity = crs.getDomainOfValidity();
            if (domainOfValidity != null) {
                for (final GeographicExtent extent : domainOfValidity.getGeographicElements()) {
                    if (Boolean.FALSE.equals(extent.getInclusion())) {
                        continue;
                    }
                    if (extent instanceof BoundingPolygon) {
                        for (final Geometry geometry : ((BoundingPolygon) extent).getPolygons()) {
                            final Envelope candidate = geometry.getEnvelope();
                            if (candidate != null) {
                                final CoordinateReferenceSystem sourceCRS =
                                        candidate.getCoordinateReferenceSystem();
                                if (sourceCRS == null || equalsIgnoreMetadata(sourceCRS, crs)) {
                                    if (envelope == null) {
                                        envelope = candidate;
                                    } else {
                                        if (merged == null) {
                                            envelope = merged = new GeneralEnvelope(envelope);
                                        }
                                        merged.add(envelope);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        /*
         * If no envelope was found, uses the geographic bounding box as a fallback. We will
         * need to transform it from WGS84 to the supplied CRS. This step was not required in
         * the previous block because the later selected only envelopes in the right CRS.
         */
        if (envelope == null) {
            final GeographicBoundingBox bounds = getGeographicBoundingBox(crs);
            if (bounds != null && !Boolean.FALSE.equals(bounds.getInclusion())) {
                envelope = merged = new GeneralEnvelope(
                        new double[] {bounds.getWestBoundLongitude(), bounds.getSouthBoundLatitude()},
                        new double[] {bounds.getEastBoundLongitude(), bounds.getNorthBoundLatitude()});
                /*
                 * We do not assign WGS84 inconditionnaly to the geographic bounding box, because
                 * it is not defined to be on a particular datum; it is only approximative bounds.
                 * We try to get the GeographicCRS from the user-supplied CRS and fallback on WGS
                 * 84 only if we found none.
                 */
                final SingleCRS     targetCRS = getHorizontalCRS(crs);
                final GeographicCRS sourceCRS = CRSUtilities.getStandardGeographicCRS2D(targetCRS);
                merged.setCoordinateReferenceSystem(sourceCRS);
                try {
                    envelope = transform(envelope, targetCRS);
                } catch (TransformException exception) {
                    /*
                     * The envelope is probably outside the range of validity for this CRS.
                     * It should not occurs, since the envelope is supposed to describe the
                     * CRS area of validity. Logs a warning and returns null, since it is a
                     * legal return value according this method contract.
                     */
                    envelope = null;
                    unexpectedException("getEnvelope", exception);
                }
                /*
                 * If transform(...) created a new envelope, its CRS is already targetCRS so it
                 * doesn't matter if 'merged' is not anymore the right instance. If 'transform'
                 * returned the envelope unchanged, the 'merged' reference still valid and we
                 * want to ensure that it have the user-supplied CRS.
                 */
                merged.setCoordinateReferenceSystem(targetCRS);
            }
        }
        return envelope;
    }

    /**
     * Returns the valid geographic area for the specified coordinate reference system,
     * or {@code null} if unknown.
     *
     * This method fetches the {@linkplain CoordinateReferenceSystem#getDomainOfValidity domain
     * of validity} associated with the given CRS. Only {@linkplain GeographicExtent geographic
     * extents} of kind {@linkplain GeographicBoundingBox geographic bounding box} are taken in
     * account.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The geographic area, or {@code null} if none.
     *
     * @see #getEnvelope
     *
     * @since 2.3
     */
    public static GeographicBoundingBox getGeographicBoundingBox(final CoordinateReferenceSystem crs) {
        GeographicBoundingBox bounds = null;
        DefaultGeographicBoundingBox merged = null;
        if (crs != null) {
            final Extent domainOfValidity = crs.getDomainOfValidity();
            if (domainOfValidity != null) {
                for (final GeographicExtent extent : domainOfValidity.getGeographicElements()) {
                    if (extent instanceof GeographicBoundingBox) {
                        final GeographicBoundingBox candidate = (GeographicBoundingBox) extent;
                        if (bounds == null) {
                            bounds = candidate;
                        } else {
                            if (merged == null) {
                                bounds = merged = new DefaultGeographicBoundingBox(bounds);
                            }
                            merged.add(candidate);
                        }
                    }
                }
            }
        }
        return bounds;
    }

    /**
     * Returns the first horizontal coordinate reference system found in the given CRS,
     * or {@code null} if there is none. A horizontal CRS is usually a two-dimensional
     * {@linkplain GeographicCRS geographic} or {@linkplain ProjectedCRS projected} CRS.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The horizontal CRS, or {@code null} if none.
     *
     * @since 2.4
     */
    public static SingleCRS getHorizontalCRS(final CoordinateReferenceSystem crs) {
        if (crs instanceof SingleCRS) {
            return getHorizontalCRS((SingleCRS) crs);
        } else if (crs instanceof CompoundCRS) {
            for (final SingleCRS c : ((CompoundCRS) crs).getComponents()) {
                final SingleCRS candidate = getHorizontalCRS(c);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Returns the first horizontal coordinate reference system found in the given CRS,
     */
    private static SingleCRS getHorizontalCRS(final SingleCRS crs) {
        final CoordinateSystem cs = crs.getCoordinateSystem();
        final int dimension = cs.getDimension();
        if (dimension == 2) {
            /*
             * For two-dimensional CRS, returns the CRS directly if it is either a
             * GeographicCRS, or any kind of derived CRS having a GeographicCRS as
             * its base.
             */
            CoordinateReferenceSystem base = crs;
            while (base instanceof GeneralDerivedCRS) {
                base = ((GeneralDerivedCRS) base).getBaseCRS();
            }
            // No need to test for ProjectedCRS, since the code above unwrap it.
            if (base instanceof GeographicCRS) {
                return crs; // Really returns 'crs', not 'base'.
            }
        } else if (dimension >= 3 && crs instanceof GeographicCRS) {
            /*
             * For three-dimensional Geographic CRS, extracts the axis having a direction
             * like "North", "North-East", "East", etc. If we find exactly two of them,
             * we can build a new GeographicCRS using them.
             */
            CoordinateSystemAxis axis0 = null, axis1 = null;
            int count = 0;
            for (int i=0; i<dimension; i++) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
search:         if (DefaultCoordinateSystemAxis.isCompassDirection(axis.getDirection())) {
                    switch (count++) {
                        case 0: axis0 = axis; break;
                        case 1: axis1 = axis; break;
                        default: break search;
                    }
                }
            }
            if (count == 2) {
                final GeodeticDatum datum = ((GeographicCRS) crs).getDatum();
                Map<String,?> properties = CRSUtilities.changeDimensionInName(cs, "3D", "2D");
                EllipsoidalCS horizontalCS;
                try {
                    horizontalCS = FactoryFinder.getCSFactory(null).
                            createEllipsoidalCS(properties, axis0, axis1);
                } catch (FactoryException e) {
                    Logging.recoverableException(CRS.class, "getHorizontalCRS", e);
                    horizontalCS = new DefaultEllipsoidalCS(properties, axis0, axis1);
                }
                properties = CRSUtilities.changeDimensionInName(crs, "3D", "2D");
                GeographicCRS horizontalCRS;
                try {
                    horizontalCRS = getCRSFactory().createGeographicCRS(properties, datum, horizontalCS);
                } catch (FactoryException e) {
                    Logging.recoverableException(CRS.class, "getHorizontalCRS", e);
                    horizontalCRS = new DefaultGeographicCRS(properties, datum, horizontalCS);
                }
                return horizontalCRS;
            }
        }
        return null;
    }

    /**
     * Returns the first projected coordinate reference system found in a the given CRS,
     * or {@code null} if there is none.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The projected CRS, or {@code null} if none.
     *
     * @since 2.4
     */
    public static ProjectedCRS getProjectedCRS(final CoordinateReferenceSystem crs) {
        if (crs instanceof ProjectedCRS) {
            return (ProjectedCRS) crs;
        }
        if (crs instanceof CompoundCRS) {
            for (final SingleCRS c : ((CompoundCRS) crs).getComponents()) {
                if (c instanceof ProjectedCRS) {
                    return (ProjectedCRS) c;
                }
            }
        }
        return null;
    }

    /**
     * Returns the first vertical coordinate reference system found in a the given CRS,
     * or {@code null} if there is none.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The vertical CRS, or {@code null} if none.
     *
     * @since 2.4
     */
    public static VerticalCRS getVerticalCRS(final CoordinateReferenceSystem crs) {
        if (crs instanceof VerticalCRS) {
            return (VerticalCRS) crs;
        }
        if (crs instanceof CompoundCRS) {
            for (final SingleCRS c : ((CompoundCRS) crs).getComponents()) {
                if (c instanceof VerticalCRS) {
                    return (VerticalCRS) c;
                }
            }
        }
        return null;
    }

    /**
     * Returns the first temporal coordinate reference system found in the given CRS,
     * or {@code null} if there is none.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The temporal CRS, or {@code null} if none.
     *
     * @since 2.4
     */
    public static TemporalCRS getTemporalCRS(final CoordinateReferenceSystem crs) {
        if (crs instanceof TemporalCRS) {
            return (TemporalCRS) crs;
        }
        if (crs instanceof CompoundCRS) {
            for (final SingleCRS c : ((CompoundCRS) crs).getComponents()) {
                if (c instanceof TemporalCRS) {
                    return (TemporalCRS) c;
                }
            }
        }
        return null;
    }

    /**
     * Returns the first ellipsoid found in a coordinate reference system,
     * or {@code null} if there is none.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return The ellipsoid, or {@code null} if none.
     *
     * @since 2.4
     */
    public static Ellipsoid getEllipsoid(final CoordinateReferenceSystem crs) {
        Datum datum = CRSUtilities.getDatum(crs);
        if (datum instanceof GeodeticDatum) {
            return ((GeodeticDatum) datum).getEllipsoid();
        }
        if (crs instanceof CompoundCRS) {
            for (final SingleCRS c : ((CompoundCRS) crs).getComponents()) {
                datum = c.getDatum();
                if (datum instanceof GeodeticDatum) {
                    return ((GeodeticDatum) datum).getEllipsoid();
                }
            }
        }
        return null;
    }

    /**
     * Compares the specified objects for equality. If both objects are Geotoolkit
     * implementations of class {@link AbstractIdentifiedObject}, then this method
     * will ignore the metadata during the comparison.
     *
     * @param  object1 The first object to compare (may be null).
     * @param  object2 The second object to compare (may be null).
     * @return {@code true} if both objects are equals.
     *
     * @since 2.2
     */
    public static boolean equalsIgnoreMetadata(final Object object1, final Object object2) {
        if (object1 == object2) {
            return true;
        }
        if (object1 instanceof AbstractIdentifiedObject &&
            object2 instanceof AbstractIdentifiedObject)
        {
            return ((AbstractIdentifiedObject) object1).equals(
                   ((AbstractIdentifiedObject) object2), false);
        }
        if (object2 instanceof MathTransform && object1 instanceof AbstractMathTransform) {
            return ((AbstractMathTransform) object1).equivalent((MathTransform) object2, false);
            /*
             * Note: the 'false' boolean argument causes the comparison to tolerate small numerical
             * departures, presumed due to rounding errors.  Consequently the two MT may not produce
             * strictly identical transformed values.
             */
        }
        return object1!=null && object1.equals(object2);
    }

    /**
     * Returns the <cite>Spatial Reference System</cite> identifier, or {@code null} if none. OGC
     * Web Services have the concept of a Spatial Reference System identifier used to communicate
     * CRS information between systems.
     * <p>
     * Examples of Spatial Reference System (SRS) values:
     * <ul>
     *   <li>{@code EPSG:4326} - this is the usual format understood to mean <cite>force XY axis
     *       order</cite>. Note that the axis order is <em>not necessarly</em> (<var>longitude</var>,
     *       <var>latitude</var>), but this is the common behavior we observe in practice.</li>
     *   <li>{@code ogc:uri:.....} - understood to match the EPSG database axis order.</li>
     *   <li>{@code AUTO:43200} - without the parameters that are specific to AUTO codes.</li>
     * </ul>
     * <p>
     * Note that this method returns the code of an arbitrary authority.
     * If the code of a specific authority is wanted (typically EPSG), then consider using
     * the static methods defined in {@link AbstractIdentifiedObject} instead.
     *
     * @param  crs The coordinate reference system, or {@code null}.
     * @return SRS represented as a string for communication between systems, or {@code null}.
     *
     * @see AbstractIdentifiedObject#getIdentifier(IdentifiedObject, Citation)
     *
     * @since 2.5
     */
    public static String toSRS(final CoordinateReferenceSystem crs) {
        if (crs != null) {
            final ReferenceIdentifier name;
            final Set<ReferenceIdentifier> identifiers = crs.getIdentifiers();
            if (identifiers!=null && !identifiers.isEmpty()) {
                name = identifiers.iterator().next();
            } else {
                name = crs.getName();
            }
            if (name != null) {
                return name.toString();
            }
        }
        return null;
    }

    /**
     * Looks up an {@linkplain ReferenceIdentifier identifier}, such as {@code "EPSG:4326"},
     * of the specified object. This method searches in registered factories for an object
     * {@linkplain #equalsIgnoreMetadata equal, ignoring metadata}, to the specified
     * object. If such an object is found, then its first identifier is returned. Otherwise
     * this method returns {@code null}.
     * <p>
     * This convenience method delegates its work to {@link IdentifiedObjectFinder}. If you
     * want more control, consider using that class. For example, use that class if the search
     * should be performed only against some {@linkplain AuthorityFactory authority factories}
     * instead of against all the registered factories, or if you want access to the full
     * {@linkplain IdentifiedObject identified object} instead of only its string value.
     *
     * @param  object The object (usually a {@linkplain CoordinateReferenceSystem coordinate
     *         reference system}) whose identifier is to be found.
     * @param  fullScan If {@code true}, an exhaustive full scan against all registered objects
     *         should be performed (may be slow). Otherwise only a fast lookup based on embedded
     *         identifiers and names will be performed.
     * @return The identifier, or {@code null} if not found.
     * @throws FactoryException If an unexpected failure occured during the search.
     *
     * @see AbstractAuthorityFactory#getIdentifiedObjectFinder
     * @see IdentifiedObjectFinder#find
     *
     * @since 2.4
     */
    public static String lookupIdentifier(final IdentifiedObject object, final boolean fullScan)
            throws FactoryException
    {
        /*
         * We perform the search using the 'xyFactory' because our implementation of
         * IdentifiedObjectFinder should be able to inspect both the (x,y) and (y,x)
         * axis order using this factory.
         */
        final AbstractAuthorityFactory xyFactory = (AbstractAuthorityFactory) getAuthorityFactory(true);
        final IdentifiedObjectFinder finder = xyFactory.getIdentifiedObjectFinder(object.getClass());
        finder.setFullScanAllowed(fullScan);
        return finder.findIdentifier(object);
    }

    /**
     * Looks up an {@linkplain ReferenceIdentifier identifier} in the namespace of the given
     * authority, such as {@link Citations#EPSG EPSG}, of the specified CRS. Invoking this
     * method is similar to invoking
     * {@link #lookupIdentifier(IdentifiedObject, boolean) lookupIdentifier(object, fullScan)}
     * except that the search is performed only among the factories of the given authority.
     *
     * @param  authority The authority for the code to search.
     * @param  crs The Coordinate Reference System whose identifier is to be found, or {@code null}.
     * @param  fullScan If {@code true}, an exhaustive full scan against all registered objects
     *         should be performed (may be slow). Otherwise only a fast lookup based on embedded
     *         identifiers and names will be performed.
     * @return The CRS identifier, or {@code null} if none was found or if the given CRS was null.
     * @throws FactoryException If an unexpected failure occured during the search.
     *
     * @since 2.5
     */
    // Note on method signature: the type is restricted to CoordinateReferenceSystem instead than
    // IdentifiedObject because current implementation searchs using only CRS authority factory.
    public static String lookupIdentifier(final Citation authority,
                                          final CoordinateReferenceSystem crs,
                                          final boolean fullScan)
            throws FactoryException
    {
        ReferenceIdentifier id = AbstractIdentifiedObject.getIdentifier(crs, authority);
        if (id != null) {
            return id.getCode();
        }
        final DefaultAuthorityFactory df = (DefaultAuthorityFactory) getAuthorityFactory(true);
        for (final AuthorityFactory factory : df.backingStore.getFactories()) {
            if (!Citations.identifierMatches(factory.getAuthority(), authority)) {
                continue;
            }
            if (!(factory instanceof AbstractAuthorityFactory)) {
                continue;
            }
            final AbstractAuthorityFactory f = (AbstractAuthorityFactory) factory;
            final IdentifiedObjectFinder finder = f.getIdentifiedObjectFinder(crs.getClass());
            finder.setFullScanAllowed(fullScan);
            final String code = finder.findIdentifier(crs);
            if (code != null) {
                return code;
            }
        }
        return null;
    }

    /**
     * Looks up an EPSG code of the given {@linkplain CoordinateReferenceSystem
     * coordinate reference system}). This is a convenience method for <code>{@linkplain
     * #lookupIdentifier(Citation, CoordinateReferenceSystem, boolean) lookupIdentifier}({@linkplain
     * Citations#EPSG}, crs, fullScan)</code> except that code is parsed as an integer.
     *
     * @param  crs The Coordinate Reference System whose identifier is to be found, or {@code null}.
     * @param  fullScan If {@code true}, an exhaustive full scan against all registered objects
     *         should be performed (may be slow). Otherwise only a fast lookup based on embedded
     *         identifiers and names will be performed.
     * @return The CRS identifier, or {@code null} if none was found or if the given CRS was null.
     * @throws FactoryException If an unexpected failure occured during the search.
     *
     * @since 2.5
     */
    public static Integer lookupEpsgCode(final CoordinateReferenceSystem crs, final boolean fullScan)
            throws FactoryException
    {
        final String identifier = lookupIdentifier(Citations.EPSG, crs, fullScan);
        if (identifier != null) {
            final int split = identifier.lastIndexOf(DefaultNameSpace.DEFAULT_SEPARATOR);
            final String code = identifier.substring(split + 1);
            // The above code works even if the separator was not found, since in such case
            // split == -1, which implies a call to substring(0) which returns 'identifier'.
            try {
                return Integer.parseInt(code);
            } catch (NumberFormatException e) {
                throw new FactoryException(Errors.format(Errors.Keys.ILLEGAL_IDENTIFIER_$1, identifier), e);
            }
        }
        return null;
    }


    /////////////////////////////////////////////////
    ////                                         ////
    ////          COORDINATE OPERATIONS          ////
    ////                                         ////
    /////////////////////////////////////////////////

    /**
     * Grabs a transform between two Coordinate Reference Systems. This convenience method is a
     * shorthand for the following:
     *
     * {@preformat java
     *     CoordinateOperationFactory factory = FactoryFinder.getCoordinateOperationFactory(null);
     *     CoordinateOperation operation = factory.createOperation(sourceCRS, targetCRS);
     *     MathTransform transform = operation.getMathTransform();
     * }
     *
     * Note that some metadata like {@linkplain CoordinateOperation#getCoordinateOperationAccuracy
     * coordinate operation accuracy} are lost by this method. If those metadata are wanted, use the
     * {@linkplain CoordinateOperationFactory coordinate operation factory} directly.
     * <p>
     * Sample use:
     *
     * {@preformat java
     *     CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:42102");
     *     CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
     *     MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
     * }
     *
     * @param  sourceCRS The source CRS.
     * @param  targetCRS The target CRS.
     * @return The math transform from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException If no math transform can be created for the specified source and
     *         target CRS.
     */
    public static MathTransform findMathTransform(final CoordinateReferenceSystem sourceCRS,
                                                  final CoordinateReferenceSystem targetCRS)
            throws FactoryException
    {
        // No need to synchronize; this is not a big deal if 'defaultLenient' is computed twice.
        Boolean lenient = defaultLenient;
        if (lenient == null) {
            defaultLenient = lenient = Boolean.TRUE.equals(
                    Hints.getSystemDefault(Hints.LENIENT_DATUM_SHIFT));
        }
        return findMathTransform(sourceCRS, targetCRS, lenient);
    }

    /**
     * Grab a transform between two Coordinate Reference Systems. This method is similar to
     * <code>{@linkplain #findMathTransform(CoordinateReferenceSystem, CoordinateReferenceSystem)
     * findMathTransform}(sourceCRS, targetCRS)</code>, except that it specifies whatever this
     * method should tolerate <cite>lenient datum shift</cite>. If the {@code lenient} argument
     * is {@code true}, then this method will not throw a "<cite>Bursa-Wolf parameters required</cite>"
     * exception during datum shifts if the Bursa-Wolf paramaters are not specified.
     * Instead it will assume a no datum shift.
     *
     * @param  sourceCRS The source CRS.
     * @param  targetCRS The target CRS.
     * @param  lenient {@code true} if the math transform should be created even when there is
     *         no information available for a datum shift. if this argument is not specified,
     *         then the default value is determined from the {@linkplain Hints#getSystemDefault
     *         system default}.
     * @return The math transform from {@code sourceCRS} to {@code targetCRS}.
     * @throws FactoryException If no math transform can be created for the specified source and
     *         target CRS.
     *
     * @see Hints#LENIENT_DATUM_SHIFT
     */
    public static MathTransform findMathTransform(final CoordinateReferenceSystem sourceCRS,
                                                  final CoordinateReferenceSystem targetCRS,
                                                  boolean lenient)
            throws FactoryException
    {
        if (equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            // Slight optimization in order to avoid the overhead of loading the full referencing engine.
            return IdentityTransform.create(sourceCRS.getCoordinateSystem().getDimension());
        }
        CoordinateOperationFactory operationFactory = getCoordinateOperationFactory(lenient);
        return operationFactory.createOperation(sourceCRS, targetCRS).getMathTransform();
    }

    /**
     * Transforms the given envelope to the specified CRS. If the given envelope is null, or the
     * {@linkplain Envelope#getCoordinateReferenceSystem envelope CRS} is null, or the given
     * target CRS is null, or the transform {@linkplain MathTransform#isIdentity is identity},
     * then the envelope is returned unchanged. Otherwise a new transformed envelope is returned.
     * <p>
     * <strong>Don't use this method if there is many envelopes to transform.</strong>
     * This method is provided as a convenience when there is only one envelope to transform
     * between CRS that can't be known in advance. If there is many of them or if the CRS are
     * restricted to known values, get the {@linkplain CoordinateOperation coordinate operation}
     * or {@linkplain MathTransform math transform} once for ever and invoke one of the methods
     * below instead (unless if performance is not a concern).
     *
     * @param  envelope The envelope to transform (may be {@code null}).
     * @param  targetCRS The target CRS (may be {@code null}).
     * @return A new transformed envelope, or directly {@code envelope}
     *         if no transformation was required.
     * @throws TransformException If a transformation was required and failed.
     *
     * @since 2.5
     */
    public static Envelope transform(Envelope envelope, final CoordinateReferenceSystem targetCRS)
            throws TransformException
    {
        if (envelope != null && targetCRS != null) {
            final CoordinateReferenceSystem sourceCRS = envelope.getCoordinateReferenceSystem();
            if (sourceCRS != null) {
                if (!equalsIgnoreMetadata(sourceCRS, targetCRS)) {
                    final CoordinateOperationFactory factory = getCoordinateOperationFactory(true);
                    final CoordinateOperation operation;
                    try {
                        operation = factory.createOperation(sourceCRS, targetCRS);
                    } catch (FactoryException exception) {
                        throw new TransformException(Errors.format(
                                Errors.Keys.CANT_TRANSFORM_ENVELOPE), exception);
                    }
                    if (!operation.getMathTransform().isIdentity()) {
                        envelope = transform(operation, envelope);
                    }
                }
                assert equalsIgnoreMetadata(envelope.getCoordinateReferenceSystem(), targetCRS);
            }
        }
        return envelope;
    }

    /**
     * Transforms an envelope using the given {@linkplain MathTransform math transform}.
     * The transformation is only approximative. Note that the returned envelope may not
     * have the same number of dimensions than the original envelope.
     * <p>
     * Note that this method can not handle the case where the envelope contains the North or South
     * pole, or when it cross the &plusmn;180&deg; longitude, because {@linkplain MathTransform
     * math transforms} does not carry suffisient informations. For a more robust envelope
     * transformation, use {@link #transform(CoordinateOperation, Envelope)} instead.
     *
     * @param  transform The transform to use.
     * @param  envelope Envelope to transform, or {@code null}. This envelope will not be modified.
     * @return The transformed envelope, or {@code null} if {@code envelope} was null.
     * @throws TransformException if a transform failed.
     *
     * @since 2.4
     *
     * @see #transform(CoordinateOperation, Envelope)
     */
    public static GeneralEnvelope transform(final MathTransform transform, final Envelope envelope)
            throws TransformException
    {
        return transform(transform, envelope, null);
    }

    /**
     * Implementation of {@link #transform(MathTransform, Envelope)} with the opportunity to
     * save the projected center coordinate. If {@code targetPt} is non-null, then this method
     * will set it to the center of the source envelope projected to the target CRS.
     */
    private static GeneralEnvelope transform(final MathTransform   transform,
                                             final Envelope        envelope,
                                             GeneralDirectPosition targetPt)
            throws TransformException
    {
        if (envelope == null) {
            return null;
        }
        if (transform.isIdentity()) {
            /*
             * Slight optimisation: Just copy the envelope. Note that we need to set the CRS
             * to null because we don't know what the target CRS was supposed to be. Even if
             * an identity transform often imply that the target CRS is the same one than the
             * source CRS, it is not always the case. The metadata may be differents, or the
             * transform may be a datum shift without Bursa-Wolf parameters, etc.
             */
            final GeneralEnvelope e = new GeneralEnvelope(envelope);
            e.setCoordinateReferenceSystem(null);
            if (targetPt != null) {
                for (int i=envelope.getDimension(); --i>=0;) {
                    targetPt.setOrdinate(i, e.getMedian(i));
                }
            }
            return e;
        }
        /*
         * Checks argument validity: envelope and math transform dimensions must be consistent.
         */
        final int sourceDim = transform.getSourceDimensions();
        if (envelope.getDimension() != sourceDim) {
            throw new MismatchedDimensionException(Errors.format(Errors.Keys.MISMATCHED_DIMENSION_$2,
                      sourceDim, envelope.getDimension()));
        }
        int coordinateNumber = 0;
        GeneralEnvelope transformed = null;
        if (targetPt == null) {
            targetPt = new GeneralDirectPosition(transform.getTargetDimensions());
        }
        /*
         * Before to run the loops, we must initialize the coordinates to the minimal values.
         * This coordinates will be updated in the 'switch' statement inside the 'while' loop.
         */
        final GeneralDirectPosition sourcePt = new GeneralDirectPosition(sourceDim);
        for (int i=sourceDim; --i>=0;) {
            sourcePt.setOrdinate(i, envelope.getMinimum(i));
        }
  loop: while (true) {
            /*
             * Transform a point and add the transformed point to the destination envelope.
             * Note that the very last point to be projected must be the envelope center.
             */
            if (targetPt != transform.transform(sourcePt, targetPt)) {
                throw new UnsupportedImplementationException(transform.getClass());
            }
            if (transformed != null) {
                transformed.add(targetPt);
            } else {
                transformed = new GeneralEnvelope(targetPt, targetPt);
            }
            /*
             * Get the next point's coordinates.  The 'coordinateNumber' variable should
             * be seen as a number in base 3 where the number of digits is equal to the
             * number of dimensions. For example, a 4-D space would have numbers ranging
             * from "0000" to "2222" (numbers in base 3). The digits are then translated
             * into minimal, central or maximal ordinates. The outer loop stops when the
             * counter roll back to "0000".  Note that 'targetPt' must keep the value of
             * the last projected point, which must be the envelope center identified by
             * "2222" in the 4-D case.
             */
            int n = ++coordinateNumber;
            for (int i=sourceDim; --i>=0;) {
                switch (n % 3) {
                    case 0:  sourcePt.setOrdinate(i, envelope.getMinimum(i)); n /= 3; break;
                    case 1:  sourcePt.setOrdinate(i, envelope.getMaximum(i)); continue loop;
                    case 2:  sourcePt.setOrdinate(i, envelope.getMedian (i)); continue loop;
                    default: throw new AssertionError(n); // Should never happen
                }
            }
            break;
        }
        return transformed;
    }

    /**
     * Transforms an envelope using the given {@linkplain CoordinateOperation coordinate operation}.
     * The transformation is only approximative. It may be bigger than the smallest possible
     * bounding box, but should not be smaller. Note that the returned envelope may not have
     * the same number of dimensions than the original envelope.
     * <p>
     * This method can handle the case where the envelope contains the North or South pole,
     * or when it cross the &plusmn;180&deg; longitude.
     *
     * @param  operation The operation to use. Source and target dimension must be 2.
     * @param  envelope Envelope to transform, or {@code null}. This envelope will not be modified.
     * @return The transformed envelope, or {@code null} if {@code envelope} was null.
     * @throws TransformException if a transform failed.
     *
     * @since 2.4
     *
     * @see #transform(MathTransform, Envelope)
     */
    public static GeneralEnvelope transform(final CoordinateOperation operation, final Envelope envelope)
            throws TransformException
    {
        if (envelope == null) {
            return null;
        }
        final CoordinateReferenceSystem sourceCRS = operation.getSourceCRS();
        if (sourceCRS != null) {
            final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
            if (crs != null && !equalsIgnoreMetadata(crs, sourceCRS)) {
                throw new MismatchedReferenceSystemException(
                        Errors.format(Errors.Keys.MISMATCHED_COORDINATE_REFERENCE_SYSTEM));
            }
        }
        MathTransform mt = operation.getMathTransform();
        final GeneralDirectPosition centerPt = new GeneralDirectPosition(mt.getTargetDimensions());
        final GeneralEnvelope transformed = transform(mt, envelope, centerPt);
        /*
         * If the source envelope crosses the expected range of valid coordinates, also projects
         * the range bounds as a safety. Example: if the source envelope goes from 150 to 200°E,
         * some map projections will interpret 200° as if it was -160°, and consequently produce
         * an envelope which do not include the 180°W extremum. We will add those extremum points
         * explicitly as a safety. It may leads to bigger than necessary target envelope, but the
         * contract is to include at least the source envelope, not to returns the smallest one.
         */
        if (sourceCRS != null) {
            final CoordinateSystem cs = sourceCRS.getCoordinateSystem();
            if (cs != null) { // Should never be null, but check as a paranoiac safety.
                DirectPosition sourcePt = null;
                DirectPosition targetPt = null;
                final int dimension = cs.getDimension();
                for (int i=0; i<dimension; i++) {
                    final CoordinateSystemAxis axis = cs.getAxis(i);
                    if (axis == null) { // Should never be null, but check as a paranoiac safety.
                        continue;
                    }
                    final double min = envelope.getMinimum(i);
                    final double max = envelope.getMaximum(i);
                    final double  v1 = axis.getMinimumValue();
                    final double  v2 = axis.getMaximumValue();
                    final boolean b1 = (v1 > min && v1 < max);
                    final boolean b2 = (v2 > min && v2 < max);
                    if (!b1 && !b2) {
                        continue;
                    }
                    if (sourcePt == null) {
                        sourcePt = new GeneralDirectPosition(dimension);
                        for (int j=0; j<dimension; j++) {
                            sourcePt.setOrdinate(j, envelope.getMedian(j));
                        }
                    }
                    if (b1) {
                        sourcePt.setOrdinate(i, v1);
                        transformed.add(targetPt = mt.transform(sourcePt, targetPt));
                    }
                    if (b2) {
                        sourcePt.setOrdinate(i, v2);
                        transformed.add(targetPt = mt.transform(sourcePt, targetPt));
                    }
                    sourcePt.setOrdinate(i, envelope.getMedian(i));
                }
            }
        }
        /*
         * Now takes the target CRS in account...
         */
        final CoordinateReferenceSystem targetCRS = operation.getTargetCRS();
        if (targetCRS == null) {
            return transformed;
        }
        transformed.setCoordinateReferenceSystem(targetCRS);
        final CoordinateSystem targetCS = targetCRS.getCoordinateSystem();
        if (targetCS == null) {
            // It should be an error, but we keep this method tolerant.
            return transformed;
        }
        /*
         * Checks for singularity points. For example the south pole is a singularity point in
         * geographic CRS because we reach the maximal value allowed on one particular geographic
         * axis, namely latitude. This point is not a singularity in the stereographic projection,
         * where axis extends toward infinity in all directions (mathematically) and south pole
         * has nothing special apart being the origin (0,0).
         *
         * Algorithm:
         *
         * 1) Inspect the target axis, looking if there is any bounds. If bounds are found, get
         *    the coordinates of singularity points and project them from target to source CRS.
         *
         *    Example: if the transformed envelope above is (80°S to 85°S, 10°W to 50°W), and if
         *             target axis inspection reveal us that the latitude in target CRS is bounded
         *             at 90°S, then project (90°S,30°W) to source CRS. Note that the longitude is
         *             set to the the center of the envelope longitude range (more on this later).
         *
         * 2) If the singularity point computed above is inside the source envelope, add that
         *    point to the target (transformed) envelope.
         *
         * Note: We could choose to project the (-180, -90), (180, -90), (-180, 90), (180, 90)
         * points, or the (-180, centerY), (180, centerY), (centerX, -90), (centerX, 90) points
         * where (centerX, centerY) are transformed from the source envelope center. It make
         * no difference for polar projections because the longitude is irrelevant at pole, but
         * may make a difference for the 180° longitude bounds.  Consider a Mercator projection
         * where the transformed envelope is between 20°N and 40°N. If we try to project (-180,90),
         * we will get a TransformException because the Mercator projection is not supported at
         * pole. If we try to project (-180, 30) instead, we will get a valid point. If this point
         * is inside the source envelope because the later overlaps the 180° longitude, then the
         * transformed envelope will be expanded to the full (-180 to 180) range. This is quite
         * large, but at least it is correct (while the envelope without expansion is not).
         */
        GeneralEnvelope generalEnvelope = null;
        DirectPosition sourcePt = null;
        DirectPosition targetPt = null;
        final int dimension = targetCS.getDimension();
        for (int i=0; i<dimension; i++) {
            final CoordinateSystemAxis axis = targetCS.getAxis(i);
            if (axis == null) { // Should never be null, but check as a paranoiac safety.
                continue;
            }
            boolean testMax = false; // Tells if we are testing the minimal or maximal value.
            do {
                final double extremum = testMax ? axis.getMaximumValue() : axis.getMinimumValue();
                if (Double.isInfinite(extremum) || Double.isNaN(extremum)) {
                    /*
                     * The axis is unbounded. It should always be the case when the target CRS is
                     * a map projection, in which case this loop will finish soon and this method
                     * will do nothing more (no object instantiated, no MathTransform inversed...)
                     */
                    continue;
                }
                if (targetPt == null) {
                    try {
                        mt = mt.inverse();
                    } catch (NoninvertibleTransformException exception) {
                        /*
                         * If the transform is non invertible, this method can't do anything. This
                         * is not a fatal error because the envelope has already be transformed by
                         * the caller. We lost the check for singularity points performed by this
                         * method, but it make no difference in the common case where the source
                         * envelope didn't contains any of those points.
                         *
                         * Note that this exception is normal if target dimension is smaller than
                         * source dimension, since the math transform can not reconstituate the
                         * lost dimensions. So we don't log any warning in this case.
                         */
                        if (dimension >= mt.getSourceDimensions()) {
                            unexpectedException("transform", exception);
                        }
                        return transformed;
                    }
                    targetPt = new GeneralDirectPosition(mt.getSourceDimensions());
                    for (int j=0; j<dimension; j++) {
                        targetPt.setOrdinate(j, centerPt.getOrdinate(j));
                    }
                    // TODO: avoid the hack below if we provide a contains(DirectPosition)
                    //       method in GeoAPI Envelope interface.
                    if (envelope instanceof GeneralEnvelope) {
                        generalEnvelope = (GeneralEnvelope) envelope;
                    } else {
                        generalEnvelope = new GeneralEnvelope(envelope);
                    }
                }
                targetPt.setOrdinate(i, extremum);
                try {
                    sourcePt = mt.transform(targetPt, sourcePt);
                } catch (TransformException e) {
                    /*
                     * This exception may be normal. For example we are sure to get this exception
                     * when trying to project the latitude extremums with a cylindrical Mercator
                     * projection. Do not log any message and try the other points.
                     */
                    continue;
                }
                if (generalEnvelope.contains(sourcePt)) {
                    transformed.add(targetPt);
                }
            } while ((testMax = !testMax) == true);
            if (targetPt != null) {
                targetPt.setOrdinate(i, centerPt.getOrdinate(i));
            }
        }
        return transformed;
    }

    /**
     * Transforms a rectangular envelope using the given {@linkplain MathTransform math transform}.
     * The transformation is only approximative. Invoking this method is equivalent to invoking the
     * following:
     * <p>
     * <pre>transform(transform, new GeneralEnvelope(envelope)).toRectangle2D()</pre>
     * <p>
     * Note that this method can not handle the case where the rectangle contains the North or South
     * pole, or when it cross the &plusmn;180&deg; longitude, because {@linkplain MathTransform
     * math transforms} do not carry suffisient informations. For a more robust rectangle
     * transformation, use {@link #transform(CoordinateOperation, Rectangle2D, Rectangle2D)}
     * instead.
     *
     * @param  transform   The transform to use. Source and target dimension must be 2.
     * @param  envelope    The rectangle to transform (may be {@code null}).
     * @param  destination The destination rectangle (may be {@code envelope}).
     *         If {@code null}, a new rectangle will be created and returned.
     * @return {@code destination}, or a new rectangle if {@code destination} was non-null
     *         and {@code envelope} was null.
     * @throws TransformException if a transform failed.
     *
     * @since 2.4
     *
     * @see #transform(CoordinateOperation, Rectangle2D, Rectangle2D)
     * @see org.geotoolkit.referencing.operation.matrix.XAffineTransform#transform(
     *      java.awt.geom.AffineTransform, Rectangle2D, Rectangle2D)
     */
    public static Rectangle2D transform(final MathTransform2D transform,
                                        final Rectangle2D     envelope,
                                              Rectangle2D     destination)
            throws TransformException
    {
        return transform(transform, envelope, destination, new Point2D.Double());
    }

    /**
     * Implementation of {@link #transform(MathTransform, Rectangle2D, Rectangle2D)} with the
     * opportunity to save the projected center coordinate. This method sets {@code point} to
     * the center of the source envelope projected to the target CRS.
     */
    @SuppressWarnings("fallthrough")
    private static Rectangle2D transform(final MathTransform2D transform,
                                         final Rectangle2D     envelope,
                                               Rectangle2D     destination,
                                         final Point2D.Double  point)
            throws TransformException
    {
        if (envelope == null) {
            return null;
        }
        double xmin = Double.POSITIVE_INFINITY;
        double ymin = Double.POSITIVE_INFINITY;
        double xmax = Double.NEGATIVE_INFINITY;
        double ymax = Double.NEGATIVE_INFINITY;
        for (int i=0; i<=8; i++) {
            /*
             *   (0)────(5)────(1)
             *    |             |
             *   (4)    (8)    (7)
             *    |             |
             *   (2)────(6)────(3)
             *
             * (note: center must be last)
             */
            point.x = (i & 1) == 0 ? envelope.getMinX() : envelope.getMaxX();
            point.y = (i & 2) == 0 ? envelope.getMinY() : envelope.getMaxY();
            switch (i) {
                case 5: // fall through
                case 6: point.x = envelope.getCenterX(); break;
                case 8: point.x = envelope.getCenterX(); // fall through
                case 7: // fall through
                case 4: point.y = envelope.getCenterY(); break;
            }
            if (point != transform.transform(point, point)) {
                throw new UnsupportedImplementationException(transform.getClass());
            }
            if (point.x < xmin) xmin = point.x;
            if (point.x > xmax) xmax = point.x;
            if (point.y < ymin) ymin = point.y;
            if (point.y > ymax) ymax = point.y;
        }
        if (destination != null) {
            destination.setRect(xmin, ymin, xmax-xmin, ymax-ymin);
        } else {
            destination = XRectangle2D.createFromExtremums(xmin, ymin, xmax, ymax);
        }
        // Attempt the 'equalsEpsilon' assertion only if source and destination are not same and
        // if the target envelope is Float or Double (this assertion doesn't work with integers).
        assert (destination == envelope || !(destination instanceof Rectangle2D.Double ||
                destination instanceof Rectangle2D.Float)) || XRectangle2D.equalsEpsilon(destination,
                transform(transform, new Envelope2D(null, envelope)).toRectangle2D()) : destination;
        return destination;
    }

    /**
     * Transforms a rectangular envelope using the given {@linkplain CoordinateOperation coordinate
     * operation}. The transformation is only approximative. Invoking this method is equivalent to
     * invoking the following:
     *
     * {@preformat java
     *     transform(operation, new GeneralEnvelope(envelope)).toRectangle2D()
     * }
     *
     * This method can handle the case where the rectangle contains the North or South pole,
     * or when it cross the &plusmn;180&deg; longitude.
     *
     * @param  operation The operation to use. Source and target dimension must be 2.
     * @param  envelope The rectangle to transform (may be {@code null}).
     * @param  destination The destination rectangle (may be {@code envelope}).
     *         If {@code null}, a new rectangle will be created and returned.
     * @return {@code destination}, or a new rectangle if {@code destination} was non-null
     *         and {@code envelope} was null.
     * @throws TransformException if a transform failed.
     *
     * @since 2.4
     *
     * @see #transform(MathTransform2D, Rectangle2D, Rectangle2D)
     * @see org.geotoolkit.referencing.operation.matrix.XAffineTransform#transform(
     *      java.awt.geom.AffineTransform, Rectangle2D, Rectangle2D)
     */
    public static Rectangle2D transform(final CoordinateOperation operation,
                                        final Rectangle2D         envelope,
                                              Rectangle2D         destination)
            throws TransformException
    {
        if (envelope == null) {
            return null;
        }
        final MathTransform transform = operation.getMathTransform();
        if (!(transform instanceof MathTransform2D)) {
            throw new MismatchedDimensionException(Errors.format(Errors.Keys.NO_TRANSFORM2D_AVAILABLE));
        }
        MathTransform2D mt = (MathTransform2D) transform;
        final Point2D.Double center = new Point2D.Double();
        destination = transform(mt, envelope, destination, center);
        /*
         * If the source envelope crosses the expected range of valid coordinates, also projects
         * the range bounds as a safety. See the comments in transform(Envelope, ...).
         */
        final CoordinateReferenceSystem sourceCRS = operation.getSourceCRS();
        if (sourceCRS != null) {
            final CoordinateSystem cs = sourceCRS.getCoordinateSystem();
            if (cs != null && cs.getDimension() == 2) { // Paranoiac check.
                CoordinateSystemAxis axis = cs.getAxis(0);
                double min = envelope.getMinX();
                double max = envelope.getMaxX();
                Point2D.Double pt = null;
                for (int i=0; i<4; i++) {
                    if (i == 2) {
                        axis = cs.getAxis(1);
                        min = envelope.getMinY();
                        max = envelope.getMaxY();
                    }
                    final double v = (i & 1) == 0 ? axis.getMinimumValue() : axis.getMaximumValue();
                    if (!(v > min && v < max)) {
                        continue;
                    }
                    if (pt == null) {
                        pt = new Point2D.Double();
                    }
                    if ((i & 2) == 0) {
                        pt.x = v;
                        pt.y = envelope.getCenterY();
                    } else {
                        pt.x = envelope.getCenterX();
                        pt.y = v;
                    }
                    destination.add(mt.transform(pt, pt));
                }
            }
        }
        /*
         * Now takes the target CRS in account...
         */
        final CoordinateReferenceSystem targetCRS = operation.getTargetCRS();
        if (targetCRS == null) {
            return destination;
        }
        final CoordinateSystem targetCS = targetCRS.getCoordinateSystem();
        if (targetCS == null || targetCS.getDimension() != 2) {
            // It should be an error, but we keep this method tolerant.
            return destination;
        }
        /*
         * Checks for singularity points. See the transform(CoordinateOperation, Envelope)
         * method for comments about the algorithm. The code below is the same algorithm
         * adapted for the 2D case and the related objects (Point2D, Rectangle2D, etc.).
         */
        Point2D sourcePt = null;
        Point2D targetPt = null;
        for (int flag=0; flag<4; flag++) { // 2 dimensions and 2 extremums compacted in a flag.
            final int i = flag >> 1; // The dimension index being examined.
            final CoordinateSystemAxis axis = targetCS.getAxis(i);
            if (axis == null) { // Should never be null, but check as a paranoiac safety.
                continue;
            }
            final double extremum = (flag & 1) == 0 ? axis.getMinimumValue() : axis.getMaximumValue();
            if (Double.isInfinite(extremum) || Double.isNaN(extremum)) {
                continue;
            }
            if (targetPt == null) {
                try {
                    // TODO: remove the cast when we will be allowed to compile for J2SE 1.5.
                    mt = mt.inverse();
                } catch (NoninvertibleTransformException exception) {
                    unexpectedException("transform", exception);
                    return destination;
                }
                targetPt = new Point2D.Double();
            }
            switch (i) {
                case 0: targetPt.setLocation(extremum, center.y); break;
                case 1: targetPt.setLocation(center.x, extremum); break;
                default: throw new AssertionError(flag);
            }
            try {
                sourcePt = mt.transform(targetPt, sourcePt);
            } catch (TransformException e) {
                // Do not log; this exception is often expected here.
                continue;
            }
            if (envelope.contains(sourcePt)) {
                destination.add(targetPt);
            }
        }
        // Attempt the 'equalsEpsilon' assertion only if source and destination are not same.
        assert (destination == envelope) || XRectangle2D.equalsEpsilon(destination,
                transform(operation, new GeneralEnvelope(envelope)).toRectangle2D()) : destination;
        return destination;
    }

    /**
     * Invoked when an unexpected exception occured. Those exceptions must be non-fatal,
     * i.e. the caller <strong>must</strong> have a raisonable fallback (otherwise it
     * should propagate the exception).
     */
    static void unexpectedException(final String methodName, final Exception exception) {
        Logging.unexpectedException(CRS.class, methodName, exception);
    }

    /**
     * Resets some aspects of the referencing system. The aspects to be reset are specified by
     * a space or comma delimited string, which may include any of the following elements:
     * <p>
     * <ul>
     *   <li>{@code "plugins"} for {@linkplain AuthorityFactoryFinder#scanForPlugins searching
     *       the classpath for new plugins}.</li>
     *   <li>{@code "warnings"} for {@linkplain UnitaryProjection#resetWarnings re-enabling the
     *       warnings to be issued when a coordinate is out of geographic valid area}.</li>
     * </ul>
     *
     * @param aspects The aspects to reset, or {@code "all"} for all of them.
     *        Unknown aspects are silently ignored.
     *
     * @since 2.5
     */
    public static synchronized void reset(final String aspects) {
        final StringTokenizer tokens = new StringTokenizer(aspects, ", \t\n\r\f");
        while (tokens.hasMoreTokens()) {
            final String aspect = tokens.nextToken().trim();
            final boolean all = aspect.equalsIgnoreCase("all");
            if (all || aspect.equalsIgnoreCase("plugins")) {
                AuthorityFactoryFinder.scanForPlugins();
            }
            if (all || aspect.equalsIgnoreCase("warnings")) {
                UnitaryProjection.resetWarnings();
            }
        }
    }
}
