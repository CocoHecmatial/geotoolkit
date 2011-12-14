/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007-2011, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.image.io.metadata;

import java.text.Format;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOInvalidTreeException;
import org.w3c.dom.Node;

import org.opengis.coverage.grid.RectifiedGrid;
import org.opengis.metadata.content.ImageDescription;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.acquisition.AcquisitionInformation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.geotoolkit.gui.swing.tree.Trees;
import org.geotoolkit.util.XArrays;
import org.geotoolkit.util.NumberRange;
import org.geotoolkit.util.logging.LoggedFormat;
import org.geotoolkit.image.io.SpatialImageReader;
import org.geotoolkit.image.io.SpatialImageWriter;
import org.geotoolkit.image.io.WarningProducer;
import org.geotoolkit.internal.image.io.Warnings;
import org.geotoolkit.measure.RangeFormat;
import org.geotoolkit.resources.IndexedResourceBundle;
import org.geotoolkit.resources.Errors;

import static org.geotoolkit.util.ArgumentChecks.ensureNonNull;


/**
 * Spatial (usually geographic) informations encoded in an image. This class converts the
 * {@link IIOMetadataNode} elements and attribute values to ISO 19115-2 metadata objects.
 * While ISO 19115-2 is the primary standard supported by this class, other standards can
 * works if they are designed with the same rules than the {@link org.opengis.metadata}
 * package.
 * <p>
 * ISO 19115-2 metadata instances are obtained by {@link #getInstanceForType(Class)} (for
 * a single instance) or {@link #getListForType(Class)} (for a list of metadata instances)
 * methods. The table below lists some common metadata elements. The "<cite>Format</cite>"
 * and "<cite>Path to node</cite>" columns give the location of the metadata element in a
 * tree conform to the <a href="SpatialMetadataFormat.html#default-formats">spatial metadata
 * format</a> defined in this package.
 * <p>
 * <blockquote><table border="1" cellspacing="0">
 *   <tr bgcolor="lightblue">
 *     <th>&nbsp;Format&nbsp;</th>
 *     <th>&nbsp;Path to node&nbsp;</th>
 *     <th>&nbsp;Method call&nbsp;</th>
 *   </tr>
 *   <tr>
 *     <td>&nbsp;{@link SpatialMetadataFormat#STREAM STREAM}&nbsp;</td>
 *     <td>&nbsp;{@code "DiscoveryMetadata"}</td>
 *     <td>&nbsp;<code>getInstanceForType({@linkplain DataIdentification}.class)</code></td>
 *   </tr>
 *   <tr>
 *     <td>&nbsp;{@link SpatialMetadataFormat#STREAM STREAM}&nbsp;</td>
 *     <td>&nbsp;{@code "DiscoveryMetadata/Extent/GeographicElement"}</td>
 *     <td>&nbsp;<code>getInstanceForType({@linkplain GeographicBoundingBox}.class)</code></td>
 *   </tr>
 *   <tr>
 *     <td>&nbsp;{@link SpatialMetadataFormat#STREAM STREAM}&nbsp;</td>
 *     <td>&nbsp;{@code "AcquisitionMetadata"}</td>
 *     <td>&nbsp;<code>getInstanceForType({@linkplain AcquisitionInformation}.class)</code></td>
 *   </tr>
 *   <tr>
 *     <td>&nbsp;{@link SpatialMetadataFormat#IMAGE IMAGE}&nbsp;</td>
 *     <td>&nbsp;{@code "ImageDescription"}</td>
 *     <td>&nbsp;<code>getInstanceForType({@linkplain ImageDescription}.class)</code></td>
 *   </tr>
 *   <tr>
 *     <td>&nbsp;{@link SpatialMetadataFormat#IMAGE IMAGE}&nbsp;</td>
 *     <td>&nbsp;{@code "ImageDescription/Dimensions"}</td>
 *     <td>&nbsp;<code>getListForType({@linkplain SampleDimension}.class)</code></td>
 *   </tr>
 *   <tr>
 *     <td>&nbsp;{@link SpatialMetadataFormat#IMAGE IMAGE}&nbsp;</td>
 *     <td>&nbsp;{@code "RectifiedGridDomain"}</td>
 *     <td>&nbsp;<code>getInstanceForType({@linkplain RectifiedGrid}.class)</code></td>
 *   </tr>
 * </table></blockquote>
 * <p>
 * The {@code getInstanceForType(Class)} and {@code getListForType(Class)} methods are
 * tolerant to metadata located in other paths than the ones documented below, provided
 * that the underlying metadata {@linkplain #format} declares exactly one element accepting
 * a {@linkplain IIOMetadataFormat#getObjectClass(String) user object}
 * {@linkplain Class#isAssignableFrom(Class) assignable} to the given type.
 * In case of ambiguity, an {@link IllegalArgumentException} is thrown.
 *
 * {@section Errors handling}
 * If some inconsistency are found while reading (for example if the coordinate system
 * dimension doesn't match the envelope dimension), then the default implementation
 * {@linkplain #warningOccurred logs a warning}. We do not throw an exception because
 * minor errors are not uncommon in geographic data, and we want to process the data on
 * a "<cite>best effort</cite>" basis. However because every warnings are logged through
 * the {@code warningOccurred} method, subclasses can override this method if they want
 * to treat some warnings as fatal errors.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.08
 *
 * @see SpatialMetadataFormat
 *
 * @since 3.04 (derived from 2.4)
 * @module
 */
public class SpatialMetadata extends IIOMetadata implements WarningProducer {
    /**
     * An empty {@code SpatialMetadata} with no data and no format. This constant is
     * an alternative to {@code null} for meaning that no metadata are available.
     *
     * @since 3.06
     */
    public static final SpatialMetadata EMPTY = new SpatialMetadata();

    /**
     * The metadata format given at construction time. This is typically
     * {@link SpatialMetadataFormat#IMAGE} or {@link SpatialMetadataFormat#STREAM}.
     */
    public final IIOMetadataFormat format;

    /**
     * The {@link ImageReader} or {@link ImageWriter} that holds the metadata,
     * or {@code null} if none.
     */
    private final Object owner;

    /**
     * The metadata provided by standard {@link ImageReader} instances, on which to fallback
     * if the user asked for something else than the spatial metadata managed by this class.
     * This is {@code null} if there is no such fallback.
     */
    final IIOMetadata fallback;

    /**
     * The root node to be returned by {@link #getAsTree}.
     */
    private Node root;

    /**
     * The values created by {@link #getInstanceForType(Class)}, cached for reuse.
     */
    private transient Map<Class<?>, Object> instances;

    /**
     * The values created by {@link #getListForType(Class)}, cached for reuse.
     */
    private transient Map<Class<?>, List<?>> lists;

    /**
     * The standard date format. Will be created only when first needed.
     *
     * @see #dateFormat()
     */
    private transient LoggedFormat<Date> dateFormat;

    /**
     * The standard range format. Will be created only when first needed.
     *
     * @see #rangeFormat()
     */
    private transient LoggedFormat<NumberRange<?>> rangeFormat;

    /**
     * The default logging level for the warnings. This is given to the
     * {@link MetadataAccessor} objects created for this metadata.
     */
    private Level warningLevel = Level.WARNING;

    /**
     * {@code true} if this {@code SpatialMetadata} is read-only.
     * The default value is {@code false}.
     *
     * @see #isReadOnly()
     *
     * @since 3.08
     */
    private boolean isReadOnly;

    /**
     * Creates a metadata with no format. This constructor
     * is for the {@link #EMPTY} constant only.
     */
    private SpatialMetadata() {
        format     = null;
        owner      = null;
        fallback   = null;
        isReadOnly = true;
    }

    /**
     * Creates an initially empty metadata instance for the given format.
     * The {@code format} argument is usually one of the {@code SpatialMetadataFormat}
     * {@link SpatialMetadataFormat#STREAM STREAM} or {@link SpatialMetadataFormat#IMAGE IMAGE}
     * constants, but other formats are allowed.
     *
     * @param format The metadata format.
     */
    public SpatialMetadata(final IIOMetadataFormat format) {
        this(format, (Object) null, null);
    }

    /**
     * Creates an initially empty metadata instance for the given format and reader.
     * The {@code format} argument is usually one of the {@code SpatialMetadataFormat}
     * {@link SpatialMetadataFormat#STREAM STREAM} or {@link SpatialMetadataFormat#IMAGE IMAGE}
     * constants, but other formats are allowed.
     * <p>
     * If the {@code fallback} argument is non-null, then any call to a
     * {@link #getMetadataFormat(String) getMetadataFormat}, {@link #getAsTree(String) getAsTree},
     * {@link #mergeTree(String,Node) mergeTree} or {@link #setFromTree(String,Node) setFromTree}
     * method with a format name different than
     * <code>format.{@linkplain IIOMetadataFormat#getRootName() getRootName()}</code> will be
     * delegated to that fallback. This is useful when the given {@code ImageReader} is actually
     * a wrapper around an other {@code ImageReader}.
     *
     * @param format   The metadata format.
     * @param reader   The source image reader, or {@code null} if none.
     * @param fallback The fallback for any format name different than {@code format.getRootName()},
     *                 or {@code null} if none.
     */
    public SpatialMetadata(final IIOMetadataFormat format, final ImageReader reader, final IIOMetadata fallback) {
        this(format, (Object) reader, fallback);
    }

    /**
     * Creates an initially empty metadata instance for the given format and writer.
     * The {@code format} argument is usually one of the {@code SpatialMetadataFormat}
     * {@link SpatialMetadataFormat#STREAM STREAM} or {@link SpatialMetadataFormat#IMAGE IMAGE}
     * constants, but other formats are allowed.
     * <p>
     * If the {@code fallback} argument is non-null, then any call to a
     * {@link #getMetadataFormat(String) getMetadataFormat}, {@link #getAsTree(String) getAsTree},
     * {@link #mergeTree(String,Node) mergeTree} or {@link #setFromTree(String,Node) setFromTree}
     * method with a format name different than
     * <code>format.{@linkplain IIOMetadataFormat#getRootName() getRootName()}</code> will be
     * delegated to that fallback. This is useful when the given {@code ImageWriter} is actually
     * a wrapper around an other {@code ImageWriter}.
     *
     * @param format   The metadata format.
     * @param writer   The target image writer, or {@code null} if none.
     * @param fallback The fallback for any format name different than {@code format.getRootName()},
     *                 or {@code null} if none.
     */
    public SpatialMetadata(final IIOMetadataFormat format, final ImageWriter writer, final IIOMetadata fallback) {
        this(format, (Object) writer, fallback);
    }

    /**
     * Creates an initially empty metadata instance for the given format and reader/writer.
     * The format name of the given fallback are merged with the name of the given format.
     */
    private SpatialMetadata(final IIOMetadataFormat format, final Object owner, final IIOMetadata fallback) {
        ensureNonNull("format", format);
        this.format   = format;
        this.owner    = owner;
        this.fallback = fallback;
        if (fallback != null) {
            standardFormatSupported   = fallback.isStandardMetadataFormatSupported();
            nativeMetadataFormatName  = fallback.getNativeMetadataFormatName();
            String[] extraFormatNames = fallback.getExtraMetadataFormatNames();
            if (extraFormatNames != null) {
                extraMetadataFormatNames = XArrays.append(extraFormatNames, format.getRootName());
                return;
            }
            /*
             * Note: we leave 'nativeMetadataFormatClassName' and 'extraMetadataFormatClassNames'
             * to null, which is illegal. However the only method to use those informations is
             * IIOMetadata.getMetadataFormat(String), so this is okay if we overload that method.
             *
             * Getting the format class would be costly since there is no IIOMetadata method
             * giving that information.
             */
        }
        extraMetadataFormatNames = new String[] {format.getRootName()};
    }

    /**
     * Returns an instance of the given type extracted from this {@code IIOMetadata}.
     * This method performs the following steps:
     *
     * <ol>
     *   <li><p><b>Invoke <code>{@linkplain MetadataAccessor#listPaths
     *       MetadataAccessor.listPaths}({@linkplain #format}, type)</code>:</b><br>
     *
     *       Search for an element declared in the {@linkplain #format} specified at construction
     *       time which accept a {@linkplain IIOMetadataNode#getUserObject() user object} of the
     *       given type. Exactly one element is expected, otherwise an {@link IllegalArgumentException}
     *       is thrown.</p></li>
     *
     *   <li><p><b>Create a <code>new {@linkplain MetadataAccessor#MetadataAccessor(IIOMetadata,
     *       String, String, String) MetadataAccessor}(this, {@linkplain #format}.getRootName(),
     *       path, "#auto")</code>:</b><br>
     *
     *       Create a new metadata accessor for the single path found at the previous step.</p></li>
     *
     *   <li><p><b>Invoke <code>{@linkplain MetadataAccessor#getUserObject(Class)
     *       MetadataAccessor.getUserObject}(type)</code>:</b><br>
     *
     *       If a user object was explicitly specified, return that object.</p></li>
     *
     *   <li><p><b>Invoke <code>{@linkplain MetadataAccessor#newProxyInstance(Class)
     *       MetadataAccessor.newProxyInstance}(type)</code>:</b><br>
     *
     *       If no explicit user object was found, create a proxy which will implement the getter
     *       methods by a code that fetch the value from the corresponding attribute.</p></li>
     * </ol>
     *
     * If this {@code SpatialMetadata} does not contain a node for the given type, then this
     * method returns {@code null} if this {@code SpatialMetadata} {@linkplain #isReadOnly()
     * is read only}, or create the missing nodes otherwise.
     *
     * @param  <T> The compile-time type specified as the {@code type} argument.
     * @param  type The interface implemented by the instance to fetch.
     * @return An implementation of the given interface, or {@code null} if none.
     * @throws IllegalArgumentException If the given type is not a valid interface,
     *         or if no element or more than one element exist for the given type.
     *
     * @see MetadataAccessor#newProxyInstance(Class)
     *
     * @since 3.06
     */
    public <T> T getInstanceForType(final Class<T> type) throws IllegalArgumentException {
        if (instances == null) {
            instances = new HashMap<Class<?>, Object>();
        }
        @SuppressWarnings("unchecked")
        T object = (T) instances.get(type);
        if (object == null) {
            /*
             * No previous instance in the cache. Before to create a new instance,
             * check for the special case of CRS objects. This special case will
             * create "real" Geotk CRS objects, not proxies that use reflection.
             */
            if (CoordinateReferenceSystem.class.isAssignableFrom(type)) {
                final ReferencingBuilder builder;
                try {
                    builder = new ReferencingBuilder(this);
                } catch (NoSuchElementException e) {
                    return null; // As of method contract.
                }
                object = type.cast(builder.build());
            } else {
                /*
                 * For the general case (typically ISO 19115-2 metadata), create
                 * a proxy which will fetch attribute values using reflections.
                 */
                final MetadataAccessor accessor;
                try {
                    accessor = new MetadataAccessor(this, null, type);
                } catch (NoSuchElementException e) {
                    return null; // As of method contract.
                }
                object = getInstanceForType(type, accessor);
            }
            instances.put(type, object);
        }
        return object;
    }

    /**
     * Returns an instance of the given type extracted from this {@code IIOMetadata} at the given
     * path. This method performs the same work than {@link #getInstanceForType(Class)}, except
     * that the path is explicitly specified instead than automatically inferred from the type.
     * <p>
     * <b>Example:</b>
     *
     * {@preformat java
     *     EnvironmentalRecord er = getInstanceForType(EnvironmentalRecord.class, "AcquisitionMetadata/EnvironmentalConditions");
     * }
     *
     * If this {@code SpatialMetadata} does not contain a node for the given path, then this
     * method returns {@code null} if this {@code SpatialMetadata} {@linkplain #isReadOnly()
     * is read only}, or create the missing nodes otherwise.
     *
     * {@note At the opposite of <code>getInstanceForType(Class)</code>, the current implementation
     *        of this method does not cache the returned proxy. Caching may be implemented in a
     *        future version.}
     *
     * @param  <T> The compile-time type specified as the {@code type} argument.
     * @param  type The interface implemented by the instance to fetch.
     * @param  path The path where to fetch the metadata.
     * @return An implementation of the given interface, or {@code null} if none.
     * @throws IllegalArgumentException If the given type is not a valid interface.
     *
     * @since 3.07
     */
    public <T> T getInstanceForType(final Class<T> type, final String path) throws IllegalArgumentException {
        // Handle CRS in the same special way than getInstanceForType(Class).
        final boolean isCRS = CoordinateReferenceSystem.class.isAssignableFrom(type);
        final MetadataAccessor accessor;
        try {
            accessor = new MetadataAccessor(this, null, path, isCRS ? null : "#auto");
        } catch (NoSuchElementException e) {
            return null; // As of method contract.
        }
        if (isCRS) {
            return type.cast(new ReferencingBuilder(accessor).build());
        } else {
            return getInstanceForType(type, accessor);
        }
    }

    /**
     * Returns the instance for the given type using the given accessor.
     * <p>
     * The normal usage is to invoke this method for a singleton. However if the user invoked
     * this method for a list (he should have invoked {@link #getListForType(Class)} instead),
     * returns the first element of that list as a convenience, creating an empty one if needed.
     */
    private <T> T getInstanceForType(final Class<T> type, final MetadataAccessor accessor) {
        T object;
        if (accessor.allowsChildren()) {
            final List<T> list = getListForType(type);
            if (list.isEmpty()) {
                return null;
            }
            object = list.get(0);
        } else {
            // The normal case (singleton).
            object = accessor.getUserObject(type);
            if (object == null) {
                object = accessor.newProxyInstance(type);
            }
        }
        return object;
    }

    /**
     * Returns a list of instances of the given type extracted from this {@code IIOMetadata}.
     * This method performs the same work than {@link #getInstanceForType(Class)}, but for a
     * list.
     * <p>
     * If this {@code SpatialMetadata} does not contain a node for the given type, then this
     * method returns {@code null} if this {@code SpatialMetadata} {@linkplain #isReadOnly()
     * is read only}, or create the missing nodes otherwise.
     * <p>
     * Note that a {@code null} return value has a different meaning than an empty list:
     * an empty list means that the node exists but have no children, while a {@code null}
     * value means that the parent node does not exist.
     *
     * @param  <T> The compile-time type specified as the {@code type} argument.
     * @param  type The interface implemented by the elements of the list to fetch.
     * @return A list of implementations of the given interface, or {@code null} if none.
     * @throws IllegalArgumentException If the given type is not a valid interface,
     *         or if no element or more than one element exist for the given type.
     *
     * @see MetadataAccessor#newProxyList(Class)
     *
     * @since 3.06
     */
    public <T> List<T> getListForType(final Class<T> type) throws IllegalArgumentException {
        if (lists == null) {
            lists = new HashMap<Class<?>, List<?>>();
        }
        @SuppressWarnings("unchecked")
        List<T> list = (List<T>) lists.get(type);
        if (list == null) {
            final MetadataAccessor accessor;
            try {
                accessor = new MetadataAccessor(this, null, type);
            } catch (NoSuchElementException e) {
                return null; // As of method contract.
            }
            list = accessor.newProxyList(type);
            lists.put(type, list);
        }
        return list;
    }

    /**
     * Returns a list of instances of the given type extracted from this {@code IIOMetadata} at the
     * given path. This method performs the same work than {@link #getListForType(Class)}, except
     * that the path and the name of children are explicitly specified instead than automatically
     * inferred from the type.
     * <p>
     * <b>Example:</b>
     *
     * {@preformat java
     *     Instrument it = getListForType(Instrument.class, "AcquisitionMetadata/Platform/Instruments", "Instrument");
     * }
     *
     * If this {@code SpatialMetadata} does not contain a node for the given path, then this
     * method returns {@code null} if this {@code SpatialMetadata} {@linkplain #isReadOnly()
     * is read only}, or create the missing nodes otherwise.
     * <p>
     * Note that a {@code null} return value has a different meaning than an empty list:
     * an empty list means that the node exists but have no children, while a {@code null}
     * value means that the parent node does not exist.
     *
     * {@note At the opposite of <code>getListForType(Class)</code>, the current implementation
     *        of this method does not cache the returned proxy. Caching may be implemented in a
     *        future version.}
     *
     * @param  <T> The compile-time type specified as the {@code type} argument.
     * @param  type The interface implemented by the elements of the list to fetch.
     * @param  path The path of the parent node where to fetch the metadata.
     * @param  children The name of children under the parent node.
     * @return A list of implementations of the given interface, or {@code null} if none.
     * @throws IllegalArgumentException If the given type is not a valid interface.
     *
     * @since 3.07
     */
    public <T> List<T> getListForType(final Class<T> type, final String path, final String children)
            throws IllegalArgumentException
    {
        final MetadataAccessor accessor;
        try {
            accessor = new MetadataAccessor(this, null, path, children);
        } catch (NoSuchElementException e) {
            return null; // As of method contract.
        }
        return accessor.newProxyList(type);
    }

    /**
     * Returns the language to use when formatting messages, or {@code null} for the
     * default. The default implementation delegates to {@link ImageReader#getLocale} or
     * {@link ImageWriter#getLocale} if possible, or returns {@code null} otherwise.
     *
     * @return The locale for formatting messages, or {@code null}.
     */
    @Override
    public Locale getLocale() {
        if (owner instanceof ImageReader) {
            return ((ImageReader) owner).getLocale();
        }
        if (owner instanceof ImageWriter) {
            return ((ImageWriter) owner).getLocale();
        }
        return null;
    }

    /**
     * Returns the resources for formatting error messages.
     */
    private IndexedResourceBundle getErrorResources() {
        return Errors.getResources(getLocale());
    }

    /**
     * Returns {@code true} if we should use the {@linkplain #format} given at construction
     * time, {@code false} if we should use the fallback, or thrown an exception otherwise.
     */
    private boolean isValidName(final String formatName) throws IllegalArgumentException {
        ensureNonNull("formatName", formatName);
        if (format.getRootName().equalsIgnoreCase(formatName)) {
            return true;
        }
        if (fallback != null) {
            return false;
        }
        throw new IllegalArgumentException(getErrorResources().getString(
                Errors.Keys.ILLEGAL_ARGUMENT_$2, "formatName", formatName));
    }

    /**
     * Returns an object describing the given metadata format, or {@code null} if no description is
     * available. The default implementation is as below:
     * <p>
     * <ul>
     *   <li>If {@code formatName} is equals, ignoring case, to <code>{@linkplain #format}.getRootName()</code>
     *       where {@code format} is the argument given at construction time, then return that format.</li>
     *   <li>Otherwise if a fallback has been specified at construction time, delegate to that
     *       fallback.</li>
     *   <li>Otherwise throw an {@code IllegalArgumentException}.</li>
     * </ul>
     *
     * @param  formatName The desired metadata format.
     * @return The desired metadata format, or {@code null} if none.
     * @throws IllegalArgumentException If the specified format name is not recognized.
     */
    @Override
    public IIOMetadataFormat getMetadataFormat(final String formatName) throws IllegalArgumentException {
        if (isValidName(formatName)) {
            return format;
        } else {
            return fallback.getMetadataFormat(formatName);
        }
    }

    /**
     * Returns the root of a tree of metadata contained within this object according to the
     * conventions defined by the metadata {@linkplain #format} associated to this instance.
     */
    final Node getAsTree() {
        if (format == null) {
            throw new UnsupportedOperationException();
        }
        if (root == null) {
            root = new IIONode(format.getRootName());
        }
        return root;
    }

    /**
     * Returns the root of a tree of metadata contained within this object
     * according to the conventions defined by a given metadata format.
     *
     * @param  formatName the desired metadata format.
     * @return The node forming the root of metadata tree.
     * @throws IllegalArgumentException if the format name is {@code null} or is not
     *         one of the names returned by {@link #getMetadataFormatNames}.
     */
    @Override
    public Node getAsTree(final String formatName) throws IllegalArgumentException {
        if (isValidName(formatName)) {
            return getAsTree();
        } else {
            return fallback.getAsTree(formatName);
        }
    }

    /**
     * Alters the internal state of this metadata from a tree whose syntax is defined by
     * the given metadata format. The semantics of how a tree or subtree may be merged with
     * another tree are format-specific. It may be simply replacing all existing state with
     * the contents of the given tree.
     *
     * @param  formatName The desired metadata format.
     * @param  root An XML DOM Node object forming the root of a tree.
     * @throws IllegalStateException If this metadata is {@linkplain #isReadOnly() read only}.
     * @throws IIOInvalidTreeException If the tree cannot be parsed successfully using the
     *         rules of the given format.
     */
    @Override
    public void mergeTree(final String formatName, final Node root) throws IIOInvalidTreeException {
        if (isReadOnly()) {
            throw new IllegalStateException(getErrorResources()
                    .getString(Errors.Keys.UNMODIFIABLE_METADATA));
        }
        if (isValidName(formatName)) {
            this.root = root;
        } else {
            fallback.mergeTree(formatName, root);
        }
    }

    /**
     * Returns {@code true} if this object does not allows modification. The default value is
     * {@code false}. If the read-only state is set to {@code true}, then:
     *
     * <ul>
     *   <li><p>Calls to {@link #mergeTree mergeTree}, {@link #setFromTree setFromTree} and
     *       {@link #reset reset} methods will throw an {@link IllegalStateException}, as
     *       required by the standard {@link IIOMetadata} contract.</p></li>
     *
     *   <li><p>Creation of {@link MetadataAccessor}s will throw a {@link NoSuchElementException}
     *       if the named element does not exist in this {@code SpatialMetadata} object. Note that
     *       if the {@code SpatialMetadata} has not been declared read-only, then the default
     *       {@code MetadataAccessor} behavior is to create any missing nodes.</p></li>
     * </ul>
     */
    @Override
    public boolean isReadOnly() {
        return isReadOnly;
    }

    /**
     * Sets whatever this {@code SpatialMetadata} should be read-only.
     *
     * @param ro The new read-only state of this metadata object.
     *
     * @since 3.08
     */
    public void setReadOnly(final boolean ro) {
        isReadOnly = ro;
    }

    /**
     * Returns the level at which warnings are emitted. The default implementation returns
     * the last value given to the {@link #setWarningLevel(Level)}, or {@link Level#WARNING}
     * if the level has not been explicitly defined.
     *
     * @return The current level at which warnings are emitted.
     *
     * @see MetadataAccessor#getWarningLevel()
     *
     * @since 3.07
     */
    public Level getWarningLevel() {
        return warningLevel;
    }

    /**
     * Sets the warning level. This logging level is given to all {@link MetadataAccessor} created
     * by this {@code SpatialMetadata} instance. The default value is {@link Level#WARNING}.
     * <p>
     * Note that in the default implementation, warnings are logged only
     * if this {@code SpatialMetadata} instance is not associated to an
     * {@linkplain ImageReader image reader} or {@linkplain ImageWriter writer} having
     * {@linkplain javax.imageio.event.IIOReadWarningListener warning listeners}.
     * See {@link #warningOccurred(LogRecord)} for more details.
     *
     * @param level The new logging level.
     *
     * @see MetadataAccessor#setWarningLevel(Level)
     *
     * @since 3.07
     */
    public void setWarningLevel(final Level level) {
        ensureNonNull("level", level);
        warningLevel = level;
        if (instances   != null) MetadataProxy.setWarningLevel(instances.values(), level);
        if (lists       != null) MetadataProxy.setWarningLevel(lists    .values(), level);
        if (dateFormat  != null) dateFormat .setLevel(level);
        if (rangeFormat != null) rangeFormat.setLevel(level);
    }

    /**
     * Invoked when some inconsistency has been detected in the spatial metadata. The default
     * implementation delegates to the first of the following choices which is applicable:
     * <p>
     * <ul>
     *   <li>{@link SpatialImageReader#warningOccurred(LogRecord)}</li>
     *   <li>{@link SpatialImageWriter#warningOccurred(LogRecord)}</li>
     *   <li>Send the record to the {@link #LOGGER "org.geotoolkit.image.io"} logger otherwise.</li>
     * </ul>
     * <p>
     * Subclasses can override this method if more processing is wanted, or for
     * throwing exception if some warnings should be considered as fatal errors.
     *
     * @param record The warning record to log.
     * @return {@code true} if the message has been sent to at least one warning listener,
     *         or {@code false} if it has been sent to the logging system as a fallback.
     *
     * @see MetadataAccessor#warningOccurred(LogRecord)
     * @see javax.imageio.event.IIOReadWarningListener
     */
    @Override
    public boolean warningOccurred(final LogRecord record) {
        return Warnings.log(owner, record);
    }

    /**
     * A {@link LoggedFormat} which use the {@link SpatialMetadata#getLocale reader locale}
     * for warnings.
     */
    private final class FormatAdapter<T> extends LoggedFormat<T> {
        private static final long serialVersionUID = -1108933164506428318L;

        FormatAdapter(final Format format, final Class<T> type) {
            super(format, type);
        }

        @Override
        protected Locale getWarningLocale() {
            return getLocale();
        }

        @Override
        protected void logWarning(final LogRecord warning) {
            warningOccurred(warning);
        }
    }

    /**
     * Wraps the specified format in order to either parse fully a string, or log a warning.
     *
     * @param  <T>    The expected type of parsed values.
     * @param  format The format to use for parsing and formatting.
     * @param  type   The expected type of parsed values.
     * @return A format that logs warnings when it can't parse fully a string.
     */
    protected <T> LoggedFormat<T> createLoggedFormat(final Format format, final Class<T> type) {
        return new FormatAdapter<T>(format, type);
    }

    /**
     * Creates a logged format of the given type.
     *
     * @param  <T>    The expected type of parsed values.
     * @param  type   The expected type of parsed values.
     * @param  format The method to logs as the "caller" when the parsing fails.
     * @return A format that logs warnings when it can't parse fully a string.
     */
    private <T> LoggedFormat<T> createLoggedFormat(final Class<T> type, final String caller) {
        final LoggedFormat<T> format = createLoggedFormat(createFormat(type), type);
        format.setLogger("org.geotoolkit.image.io.metadata");
        format.setCaller(MetadataAccessor.class, caller);
        return format;
    }

    /**
     * Creates the format to use for parsing and formatting dates or number ranges.
     *
     * {@note We use the Canada locale because it is a bit closer to ISO standards than
     *        the US locale (e.g. order of elements in a date) while using the English
     *        symbols (e.g. the dot as a decimal separator instead than coma).}
     *
     * @param  type The expected type of parsed values.
     * @return The format to use.
     */
    private static Format createFormat(final Class<?> type) {
        if (Date.class.isAssignableFrom(type)) {
            final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format;
        }
        if (NumberRange.class.isAssignableFrom(type)) {
            final RangeFormat format = new RangeFormat(Locale.CANADA);
            format.setElementPattern("0.######", false);
            return format;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Parses the given string as an object of the given type. This method should never be invoked,
     * except as a (admitly inefficient) fallback when the {@link IIOMetadata} in use is not an
     * instance of {@code SpatialMetadata} (this usually don't happen).
     *
     * @param  type The expected type of the parsed value.
     * @param  text The text to parse
     * @return The parsed value.
     * @throws ParseException If the parsing failed.
     */
    static <T> T parse(final Class<T> type, final String text) throws ParseException {
        return type.cast(createFormat(type).parseObject(text));
    }

    /**
     * Formats the given object as a string. This method should never be invoked, except as a
     * (admitly inefficient) fallback when the {@link IIOMetadata} in use is not an instance of
     * {@code SpatialMetadata} (this usually don't happen).
     *
     * @param  type The expected type of the value.
     * @param  value The value to format.
     * @return The formatted value.
     */
    static <T> String format(final Class<T> type, final T value) {
        return createFormat(type).format(value);
    }

    /**
     * Returns a standard date format to be shared by {@link MetadataAccessor}.
     * This method creates a new format only when first invoked, and reuses the
     * existing instance on subsequent invocations.
     */
    final LoggedFormat<Date> dateFormat() {
        if (dateFormat == null) {
            dateFormat = createLoggedFormat(Date.class, "getAttributeAsDate");
        }
        return dateFormat;
    }

    /**
     * Returns a standard range format to be shared by {@link MetadataAccessor}.
     * This method creates a new format only when first invoked, and reuses the
     * existing instance on subsequent invocations.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    final LoggedFormat<NumberRange<?>> rangeFormat() {
        if (rangeFormat == null) {
            rangeFormat = (LoggedFormat) createLoggedFormat(NumberRange.class, "getAttributeAsRange");
        }
        return rangeFormat;
    }

    /**
     * Resets all the data stored in this object to default values.
     * All nodes below the root node are discarded.
     *
     * @throws IllegalStateException If this metadata is {@linkplain #isReadOnly() read only}.
     */
    @Override
    public void reset() {
        if (isReadOnly()) {
            throw new IllegalStateException(getErrorResources()
                    .getString(Errors.Keys.UNMODIFIABLE_METADATA));
        }
        root = null;
        if (fallback != null) {
            fallback.reset();
        }
    }

    /**
     * Returns a string representation of this metadata, mostly for debugging purpose.
     * The default implementation formats the metadata as a tree similar to the one
     * formatted by {@link MetadataAccessor#toString()}.
     */
    @Override
    public String toString() {
        if (format == null) {
            return "EMPTY";
        }
        return Trees.toString(Trees.xmlToSwing(getAsTree(format.getRootName())));
    }
}
