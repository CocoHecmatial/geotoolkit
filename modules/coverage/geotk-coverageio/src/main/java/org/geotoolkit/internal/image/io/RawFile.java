/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import com.sun.media.imageio.stream.RawImageInputStream;

import java.awt.Dimension;
import org.geotoolkit.util.Utilities;
import org.geotoolkit.image.io.stream.FileImageInputStream;


/**
 * An entry for a temporary RAW file associated with its color and sample model.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.01
 *
 * @since 3.00
 * @module
 */
public final class RawFile implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 694564932577879529L;

    /**
     * The temporary file.
     */
    public final File file;

    /**
     * The color and sample model of the RAW image.
     */
    private final ImageTypeSpecifier type;

    /**
     * The image width and height.
     */
    private final int width, height;

    /**
     * Creates a new entry for the given temporary file.
     *
     * @param file   The temporary file.
     * @param type   The color and sample model of the RAW image.
     * @param width  The image width, in pixels.
     * @param height The image height, in pixels.
     */
    public RawFile(final File file, final ImageTypeSpecifier type, final int width, final int height) {
        this.file   = file;
        this.type   = type;
        this.width  = width;
        this.height = height;
    }

    /**
     * Returns the input stream to use for reading the RAW image represented by this object.
     *
     * @return The input stream.
     * @throws IOException If an error occured while creating the input stream.
     *
     * @since 3.01
     */
    public ImageInputStream getImageInputStream() throws IOException {
        ImageInputStream in;
        in = new FileImageInputStream(file);
        in = new RawImageInputStream(in, type, new long[1], new Dimension[] {new Dimension(width, height)});
        return in;
    }

    /**
     * Compares this {@code RawFile} with the given object for equality.
     *
     * @param  object The object to compare with {@code this}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof RawFile) {
            final RawFile that = (RawFile) object;
            return Utilities.equals(this.file, that.file) &&
                   Utilities.equals(this.type, that.type) &&
                   this.width == width && this.height == height;
        }
        return false;
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Utilities.hash(type, file.hashCode());
    }

    /**
     * Returns a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return "RawFile[" + file + "\"]";
    }
}
