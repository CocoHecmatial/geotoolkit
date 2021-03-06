/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Geomatys
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

package org.geotoolkit.filter;

import java.util.ArrayList;
import java.util.List;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.filter.visitor.IsStaticExpressionVisitor;
import org.geotoolkit.filter.visitor.PrepareFilterVisitor;
import org.geotoolkit.lang.Static;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * Utility methods for filters.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class FilterUtilities extends Static {

    /**
     * Avoid instanciation.
     */
    private FilterUtilities() {}

    /**
     * Prepare a filter against a given class.
     * @param filter : filter to optimize
     * @param objectClazz : target class against which to optimize
     * @return optimized filter
     */
    public static Filter prepare(final Filter filter, final Class objectClazz,final FeatureType expectedType){
        if(filter == null) return null;
        final PrepareFilterVisitor visitor = new PrepareFilterVisitor(objectClazz,expectedType);
        return (Filter) filter.accept(visitor, null);
    }

    /**
     * Generates a property name which caches the value accessor.
     * the returned PropertyName should not be used against objects of a different
     * class, the result will be unpredictable.
     *
     * @param exp : the property name to prepare
     * @param objectClazz : the target class against which this prepared property
     *      will be used.
     * @return prepared property name expression.
     */
    public static PropertyName prepare(final PropertyName exp, final Class objectClazz, final FeatureType expectedType){
        return new CachedPropertyName(exp.getPropertyName(), objectClazz,expectedType);
    }

    /**
     * Test if an expression is static.
     * Static is the way no expressions use the candidate object for evaluation.
     *
     * @param exp
     * @return true if expression is static
     */
    public static boolean isStatic(final Expression exp){
        ensureNonNull("expression", exp);
        return (Boolean) exp.accept(IsStaticExpressionVisitor.VISITOR, null);
    }

    /**
     * Convert a logic OR in and AND filter.
     *
     * (a OR b) =  NOT (NOT a AND NOT b)
     *
     * @param filter
     * @param ff
     * @return Not filter
     */
    public static Not orToAnd(final Or filter, FilterFactory ff) {
        if(ff==null) ff = FactoryFinder.getFilterFactory(null);

        final List<Filter> children = filter.getChildren();
        final int size = children.size();
        final List<Filter> newChildren = new ArrayList<>(size);
        for(int i=0;i<size;i++) {
            Filter f = children.get(i);
            f = (f instanceof Not) ? ((Not)f).getFilter() : ff.not(f);
            newChildren.add(f);
        }
        return ff.not(ff.and(newChildren));
    }

}
