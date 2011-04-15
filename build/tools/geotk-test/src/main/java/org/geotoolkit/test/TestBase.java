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

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.Console;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.*;
import static org.junit.Assert.*;


/**
 * Base class of Geotoolkit.org tests. This base class provides some configuration that
 * are commons to all subclasses.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.18
 *
 * @since 3.16
 */
public abstract class TestBase {
    /**
     * The separator characters used for reporting the verbose output.
     */
    private static final String SEPARATOR = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

    /**
     * The name of a system property for setting whatever the tests should provide verbose output.
     * If the value returned by the following is {@code true}, then the {@link #out} field will be
     * set to a non-null value:
     *
     * {@preformat java
     *     Boolean.getBoolean(VERBOSE_KEY);
     * }
     *
     * The value of this property key is {@value}.
     *
     * @see org.geotoolkit.test.gui.SwingTestBase#SHOW_PROPERTY_KEY
     *
     * @since 3.18
     */
    public static final String VERBOSE_KEY = "org.geotoolkit.test.verbose";

    /**
     * If verbose output are enabled, the output stream where to print the output.
     * Otherwise {@code null}.
     *
     * @since 3.18
     */
    protected static final PrintWriter out;

    /**
     * The buffer which is backing the {@linkplain #out} stream, or {@code null} if none.
     */
    private static final StringWriter buffer;

    /**
     * Invokes a method of {@link org.geotoolkit.util.logging.Logging#GEOTOOLKIT}.
     */
    static void invokeLogging(final String method, final Class<?>[] argTypes, final Object[] argValues) {
        try {
            final Class<?> logging = Class.forName("org.geotoolkit.util.logging.Logging");
            logging.getMethod(method, argTypes).invoke(logging.getField("GEOTOOLKIT").get(null), argValues);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Configures the logging handler and the logging level to use for the test suite.
     * This method uses reflection for installing the handler provided in Geotk.
     */
    static {
        invokeLogging("forceMonolineConsoleOutput", new Class<?>[] {Level.class}, new Object[] {
                Boolean.getBoolean(VERBOSE_KEY) ? Level.FINE : null});
        if (Boolean.getBoolean(VERBOSE_KEY)) {
            out = new PrintWriter(buffer = new StringWriter());
        } else {
            buffer = null;
            out = null;
        }
    }

    /**
     * Date parser, created when first needed.
     */
    private transient DateFormat dateFormat;

    /**
     * Creates a new test case.
     */
    protected TestBase() {
    }

    /**
     * If verbose output were enabled, flush the {@link #out} stream to the console.
     * This method is invoked automatically by JUnit and doesn't need to be invoked
     * explicitely.
     */
    @AfterClass
    public static void flushVerboseOutput() {
        invokeLogging("flush", null, null);
        System.out.flush();
        System.err.flush();
        if (out != null) {
            out.flush();
            final String content = buffer.toString();
            if (content.length() != 0) {
                final Console console = System.console();
                if (console != null) {
                    final PrintWriter w = console.writer();
                    w.println(SEPARATOR);
                    w.println(content);
                    w.println(SEPARATOR);
                } else {
                    final PrintStream w = System.out;
                    w.println(SEPARATOR);
                    w.println(content);
                    w.println(SEPARATOR);
                }
                buffer.getBuffer().setLength(0);
            }
        }
    }

    /**
     * Returns the date format.
     */
    private DateFormat getDateFormat() {
        DateFormat df = dateFormat;
        if (df == null) {
            dateFormat = df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            df.setLenient(false);
        }
        return df;
    }

    /**
     * Parses the date for the given string using the {@code "yyyy-MM-dd HH:mm:ss"} pattern
     * in UTC timezone.
     *
     * @param  date The date as a {@link String}.
     * @return The date as a {@link Date}.
     *
     * @since 3.15
     */
    protected final synchronized Date date(final String date) {
        assertNotNull("A date must be specified", date);
        final DateFormat dateFormat = getDateFormat();
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Formats the given date using the {@code "yyyy-MM-dd HH:mm:ss"} pattern in UTC timezone.
     *
     * @param  date The date to format.
     * @return The date as a {@link String}.
     *
     * @since 3.17
     */
    protected final synchronized String format(final Date date) {
        assertNotNull("A date must be specified", date);
        return getDateFormat().format(date);
    }
}
