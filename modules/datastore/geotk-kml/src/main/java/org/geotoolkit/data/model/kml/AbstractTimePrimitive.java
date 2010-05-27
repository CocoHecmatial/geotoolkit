package org.geotoolkit.data.model.kml;

import java.util.List;
import org.geotoolkit.data.model.xsd.SimpleType;

/**
 * <p>This interface maps AbstractTimePrimitiveGroup element.</p>

 * <br />&lt;element name="AbstractTimePrimitiveGroup" type="kml:AbstractTimePrimitiveType" abstract="true" substitutionGroup="kml:AbstractObjectGroup"/>
 * <br />&lt;complexType name="AbstractTimePrimitiveType" abstract="true">
 * <br />&lt;complexContent>
 * <br />&lt;extension base="kml:AbstractObjectType">
 * <br />&lt;sequence>
 * <br />&lt;element ref="kml:AbstractTimePrimitiveSimpleExtensionGroup" minOccurs="0" maxOccurs="unbounded"/>
 * <br />&lt;element ref="kml:AbstractTimePrimitiveObjectExtensionGroup" minOccurs="0" maxOccurs="unbounded"/>
 * <br />&lt;/sequence>
 * <br />&lt;/extension>
 * <br />&lt;/complexContent>
 * <br />&lt;/complexType>
 * <br />&lt;element name="AbstractTimePrimitiveSimpleExtensionGroup" abstract="true" type="anySimpleType"/>
 * <br />&lt;element name="AbstractTimePrimitiveObjectExtensionGroup" abstract="true" substitutionGroup="kml:AbstractObjectGroup"/>
 *
 * @author Samuel Andrés
 */
public interface AbstractTimePrimitive extends AbstractObject {

    /**
     *
     * @return the list of AbstractTimePrimitive simple extensions.
     */
    public List<SimpleType> getAbstractTimePrimitiveSimpleExtensions();

    /**
     *
     * @return the list of AbtractTimePrimitive object extensions.
     */
    public List<AbstractObject> getAbstractTimePrimitiveObjectExtensions();

}
