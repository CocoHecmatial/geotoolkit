/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010-2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2010-2011, Geomatys
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
package org.geotoolkit.test;

import java.io.IOException;
import java.util.Enumeration;
import java.awt.image.RenderedImage;
import javax.swing.tree.TreeNode;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.geotoolkit.test.xml.DomComparator;


/**
 * Assertion methods used by the Geotk project in addition of the JUnit and GeoAPI assertions.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.17
 *
 * @since 3.16 (derived from 3.00)
 */
public class Assert extends org.opengis.test.Assert {
    /**
     * For subclass constructor only.
     */
    protected Assert() {
    }

    /**
     * Asserts that two strings are equal, ignoring the differences in EOL characters.
     * The comparisons is performed one a line-by-line basis. For each line, leading
     * and trailing spaces are ignored in order to make the comparison independent of
     * indentation.
     *
     * @param expected The expected string.
     * @param actual The actual string.
     */
    public static void assertMultilinesEquals(final String expected, final String actual) {
        assertMultilinesEquals(null, expected, actual);
    }

    /**
     * Asserts that two strings are equal, ignoring the differences in EOL characters.
     * The comparisons is performed one a line-by-line basis. For each line, leading
     * and trailing spaces are ignored in order to make the comparison independent of
     * indentation.
     *
     * @param message  The message to print in case of failure, or {@code null} if none.
     * @param expected The expected string.
     * @param actual The actual string.
     */
    public static void assertMultilinesEquals(final String message, final String expected, final String actual) {
        final StringBuilder buffer = new StringBuilder();
        if (message != null) {
            buffer.append(message).append(" at line ");
        } else {
            buffer.append("Line ");
        }
        int line = 0;
        final int length = buffer.length();
        final StringIterator c1 = new StringIterator(expected);
        final StringIterator c2 = new StringIterator(actual);
        while (c1.hasNext()) {
            final String next = c1.next().trim();
            if (!c2.hasNext()) {
                fail("Expected more lines: " + next);
            }
            buffer.setLength(length);
            buffer.append(++line);
            assertEquals(buffer.toString(), next, c2.next().trim());
        }
        if (c2.hasNext()) {
            fail("Unexpected line: " + c2.next());
        }
    }

    /**
     * Asserts that two strings formatted by the XML marshaller are equal. The XML header is
     * skipped, then the remaining is compared as with {@link #assertMultilinesEquals(String,
     * String)}.
     *
     * @param expected The expected string.
     * @param actual The actual string.
     *
     * @since 3.07
     */
    public static void assertXmlEquals(String expected, String actual) {
        expected = skipHeader(expected);
        actual   = skipHeader(actual);
        assertMultilinesEquals(expected, actual);
    }

    /**
     * Skips the two first lines, because the xlmns are not always in the same order.
     */
    private static String skipHeader(final String xml) {
        return xml.substring(xml.indexOf('\n', xml.indexOf('\n') + 1) + 1);
    }

    /**
     * Parses two XML tree as DOM documents, and compares the nodes.
     * The inputs given to this method can be any of the following types:
     * <p>
     * <ul>
     *   <li>{@link org.w3c.dom.Node}; used directly without further processing.</li>
     *   <li>{@link java.io.File}, {@link java.net.URL} or {@link java.net.URI}: the
     *       stream is opened and parsed as a XML document.</li>
     *   <li>{@link String}: The string content is parsed directly as a XML document.
     *       Encoding <strong>must</strong> be UTF-8 (no other encoding is supported
     *       by current implementation of this method).</li>
     * </ul>
     * <p>
     * This method will ignore comments and the following attributes:
     * <p>
     * <ul>
     *   <li>All namespace declarations ({@code "xmlns:*"})</li>
     *   <li>{@code "xsi:schemaLocation"}</li>
     *   <li>{@code "xsi:type"}</li>
     * </ul>
     * <p>
     * In order to customize the list of attributes to ignore, users shall use
     * {@link DomComparator} directly.
     *
     * @param  expected     The expected XML document.
     * @param  actual       The XML document to compare.
     * @throws IOException  If the stream can not be read.
     * @throws SAXException If an error occurred while parsing the XML document.
     *
     * @see DomComparator
     *
     * @since 3.17
     */
    public static void assertDomEquals(final Object expected, final Object actual)
            throws IOException, SAXException
    {
        final DomComparator comparator;
        try {
            comparator = new DomComparator(expected, actual);
        } catch (ParserConfigurationException e) {
            // In order to simplify a little bit the usage of this method, we do declare this
            // checked exception since it should not happen in the controlled test environment.
            throw new AssertionError(e);
        }
        comparator.ignoreComments = true;
        comparator.ignoredAttributes.add("xmlns:*");
        comparator.ignoredAttributes.add("xsi:schemaLocation");
        comparator.ignoredAttributes.add("xsi:type");
        comparator.compare();
    }

    /**
     * Ensures that a tree is equals to an other tree. This method invokes itself
     * recursively for every child nodes.
     *
     * @param  expected The expected tree, or {@code null}.
     * @param  actual The tree to compare with the expected one, or {@code null}.
     * @return The number of nodes.
     *
     * @since 3.04
     */
    public static int assertTreeEquals(final TreeNode expected, final TreeNode actual) {
        if (expected == null) {
            assertNull(actual);
            return 0;
        }
        int n = 1;
        assertNotNull(actual);
        assertEquals("isLeaf()",            expected.isLeaf(),            actual.isLeaf());
        assertEquals("getAllowsChildren()", expected.getAllowsChildren(), actual.getAllowsChildren());
        assertEquals("getChildCount()",     expected.getChildCount(),     actual.getChildCount());
        @SuppressWarnings("unchecked") final Enumeration<? extends TreeNode> ec = expected.children();
        @SuppressWarnings("unchecked") final Enumeration<? extends TreeNode> ac = actual  .children();

        int childIndex = 0;
        while (ec.hasMoreElements()) {
            assertTrue("hasMoreElements()", ac.hasMoreElements());
            final TreeNode nextExpected = ec.nextElement();
            final TreeNode nextActual   = ac.nextElement();
            final String message = "getChildAt(" + childIndex + ')';
            assertSame(message, nextExpected, expected.getChildAt(childIndex));
            assertSame(message, nextActual,   actual  .getChildAt(childIndex));
            assertSame("getParent()", expected, nextExpected.getParent());
            assertSame("getParent()", actual,   nextActual  .getParent());
            assertSame("getIndex(TreeNode)", childIndex, expected.getIndex(nextExpected));
            assertSame("getIndex(TreeNode)", childIndex, actual  .getIndex(nextActual));
            n += assertTreeEquals(nextExpected, nextActual);
            childIndex++;
        }
        assertFalse("hasMoreElements()", ac.hasMoreElements());
        assertEquals("toString()", expected.toString(), actual.toString());
        return n;
    }

    /**
     * Asserts that two images have the same origin and the same size.
     *
     * @param expected The image having the expected size.
     * @param actual   The image to compare with the expected one.
     */
    public static void assertSameImageBounds(final RenderedImage expected, final RenderedImage actual) {
        assertEquals("Min X",  expected.getMinX(),   actual.getMinX());
        assertEquals("Min Y",  expected.getMinY(),   actual.getMinY());
        assertEquals("Width",  expected.getWidth(),  actual.getWidth());
        assertEquals("Height", expected.getHeight(), actual.getHeight());
    }
}
