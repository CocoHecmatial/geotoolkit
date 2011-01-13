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
package org.geotoolkit.image.io.mosaic;

import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.InvalidClassException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;

import org.geotoolkit.factory.Hints;
import org.geotoolkit.factory.Factory;
import org.geotoolkit.resources.Errors;
import org.geotoolkit.util.XArrays;
import org.geotoolkit.util.logging.Logging;
import org.geotoolkit.util.NullArgumentException;
import org.geotoolkit.coverage.grid.ImageGeometry;
import org.geotoolkit.referencing.operation.matrix.XAffineTransform;

import static org.geotoolkit.image.io.mosaic.Tile.LOGGER;


/**
 * Creates {@link TileManager} instances from a collection or a directory of tiles.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.15
 *
 * @since 2.5
 * @module
 */
public class TileManagerFactory extends Factory {
    /**
     * The default instance.
     */
    public static final TileManagerFactory DEFAULT = new TileManagerFactory(EMPTY_HINTS);

    /**
     * Creates a new factory from the specified hints.
     *
     * @param hints Optional hints, or {@code null} if none.
     */
    protected TileManagerFactory(final Hints hints) {
        // We have no usage for those hints at this time, but some may be added later.
    }

    /**
     * Creates tile managers from the specified object, which may be {@code null}. If non-null, the
     * object shall be an instance of {@code TileManager[]}, {@code TileManager}, {@code Tile[]},
     * {@code Collection<Tile>} or {@link File}.
     *
     * @param  tiles The tiles, or {@code null}.
     * @return The tile managers, or {@code null} if {@code tiles} was null.
     * @throws IllegalArgumentException if {@code tiles} is not an instance of a valid class,
     *         or if it is an array or a collection containing null elements.
     * @throws IOException If an I/O operation was required and failed.
     */
    public TileManager[] createFromObject(final Object tiles)
            throws IOException, IllegalArgumentException
    {
        final TileManager[] managers;
        if (tiles == null) {
            managers = null;
        } else if (tiles instanceof File) {
            managers = create((File) tiles);
        } else if (tiles instanceof TileManager[]) {
            managers = ((TileManager[]) tiles).clone();
        } else if (tiles instanceof TileManager) {
            managers = new TileManager[] {(TileManager) tiles};
        } else if (tiles instanceof Tile[]) {
            managers = create((Tile[]) tiles);
        } else if (tiles instanceof Collection<?>) {
            @SuppressWarnings("unchecked") // create(Collection) will checks indirectly.
            final Collection<Tile> c = (Collection<Tile>) tiles;
            managers = create(c);
        } else {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.ILLEGAL_CLASS_$2, tiles.getClass(), TileManager.class));
        }
        if (managers != null) {
            for (int i=0; i<managers.length; i++) {
                if (managers[i] == null) {
                    throw new NullArgumentException(Errors.format(
                            Errors.Keys.NULL_ARGUMENT_$1, "input[" + i + ']'));
                }
            }
        }
        return managers;
    }

    /**
     * Creates a tile manager from the given file or directory.
     * <p>
     * <ul>
     *   <li>If the argument {@linkplain File#isFile() is a file} having the {@code ".serialized"}
     *       suffix, then this method deserializes the object in the given file and passes it to
     *       {@link #createFromObject(Object)}.</li>
     *   <li>If the given argument {@linkplain File#isDirectory() is a directory}, then this
     *       method delegates to {@link #create(File, FileFilter, ImageReaderSpi)} which scan
     *       all image files found in the directory.</li>
     *   <li>Otherwise an exception is thrown.</li>
     * </ul>
     *
     * @param  file The serialized file or the directory to scan.
     * @return A tile manager created from the tiles in the given directory.
     * @throws IOException If the given file is not recognized, or an I/O operation failed.
     *
     * @since 3.15
     */
    public TileManager[] create(final File file) throws IOException {
        if (file.isFile()) {
            final String suffix = file.getName();
            if (!suffix.endsWith(".serialized")) {
                throw new IOException(Errors.format(Errors.Keys.UNKNOWN_FILE_SUFFIX_$1, suffix));
            }
            final ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            final Object manager;
            try {
                manager = in.readObject();
            } catch (ClassNotFoundException cause) {
                InvalidClassException ex = new InvalidClassException(cause.getLocalizedMessage());
                ex.initCause(cause);
                throw ex;
            }
            in.close();
            return setSourceFile(createFromObject(manager), file);
        } else if (file.isDirectory()) {
            return create(file, null, null);
        } else {
            throw new IOException(Errors.format(Errors.Keys.NOT_A_DIRECTORY_$1, file));
        }
    }

    /**
     * Creates tile managers from the files found in the given directory. This method scans
     * also the sub-directories recursively if the given file filter accepts directories.
     * <p>
     * Each image file in the given directory shall have a
     * <a href="http://fr.wikipedia.org/wiki/World_file">World File</a> of the same name,
     * typically with the {@code ".tfw"} extension.
     * <p>
     * If the file filter is null, a default file filter will be created from the given SPI.
     * If the given SPI is null, a SPI will be inferred automatically for each image files
     * found in the directory.
     *
     * @param  directory  The directory to scan.
     * @param  filter     An optional file filter, or {@code null} for a default filter.
     * @param  spi        An optional image provider, or {@code null} for auto-detect.
     * @return A tile manager created from the tiles in the given directory.
     * @throws IOException If the given file is not a directory, or an I/O operation failed.
     *
     * @since 3.15
     */
    public TileManager[] create(final File directory, FileFilter filter, final ImageReaderSpi spi)
            throws IOException
    {
        if (filter == null) {
            final String[] suffixes;
            if (spi != null) {
                suffixes = spi.getFileSuffixes();
            } else {
                suffixes = ImageIO.getReaderFileSuffixes();
            }
            if (suffixes != null) {
                filter = new FileFilter() {
                    @Override public boolean accept(final File pathname) {
                        if (pathname.isDirectory()) {
                            return true;
                        }
                        String suffix = pathname.getName();
                        final int s = suffix.lastIndexOf('.');
                        suffix = (s >= 0) ? suffix.substring(s + 1) : "";
                        return XArrays.containsIgnoreCase(suffixes, suffix);
                    }
                };
            }
        }
        final ArrayList<Tile> tiles = new ArrayList<Tile>();
        createTilesFromFiles(directory, filter, spi, 0, tiles);
        return setSourceFile(create(tiles), directory);
    }

    /**
     * Invokes {@link TileManager#setSourceFile(File)} for all managers in the given array.
     * Returns the given array for convenience.
     */
    private static TileManager[] setSourceFile(final TileManager[] managers, final File file) {
        for (final TileManager manager : managers) {
            manager.setSourceFile(file);
        }
        return managers;
    }

    /**
     * Fills the given tiles collection with all tiles found in the given directory and
     * sub-directories which are accepted by the given optional file filter.
     *
     * @param directory  The directory to scan.
     * @param filter     An optional file filter, or {@code null}.
     * @param spi        An optional image provider, or {@code null} for auto-detect.
     * @param imageIndex The image index (usually 0).
     * @param tiles      The collection to fill with new tiles.
     *
     * @since 3.15
     */
    private static void createTilesFromFiles(final File directory, final FileFilter filter,
            final ImageReaderSpi spi, final int imageIndex, final ArrayList<Tile> tiles)
            throws IOException
    {
        final File[] files = directory.listFiles(filter);
        if (files == null) {
            throw new IOException(Errors.format(Errors.Keys.NOT_A_DIRECTORY_$1, directory));
        }
        tiles.ensureCapacity(tiles.size() + files.length);
        for (final File file : files) {
            if (file.isDirectory()) {
                createTilesFromFiles(file, filter, spi, imageIndex, tiles);
            } else {
                tiles.add(new Tile(spi, file, imageIndex));
            }
        }
    }

    /**
     * Creates tile managers from the specified array of tiles.
     * This method usually returns a single tile manager, but more could be
     * returned if this factory has been unable to put every tiles in a single mosaic
     * (for example if the ratio between {@linkplain AffineTransform affine transform} given to
     * {@linkplain Tile#Tile(ImageReaderSpi,Object,int,Rectangle,AffineTransform) tile constructor}
     * would lead to fractional {@linkplain Tile#getSubsampling subsampling}).
     *
     * @param  tiles The tiles to give to a tile manager.
     * @return A tile manager created from the given tiles.
     * @throws IOException If an I/O operation was required and failed.
     */
    public TileManager[] create(final Tile[] tiles) throws IOException {
        // The default called invokes Collection.toArray(), which will copy the array.
        return create(Arrays.asList(tiles));
    }

    /**
     * Creates tile managers from the specified collection of tiles.
     * This method usually returns a single tile manager, but more could be
     * returned if this factory has been unable to put every tiles in a single mosaic
     * (for example if the ratio between {@linkplain AffineTransform affine transform} given to
     * {@linkplain Tile#Tile(ImageReaderSpi,Object,int,Rectangle,AffineTransform) tile constructor}
     * would lead to fractional {@linkplain Tile#getSubsampling subsampling}).
     *
     * @param  tiles The tiles to give to a tile manager.
     * @return A tile manager created from the given tiles.
     * @throws IOException If an I/O operation was required and failed.
     */
    public TileManager[] create(Collection<Tile> tiles) throws IOException {
        int count = 0;
        final TileManager[] managers;
        if (!hasPendingGridToCRS(tiles)) {
            /*
             * There is no tile having a "gridToCRS" transform pending RegionCalculator work. So we
             * can create (at the end of this method) a single TileManager using all those tiles.
             */
            if (!tiles.isEmpty()) {
                count = 1;
            }
            managers = new TileManager[count];
        } else {
            /*
             * At least one tile have a pending "gridToCRS" transform (actually we should have
             * more than one - typically all of them - otherwise the RegionCalculator work will
             * be useless). Computes their region now. Note that we could execute this block
             * inconditionnaly. The 'hasPendingGridToCRS' check we just for avoiding the cost
             * of creating RegionCalculator in the common case where it is not needed. So it is
             * not a big deal if 'hasPendingGridToCRS' conservatively returned 'true'.
             */
            final Collection<Tile> remainings = new ArrayList<Tile>(Math.min(16, tiles.size()));
            final RegionCalculator calculator = new RegionCalculator();
            for (final Tile tile : tiles) {
                if (!calculator.add(tile)) {
                    remainings.add(tile);
                }
            }
            if (!remainings.isEmpty()) {
                count = 1;
            }
            final Map<ImageGeometry,Tile[]> split = calculator.tiles();
            managers = new TileManager[split.size() + count];
            for (final Map.Entry<ImageGeometry,Tile[]> entry : split.entrySet()) {
                final TileManager manager = createGeneric(entry.getValue());
                manager.setGridGeometry(entry.getKey());
                managers[count++] = manager;
            }
            tiles = remainings;
        }
        /*
         * The collection now contains tiles that has not been processed by RegionCalculator,
         * because their 'gridToCRS' transform is flagged as already computed. Create a mosaic
         * for them, and use the affine transform having the finest resolution as the "global"
         * one.
         */
        if (!tiles.isEmpty()) {
            final TileManager manager = createGeneric(tiles.toArray(new Tile[tiles.size()]));
            final Rectangle imageBounds = new Rectangle(-1, -1);
            AffineTransform gridToCRS   = null;
            Dimension       subsampling = null;
            double scale = Double.POSITIVE_INFINITY;
            for (final Tile tile : tiles) {
                imageBounds.add(tile.getAbsoluteRegion());
                final AffineTransform candidate = tile.getGridToCRS();
                if (candidate != null && !candidate.equals(gridToCRS)) {
                    final double cs = XAffineTransform.getScale(candidate);
                    if (cs < scale) {
                        // Found a new tile at a finer resolution.
                        scale       = cs;
                        gridToCRS   = candidate;
                        subsampling = tile.getSubsampling();
                    } else if (cs == scale) {
                        // Inconsistent transform at the finest level.
                        // Abandon the attempt to create a grid geometry.
                        gridToCRS = null;
                        break;
                    }
                }
            }
            if (gridToCRS != null) {
                if (subsampling.width != 1 || subsampling.height != 1) {
                    gridToCRS = new AffineTransform(gridToCRS);
                    gridToCRS.scale(subsampling.width, subsampling.height);
                }
                manager.setGridGeometry(new ImageGeometry(imageBounds, gridToCRS));
            }
            managers[0] = manager;
        }
        return managers;
    }

    /**
     * Returns {@code true} if at least one tile in the given collection has at "<cite>grid to
     * real world</cite>" transform waiting to be processed by {@link RegionCalculator}. It is
     * okay to conservatively returns {@code true} in situations where we would have got
     * {@code false} if synchronization was performed on every tiles.
     */
    private static boolean hasPendingGridToCRS(final Collection<Tile> tiles) {
        for (final Tile tile : tiles) {
            if (tile.getPendingGridToCRS(false) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a single {@linkplain TileManager tile manager} from the given array
     * of tiles. This method is automatically invoked by {@code create} methods.
     * The tile array has already been cloned and can be stored directly by the
     * tile manager constructors.
     * <p>
     * Subclasses can override this method if they want to create other kinds of tile managers.
     *
     * @param  tiles A copy of user-supplied tiles.
     * @return The tile manager for the given tiles.
     * @throws IOException If an I/O operation was required and failed.
     */
    protected TileManager createGeneric(final Tile[] tiles) throws IOException {
        TileManager manager;
        try {
            manager = new GridTileManager(tiles);
        } catch (IllegalArgumentException e) {
            Logging.recoverableException(LOGGER, GridTileManager.class, "<init>", e);
            try {
                manager = new GDALTileManager(tiles);
            } catch (IllegalArgumentException e2) {
                // Failed to created the instance optimized for grid.
                // Fallback on the more generic instance using RTree.
                Logging.recoverableException(LOGGER, GDALTileManager.class, "<init>", e2);
                return new TreeTileManager(tiles);
            }
        }
        // Intentional side effect: use ComparedTileManager only if assertions are enabled.
        assert (manager = new ComparedTileManager(manager, new TreeTileManager(tiles))) != null;
        return manager;
    }
}
