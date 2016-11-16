/*
 *    GeotoolKit - An Open Source Java GIS Toolkit
 *    http://geotoolkit.org
 *
 *    (C) 2009, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotoolkit.feature.catalog;

import java.util.Map;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.feature.catalog.BoundFeatureAttribute;
import org.opengis.feature.catalog.FeatureAttribute;
import org.opengis.feature.catalog.FeatureType;

/**
 *
 * @author Guilhem Legal
 * @module
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FC_BoundFeature_Type", propOrder = {
    "featureType",
    "attribute"
})
@XmlRootElement(name = "FC_BoundFeature")
public class BoundFeatureAttributeImpl implements BoundFeatureAttribute, Referenceable {

    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    private String id;

    @XmlElement(required = true)
    private FeatureType featureType;
    @XmlElement(required = true)
    private FeatureAttribute attribute;

    @XmlTransient
    private boolean isReference = false;

    /**
     * An empty constructor used by JAXB
     */
    public BoundFeatureAttributeImpl() {

    }

    /**
     * Clone a FeatureAssociation
     */
    public BoundFeatureAttributeImpl(final BoundFeatureAttribute feature) {
        if (feature != null) {
            this.id          = feature.getId();
            this.attribute   = feature.getAttribute();
            this.featureType = feature.getFeatureType();
        }

    }

    public FeatureType getFeatureType() {
        return featureType;
    }

    public void setFeatureType(final FeatureType featureType) {
        this.featureType = featureType;
    }

    public FeatureAttribute getAttribute() {
       return attribute;
    }

    public void setAttribute(final FeatureAttribute attribute) {
       this.attribute = attribute;
    }

     public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    /**
     * set the feature in reference mode
     */
    public void setReference(final boolean mode) {
        this.isReference = mode;
    }

     /**
     * get the current feature in reference mode
     */
    public boolean isReference() {
        return isReference;
    }

    public BoundFeatureAttributeImpl getReference() {
        BoundFeatureAttributeImpl reference = new BoundFeatureAttributeImpl(this);
        reference.setReference(true);
        return reference;
    }

    public Map<String, Referenceable> beforeMarshal(Map<String, Referenceable> alreadySee) {
        if (id != null && !id.isEmpty()) {
            alreadySee.put(id, this);
        }

        if (featureType != null) {
            if (alreadySee.get(featureType.getId()) != null) {
                featureType = ((FeatureTypeImpl)featureType).getReference();
            } else {
                alreadySee = ((FeatureTypeImpl)featureType).beforeMarshal(alreadySee);
            }
        }

        if (attribute != null) {
            if (alreadySee.get(attribute.getId()) != null) {
                attribute = ((FeatureAttributeImpl)attribute).getReference();
            } else {
                alreadySee = ((FeatureAttributeImpl)attribute).beforeMarshal(alreadySee);
            }
        }
        return alreadySee;
     }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("[BoundFeatureAttribute]:").append('\n');
        if (featureType != null)
            s.append("featureType: ").append(featureType.toString()).append('\n');
        if (attribute != null)
            s.append("attribute: ").append(attribute.toString()).append('\n');

        return s.toString();
    }

    /**
     * Verify if this entry is identical to the specified object.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof BoundFeatureAttributeImpl) {
            final BoundFeatureAttributeImpl that = (BoundFeatureAttributeImpl) object;

            return Objects.equals(this.attribute,   that.attribute) &&
                   Objects.equals(this.featureType, that.featureType);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 11 * hash + (this.featureType != null ? this.featureType.hashCode() : 0);
        hash = 11 * hash + (this.attribute != null ? this.attribute.hashCode() : 0);
        return hash;
    }

}
