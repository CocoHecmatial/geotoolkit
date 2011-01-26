/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008-2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2011, Geomatys
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

/**
 * {@linkplain org.geotoolkit.metadata.iso.DefaultMetadata Metadata} implementation. An explanation
 * for this package is provided in the {@linkplain org.opengis.metadata OpenGIS&reg; javadoc}.
 * The remaining discussion on this page is specific to the Geotk implementation.
 *
 * {@section Overview}
 * For a global overview of metadata in Geotk, see the
 * <a href="{@docRoot}/../modules/metadata/index.html">Metadata page on the project web site</a>.
 *
 * {@section Parameterized types}
 * In GeoAPI interfaces, most collections are typed with wildcards, for example
 * {@code Collection<? extends Citation>}. The Geotk implementation removes the
 * wildcards and declares {@code Collection<Citation>} instead. This allows collections
 * to be <cite>live</cite>. Consequently it is possible to add new elements directly in
 * an existing collection using code like {@code getCitations().add(myCitation)} instead
 * than setting the collection as a whole with {@code setCitations(myCitations)}.
 *
 * @author Martin Desruisseaux (IRD)
 * @author Touraïvane (IRD)
 * @author Cédric Briançon (Geomatys)
 * @version 3.07
 *
 * @since 2.1
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GMD, xmlns = {
    @XmlNs(prefix = "gmi", namespaceURI = Namespaces.GMI),
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(CI_OnlineResource.class),
    @XmlJavaTypeAdapter(CI_ResponsibleParty.class),
    @XmlJavaTypeAdapter(DQ_DataQuality.class),
    @XmlJavaTypeAdapter(MD_ApplicationSchemaInformation.class),
    @XmlJavaTypeAdapter(MD_CharacterSetCode.class),
    @XmlJavaTypeAdapter(MD_Constraints.class),
    @XmlJavaTypeAdapter(MD_ContentInformation.class),
    @XmlJavaTypeAdapter(MD_DatatypeCode.class),
    @XmlJavaTypeAdapter(MD_Distribution.class),
    @XmlJavaTypeAdapter(MD_ExtendedElementInformation.class),
    @XmlJavaTypeAdapter(MD_FeatureTypeList.class),
    @XmlJavaTypeAdapter(MD_Identification.class),
    @XmlJavaTypeAdapter(MD_MaintenanceInformation.class),
    @XmlJavaTypeAdapter(MD_MetadataExtensionInformation.class),
    @XmlJavaTypeAdapter(MD_ObligationCode.class),
    @XmlJavaTypeAdapter(MD_PortrayalCatalogueReference.class),
    @XmlJavaTypeAdapter(MD_ScopeCode.class),
    @XmlJavaTypeAdapter(MD_SpatialRepresentation.class),
    @XmlJavaTypeAdapter(MI_AcquisitionInformation.class),
    @XmlJavaTypeAdapter(RS_ReferenceSystem.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(DateAdapter.class),
    @XmlJavaTypeAdapter(LocaleAdapter.class),
    @XmlJavaTypeAdapter(AnchoredStringAdapter.class),
    @XmlJavaTypeAdapter(AnchoredInternationalStringAdapter.class),
    @XmlJavaTypeAdapter(BooleanAdapter.class), @XmlJavaTypeAdapter(type=boolean.class, value=BooleanAdapter.class),
    @XmlJavaTypeAdapter(IntegerAdapter.class), @XmlJavaTypeAdapter(type=int.class,     value=IntegerAdapter.class)
})
package org.geotoolkit.metadata.iso;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.geotoolkit.xml.Namespaces;
import org.geotoolkit.internal.jaxb.uom.*;
import org.geotoolkit.internal.jaxb.text.*;
import org.geotoolkit.internal.jaxb.code.*;
import org.geotoolkit.internal.jaxb.metadata.*;
import org.geotoolkit.internal.jaxb.primitive.*;
