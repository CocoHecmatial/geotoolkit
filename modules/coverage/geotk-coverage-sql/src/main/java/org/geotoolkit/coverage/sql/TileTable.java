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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Comparator;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.imageio.IIOException;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

import org.geotoolkit.image.io.mosaic.Tile;
import org.geotoolkit.image.io.mosaic.TileManager;
import org.geotoolkit.image.io.mosaic.TileManagerFactory;
import org.geotoolkit.image.io.plugin.WorldFileImageReader;
import org.geotoolkit.util.XArrays;
import org.geotoolkit.util.collection.Cache;
import org.geotoolkit.util.collection.BackingStoreException;
import org.geotoolkit.internal.sql.table.Table;
import org.geotoolkit.internal.sql.table.Database;
import org.geotoolkit.internal.sql.table.QueryType;
import org.geotoolkit.internal.sql.table.LocalCache;
import org.geotoolkit.internal.sql.table.SpatialDatabase;
import org.geotoolkit.internal.sql.table.CatalogException;
import org.geotoolkit.resources.Errors;


/**
 * Connection to a table of {@linkplain Tiles tiles}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.11
 *
 * @since 3.10 (derived from Seagis)
 * @module
 */
final class TileTable extends Table implements Comparator<TileManager> {
    /**
     * The table of grid geometries. Will be created only when first needed.
     */
    private transient GridGeometryTable gridGeometryTable;

    /**
     * A cache of tile managers created up to date. This cache is shared by all
     * instances of {@link TileTable} created from the same {@link SpatialDatabase}.
     */
    private final Cache<CoverageRequest,TileManager[]> cache;

    /**
     * Creates a tile table.
     *
     * {@section Implementation note}
     * This constructor actually expects an instance of {@link SpatialDatabase},
     * but we have to keep {@link Database} in the method signature because this
     * constructor is fetched by reflection.
     *
     * @param database The connection to the database.
     */
    public TileTable(final Database database) {
        super(new TileQuery((SpatialDatabase) database));
        cache = new Cache<CoverageRequest,TileManager[]>();
    }

    /**
     * Creates a new instance having the same configuration than the given table.
     * This is a copy constructor used for obtaining a new instance to be used
     * concurrently with the original instance.
     *
     * @param table The table to use as a template.
     */
    private TileTable(final TileTable table) {
        super(table);
        cache = table.cache;
    }

    /**
     * Returns a copy of this table. This is a copy constructor used for obtaining
     * a new instance to be used concurrently with the original instance.
     */
    @Override
    protected TileTable clone() {
        return new TileTable(this);
    }

    /**
     * Returns the {@link GridGeometryTable} instance, creating it if needed.
     */
    private GridGeometryTable getGridGeometryTable() throws CatalogException {
        GridGeometryTable table = gridGeometryTable;
        if (table == null) {
            gridGeometryTable = table = getDatabase().getTable(GridGeometryTable.class);
        }
        return table;
    }

    /**
     * Returns {@code true} if at least one tile exists for the given layer.
     *
     * @param  layer The layer to test.
     * @return {@code true} if a tile exists.
     */
    public boolean exists(final LayerEntry layer) throws SQLException {
        final TileQuery query = (TileQuery) this.query;
        final boolean exists;
        synchronized (getLock()) {
            final LocalCache.Stmt ce = getStatement(QueryType.EXISTS);
            final PreparedStatement statement = ce.statement;
            statement.setString(indexOf(query.byLayer), layer.getName());
            final ResultSet results = statement.executeQuery();
            exists = results.next();
            results.close();
            release(ce);
        }
        return exists;
    }

    /**
     * Returns the tile manager for the given layer and date range. This method usually returns a
     * single tile manager, but more could be returned if the tiles can not fit all in the same
     * instance.
     *
     * @param  layer     The layer.
     * @param  startTime The start time, or {@code null} if none.
     * @param  endTime   The end time, or {@code null} if none.
     * @param  srid      The numeric identifier of the CRS.
     * @return The tile managers for the given series and date range.
     * @throws CatalogException if an inconsistent record is found in the database.
     * @throws SQLException if an error occurred while reading the database.
     */
    public TileManager[] getTiles(final LayerEntry layer, final Timestamp startTime,
            final Timestamp endTime, final int srid) throws SQLException, IOException
    {
        final CoverageRequest request = new CoverageRequest(layer, startTime, endTime, srid);
        TileManager[] managers = cache.peek(request);
        if (managers == null) {
            final Cache.Handler<TileManager[]> handler = cache.lock(request);
            try {
                managers = handler.peek();
                if (managers == null) {
                    final TileQuery query   = (TileQuery) this.query;
                    final Calendar calendar = getCalendar();
                    final List<Tile> tiles  = new ArrayList<Tile>();
                    synchronized (getLock()) {
                        final LocalCache.Stmt ce = getStatement(QueryType.LIST);
                        final PreparedStatement statement = ce.statement;
                        statement.setString   (indexOf(query.byLayer), layer.getName());
                        statement.setTimestamp(indexOf(query.byStartTime), startTime, calendar);
                        statement.setTimestamp(indexOf(query.byEndTime),   endTime,   calendar);
                        statement.setInt      (indexOf(query.byHorizontalSRID), srid);
                        final int seriesIndex   = indexOf(query.series);
                        final int filenameIndex = indexOf(query.filename);
                        final int indexIndex    = indexOf(query.index);
                        final int extentIndex   = indexOf(query.spatialExtent);
                        final int dxIndex       = indexOf(query.dx);
                        final int dyIndex       = indexOf(query.dy);
                        final ResultSet results = statement.executeQuery();
                        SeriesEntry       series   = null;
                        ImageReaderSpi    provider = null;
                        GridGeometryEntry geometry = null;
                        int lastSeriesID = 0;
                        int lastExtentID = 0;
                        while (results.next()) {
                            final int    seriesID = results.getInt   (seriesIndex);
                            final String filename = results.getString(filenameIndex);
                            final int    index    = results.getInt   (indexIndex);
                            final int    extent   = results.getInt   (extentIndex);
                            final int    dx       = results.getInt   (dxIndex); // '0' if null, which is fine.
                            final int    dy       = results.getInt   (dyIndex); // '0' if null, which is fine.
                            /*
                             * Gets the series, which usually never change for the whole mosaic (but this is not
                             * mandatory - the real thing that can't change is the layer).  The series is needed
                             * in order to build the absolute pathname from the relative one.
                             */
                            if (series == null || seriesID != lastSeriesID) {
                                // Computes only if the series changed. Usually it doesn't change.
                                series       = layer.getSeries(seriesID);
                                provider     = getImageReaderSpi(series.format.imageFormat);
                                lastSeriesID = seriesID;
                            }
                            Object input = series.file(filename);
                            if (!((File) input).isAbsolute()) try {
                                input = series.uri(filename);
                            } catch (URISyntaxException e) {
                                throw new IIOException(e.getLocalizedMessage(), e);
                            }
                            /*
                             * Gets the geometry, which usually don't change often.  The same geometry can be shared
                             * by all tiles at the same level, given that the only change is the (dx,dy) translation
                             * term defined explicitly in the "Tiles" table. Doing so avoid the creation a thousands
                             * of new "GridGeometries" entries.
                             */
                            if (geometry == null || extent != lastExtentID) {
                                geometry = getGridGeometryTable().getEntry(extent);
                                lastExtentID = extent;
                            }
                            AffineTransform gridToCRS = geometry.gridToCRS;
                            if (dx != 0 || dy != 0) {
                                gridToCRS = new AffineTransform(gridToCRS);
                                gridToCRS.translate(dx, dy);
                            }
                            final Rectangle bounds = geometry.getImageBounds();
                            final Tile tile = new Tile(provider, input, (index != 0) ? index-1 : 0, bounds, gridToCRS);
                            tiles.add(tile);
                        }
                        results.close();
                        release(ce);
                    }
                    /*
                     * Get the array of TileManager. The array should contains only one element.
                     * But if we get more element, put the TileManager having the greatest amount
                     * of tiles first because it is typically the one which will be used by the
                     * GridCoverageLoader.
                     */
                    if (!tiles.isEmpty()) try {
                        managers = TileManagerFactory.DEFAULT.create(tiles);
                        Arrays.sort(managers, this);
                    } catch (BackingStoreException e) {
                        throw e.unwrapOrRethrow(IOException.class);
                    }
                }
            } finally {
                handler.putAndUnlock(managers);
            }
        }
        return managers;
    }

    /**
     * The comparator used by the above {@link #getTiles} method for putting first the
     * {@code TileManager} which have the greatest amount of tiles.
     *
     * @param  o1 The first tile manager to compare.
     * @param  o2 The second tile manager to compare.
     * @return A negative number if o1 has more times than o2.
     * @throws BackingStoreException If an {@link IOException} occurred.
     */
    @Override
    public int compare(final TileManager o1, final TileManager o2) throws BackingStoreException {
        try {
            return o2.getTiles().size() - o1.getTiles().size();
        } catch (IOException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Returns an image reader for the specified name. The argument can be either a format
     * name or a mime type.
     */
    private static ImageReaderSpi getImageReaderSpi(final String format) throws IIOException {
        final IIORegistry registry = IIORegistry.getDefaultInstance();
        Iterator<ImageReaderSpi> providers = registry.getServiceProviders(ImageReaderSpi.class, true);
        while (providers.hasNext()) {
            final ImageReaderSpi provider = providers.next();
            if (!(provider instanceof WorldFileImageReader.Spi)) {
                if (XArrays.containsIgnoreCase(provider.getFormatNames(), format)) {
                    return provider;
                }
            }
        }
        /*
         * Tests for MIME type only if no provider was found for the format name. We do not merge
         * the check for MIME type in the above loop because it has a cost (getMIMETypes() clones
         * an array) and should not be needed for database registering their format by name. This
         * check is performed mostly for compatibility purpose with policy in previous versions.
         */
        providers = registry.getServiceProviders(ImageReaderSpi.class, true);
        while (providers.hasNext()) {
            final ImageReaderSpi provider = providers.next();
            if (!(provider instanceof WorldFileImageReader.Spi)) {
                if (XArrays.containsIgnoreCase(provider.getMIMETypes(), format)) {
                    return provider;
                }
            }
        }
        throw new IIOException(Errors.format(Errors.Keys.NO_IMAGE_READER));
    }
}
