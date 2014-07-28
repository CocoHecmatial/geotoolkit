/**
 * An explanation
 * for this package is provided in the {@linkplain org.opengis.service OpenGIS&reg; javadoc}.
 * The remaining discussion on this page is specific to the GeotoolKit implementation.
 */
@XmlSchema(elementFormDefault= XmlNsForm.QUALIFIED,
namespace="http://www.isotc211.org/2005/srv",
xmlns = {
    @XmlNs(prefix = "srv", namespaceURI = "http://www.isotc211.org/2005/srv"),
    @XmlNs(prefix = "gmd", namespaceURI = "http://www.isotc211.org/2005/gmd"),
    @XmlNs(prefix = "gco", namespaceURI = "http://www.isotc211.org/2005/gco"),
    @XmlNs(prefix = "xsi", namespaceURI = "http://www.w3.org/2001/XMLSchema-instance")
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    // ISO 19115 adapter (metadata module)
//  @XmlJavaTypeAdapter(ScopedNameAdapter.class), // TODO
    //@XmlJavaTypeAdapter(LocalNameAdapter.class),
    @XmlJavaTypeAdapter(GO_GenericName.class),
    @XmlJavaTypeAdapter(MD_Constraints.class),
    @XmlJavaTypeAdapter(MD_Keywords.class),
    @XmlJavaTypeAdapter(EX_Extent.class),
    @XmlJavaTypeAdapter(CI_OnlineResource.class),
    @XmlJavaTypeAdapter(MD_DataIdentification.class),
    @XmlJavaTypeAdapter(MD_StandardOrderProcess.class),
    @XmlJavaTypeAdapter(CI_ResponsibleParty.class),
    // ISO 19119 adapter
    @XmlJavaTypeAdapter(PortAdapter.class),
    @XmlJavaTypeAdapter(InterfaceAdapter.class),
    @XmlJavaTypeAdapter(org.geotoolkit.resources.jaxb.service.OperationAdapter.class),
    @XmlJavaTypeAdapter(SV_Parameter.class),
    @XmlJavaTypeAdapter(SV_OperationMetadata.class),
    @XmlJavaTypeAdapter(ServiceTypePropertyAdapter.class),
    @XmlJavaTypeAdapter(PlatformSpecificServiceSpecificationAdapter.class),
    @XmlJavaTypeAdapter(PlatformNeutralServiceSpecificationAdapter.class),
    @XmlJavaTypeAdapter(ServiceAdapter.class),
    @XmlJavaTypeAdapter(CoupledResourceAdapter.class),
    @XmlJavaTypeAdapter(ServiceProviderAdapter.class),
    @XmlJavaTypeAdapter(ServiceIdentificationAdapter.class),
    //CodeList handling
    @XmlJavaTypeAdapter(DCPList.class),
    @XmlJavaTypeAdapter(SV_CouplingType.class),
    // Primitive type handling
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(GO_Decimal.class),
    @XmlJavaTypeAdapter(type=double.class, value=GO_Decimal.class),
    @XmlJavaTypeAdapter(GO_Decimal32.class),
    @XmlJavaTypeAdapter(type=float.class, value=GO_Decimal32.class),
    @XmlJavaTypeAdapter(GO_Integer.class),
    @XmlJavaTypeAdapter(type=int.class, value=GO_Integer.class),
    @XmlJavaTypeAdapter(GO_Integer64.class),
    @XmlJavaTypeAdapter(type=long.class, value=GO_Integer64.class),
    @XmlJavaTypeAdapter(GO_Boolean.class),
    @XmlJavaTypeAdapter(type=boolean.class, value=GO_Boolean.class)
})
package org.geotoolkit.service;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.internal.jaxb.metadata.*;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.code.*;
import org.geotoolkit.resources.jaxb.service.*;

