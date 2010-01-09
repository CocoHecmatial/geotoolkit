/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007-2010, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.image.io.metadata;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import javax.imageio.ImageReader; // For javadoc
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.opengis.util.CodeList;
import org.opengis.metadata.citation.Citation;

import org.geotoolkit.resources.Errors;
import org.geotoolkit.gui.swing.tree.Trees;
import org.geotoolkit.internal.CodeLists;
import org.geotoolkit.internal.StringUtilities;
import org.geotoolkit.internal.jaxb.XmlUtilities;
import org.geotoolkit.measure.Units;
import org.geotoolkit.util.Localized;
import org.geotoolkit.util.NumberRange;
import org.geotoolkit.util.converter.Classes;
import org.geotoolkit.util.NullArgumentException;
import org.geotoolkit.util.UnsupportedImplementationException;
import org.geotoolkit.util.logging.Logging;
import org.geotoolkit.metadata.iso.citation.Citations;
import org.geotoolkit.resources.IndexedResourceBundle;


/**
 * Convenience class for extracting attribute values from a {@link SpatialMetadata} object.
 * This class is used by {@link SpatialMetadata} and usually don't need to be created explicitly.
 * It is available in public API for users who need more flexibility than what
 * {@code SpatialMetadata} provides.
 * <p>
 * The metadata object is specified at construction time, together with a path to the
 * {@linkplain Element element} of interest. Examples of valid paths:
 *
 * <blockquote><table cellspacing="0" cellpadding="0">
 * <tr>
 *   <td>{@code "RectifiedGridDomain/CRS/Datum"}</td>
 *   <td>&nbsp;&nbsp;(assuming the {@linkplain SpatialMetadataFormat#IMAGE image} metadata format)</td>
 * </tr><tr>
 *   <td>{@code "RectifiedGridDomain/CRS/CoordinateSystem"}</td>
 *   <td>&nbsp;&nbsp;(assuming the {@linkplain SpatialMetadataFormat#IMAGE image} metadata format)</td>
 * </tr><tr>
 *   <td>{@code "DiscoveryMetadata/Extent/GeographicElement"}</td>
 *   <td>&nbsp;&nbsp;(assuming the {@linkplain SpatialMetadataFormat#STREAM stream} metadata format)</td>
 * </tr>
 * </table></blockquote>
 *
 * If no node exists for the given path, then the node will be created at {@code MetadataAccessor}
 * construction time. For example the last exemple in the above list will ensure that the metadata
 * tree contains at least the nodes below, creating the missing ones if needed (Note: the value of
 * {@code <root>} depends on the metadata format, but is typically
 * {@value org.geotoolkit.image.io.metadata.SpatialMetadataFormat#FORMAT_NAME}):
 *
 * {@preformat text
 *    <root>
 *    └───DiscoveryMetadata
 *        └───Extent
 *            └───GeographicElement
 * }
 *
 * After a {@code MetadataAccessor} instance has been created, the {@code getAttributeAs<Type>(String)}
 * methods can be invoked for fetching any attribute values, taking care of conversions to
 * {@link String}, {@link Double}, {@link Integer} or {@link Date}. Corresponding setter
 * methods are also provided.
 *
 * {@section Accessing child elements}
 * If order to access a child element when the child policy is
 * {@link IIOMetadataFormat#CHILD_POLICY_ALL    CHILD_POLICY_ALL},
 * {@link IIOMetadataFormat#CHILD_POLICY_SOME   CHILD_POLICY_SOME} or
 * {@link IIOMetadataFormat#CHILD_POLICY_CHOICE CHILD_POLICY_CHOICE},
 * create a new {@code MetadataAccessor} with the complete path to that element.
 * <p>
 * If the child policy of the node is {@link IIOMetadataFormat#CHILD_POLICY_REPEAT CHILD_POLICY_REPEAT},
 * then this class provide convenience methods for accessing the attributes of the childs.
 * The path to unique legal child elements shall be specified to the constructor, as in the
 * examples below:
 * <p>
 * <ul>
 *   <li>{@code new MetadataAccessor(..., "RectifiedGridDomain/CRS/CoordinateSystem", "Axis")}</li>
 *   <li>{@code new MetadataAccessor(..., "ImageDescription/Dimensions", "Dimension")}</li>
 * </ul>
 * <p>
 * The {@code get} and {@code set} methods defined in this class will operate on the
 * <cite>selected</cite> {@linkplain Element element}, which may be either the one
 * specified at construction time, or one of its childs. The element can be selected
 * by {@link #selectParent} (the default) or {@link #selectChild}.
 * <p>
 * Note that this mechanism is not suitable to nested childs, i.e. {@code MetadataAccessor} gives
 * access only to the attributes of child elements. If an access to the childs nested in a child
 * element is wanted, then the users may find more convenient to parse the XML tree by an other
 * way than this convenience class.
 *
 * {@section Example reading attributes}
 * The example below creates an accessor for a node called {@code "CoordinateSystem"}
 * which is expected to have an arbitrary amount of childs called {@code "Axis"}. The
 * name of the first axis is fetched.
 *
 * {@preformat java
 *     IIOMetadata metadata = new SpatialMetadata(SpatialMetadataFormat.IMAGE);
 *     MetadataAccessor accessor = new MetadataAccessor(metadata, null,
 *             "RectifiedGridDomain/CRS/CoordinateSystem", "Axis");
 *
 *     accessor.selectParent();
 *     String csName = accessor.getAttribute("name");
 *
 *     accessor.selectChild(0);
 *     String firstAxisName = accessor.getAttribute("name");
 * }
 *
 * {@section Example adding childs and writing attributes}
 * The example below uses the same accessor than above, but this time for adding a new
 * child under the {@code "CoordinateSystem"} node:
 *
 * {@preformat java
 *     accessor.selectChild(accessor.appendChild());
 *     accessor.setAttribute("name", "The name of a new axis");
 * }
 *
 * {@section Getting ISO 19115-2 instances}
 * This class can provide implementations of the ISO 19115-2 interfaces. Each getter method in
 * an interface is implemented as a call to a {@code getAttribute(String)} method, or as the
 * creation of a nested ISO 19115-2 object. See {@link #newProxyInstance(Class)} for more details.
 * <p>
 * While this mechanism is primarily targeted at ISO 19115-2 interfaces, it can be used with
 * other set of interfaces as well.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @author Cédric Briançon (Geomatys)
 * @version 3.07
 *
 * @see SpatialMetadata#getInstanceForType(Class)
 * @see SpatialMetadata#getListForType(Class)
 *
 * @since 2.5
 * @module
 */
public class MetadataAccessor implements Localized {
    /**
     * The separator between names in a node path.
     */
    private static final char SEPARATOR = '/';

    /**
     * No-break space. This is used for replacing ordinary spaces in a string which is to
     * be included in a list of strings. We can not keep the ordinary space since it is the
     * item separator.
     */
    private static final char NBSP = '\u00A0';

    /**
     * The Image I/O metadata for which this accessor is a wrapper. An instance
     * of the {@link SpatialMetadata} subclass is recommanded, but not mandatory.
     *
     * @since 3.06
     */
    protected final IIOMetadata metadata;

    /**
     * The metadata format used by this accessor.
     */
    final IIOMetadataFormat format;

    /**
     * The parent of child {@linkplain Element elements}.
     */
    private final Node parent;

    /**
     * The {@linkplain #childs} path. This is the {@code childPath} parameter
     * given to the constructor if explicitly specified, or the computed value
     * if the parmeter given to the contructor was {@code "#auto"}.
     */
    final String childPath;

    /**
     * The list of child elements. May be empty but never null. This list is non-modifiable if
     * {@link #childPath} is {@code null}. Otherwise, new elements can be added to this list.
     */
    private final List<Node> childs;

    /**
     * The current element, or {@code null} if not yet selected.
     *
     * @see #selectChild
     * @see #currentElement()
     */
    private transient Element current;

    /**
     * The logging level for the warnings, or {@link Level#OFF} if disabled.
     */
    private transient Level warningLevel;

    /**
     * Creates an accessor with the same parent and childs than the specified one. The two
     * accessors will share the same {@linkplain Node metadata nodes} (including the list
     * of childs), so change in one accessor will be immediately reflected in the other
     * accessor. However each accessor can {@linkplain #selectChild select their child}
     * independently.
     * <p>
     * The initially {@linkplain #selectChild selected child} and {@linkplain #getWarningLevel()
     * warnings level} are the same one than the given accessor.
     * <p>
     * The main purpose of this constructor is to create many views over the same list
     * of childs, where each view can {@linkplain #selectChild select} a different child.
     *
     * @param clone The accessor to clone.
     */
    public MetadataAccessor(final MetadataAccessor clone) {
        metadata     = clone.metadata;
        format       = clone.format;
        parent       = clone.parent;
        childPath    = clone.childPath;
        childs       = clone.childs;
        current      = clone.current;
        warningLevel = clone.warningLevel;
    }

    /**
     * Creates an accessor for the {@linkplain Element element} at the given path relative
     * to the given parent. In the example below, the complete path to the child accessor
     * is {@code "DiscoveryMetadata/Extent/GeographicElement"}:
     *
     * {@preformat java
     *     MetadataAccessor parent = new MetadataAccessor(..., "DiscoveryMetadata/Extent", ...);
     *     MetadataAccessor child  = new MetadataAccessor(parent, "GeographicElement", null);
     * }
     *
     * {@section Auto-detection of children}
     * If the metadata node has no child, then {@code childPath} shall be {@code null}.
     * If the caller does not know whatever the node has childs or not, then the
     * {@code "#auto"} special value can be used. Note that this auto-detection may
     * throw an {@link IllegalArgumentException} if the node is not defined by the
     * {@link IIOMetadataFormat}. It is preferable to specify explicitly the child
     * element name when this name is known.
     *
     * @param parent    The accessor for which the {@code path} is relative.
     * @param path      The path to the {@linkplain Node node} of interest.
     * @param childPath The path to the child {@linkplain Element elements}, or {@code null}
     *                  if none, or {@code "#auto"} for auto-detection.
     *
     * @since 3.06
     */
    public MetadataAccessor(final MetadataAccessor parent, final String path, final String childPath) {
        this(parent, parent.metadata, null, null, path, childPath);
    }

    /**
     * Creates an accessor for the {@linkplain Element element} accepting a user object of the
     * given type. This method is convenient for fetching an object of some known type without
     * regards to its location in the sub-tree.
     *
     * @param  parent      The accessor from which to start the search for an element accepting
     *                     the given type.
     * @param  objectClass The {@linkplain IIOMetadataFormat#getObjectClass(String) class of user
     *                     object} to locate.
     * @throws IllegalArgumentException If no element accepting the given type was found,
     *         or if more than one element accepting that type was found.
     *
     * @see #listPaths(IIOMetadataFormat, Class)
     *
     * @since 3.06
     */
    public MetadataAccessor(final MetadataAccessor parent, final Class<?> objectClass)
            throws IllegalArgumentException
    {
        this(parent, parent.metadata, null, objectClass, null, "#auto");
    }

    /**
     * Creates an accessor for the {@linkplain Element element} at the given path relative to
     * the {@link IIOMetadataFormat#getRootName() root}. This is a convenience method for the
     * {@linkplain #MetadataAccessor(IIOMetadata, String, String, String) constructor below}
     * with {@code formatName} and {@code childPath} argument set to {@code "#auto"} value.
     *
     * @param  metadata   The Image I/O metadata. An instance of the {@link SpatialMetadata}
     *                    sub-class is recommanded, but not mandatory.
     * @param  parentPath The path to the {@linkplain Node node} of interest, or {@code null}
     *                    if the {@code metadata} root node is directly the node of interest.
     *
     * @since 3.06
     */
    public MetadataAccessor(final IIOMetadata metadata, final String parentPath) {
        this(metadata, "#auto", parentPath, "#auto");
    }

    /**
     * Creates an accessor for the {@linkplain Element element} at the given path relative to
     * the {@link IIOMetadataFormat#getRootName() root}. The paths can contain many elements
     * separated by the {@code '/'} character.
     * See the <a href="#skip-navbar_top">class javadoc</a> for more details.
     *
     * {@section Auto-detection of children and format}
     * The {@code childPath} argument can be {@code "#auto"}, which is processed as documented
     * in the {@linkplain #MetadataAccessor(MetadataAccessor, String, String) above constructor}.
     * <p>
     * The {@code formatName} can be {@code null} or {@code "#auto"}, in which case a format
     * is selected automatically:
     * <p>
     * <ul>
     *   <li>If {@code metadata} is an instance of {@link SpatialMetadata}, then the
     *       {@linkplain SpatialMetadata#format format} given at {@code SpatialMetadata}
     *       construction time is used.</li>
     *   <li>Otherwise the first format returned by {@link IIOMetadata#getMetadataFormatNames()}
     *       is used. This is usually in preference order: the native format, the standard format
     *       or the first extra format.</li>
     * </ul>
     *
     * @param  metadata   The Image I/O metadata. An instance of the {@link SpatialMetadata}
     *                    sub-class is recommanded, but not mandatory.
     * @param  formatName The name of the {@linkplain IIOMetadata#getMetadataFormat(String) format
     *                    to use}, or {@code null} or {@code "#auto"} for an automatic selection.
     * @param  parentPath The path to the {@linkplain Node node} of interest, or {@code null}
     *                    if the {@code metadata} root node is directly the node of interest.
     * @param  childPath  The path (relative to {@code parentPath}) to the child
     *                    {@linkplain Element elements}, or {@code null} if none,
     *                    or {@code "#auto"} for auto-detection.
     */
    public MetadataAccessor(final IIOMetadata metadata, final String formatName,
            final String parentPath, final String childPath)
    {
        this(null, metadata, formatName, null, parentPath, childPath);
    }

    /**
     * Creates an accessor for the {@linkplain Element element} accepting a user object of the
     * given type. This method is convenient for fetching an object of some known type without
     * regards to its location in the tree. For example if the metadata format is
     * {@link SpatialMetadataFormat#STREAM} then:
     *
     * {@preformat java
     *     new MetadataAccessor(metadata, formatName, GeographicElement.class);
     * }
     *
     * is equivalent to:
     *
     * {@preformat java
     *     new MetadataAccessor(metadata, formatName, "DiscoveryMetadata/Extent/GeographicElement", "#auto");
     * }
     *
     * @param  metadata    The Image I/O metadata. An instance of the {@link SpatialMetadata}
     *                     sub-class is recommanded, but not mandatory.
     * @param  formatName  The name of the {@linkplain IIOMetadata#getMetadataFormat(String) format
     *                     to use}, or {@code null} or {@code "#auto"} for an automatic selection.
     * @param  objectClass The {@linkplain IIOMetadataFormat#getObjectClass(String) class of user
     *                     object} to locate.
     * @throws IllegalArgumentException If no element accepting the given type was found,
     *         or if more than one element accepting that type was found.
     *
     * @see #listPaths(IIOMetadataFormat, Class)
     *
     * @since 3.06
     */
    public MetadataAccessor(final IIOMetadata metadata, final String formatName, final Class<?> objectClass)
            throws IllegalArgumentException
    {
        this(null, metadata, formatName, objectClass, null, "#auto");
    }

    /**
     * Implementation of the public constructors. If the {@code parentAccessor}
     * argument is non-null, then {@code parentPath} is relative to that parent.
     */
    @SuppressWarnings("fallthrough")
    private MetadataAccessor(final MetadataAccessor parentAccessor, final IIOMetadata metadata,
            String formatName, final Class<?> type, String parentPath, String childPath)
            throws IllegalArgumentException
    {
        ensureNonNull("metadata", metadata);
        this.metadata = metadata;
        /*
         * Fetch IIOMetadataFormat to use and the root of the tree,
         * or the root of the sub-tree is 'parentAccessor' is non-null.
         */
        final Node root;
        if (parentAccessor != null) {
            format       = parentAccessor.format;
            root         = parentAccessor.parent;
            warningLevel = parentAccessor.warningLevel;
        } else if (formatName != null && !formatName.equals("#auto")) {
            format = metadata.getMetadataFormat(formatName);
            root   = metadata.getAsTree(formatName);
        } else if (metadata instanceof SpatialMetadata) {
            final SpatialMetadata sp = (SpatialMetadata) metadata;
            format = sp.format;
            root   = sp.getAsTree();
        } else {
            // In preference order: native, standard, extra formats.
            formatName = metadata.getMetadataFormatNames()[0];
            format = metadata.getMetadataFormat(formatName);
            root   = metadata.getAsTree(formatName);
        }
        if (format == null) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UNDEFINED_FORMAT_$1, formatName));
        }
        if (warningLevel == null) {
            warningLevel = (metadata instanceof SpatialMetadata) ?
                ((SpatialMetadata) metadata).getWarningLevel() : Level.WARNING;
        }
        /*
         * If the user asked for a metadata working on a node for user object of
         * a specific type, get the path to that node. We expect a single path.
         */
        if (type != null) {
            final List<String> paths = new ArrayList<String>(4);
            listPaths(format, type, root.getNodeName(), new StringBuilder(), paths);
            switch (paths.size()) {
                case 0: {
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.UNKNOW_TYPE_$1, type));
                }
                case 1: {
                    parentPath = paths.get(0);
                    break;
                }
                default: {
                    final String lineSeparator = System.getProperty("line.separator", "\n");
                    final StringBuilder buffer = new StringBuilder(Errors.format(
                            Errors.Keys.AMBIGIOUS_VALUE_$1, type)).append(lineSeparator);
                    for (final String path : paths) {
                        buffer.append("  \u2022 ").append(path).append(lineSeparator);
                    }
                    throw new IllegalArgumentException(buffer.toString());
                }
            }
        }
        /*
         * Fetch the parent node and ensure that we got a singleton. If there is more nodes than
         * expected, log a warning and pickup the first one. If there is no node, create a new one.
         */
        final List<Node> childs = new ArrayList<Node>(4);
        if (parentPath != null) {
            listChilds(root, parentPath, 0, childs, true);
            final int count = childs.size();
            switch (count) {
                default: {
                    warning("<init>", Errors.Keys.TOO_MANY_OCCURENCES_$2, parentPath, count);
                    // Fall through for picking the first node.
                }
                case 1: {
                    parent = childs.get(0);
                    childs.clear();
                    break;
                }
                case 0: {
                    parent = appendChild(root, parentPath);
                    break;
                }
            }
        } else {
            parent = root;
        }
        /*
         * If the child is "#auto", get the name from the metadata format. We will pick the
         * child name only if there is no ambiguity: only one child with the repeat policy.
         * If we can not find an unambiguous child, we will process as if there is no child.
         */
        if ("#auto".equals(childPath)) {
            childPath = null;
            final String name = parent.getNodeName();
            if (format.getChildPolicy(name) == IIOMetadataFormat.CHILD_POLICY_REPEAT) {
                final String[] childNames = format.getChildNames(name);
                if (childNames != null && childNames.length == 1) {
                    childPath = childNames[0];
                }
            }
        }
        /*
         * Computes a full path to children. Searching from 'metadata' root node using 'path'
         * should be identical to searching from 'parent' node using 'childPath', except in
         * case of unexpected metadata format where the parent node appears more than once.
         */
        this.childPath = childPath;
        if (childPath != null) {
            final String path;
            if (parentPath != null) {
                path = parentPath + SEPARATOR + childPath;
            } else {
                path = childPath;
            }
            listChilds(root, path, 0, childs, false);
            this.childs = childs;
        } else {
            this.childs = Collections.emptyList();
        }
        if (parent instanceof Element) {
            current = (Element) parent;
        }
    }

    /**
     * Adds to the {@link #childs} list the child nodes at the given {@code path}.
     * This method is for constructor implementation only and invokes itself recursively.
     *
     * @param  parent The parent metadata node.
     * @param  path   The path to the nodes or elements to insert into the list.
     * @param  base   The offset in {@code path} for the next element name.
     * @param  childs The list where to insert the nodes or elements.
     * @param  includeNodes {@code true} of adding nodes as well as elements.
     */
    private static void listChilds(final Node parent, final String path, final int base,
                                   final List<Node> childs, final boolean includeNodes)
    {
        final int upper = path.indexOf(SEPARATOR, base);
        final String name = ((upper >= 0) ? path.substring(base, upper)
                                          : path.substring(base)).trim();
        final NodeList list = parent.getChildNodes();
        final int length = list.getLength();
        for (int i=0; i<length; i++) {
            final Node candidate = list.item(i);
            if (name.equals(candidate.getNodeName())) {
                if (upper >= 0) {
                    listChilds(candidate, path, upper+1, childs, includeNodes);
                } else if (includeNodes || (candidate instanceof Element)) {
                    // For the very last node, we may require an element.
                    childs.add(candidate);
                }
            }
        }
    }

    /**
     * Appends a child to the given parent.
     *
     * @param parent   The parent to add a child to.
     * @param path     The path of the child to add.
     * @return element The new child.
     */
    private static Node appendChild(Node parent, final String path) {
        int lower = 0;
search: for (int upper; (upper = path.indexOf(SEPARATOR, lower)) >= 0; lower=upper+1) {
            final String name = path.substring(lower, upper).trim();
            final NodeList list = parent.getChildNodes();
            final int length = list.getLength();
            for (int i=length; --i>=0;) {
                final Node candidate = list.item(i);
                if (name.equals(candidate.getNodeName())) {
                    parent = candidate;
                    continue search;
                }
            }
            parent = parent.appendChild(new IIONode(name.intern()));
        }
        final String name = path.substring(lower).trim().intern();
        return parent.appendChild(new IIONode(name));
    }

    /**
     * Returns the paths to every {@linkplain Element elements} declared in the given format which
     * accept a {@linkplain IIOMetadataNode#getUserObject() user object} of the given type. If no
     * path is found, returns an empty list.
     *
     * {@section Collection handling}
     * In the particular case where the element is the child of a node having having
     * {@link IIOMetadataFormat#CHILD_POLICY_REPEAT CHILD_POLICY_REPEAT}, then the path to the
     * parent node is returned. In other words, if the given type is the type of elements in a
     * collection, then the path to the whole collection is returned instead than the path to a
     * single element in that collection.
     *
     * @param  format The metadata format in which to search.
     * @param  objectClass The {@linkplain IIOMetadataFormat#getObjectClass(String)
     *         class of user object} to locate.
     * @return The list of paths to elements that accept user objects of the given type.
     *
     * @see #MetadataAccessor(MetadataAccessor, Class)
     * @see #MetadataAccessor(IIOMetadata, String, Class)
     *
     * @since 3.06
     */
    public static List<String> listPaths(final IIOMetadataFormat format, final Class<?> objectClass) {
        ensureNonNull("type",   objectClass);
        ensureNonNull("format", format);
        final List<String> paths = new ArrayList<String>(4);
        listPaths(format, objectClass, format.getRootName(), new StringBuilder(), paths);
        return paths;
    }

    /**
     * Adds to the given list the path to every elements of the given type.
     * This method invokes itself recursively for scanning down the tree.
     *
     * @param format      The metadata format in which to search.
     * @param objectClass The type of user object to locate.
     * @param elementName The current element to be scaned.
     * @param buffer      An initially empty buffer, for internal use by this method.
     * @param paths       The list where to add the paths that we found.
     */
    private static void listPaths(final IIOMetadataFormat format, final Class<?> objectClass,
            final String elementName, final StringBuilder buffer, final List<String> paths)
    {
        final int childPolicy = format.getChildPolicy(elementName);
        if (childPolicy != IIOMetadataFormat.CHILD_POLICY_EMPTY) {
            final String[] childs = format.getChildNames(elementName);
            if (childs != null) {
                final int base = buffer.length();
                for (final String child : childs) {
                    if (base != 0) {
                        buffer.append('/');
                    }
                    buffer.append(child);
                    if (format.getObjectValueType(child) != IIOMetadataFormat.VALUE_NONE) {
                        final Class<?> candidate = format.getObjectClass(child);
                        if (objectClass != null && objectClass.isAssignableFrom(candidate)) {
                            /*
                             * We found an element. If this element is to be repeated in a collection,
                             * then use the path of the parent which describe the whole collection.
                             */
                            paths.add(childPolicy == IIOMetadataFormat.CHILD_POLICY_REPEAT ?
                                    buffer.substring(0, base) : buffer.toString());
                        }
                    }
                    listPaths(format, objectClass, child, buffer, paths);
                    buffer.setLength(base);
                }
            }
        }
    }

    /**
     * Makes sure an argument is non-null.
     *
     * @param  name   Argument name.
     * @param  object User argument.
     * @throws NullArgumentException if {@code object} is null.
     */
    static void ensureNonNull(String name, Object object) throws NullArgumentException {
        if (object == null) {
            throw new NullArgumentException(Errors.format(Errors.Keys.NULL_ARGUMENT_$1, name));
        }
    }

    /**
     * Returns the name of the element for which this accessor will fetch attribute values.
     * This is the last part of the {@code parentPath} argument given at construction time.
     * For example if the given path was {@code "DiscoveryMetadata/Extent/GeographicElement"},
     * then the name returned by this method is {@code "GeographicElement"}.
     *
     * @return The name of the metadata element.
     *
     * @since 3.06
     */
    public String name() {
        return parent.getNodeName();
    }

    /**
     * Returns the number of child {@linkplain Element elements}.
     * This is the upper value (exclusive) for {@link #selectChild}.
     *
     * @return The child {@linkplain Element elements} count.
     *
     * @see #selectChild
     * @see #appendChild
     */
    public int childCount() {
        return childs.size();
    }

    /**
     * Returns {@code true} if this accessor allows children. If this method returns
     * {@code false}, then attempts to {@linkplain #appendChild() append a child} will
     * throw a {@link UnsupportedOperationException}.
     *
     * @return {@code true) if this accessor allows children.
     */
    final boolean allowsChildren() {
        return childs != Collections.EMPTY_LIST;
    }

    /**
     * Adds a new child {@linkplain Element element} at the path given at construction time.
     * The {@linkplain #childCount child count} will be increased by 1.
     * <p>
     * The new child is <strong>not</strong> automatically selected. In order to select this
     * new child, the {@link #selectChild} method must be invoked explicitly.
     *
     * @return The index of the new child element.
     * @throws UnsupportedOperationException If this accessor does not allow children.
     *
     * @see #childCount
     * @see #selectChild
     */
    public int appendChild() throws UnsupportedOperationException {
        final int size = childs.size();
        final Node child = appendChild(parent, childPath);
        if (child instanceof Element) {
            childs.add((Element) child);
            return size;
        } else {
            throw new UnsupportedImplementationException(child.getClass());
        }
    }

    /**
     * Selects the {@linkplain Element element} at the given index. Every subsequent calls
     * to {@code get} or {@code set} methods will apply to this selected child element.
     *
     * @param index The index of the element to select.
     * @throws IndexOutOfBoundsException if the specified index is out of bounds.
     *
     * @see #childCount
     * @see #appendChild
     * @see #selectParent
     */
    public void selectChild(final int index) throws IndexOutOfBoundsException {
        current = (Element) childs.get(index);
    }

    /**
     * Selects the <em>parent</em> of child elements. Every subsequent calls to {@code get}
     * or {@code set} methods will apply to this parent element.
     *
     * @throws NoSuchElementException if there is no parent {@linkplain Element element}.
     *
     * @see #selectChild
     */
    public void selectParent() throws NoSuchElementException {
        if (parent instanceof Element) {
            current = (Element) parent;
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the current element.
     *
     * @return The currently selected element.
     * @throws IllegalStateException if there is no selected element.
     *
     * @see #selectChild
     */
    private Element currentElement() throws IllegalStateException {
        if (current == null) {
            throw new IllegalStateException();
        }
        return current;
    }

    /**
     * Returns the {@linkplain IIOMetadataNode#getUserObject user object} associated with the
     * {@linkplain #selectChild selected element}, or {@code null} if none. This method returns
     * the first of the following methods which return a non-null value:
     * <p>
     * <ul>
     *   <li>{@link IIOMetadataNode#getUserObject()} (only if the node is an instance of {@code IIOMetadata})</li>
     *   <li>{@link Node#getNodeValue()}</li>
     * </ul>
     * <p>
     * The <cite>node value</cite> fallback is consistent with {@link #setUserObject(Object)}
     * implementation, and allows processing of nodes that are not {@link IIOMetadataNode}
     * instances.
     *
     * {@note This <code>getUserObject()</code> method and the <code>getUserObject(Class)</code>
     *        method below are the only getters that do not fetch the string to parse by a call
     *        to <code>getAttribute</code>.}
     *
     * @return The user object, or {@code null} if none.
     *
     * @see #getUserObject(Class)
     * @see #setUserObject(Object)
     */
    public Object getUserObject() {
        final Element element = currentElement();
        if (element instanceof IIOMetadataNode) {
            final Object candidate = ((IIOMetadataNode) element).getUserObject();
            if (candidate != null) {
                return candidate;
            }
        }
        /*
         * getNodeValue() returns a String. We use it as a fallback, but in typical
         * IIOMetadataNode usage this value is not used (according its javadoc), so
         * it will often be null.
         */
        return element.getNodeValue();
    }

    /**
     * Returns the user object as an instance of the specified class. This method first invokes
     * {@link #getUserObject()}, then checks the type of the returned object. The type shall be
     * the requested one - this method does not attempt conversions.
     * <p>
     * A special processing is performed if the type of the user object is assignable to
     * {@link CharSequence}. This special processing is performed because if the node is
     * not an instance of {@link IIOMetadataNode}, then {@code getUserObject()} fallbacks
     * on {@link Node#getNodeValue()}, which can return only a {@code String}. In such
     * case, this method will attempt a parsing of the string if the requested type is
     * a subclass of {@link Number}, {@link Date}, {@code double[]} or {@code int[]}.
     *
     * @param  <T>  The expected class.
     * @param  type The expected class.
     * @return The user object, or {@code null} if none.
     * @throws NumberFormatException If attempt to parse the user object as a number failed.
     * @throws IllegalArgumentException If attempt to parse the user object as a date failed.
     * @throws ClassCastException If the user object can not be casted to the specified type.
     *
     * @see #getUserObject()
     * @see #setUserObject(Object)
     * @see SpatialMetadata#getInstanceForType(Class)
     */
    public <T> T getUserObject(final Class<? extends T> type) throws ClassCastException {
        ensureNonNull("type", type);
        Object value = getUserObject();
        if (value instanceof CharSequence) {
            if (String.class.isAssignableFrom(type)) {
                value = value.toString();
            } else if (Number.class.isAssignableFrom(type)) {
                value = Classes.valueOf(type, value.toString());
            } else if (Date.class.isAssignableFrom(type)) {
                value = XmlUtilities.parseDateTime(value.toString());
            } else if (type.isArray()) {
                final Class<?> component = Classes.primitiveToWrapper(type.getComponentType());
                if (Double.class.equals(component)) {
                    value = parseSequence(value.toString(), Double.TYPE, false, null);
                } else if (Integer.class.equals(component)) {
                    value = parseSequence(value.toString(), Integer.TYPE, false, null);
                }
            }
        }
        return type.cast(value);
    }

    /**
     * Returns a view of the {@linkplain #selectParent() parent element} as an implementation of
     * the given interface. This method returns an instance of the given interface where each
     * getter method is implemented like the following pseudo-code, where {@code <T>} is the type
     * given in argument to this method, {@code <RT1>} is the return type of the first method
     * and {@code <RT2>} is the return type of the second method:
     *
     * {@preformat java
     *     class Proxy implements <T> {
     *         public <RT1> getBanana() {
     *             return Accessor.this.getAttributeAs<RT1>("banana");
     *         }
     *
     *         public <RT2> getApple() {
     *             return Accessor.this.getAttributeAs<RT2>("apple");
     *         }
     *
     *         // etc. for every getter methods declared in the interface.
     *     }
     * }
     *
     * The {@code <T>} type is typically one of the types given to the
     * {@link SpatialMetadataFormat#addTree(org.geotoolkit.metadata.MetadataStandard,
     * Class, String, String, java.util.Map) SpatialMetadataFormat.addTree(...)} method,
     * but this is not mandatory. This {@code <T>} is usually an interface from the ISO
     * 19115-2 standard, but this is not mandatory neither. However all getter methods
     * declared in that type shall comply with the <cite>Java Beans</cite> conventions.
     *
     * {@section Example}
     * Lets assume a metadata format conforms to the
     * <a href="SpatialMetadataFormat.html#default-formats"><cite>Stream metadata</cite>
     * format documented here</a>. There is an extract of that format:
     *
     * {@preformat text
     *     DiscoveryMetadata
     *     └───SpatialResolution
     *         └───distance
     * }
     *
     * In the following code, every call to the
     * {@link org.opengis.metadata.identification.Resolution#getDistance()} method on the instance
     * returned by this {@code newProxyInstance(type)} method will be implemented as a call to the
     * <code>{@linkplain #getAttributeAsDouble(String) getAttributeAsDouble}("distance")</code>
     * method on this {@code MetadataAccessor} instance:
     *
     * {@preformat java
     *     IIOMetadata       metadata   = new SpatialMetadata(SpatialMetadataFormat.STREAM);
     *     MetadataAccessor  accessor   = new MetadataAccessor(metadata, "#auto", "DiscoveryMetadata/SpatialResolution", null);
     *     SpatialResolution resolution = accessor.newProxyInstance(SpatialResolution.class);
     *
     *     // From this point, we can forget that the metadata are stored in an IIOMetadata object.
     *     // The following line delegates the work to accessor.getAttributeAsDouble("distance");
     *     Double distance = resolution.getDistance();
     *     System.out.println("The resolution is " + distance);
     * }
     *
     * Changes to the underlying {@code IIOMetadata} attributes are immediately reflected in the
     * {@code Resolution} instance:
     *
     * {@preformat java
     *     accessor.setAttribute("distance", 20);
     *     distance = resolution.getDistance(); // Should now return 20.
     * }
     *
     * {@section Nested proxies}
     * If the return type of a getter method (the {@code <RT1>} and {@code <RT2>} types in the
     * above example) is not assignable from {@link String}, {@link Double} or other types for
     * which a {@code getAttribute} method is defined in this {@code MetadataAccessor} class
     * (see <a href="package-summary.html#accessor-types">here</a> for a complete list), then
     * that type is assumed to be an other metadata interface. In such case, a new
     * {@code MetadataAccessor} is created for that element and a new proxy created by
     * this {@code newProxyInstance} method is returned.
     *
     * @param  <T> The compile-time type specified as the {@code type} argument.
     * @param  type The interface for which to create a proxy instance.
     * @return An implementation of the given interface with getter methods that fetch
     *         their return values from the attribute values.
     * @throws IllegalArgumentException If the given type is not a valid interface.
     *
     * @see SpatialMetadata#getInstanceForType(Class)
     * @see java.lang.reflect.Proxy
     *
     * @since 3.06
     */
    public <T> T newProxyInstance(final Class<T> type) throws IllegalArgumentException {
        return MetadataProxy.newProxyInstance(type, this);
    }

    /**
     * Returns a view of the {@linkplain #selectChild(int) child elements} as a list
     * of implementations of the given type. This method performs the same work than
     * {@link #newProxyInstance(Class)} for every childs of the element represented
     * by this accessor.
     *
     * @param  <T> The compile-time type specified as the {@code type} argument.
     * @param  type The interface for which to create proxy instances.
     * @return A list of implementations of the given interface with getter methods
     *         that fetch their return values from the attribute values.
     * @throws IllegalArgumentException If the given type is not a valid interface.
     *
     * @see SpatialMetadata#getListForType(Class)
     *
     * @since 3.06
     */
    public <T> List<T> newProxyList(final Class<T> type) throws IllegalArgumentException {
        if (allowsChildren()) {
            return MetadataProxyList.create(type, this);
        } else {
            return Collections.singletonList(MetadataProxy.newProxyInstance(type, this));
        }
    }

    /**
     * Returns {@code true} if values of the specified type can be formatted as a
     * text. We allows formatting only for reasonably cheap objects, for example
     * a Number but not a CoordinateReferenceSystem.
     */
    private static boolean isFormattable(final Class<?> type) {
        return CharSequence.class.isAssignableFrom(type) ||
               Number.class.isAssignableFrom(Classes.primitiveToWrapper(type));
    }

    /**
     * Returns an attribute as a string for the {@linkplain #selectChild selected element},
     * or {@code null} if none. This method never returns an empty string.
     * <p>
     * Every {@code getAttribute} methods in this class invoke this method first. Consequently,
     * this method provides a single overriding point for subclasses that want to process the
     * attribute before parsing.
     *
     * @param attribute The attribute to fetch (e.g. {@code "name"}).
     * @return The attribute value (never an empty string), or {@code null} if none.
     *
     * @see #setAttribute(String, String)
     * @see IIOMetadataFormat#DATATYPE_STRING
     */
    public String getAttribute(final String attribute) {
        ensureNonNull("attribute", attribute);
        String candidate = currentElement().getAttribute(attribute);
        if (candidate != null) {
            candidate = candidate.trim();
            if (candidate.length() == 0) {
                candidate = null;
            }
        }
        return candidate;
    }

    /**
     * Returns an attribute as an array of strings for the {@linkplain #selectChild selected element},
     * or {@code null} if none. This method gets the attribute as a single string, then splits that
     * string on the ordinary space separator. If some items in the resulting strings contain the
     * no-break space (<code>'\\u00A0'</code>), then those characters are replaced by ordinary spaces
     * after the split.
     *
     * @param  attribute The attribute to fetch (e.g. {@code "keywords"}).
     * @param  unique {@code true} if duplicated values should be collapsed into unique values,
     *         or {@code false} for preserving duplicated values.
     * @return The attribute values, or {@code null} if none.
     *
     * @see #setAttribute(String, String[])
     *
     * @since 3.06
     */
    public String[] getAttributeAsStrings(final String attribute, final boolean unique) {
        return (String[]) parseSequence(getAttribute(attribute), String.class, unique, "getAttributeAsStrings");
    }

    /**
     * Returns an attribute as a code for the {@linkplain #selectChild selected element},
     * or {@code null} if none. If the code stored in the given attribute is not a known
     * element, then this method logs a warning and returns {@code null}.
     *
     * @param  <T> The type of the code list.
     * @param  attribute The attribute to fetch (e.g. {@code "imagingCondition"}).
     * @param  codeType The type of the code list. This is used for determining the expected values.
     * @return The attribute value, or {@code null} if none or unknown.
     *
     * @see #setAttribute(String, CodeList)
     *
     * @since 3.06
     */
    public <T extends CodeList<T>> T getAttributeAsCode(final String attribute, final Class<T> codeType) {
        final String value = getAttribute(attribute);
        final T code = CodeLists.valueOf(codeType, value, false);
        if (code == null && value != null) {
            warning("getAttributeAsCode", Errors.Keys.BAD_PARAMETER_$2, attribute, value);
        }
        return code;
    }

    /**
     * Returns an attribute as a boolean for the {@linkplain #selectChild selected element},
     * or {@code null} if none. If the attribute can't be parsed as a boolean, then this
     * method logs a warning and returns {@code null}.
     *
     * @param attribute The attribute to fetch (e.g. {@code "inclusion"}).
     * @return The attribute value, or {@code null} if none or unparseable.
     *
     * @see #setAttribute(String, boolean)
     * @see IIOMetadataFormat#DATATYPE_BOOLEAN
     *
     * @since 3.06
     */
    public Boolean getAttributeAsBoolean(final String attribute) {
        final String value = getAttribute(attribute);
        if (value != null) {
            if (value.equalsIgnoreCase("true") ||
                value.equalsIgnoreCase("yes")  ||
                value.equalsIgnoreCase("on"))
            {
                return Boolean.TRUE;
            }
            if (value.equalsIgnoreCase("false") ||
                value.equalsIgnoreCase("no")    ||
                value.equalsIgnoreCase("off"))
            {
                return Boolean.FALSE;
            }
            warning("getAttributeAsBoolean", Errors.Keys.BAD_PARAMETER_$2, attribute, value);
        }
        return null;
    }

    /**
     * Returns an attribute as an integer for the {@linkplain #selectChild selected element},
     * or {@code null} if none. If the attribute can't be parsed as an integer, then this method
     * logs a warning and returns {@code null}.
     *
     * @param attribute The attribute to fetch (e.g. {@code "minimum"}).
     * @return The attribute value, or {@code null} if none or unparseable.
     *
     * @see #setAttribute(String, int)
     * @see IIOMetadataFormat#DATATYPE_INTEGER
     */
    public Integer getAttributeAsInteger(final String attribute) {
        String value = getAttribute(attribute);
        if (value != null) {
            value = StringUtilities.trimFractionalPart(value);
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                warning("getAttributeAsInteger", Errors.Keys.UNPARSABLE_NUMBER_$1, value);
            }
        }
        return null;
    }

    /**
     * Returns an attribute as an array of integers for the {@linkplain #selectChild selected
     * element}, or {@code null} if none. If an element can't be parsed as an integer, then this
     * method logs a warning and returns {@code null}.
     *
     * @param attribute The attribute to fetch (e.g. {@code "minimum"}).
     * @param unique {@code true} if duplicated values should be collapsed into unique values,
     *         or {@code false} for preserving duplicated values.
     * @return The attribute values, or {@code null} if none.
     *
     * @see #setAttribute(String, int[])
     */
    public int[] getAttributeAsIntegers(final String attribute, final boolean unique) {
        return (int[]) parseSequence(getAttribute(attribute), Integer.TYPE, unique, "getAttributeAsIntegers");
    }

    /**
     * Returns an attribute as a floating point values for the {@linkplain #selectChild selected
     * element}, or {@code null} if none. If the attribute can't be parsed as a floating point,
     * then this method logs a warning and returns {@code null}.
     *
     * @param attribute The attribute to fetch (e.g. {@code "minimum"}).
     * @return The attribute value, or {@code null} if none or unparseable.
     *
     * @see #setAttribute(String, float)
     * @see IIOMetadataFormat#DATATYPE_FLOAT
     *
     * @since 3.06
     */
    public Float getAttributeAsFloat(final String attribute) {
        final String value = getAttribute(attribute);
        if (value != null) try {
            return Float.valueOf(value);
        } catch (NumberFormatException e) {
            warning("getAttributeAsFloat", Errors.Keys.UNPARSABLE_NUMBER_$1, value);
        }
        return null;
    }

    /**
     * Returns an attribute as an array of floating point values for the {@linkplain #selectChild
     * selected element}, or {@code null} if none. If an element can't be parsed as a floating
     * point, then this method logs a warning and returns {@code null}.
     *
     * @param  attribute The attribute to fetch (e.g. {@code "fillValues"}).
     * @param  unique {@code true} if duplicated values should be collapsed into unique values,
     *         or {@code false} for preserving duplicated values.
     * @return The attribute values, or {@code null} if none.
     *
     * @see #setAttribute(String, float[])
     *
     * @since 3.06
     */
    public float[] getAttributeAsFloats(final String attribute, final boolean unique) {
        return (float[]) parseSequence(getAttribute(attribute), Float.TYPE, unique, "getAttributeAsFloats");
    }

    /**
     * Returns an attribute as a floating point for the {@linkplain #selectChild selected element},
     * or {@code null} if none. If the attribute can't be parsed as a floating point, then this
     * method logs a warning and returns {@code null}.
     *
     * @param attribute The attribute to fetch (e.g. {@code "minimum"}).
     * @return The attribute value, or {@code null} if none or unparseable.
     *
     * @see #setAttribute(String, double)
     * @see IIOMetadataFormat#DATATYPE_DOUBLE
     */
    public Double getAttributeAsDouble(final String attribute) {
        final String value = getAttribute(attribute);
        if (value != null) try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            warning("getAttributeAsDouble", Errors.Keys.UNPARSABLE_NUMBER_$1, value);
        }
        return null;
    }

    /**
     * Returns an attribute as an array of floating point for the {@linkplain #selectChild
     * selected element}, or {@code null} if none. If an element can't be parsed as a floating
     * point, then this method logs a warning and returns {@code null}.
     *
     * @param  attribute The attribute to fetch (e.g. {@code "fillValues"}).
     * @param  unique {@code true} if duplicated values should be collapsed into unique values,
     *         or {@code false} for preserving duplicated values.
     * @return The attribute values, or {@code null} if none.
     *
     * @see #setAttribute(String, double[])
     */
    public double[] getAttributeAsDoubles(final String attribute, final boolean unique) {
        return (double[]) parseSequence(getAttribute(attribute), Double.TYPE, unique, "getAttributeAsDoubles");
    }

    /**
     * Returns an attribute as a date for the {@linkplain #selectChild selected element},
     * or {@code null} if none. If the attribute can't be parsed as a date, then this method
     * logs a warning and returns {@code null}.
     *
     * @param attribute The attribute to fetch (e.g. {@code "origin"}).
     * @return The attribute value, or {@code null} if none or unparseable.
     *
     * @see #setAttribute(String, Date)
     */
    public Date getAttributeAsDate(final String attribute) {
        String value = getAttribute(attribute);
        if (value != null) {
            value = StringUtilities.trimFractionalPart(value);
            if (metadata instanceof SpatialMetadata) {
                return ((SpatialMetadata) metadata).dateFormat().parse(value);
            } else try {
                // Inefficient fallback, but should usually not happen anyway.
                return SpatialMetadata.parse(Date.class, value);
            } catch (ParseException e) {
                warning(MetadataAccessor.class, "getAttributeAsDate", e);
            }
        }
        return null;
    }

    /**
     * Returns an attribute as a range of numbers for the {@linkplain #selectChild selected element},
     * or {@code null} if none. If the attribute can't be parsed as a range of numbers, then this
     * method logs a warning and returns {@code null}.
     *
     * @param attribute The attribute to fetch (e.g. {@code "validSampleValues"}).
     * @return The attribute value, or {@code null} if none or unparseable.
     *
     * @see #setAttribute(String, NumberRange)
     *
     * @since 3.06
     */
    public NumberRange<?> getAttributeAsRange(final String attribute) {
        final String value = getAttribute(attribute);
        if (value != null) {
            if (metadata instanceof SpatialMetadata) {
                return ((SpatialMetadata) metadata).rangeFormat().parse(value);
            } else try {
                // Inefficient fallback, but should usually not happen anyway.
                return (NumberRange<?>) SpatialMetadata.parse(NumberRange.class, value);
            } catch (ParseException e) {
                warning(MetadataAccessor.class, "getAttributeAsRange", e);
            }
        }
        return null;
    }

    /**
     * Returns an attribute as a unit for the {@linkplain #selectChild selected element},
     * or {@code null} if none. If the attribute can't be parsed as a unit of the given
     * quantity, then this method logs a warning and returns {@code null}.
     *
     * @param  <Q> The compile-time type of the {@code quantity} argument.
     * @param  attribute The attribute to fetch (e.g. {@code "axisUnit"}).
     * @param  quantity The quantity of the unit to be returned, or {@code null} for any.
     * @return The attribute value, or {@code null} if none or unparseable.
     *
     * @see #setAttribute(String, Unit)
     *
     * @since 3.07
     */
    @SuppressWarnings("unchecked")
    public <Q extends Quantity> Unit<Q> getAttributeAsUnit(final String attribute, final Class<Q> quantity) {
        String value = getAttribute(attribute);
        if (value != null) try {
            final Unit<?> unit = Units.valueOf(value);
            if (quantity == null) {
                return (Unit<Q>) unit;
            }
            try {
                return unit.asType(quantity);
            } catch (ClassCastException e) {
                warning("getAttributeAsUnit", Errors.Keys.INCOMPATIBLE_UNIT_$1, unit);
            }
        } catch (IllegalArgumentException e) {
            warning(MetadataAccessor.class, "getAttributeAsUnit", e);
        }
        return null;
    }

    /**
     * Returns an attribute as a citation for the {@linkplain #selectChild selected element},
     * or {@code null} if none.
     *
     * @param  attribute The attribute to fetch (e.g. {@code "authority"}).
     * @return The attribute value, or {@code null} if none.
     *
     * @see #setAttribute(String, Citation)
     *
     * @since 3.06
     */
    public Citation getAttributeAsCitation(final String attribute) {
        return Citations.fromName(getAttribute(attribute));
    }

    /**
     * Implementation of methods that parse a list.
     * Example: {@link #getAttributeAsIntegers}, {@link #getAttributeAsDoubles}.
     *
     * @param sequence
     *          The character sequence to parse.
     * @param type
     *          {@code Integer.TYPE} for parsing as {@code int},
     *          {@code Double.TYPE}  for parsing as {@code double} or
     *          {@code String.class} for parsing as {@link String}.
     * @param unique
     *          {@code true} if duplicated values should be collapsed into unique values, or
     *          {@code false} for preserving duplicated values.
     * @param caller
     *          The method to report as the caller when logging warnings, or {@code null}
     *          for throwing exceptions instead than logging warnings.
     * @return The attribute values, or {@code null} if none.
     * @throws NumberFormatException if {@code logFailures} if {@code false} and an exception
     *         occured while parsing a number.
     */
    private Object parseSequence(final String sequence, final Class<?> type, final boolean unique,
            final String caller) throws NumberFormatException
    {
        if (sequence == null) {
            return null;
        }
        final Collection<Object> values;
        if (unique) {
            values = new LinkedHashSet<Object>();
        } else {
            values = new ArrayList<Object>();
        }
        final Class<?> wrapperType = Classes.primitiveToWrapper(type);
        final StringTokenizer tokens = new StringTokenizer(sequence);
        while (tokens.hasMoreTokens()) {
            final String token = tokens.nextToken().replace(NBSP, ' ').trim();
            final Object value;
            try {
                value = Classes.valueOf(wrapperType, token);
            } catch (NumberFormatException e) {
                if (caller == null) {
                    throw e;
                }
                warning(caller, Errors.Keys.UNPARSABLE_NUMBER_$1, token);
                continue;
            }
            values.add(value);
        }
        int count = 0;
        final Object array = Array.newInstance(type, values.size());
        for (final Object n : values) {
            Array.set(array, count++, n);
        }
        assert Array.getLength(array) == count;
        return array;
    }

    /**
     * Formats a sequence for {@link #setAttribute} implementations working on list arguments.
     *
     * @param  value The attribute value.
     * @return The formatted sequence.
     */
    private static String formatSequence(final Object values) {
        String text = null;
        if (values != null) {
            final StringBuilder buffer = new StringBuilder();
            final int length = Array.getLength(values);
            for (int i=0; i<length; i++) {
                if (i != 0) {
                    buffer.append(' ');
                }
                final Object value = Array.get(values, i);
                if (value != null) {
                    final String s = value.toString().trim();
                    final int sl = s.length();
                    for (int j=0; j<sl; j++) {
                        final char c = s.charAt(j);
                        buffer.append(Character.isWhitespace(c) ? NBSP : c);
                    }
                }
            }
            text = buffer.toString();
        }
        return text;
    }

    /**
     * Sets the {@linkplain IIOMetadataNode#setUserObject user object} associated with the
     * {@linkplain #selectChild selected element}. At the difference of every {@code setAttribute}
     * methods defined in this class, this method does not delegate to
     * {@link #setAttribute(String, String)}.
     * <p>
     * If the specified value is formattable (i.e. is a {@linkplain CharSequence character
     * sequence}, a {@linkplain Number number} or an array of the above), then this method
     * also {@linkplain IIOMetadataNode#setNodeValue sets the node value} as a string. This
     * is mostly a convenience for formatting purpose since {@link IIOMetadataNode} don't
     * use the node value. But it may help some libraries that are not designed to work with
     * with user objects, since they are particular to Image I/O metadata.
     *
     * @param  value The user object, or {@code null} if none.
     * @throws UnsupportedImplementationException if the selected element is not an instance of
     *         {@link IIOMetadataNode}.
     *
     * @see #getUserObject()
     */
    public void setUserObject(final Object value) throws UnsupportedImplementationException {
        final Element element = currentElement();
        String asText = null;
        if (value != null) {
            final Class<?> type = value.getClass();
            if (Date.class.isAssignableFrom(type)) {
                asText = XmlUtilities.printDateTime((Date) value);
            } else if (isFormattable(type)) {
                asText = value.toString();
            } else if (isFormattable(type.getComponentType())) {
                asText = formatSequence(value);
            }
        }
        if (element instanceof IIOMetadataNode) {
            ((IIOMetadataNode) element).setUserObject(value);
        } else if (value!=null && asText==null) {
            throw new UnsupportedImplementationException(Errors.format(Errors.Keys.ILLEGAL_CLASS_$2,
                    Classes.getClass(element), IIOMetadataNode.class));
        }
        element.setNodeValue(asText);
    }

    /**
     * Sets the attribute to the specified value, or remove the attribute if the value is null.
     * <p>
     * Every {@code setAttribute} methods in this class invoke this method last. Consequently,
     * this method provides a single overriding point for subclasses that want to process the
     * attribute after formatting.
     *
     * @param attribute The attribute name.
     * @param value The attribute value, or {@code null} for removing the attribute.
     *
     * @see #getAttribute(String)
     * @see IIOMetadataFormat#DATATYPE_STRING
     */
    public void setAttribute(final String attribute, String value) {
        ensureNonNull("attribute", attribute);
        final Element element = currentElement();
        if (value == null || (value=value.trim()).length() == 0) {
            if (element.hasAttribute(attribute)) {
                element.removeAttribute(attribute);
            }
        } else {
            element.setAttribute(attribute, value);
        }
    }

    /**
     * Sets the attribute to the specified array of values, or remove the attribute if the array
     * is {@code null}. The given items are formatted in a single string with an ordinary space
     * used as the item separator, as mandated by {@link IIOMetadataFormat#VALUE_LIST}. If some
     * of the given items contain spaces, then those spaces are replaced by a no-break space
     * (<code>'\\u00A0'</code>) for avoiding confusion with the space separator.
     *
     * @param attribute The attribute name.
     * @param values The attribute values, or {@code null} for removing the attribute.
     *
     * @see #getAttributeAsStrings(String, boolean)
     *
     * @since 3.06
     */
    public void setAttribute(final String attribute, final String... values) {
        setAttribute(attribute, formatSequence(values));
    }

    /**
     * Sets the attribute to the specified enumeration value,
     * or remove the attribute if the value is null.
     *
     * @param attribute The attribute name.
     * @param value     The attribute value.
     * @param enums     The set of allowed values, or {@code null} if unknown.
     */
    @Deprecated
    final void setAttribute(final String attribute, String value, final Collection<String> enums) {
        if (value != null) {
            value = value.replace('_', ' ').trim();
            for (final String e : enums) {
                if (value.equalsIgnoreCase(e)) {
                    value = e;
                    break;
                }
            }
        }
        setAttribute(attribute, value);
    }

    /**
     * Sets the attribute to the specified code value, or remove the attribute if the value is null.
     *
     * @param attribute The attribute name.
     * @param value The attribute value, or {@code null} for removing the attribute.
     *
     * @see #getAttributeAsCode(String, Class)
     *
     * @since 3.06
     */
    public void setAttribute(final String attribute, final CodeList<?> value) {
        setAttribute(attribute, CodeLists.identifier(value));
    }

    /**
     * Sets the attribute to the specified boolean value.
     *
     * @param attribute The attribute name.
     * @param value The attribute value.
     *
     * @see #getAttributeAsBoolean(String)
     * @see IIOMetadataFormat#DATATYPE_BOOLEAN
     *
     * @since 3.06
     */
    public void setAttribute(final String attribute, final boolean value) {
        setAttribute(attribute, Boolean.toString(value));
    }

    /**
     * Sets the attribute to the specified integer value.
     *
     * @param attribute The attribute name.
     * @param value The attribute value.
     *
     * @see #getAttributeAsInteger(String)
     * @see IIOMetadataFormat#DATATYPE_INTEGER
     */
    public void setAttribute(final String attribute, final int value) {
        setAttribute(attribute, Integer.toString(value));
    }

    /**
     * Set the attribute to the specified array of values,
     * or remove the attribute if the array is {@code null}.
     *
     * @param attribute The attribute name.
     * @param values The attribute values, or {@code null} for removing the attribute.
     *
     * @see #getAttributeAsIntegers(String, boolean)
     */
    public void setAttribute(final String attribute, final int... values) {
        setAttribute(attribute, formatSequence(values));
    }

    /**
     * Sets the attribute to the specified floating point value,
     * or remove the attribute if the value is NaN or infinity.
     *
     * @param attribute The attribute name.
     * @param value The attribute value.
     *
     * @see #getAttributeAsFloat(String)
     * @see IIOMetadataFormat#DATATYPE_FLOAT
     *
     * @since 3.06
     */
    public void setAttribute(final String attribute, final float value) {
        String text = null;
        if (!Float.isNaN(value) && !Float.isInfinite(value)) {
            text = Float.toString(value);
        }
        setAttribute(attribute, text);
    }

    /**
     * Set the attribute to the specified array of values,
     * or remove the attribute if the array is {@code null}.
     *
     * @param attribute The attribute name.
     * @param values The attribute values, or {@code null} for removing the attribute.
     *
     * @see #getAttributeAsDoubles(String, boolean)
     *
     * @since 3.06
     */
    public void setAttribute(final String attribute, final float... values) {
        setAttribute(attribute, formatSequence(values));
    }

    /**
     * Sets the attribute to the specified floating point value,
     * or remove the attribute if the value is NaN or infinity.
     *
     * @param attribute The attribute name.
     * @param value The attribute values.
     *
     * @see #getAttributeAsDouble(String)
     * @see IIOMetadataFormat#DATATYPE_DOUBLE
     */
    public void setAttribute(final String attribute, final double value) {
        String text = null;
        if (!Double.isNaN(value) && !Double.isInfinite(value)) {
            text = Double.toString(value);
        }
        setAttribute(attribute, text);
    }

    /**
     * Set the attribute to the specified array of values,
     * or remove the attribute if the array is {@code null}.
     *
     * @param attribute The attribute name.
     * @param values The attribute values, or {@code null} for removing the attribute.
     *
     * @see #getAttributeAsDoubles(String, boolean)
     */
    public void setAttribute(final String attribute, final double... values) {
        setAttribute(attribute, formatSequence(values));
    }

    /**
     * Sets the attribute to the specified value, or remove the attribute if the value is null.
     *
     * @param attribute The attribute name.
     * @param value The attribute value, or {@code null} for removing the attribute.
     *
     * @see #getAttributeAsDate(String)
     */
    public void setAttribute(final String attribute, final Date value) {
        String text = null;
        if (value != null) {
            if (metadata instanceof SpatialMetadata) {
                text = ((SpatialMetadata) metadata).dateFormat().format(value);
            } else {
                // Inefficient fallback, but should usually not happen anyway.
                text = SpatialMetadata.format(Date.class, value);
            }
        }
        setAttribute(attribute, text);
    }

    /**
     * Sets the attribute to the specified range value.
     *
     * @param attribute The attribute name.
     * @param value The attribute value, or {@code null} for removing the attribute.
     *
     * @see #getAttributeAsRange(String)
     *
     * @since 3.06
     */
    public void setAttribute(final String attribute, final NumberRange<?> value) {
        String text = null;
        if (value != null) {
            if (metadata instanceof SpatialMetadata) {
                text = ((SpatialMetadata) metadata).rangeFormat().format(value);
            } else {
                // Inefficient fallback, but should usually not happen anyway.
                text = SpatialMetadata.format(NumberRange.class, value);
            }
        }
        setAttribute(attribute, text);
    }

    /**
     * Sets the attribute to the specified unit value.
     *
     * @param attribute The attribute name.
     * @param value The attribute value.
     *
     * @see #getAttributeAsUnit(String, Class)
     *
     * @since 3.07
     */
    public void setAttribute(final String attribute, final Unit<?> value) {
        setAttribute(attribute, (value != null) ? value.toString() : null);
    }

    /**
     * Sets the attribute to the specified citation value,
     * or remove the attribute if the value is null.
     *
     * @param attribute The attribute name.
     * @param value The attribute value, or {@code null} for removing the attribute.
     *
     * @see #getAttributeAsCitation(String)
     *
     * @since 3.06
     */
    public void setAttribute(final String attribute, final Citation value) {
        setAttribute(attribute, Citations.getIdentifier(value));
    }

    /**
     * Convenience flavor of {@link #warning(String, int, Object)} with two arguments.
     * We do not use the "variable argument list" syntax because of possible confusion
     * with the {@code Object} type, which is too generic.
     */
    final void warning(final String method, final int key, final Object arg1, final Object arg2) {
        if (!Level.OFF.equals(warningLevel)) {
            warning(method, key, new Object[] {arg1, arg2});
        }
    }

    /**
     * Convenience method for logging a warning. Do not allow overriding, because it
     * would not work for warnings emitted by the {@link #getAttributeAsDate} method.
     */
    final void warning(final String method, final int key, final Object value) {
        if (!Level.OFF.equals(warningLevel)) {
            warning(MetadataAccessor.class, method, Errors.getResources(getLocale()), key, value);
        }
    }

    /**
     * Convenience method for logging a warning from a caller which may be outside this
     * {@code MetadataAccessor} class.
     */
    final void warning(final Class<?> classe, final String method,
            final IndexedResourceBundle resource, final int key, final Object value)
    {
        if (!Level.OFF.equals(warningLevel)) {
            final LogRecord record = resource.getLogRecord(warningLevel, key, value);
            record.setSourceClassName(classe.getName());
            record.setSourceMethodName(method);
            warningOccurred(record);
        }
    }

    /**
     * Convenience flavor of {@link #warning(String, int, Object)} with a message fetched
     * from the given exception. This is invoked when we failed to parse an attribute.
     * <p>
     * We put the name of the exception class in the message only if the exception does
     * not provide a localized message, or that message is made of only one word.
     */
    final void warning(final Class<?> classe, final String method, final Exception exception) {
        if (!Level.OFF.equals(warningLevel)) {
            String message = exception.getLocalizedMessage();
            if (message == null || ((message = message.trim()).indexOf(' ') < 0)) {
                final String word = message;
                message = Classes.getShortClassName(exception);
                if (word != null) {
                    message = message + ": " + word;
                }
            }
            final LogRecord record = new LogRecord(warningLevel, message);
            record.setSourceClassName(classe.getName());
            record.setSourceMethodName(method);
            warningOccurred(record);
        }
    }

    /**
     * Invoked when a warning occured. This method is invoked when some inconsistency
     * has been detected in the spatial metadata. The default implementation tries to
     * send the message to the warning listeners, if possible. The record is actually
     * logged only if no listener can be reached.
     * <p>
     * More specifically, the typical chain of method calls is as below. Note that the
     * actual chain may be different since any of those methods can be overriden, and
     * the {@code ImageReader} can be an {@code ImageWriter} instead.
     * <p>
     * <ol>
     *   <li>{@code MetadataAccessor.warningOccurred(LogRecord)}</li>
     *   <li>{@link SpatialMetadata#warningOccurred(LogRecord)}</li>
     *   <li>{@link org.geotoolkit.image.io.SpatialImageReader#warningOccurred(LogRecord)}</li>
     *   <li>{@link ImageReader#processWarningOccurred(String)}</li>
     *   <li>{@link javax.imageio.event.IIOReadWarningListener#warningOccurred(ImageReader, String)}</li>
     * </ol>
     *
     * @param record The logging record to log.
     * @return {@code true} if the message has been sent to at least one warning listener,
     *         or {@code false} otherwise (either the message has been sent to the logging
     *         system as a fallback, or the {@linkplain #getWarningLevel() warning level}
     *         if {@link Level#OFF OFF}).
     */
    protected boolean warningOccurred(final LogRecord record) {
        if (!Level.OFF.equals(warningLevel)) {
            if (metadata instanceof SpatialMetadata) {
                return ((SpatialMetadata) metadata).warningOccurred(record);
            } else {
                final Logger logger = Logging.getLogger(MetadataAccessor.class);
                record.setLoggerName(logger.getName());
                logger.log(record);
            }
        }
        return false;
    }

    /**
     * Returns the level at which warnings are emitted, or {@link Level#OFF} if they are disabled.
     * The default value is {@link Level#WARNING}.
     * <p>
     * Note that the warnings are effectively sent to the logging framework only if there is
     * no registered Image I/O warning listeners. See the {@link #warningOccurred(LogRecord)}
     * javadoc for details.
     *
     * @return The current level at which warnings are emitted.
     */
    public Level getWarningLevel() {
        return warningLevel;
    }

    /**
     * @deprecated Renamed {@link #getWarningLevel()}.
     *
     * @return The current level at which warnings are emitted.
     */
    @Deprecated
    public Level getWarningsLevel() {
        return getWarningLevel();
    }

    /**
     * Sets the warning level, or disable warnings. By default, warnings are enabled and set to
     * the {@link Level#WARNING WARNING} level. Subclasses way want to temporarily disable the
     * warnings (using the {@link Level#OFF} argument value) when failures are expected as the
     * normal behavior. For example a subclass may invoke {@link #getAttributeAsInteger(String)}
     * and fallback on {@link #getAttributeAsDouble(String)} if the former failed. In such case,
     * the warnings should be disabled for the integer parsing, but not for the floating point
     * parsing.
     * <p>
     * Note that a low warning level like {@link Level#FINE} may prevent the warnings to
     * be sent to the console logger, but does not prevent the warnings to be sent to the
     * Image I/O warning listeners (see {@link #warningOccurred(LogRecord)} javadoc). Only
     * {@link Level#OFF} really disables warnings.
     *
     * @param  level {@link Level#OFF} for disabling warnings, or an other value for enabling them.
     * @return The previous state before this method has been invoked.
     */
    public Level setWarningLevel(final Level level) {
        ensureNonNull("level", level);
        final Level old = warningLevel;
        warningLevel = level;
        return old;
    }

    /**
     * @deprecated Renamed {@link #setWarningLevel(Level)}.
     *
     * @param  level {@link Level#OFF} for disabling warnings, or an other value for enabling them.
     * @return The previous state before this method has been invoked.
     */
    @Deprecated
    public Level setWarningsLevel(final Level level) {
        return setWarningLevel(level);
    }

    /**
     * Returns the locale to use for formatting warnings and error messages.
     * This method delegates to {@link SpatialMetadata#getLocale()} if possible,
     * or returns {@code null} otherwise.
     *
     * @return The locale to use for formatting the warnings, or {@code null}.
     *
     * @since 3.07
     */
    @Override
    public Locale getLocale() {
        return (metadata instanceof Localized) ? ((Localized) metadata).getLocale() : null;
    }

    /**
     * Returns a string representation of the wrapped {@link IIOMetadata} as a tree. The root of
     * the tree contains the class of this accessor and the value defined in the {@link #name()}
     * javadoc. Attributes are leafs formatted as <var>key</var>="<var>value</var>", while elements
     * and child branches.
     * <p>
     * This method is useful for visual check of the {@link IIOMetadata} content and should be
     * used only for debugging purpose. Note that the output may span many lines.
     */
    @Override
    public String toString() {
        return toString(getClass());
    }

    /**
     * Implementation of {@link #toString()} using the name of the given class for formatting
     * the root.
     */
    final String toString(final Class<?> owner) {
        final StringBuilder buffer = new StringBuilder(Classes.getShortName(owner)).append("[\"");
        int offset = buffer.length();
        final String lineSeparator = System.getProperty("line.separator", "\n");
        try {
            Trees.format(Trees.xmlToSwing(parent), buffer, lineSeparator);
        } catch (IOException e) {
            throw new AssertionError(e); // Should never happen.
        }
        offset = buffer.indexOf(lineSeparator, offset); // Should never be -1.
        return buffer.insert(offset, "\"]").toString();
    }
}
