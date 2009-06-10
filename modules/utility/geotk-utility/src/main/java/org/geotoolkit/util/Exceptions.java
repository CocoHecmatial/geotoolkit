/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2001-2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.util;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.sql.SQLException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.geotoolkit.lang.Static;
import org.geotoolkit.io.ExpandedTabWriter;


/**
 * Utilities methods for dealing with exceptions.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.01
 *
 * @since 2.0
 * @module
 */
@Static
public final class Exceptions {
    /**
     * Number of spaces to leave between each tab.
     */
    private static final int TAB_WIDTH = 4;

    /**
     * Do not allow instantiation of this class.
     */
    private Exceptions() {
    }

    /**
     * Returns a string which contain the given message on the first line, followed by the
     * {@linkplain Throwable#getLocalizedMessage() localized message} of the given exception
     * on the next line. If the exception has a {@linkplain Throwable#getCause() causes}, then
     * the localized message of the cause is formatted on the next line and the process is
     * repeated for the whole cause chain.
     * <p>
     * {@link SQLException} is handled especially in order to process the
     * {@linkplain SQLException#getNextException() next exception} instead than the cause.
     *
     * @param  header The message to insert on the first line, or {@code null} if none.
     * @param  cause  The exception, or {@code null} if none.
     * @return The formatted message, or {@code null} if both the header was {@code null}
     *         and no exception provide a message.
     *
     * @since 3.01
     */
    public static String formatMessages(String header, Throwable cause) {
        Set<String> done = null;
        String lineSeparator = null;
        StringBuilder buffer = null;
        while (cause != null) {
            String message = cause.getLocalizedMessage();
            if (message != null && (message = message.trim()).length() != 0) {
                if (buffer == null) {
                    done = new HashSet<String>();
                    buffer = new StringBuilder();
                    lineSeparator = System.getProperty("line.separator", "\n");
                    if (header != null && (header = header.trim()).length() != 0) {
                        buffer.append(header);
                    }
                }
                if (done.add(message)) {
                    if (buffer.length() != 0) {
                        buffer.append(lineSeparator);
                    }
                    buffer.append(message);
                }
            }
            if (cause instanceof SQLException) {
                final SQLException next = ((SQLException) cause).getNextException();
                if (next != null) {
                    cause = next;
                    continue;
                }
            }
            cause = cause.getCause();
        }
        if (buffer != null) {
            header = buffer.toString();
        }
        return header;
    }

    /**
     * Returns the exception trace as a string. Tabulation characters will have been
     * replaced by 4 white spaces.
     *
     * @param exception The exception to format.
     * @return A string representation of the given exception.
     */
    public static String formatStackTrace(final Throwable exception) {
        final StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(new ExpandedTabWriter(writer, TAB_WIDTH)));
        return writer.toString();
    }

    /**
     * Writes the specified exception trace in the specified graphics context. This method is
     * useful when an exception has occurred inside a {@link java.awt.Component#paint} method
     * and we want to write it rather than leaving an empty window.
     *
     * @param graphics Graphics context in which to write exception. The graphics context shall
     *        be in its initial state (default affine transform, default color, <cite>etc.</cite>)
     * @param widgetBounds Size of the trace which was being drawn.
     * @param exception Exception whose trace we want to write.
     */
    public static void paintStackTrace(final Graphics2D graphics,
                                       final Rectangle  widgetBounds,
                                       final Throwable  exception)
    {
        /*
         * Obtains the exception trace in the form of a character chain.
         * The carriage returns in this chain can be "\r", "\n" or "r\n".
         */
        final String message = formatStackTrace(exception);
        /*
         * Examines the character chain line by line.
         * "Glyphs" will be created as we go along and we will take advantage
         * of this to calculate the necessary space.
         */
        double width = 0, height = 0;
        final List<GlyphVector> glyphs = new ArrayList<GlyphVector>();
        final List<Rectangle2D> bounds = new ArrayList<Rectangle2D>();
        final int length = message.length();
        final Font font = graphics.getFont();
        final FontRenderContext context = graphics.getFontRenderContext();
        for (int i = 0; i < length;) {
            int ir = message.indexOf('\r', i);
            int in = message.indexOf('\n', i);
            if (ir < 0) ir = length;
            if (in < 0) in = length;
            final int irn = Math.min(ir, in);
            final GlyphVector line = font.createGlyphVector(context, message.substring(i, irn));
            final Rectangle2D rect = line.getVisualBounds();
            final double w = rect.getWidth();
            if (w > width) width = w;
            height += rect.getHeight();
            glyphs.add(line);
            bounds.add(rect);
            i = (Math.abs(ir - in) <= 1 ? Math.max(ir, in) : irn) + 1;
        }
        /*
         * Proceeds to draw all the previously calculated glyphs.
         */
        float xpos = (float) (0.5 * (widgetBounds.width - width));
        float ypos = (float) (0.5 * (widgetBounds.height - height));
        final int size = glyphs.size();
        for (int i = 0; i < size; i++) {
            final GlyphVector line = glyphs.get(i);
            final Rectangle2D rect = bounds.get(i);
            ypos += rect.getHeight();
            graphics.drawGlyphVector(line, xpos, ypos);
        }
    }
}
