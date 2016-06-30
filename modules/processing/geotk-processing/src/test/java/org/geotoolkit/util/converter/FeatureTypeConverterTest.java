/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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
package org.geotoolkit.util.converter;

import com.vividsolutions.jts.geom.Point;

import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.processing.util.converter.StringToFeatureTypeConverter;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.ObjectConverter;

import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;

import org.junit.Test;
import org.geotoolkit.feature.type.FeatureType;
import static org.junit.Assert.*;

/**
 * Junit test for StringToFeatyreTypeConverter
 * @author Quentin Boileau
 * @module pending
 */
public class FeatureTypeConverterTest extends org.geotoolkit.test.TestBase {


    @Test
    public void FeatureTypeConvertTest() throws NoSuchAuthorityCodeException, FactoryException, UnconvertibleObjectException {

        final ObjectConverter<String,FeatureType> converter = StringToFeatureTypeConverter.getInstance();

        String inputString = "Person{name:String,age:0,position:Point:srid=3395}";
        FeatureType convertedType = converter.apply(inputString);
        FeatureType expectedType = buildResultType();
        assertEquals(expectedType, convertedType);
    }

    private FeatureType buildResultType() throws FactoryException {

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("Person");
        ftb.add("name", String.class);
        ftb.add("age",Integer.class);
        ftb.add("position", Point.class, CRS.forCode("EPSG:3395"));

        ftb.setDefaultGeometry("position");
        final FeatureType sft = ftb.buildFeatureType();
        return sft;
    }
}
