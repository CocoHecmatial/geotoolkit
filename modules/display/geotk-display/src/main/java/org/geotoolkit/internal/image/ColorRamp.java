/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 1999-2010, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.internal.image;

import javax.swing.SwingConstants;

import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.AbstractMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import javax.measure.unit.Unit;

import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.PaletteInterpretation;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;

import org.geotoolkit.util.logging.Logging;
import org.geotoolkit.util.converter.Classes;
import org.geotoolkit.display.axis.Graduation;
import org.geotoolkit.display.axis.TickIterator;
import org.geotoolkit.display.axis.NumberGraduation;
import org.geotoolkit.display.axis.AbstractGraduation;
import org.geotoolkit.display.axis.LogarithmicNumberGraduation;
import org.geotoolkit.coverage.GridSampleDimension;
import org.geotoolkit.resources.Loggings;
import org.geotoolkit.resources.Errors;


/**
 * Paints color ramps with a graduation. This class provides the implementation of
 * {@link org.geotoolkit.gui.swing.image.ColorRamp}. It has been factored out in order
 * to be leveraged in other modules without introducing a dependency to Swing widgets.
 *
 * @author Martin Desruisseaux (MPO, IRD, Geomatys)
 * @version 3.10
 *
 * @since 3.10 (derived from 1.1)
 * @module
 */
@SuppressWarnings("serial") // Used only for Swing serialization.
public class ColorRamp implements Serializable {
    /**
     * Margin (in pixel) on each sides: top, left, right and bottom of the color ramp.
     */
    public static final int MARGIN = 10;

    /**
     * Small tolerance factor ror rounding error.
     */
    private static final double EPS = 1E-6;

    /**
     * The graduation to write over the color ramp.
     */
    private Graduation graduation;

    /**
     * Graduation units. This is constructed from {@link Graduation#getUnit()} and cached
     * for faster rendering.
     */
    private String units;

    /**
     * The colors to paint as ARGB values (never {@code null}).
     */
    private int[] colors = new int[0];

    /**
     * {@code true} if tick labels shall be painted.
     */
    public boolean labelVisibles = true;

    /**
     * {@code true} if tick labels can be painted with an automatic color. The
     * automatic color will be white or black depending on the background color.
     */
    public boolean autoForeground = true;

    /**
     * {@code true} if the color bar should be drawn horizontally,
     * or {@code false} if it should be drawn vertically.
     */
    private boolean horizontal = true;

    /**
     * Rendering hints for the graduation. This include the color bar
     * length, which is used for the space between ticks.
     */
    private transient RenderingHints hints;

    /**
     * The tick iterator used during the last painting. This iterator will be reused as mush
     * as possible in order to reduce garbage-collections.
     */
    private transient TickIterator reuse;

    /**
     * A temporary buffer for conversions from RGB to HSB
     * values. This is used by {@link #getForeground(int)}.
     */
    private transient float[] HSB;

    /**
     * Constructs an initially empty color ramp. Colors can be
     * set using one of the {@code setColors(...)} methods.
     */
    public ColorRamp() {
    }

    /**
     * Returns {@code false} if the methods having a {@code Color[][]} return type are allowed
     * to return {@code null} inconditionaly. This is more efficient for callers which are not
     * interrested to fire property change events.
     * <p>
     * The default implementation returns {@code false} in every cases. Subclasses shall
     * override this method with a cheap test if they want to be informed about changes.
     *
     * @return Whatever the caller wants to be informed about color changes.
     */
    protected boolean reportColorChanges() {
        return false;
    }

    /**
     * Returns the graduation to paint over colors. If the graduation is
     * not yet defined, then this method returns {@code null}.
     *
     * @return The graduation to draw.
     */
    public final Graduation getGraduation() {
        return graduation;
    }

    /**
     * Sets the graduation to paint on top of the color bar.
     * The graduation minimum and maximum values should be both inclusive.
     *
     * @param  graduation The new graduation, or {@code null} if none.
     * @return The old graduation, or {@code null} if none.
     */
    public final Graduation setGraduation(final Graduation graduation) {
        final Graduation oldGraduation = this.graduation;
        if (graduation != oldGraduation) {
            this.graduation = graduation;
            units = null;
            if (graduation != null) {
                final Unit<?> unit = graduation.getUnit();
                if (unit != null) {
                    units = unit.toString();
                }
            }
        }
        return oldGraduation;
    }

    /**
     * Returns {@code true} if some colors are defined.
     *
     * @return {@code true} if some colors are defined.
     */
    public final boolean hasColors() {
        return colors != null;
    }

    /**
     * Returns the colors painted by this {@code ColorRamp}.
     *
     * @return The colors (never {@code null}).
     */
    public final Color[] getColors() {
        return getColors(colors, new HashMap<Integer,Color>());
    }

    /**
     * Creates an array of {@link Color} values from the given array of ARGB values.
     *
     * @param  ARGB  The array of ARGB values.
     * @param  share A map of {@link Color} instances previously created, or an empty map if none.
     * @return The array of color instances.
     */
    private static Color[] getColors(final int[] ARGB, final Map<Integer,Color> share) {
        final Color[] colors = new Color[ARGB.length];
        for (int i=0; i<colors.length; i++) {
            final Integer value = ARGB[i];
            Color ci = share.get(value);
            if (ci == null) {
                ci = new Color(value, true);
                share.put(value, ci);
            }
            colors[i] = ci;
        }
        return colors;
    }

    /**
     * Sets the colors to paint.
     *
     * @param  colors The colors to paint, or {@code null} if none.
     * @return The old and new colors, or {@code null} if there is no change.
     */
    public final Color[][] setColors(final Color[] colors) {
        final Map<Integer,Color> share = new HashMap<Integer,Color>();
        int[] ARGB = null;
        if (colors != null) {
            ARGB = new int[colors.length];
            for (int i=0; i<colors.length; i++) {
                final Color c = colors[i];
                share.put(ARGB[i] = c.getRGB(), c);
            }
        }
        return setColors(ARGB, share);
    }

    /**
     * Sets the colors to paint as an array of ARGB values. This method is the most
     * efficient one if the colors were already available as an array of ARGB values.
     *
     * @param  colors The colors to paint, or {@code null} if none.
     * @return The old and new colors, or {@code null} if there is no change.
     */
    public final Color[][] setColors(final int[] colors) {
        return setColors((colors != null) ? colors.clone() : null, null);
    }

    /**
     * Sets the colors to paint as an array of ARGB values.
     *
     * @param  newColors The colors to paint, or {@code null} if none.
     * @param  share A map of {@link Color} instances previously created, or {@code null} if none.
     * @return The old and new colors, or {@code null} if there is no change.
     */
    private Color[][] setColors(int[] newColors, Map<Integer,Color> share) {
        if (newColors == null) {
            newColors = new int[0];
        }
        final int[] oldColors = colors;
        colors = newColors;
        if (!reportColorChanges() || Arrays.equals(oldColors, newColors)) {
            return null;
        }
        if (share == null) {
            share = new HashMap<Integer,Color>();
        }
        return new Color[][] {getColors(oldColors, share), getColors(newColors, share)};
    }

    /**
     * Sets the graduation and the colors from a sample dimension.
     *
     * @param band The sample dimension, or {@code null} if none.
     */
    public final void setColors(final SampleDimension band) {
        final Map.Entry<Graduation,Color[]> entry = getColors(band);
        setGraduation(entry.getKey());
        setColors(entry.getValue());
    }

    /**
     * Returns the graduation and the colors from a sample dimension. This is caller
     * responsability to invoke {@code setColors} and {@code setGraduation} with the
     * returned values.
     *
     * @param  band The sample dimension, or {@code null} if none.
     * @return The pair of graduation and colors.
     */
    @SuppressWarnings("fallthrough")
    public final Map.Entry<Graduation,Color[]> getColors(SampleDimension band) {
        Color[] colors = null;
        Graduation graduation = null;
        /*
         * Gets the color palette, preferably from the "non-geophysics" view since it is usually
         * the one backed by an IndexColorModel.  We assume that 'palette[i]' gives the color of
         * sample value 'i'. We will search for the largest range of valid sample integer values,
         * ignoring "nodata" values. Those "nodata" values appear usually at the begining or at
         * the end of the whole palette range.
         *
         * Note that the above algorithm works without Category. We try to avoid dependency
         * on categories because some applications don't use them. TODO: should we use this
         * algorithm only as a fallback (i.e. use categories when available)?
         */
        if (band != null) {
            if (band instanceof GridSampleDimension) {
                band = ((GridSampleDimension) band).geophysics(false);
            }
            final int[][] palette = band.getPalette();
            if (palette != null) {
                int lower = 0; // Will be inclusive
                int upper = 0; // Will be exclusive
                final double[] nodata = band.getNoDataValues();
                final double[] sorted = new double[nodata!=null ? nodata.length + 2 : 2];
                sorted[0] = -1;
                sorted[sorted.length - 1] = palette.length;
                if (nodata != null) {
                    System.arraycopy(nodata, 0, sorted, 1, nodata.length);
                }
                Arrays.sort(sorted);
                for (int i=1; i<sorted.length; i++) {
                    // Note: Don't cast to integer now, because we
                    // want to take NaN and infinity in account.
                    final double lo = Math.floor(sorted[i-1])+1; // "Nodata" always excluded
                    final double hi = Math.ceil (sorted[i  ]);   // "Nodata" included if integer
                    if (lo>=0 && hi<=palette.length && (hi-lo)>(upper-lower)) {
                        lower = (int) lo;
                        upper = (int) hi;
                    }
                }
                /*
                 * We now know the range of values to show on the palette. Creates the colors from
                 * the palette. Only palette using RGB colors are understood at this time, but the
                 * graduation (after this block) is still created for all kind of palette.
                 */
                if (PaletteInterpretation.RGB.equals(band.getPaletteInterpretation())) {
                    colors = new Color[upper - lower];
                    for (int i=0; i<colors.length; i++) {
                        int r=0, g=0, b=0, a=255;
                        final int[] c = palette[i+lower];
                        if (c != null) switch (c.length) {
                            default:        // Fall through
                            case 4: a=c[3]; // Fall through
                            case 3: b=c[2]; // Fall through
                            case 2: g=c[1]; // Fall through
                            case 1: r=c[0]; // Fall through
                            case 0: break;
                        }
                        colors[i] = new Color(r,g,b,a);
                    }
                }
                /*
                 * Transforms the lower and upper sample values into minimum and maximum geophysics
                 * values and creates the graduation. Note that the maximum value will be inclusive,
                 * at the difference of upper value which was exclusive prior this point.
                 */
                if (upper > lower) {
                    upper--; // Make it inclusive.
                }
                double min, max;
                try {
                    final MathTransform1D tr = band.getSampleToGeophysics();
                    min = tr.transform(lower);
                    max = tr.transform(upper);
                } catch (TransformException cause) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.ILLEGAL_ARGUMENT_$2, "band", band), cause);
                }
                if (min > max) {
                    // This case occurs typically when displaying a color ramp for
                    // sea bathymetry, for which floor level are negative numbers.
                    min = -min;
                    max = -max;
                }
                if (!(min <= max)) {
                    // This case occurs if one or both values is NaN.
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.ILLEGAL_ARGUMENT_$2, "band", band));
                }
                graduation = createGraduation(this.graduation, band, min, max);
            }
        }
        return new AbstractMap.SimpleEntry<Graduation,Color[]>(graduation, colors);
    }

    /**
     * Returns the component's orientation (horizontal or vertical). It should be one of the
     * following constants: {@link SwingConstants#HORIZONTAL} or {@link SwingConstants#VERTICAL}.
     *
     * @return The component orientation.
     */
    public final int getOrientation() {
        return (horizontal) ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL;
    }

    /**
     * Sets the component's orientation (horizontal or vertical).
     *
     * @param orient {@link SwingConstants#HORIZONTAL} or {@link SwingConstants#VERTICAL}.
     * @return The old orientation.
     */
    public final int setOrientation(final int orient) {
        final int old = getOrientation();
        switch (orient) {
            case SwingConstants.HORIZONTAL: horizontal=true;  break;
            case SwingConstants.VERTICAL:   horizontal=false; break;
            default: throw new IllegalArgumentException(String.valueOf(orient));
        }
        return old;
    }

    /**
     * Returns a color for label at the specified index. The default color will be
     * black or white, depending of the background color at the specified index.
     */
    private Color getForeground(final int colorIndex) {
        final int color = colors[colorIndex];
        final int R = ((color >>> 16) & 0xFF);
        final int G = ((color >>>  8) & 0xFF);
        final int B = ( color         & 0xFF);
        HSB = Color.RGBtoHSB(R, G, B, HSB);
        return (HSB[2] >= 0.5f) ? Color.BLACK : Color.WHITE;
    }

    /**
     * Paints the color ramp. This method doesn't need to restore
     * {@link Graphics2D} to its initial state once finished.
     *
     * @param  graphics   The graphic context in which to paint.
     * @param  bounds     The bounding box where to paint the color ramp.
     * @param  font       The font to use for the label, or {@code null} for a default font.
     * @param  foreground The color to use for label, or {@code null} for a default color.
     * @return Bounding   box of graduation labels (NOT taking in account the color ramp
     *                    behind them), or {@code null} if no label has been painted.
     */
    public final Rectangle2D paint(final Graphics2D graphics, final Rectangle bounds, Font font, Color foreground) {
        final int[] colors = this.colors;
        final int length = colors.length;
        final double dx, dy;
        if (length == 0) {
            dx = 0;
            dy = 0;
        } else {
            dx = (double) (bounds.width  - 2*MARGIN) / length;
            dy = (double) (bounds.height - 2*MARGIN) / length;
            int i=0, lastIndex=0;
            int color = colors[0];
            int nextColor = color;
            final int ox = bounds.x + MARGIN;
            final int oy = bounds.y + bounds.height - MARGIN;
            final Rectangle2D.Double rect = new Rectangle2D.Double();
            rect.setRect(bounds);
            while (++i <= length) {
                if (i != length) {
                    nextColor = colors[i];
                    if (nextColor == color) {
                        continue;
                    }
                }
                if (horizontal) {
                    rect.x      = ox + dx*lastIndex;
                    rect.width  = dx * (i-lastIndex);
                    if (lastIndex == 0) {
                        rect.x     -= MARGIN;
                        rect.width += MARGIN;
                    }
                    if (i == length) {
                        rect.width += MARGIN;
                    }
                } else {
                    rect.y      = oy - dy*i;
                    rect.height = dy * (i-lastIndex);
                    if (lastIndex == 0) {
                        rect.height += MARGIN;
                    }
                    if (i == length) {
                        rect.y      -= MARGIN;
                        rect.height += MARGIN;
                    }
                }
                graphics.setColor(new Color(color, true));
                graphics.fill(rect);
                lastIndex = i;
                color = nextColor;
            }
        }
        Rectangle2D labelBounds = null;
        if (labelVisibles && graduation!=null) {
            /*
             * Prepares graduation writing. First, computes the color ramp width in pixels.
             * Then, computes the coefficients for conversion of graduation values to pixel
             * coordinates.
             */
            double x = bounds.getCenterX();
            double y = bounds.getCenterY();
            final double axisRange   = graduation.getSpan();
            final double axisMinimum = graduation.getMinimum();
            final double visualLength, scale, offset;
            if (horizontal) {
                visualLength = bounds.getWidth() - 2*MARGIN - dx;
                scale        = visualLength / axisRange;
                offset       = (bounds.getMinX() + MARGIN + 0.5*dx) - scale*axisMinimum;
            } else {
                visualLength = bounds.getHeight() - 2*MARGIN - dy;
                scale        = -visualLength / axisRange;
                offset       = (bounds.getMaxY() - MARGIN - 0.5*dy) + scale*axisMinimum;
            }
            if (hints == null) {
                hints = new RenderingHints(null);
            }
            final double valueToLocation = length / axisRange;
            if (font == null) {
                font = Font.decode("SansSerif-10");
            }
            final FontRenderContext context = graphics.getFontRenderContext();
            hints.put(Graduation.VISUAL_AXIS_LENGTH, new Float((float)visualLength));
            if (foreground == null) {
                foreground = Color.BLACK;
            }
            graphics.setColor(foreground);
            /*
             * Now write the graduation.
             */
            final TickIterator ticks = graduation.getTickIterator(hints, reuse);
            for (reuse=ticks; !ticks.isDone(); ticks.nextMajor()) {
                if (ticks.isMajorTick()) {
                    final GlyphVector glyph = font.createGlyphVector(context, ticks.currentLabel());
                    final Rectangle2D rectg = glyph.getVisualBounds();
                    final double      width = rectg.getWidth();
                    final double     height = rectg.getHeight();
                    final double      value = ticks.currentPosition();
                    final double   position = value*scale + offset;
                    final int    colorIndex = Math.min(Math.max((int) Math.round(
                                              (value - axisMinimum)*valueToLocation),0), length-1);
                    if (horizontal) x=position;
                    else            y=position;
                    rectg.setRect(x-0.5*width, y-0.5*height, width, height);
                    if (autoForeground) {
                        graphics.setColor(getForeground(colorIndex));
                    }
                    graphics.drawGlyphVector(glyph, (float)rectg.getMinX(), (float)rectg.getMaxY());
                    if (labelBounds != null) {
                        labelBounds.add(rectg);
                    } else {
                        labelBounds = rectg;
                    }
                }
            }
            /*
             * Writes units.
             */
            if (units != null) {
                final GlyphVector glyph = font.createGlyphVector(context, units);
                final Rectangle2D rectg = glyph.getVisualBounds();
                final double      width = rectg.getWidth();
                final double     height = rectg.getHeight();
                if (horizontal) {
                    double left = bounds.getMaxX() - width;
                    if (labelBounds != null) {
                        final double check = labelBounds.getMaxX() + 4;
                        if (check < left) {
                            left = check;
                        }
                    }
                    rectg.setRect(left, y - 0.5*height, width, height);
                } else {
                    rectg.setRect(x - 0.5*width, bounds.getMinY() + height, width, height);
                }
                if (autoForeground) {
                    graphics.setColor(getForeground(length-1));
                }
                if (labelBounds==null || !labelBounds.intersects(rectg)) {
                    graphics.drawGlyphVector(glyph, (float)rectg.getMinX(), (float)rectg.getMaxY());
                }
            }
        }
        return labelBounds;
    }

    /**
     * Returns a graduation for the specified sample dimension, minimum and maximum values. If
     * the supplied {@code reuse} object is non-null and is of the appropriate class, then this
     * method can return {@code reuse} without creating a new graduation object. Otherwise this
     * method must returns a graduation of the appropriate class, usually {@link NumberGraduation}
     * or {@link LogarithmicNumberGraduation}.
     * <p>
     * In every cases, this method must set graduations's
     * {@linkplain AbstractGraduation#setMinimum minimum},
     * {@linkplain AbstractGraduation#setMaximum maximum} and
     * {@linkplain AbstractGraduation#setUnit unit} according the values given in arguments.
     *
     * @param  reuse   The graduation to reuse if possible.
     * @param  band    The sample dimension to create graduation for.
     * @param  minimum The minimal geophysics value to appears in the graduation.
     * @param  maximum The maximal geophysics value to appears in the graduation.
     * @return A graduation for the supplied sample dimension.
     */
    protected Graduation createGraduation(final Graduation reuse, final SampleDimension band,
                                          final double minimum, final double maximum)
    {
        return createDefaultGraduation(reuse, band, minimum, maximum);
    }

    /**
     * Default implementation of {@code createGraduation}.
     *
     * @param  reuse   The graduation to reuse if possible.
     * @param  band    The sample dimension to create graduation for.
     * @param  minimum The minimal geophysics value to appears in the graduation.
     * @param  maximum The maximal geophysics value to appears in the graduation.
     * @return A graduation for the supplied sample dimension.
     */
    public static Graduation createDefaultGraduation(
            final Graduation reuse, final SampleDimension band,
            final double minimum, final double maximum)
    {
        MathTransform1D tr  = band.getSampleToGeophysics();
        boolean linear      = false;
        boolean logarithmic = false;
        try {
            /*
             * An heuristic approach to determine if the transform is linear or logarithmic.
             * We look at the derivative, which should be constant everywhere for a linear
             * scale and be proportional to the inverse of 'x' for a logarithmic one.
             */
            tr = tr.inverse();
            final double ratio = tr.derivative(minimum) / tr.derivative(maximum);
            if (Math.abs(ratio-1) <= EPS) {
                linear = true;
            }
            if (Math.abs(ratio*(minimum/maximum) - 1) <= EPS) {
                logarithmic = true;
            }
        } catch (TransformException exception) {
            // Transformation failed. We don't know if the scale is linear or logarithmic.
            // Continue anyway. A warning will be logged later in this method.
        }
        final Unit<?> units = band.getUnits();
        AbstractGraduation graduation = (reuse instanceof AbstractGraduation) ?
                (AbstractGraduation) reuse : null;
        if (linear) {
            if (graduation == null || !graduation.getClass().equals(NumberGraduation.class)) {
                graduation = new NumberGraduation(units);
            }
        } else if (logarithmic) {
            if (graduation == null || !graduation.getClass().equals(LogarithmicNumberGraduation.class)) {
                graduation = new LogarithmicNumberGraduation(units);
            }
        } else {
            final Logger logger = Logging.getLogger("org.geotoolkit.image");
            final LogRecord record = Loggings.format(Level.WARNING,
                    Loggings.Keys.UNRECOGNIZED_SCALE_TYPE_$1, Classes.getShortClassName(tr));
            record.setLoggerName(logger.getName());
            logger.log(record);
            graduation = new NumberGraduation(units);
        }
        if (graduation == reuse) {
            graduation.setUnit(units);
        }
        graduation.setMinimum(minimum);
        graduation.setMaximum(maximum);
        return graduation;
    }

    /**
     * Returns an image representation for this color ramp.
     *
     * @param  width      The image width.
     * @param  height     The image height.
     * @param  font       The font to use for the label, or {@code null} for a default font.
     * @param  foreground The color to use for label, or {@code null} for a default color.
     * @return The color ramp as a buffered image.
     */
    public final BufferedImage toImage(final int width, final int height, final Font font, final Color foreground) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        paint(graphics, new Rectangle(image.getWidth(), image.getHeight()), font, foreground);
        graphics.dispose();
        return image;
    }

    /**
     * Returns a string representation for this color ramp.
     *
     * @param caller The caller class.
     * @return A string representation of the color ramp.
     */
    public final String toString(final Class<?> caller) {
        final int[] colors = this.colors;
        int count = 0;
        int i = 0;
        if (i < colors.length) {
            int last = colors[i];
            while (++i < colors.length) {
                int c = colors[i];
                if (c != last) {
                    last = c;
                    count++;
                }
            }
        }
        return Classes.getShortName(caller) + '[' + count + " colors]";
    }

    /**
     * Returns a string representation for this color ramp.
     */
    @Override
    public final String toString() {
        return toString(getClass());
    }
}
