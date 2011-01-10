/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008 - 2009, Geomatys
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
package org.geotoolkit.wfs.xml.v110;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * A GetFeature element contains one or more Query elements that describe a query operation on one feature type.  
 * In response to a GetFeature request, a Web Feature Service must be able to generate a GML3 response that validates
 * using a schema generated by the DescribeFeatureType request.
 * A Web Feature Service may support other possibly non-XML (and even binary) output formats as long as those formats
 * are advertised in the capabilities document.
 *          
 * 
 * <p>Java class for GetFeatureType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GetFeatureType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.opengis.net/wfs}BaseRequestType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.opengis.net/wfs}Query" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="resultType" type="{http://www.opengis.net/wfs}ResultTypeType" default="results" />
 *       &lt;attribute name="outputFormat" type="{http://www.w3.org/2001/XMLSchema}string" default="text/xml; subtype=gml/3.1.1" />
 *       &lt;attribute name="maxFeatures" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" />
 *       &lt;attribute name="traverseXlinkDepth" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="traverseXlinkExpiry" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 * @module pending
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetFeatureType", propOrder = {
    "query",
    "featureId"
})
@XmlRootElement(name = "GetFeature")
public class GetFeatureType extends BaseRequestType {

    @XmlElement(name = "Query", required = true)
    private List<QueryType> query;
    @XmlAttribute
    private ResultTypeType resultType;
    @XmlAttribute
    private String outputFormat;
    @XmlAttribute
    @XmlSchemaType(name = "positiveInteger")
    private Integer maxFeatures;
    @XmlAttribute
    private String traverseXlinkDepth;
    @XmlAttribute
    @XmlSchemaType(name = "positiveInteger")
    private Integer traverseXlinkExpiry;
    private String featureId;
    


    public GetFeatureType() {

    }

    public GetFeatureType(final String service, final String version, final String handle, final Integer maxFeatures,
            final List<QueryType> query, final ResultTypeType resultType, final String outputformat) {
        super(service, version, handle);
        this.maxFeatures  = maxFeatures;
        this.query        = query;
        this.resultType   = resultType;
        this.outputFormat = outputformat;
    }

    public GetFeatureType(final String service, final String version, final String handle, final Integer maxFeatures,
            final String featureId, final List<QueryType> query, final ResultTypeType resultType, final String outputformat) {
        super(service, version, handle);
        this.maxFeatures  = maxFeatures;
        this.featureId    = featureId;
        this.resultType   = resultType;
        this.outputFormat = outputformat;
        this.query        = query;
    }

    public GetFeatureType(final String service, final String version, final String handle, final Integer maxFeatures,
            final List<QueryType> query, final ResultTypeType resultType, final String outputformat, final String trXlinkDepth, final Integer trXlinkExpiry) {
        super(service, version, handle);
        this.maxFeatures  = maxFeatures;
        this.query        = query;
        this.resultType   = resultType;
        this.outputFormat = outputformat;
        this.traverseXlinkDepth  = trXlinkDepth;
        this.traverseXlinkExpiry = trXlinkExpiry;
    }

    /**
     * Gets the value of the query property.
     */
    public List<QueryType> getQuery() {
        if (query == null) {
            query = new ArrayList<QueryType>();
        }
        return this.query;
    }

    /**
     * Gets the value of the resultType property.
     * 
     * @return
     *     possible object is
     *     {@link ResultTypeType }
     *     
     */
    public ResultTypeType getResultType() {
        if (resultType == null) {
            return ResultTypeType.RESULTS;
        } else {
            return resultType;
        }
    }

    /**
     * Sets the value of the resultType property.
     * 
     * @param value
     *     allowed object is
     *     {@link ResultTypeType }
     *     
     */
    public void setResultType(final ResultTypeType value) {
        this.resultType = value;
    }

    /**
     * Gets the value of the outputFormat property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOutputFormat() {
        if (outputFormat == null) {
            return "text/xml; subtype=gml/3.1.1";
        } else {
            return outputFormat;
        }
    }

    /**
     * Sets the value of the outputFormat property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOutputFormat(final String value) {
        this.outputFormat = value;
    }

    /**
     * Gets the value of the maxFeatures property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMaxFeatures() {
        return maxFeatures;
    }

    /**
     * Sets the value of the maxFeatures property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMaxFeatures(final Integer value) {
        this.maxFeatures = value;
    }

    /**
     * Gets the value of the traverseXlinkDepth property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTraverseXlinkDepth() {
        return traverseXlinkDepth;
    }

    /**
     * Sets the value of the traverseXlinkDepth property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTraverseXlinkDepth(final String value) {
        this.traverseXlinkDepth = value;
    }

    /**
     * Gets the value of the traverseXlinkExpiry property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getTraverseXlinkExpiry() {
        return traverseXlinkExpiry;
    }

    /**
     * Sets the value of the traverseXlinkExpiry property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setTraverseXlinkExpiry(final Integer value) {
        this.traverseXlinkExpiry = value;
    }

    /**
     * @return the featureId
     */
    public String getFeatureId() {
        return featureId;
    }

    /**
     * @param featureId the featureId to set
     */
    public void setFeatureId(final String featureId) {
        this.featureId = featureId;
    }

}
