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
package org.geotoolkit.metadata;

import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collection;
import java.util.Locale;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.tree.TreeNode;

import org.opengis.util.CodeList;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;

import org.geotoolkit.util.Strings;
import org.geotoolkit.util.converter.Classes;
import org.geotoolkit.util.converter.Numbers;
import org.geotoolkit.util.collection.CheckedCollection;
import org.geotoolkit.internal.jaxb.IdentifierAuthority;
import org.geotoolkit.gui.swing.tree.NamedTreeNode;
import org.geotoolkit.gui.swing.tree.MutableTreeNode;
import org.geotoolkit.gui.swing.tree.DefaultMutableTreeNode;
import org.geotoolkit.gui.swing.tree.Trees;
import org.geotoolkit.internal.CodeLists;
import org.geotoolkit.resources.Errors;

import static org.geotoolkit.metadata.AbstractMetadata.LOGGER;


/**
 * Represents the metadata property as a tree made from {@linkplain TreeNode tree nodes}.
 * Note that while {@link TreeNode} is defined in the {@link javax.swing.tree} package,
 * it can be seen as a data structure independent of Swing.
 * <p>
 * This class is called {@code PropertyTree} because it may implements
 * {@link javax.swing.tree.TreeModel} in some future Geotk implementation.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.19
 *
 * @since 2.4
 * @module
 *
 * @todo It may make sense to refactor this class as a subclass of {@link java.text.Format}
 *       and make it public.
 */
final class PropertyTree {
    /**
     * The default number of significant digits (may or may not be fraction digits).
     */
    private static final int PRECISION = 12;

    /**
     * Maximum number of characters to use in the heuristic titles.
     * This is an arbitrary limit.
     *
     * @since 3.19
     */
    private static final int TITLE_LIMIT = 40;

    /**
     * The kind of objects to use for inferring a title, in preference order.
     * {@code Object.class} must exist and be last.
     *
     * @since 3.19
     */
    private static final Class<?>[] TITLE_TYPES = {
        InternationalString.class, CharSequence.class, CodeList.class, Object.class
    };

    /**
     * The expected standard implemented by the metadata.
     */
    private final MetadataStandard standard;

    /**
     * The locale to use for {@linkplain Date date}, {@linkplain Number number}
     * and {@linkplain InternationalString international string} formatting.
     */
    private final Locale locale;

    /**
     * The object to use for formatting numbers.
     * Will be created only when first needed.
     */
    private transient NumberFormat numberFormat;

    /**
     * The object to use for formatting dates.
     * Will be created only when first needed.
     */
    private transient DateFormat dateFormat;

    /**
     * Creates a new tree builder using the default locale.
     *
     * @param standard The expected standard implemented by the metadata.
     */
    public PropertyTree(final MetadataStandard standard) {
        this(standard, Locale.getDefault());
    }

    /**
     * Creates a new tree builder.
     *
     * @param standard The expected standard implemented by the metadata.
     * @param locale   The locale to use for {@linkplain Date date}, {@linkplain Number number}
     *                 and {@linkplain InternationalString international string} formatting.
     */
    public PropertyTree(final MetadataStandard standard, final Locale locale) {
        this.standard = standard;
        this.locale   = locale;
    }


    // ---------------------- PARSING -------------------------------------------------------------

    /**
     * Fetches values from every nodes of the given tree except the root, and puts them in
     * the given metadata object. The value of the root node is ignored (it is typically
     * just the name of the metadata class).
     * <p>
     * If the given metadata object already contains property values, then the parsing will be
     * merged with the existing values: attributes not defined in the tree will be left unchanged,
     * and collections will be augmented with new entries without change in the previously existing
     * entries.
     *
     * @param  node     The node from which to fetch the values.
     * @param  metadata The metadata where to store the values.
     * @throws ParseException If a value can not be stored in the given metadata object.
     */
    final void parse(final TreeNode node, final Object metadata) throws ParseException {
        final Class<?> type = metadata.getClass();
        final PropertyAccessor accessor = standard.getAccessorOptional(type);
        if (accessor == null) {
            throw new ParseException(Errors.format(Errors.Keys.UNKNOWN_TYPE_$1, type), 0);
        }
        final int duplicated = parse(node, type, metadata, null, accessor);
        if (duplicated != 0) {
            final LogRecord record = Errors.getResources(locale).getLogRecord(
                    Level.WARNING, Errors.Keys.DUPLICATED_VALUES_COUNT_$1, duplicated);
            record.setSourceClassName("AbstractMetadata"); // This is the public API.
            record.setSourceMethodName("parse");
            record.setLoggerName(LOGGER.getName());
            LOGGER.log(record);
        }
    }

    /**
     * Fetches values from every nodes of the given tree except the root, and puts them in
     * the given metadata object. This method invokes itself recursively.
     *
     * @param  node       The node from which to fetch the values.
     * @param  type       The implementation class of the given {@code metadata}.
     * @param  metadata   The metadata where to store the values, or {@code null} if it should
     *                    be created from the given {@code type} when first needed.
     * @param  addTo      If non-null, then metadata objects (including the one provided to this
     *                    method) are added to this collection after they have been parsed.
     * @param  accessor   The object to use for writing in the metadata object.
     * @return The number of duplicated elements, for logging purpose.
     * @throws ParseException If a value can not be stored in the given metadata object.
     */
    private <T> int parse(final TreeNode node, final Class<? extends T> type, T metadata,
            final Collection<? super T> addTo, final PropertyAccessor accessor) throws ParseException
    {
        int duplicated = 0;
        final Set<String> done = new HashSet<String>();
        final int childCount = node.getChildCount();
        for (int i=0; i<childCount; i++) {
            final TreeNode child = node.getChildAt(i);
            final String name = child.toString().trim();
            /*
             * If the node label is something like "[2] A title", then the node should be the
             * container of the element of a collection. This rule is based on the assumption
             * that '[' and ']' are not valid characters for UML or Java identifiers.
             */
            if (addTo != null && isCollectionElement(name)) {
                duplicated += parse(child, type, null, addTo, accessor);
                continue;
            }
            if (!done.add(name)) {
                /*
                 * If a child of the same name occurs one more time, assume that it is starting
                 * a new metadata object. For example if "Individual Name" appears one more time
                 * inside a "Responsible Party" metadata, then what we are building is actually
                 * a collection of Responsible Parties instead than a single instance, and we
                 * are starting a new instance.
                 *
                 * Note that this is not confused with the case where a single "Responsible Party"
                 * has many "Individual Name" (if it was allowed by ISO), since in such case every
                 * names would be childs under the same "Individual Name" child.
                 *
                 * This approach is ambiguous (unless the first child is always a mandatory property)
                 * and actually not needed for parsing the trees formatted by this class. However this
                 * method applies this lenient approach anyway in order to still be able to parse trees
                 * that are not properly formatted, but still understandable in some way.
                 */
                if (addTo == null) {
                    throw new ParseException(Errors.format(Errors.Keys.DUPLICATED_VALUES_FOR_KEY_$1, name), 0);
                }
                // Something would be wrong with the 'done' map if the following assertion fails.
                assert (metadata != null) : done;
                if (!addTo.add(metadata)) {
                    duplicated++;
                }
                done.clear();  // Starts a new cycle.
                done.add(name);
                metadata = null;
            }
            /*
             * Get the type of the child from the method signature. If the type is a collection,
             * get the type of collection element instead than the type of the collection itself.
             */
            final int index = accessor.indexOf(name);
            if (index < 0) {
                throw new ParseException(Errors.format(Errors.Keys.UNKNOWN_PARAMETER_NAME_$1, name), 0);
            }
            Class<?> childType = accessor.type(index, TypeValuePolicy.ELEMENT_TYPE);
            if (childType == null) {
                // The type of the parameter is unknown (actually the message is a bit
                // misleading since it doesn't said that only the type is unknown).
                throw new ParseException(Errors.format(Errors.Keys.UNKNOWN_PARAMETER_$1, name), 0);
            }
            childType = standard.getImplementation(childType);
            /*
             * If the type is an other metadata implementation, invokes this method
             * recursively for every childs of the metadata object we just found.
             */
            Object value;
            final PropertyAccessor ca = standard.getAccessorOptional(childType);
            if (ca != null) {
                value = accessor.get(index, metadata);
                if (value instanceof Collection<?>) {
                    @SuppressWarnings("unchecked") // We will rely on CheckedCollection checks.
                    final Collection<Object> childs = (Collection<Object>) value;
                    duplicated += parse(child, childType, null, childs, ca);
                } else {
                    duplicated += parse(child, childType, value, null, ca);
                }
            } else {
                /*
                 * Otherwise if the type is not an other metadata implementation, we assume that
                 * this is some primitive (numbers, etc.), a date or a string. Stores them in the
                 * current metadata object using the accessor.
                 */
                final int n = child.getChildCount();
                final Object[] values = new Object[n];
                for (int j=0; j<n; j++) {
                    final TreeNode c = child.getChildAt(j);
                    value = getUserObject(c, childType);
                    if (value == null) {
                        final String s = c.toString();
                        if (Number.class.isAssignableFrom(childType)) {
                            value = numberFormat.parse(s);
                        } else if (Date.class.isAssignableFrom(childType)) {
                            value = dateFormat.parse(s);
                        } else {
                            value = s;
                        }
                    }
                    values[j] = value;
                }
                value = Arrays.asList(values);
            }
            /*
             * Now create a new metadata instance if we need to.
             */
            if (metadata == null) {
                metadata = newInstance(type);
            }
            accessor.set(index, metadata, value, false);
        }
        if (addTo != null && metadata != null) {
            if (!addTo.add(metadata)) {
                duplicated++;
            }
        }
        return duplicated;
    }

    /**
     * Returns the user object of the given node if applicable, or {@code null} otherwise. This method
     * returns the user object only if it is of the expected type. We intentionally don't allow object
     * conversion. This is necessary because the user object of a node is often its {@code String} label
     * ({@link DefaultMutableTreeNode#toString()} is implemented as {@code return userObject.toString()}).
     * Consequently this user object is often <strong>not</strong> an object encapsulating the information
     * provided in child nodes. We want to distinguish those cases.
     */
    private static Object getUserObject(final TreeNode node, final Class<?> childType) {
        final Object value = Trees.getUserObject(node);
        return childType.isInstance(value) ? value : null;
    }

    /**
     * Creates a new instance of the given type, wrapping the {@code java.lang.reflect}
     * exceptions in the exception using by our parsing API.
     */
    private static <T> T newInstance(final Class<? extends T> type) throws ParseException {
        try {
            return type.newInstance();
        } catch (Exception cause) { // InstantiationException & IllegalAccessException
            ParseException exception = new ParseException(Errors.format(
                    Errors.Keys.CANT_CREATE_FROM_TEXT_$1, type), 0);
            exception.initCause(cause);
            throw exception;
        }
    }


    // ---------------------- FORMATING -----------------------------------------------------------


    /**
     * Creates a tree for the specified metadata.
     */
    public MutableTreeNode asTree(final Object metadata) {
        final String name = Classes.getShortName(standard.getInterface(metadata.getClass()));
        final DefaultMutableTreeNode root = new NamedTreeNode(localize(name), metadata, true);
        append(root, metadata, 0);
        return root;
    }

    /**
     * Appends the specified value to a branch. The value may be a metadata
     * (treated {@linkplain AbstractMetadata#asMap as a Map} - see below),
     * a collection or a singleton.
     * <p>
     * Map or metadata are constructed as a sub tree where every nodes is a
     * property name, and the childs are the value(s) for that property.
     *
     * @param  branch The node where to add childs.
     * @param  value the value to add as a child.
     * @param  number Greater than 0 if metadata elements (not code list) should be numbered.
     */
    private void append(final DefaultMutableTreeNode branch, final Object value, final int number) {
        if (value == null) {
            return;
        }
        final Map<?,?> asMap;
        final PropertyAccessor accessor;
        if (value instanceof Map<?,?>) {
            /*
             * The value is a Map derived from a metadata object (usually).
             */
            asMap = (Map<?,?>) value;
        } else if (value instanceof AbstractMetadata) {
            /*
             * The value is a metadata object (Geotk implementation).
             */
            asMap = ((AbstractMetadata) value).asMap();
        } else if ((accessor = standard.getAccessorOptional(value.getClass())) != null) {
            /*
             * The value is a metadata object (unknown implementation).
             */
            asMap = new PropertyMap(value, accessor, NullValuePolicy.NON_EMPTY, KeyNamePolicy.JAVABEANS_PROPERTY);
        } else if (value instanceof Collection<?>) {
            /*
             * The value is a collection of any other cases. Add all the childs recursively,
             * putting them in a numbered element if this is needed for avoiding ambiguity.
             */
            final Collection<?> values = (Collection<?>) value;
            int n = (values.size() > 1) ? 1 : 0;
            for (final Object element : values) {
                if (!ignore(element)) {
                    append(branch, element, n);
                    if (n != 0) n++;
                }
            }
            return;
        } else {
            /*
             * The value is anything else, to be converted to String.
             */
            final String asText;
            if (value instanceof CodeList<?>) {
                asText = localize((CodeList<?>) value);
            } else if (value instanceof Date) {
                asText = format((Date) value);
            } else if (value instanceof Number) {
                asText = format((Number) value);
            } else if (value instanceof InternationalString) {
                asText = ((InternationalString) value).toString(locale);
            } else {
                asText = String.valueOf(value);
            }
            assert !isCollectionElement(asText) : asText;
            branch.add(new NamedTreeNode(asText, value, false));
            return;
        }
        /*
         * Appends the specified map (usually a metadata) to a branch. Each map keys
         * is a child in the specified {@code branch}, and each value is a child of
         * the map key. There is often only one value for a map key, but not always;
         * some are collections, which are formatted as many childs for the same key.
         */
        DefaultMutableTreeNode addTo = branch;
        if (number != 0) {
            // String formatted below must comply with the isCollectionElement(...) condition.
            final StringBuilder buffer = new StringBuilder(32);
            buffer.append('[').append(format(number)).append("] ");
            Class<?> type = value.getClass();
            if (standard.isMetadata(type)) {
                type = standard.getInterface(type);
            }
            buffer.append(Strings.camelCaseToSentence(Classes.getShortName(type)));
            String title = getTitle(asMap.values());
            if (title != null) {
                boolean exceed = title.length() > TITLE_LIMIT;
                if (exceed) {
                    title = title.substring(0, TITLE_LIMIT);
                }
                buffer.append(" \u2012 ").append(title);
                if (exceed) {
                    buffer.append('\u2026'); // "..."
                }
            }
            title = buffer.toString();
            assert isCollectionElement(title) : title;
            addTo = new NamedTreeNode(title, value, true);
            branch.add(addTo);
        }
        for (final Map.Entry<?,?> entry : asMap.entrySet()) {
            final Object element = entry.getValue();
            if (!ignore(element)) {
                final String name = localize((String) entry.getKey());
                assert !isCollectionElement(name) : name;
                final DefaultMutableTreeNode child = new NamedTreeNode(name, element, true);
                append(child, element, 0);
                addTo.add(child);
            }
        }
    }

    /**
     * Returns {@code true} if a node having the given label is the container of an element
     * of a collection. The rule is to return {@code true} if the label contains a pattern
     * like {@code "[any characters]"}, and if the characters before that pattern are not
     * valid identifier part.
     * <p>
     * The search for {@code '['} and {@code ']'} characters are okay if we assume that
     * those characters are not allowed in a UML or Java identifiers.
     *
     * @param  label The label of the node to test.
     * @return {@code true} if a node having the given label is an element of a collection.
     */
    private static boolean isCollectionElement(final String label) {
        int start = label.indexOf('[');
        if (start >= 0) {
            final int end = label.indexOf(']', start);
            if (end > start) {
                while (--start >= 0) {
                    if (Character.isJavaIdentifierPart(label.charAt(start))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to figure out a title for the given metadata (represented as a the values
     * of a Map) and appends that title to the given buffer. If no title can be found,
     * return {@code null}.
     *
     * @param  values The values of the metadata for which to append a title.
     * @return The title, or {@code null} if none were found.
     */
    private String getTitle(final Collection<?> values) {
        String shortestName  = null;
        for (int i=0; i<TITLE_TYPES.length-1; i++) { // Exclude the last type, which is Object.class
            final Class<?> baseType = TITLE_TYPES[i];
            for (final Object element : values) {
                if (baseType.isInstance(element)) {
                    String name;
                    if (element instanceof InternationalString) {
                        name = ((InternationalString) element).toString(locale);
                    } else if (element instanceof CodeList<?>) {
                        name = CodeLists.localize((CodeList<?>) element, locale);
                    } else {
                        name = element.toString();
                    }
                    if (name != null && !(name = name.trim()).isEmpty()) {
                        if (shortestName == null || name.length() < shortestName.length()) {
                            shortestName = name;
                        }
                    }
                }
            }
            if (shortestName != null) {
                return shortestName;
            }
        }
        /*
         * If no title were found, search if any element is a collection and search again in
         * collection elements. Return the shortest suitable element found, if any. We do this
         * check after the above loop because we want singleton element to have precedence.
         */
        final Collection<?>[] collections = new Collection<?>[values.size()];
        int count = 0;
        for (final Object element : values) {
            if (element instanceof Collection<?>) {
                collections[count++] = (Collection<?>) element;
            }
        }
        for (final Class<?> baseType : TITLE_TYPES) {
            int newCount = 0;
            for (int i=0; i<count; i++) {
                final Collection<?> collection = collections[i];
                if (baseType.isAssignableFrom((collection instanceof CheckedCollection<?>) ?
                        ((CheckedCollection<?>) collection).getElementType() : Object.class))
                {
                    final String name = getTitle(collection);
                    if (name != null && (shortestName == null || name.length() < shortestName.length())) {
                        shortestName = name;
                    }
                } else {
                    collections[newCount++] = collection;
                }
            }
            count = newCount;
            if (shortestName != null) {
                return shortestName;
            }
        }
        return null;
    }

    /**
     * Formats the specified number.
     */
    private String format(final Number value) {
        if (numberFormat == null) {
            numberFormat = NumberFormat.getNumberInstance(locale);
            numberFormat.setMinimumFractionDigits(0);
        }
        int precision = 0;
        if (!Numbers.isInteger(value.getClass())) {
            precision = PRECISION;
            final double v = Math.abs(value.doubleValue());
            if (v > 0) {
                final int digits = (int) Math.log10(v);
                if (Math.abs(digits) >= PRECISION) {
                    // TODO: Switch to exponential notation when a convenient API will be available in J2SE.
                    return value.toString();
                }
                if (digits >= 0) {
                    precision -= digits;
                }
                precision = Math.max(0, PRECISION - precision);
            }
        }
        numberFormat.setMaximumFractionDigits(precision);
        return numberFormat.format(value);
    }

    /**
     * Formats the specified date.
     */
    private String format(final Date value) {
        if (dateFormat == null) {
            dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
        }
        return dateFormat.format(value);
    }

    /**
     * Implementation of {@link #ignore(Object)} without the recursive checks in collections.
     * We usually don't need this recursivity, and omitting it allows us to omit the checks
     * against infinite recursivity.
     */
    private static boolean ignoreElement(final Object value) {
        return PropertyAccessor.isEmpty(value) || (value instanceof Identifier &&
                ((Identifier) value).getAuthority() instanceof IdentifierAuthority<?>);
    }

    /**
     * Returns {@code true} if the given element should be ignored. An element should be ignored
     * if empty, or if it is a XML identifier (because we marshal them in a special way), or a
     * collection containing only elements to omit.
     *
     * @since 3.19
     */
    private static boolean ignore(final Object value) {
        if (ignoreElement(value)) {
            return true;
        }
        if (!(value instanceof Iterable<?>)) {
            return false;
        }
        for (final Object element : (Iterable<?>) value) {
            if (!ignoreElement(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Localize the specified property name. In current version, this is merely
     * a hook for future development. For now we reformat the programmatic name.
     * <p>
     * NOTE: If we localize the name, then we must find some way to allow the reverse
     * association in the {@link #parse(AbstractMetadata, TreeNode)} method.
     */
    private String localize(String name) {
        name = name.trim();
        final int length = name.length();
        if (length != 0) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append(Character.toUpperCase(name.charAt(0)));
            boolean previousIsUpper = true;
            int base = 1;
            for (int i=1; i<length; i++) {
                final boolean currentIsUpper = Character.isUpperCase(name.charAt(i));
                if (currentIsUpper != previousIsUpper) {
                    /*
                     * When a case change is detected (lower case to upper case as in "someName",
                     * or "someURL", or upper case to lower case as in "HTTPProxy"), then insert
                     * a space just before the upper case letter.
                     */
                    int split = i;
                    if (previousIsUpper) {
                        split--;
                    }
                    if (split > base) {
                        buffer.append(name.substring(base, split)).append(' ');
                        base = split;
                    }
                }
                previousIsUpper = currentIsUpper;
            }
            final String candidate = buffer.append(name.substring(base)).toString();
            if (!candidate.equals(name)) {
                // Holds a reference to this new String object only if it worth it.
                name = candidate;
            }
        }
        return name;
    }

    /**
     * Localize the specified property name. In current version, this is merely
     * a hook for future development.  For now we reformat the programmatic name.
     */
    private String localize(final CodeList<?> code) {
        return code.name().trim().replace('_', ' ').toLowerCase(locale);
    }

    /**
     * Returns a string representation of the specified tree node.
     */
    public static String toString(final TreeNode node) {
        final StringBuilder buffer = new StringBuilder();
        toString(node, buffer, 0, System.getProperty("line.separator", "\n"));
        return buffer.toString();
    }

    /**
     * Append a string representation of the specified node to the specified buffer.
     */
    private static void toString(final TreeNode      node,
                                 final StringBuilder buffer,
                                 final int           indent,
                                 final String        lineSeparator)
    {
        final int count = node.getChildCount();
        if (count == 0) {
            if (node.isLeaf()) {
                /*
                 * If the node has no child and is a leaf, then it is some value like a number,
                 * a date or a string.  We just display this value, which is usually part of a
                 * collection. If the node has no child and is NOT a leaf, then it is an empty
                 * metadata and we just omit it.
                 */
                buffer.append(Strings.spaces(indent)).append(node).append(lineSeparator);
            }
            return;
        }
        buffer.append(Strings.spaces(indent)).append(node).append(':');
        if (count == 1) {
            final TreeNode child = node.getChildAt(0);
            if (child.isLeaf()) {
                buffer.append(' ').append(child).append(lineSeparator);
                return;
            }
        }
        for (int i=0; i<count; i++) {
            final TreeNode child = node.getChildAt(i);
            if (i == 0) {
                buffer.append(lineSeparator);
            }
            toString(child, buffer, indent+2, lineSeparator);
        }
    }
}
