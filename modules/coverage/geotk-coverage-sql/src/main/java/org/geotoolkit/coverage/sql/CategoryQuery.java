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

import static org.geotoolkit.coverage.sql.QueryType.*;


/**
 * The query to execute for a {@link CategoryTable}.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.09
 *
 * @since 3.09 (derived from Seagis)
 * @module
 */
final class CategoryQuery extends Query {
    /**
     * Column to appear after the {@code "SELECT"} clause.
     */
    final Column band, name, lower, upper, c0, c1, function, colors;

    /**
     * Parameter to appear after the {@code "FROM"} clause.
     */
    final Parameter byFormat;

    /**
     * Creates a new query for the specified database.
     *
     * @param database The database for which this query is created.
     */
    public CategoryQuery(final Database database) {
        super(database, "Categories");
        final Column format;
        final QueryType[] none = {    };
        final QueryType[] list = {LIST};
        format   = addMandatoryColumn("format",         none);
        band     = addMandatoryColumn("band",           list);
        name     = addMandatoryColumn("name",           list);
        lower    = addMandatoryColumn("lower",          list);
        upper    = addMandatoryColumn("upper",          list);
        c0       = addMandatoryColumn("c0",             list);
        c1       = addMandatoryColumn("c1",             list);
        function = addOptionalColumn ("function", null, list);
        colors   = addOptionalColumn ("colors",   null, list);
        byFormat = addParameter(format, list);
        band .setOrdering(Ordering.ASC, list);
        lower.setOrdering(Ordering.ASC, list);
    }
}
