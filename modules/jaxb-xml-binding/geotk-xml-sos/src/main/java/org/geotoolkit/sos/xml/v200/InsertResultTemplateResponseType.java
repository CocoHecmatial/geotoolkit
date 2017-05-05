/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Geomatys
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

package org.geotoolkit.sos.xml.v200;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import org.geotoolkit.sos.xml.InsertResultTemplateResponse;
import org.geotoolkit.swes.xml.v200.ExtensibleResponseType;


/**
 * <p>Java class for InsertResultTemplateResponseType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="InsertResultTemplateResponseType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.opengis.net/swes/2.0}ExtensibleResponseType">
 *       &lt;sequence>
 *         &lt;element name="acceptedTemplate" type="{http://www.w3.org/2001/XMLSchema}anyURI"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InsertResultTemplateResponseType", propOrder = {
    "acceptedTemplate"
})
@XmlRootElement(name="InsertResultTemplateResponse")
public class InsertResultTemplateResponseType extends ExtensibleResponseType implements InsertResultTemplateResponse {

    @XmlElement(required = true)
    @XmlSchemaType(name = "anyURI")
    private String acceptedTemplate;

    public InsertResultTemplateResponseType() {

    }

    public InsertResultTemplateResponseType(final String acceptedTemplate) {
        this.acceptedTemplate = acceptedTemplate;
    }

    /**
     * Gets the value of the acceptedTemplate property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getAcceptedTemplate() {
        return acceptedTemplate;
    }

    /**
     * Sets the value of the acceptedTemplate property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setAcceptedTemplate(String value) {
        this.acceptedTemplate = value;
    }

}
