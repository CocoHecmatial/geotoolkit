/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.feature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import org.apache.sis.referencing.CommonCRS;

import org.geotoolkit.feature.type.AttributeDescriptor;
import org.geotoolkit.feature.type.NamesExt;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.GeometryDescriptor;
import org.opengis.feature.MismatchedFeatureException;


/**
 * This is a support class which creates test features for use in testing.
 *
 * @author jamesm
 * @module pending
 */
public class SampleFeatureFixtures {
    /**
     * Feature on which to preform tests
     */

    // private Feature testFeature = null;

    /**
     * Creates a new instance of SampleFeatureFixtures
     */
    public SampleFeatureFixtures() {
    }

    public static Feature createFeature() {
        try {
            FeatureType testType = createTestType();
            Object[] attributes = createAttributes();

            return FeatureBuilder.build( testType,attributes,null);
        } catch (Exception e) {
            Error ae = new AssertionError(
                    "Sample Feature for tests has been misscoded");
            ae.initCause(e);
            throw ae;
        }
    }

    public static Feature createAddressFeature() {
        try {
            return createFeature();

            //FeatureType addressType = createAddressType();
            //Object[] attributes = createComplexAttributes();
            //return addressType.create(attributes);
        } catch (Exception e) {
            Error ae = new AssertionError(
                    "Sample Feature for tests has been misscoded");
            ae.initCause(e);
            throw ae;
        }
    }

    /**
     * creates and returns an array of sample attributes.
     *
     */
    public static Object[] createAttributes() {
        Object[] attributes = new Object[10];
        GeometryFactory gf = new GeometryFactory();
        attributes[0] = gf.createPoint(new Coordinate(1, 2));
        attributes[1] = new Boolean(true);
        attributes[2] = new Character('t');
        attributes[3] = new Byte("10");
        attributes[4] = new Short("101");
        attributes[5] = new Integer(1002);
        attributes[6] = new Long(10003);
        attributes[7] = new Float(10000.4);
        attributes[8] = new Double(100000.5);
        attributes[9] = "test string data";

        return attributes;
    }

    //If we go to factories/protected constructors this won't work, will need
    //to move to a types directory, or use the factory
    public static AttributeDescriptor getChoiceAttrType1() {
        return createChoiceAttrType("choiceTest1", createType1Choices());
    }

    public static AttributeDescriptor[] createType1Choices() {
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        AttributeDescriptorBuilder adb = new AttributeDescriptorBuilder();
        AttributeDescriptor[] choices = new AttributeDescriptor[3];

        ab.reset();
        ab.setBinding(Byte.class);
        adb.reset();
        adb.setName(NamesExt.create("testByte"));
        adb.setType(ab.buildType());

        choices[0] = adb.buildDescriptor();

        ab.reset();
        ab.setBinding(Double.class);
        adb.reset();
        adb.setName(NamesExt.create("testDouble"));
        adb.setType(ab.buildType());
        choices[1] = adb.buildDescriptor();

        ab.reset();
        ab.setBinding(String.class);
        adb.reset();
        adb.setName(NamesExt.create("testString"));
        adb.setType(ab.buildType());
        choices[2] = adb.buildDescriptor();

        return choices;
    }

    public static AttributeDescriptor getChoiceAttrType2() {
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        AttributeDescriptorBuilder adb = new AttributeDescriptorBuilder();
        AttributeDescriptor[] choices = new AttributeDescriptor[2];

        ab.reset();
        ab.setBinding(String.class);
        adb.reset();
        adb.setName(NamesExt.create("testString"));
        adb.setType(ab.buildType());
        choices[0] = adb.buildDescriptor();

        ab.reset();
        ab.setBinding(Integer.class);
        adb.reset();
        adb.setName(NamesExt.create("testInt"));
        adb.setType(ab.buildType());
        choices[1] = adb.buildDescriptor();

        return createChoiceAttrType("choiceTest2", choices);
    }

    public static AttributeDescriptor createChoiceAttrType(final String name,
        final AttributeDescriptor[] choices) {

        throw new RuntimeException("Figure out how to handle choice");
        //return new ChoiceAttributeType(name, choices);
    }

    public static AttributeDescriptor createGeomChoiceAttrType(final String name,
        final GeometryDescriptor[] choices) {
        throw new RuntimeException("Figure out how to handle choice");
        //return new ChoiceAttributeType.Geometric(name, choices);
    }

    public static AttributeDescriptor getChoiceGeomType() {
        throw new RuntimeException("Figure out how to handle choice");

//        GeometryAttributeType[] choices = new GeometryAttributeType[2];
//        choices[0] = (GeometryAttributeType) AttributeTypeFactory
//            .newAttributeType("testLine", LineString.class);
//        choices[1] = (GeometryAttributeType) AttributeTypeFactory
//            .newAttributeType("testMultiLine", MultiLineString.class);
//
//        return createGeomChoiceAttrType("choiceGeom", choices);
    }

    public static FeatureType createChoiceFeatureType() {
        throw new RuntimeException("Figure out how to handle choice");

//        DefaultFeatureTypeBuilder tb = new DefaultFeatureTypeBuilder();
//        tb.setName( "test" );
//
//        tb.add(getChoiceAttrType1());
//        tb.add(getChoiceAttrType2());
//        tb.add(getChoiceGeomType());
//        tb.setDefaultGeometry(getChoiceGeomType());
//
//        return tb.buildSimpleFeatureType();
    }

    /**
     * DOCUMENT ME!
     *
     *
     * @throws SchemaException
     */
    public static FeatureType createTestType() throws MismatchedFeatureException {
        FeatureTypeBuilder tb = new FeatureTypeBuilder();
        tb.setName(NamesExt.create("test"));

        tb.add(NamesExt.create("testGeometry"), Point.class, CommonCRS.WGS84.normalizedGeographic());
        tb.add(NamesExt.create("testBoolean"), Boolean.class);
        tb.add(NamesExt.create("testCharacter"), Character.class);
        tb.add(NamesExt.create("testByte"), Byte.class);
        tb.add(NamesExt.create("testShort"), Short.class);
        tb.add(NamesExt.create("testInteger"), Integer.class);
        tb.add(NamesExt.create("testLong"), Long.class);
        tb.add(NamesExt.create("testFloat"), Float.class);
        tb.add(NamesExt.create("testDouble"), Double.class);
        tb.add(NamesExt.create("testString"), String.class);

        tb.setDefaultGeometry("testGeometry");
        return tb.buildSimpleFeatureType();

    }
}
