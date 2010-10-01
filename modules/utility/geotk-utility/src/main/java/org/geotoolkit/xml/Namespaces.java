/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2010, Geomatys
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
package org.geotoolkit.xml;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

import org.geotoolkit.lang.Static;


/**
 * List some namespaces URLs used by JAXB when (un)marshalling.
 *
 * @author Cédric Briançon (Geomatys)
 * @version 3.07
 *
 * @since 3.00
 * @module
 */
@Static
public final class Namespaces {
    /**
     * Do not allow instantiation of this class.
     */
    private Namespaces() {
    }

    /**
     * The <code>{@value}</code> URL.
     *
     * @category ISO
     */
    public static final String GCO = "http://www.isotc211.org/2005/gco";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category ISO
     * @since 3.02
     */
    public static final String GFC = "http://www.isotc211.org/2005/gfc";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category ISO
     */
    public static final String GMD = "http://www.isotc211.org/2005/gmd";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category ISO
     *
     * @since 3.07
     */
    public static final String GMI = "http://www.isotc211.org/2005/gmi";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category ISO
     */
    public static final String GMX = "http://www.isotc211.org/2005/gmx";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category OGC
     */
    public static final String GML = "http://www.opengis.net/gml";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category OGC
     * @since 3.02
     */
    public static final String CSW_202 = "http://www.opengis.net/cat/csw/2.0.2";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category W3C
     */
    public static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category W3C
     */
    public static final String XLINK = "http://www.w3.org/1999/xlink";

    /**
     * The <code>{@value}</code> URL.
     *
     * @category Profiles
     */
    public static final String FRA = "http://www.cnig.gouv.fr/2005/fra";

    /**
     * An empty array of strings.
     */
    static final String[] EMPTY = new String[0];

    /**
     * URLs for which the prefix to use directly follows them.
     */
    private static final String[] GENERIC_URLS = {
        "http://www.isotc211.org/2005/",
        "http://www.opengis.net/",
        "http://www.w3.org/1999/",
        "http://www.cnig.gouv.fr/2005/",
        "http://purl.org/"
    };

    /**
     * A map of (<var>urls</var>, <var>prefix</var>). Stores URLs for which
     * the prefix to use can not be easily inferred from the URL itself.
     */
    private static final Map<String,String> SPECIFIC_URLS;
    static {
        final Map<String,String> p = new HashMap<String,String>(40);
        p.put("http://www.w3.org/2001/XMLSchema",                         "xsd");
        p.put("http://www.w3.org/2001/XMLSchema-instance",                "xsi");
        p.put("http://www.w3.org/2004/02/skos/core#",                    "skos");
        p.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#",              "rdf");
        p.put("http://www.opengis.net/sensorML/1.0",                     "sml1");
        p.put("http://www.opengis.net/sensorML/1.0.1",                    "sml");
        p.put("http://www.opengis.net/swe/1.0",                          "swe1");
        p.put("http://www.opengis.net/cat/csw/2.0.2",                     "csw");
        p.put("http://www.opengis.net/cat/wrs/1.0",                       "wrs");
        p.put("http://www.opengis.net/cat/wrs",                         "wrs09");
        p.put("http://www.opengis.net/ows-6/utds/0.3",                   "utds");
        p.put("http://www.opengis.net/citygml/1.0",                      "core");
        p.put("http://www.opengis.net/citygml/building/1.0",            "build");
        p.put("http://www.opengis.net/citygml/cityfurniture/1.0",   "furniture");
        p.put("http://www.opengis.net/citygml/transportation/1.0",         "tr");
        p.put("http://www.purl.org/dc/elements/1.1/",                     "dc2");
        p.put("http://www.purl.org/dc/terms/",                           "dct2");
        p.put("http://purl.org/dc/terms/",                                "dct");
        p.put("http://www.inspire.org",                                   "ins");
        p.put("http://inspira.europa.eu/networkservice/view/1.0",  "inspire_vs");
        p.put("urn:oasis:names:tc:ciq:xsdschema:xAL:2.0",                 "xal");
        p.put("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0",              "rim");
        p.put("urn:oasis:names:tc:ebxml-regrep:rim:xsd:2.5",            "rim25");
        p.put("urn:oasis:names:tc:xacml:2.0:context:schema:os", "xacml-context");
        p.put("urn:oasis:names:tc:xacml:2.0:policy:schema:os",   "xacml-policy");
        p.put("urn:us:gov:ic:ism:v2",                                   "icism");
        SPECIFIC_URLS = p;
    }

    /**
     * Returns the preferred prefix for the given namespace URI, assuming that the namespace is
     * not {@code null}.
     *
     * @param  namespace  The namespace URI for which the prefix needs to be found.
     *                    Should not be {@code null}.
     * @param  suggestion The suggested prefix, returned if the given namespace is not recognized.
     * @return The prefix inferred from the namespace URI.
     */
    public static String getPreferredPrefix(String namespace, final String suggestion) {
        String prefix = SPECIFIC_URLS.get(namespace);
        if (prefix != null) {
            return prefix;
        }
        namespace = namespace.toLowerCase(Locale.US);
        for (final String baseURL : GENERIC_URLS) {
            if (namespace.startsWith(baseURL)) {
                final int startAt = baseURL.length();
                final int endAt = namespace.indexOf('/', startAt);
                if (endAt >= 0) {
                    prefix = namespace.substring(startAt, endAt);
                } else {
                    prefix = namespace.substring(startAt);
                }
                return prefix;
            }
        }
        return suggestion;
    }
}
