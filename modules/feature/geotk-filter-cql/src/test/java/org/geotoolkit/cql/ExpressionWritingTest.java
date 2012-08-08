/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Geomatys
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
package org.geotoolkit.cql;

import org.geotoolkit.filter.DefaultFilterFactory2;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 * Test writing in CQL expressions.
 * 
 * @author Johann Sorel (Geomatys)
 */
public class ExpressionWritingTest {
    
    private final FilterFactory2 FF = new DefaultFilterFactory2();
    
    @Test
    public void testPropertyName1() throws CQLException{
        final Expression exp = FF.property("geom");
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("geom", cql);              
    }
    
    @Test
    public void testPropertyName2() throws CQLException{        
        final Expression exp = FF.property("the geom");
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("\"the geom\"", cql);                   
    }
    
    @Test
    public void testInteger() throws CQLException{
        final Expression exp = FF.literal(15);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("15", cql);                     
    }
    
    @Test
    public void testDecimal1() throws CQLException{
        final Expression exp = FF.literal(3.14);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("3.14", cql);                                
    }
    
    @Test
    public void testDecimal2() throws CQLException{
        final Expression exp = FF.literal(9.0E-21);
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("9.0E-21", cql);    
    }
    
    @Test
    public void testText() throws CQLException{
        final Expression exp = FF.literal("hello world");
        final String cql = CQL.write(exp);
        assertNotNull(cql);
        assertEquals("'hello world'", cql);             
    }
    
}
