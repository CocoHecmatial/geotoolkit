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

import java.awt.Color;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.MathTransformFactory;

import org.geotoolkit.util.NumberRange;
import org.geotoolkit.coverage.Category;
import org.geotoolkit.factory.AuthorityFactoryFinder;
import org.geotoolkit.referencing.operation.matrix.Matrix2;
import org.geotoolkit.image.io.PaletteFactory;
import org.geotoolkit.resources.Errors;

import org.geotoolkit.internal.sql.table.Table;
import org.geotoolkit.internal.sql.table.Database;
import org.geotoolkit.internal.sql.table.QueryType;
import org.geotoolkit.internal.sql.table.LocalCache;
import org.geotoolkit.internal.sql.table.CatalogException;
import org.geotoolkit.internal.sql.table.IllegalRecordException;


/**
 * Connection to a table of {@linkplain Category categories}. This table creates a list of
 * {@link Category} objects for a given sample dimension. Categories are one of the components
 * required for creating a {@link org.geotoolkit.coverage.grid.GridCoverage2D}.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.09
 *
 * @since 3.09 (derived from Seagis)
 * @module
 */
final class CategoryTable extends Table {
    /**
     * Maximum number of bands allowed in an image. This is an arbitrary number used
     * only in order to catch bad records before we create too many objects in memory.
     */
    private static final int MAXIMUM_BANDS = 1000;

    /**
     * A transparent color for missing data.
     */
    private static final Color[] TRANSPARENT = new Color[] {
        new Color(0,0,0,0)
    };

    /**
     * The math transform factory, created only when first needed.
     */
    private transient volatile MathTransformFactory mtFactory;

    /**
     * Creates a category table.
     *
     * @param database Connection to the database.
     */
    public CategoryTable(final Database database) {
        super(new CategoryQuery(database));
    }

    /**
     * Returns the list of categories for the given format.
     *
     * @param  format The name of the format for which the categories are defined.
     * @return The categories for each sample dimension in the given format.
     * @throws CatalogException if an inconsistent record is found in the database.
     * @throws SQLException if an error occured while reading the database.
     */
    public Map<Integer,Category[]> getCategories(final String format) throws CatalogException, SQLException {
        final CategoryQuery query = (CategoryQuery) this.query;
        final List<Category> categories = new ArrayList<Category>();
        final Map<Integer,Category[]> dimensions = new HashMap<Integer,Category[]>();
        MathTransformFactory mtFactory = null;  // Will be fetched only if needed.
        MathTransform      exponential = null;  // Will be fetched only if needed.
        int bandOfPreviousCategory = 0;
        synchronized (getLock()) {
            final LocalCache.Stmt ce = getStatement(QueryType.LIST);
            final PreparedStatement statement = ce.statement;
            statement.setString(indexOf(query.byFormat), format);
            final int bandIndex     = indexOf(query.band    );
            final int nameIndex     = indexOf(query.name    );
            final int lowerIndex    = indexOf(query.lower   );
            final int upperIndex    = indexOf(query.upper   );
            final int c0Index       = indexOf(query.c0      );
            final int c1Index       = indexOf(query.c1      );
            final int functionIndex = indexOf(query.function);
            final int colorsIndex   = indexOf(query.colors  );
            final ResultSet results = statement.executeQuery();
            PaletteFactory palettes = null;
            while (results.next()) {
                boolean isQuantifiable = true;
                final int        band = results.getInt   (bandIndex);
                final String     name = results.getString(nameIndex);
                final int       lower = results.getInt   (lowerIndex);
                final int       upper = results.getInt   (upperIndex);
                final double       c0 = results.getDouble(c0Index); isQuantifiable &= !results.wasNull();
                final double       c1 = results.getDouble(c1Index); isQuantifiable &= !results.wasNull();
                final String function = results.getString(functionIndex);
                final String  colorID = results.getString(colorsIndex);
                /*
                 * Decode the "colors" value. This string is either the RGB numeric code starting
                 * with '#" (as in "#D2C8A0"), or the name of a color palette (as "rainbow").
                 */
                Color[] colors = null;
                if (colorID != null) {
                    final String id = colorID.trim();
                    if (id.length() != 0) try {
                        if (colorID.charAt(0) == '#') {
                            colors = new Color[] {Color.decode(id)};
                         } else {
                            if (palettes == null) {
                                palettes = PaletteFactory.getDefault();
                                palettes.setWarningLocale(getLocale());
                            }
                            colors = palettes.getColors(colorID);
                         }
                    } catch (Exception exception) { // Includes IOException and NumberFormatException
                        throw new IllegalRecordException(exception, this, results, colorsIndex, name);
                    }
                }
                /*
                 * Creates a category for the current record. A category can be 1) qualitive,
                 * 2) quantitative and linear, or 3) quantitative and logarithmic.
                 */
                Category category;
                final NumberRange<?> range = NumberRange.create(lower, upper);
                if (!isQuantifiable) {
                    // Qualitative category.
                    if (colors == null) {
                        colors = TRANSPARENT;
                    }
                    category = new Category(name, colors, range, (MathTransform1D) null);
                } else {
                    // Quantitative category.
                    if (mtFactory == null) {
                        mtFactory = this.mtFactory;
                        if (mtFactory == null) {
                            // Not a big deal if invoked concurrently in 2 threads.
                            this.mtFactory = mtFactory = AuthorityFactoryFinder
                                    .getMathTransformFactory(getDatabase().hints);
                        }
                    }
                    MathTransform tr;
                    try {
                        tr = mtFactory.createAffineTransform(new Matrix2(c1, c0, 0, 1));
                        if (function != null) {
                            if (function.equalsIgnoreCase("log")) {
                                // Quantitative and logarithmic category.
                                if (exponential == null) {
                                    final ParameterValueGroup param = mtFactory.getDefaultParameters("Exponential");
                                    param.parameter("base").setValue(10.0); // Must be a 'double'
                                    exponential = mtFactory.createParameterizedTransform(param);
                                }
                                tr = mtFactory.createConcatenatedTransform(tr, exponential);
                            } else {
                                throw new IllegalRecordException(errors().getString(
                                        Errors.Keys.UNSUPPORTED_OPERATION_$1, function),
                                        this, results, functionIndex, name);
                            }
                        }
                    } catch (FactoryException exception) {
                        results.close();
                        throw new CatalogException(exception);
                    }
                    try {
                        category = new Category(name, colors, range, (MathTransform1D) tr);
                    } catch (ClassCastException exception) { // If 'tr' is not a MathTransform1D.
                        results.close();
                        throw new CatalogException(exception);
                    }
                }
                /*
                 * Add to the new category to the lists. Note that the test below for the
                 * maximum band count is arbitrary and exists only for spotting bad records.
                 */
                final int minBand = Math.max(1, bandOfPreviousCategory);
                if (band < minBand || band > MAXIMUM_BANDS) {
                    throw new IllegalRecordException(errors().getString(Errors.Keys.VALUE_OUT_OF_BOUNDS_$3,
                            band, minBand, MAXIMUM_BANDS), this, results, bandIndex, name);
                }
                // If we are begining a new band, stores the previous
                // categories in the 'dimensions' map.
                if (band != bandOfPreviousCategory) {
                    if (!categories.isEmpty()) {
                        store(dimensions, bandOfPreviousCategory, categories);
                        categories.clear();
                    }
                    bandOfPreviousCategory = band;
                }
                categories.add(category);
            }
            results.close();
            ce.release();
        }
        if (!categories.isEmpty()) {
            store(dimensions, bandOfPreviousCategory, categories);
        }
        return dimensions;
    }

    /**
     * Puts the categories from the given list in the given map.
     */
    private static void store(final Map<Integer,Category[]> dimensions, final int band,
            final List<Category> categories)
    {
        if (dimensions.put(band, categories.toArray(new Category[categories.size()])) != null) {
            throw new AssertionError(band); // Should never happen.
        }
    }
}
