/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.04.20 at 07:08:32 PM CEST
//


package org.w3._2005.atom;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for KnownRelCodeType.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="KnownRelCodeType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="alternate"/>
 *     &lt;enumeration value="related"/>
 *     &lt;enumeration value="self"/>
 *     &lt;enumeration value="enclosure"/>
 *     &lt;enumeration value="via"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "KnownRelCodeType")
@XmlEnum
public enum KnownRelCodeType {


    /**
     * The value "alternate" signifies that the IRI in the value of the href attribute identifies an alternate version of the resource described by the containing element.
     *
     */
    @XmlEnumValue("alternate")
    ALTERNATE("alternate"),

    /**
     * The value "related" signifies that the IRI in the value of the href attribute identifies a resource related to the resource described by the containing element.  For example, the feed for a site that discusses the performance of the search engine at "http://search.example.com" might contain, as a child of atom:feed.  An identical link might appear as a child of any atom:entry whose content contains a discussion of that same search engine.
     *
     */
    @XmlEnumValue("related")
    RELATED("related"),

    /**
     * The value "self" signifies that the IRI in the value of the href attribute identifies a resource equivalent to the containing element.
     *
     */
    @XmlEnumValue("self")
    SELF("self"),

    /**
     * The value "enclosure" signifies that the IRI in the value of the href attribute identifies a related resource that is potentially large in size and might require special handling.  For atom:link elements with rel="enclosure", the length attribute SHOULD be provided.
     *
     */
    @XmlEnumValue("enclosure")
    ENCLOSURE("enclosure"),

    /**
     * The value "via" signifies that the IRI in the value of the href attribute identifies a resource that is the source of the information provided in the containing element.
     *
     */
    @XmlEnumValue("via")
    VIA("via");
    private final String value;

    KnownRelCodeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static KnownRelCodeType fromValue(String v) {
        for (KnownRelCodeType c: KnownRelCodeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
