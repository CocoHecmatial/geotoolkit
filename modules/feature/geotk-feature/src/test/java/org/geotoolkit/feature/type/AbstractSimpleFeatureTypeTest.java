/*
 *    Types - Java interfaces for OGC/ISO standards
 *    http://www.geoapi.org
 *
 *    Copyright (C) 2005-2012 Open Geospatial Consortium, Inc.
 *    All Rights Reserved. http://www.opengeospatial.org/ogc/legal
 *
 *    Permission to use, copy, and modify this software and its documentation, with
 *    or without modification, for any purpose and without fee or royalty is hereby
 *    granted, provided that you include the following on ALL copies of the software
 *    and documentation or portions thereof, including modifications, that you make:
 *
 *    1. The full text of this NOTICE in a location viewable to users of the
 *       redistributed or derivative work.
 *    2. Notice of any changes or modifications to the OGC files, including the
 *       date changes were made.
 *
 *    THIS SOFTWARE AND DOCUMENTATION IS PROVIDED "AS IS," AND COPYRIGHT HOLDERS MAKE
 *    NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 *    TO, WARRANTIES OF MERCHANTABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR THAT
 *    THE USE OF THE SOFTWARE OR DOCUMENTATION WILL NOT INFRINGE ANY THIRD PARTY
 *    PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER RIGHTS.
 *
 *    COPYRIGHT HOLDERS WILL NOT BE LIABLE FOR ANY DIRECT, INDIRECT, SPECIAL OR
 *    CONSEQUENTIAL DAMAGES ARISING OUT OF ANY USE OF THE SOFTWARE OR DOCUMENTATION.
 *
 *    The name and trademarks of copyright holders may NOT be used in advertising or
 *    publicity pertaining to the software without specific, written prior permission.
 *    Title to copyright in this software and any associated documentation will at all
 *    times remain with copyright holders.
 */

package org.geotoolkit.feature.type;

import org.geotoolkit.util.NamesExt;
import org.opengis.util.GenericName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotoolkit.feature.MockCRS;
import org.geotoolkit.feature.MockDirectPosition2D;
import org.geotoolkit.feature.MockInternationalString;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A class to test {@link FeatureTypeFactory} methods (only for simple
 * features).
 *
 * Test creation of attributes and association types as descriptors. Also test
 * geometry type & simple feature type creation
 *
 * @author Alexis MANIN (geomatys)
 *
 */
public abstract class AbstractSimpleFeatureTypeTest  {

    MockInternationalString descriptionSample;

    public AbstractSimpleFeatureTypeTest() {
        descriptionSample = new MockInternationalString("mock");
    }

    /**
     * A function which return the current used feature type factory
     *
     * @return the feature type factory to use for tests
     */
    public abstract FeatureTypeFactory getFeatureTypeFactory();

    /**
     * Test the creation of a simple {@link AttributeType}. Check for its name,
     * identifier, binding class, parent and abstract marker.
     */
    @Test
    public void testCreateAttributeType() {
        final GenericName name = NamesExt.create("objectName");
        final GenericName name2 = NamesExt.create("objectName2");
        final FeatureTypeFactory FTF = getFeatureTypeFactory();

        AttributeType res = FTF.createAttributeType(name, String.class, true, false, null, null, null);
        //tests
        assertNotNull("AttributeType creation failed", res);
        //name
        assertNotNull("AttributeType name not set", res.getName());
        assertEquals("AttributeType name does not match", res.getName().toString(), "objectName");
        assertTrue("AttributeType is not identified (but should be)", res.isIdentified());
        //abstraction
        assertFalse("AttributeType set abstract while it's not true", res.isAbstract());
        //binding
        assertEquals("AttributeType does not bind the right class", String.class, res.getBinding());

        final AttributeType res2 = FTF.createAttributeType(name2, Integer.class, false, true, null, res, descriptionSample);
        //tests
        assertNotNull("AttributeType creation failed", res2);
        //parent
        assertNotNull("Parent has not been set", res2.getSuper());
        assertEquals("Parent type is wrong", res, res2.getSuper());
        //Name
        assertNotNull("AttributeType name not set", res2.getName());
        assertEquals("AttributeType name does not match", "objectName2", res2.getName().toString());
        //other
        assertFalse("AttributeType is identified (and should not)", res2.isIdentified());
        assertTrue("AttributeType is not set abstract while it's true", res2.isAbstract());
        assertEquals("AttributeType does not bind the right class", Integer.class, res2.getBinding());
        //description
        assertNotNull("Attribute description is NULL", res2.getDescription());
        assertEquals("Attribute description is wrong", descriptionSample.toString(), res2.getDescription().toString());
    }

    /**
     * Test the creation of {@link GeometryType}. Check for its name,
     * identifier, binding class, parent and abstract marker.
     */
    @Test
    public void testCreateGeometryType() {
        final GenericName name = NamesExt.create("objectName");
        final GenericName name2 = NamesExt.create("objectName2");
        final Map crsInfo = new HashMap<String, String>();
        crsInfo.put("name", "myCRS");
        crsInfo.put("first", "one");
        crsInfo.put("second", "two");
        final CoordinateReferenceSystem curCRS = new MockCRS(crsInfo);
        final FeatureTypeFactory FTF = getFeatureTypeFactory();

        final GeometryType res = FTF.createGeometryType(name, MockDirectPosition2D.class, curCRS, true, false, null, null, null);
        assertNotNull("GeometryType creation failed", res);
        //Name
        assertNotNull("GeometryType name not set", res.getName());
        assertEquals("GeometryType name does not match", res.getName().toString(), "objectName");
        //id
        assertTrue("GeometryType is not identified (but should be)", res.isIdentified());
        //abstract
        assertFalse("GeometryType set abstract while it's not true", res.isAbstract());
        //class binding
        assertEquals("GeometryType does not bind the right class", MockDirectPosition2D.class, res.getBinding());
        //CRS
        assertNotNull("CRS has not been set", res.getCoordinateReferenceSystem());
        assertEquals("CRS is wrong", curCRS, res.getCoordinateReferenceSystem());

        //second object to test
        final AttributeType res2 = FTF.createGeometryType(name2, DirectPosition.class, null, false, true, null, res, descriptionSample);
        assertNotNull("AttributeType creation failed", res2);
        //parent
        assertNotNull("Parent has not been set", res2.getSuper());
        assertEquals("Parent type is wrong", res, res2.getSuper());
        //Name
        assertNotNull("AttributeType name not set", res2.getName());
        assertEquals("AttributeType name does not match", "objectName2", res2.getName().toString());
        //other
        assertFalse("AttributeType is identified (and should not)", res2.isIdentified());
        assertTrue("AttributeType is not set abstract while it's true", res2.isAbstract());
        assertEquals("AttributeType does not bind the right class", DirectPosition.class, res2.getBinding());
        //description
        assertNotNull("Attribute description is NULL", res2.getDescription());
        assertEquals("Attribute description is wrong", descriptionSample.toString(), res2.getDescription().toString());
    }


    /**
     * Test for {@link AssociationType} creation. Check for name, abstraction,
     * relatedType and parent.
     */
    @Test
    public void testCreateAssociationType() {
        final GenericName name = NamesExt.create("lines");
        final GenericName name2 = NamesExt.create("lines2");
        final GenericName asName = NamesExt.create("Association");
        final GenericName asName2 = NamesExt.create("Association2");
        final FeatureTypeFactory FTF = getFeatureTypeFactory();

        final AttributeType attrType = FTF.createAttributeType(name, String.class, true, false, null, null, null);

        final AssociationType asType = FTF.createAssociationType(asName, attrType, true, null, null, null);

        assertNotNull("AssociationType creation failed", asType);
        //name
        assertNotNull("AssociationType name not set", asType.getName());
        assertEquals("AssociationType name does not match", asName, asType.getName());
        //type
        assertNotNull("AssociationType related type not set", asType.getRelatedType());
        assertEquals("AssociationType related type does not match", attrType, asType.getRelatedType());
        //abstraction
        assertTrue("Association must be marked as abstract", asType.isAbstract());

        //second object to test
        final AttributeType attrType2 = FTF.createAttributeType(name2, String.class, true, false, null, null, null);
        final AssociationType asType2 = FTF.createAssociationType(asName2, attrType2, false, null, asType, descriptionSample);
        assertNotNull("AssociationType creation failed", asType2);
        //Name
        assertNotNull("AssociationType name not set", asType2.getName());
        assertEquals("AssociationType name does not match", asName2, asType2.getName());
        //parent
        assertNotNull("AssociationType parent is not set", asType2.getSuper());
        assertEquals("Association parent does not match", asType, asType2.getSuper());
        //Related Type
        assertNotNull("AssociationType related type not set", asType2.getRelatedType());
        assertEquals("AssociationType related type does not match", attrType2, asType2.getRelatedType());
        //abstraction
        assertTrue("Association must be marked as abstract", asType.isAbstract());
        //description
        assertNotNull("Attribute description is NULL", asType2.getDescription());
        assertEquals("Attribute description is wrong", descriptionSample.toString(), asType2.getDescription().toString());
    }

    /**
     * Test for {@link AttributeDescriptor} creation. Check for name,
     * defaultValue (trigger and value), occurrence numbers, and related type.
     */
    @Test
    public void testCreateAttributeDescriptor() {
        final GenericName name = NamesExt.create("lines");
        final GenericName adName = NamesExt.create("lines descriptor");
        final FeatureTypeFactory FTF = getFeatureTypeFactory();

        final AttributeType attrType = FTF.createAttributeType(name, String.class, true, false, null, null, null);
        final AttributeDescriptor res = FTF.createAttributeDescriptor(attrType, adName, 1, 1, false, "defaultvalueishere");
        //tests
        assertNotNull("Attribute descriptor not set", res);
        //type
        assertNotNull("Related type not set", res.getType());
        assertEquals("related type is wrong", attrType, res.getType());
        //name
        assertNotNull("Attribute descriptor name not set", res.getName());
        assertEquals("Attribute descriptor name does not match", res.getName(), adName);
        //occurrences
        assertTrue("Attribute occurrences are wrong", res.getMinOccurs() == 1 && res.getMaxOccurs() == 1);
        //defaul value
        assertFalse("Trigger for nillable default value have to be set to false here", res.isNillable());
        assertNotNull("Attribute default value not set", res.getDefaultValue());
        assertEquals("Attribute default value does not match", res.getDefaultValue(), "defaultvalueishere");

        //second object to test
        final AttributeDescriptor res2 = FTF.createAttributeDescriptor(attrType, adName, 666, 7777, true, null);
        assertNotNull("Attribute descriptor not set", res2);
        //type
        assertNotNull("Related type not set", res2.getType());
        assertEquals("related type is wrong", attrType, res2.getType());
        //name
        assertNotNull("Attribute descriptor name not set", res2.getName());
        assertEquals("Attribute descriptor name does not match", res2.getName(), adName);
        //default value
        assertTrue("Trigger for nillable default value have to be set to true here", res2.isNillable());
        assertNull("Attribute default value should be null here", res2.getDefaultValue());
        //occurrences
        assertTrue("Attribute occurrences are wrong", res2.getMinOccurs() == 666 && res2.getMaxOccurs() == 7777);
    }

    /**
     * Test for {@link GeometryDescriptor} creation. Check for name,
     * defaultValue (trigger and value), occurrence numbers, and related type.
     */
    @Test
    public void testCreateGeometryDescriptor() {
        final GenericName name = NamesExt.create("geometry");
        final GenericName gdName = NamesExt.create("geomatry descriptor");
        final FeatureTypeFactory FTF = getFeatureTypeFactory();

        final GeometryType attrType = FTF.createGeometryType(name, MockDirectPosition2D.class, null, true, false, null, null, null);

        final AttributeDescriptor res = FTF.createGeometryDescriptor(attrType, gdName, 0, Integer.MAX_VALUE, true, null);

        //tests
        assertNotNull("Geometry descriptor not set", res);
        //type
        assertNotNull("Related type not set", res.getType());
        assertEquals("related type is wrong", attrType, res.getType());
        //name
        assertNotNull("Geometry descriptor name not set", res.getName());
        assertEquals("Geometry descriptor name does not match", res.getName(), gdName);
        //default value
        assertTrue("Trigger for nillable default value have to be set to true here", res.isNillable());
        assertNull("Attribute default should be null here", res.getDefaultValue());
        //occurrences
        assertTrue("Geometry occurrences are wrong", res.getMinOccurs() == 0 && res.getMaxOccurs() == Integer.MAX_VALUE);

        //second object to test
        final AttributeDescriptor res2 = FTF.createAttributeDescriptor(attrType, gdName, 4, 13, false, new MockDirectPosition2D(0, 0));
        assertNotNull("Attribute descriptor not set", res2);
        //type
        assertNotNull("Related type not set", res2.getType());
        assertEquals("related type is wrong", attrType, res2.getType());
        //name
        assertNotNull("Attribute descriptor name not set", res2.getName());
        assertEquals("Attribute descriptor name does not match", res2.getName(), gdName);
        //default value
        assertFalse("Trigger for nillable default value have to be set to false here", res2.isNillable());
        assertNotNull("Attribute default value can't be null here", res2.getDefaultValue());
        assertEquals("Default value des not match", new MockDirectPosition2D(0, 0), res2.getDefaultValue());
        //occurrences
        assertTrue("Attribute occurrences are wrong", res2.getMinOccurs() == 4 && res2.getMaxOccurs() == 13);
    }

    /**
     * Test for {@link AssociationDescriptor} creation. Check for name,
     * defaultValue (trigger), occurrence numbers, and related type.
     */
    @Test
    public void testCreateAssociationDescriptor() {
        final GenericName name = NamesExt.create("lines");
        final GenericName asName = NamesExt.create("Association");
        final GenericName adName = NamesExt.create("descriptor");
        final FeatureTypeFactory FTF = getFeatureTypeFactory();

        final AttributeType attrType = FTF.createAttributeType(name, String.class, true, false, null, null, null);
       final  AssociationType asType = FTF.createAssociationType(asName, attrType, false, null, null, null);

        final AssociationDescriptor res = FTF.createAssociationDescriptor(asType, adName, 1, 1, true);

        //tests
        assertNotNull("Geometry descriptor not set", res);
        //type
        assertNotNull("Related type not set", res.getType());
        assertEquals("related type is wrong", asType, res.getType());
        //name
        assertNotNull("Geometry descriptor name not set", res.getName());
        assertEquals("Geometry descriptor name does not match", res.getName(), adName);
        //default value
        assertTrue("Trigger for nillable default value have to be set to true here", res.isNillable());
        //occurrences
        assertTrue("Geometry occurrences are wrong", res.getMinOccurs() == 1 && res.getMaxOccurs() == 1);

        //second object to test
        final AssociationDescriptor res2 = FTF.createAssociationDescriptor(asType, adName, 2, 19, false);
        assertNotNull("Attribute descriptor not set", res2);
        //type
        assertNotNull("Association type not set", res2.getType());
        assertEquals("Association type is wrong", asType, res2.getType());
        //name
        assertNotNull("Attribute descriptor name not set", res2.getName());
        assertEquals("Attribute descriptor name does not match", res2.getName(), adName);
        //default value
        assertFalse("Trigger for nillable default value have to be set to false here", res2.isNillable());
        //occurrences
        assertTrue("Attribute occurrences are wrong", res2.getMinOccurs() == 2 && res2.getMaxOccurs() == 19);
    }

    /**
     * Test for {@link SimpleFeatureType} creation. Check for name, descriptors,
     * abstract level, and parent.
     */
    @Test
    public void createSimpleFeatureType() {
        final GenericName name = NamesExt.create("point");
        final GenericName strName = NamesExt.create("String");
        final GenericName fName = NamesExt.create("featureTypeName");
        final FeatureTypeFactory FTF = getFeatureTypeFactory();

        //types and descriptors
        final GeometryType geoType = FTF.createGeometryType(name, MockDirectPosition2D.class, null, true, false, null, null, null);
        final GeometryDescriptor geoDesc = FTF.createGeometryDescriptor(geoType, name, 1, 1, true, null);
        final AttributeType type = FTF.createAttributeType(strName, String.class, true, false, null, null, null);
        final AttributeDescriptor descriptor = FTF.createAttributeDescriptor(type, strName, 1, 1, false, "line");
        final List<AttributeDescriptor> descList = new ArrayList(2);
        descList.add(descriptor);
        descList.add(geoDesc);

        //feature creation
        final FeatureType fType = FTF.createSimpleFeatureType(fName, descList, geoDesc, false, null, type, descriptionSample);

        //Tests
        assertNotNull("Feature type have not been created");
        //Name
        assertNotNull("Name is not set", fType.getName());
        assertEquals("Feature type name is not properly set", fType.getName(), fName);
        //descriptor
        assertNotNull("Descriptors are not set", fType.getDescriptors());
        assertEquals("Feature descriptors are not properly set", fType.getDescriptor(strName), descriptor);
        //geometry descriptor
        assertNotNull("Geometry descriptor is not set", fType.getGeometryDescriptor());
        assertEquals("Feature descriptors are not properly set", fType.getDescriptor(name), geoDesc);
        //abstract value
        assertFalse("Feature type set as abstracted while it's not", fType.isAbstract());
        //parent
        assertNotNull("Parent attribute has not been set", fType.getSuper());
        assertEquals("Parent is wrong", type, fType.getSuper());
        //description
        assertNotNull("Attribute description is NULL", fType.getDescription());
        assertEquals("Attribute description is wrong", descriptionSample.toString(), fType.getDescription().toString());

        //check for access to non-existant descriptor
        try {
            fType.getDescriptor("non-existant");
        } catch (Exception e) {
        }
        GenericName tmpName = NamesExt.create("non-existant");
        try {
            fType.getDescriptor(tmpName);
        } catch (Exception e) {
        }
    }
}
