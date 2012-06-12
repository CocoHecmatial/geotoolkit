/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008 - 2011, Geomatys
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


package org.geotoolkit.wfs.xml.v200;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import org.geotoolkit.ows.xml.v110.MetadataType;
import org.geotoolkit.wfs.xml.StoredQueryDescription;


/**
 * <p>Java class for StoredQueryDescriptionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StoredQueryDescriptionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.opengis.net/wfs/2.0}Title" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/wfs/2.0}Abstract" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.opengis.net/ows/1.1}Metadata" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Parameter" type="{http://www.opengis.net/wfs/2.0}ParameterExpressionType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="QueryExpressionText" type="{http://www.opengis.net/wfs/2.0}QueryExpressionTextType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StoredQueryDescriptionType", propOrder = {
    "title",
    "_abstract",
    "metadata",
    "parameter",
    "queryExpressionText"
})
public class StoredQueryDescriptionType implements StoredQueryDescription {

    @XmlElement(name = "Title")
    private List<Title> title;
    @XmlElement(name = "Abstract")
    private List<Abstract> _abstract;
    @XmlElement(name = "Metadata", namespace = "http://www.opengis.net/ows/1.1")
    private List<MetadataType> metadata;
    @XmlElement(name = "Parameter")
    private List<ParameterExpressionType> parameter;
    @XmlElement(name = "QueryExpressionText", required = true)
    private List<QueryExpressionTextType> queryExpressionText;
    @XmlAttribute(required = true)
    @XmlSchemaType(name = "anyURI")
    private String id;

    public StoredQueryDescriptionType() {
        
    }
    
    public StoredQueryDescriptionType(final StoredQueryDescription that) {
        throw new UnsupportedOperationException("TODO not implemented yet");
    }
    
    public StoredQueryDescriptionType(final String id, final String title, final String _abstract, final ParameterExpressionType parameter,
            final QueryExpressionTextType queryExpressionText) {
        this.id = id;
        if (title != null) {
            this.title = new ArrayList<Title>();
            this.title.add(new Title(title));
        }
        if (_abstract != null) {
            this._abstract = new ArrayList<Abstract>();
            this._abstract.add(new Abstract(_abstract));
        }
        if (parameter != null) {
            this.parameter = new ArrayList<ParameterExpressionType>();
            this.parameter.add(parameter);
        }
        if (queryExpressionText != null) {
            this.queryExpressionText = new ArrayList<QueryExpressionTextType>();
            this.queryExpressionText.add(queryExpressionText);
        }
    }
    
    /**
     * Gets the value of the title property.
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Title }
     */
    public List<Title> getTitle() {
        if (title == null) {
            title = new ArrayList<Title>();
        }
        return this.title;
    }

    /**
     * Gets the value of the abstract property.
     * 
     * {@link Abstract }
     */
    public List<Abstract> getAbstract() {
        if (_abstract == null) {
            _abstract = new ArrayList<Abstract>();
        }
        return this._abstract;
    }

    /**
     * Gets the value of the metadata property.
     * 
     * Objects of the following type(s) are allowed in the list
     * {@link MetadataType }
     */
    public List<MetadataType> getMetadata() {
        if (metadata == null) {
            metadata = new ArrayList<MetadataType>();
        }
        return this.metadata;
    }

    /**
     * Gets the value of the parameter property.
     * 
     * Objects of the following type(s) are allowed in the list
     * {@link ParameterExpressionType }
     */
    public List<ParameterExpressionType> getParameter() {
        if (parameter == null) {
            parameter = new ArrayList<ParameterExpressionType>();
        }
        return this.parameter;
    }

    /**
     * Gets the value of the queryExpressionText property.
     * Objects of the following type(s) are allowed in the list
     * {@link QueryExpressionTextType }
     */
    public List<QueryExpressionTextType> getQueryExpressionText() {
        if (queryExpressionText == null) {
            queryExpressionText = new ArrayList<QueryExpressionTextType>();
        }
        return this.queryExpressionText;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

}
