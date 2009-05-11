/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Open Source Geospatial Foundation (OSGeo)
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

package org.geotoolkit.filter.binaryexpression;

import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;

/**
 * Immutable "divide" expression.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultDivide extends AbstractBinaryExpression<Expression,Expression> implements Divide{

    public DefaultDivide(Expression left, Expression right) {
        super(left,right);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object evaluate(Object object) {
        final Double val1 = left.evaluate(object, Double.class);
        final Double val2 = right.evaluate(object, Double.class);

        if(val1 == null || val2 == null){
            return null;
        }

        return val1 / val2;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Object accept(ExpressionVisitor visitor, Object extraData) {
        return visitor.visit(this, extraData);
    }

}
