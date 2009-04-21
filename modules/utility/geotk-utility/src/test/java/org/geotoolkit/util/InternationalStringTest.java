/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2004-2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.util;

import java.util.Locale;
import org.geotoolkit.test.Depend;

import org.junit.*;
import static org.junit.Assert.*;
import static org.geotoolkit.test.Commons.*;


/**
 * Tests the various {@link InternationalString} implementations.
 *
 * @author Martin Desruisseaux (IRD)
 * @version 3.0
 *
 * @since 2.1
 */
@Depend(UtilitiesTest.class)
public final class InternationalStringTest {
    /**
     * Tests the {@link SimpleInternationalString} implementation.
     */
    @Test
    public void testSimple() {
        final String message = "This is an unlocalized message";
        final SimpleInternationalString toTest = new SimpleInternationalString(message);
        assertSame("Construction:", message, toTest.toString());
        basicTests(toTest);
    }

    /**
     * Tests the {@link SimpleInternationalString} implementation.
     */
    @Test
    public void testDefault() {
        final String message     = "This is an unlocalized message";
        final String messageEn   = "This is a localized message";
        final String messageFr   = "Voici un message";
        final String messageFrCa = "Caribou!";
        DefaultInternationalString toTest = new DefaultInternationalString();
        basicTests(toTest);
        toTest.add(Locale.ENGLISH, message);
        assertSame("Addition:", message, toTest.toString());
        basicTests(toTest);

        toTest = new DefaultInternationalString(message);
        assertSame("Construction:", message, toTest.toString());
        basicTests(toTest);
        toTest.add(Locale.ENGLISH, messageEn);
        basicTests(toTest);
        toTest.add(Locale.FRENCH,  messageFr);
        basicTests(toTest);
        assertEquals("Unlocalized message:", message,   toTest.toString(null));
        assertEquals("English message:",     messageEn, toTest.toString(Locale.ENGLISH));
        assertEquals("French message:",      messageFr, toTest.toString(Locale.FRENCH));
        assertEquals("French message:",      messageFr, toTest.toString(Locale.CANADA_FRENCH));
        assertNotNull("Other language:",                toTest.toString(Locale.CHINESE));
        toTest.add(Locale.CANADA_FRENCH, messageFrCa);
        basicTests(toTest);
        assertEquals("Unlocalized message:", message,     toTest.toString(null));
        assertEquals("English message:",     messageEn,   toTest.toString(Locale.ENGLISH));
        assertEquals("French message:",      messageFr,   toTest.toString(Locale.FRENCH));
        assertEquals("French message:",      messageFrCa, toTest.toString(Locale.CANADA_FRENCH));
        assertNotNull("Other language:",                  toTest.toString(Locale.CHINESE));
    }

    /**
     * Performs basic test on the given object.
     */
    private static <T extends Comparable<? super T>> void basicTests(final T toTest) {
        assertEquals("CompareTo: ", 0, toTest.compareTo(toTest));
        assertEquals("Equals:", toTest, toTest);
        if (toTest instanceof CharSequence) {
            assertEquals("CharSequence:", toTest.toString(),
                    new StringBuilder((CharSequence) toTest).toString());
        }
        /*
         * Tests serialization
         */
        final Object object = serialize(toTest);
        assertEquals("Serialization:", toTest.getClass(), object.getClass());
        assertEquals("Serialization:", toTest,            object           );
        assertEquals("Hash code:",     toTest.hashCode(), object.hashCode());
    }
}
