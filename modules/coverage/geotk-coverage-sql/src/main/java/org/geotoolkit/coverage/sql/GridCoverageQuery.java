/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2005-2010, Open Source Geospatial Foundation (OSGeo)
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

import org.geotoolkit.internal.sql.Ordering;
import org.geotoolkit.internal.sql.table.Column;
import org.geotoolkit.internal.sql.table.Parameter;
import org.geotoolkit.internal.sql.table.Query;
import org.geotoolkit.internal.sql.table.QueryType;
import org.geotoolkit.internal.sql.table.SpatialDatabase;

import static org.geotoolkit.internal.sql.table.QueryType.*;


/**
 * The query to execute for a {@link GridCoverageTable}.
 * Entries <strong>must</strong> be sorted by date (either start or end time).
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.10
 *
 * @since 3.10 (derived from Seagis)
 * @module
 */
final class GridCoverageQuery extends Query {
    /**
     * Column to appear after the {@code "SELECT"} clause.
     */
    final Column series, filename, index, startTime, endTime, spatialExtent;

    /**
     * Parameter to appear after the {@code "FROM"} clause.
     */
    final Parameter byLayer, bySeries, byFilename, byIndex, byStartTime, byEndTime, byHorizontalExtent;

    /**
     * Creates a new query for the specified database.
     *
     * @param database The database for which this query is created.
     */
    public GridCoverageQuery(final SpatialDatabase database) {
        super(database, "GridCoverages");
        final Column layer, horizontalExtent;
        layer            = addForeignerColumn("layer", "Series");
        series           = addMandatoryColumn("series",    SELECT, LIST, INSERT, COUNT);
        filename         = addMandatoryColumn("filename",  SELECT, LIST, INSERT, EXISTS);
        index            = addOptionalColumn ("index", 1,  SELECT, LIST, INSERT);
        startTime        = addMandatoryColumn("startTime", SELECT, LIST, INSERT, AVAILABLE_DATA, BOUNDING_BOX);
        endTime          = addMandatoryColumn("endTime",   SELECT, LIST, INSERT, AVAILABLE_DATA, BOUNDING_BOX);
        spatialExtent    = addMandatoryColumn("extent",    SELECT, LIST, INSERT);
        horizontalExtent = addForeignerColumn("horizontalExtent", "GridGeometries", new QueryType[] {BOUNDING_BOX});
        startTime.setFunction("MIN", BOUNDING_BOX);
        endTime  .setFunction("MAX", BOUNDING_BOX);
        endTime  .setOrdering(Ordering.DESC, SELECT, LIST);

        byLayer            = addParameter(layer,            LIST, AVAILABLE_DATA, BOUNDING_BOX);
        bySeries           = addParameter(series,           SELECT, EXISTS, DELETE, DELETE_ALL, COUNT);
        byFilename         = addParameter(filename,         SELECT, EXISTS, DELETE);
        byIndex            = addParameter(index,            SELECT, EXISTS, DELETE);
        byStartTime        = addParameter(startTime,        LIST, AVAILABLE_DATA, BOUNDING_BOX, DELETE, DELETE_ALL);
        byEndTime          = addParameter(endTime,          LIST, AVAILABLE_DATA, BOUNDING_BOX, DELETE, DELETE_ALL);
        byHorizontalExtent = addParameter(horizontalExtent, LIST, AVAILABLE_DATA, BOUNDING_BOX);
        byHorizontalExtent.setComparator("&&");
        byHorizontalExtent.setSearchValue("GeometryFromText(?," + database.horizontalSRID + ')', LIST, AVAILABLE_DATA, BOUNDING_BOX);
        horizontalExtent  .setFunction("EXTENT", BOUNDING_BOX);
        byStartTime.setComparator("IS NULL OR <=");
        byEndTime  .setComparator("IS NULL OR >=");
    }

    /**
     * Do not include child tables if the table is {@code "GridCoverages"},
     * because we don't want the tiles.
     */
    @Override
    public boolean isIncludingChildTables() {
        return !table.equals("GridCoverages");
    }
}
