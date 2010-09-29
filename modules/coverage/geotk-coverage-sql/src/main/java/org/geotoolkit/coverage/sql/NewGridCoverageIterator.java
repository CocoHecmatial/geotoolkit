/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2007-2010, Geomatys
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
package org.geotoolkit.coverage.sql;

import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.io.IOException;
import java.sql.SQLException;
import javax.imageio.ImageReader;
import javax.imageio.IIOException;

import org.opengis.util.FactoryException;

import org.geotoolkit.image.io.XImageIO;
import org.geotoolkit.image.io.mosaic.Tile;
import org.geotoolkit.image.io.NamedImageStore;
import org.geotoolkit.internal.sql.table.SpatialDatabase;
import org.geotoolkit.resources.Vocabulary;
import org.geotoolkit.resources.Errors;


/**
 * An iterator creating {@link NewGridCoverageReference} on-the-fly using different input source.
 * For the sake of simplicity, this method does not have {@code hasNext()} method. Instead, the
 * {@code #next()} method returns {@code null} when there is no more element to return.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.15
 *
 * @since 3.12 (derived from Seagis)
 * @module
 */
final class NewGridCoverageIterator {
    /**
     * The object which contains the listeners. While we are keeping a reference to the
     * full {@link CoverageDatabase} objects, only the listeners are of interest to this
     * iterator.
     */
    private final CoverageDatabase listeners;

    /**
     * An optional controller to invoke before the listeners, or {@code null} if none.
     */
    private final CoverageDatabaseController controller;

    /**
     * The database where new entries will be added.
     */
    private final SpatialDatabase database;

    /**
     * An iterator over the inputs to read.
     * If input are {@link File} or {@link URI}, they shall be relative to current directory.
     * Inputs may also be {@link Tile} or {@link ImageReader} instances.
     */
    private final Iterator<?> inputToAdd;

    /**
     * Creates an iterator for the specified files.
     *
     * @param  listeners  The object which hold the {@link CoverageDatabaseListener}s. While this
     *                    argument is of kind {@link CoverageDatabase}, only the listeners are of
     *                    interest to this class.
     * @param  controller An optional controller to invoke before the listeners, or {@code null}.
     * @param  database   The database where new entries will be added. This is mandatory.
     * @param  inputToAdd The files to read.
     * @throws IOException if an I/O operation was required and failed.
     */
    NewGridCoverageIterator(final CoverageDatabase           listeners,
                            final CoverageDatabaseController controller,
                            final SpatialDatabase            database,
                            final Collection<?>              inputToAdd)
                            throws SQLException, IOException, FactoryException, DatabaseVetoException
    {
        this.listeners  = listeners;
        this.controller = controller;
        this.database   = database;
        this.inputToAdd = inputToAdd.iterator();
    }

    /**
     * Creates an entry for the given input. If this method detects that there is many images
     * in the file, then {@link CoverageDatabaseController#filterImages} is invoked. Finally,
     * the {@link NewGridCoverageReference} constructor will fetch the image metadata.
     *
     * @param  input The input.
     * @return The entry.
     * @throws IOException if an I/O operation was required and failed.
     */
    private NewGridCoverageReference createEntry(Object input)
            throws SQLException, IOException, FactoryException, DatabaseVetoException
    {
        if (input instanceof NewGridCoverageReference) {
            return (NewGridCoverageReference) input;
        }
        if (input instanceof Tile) {
            return new NewGridCoverageReference(database, (Tile) input);
        }
        final ImageReader reader;
        if (input instanceof ImageReader) {
            reader = (ImageReader) input;
            input = reader.getInput();
        } else {
            reader = XImageIO.getReaderBySuffix(input, true, false);
        }
        /*
         * If there is many images, get the list of them. If the file is some format having
         * named images (e.g. NetCDF files where images are actually NetCDF variables), then
         * this is the list of those variables. Otherwise we generate a list with "Image 1",
         * "Image 2", etc. items. After the list has been created, ask the controller to
         * choose some images in that list. The selection may happen in a Swing GUI.
         */
        int imageIndex = 0;
        if (controller != null) {
            final int numImages = reader.getNumImages(true);
            if (numImages > 1) {
                final boolean multiSelectionAllowed = (reader instanceof NamedImageStore);
                final List<String> variables;
                if (multiSelectionAllowed) {
                    variables = ((NamedImageStore) reader).getImageNames();
                } else {
                    final String[] names = new String[numImages];
                    final Vocabulary resources = Vocabulary.getResources(listeners.getLocale());
                    for (int i=0; i<names.length; i++) {
                        names[i] = resources.getString(Vocabulary.Keys.IMAGE_$1, i+1);
                    }
                    variables = Arrays.asList(names);
                }
                final Collection<String> selected = controller.filterImages(variables, multiSelectionAllowed);
                if (selected != null) {
                    /*
                     * At this point, the controller selected some images in the proposed list.
                     * Get the image index of each selected item. If the image reader supports
                     * named bands, each selected item will be a band of the only image to be
                     * read. For example in a NetCDF file, the "U" and "V" variable may be two
                     * bands of the same image. For any other kind of image reader, the selection
                     * can contain only one image.
                     */
                    int numSelected = 0;
                    final int[] index = new int[selected.size()];
                    for (final String variable : selected) {
                        if ((index[numSelected++] = variables.indexOf(variable)) < 0) {
                            throw new IIOException(error(Errors.Keys.NO_SUCH_ELEMENT_$1, variable));
                        }
                    }
                    if (numSelected != 0) {
                        if (multiSelectionAllowed) {
                            final String[] names = new String[numSelected];
                            for (int i=0; i<names.length; i++) {
                                names[i] = variables.get(index[i]);
                            }
                            final NamedImageStore store = (NamedImageStore) reader;
                            store.setImageNames(names[0]);
                            store.setBandNames(0, names);
                        } else if (numSelected != 1) {
                            throw new IIOException(error(Errors.Keys.UNEXPECTED_PARAMETER_$1, "images[2]"));
                        } else {
                            imageIndex = index[0];
                        }
                    }
                }
            }
        }
        return new NewGridCoverageReference(database, reader, input, imageIndex);
    }

    /**
     * Returns the next elements (skipping {@code null} values) from the {@link #inputToAdd}
     * iterator, or {@code null} if we have reached the iteration end.
     *
     * @return The next input, or {@code null} if we have reached iteration end.
     */
    public NewGridCoverageReference next()
            throws SQLException, IOException, FactoryException, DatabaseVetoException
    {
        while (inputToAdd.hasNext()) {
            final Object input = inputToAdd.next();
            if (input != null) {
                return createEntry(input);
            }
        }
        return null;
    }

    /**
     * Invoked by {@link WritableGridCoverageTable} after a {@link NewGridCoverageReference}
     * element has been fully constructed.
     *
     * @param  isBefore {@code true} if the event is invoked before the change,
     *                  or {@code false} if the event occurs after the change.
     * @param  value    The entry which is added.
     * @throws DatabaseVetoException if {@code isBefore} is {@code true} and a listener vetoed
     *         against the change.
     */
    final void fireCoverageAdding(final boolean isBefore, final NewGridCoverageReference value)
            throws DatabaseVetoException
    {
        if (isBefore && controller != null) {
            controller.coverageAdding(new CoverageDatabaseEvent(listeners, isBefore, 1), value);
        }
        if (listeners != null) {
            listeners.fireChange(isBefore, 1, value);
        }
    }

    /**
     * Formats an error message for the given key.
     */
    private String error(final int key, final Object argument) {
        return Errors.getResources(listeners.getLocale()).getString(key, argument);
    }
}
