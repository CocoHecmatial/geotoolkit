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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotoolkit.factory.FactoryFinder;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.feature.type.DefaultFeatureTypeFactory;
import org.geotoolkit.filter.function.other.OtherFunctionFactory;
import org.geotoolkit.filter.IllegalFilterException;
import org.geotoolkit.util.converter.Classes;
import org.geotoolkit.util.SimpleInternationalString;

import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Builder for attribute types and descriptors.
 * <p>
 * Building an attribute type:
 * <pre>
 * <code>
 *  //create the builder
 * 	AttributeTypeBuilder builder = new AttributeTypeBuilder();
 *
 *  //set type information
 *  builder.setName( "intType" ):
 *  builder.setBinding( Integer.class );
 *  builder.setNillable( false );
 *
 *  //build the type
 *  AttributeType type = builder.buildType();
 * </code>
 * </pre>
 * </p>
 * <p>
 * Building an attribute descriptor:
 * <pre>
 * <code>
 *  //create the builder
 * 	AttributeTypeBuilder builder = new AttributeTypeBuilder();
 *
 *  //set type information
 *  builder.setName( "intType" ):
 *  builder.setBinding( Integer.class );
 *  builder.setNillable( false );
 *
 *  //set descriptor information
 *  builder.setMinOccurs(0);
 *  builder.setMaxOccurs(1);
 *  builder.setNillable(true);
 *
 *  //build the descriptor
 *  AttributeDescriptor descriptor = builder.buildDescriptor("intProperty");
 * </code>
 * </pre>
 * <p>
 * This class maintains state and is not thread safe.
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 * @module pending
 */
public class AttributeTypeBuilder {

    /**
     * filter factory
     */
    protected static final FilterFactory2 FF = (FilterFactory2) FactoryFinder.getFilterFactory(new Hints(Hints.FILTER_FACTORY, FilterFactory2.class));

    /**
     * factory
     */
    protected FeatureTypeFactory factory;
    //AttributeType
    //
    /**
     * Local name used to name a descriptor; or combined with namespaceURI to name a type.
     */
    protected String name;
    /**
     * Separator used to combine namespaceURI and name.
     */
    private String separator = ":";
    /**
     * namespace used to distingish between otherwise identical type names.
     */
    protected String namespaceURI;
    /**
     * abstract flag
     */
    protected boolean isAbstract = false;
    /**
     * restrictions
     */
    protected List<Filter> restrictions;
    /**
     * string description
     */
    protected String description;
    /**
     * identifiable flag
     */
    protected boolean isIdentifiable = false;
    /**
     * bound java class
     */
    protected Class binding;
    /**
     * super type
     */
    protected AttributeType superType;
    /**
     * default value
     */
    protected Object defaultValue;
    protected boolean isDefaultValueSet = false;
    //GeometryType
    //
    protected CoordinateReferenceSystem crs;
    protected boolean isCrsSet = false;
    //AttributeDescriptor
    //
    /**
     * Minimum number of occurrences allowed.
     * See minOccurs() function for the default value
     * based on nillable if not explicitly set.
     */
    protected Integer minOccurs = null;
    /**
     * Maximum number of occurrences allowed.
     * See maxOccurs() function for the default value (of 1).
     */
    protected Integer maxOccurs = null;
    /**
     * True if value is allowed to be null.
     * <p>
     * Depending on this value minOccurs, maxOccurs and defaultValue()
     * will return different results.
     * <p>
     * The default value is <code>true</code>.
     */
    protected boolean isNillable = true;
    /**
     * If this value is set an additional restriction
     * will be added based on the length function.
     */
    protected Integer length = null;
    /**
     * User data for the attribute.
     */
    protected Map userData = null;
    
    /**
     * Constructs the builder.
     *
     */
    public AttributeTypeBuilder() {
        this(new DefaultFeatureTypeFactory());
        init();
    }

    /**
     * Constructs the builder specifying the factory used to build attribute
     * types.
     *
     */
    public AttributeTypeBuilder(final FeatureTypeFactory factory) {
        this.factory = factory;
        init();
    }

    /**
     * Resets all internal state.
     */
    protected void init() {
        resetTypeState();
        resetDescriptorState();
    }

    /**
     * Resets all builder state used to build the attribute type.
     * <p>
     * This method is called automatically after {@link #buildType()} and
     * {@link #buildGeometryType()}.
     * </p>
     */
    protected void resetTypeState() {
        name = null;
        namespaceURI = null;
        isAbstract = false;
        restrictions = null;
        description = null;
        isIdentifiable = false;
        binding = null;
        defaultValue = null;
        superType = null;
        crs = null;
        length = null;
        isCrsSet = false;
        isDefaultValueSet = false;
    }

    protected void resetDescriptorState() {
        minOccurs = null;
        maxOccurs = null;
        isNillable = true;
        userData = new HashMap();
    }

    public AttributeTypeBuilder setFactory(final FeatureTypeFactory factory) {
        this.factory = factory;
        return this;
    }

    /**
     * Initializes builder state from another attribute type.
     */
    public AttributeTypeBuilder init(final AttributeType type) {
        name = type.getName().getLocalPart();
        separator = type.getName().getSeparator();
        namespaceURI = type.getName().getNamespaceURI();
        isAbstract = type.isAbstract();

        if (type.getRestrictions() != null) {
            restrictions().addAll(type.getRestrictions());
        }

        description = type.getDescription() != null ? type.getDescription().toString() : null;
        isIdentifiable = type.isIdentified();
        binding = type.getBinding();
        superType = type.getSuper();

        if (type instanceof GeometryType) {
            crs = ((GeometryType) type).getCoordinateReferenceSystem();
        }
        return this;
    }

    /**
     * Initializes builder state from another attribute descriptor.
     */
    public void init(final AttributeDescriptor descriptor) {
        init(descriptor.getType());
        minOccurs = descriptor.getMinOccurs();
        maxOccurs = descriptor.getMaxOccurs();
        isNillable = descriptor.isNillable();
    }

    // Type methods
    //
    public void setBinding(final Class binding) {
        this.binding = binding;

        //JD: tidbit here
        if (!isDefaultValueSet) {
            //genereate a good default value based on class
            try {
                defaultValue = FeatureUtilities.defaultValue(binding);
            } catch (Exception e) {
                //do nothing
            }
        }
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setNamespaceURI(final String namespaceURI) {
        this.namespaceURI = namespaceURI;
    }

    public void setCRS(final CoordinateReferenceSystem crs) {
        this.crs = crs;
        isCrsSet = true;
    }

    public boolean isCRSSet() {
        return isCrsSet;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setAbstract(final boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public void setIdentifiable(final boolean isIdentifiable) {
        this.isIdentifiable = isIdentifiable;
    }

    public void setLength(final int length) {
        this.length = length;
    }

    public void addRestriction(final Filter restriction) {
        restrictions().add(restriction);
    }

    public void addUserData(final Object key, final Object value) {
        userData.put(key, value);
    }

    // Descriptor methods
    //
    public void setNillable(final boolean isNillable) {
        this.isNillable = isNillable;
    }

    public void setMaxOccurs(final int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public void setMinOccurs(final int minOccurs) {
        this.minOccurs = minOccurs;
    }

    public void setDefaultValue(final Object defaultValue) {
        this.defaultValue = defaultValue;
        isDefaultValueSet = true;
    }

    public AttributeTypeBuilder binding(final Class binding) {
        setBinding(binding);
        return this;
    }

    public AttributeTypeBuilder name(final String name) {
        setName(name);
        return this;
    }

    public AttributeTypeBuilder namespaceURI(final String namespaceURI) {
        setNamespaceURI(namespaceURI);
        return this;

    }

    public AttributeTypeBuilder crs(final CoordinateReferenceSystem crs) {
        setCRS(crs);
        return this;
    }

    public AttributeTypeBuilder description(final String description) {
        setDescription(description);
        return this;
    }

    public AttributeTypeBuilder abstrct(final boolean isAbstract) {
        setAbstract(isAbstract);
        return this;
    }

    public AttributeTypeBuilder identifiable(final boolean isIdentifiable) {
        setIdentifiable(isIdentifiable);
        return this;
    }

    public AttributeTypeBuilder length(final int length) {
        setLength(length);
        return this;
    }

    public AttributeTypeBuilder restriction(final Filter restriction) {
        addRestriction(restriction);
        return this;
    }

    // Descriptor methods
    //
    public AttributeTypeBuilder nillable(final boolean isNillable) {
        setNillable(isNillable);
        return this;
    }

    public AttributeTypeBuilder maxOccurs(final int maxOccurs) {
        setMaxOccurs(maxOccurs);
        return this;
    }

    public AttributeTypeBuilder minOccurs(final int minOccurs) {
        setMinOccurs(minOccurs);
        return this;
    }

    public AttributeTypeBuilder defaultValue(final Object defaultValue) {
        setDefaultValue(defaultValue);
        return this;
    }

    public AttributeTypeBuilder userData(final Object key, final Object value) {
        addUserData(key, value);
        return this;
    }

    // construction methods
    //
    /**
     * Builds the attribute type.
     * <p>
     * This method resets all state after the attribute is built.
     * </p>
     */
    public AttributeType buildType() {
        if (length != null) {
            final Filter lengthRestriction = lengthRestriction(length);
            restrictions().add(lengthRestriction);
        }

        final AttributeType type = factory.createAttributeType(
                name(), binding, isIdentifiable, isAbstract,
                restrictions(), superType, description());
        resetTypeState();

        return type;
    }

    protected String typeName() {
        if (name == null) {
            return Classes.getShortName(binding);
        }
        return name;
    }

    private InternationalString description() {
        return description != null ? new SimpleInternationalString(description) : null;
    }

    /**
     * Builds the geometry attribute type.
     * <p>
     * This method resets all state after the attribute is built.
     * </p>
     */
    public GeometryType buildGeometryType() {
        final GeometryType type = factory.createGeometryType(
                name(), binding, crs, isIdentifiable, isAbstract,
                restrictions(), superType, description());

        resetTypeState();

        return type;
    }

    /**
     * Builds an attribute descriptor first building an attribute type from
     * internal state.
     * <p>
     * If {@link #crs} has been set via {@link #setCRS(CoordinateReferenceSystem)}
     * the internal attribute type will be built via {@link #buildGeometryType()},
     * otherwise it will be built via {@link #buildType()}.
     * </p>
     * <p>
     * This method calls through to {@link #buildDescriptor(String, AttributeType)}.
     * </p>
     * @param name The name of the descriptor.
     *
     * @see #buildDescriptor(String, AttributeType)
     */
    public AttributeDescriptor buildDescriptor(final String name) {
        setName(name);
        if (binding == null) {
            throw new IllegalStateException("No binding has been provided for this attribute");
        }
        if (crs != null || Geometry.class.isAssignableFrom(binding)) {
            return buildDescriptor(name, buildGeometryType());
        } else {
            return buildDescriptor(name, buildType());
        }
    }

    /**
     * Builds an attribute descriptor specifying its attribute type.
     * <p>
     * Internal state is reset after the descriptor is built.
     * </p>
     * @param name The name of the descriptor.
     * @param type The type referenced by the descriptor.
     *
     */
    public AttributeDescriptor buildDescriptor(final String name, final AttributeType type) {
        return buildDescriptor(new DefaultName(namespaceURI,name), type);
    }

    /**
     * Builds a geometry descriptor specifying its attribute type.
     * <p>
     * Internal state is reset after the descriptor is built.
     * </p>
     * @param name The name of the descriptor.
     * @param type The geometry type referenced by the descriptor.
     *
     */
    public GeometryDescriptor buildDescriptor(final String name, final GeometryType type) {
        return buildDescriptor(new DefaultName(namespaceURI,name), type);
    }

    public AttributeDescriptor buildDescriptor(final Name name, final AttributeType type) {

        //build the descriptor
        final AttributeDescriptor descriptor = factory.createAttributeDescriptor(
                type, name, minOccurs(), maxOccurs(), isNillable, defaultValue());

        //set the user data
        descriptor.getUserData().putAll(userData);
        resetDescriptorState();
        return descriptor;
    }

    public GeometryDescriptor buildDescriptor(final Name name, final GeometryType type) {
        final GeometryDescriptor descriptor = factory.createGeometryDescriptor(
                type, name, minOccurs(), maxOccurs(), isNillable, defaultValue());

        // set the user data
        descriptor.getUserData().putAll(userData);
        resetDescriptorState();
        return descriptor;
    }

    /**
     * This is not actually right but we do it for backwards compatibility.
     * @return minOccurs if set or a default based on isNillable.
     */
    private int minOccurs() {
        if (minOccurs == null) {
            return isNillable ? 0 : 1;
        }
        return minOccurs;
    }

    /**
     * This is not actually right but we do it for backwards compatibility.
     * @return minOccurs if set or a default based on isNillable.
     */
    private int maxOccurs() {
        if (maxOccurs == null) {
            return 1;
        }
        return maxOccurs;
    }

    private Name name() {
        if (separator == null) {
            return new DefaultName(namespaceURI, typeName());
        } else {
            return new DefaultName(namespaceURI, separator, typeName());
        }
    }

    private Object defaultValue() {
        if (defaultValue == null && !isNillable && binding != null) {
            defaultValue = FeatureUtilities.defaultValue(binding);
        }
        return defaultValue;
    }

    // internal / subclass api
    //
    protected List<Filter> restrictions() {
        if (restrictions == null) {
            restrictions = new ArrayList();
        }

        return restrictions;
    }

    /**
     * Helper method to create a "length" filter.
     */
    protected Filter lengthRestriction(final int length) {
        if (length < 0) {
            return null;
        }
        final Expression lengthFunction = FF.function(OtherFunctionFactory.EXPRESSION_VALUE_LENGHT,FF.property(name));
        if (lengthFunction == null) {
            return null;
        }
        Filter cf = null;
        try {
            cf = FF.lessOrEqual(lengthFunction, FF.literal(length));
        } catch (IllegalFilterException e) {
            // TODO something
        }
        return (cf == null) ? Filter.EXCLUDE : cf;
    }
}
