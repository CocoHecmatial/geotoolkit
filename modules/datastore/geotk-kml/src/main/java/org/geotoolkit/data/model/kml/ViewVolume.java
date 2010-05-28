package org.geotoolkit.data.model.kml;

import java.util.List;
import org.geotoolkit.data.model.xsd.SimpleType;

/**
 * <p>This interface maps viewVolume element.</p>
 *
 * <br />&lt;element name="ViewVolume" type="kml:ViewVolumeType" substitutionGroup="kml:AbstractObjectGroup"/>
 * <br />&lt;complexType name="ViewVolumeType" final="#all">
 * <br />&lt;complexContent>
 * <br />&lt;extension base="kml:AbstractObjectType">
 * <br />&lt;sequence>
 * <br />&lt;element ref="kml:leftFov" minOccurs="0"/>
 * <br />&lt;element ref="kml:rightFov" minOccurs="0"/>
 * <br />&lt;element ref="kml:bottomFov" minOccurs="0"/>
 * <br />&lt;element ref="kml:topFov" minOccurs="0"/>
 * <br />&lt;element ref="kml:near" minOccurs="0"/>
 * <br />&lt;element ref="kml:ViewVolumeSimpleExtensionGroup" minOccurs="0" maxOccurs="unbounded"/>
 * <br />&lt;element ref="kml:ViewVolumeObjectExtensionGroup" minOccurs="0" maxOccurs="unbounded"/>
 * <br />&lt;/sequence>
 * <br />&lt;/extension>
 * <br />&lt;/complexContent>
 * <br />&lt;/complexType>
 * <br />&lt;element name="ViewVolumeSimpleExtensionGroup" abstract="true" type="anySimpleType"/>
 * <br />&lt;element name="ViewVolumeObjectExtensionGroup" abstract="true" substitutionGroup="kml:AbstractObjectGroup"/>
 *
 * @author Samuel Andrés
 */
public interface ViewVolume extends AbstractObject {

    /**
     *
     * @return
     */
    public Angle180 getLeftFov();

    /**
     *
     * @return
     */
    public Angle180 getRightFov();

    /**
     *
     * @return
     */
    public Angle90 getBottomFov();

    /**
     *
     * @return
     */
    public Angle90 getTopFov();

    /**
     *
     * @return
     */
    public double getNear();

    /**
     *
     * @return the list of ViewVolume simple extensions.
     */
    public List<SimpleType> getViewVolumeSimpleExtensions();

    /**
     *
     * @return the list of ViewVolume object extensions.
     */
    public List<AbstractObject> getViewVolumeObjectExtensions();

}
