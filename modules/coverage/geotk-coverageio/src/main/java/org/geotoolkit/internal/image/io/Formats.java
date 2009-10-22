/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.internal.image.io;

import java.util.Locale;
import java.util.Iterator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import org.geotoolkit.lang.Static;
import org.geotoolkit.util.XArrays;
import org.geotoolkit.resources.Errors;


/**
 * Utility methods about image formats.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.05
 *
 * @since 3.01
 * @module
 */
@Static
public final class Formats {
    /**
     * Do not allow instantiation of this class.
     */
    private Formats() {
    }

    /**
     * A callback for performing an arbitrary operation using an {@link ImageReader}
     * selected from a given input. An implementation of this interface is given to
     * {@link Formats#selectImageReader Formats.selectImageReader(...)}.
     *
     * @author Martin Desruisseaux (Geomatys)
     * @version 3.05
     *
     * @since 3.05
     * @module
     */
    public static interface ReadCall {
        /**
         * Invoked when a suitable image reader has been found. If the operation fails with
         * an {@link IOException}, then {@link Formats#selectImageReader selectImageReader}
         * will searchs for an other image reader. If none are found, then the first exception
         * will be rethrown.
         * <p>
         * This method should not retain a reference to the image reader, because it will be
         * disposed by the caller.
         *
         * @param  reader The image reader.
         * @throws IOException If an I/O error occured.
         */
        void read(ImageReader reader) throws IOException;

        /**
         * Invoked when a recoverable error occured. Implementors will typically delegate to
         * {@link org.geotoolkit.util.logging.Logging#recoverableException(Class, String, Throwable)}
         * whith appropriate class an method name.
         *
         * @param error The error which occured.
         */
        void recoverableException(Throwable error);
    }

    /**
     * Searchs {@link ImageReader}s that claim to be able to decode the given input, and call
     * {@link ReadCall#filter(ImageReader)} for each of them until a call succeed. If every
     * readers fail with an {@link IOException}, the exception of the first reader is rethrown
     * by this method.
     *
     * @param  input     The input for which image reader are searched.
     * @param  locale    The locale to set to the image readers, or {@code null} if none.
     * @param  callback  The method to call when an {@link ImageReader} seems suitable.
     * @throws IOException If no suitable image reader has been found.
     *
     * @since 3.05
     */
    public static void selectImageReader(final Object input, final Locale locale, final ReadCall callback)
            throws IOException
    {
        boolean     success = false;
        IOException failure = null;
        Object      stream  = input;
attmpt: while (stream != null) { // This loop will be executed at most twice.
            final Iterator<ImageReader> it = ImageIO.getImageReaders(stream);
            while (it.hasNext()) {
                if (stream instanceof ImageInputStream) {
                    ((ImageInputStream) stream).mark();
                }
                final ImageReader reader = it.next();
                reader.setInput(stream, true, false);
                if (locale != null) try {
                    reader.setLocale(locale);
                } catch (IllegalArgumentException e) {
                    // Unsupported locale. Not a big deal, so ignore...
                }
                try {
                    callback.read(reader);
                    success = true;
                    break attmpt;
                } catch (IOException e) {
                    if (failure == null) {
                        failure = e; // Remember only the first failure.
                    }
                } finally {
                    reader.dispose();
                    // Don't bother to reset the stream if we succeeded
                    // and the stream was created by ourself.
                    if (!success || stream == input) {
                        if (stream instanceof ImageInputStream) try {
                            ((ImageInputStream) stream).reset();
                        } catch (IOException e) {
                            if (stream == input) {
                                throw e;
                            }
                            // Failed to reset the stream, but we created
                            // it ourself. So let just create an other one.
                            callback.recoverableException(e);
                            ((ImageInputStream) stream).close();
                            stream = ImageIO.createImageInputStream(input);
                        }
                    }
                }
            }
            /*
             * If we have tried every image readers for the given input, wraps the
             * input in an ImageInputStream (if not already done) and try again.
             */
            if (stream instanceof ImageInputStream) {
                break;
            }
            stream = ImageIO.createImageInputStream(input);
        }
        /*
         * We got a success, or we tried every image readers.
         * Closes the stream only if we created it ourself.
         */
        if (stream != input && stream instanceof ImageInputStream) {
            ((ImageInputStream) stream).close();
        }
        if (!success) {
            if (failure == null) {
                if (input instanceof File && !((File) input).exists()) {
                    failure = new FileNotFoundException(Errors.format(Errors.Keys.FILE_DOES_NOT_EXIST_$1, input));
                } else {
                    failure = new IIOException(Errors.format(Errors.Keys.NO_IMAGE_READER));
                }
            }
            throw failure;
        }
    }

    /**
     * Returns the image reader provider for the given format name.
     *
     * @param  format The format name to search, or {@code null}.
     * @return The reader provider for the given format, or {@code null} if {@code format} is null.
     * @throws IllegalArgumentException If no provider is found for the given format.
     */
    public static ImageReaderSpi getReaderByFormatName(String format) throws IllegalArgumentException {
        ImageReaderSpi spi = null;
        if (format != null) {
            format = format.trim();
            final IIORegistry registry = IIORegistry.getDefaultInstance();
            final Iterator<ImageReaderSpi> it=registry.getServiceProviders(ImageReaderSpi.class, true);
            do {
                if (!it.hasNext()) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.UNKNOW_IMAGE_FORMAT_$1, format));
                }
                spi = it.next();
            } while (!XArrays.contains(spi.getFormatNames(), format));
        }
        return spi;
    }
}
