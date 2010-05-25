package org.geotoolkit.data.kml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.geotoolkit.data.model.kml.AbstractColorStyle;
import org.geotoolkit.data.model.kml.AbstractContainer;
import org.geotoolkit.data.model.kml.AbstractFeature;
import org.geotoolkit.data.model.kml.AbstractGeometry;
import org.geotoolkit.data.model.kml.AbstractLatLonBox;
import org.geotoolkit.data.model.kml.AbstractObject;
import org.geotoolkit.data.model.kml.AbstractOverlay;
import org.geotoolkit.data.model.kml.AbstractStyleSelector;
import org.geotoolkit.data.model.kml.AbstractSubStyle;
import org.geotoolkit.data.model.kml.AbstractView;
import org.geotoolkit.data.model.kml.AltitudeMode;
import org.geotoolkit.data.model.kml.Angle180;
import org.geotoolkit.data.model.kml.Angle360;
import org.geotoolkit.data.model.kml.Angle90;
import org.geotoolkit.data.model.kml.Anglepos180;
import org.geotoolkit.data.model.kml.BalloonStyle;
import org.geotoolkit.data.model.kml.BasicLink;
import org.geotoolkit.data.model.kml.Boundary;
import org.geotoolkit.data.model.kml.Camera;
import org.geotoolkit.data.model.kml.Color;
import org.geotoolkit.data.model.kml.ColorMode;
import org.geotoolkit.data.model.kml.Coordinate;
import org.geotoolkit.data.model.kml.Coordinates;
import org.geotoolkit.data.model.kml.DisplayMode;
import org.geotoolkit.data.model.kml.Document;
import org.geotoolkit.data.model.kml.Folder;
import org.geotoolkit.data.model.kml.GroundOverlay;
import org.geotoolkit.data.model.kml.IconStyle;
import org.geotoolkit.data.model.kml.IdAttributes;
import org.geotoolkit.data.model.kml.ItemIcon;
import org.geotoolkit.data.model.kml.ItemIconState;
import org.geotoolkit.data.model.kml.Kml;
import org.geotoolkit.data.model.kml.LabelStyle;
import org.geotoolkit.data.model.kml.LatLonBox;
import org.geotoolkit.data.model.kml.LineString;
import org.geotoolkit.data.model.kml.LineStyle;
import org.geotoolkit.data.model.kml.LinearRing;
import org.geotoolkit.data.model.kml.Link;
import org.geotoolkit.data.model.kml.ListItem;
import org.geotoolkit.data.model.kml.ListStyle;
import org.geotoolkit.data.model.kml.Location;
import org.geotoolkit.data.model.kml.LookAt;
import org.geotoolkit.data.model.kml.Model;
import org.geotoolkit.data.model.kml.MultiGeometry;
import org.geotoolkit.data.model.kml.Orientation;
import org.geotoolkit.data.model.kml.Pair;
import org.geotoolkit.data.model.kml.PhotoOverlay;
import org.geotoolkit.data.model.kml.Placemark;
import org.geotoolkit.data.model.kml.Point;
import org.geotoolkit.data.model.kml.PolyStyle;
import org.geotoolkit.data.model.kml.Polygon;
import org.geotoolkit.data.model.kml.RefreshMode;
import org.geotoolkit.data.model.kml.ResourceMap;
import org.geotoolkit.data.model.kml.Scale;
import org.geotoolkit.data.model.kml.Schema;
import org.geotoolkit.data.model.kml.ScreenOverlay;
import org.geotoolkit.data.model.kml.Style;
import org.geotoolkit.data.model.kml.StyleMap;
import org.geotoolkit.data.model.kml.StyleState;
import org.geotoolkit.data.model.kml.Vec2;
import org.geotoolkit.data.model.kml.ViewRefreshMode;
import org.geotoolkit.data.model.xsd.SimpleType;
import org.geotoolkit.xml.StaxStreamWriter;
import static org.geotoolkit.data.model.ModelConstants.*;

/**
 *
 * @author Samuel Andrés
 */
public class KmlWriter extends StaxStreamWriter {

    //private XMLOutputFactory outputFactory;
    //private File file;

    public KmlWriter(File file){
        this.initSource(file);
    }

    public void initSource(Object o) {
        System.setProperty("javax.xml.stream.XMLOutputFactory", "com.ctc.wstx.stax.WstxOutputFactory");
        try {
            //this.outputFactory = XMLOutputFactory.newInstance();
            this.setOutput(o);
        } catch (IOException ex) {
            Logger.getLogger(KmlWriter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XMLStreamException ex) {
            Logger.getLogger(KmlWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void write(Kml kml) {
        try {

            // FACULTATIF : INDENTATION DE LA SORTIE
            //streamWriter = new IndentingXMLStreamWriter(streamWriter);

            writer.writeStartDocument("UTF-8", "1.0");

            writer.writeStartElement(TAG_KML);
            writer.setDefaultNamespace(URI_KML);
            writer.writeDefaultNamespace(URI_KML);
            /*streamWriter.writeNamespace(PREFIX_ATOM, URI_ATOM);
            streamWriter.writeNamespace(PREFIX_XAL, URI_XAL);
            streamWriter.writeNamespace(PREFIX_XSI, URI_XSI);
            streamWriter.writeAttribute(URI_XSI,
                    "schemaLocation",
                    URI_KML+" C:/Users/w7mainuser/Documents/OGC_SCHEMAS/sld/1.1.0/StyledLayerDescriptor.xsd");
            streamWriter.writeAttribute("version", "0");*/
            this.writeKml(kml);
            writer.writeEndElement();
            writer.writeEndDocument();

            writer.flush();
            writer.close();

        } catch (XMLStreamException ex) {
            Logger.getLogger(KmlWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void writeKml(Kml kml) throws XMLStreamException{
        if (kml.getNetworkLinkControl() != null){
        }
        if (kml.getAbstractFeature() != null){
            this.writeAbstractFeature(kml.getAbstractFeature());
        }
        if (kml.getKmlSimpleExtensions() != null){
        }
        if (kml.getKmlObjectExtensions() != null){
        }
    }

    private void writeCommonAbstractObject(AbstractObject abstractObject) throws XMLStreamException{
        if (abstractObject.getIdAttributes() != null){
            this.writeIdAttributes(abstractObject.getIdAttributes());
        }
        if (abstractObject.getObjectSimpleExtensions() != null){
            this.writeSimpleExtensions(abstractObject.getObjectSimpleExtensions());
        }
    }

    private void writeIdAttributes(IdAttributes idAttributes) throws XMLStreamException{
        if(idAttributes.getId() != null){
            writer.writeAttribute(ATT_ID, idAttributes.getId());
        }
        if(idAttributes.getTargetId() != null){
            writer.writeAttribute(ATT_TARGET_ID, idAttributes.getTargetId());
        }
    }

    private void writeAbstractFeature(AbstractFeature abstractFeature) throws XMLStreamException{
        if (abstractFeature instanceof AbstractContainer){
            this.writeAbstractContainer((AbstractContainer)abstractFeature);
//        } else if (abstractFeature instanceof NetworkLink){
//
        } else if (abstractFeature instanceof AbstractOverlay){
            this.writeAbstractOverlay((AbstractOverlay)abstractFeature);
        } else if (abstractFeature instanceof Placemark){
            this.writePlacemark((Placemark)abstractFeature);
        }
    }

    private void writeCommonAbstractFeature(AbstractFeature abstractFeature) throws XMLStreamException{
        this.writeCommonAbstractObject(abstractFeature);
        if (abstractFeature.getName() != null){
            this.writeName(abstractFeature.getName());
        }
        this.writeVisibility(abstractFeature.getVisibility());
        this.writeOpen(abstractFeature.getOpen());
//        if (abstractFeature.getAuthor() != null){
//            this.writeAuthor(abstractFeature.getAuthor(), streamWriter);
//        }
//        if (abstractFeature.getLink() != null){
//            this.writeLink(abstractFeature.getLink(), streamWriter);
//        }
        if (abstractFeature.getAddress() != null){
            this.writeAddress(abstractFeature.getAddress());
        }
//        if (abstractFeature.getAddressDetails() != null){
//            this.writeAddresDetails(abstractFeature.getAddressDetails(), streamWriter);
//        }
        if (abstractFeature.getPhoneNumber() != null){
            this.writePhoneNumber(abstractFeature.getPhoneNumber());
        }
        if (abstractFeature.getSnippet() != null){
            this.writeSnippet(abstractFeature.getSnippet());
        }
        if (abstractFeature.getDescription() != null){
            this.writeDescription(abstractFeature.getDescription());
        }
        if (abstractFeature.getView() != null){
            this.writeAbstractView(abstractFeature.getView());
        }
//        if (abstractFeature.getTimePrimitive() != null){
//            this.writeTimePrimitive(abstractFeature.getTimePrimitive(), streamWriter);
//        }
        if (abstractFeature.getStyleUrl() != null){
            this.writeStyleUrl(abstractFeature.getStyleUrl());
        }
        if (abstractFeature.getStyleSelectors() != null){
            for(AbstractStyleSelector abstractStyleSelector : abstractFeature.getStyleSelectors()){
                this.writeAbstractStyleSelector(abstractStyleSelector);
            }
        }
        if (abstractFeature.getAbstractFeatureSimpleExtensions() != null){
            this.writeSimpleExtensions(abstractFeature.getAbstractFeatureSimpleExtensions());
        }
        if (abstractFeature.getAbstractFeatureObjectExtensions() != null){
            this.writeObjectExtensions(abstractFeature.getAbstractFeatureObjectExtensions());
        }
    }

    private void writeAbstractView(AbstractView abstractView) throws XMLStreamException{
        if (abstractView instanceof LookAt){
                this.writeLookAt((LookAt)abstractView);
        } else if (abstractView instanceof Camera){
                this.writeCamera((Camera)abstractView);
        }
    }

    private void writeLookAt(LookAt lookAt) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_LOOK_AT);
        this.writeCommonAbstractView(lookAt);
        if (lookAt.getLongitude() != null){
            this.writeLongitude(lookAt.getLongitude());
        }
        if (lookAt.getLatitude() != null){
            this.writeLatitude(lookAt.getLatitude());
        }
        if (isFiniteNumber(lookAt.getAltitude())){
            this.writeAltitude(lookAt.getAltitude());
        }
        if (lookAt.getHeading() != null){
            this.writeHeading(lookAt.getHeading());
        }
        if (lookAt.getTilt() != null){
            this.writeTilt(lookAt.getTilt());
        }
        if (isFiniteNumber(lookAt.getRange())){
            this.writeRange(lookAt.getRange());
        }
        if (lookAt.getLookAtSimpleExtensions() != null){
            this.writeSimpleExtensions(lookAt.getLookAtSimpleExtensions());
        }
        if (lookAt.getLookAtObjectExtensions() != null){
            this.writeObjectExtensions(lookAt.getLookAtObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeCamera(Camera camera){

    }

    private void writeCommonAbstractView(AbstractView abstractView) throws XMLStreamException{
        this.writeCommonAbstractObject(abstractView);
        if (abstractView.getAbstractViewSimpleExtensions() != null){
            this.writeSimpleExtensions(abstractView.getAbstractViewSimpleExtensions());
        }
        if (abstractView.getAbstractViewObjectExtensions() != null){
            this.writeObjectExtensions(abstractView.getAbstractViewObjectExtensions());
        }
    }

    private void writeAbstractStyleSelector(AbstractStyleSelector abstractStyleSelector) throws XMLStreamException{
        if (abstractStyleSelector instanceof Style){
            this.writeStyle((Style)abstractStyleSelector);
        } else if (abstractStyleSelector instanceof StyleMap){
            this.writeStyleMap((StyleMap)abstractStyleSelector);
        }
    }

    private void writeCommonAbstractStyleSelector(AbstractStyleSelector abstractStyleSelector) throws XMLStreamException{
        this.writeCommonAbstractObject(abstractStyleSelector);
        if (abstractStyleSelector.getAbstractStyleSelectorSimpleExtensions() != null){
            this.writeSimpleExtensions(abstractStyleSelector.getAbstractStyleSelectorSimpleExtensions());
        }
        if (abstractStyleSelector.getAbstractStyleSelectorObjectExtensions() != null){
            this.writeObjectExtensions(abstractStyleSelector.getAbstractStyleSelectorObjectExtensions());
        }
    }

    private void writeStyleMap(StyleMap styleMap) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_STYLE_MAP);
        this.writeCommonAbstractStyleSelector(styleMap);
        if (styleMap.getPairs() != null){
            for(Pair pair : styleMap.getPairs()){
                this.writePair(pair);
            }
        }
        if (styleMap.getStyleMapSimpleExtensions() != null){
            this.writeSimpleExtensions(styleMap.getStyleMapSimpleExtensions());
        }
        if (styleMap.getStyleMapObjectExtensions() != null){
            this.writeObjectExtensions(styleMap.getStyleMapObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writePair(Pair pair) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_PAIR);
        this.writeCommonAbstractObject(pair);
        if (pair.getKey() != null){
            this.writeKey(pair.getKey());
        }
        if (pair.getStyleUrl() != null){
            this.writeStyleUrl(pair.getStyleUrl());
        }
        if (pair.getAbstractStyleSelector() != null){
            this.writeAbstractStyleSelector(pair.getAbstractStyleSelector());
        }
        if (pair.getPairSimpleExtensions() != null){
            this.writeSimpleExtensions(pair.getPairSimpleExtensions());
        }
        if (pair.getPairObjectExtensions() != null){
            this.writeObjectExtensions(pair.getPairObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeStyle(Style style) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_STYLE);
        this.writeCommonAbstractStyleSelector(style);
        if (style.getIconStyle() != null){
            this.writeIconStyle(style.getIconStyle());
        }
        if (style.getLabelStyle() != null){
            this.writeLabelStyle(style.getLabelStyle());
        }
        if (style.getLineStyle() != null){
            this.writeLineStyle(style.getLineStyle());
        }
        if (style.getPolyStyle() != null){
            this.writePolyStyle(style.getPolyStyle());
        }
        if (style.getBalloonStyle() != null){
            this.writeBalloonStyle(style.getBalloonStyle());
        }
        if (style.getListStyle() != null){
            this.writeListStyle(style.getListStyle());
        }
        if (style.getStyleSimpleExtensions() != null){
            this.writeSimpleExtensions(style.getStyleSimpleExtensions());
        }
        if (style.getStyleObjectExtensions() != null){
            this.writeObjectExtensions(style.getStyleObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeIconStyle(IconStyle iconStyle) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_ICON_STYLE);
        this.writeCommonAbstractColorStyle(iconStyle);
        if (isFiniteNumber(iconStyle.getScale())){
            this.writeScale(iconStyle.getScale());
        }
        if (iconStyle.getHeading() != null){
            this.writeHeading(iconStyle.getHeading());
        }
        if (iconStyle.getIcon() != null){
            this.writeIcon(iconStyle.getIcon());
        }
        if (iconStyle.getHotSpot() != null){
            this.writeHotSpot(iconStyle.getHotSpot());
        }
        if (iconStyle.getIconStyleSimpleExtensions() != null){
            this.writeSimpleExtensions(iconStyle.getIconStyleSimpleExtensions());
        }
        if (iconStyle.getIconStyleObjectExtensions() != null){
            this.writeObjectExtensions(iconStyle.getIconStyleObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeLabelStyle(LabelStyle labelStyle) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_LABEL_STYLE);
        this.writeCommonAbstractColorStyle(labelStyle);
        if (isFiniteNumber(labelStyle.getScale())){
            this.writeScale(labelStyle.getScale());
        }
        if (labelStyle.getLabelStyleSimpleExtensions() != null){
            this.writeSimpleExtensions(labelStyle.getLabelStyleSimpleExtensions());
        }
        if (labelStyle.getLabelStyleObjectExtensions() != null){
            this.writeObjectExtensions(labelStyle.getLabelStyleObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeLineStyle(LineStyle lineStyle) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_LINE_STYLE);
        this.writeCommonAbstractColorStyle(lineStyle);
        if (isFiniteNumber(lineStyle.getWidth())){
            this.writeWidth(lineStyle.getWidth());
        }
        if (lineStyle.getLineStyleSimpleExtensions() != null){
            this.writeSimpleExtensions(lineStyle.getLineStyleSimpleExtensions());
        }
        if (lineStyle.getLineStyleObjectExtensions() != null){
            this.writeObjectExtensions(lineStyle.getLineStyleObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writePolyStyle(PolyStyle polyStyle) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_POLY_STYLE);
        this.writeCommonAbstractColorStyle(polyStyle);
        this.writeFill(polyStyle.getFill());
        this.writeOutline(polyStyle.getOutline());
        if (polyStyle.getPolyStyleSimpleExtensions() != null){
            this.writeSimpleExtensions(polyStyle.getPolyStyleSimpleExtensions());
        }
        if (polyStyle.getPolyStyleObjectExtensions() != null){
            this.writeObjectExtensions(polyStyle.getPolyStyleObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeBalloonStyle(BalloonStyle balloonStyle) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_BALLOON_STYLE);
        this.writeCommonAbstractSubStyle(balloonStyle);
        if (balloonStyle.getBgColor() != null){
            this.writeBgColor(balloonStyle.getBgColor());
        }
        if (balloonStyle.getTextColor() != null){
            this.writeTextColor(balloonStyle.getTextColor());
        }
        if (balloonStyle.getText() != null){
            this.writeText(balloonStyle.getText());
        }
        if (balloonStyle.getDisplayMode() != null){
            this.writeDisplayMode(balloonStyle.getDisplayMode());
        }
        if (balloonStyle.getBalloonStyleSimpleExtensions() != null){
            this.writeSimpleExtensions(balloonStyle.getBalloonStyleSimpleExtensions());
        }
        if (balloonStyle.getBalloonStyleObjectExtensions() != null){
            this.writeObjectExtensions(balloonStyle.getBalloonStyleObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeListStyle(ListStyle listStyle) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_LIST_STYLE);
        this.writeCommonAbstractSubStyle(listStyle);
        if (listStyle.getListItem() != null){
            this.writeListItem(listStyle.getListItem());
        }
        if (listStyle.getBgColor() != null){
            this.writeBgColor(listStyle.getBgColor());
        }
        if (listStyle.getItemIcons() != null){
            for(ItemIcon itemIcon : listStyle.getItemIcons()){
                this.writeItemIcon(itemIcon);
            }
        }
        if (isFiniteNumber(listStyle.getMaxSnippetLines())){
            this.writeMaxSnippetLines(listStyle.getMaxSnippetLines());
        }
        if (listStyle.getListStyleSimpleExtensions() != null){
            this.writeSimpleExtensions(listStyle.getListStyleSimpleExtensions());
        }
        if (listStyle.getListStyleObjectExtensions() != null){
            this.writeObjectExtensions(listStyle.getListStyleObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeItemIcon(ItemIcon itemIcon) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_ITEM_ICON);
        this.writeCommonAbstractObject(itemIcon);
        if (itemIcon.getStates() != null){
            this.writeStates(itemIcon.getStates());
        }
        if (itemIcon.getHref() != null){
            this.writeHref(itemIcon.getHref());
        }
        if (itemIcon.getItemIconSimpleExtensions() != null){
            this.writeSimpleExtensions(itemIcon.getItemIconSimpleExtensions());
        }
        if (itemIcon.getItemIconObjectExtensions() != null){
            this.writeObjectExtensions(itemIcon.getItemIconObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeStates(List<ItemIconState> itemIconStates) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_STATE);
        int i = 0;
        int size = itemIconStates.size();
        for(ItemIconState itemIconState : itemIconStates){
            i++;
            if(i == size){
                writer.writeCharacters(itemIconState.getItemIconState());
            } else {
                writer.writeCharacters(itemIconState.getItemIconState()+" ");
            }
        }
        writer.writeEndElement();
    }

    private void writeIcon(BasicLink icon) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_ICON);
        if (icon.getIdAttributes() != null){
            this.writeIdAttributes(icon.getIdAttributes());
        }
        if (icon.getObjectSimpleExtensions() != null){
            this.writeSimpleExtensions(icon.getObjectSimpleExtensions());
        }
        if (icon.getHref() != null){
            this.writeHref(icon.getHref());
        }
        if (icon.getBasicLinkSimpleExtensions() != null){
            this.writeSimpleExtensions(icon.getBasicLinkSimpleExtensions());
        }
        if (icon.getBasicLinkObjectExtensions() != null){
            this.writeObjectExtensions(icon.getBasicLinkObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeIcon(Link icon) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_ICON);
        this.writeLink_structure(icon);
        writer.writeEndElement();
    }

    private void writeLink(Link link) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_LINK);
        this.writeLink_structure(link);
        writer.writeEndElement();
    }

    private void writeLink_structure(Link link) throws XMLStreamException{
        this.writeCommonAbstractObject(link);
        if (link.getHref() != null){
            this.writeHref(link.getHref());
        }
        if (link.getBasicLinkSimpleExtensions() != null){
            this.writeSimpleExtensions(link.getBasicLinkSimpleExtensions());
        }
        if (link.getBasicLinkObjectExtensions() != null){
            this.writeObjectExtensions(link.getBasicLinkObjectExtensions());
        }
        if (link.getRefreshMode() != null){
            this.writeRefreshMode(link.getRefreshMode());
        }
        if (isFiniteNumber(link.getRefreshInterval())){
            this.writeRefreshInterval(link.getRefreshInterval());
        }
        if (link.getViewRefreshMode() != null){
            this.writeViewRefreshMode(link.getViewRefreshMode());
        }
        if (isFiniteNumber(link.getViewRefreshTime())){
            this.writeViewRefreshTime(link.getViewRefreshTime());
        }
        if (isFiniteNumber(link.getViewBoundScale())){
            this.writeViewBoundScale(link.getViewBoundScale());
        }
        if (link.getViewFormat() != null){
            this.writeViewFormat(link.getViewFormat());
        }
        if (link.getHttpQuery() != null){
            this.writeHttpQuery(link.getHttpQuery());
        }
        if (link.getLinkSimpleExtensions() != null){
            this.writeSimpleExtensions(link.getLinkSimpleExtensions());
        }
        if (link.getLinkObjectExtensions() != null){
            this.writeObjectExtensions(link.getLinkObjectExtensions());
        }
    }

    private void writeCommonAbstractColorStyle(AbstractColorStyle abstractColorStyle) throws XMLStreamException{
        this.writeCommonAbstractSubStyle(abstractColorStyle);
        if (abstractColorStyle.getColor() != null){
            this.writeColor(abstractColorStyle.getColor());
        }
        if (abstractColorStyle.getColorMode() != null){
            this.writeColorMode(abstractColorStyle.getColorMode());
        }
        if (abstractColorStyle.getColorStyleSimpleExtensions() != null){
            this.writeSimpleExtensions(abstractColorStyle.getColorStyleSimpleExtensions());
        }
        if (abstractColorStyle.getColorStyleObjectExtensions() != null){
            this.writeObjectExtensions(abstractColorStyle.getColorStyleObjectExtensions());
        }
    }

    private void writeCommonAbstractSubStyle(AbstractSubStyle abstractSubStyle) throws XMLStreamException{
        this.writeCommonAbstractObject(abstractSubStyle);
        if (abstractSubStyle.getSubStyleSimpleExtensions() != null){
            this.writeSimpleExtensions(abstractSubStyle.getSubStyleSimpleExtensions());
        }
        if (abstractSubStyle.getSubStyleObjectExtensions() != null){
            this.writeObjectExtensions(abstractSubStyle.getSubStyleObjectExtensions());
        }
    }

    private void writePlacemark(Placemark placemark) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_PLACEMARK);
        this.writeCommonAbstractFeature(placemark);
        if (placemark.getAbstractGeometry() != null){
            this.writeAbstractGeometry(placemark.getAbstractGeometry());
        }
        if (placemark.getPlacemarkSimpleExtensions() != null){
            this.writeSimpleExtensions(placemark.getPlacemarkSimpleExtensions());
        }
        if (placemark.getPlacemarkObjectExtensions() != null){
            this.writeObjectExtensions(placemark.getPlacemarkObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeAbstractContainer(AbstractContainer abstractContainer) throws XMLStreamException{
        if (abstractContainer instanceof Folder){
            this.writeFolder((Folder)abstractContainer);
        } else if (abstractContainer instanceof Document){
            this.writeDocument((Document)abstractContainer);
        }
    }
    
    private void writeAbstractOverlay(AbstractOverlay abstractOverlay) throws XMLStreamException{
        if (abstractOverlay instanceof GroundOverlay){
            this.writeGroundOverlay((GroundOverlay)abstractOverlay);
        } else if (abstractOverlay instanceof ScreenOverlay){
            this.writeScreenOverlay((ScreenOverlay)abstractOverlay);
        } else if (abstractOverlay instanceof PhotoOverlay){
            //this.writePhotoOverlay((PhotoOverlay)abstractOverlay);
        }
    }

    private void writeScreenOverlay(ScreenOverlay screenOverlay) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_SCREEN_OVERLAY);
        this.writeCommonAbstractOverlay(screenOverlay);
        if (screenOverlay.getOverlayXY() != null){
            this.writeOverlayXY(screenOverlay.getOverlayXY());
        }
        if (screenOverlay.getScreenXY() != null){
            this.writeScreenXY(screenOverlay.getScreenXY());
        }
        if (screenOverlay.getRotationXY() != null){
            this.writeRotationXY(screenOverlay.getRotationXY());
        }
        if (screenOverlay.getSize() != null){
            this.writeSize(screenOverlay.getSize());
        }
        if (screenOverlay.getRotation() != null){
            this.writeRotation(screenOverlay.getRotation());
        }
        if (screenOverlay.getScreenOverlaySimpleExtensions() != null){
            this.writeSimpleExtensions(screenOverlay.getScreenOverlaySimpleExtensions());
        }
        if (screenOverlay.getScreenOverlayObjectExtensions() != null){
            this.writeObjectExtensions(screenOverlay.getScreenOverlayObjectExtensions());
        }
        writer.writeEndElement();
    }
    
    private void writeGroundOverlay(GroundOverlay groundOverlay) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_GROUND_OVERLAY);
        this.writeCommonAbstractOverlay(groundOverlay);
        if (isFiniteNumber(groundOverlay.getAltitude())){
            this.writeAltitude(groundOverlay.getAltitude());
        }
        if (groundOverlay.getAltitudeMode() != null){
            this.writeAltitudeMode(groundOverlay.getAltitudeMode());
        }
        if (groundOverlay.getLatLonBox() != null){
            this.writeLatLonBox(groundOverlay.getLatLonBox());
        }
        if (groundOverlay.getGroundOverlaySimpleExtensions() != null){
            this.writeSimpleExtensions(groundOverlay.getGroundOverlaySimpleExtensions());
        }
        if (groundOverlay.getGroundOverlayObjectExtensions() != null){
            this.writeObjectExtensions(groundOverlay.getGroundOverlayObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeLatLonBox(LatLonBox latLonBox) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_LAT_LON_BOX);
        this.writeCommonAbstractLatLonBox(latLonBox);
        if (latLonBox.getRotation() != null){
            this.writeRotation(latLonBox.getRotation());
        }
        if (latLonBox.getLatLonBoxSimpleExtensions() != null){
            this.writeSimpleExtensions(latLonBox.getLatLonBoxSimpleExtensions());
        }
        if (latLonBox.getLatLonBoxObjectExtensions() != null){
            this.writeObjectExtensions(latLonBox.getLatLonBoxObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeCommonAbstractLatLonBox(AbstractLatLonBox abstractLatLonBox) throws XMLStreamException{
        this.writeCommonAbstractObject(abstractLatLonBox);
        if (abstractLatLonBox.getNorth() != null){
            this.writeNorth(abstractLatLonBox.getNorth());
        }
        if (abstractLatLonBox.getSouth() != null){
            this.writeSouth(abstractLatLonBox.getSouth());
        }
        if (abstractLatLonBox.getEast() != null){
            this.writeEast(abstractLatLonBox.getEast());
        }
        if (abstractLatLonBox.getWest() != null){
            this.writeWest(abstractLatLonBox.getWest());
        }
        if (abstractLatLonBox.getAbstractLatLonBoxSimpleExtensions() != null){
            this.writeSimpleExtensions(abstractLatLonBox.getAbstractLatLonBoxSimpleExtensions());
        }
        if (abstractLatLonBox.getAbstractLatLonBoxObjectExtensions() != null){
            this.writeObjectExtensions(abstractLatLonBox.getAbstractLatLonBoxObjectExtensions());
        }
    }
    
    private void writeCommonAbstractOverlay(AbstractOverlay abstractOverlay) throws XMLStreamException{
        this.writeCommonAbstractFeature(abstractOverlay);
        if (abstractOverlay.getColor() != null){
            this.writeColor(abstractOverlay.getColor());
        }
        if (isFiniteNumber(abstractOverlay.getDrawOrder())){
            this.writeDrawOrder(abstractOverlay.getDrawOrder());
        }
        if (abstractOverlay.getIcon() != null){
            this.writeIcon(abstractOverlay.getIcon());
        }
        if (abstractOverlay.getAbstractOverlaySimpleExtensions() != null){
            this.writeSimpleExtensions(abstractOverlay.getAbstractOverlaySimpleExtensions());
        }
        if (abstractOverlay.getAbstractOverlayObjectExtensions() != null){
            this.writeObjectExtensions(abstractOverlay.getAbstractOverlayObjectExtensions());
        }
    }

    private void writeCommonAbstractContainer(AbstractContainer abstractContainer) throws XMLStreamException{
        this.writeCommonAbstractFeature(abstractContainer);
        if (abstractContainer.getAbstractContainerSimpleExtensions() != null){
            this.writeSimpleExtensions(abstractContainer.getAbstractContainerSimpleExtensions());
        }
        if (abstractContainer.getAbstractContainerObjectExtensions() != null){
            this.writeObjectExtensions(abstractContainer.getAbstractContainerObjectExtensions());
        }
    }

    /**
     * Writes a folder element
     * @param folder The element to write
     * @throws XMLStreamException
     */
    private void writeFolder(Folder folder) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_FOLDER);
        this.writeCommonAbstractContainer(folder);

        if (folder.getAbstractFeatures() != null){
            for(AbstractFeature abstractFeature : folder.getAbstractFeatures()){
                this.writeAbstractFeature(abstractFeature);
            }
        }
        if (folder.getFolderSimpleExtensions() != null){
            this.writeSimpleExtensions(folder.getFolderSimpleExtensions());
        }
        if (folder.getFolderObjectExtensions() != null){
            this.writeObjectExtensions(folder.getFolderObjectExtensions());
        }
        writer.writeEndElement();
    }

    /**
     * Writes a document element
     * @param document The element to write
     * @throws XMLStreamException
     */
    private void writeDocument(Document document) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_DOCUMENT);
        this.writeCommonAbstractContainer(document);

        if (document.getSchemas() != null){
            for(Schema schema : document.getSchemas()){
                this.writeSchema(schema);
            }
        }
        if (document.getAbstractFeatures() != null){
            for(AbstractFeature abstractFeature : document.getAbstractFeatures()){
                this.writeAbstractFeature(abstractFeature);
            }
        }
        if (document.getDocumentSimpleExtensions() != null){
            this.writeSimpleExtensions(document.getDocumentSimpleExtensions());
        }
        if (document.getDocumentObjectExtensions() != null){
            this.writeObjectExtensions(document.getDocumentObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeSchema(Schema schema){
        
    }

    private void writeAbstractGeometry(AbstractGeometry abstractGeometry) throws XMLStreamException{
        if (abstractGeometry instanceof MultiGeometry){
            this.writeMultiGeometry((MultiGeometry) abstractGeometry);
        } else if (abstractGeometry instanceof LineString){
            this.writeLineString((LineString) abstractGeometry);
        } else if (abstractGeometry instanceof Polygon){
            this.writePolygon((Polygon) abstractGeometry);
        } else if (abstractGeometry instanceof Point){
            this.writePoint((Point) abstractGeometry);
        } else if (abstractGeometry instanceof LinearRing){
            this.writeLinearRing((LinearRing) abstractGeometry);
        } else if (abstractGeometry instanceof Model){
            this.writeModel((Model) abstractGeometry);
        }
    }

    private void writeCommonAbstractGeometry(AbstractGeometry abstractGeometry) throws XMLStreamException{
        this.writeCommonAbstractObject(abstractGeometry);
        if (abstractGeometry.getAbstractGeometrySimpleExtensions() != null){
            this.writeSimpleExtensions(abstractGeometry.getAbstractGeometrySimpleExtensions());
        }
        if (abstractGeometry.getAbstractGeometryObjectExtensions() != null){
            this.writeObjectExtensions(abstractGeometry.getAbstractGeometryObjectExtensions());
        }
    }

    private void writeModel(Model model) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_MODEL);
        this.writeCommonAbstractGeometry(model);
        if (model.getAltitudeMode() != null){
            this.writeAltitudeMode(model.getAltitudeMode());
        }
        if (model.getLocation() != null){
            this.writeLocation(model.getLocation());
        }
        if (model.getOrientation() != null){
            this.writeOrientation(model.getOrientation());
        }
        if (model.getScale() != null){
            this.writeScale(model.getScale());
        }
        if (model.getLink() != null){
            this.writeLink(model.getLink());
        }
        if (model.getRessourceMap() != null){
            this.writeResourceMap(model.getRessourceMap());
        }
        if (model.getModelSimpleExtensions() != null){
            this.writeSimpleExtensions(model.getModelSimpleExtensions());
        }
        if (model.getModelObjectExtensions() != null){
            this.writeObjectExtensions(model.getModelObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeLocation(Location location){

    }

    private void writeOrientation(Orientation orientation){

    }

    private void writeScale(Scale scale){

    }

    private void writeResourceMap(ResourceMap resourceMap){

    }

    private void writePolygon(Polygon polygon) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_POLYGON);
        this.writeCommonAbstractGeometry(polygon);
        this.writeExtrude(polygon.getExtrude());
        this.writeTessellate(polygon.getTessellate());
        if (polygon.getAltitudeMode() != null){
            this.writeAltitudeMode(polygon.getAltitudeMode());
        }
        if (polygon.getOuterBoundaryIs() != null){
            this.writeOuterBoundaryIs(polygon.getOuterBoundaryIs());
        }
        if (polygon.getInnerBoundariesAre() != null){
            for(Boundary innerBoundaryIs : polygon.getInnerBoundariesAre()){
                this.writeInnerBoundaryIs(innerBoundaryIs);
            }
        }
        if (polygon.getPolygonSimpleExtensions() != null){
            this.writeSimpleExtensions(polygon.getPolygonSimpleExtensions());
        }
        if (polygon.getPolygonObjectExtensions() != null){
            this.writeObjectExtensions(polygon.getPolygonObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeOuterBoundaryIs(Boundary boundary) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_OUTER_BOUNDARY_IS);
        this.writeBoundary(boundary);
        writer.writeEndElement();
    }
    
    private void writeInnerBoundaryIs(Boundary boundary) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_INNER_BOUNDARY_IS);
        this.writeBoundary(boundary);
        writer.writeEndElement();
    }

    private void writeBoundary(Boundary boundary) throws XMLStreamException{
        if (boundary.getLinearRing() != null){
            this.writeLinearRing(boundary.getLinearRing());
        }
        if (boundary.getBoundarySimpleExtensions() != null){
            this.writeSimpleExtensions(boundary.getBoundarySimpleExtensions());
        }
        if (boundary.getBoundaryObjectExtensions() != null){
            this.writeObjectExtensions(boundary.getBoundaryObjectExtensions());
        }
    }

    private void writeLineString(LineString lineString) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_LINE_STRING);
        this.writeCommonAbstractGeometry(lineString);
        this.writeExtrude(lineString.getExtrude());
        this.writeTessellate(lineString.getTessellate());
        if (lineString.getAltitudeMode() != null){
            this.writeAltitudeMode(lineString.getAltitudeMode());
        }
        if (lineString.getCoordinates() != null){
            this.writeCoordinates(lineString.getCoordinates());
        }
        if (lineString.getLineStringSimpleExtensions() != null){
            this.writeSimpleExtensions(lineString.getLineStringSimpleExtensions());
        }
        if (lineString.getLineStringObjectExtensions() != null){
            this.writeObjectExtensions(lineString.getLineStringObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeLinearRing(LinearRing linearRing) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_LINEAR_RING);
        this.writeCommonAbstractGeometry(linearRing);
        this.writeExtrude(linearRing.getExtrude());
        this.writeTessellate(linearRing.getTessellate());
        if (linearRing.getAltitudeMode() != null){
            this.writeAltitudeMode(linearRing.getAltitudeMode());
        }
        if (linearRing.getCoordinates() != null){
            this.writeCoordinates(linearRing.getCoordinates());
        }
        if (linearRing.getLinearRingSimpleExtensions() != null){
            this.writeSimpleExtensions(linearRing.getLinearRingSimpleExtensions());
        }
        if (linearRing.getLinearRingObjectExtensions() != null){
            this.writeObjectExtensions(linearRing.getLinearRingObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeMultiGeometry(MultiGeometry multiGeometry) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_MULTI_GEOMETRY);
        this.writeCommonAbstractGeometry(multiGeometry);
        if (multiGeometry.getGeometries() != null){
            for (AbstractGeometry abstractGeometry : multiGeometry.getGeometries()){
                this.writeAbstractGeometry(abstractGeometry);
            }
        }
        if (multiGeometry.getMultiGeometrySimpleExtensions() != null){
            this.writeSimpleExtensions(multiGeometry.getMultiGeometrySimpleExtensions());
        }
        if (multiGeometry.getMultiGeometryObjectExtensions() != null){
            this.writeObjectExtensions(multiGeometry.getMultiGeometryObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writePoint(Point point) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_POINT);
        this.writeCommonAbstractGeometry(point);
        this.writeExtrude(point.getExtrude());
        if (point.getAltitudeMode() != null){
            this.writeAltitudeMode(point.getAltitudeMode());
        }
        if (point.getCoordinates() != null){
            this.writeCoordinates(point.getCoordinates());
        }
        if (point.getPointSimpleExtensions() != null){
            this.writeSimpleExtensions(point.getPointSimpleExtensions());
        }
        if (point.getPointObjectExtensions() != null){
            this.writeObjectExtensions(point.getPointObjectExtensions());
        }
        writer.writeEndElement();
    }

    private void writeCoordinates(Coordinates coordinates) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_COORDINATES);
        writer.writeCharacters(coordinates.getCoordinatesString());
        writer.writeEndElement();
    }

    private void writeExtrude(boolean extrude) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_EXTRUDE);
        if(extrude){
            writer.writeCharacters(SimpleType.BOOLEAN_TRUE);
        } else {
            writer.writeCharacters(SimpleType.BOOLEAN_FALSE);
        }
        writer.writeEndElement();
    }

    private void writeVisibility(boolean visibility) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_VISIBILITY);
        if(visibility){
            writer.writeCharacters(SimpleType.BOOLEAN_TRUE);
        } else {
            writer.writeCharacters(SimpleType.BOOLEAN_FALSE);
        }
        writer.writeEndElement();
    }

    private void writeOpen(boolean open) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_OPEN);
        if(open){
            writer.writeCharacters(SimpleType.BOOLEAN_TRUE);
        } else {
            writer.writeCharacters(SimpleType.BOOLEAN_FALSE);
        }
        writer.writeEndElement();
    }

    private void writeFill(boolean fill) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_FILL);
        if(fill){
            writer.writeCharacters(SimpleType.BOOLEAN_TRUE);
        } else {
            writer.writeCharacters(SimpleType.BOOLEAN_FALSE);
        }
        writer.writeEndElement();
    }

    private void writeOutline(boolean outline) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_OUTLINE);
        if(outline){
            writer.writeCharacters(SimpleType.BOOLEAN_TRUE);
        } else {
            writer.writeCharacters(SimpleType.BOOLEAN_FALSE);
        }
        writer.writeEndElement();
    }

    private void writeTessellate(boolean tessellate) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_TESSELLATE);
        if(tessellate){
            writer.writeCharacters(SimpleType.BOOLEAN_TRUE);
        } else {
            writer.writeCharacters(SimpleType.BOOLEAN_FALSE);
        }
        writer.writeEndElement();
    }

    private void writeAddress(String address) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_ADDRESS);
        writer.writeCharacters(address);
        writer.writeEndElement();
    }

    private void writeSnippet(String snippet) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_SNIPPET);
        writer.writeCharacters(snippet);
        writer.writeEndElement();
    }

    private void writePhoneNumber(String phoneNumber) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_PHONE_NUMBER);
        writer.writeCharacters(phoneNumber);
        writer.writeEndElement();
    }

    private void writeName(String name) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_NAME);
        writer.writeCharacters(name);
        writer.writeEndElement();
    }

    private void writeDescription(String description) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_DESCRIPTION);
        writer.writeCharacters(description);
        writer.writeEndElement();
    }

    private void writeHref(String href) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_HREF);
        writer.writeCharacters(href);
        writer.writeEndElement();
    }

    private void writeText(String text) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_TEXT);
        writer.writeCharacters(text);
        writer.writeEndElement();
    }

    private void writeStyleUrl(String text) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_STYLE_URL);
        writer.writeCharacters(text);
        writer.writeEndElement();
    }

    private void writeViewFormat(String viewFormat) throws XMLStreamException {
        writer.writeStartElement(URI_KML, TAG_VIEW_FORMAT);
        writer.writeCharacters(viewFormat);
        writer.writeEndElement();
    }

    private void writeHttpQuery(String httpQuery) throws XMLStreamException {
        writer.writeStartElement(URI_KML, TAG_HTTP_QUERY);
        writer.writeCharacters(httpQuery);
        writer.writeEndElement();
    }

    private void writeColor(Color color) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_COLOR);
        writer.writeCharacters(color.getColor());
        writer.writeEndElement();
    }

    private void writeBgColor(Color color) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_BG_COLOR);
        writer.writeCharacters(color.getColor());
        writer.writeEndElement();
    }

    private void writeTextColor(Color color) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_TEXT_COLOR);
        writer.writeCharacters(color.getColor());
        writer.writeEndElement();
    }

    private void writeColorMode(ColorMode colorMode) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_COLOR_MODE);
        writer.writeCharacters(colorMode.getColorMode());
        writer.writeEndElement();
    }

    private void writeAltitudeMode(AltitudeMode altitudeMode) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_ALTITUDE_MODE);
        writer.writeCharacters(altitudeMode.getAltitudeMode());
        writer.writeEndElement();
    }

    private void writeDisplayMode(DisplayMode displayMode) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_ALTITUDE_MODE);
        writer.writeCharacters(displayMode.getDisplayMode());
        writer.writeEndElement();
    }

    private void writeKey(StyleState styleState) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_KEY);
        writer.writeCharacters(styleState.getStyleState());
        writer.writeEndElement();
    }

    private void writeRefreshMode(RefreshMode refreshMode) throws XMLStreamException {
        writer.writeStartElement(URI_KML, TAG_REFRESH_MODE);
        writer.writeCharacters(refreshMode.getRefreshMode());
        writer.writeEndElement();
    }

    private void writeViewRefreshMode(ViewRefreshMode viewRefreshMode) throws XMLStreamException {
        writer.writeStartElement(URI_KML, TAG_VIEW_REFRESH_MODE);
        writer.writeCharacters(viewRefreshMode.getViewRefreshMode());
        writer.writeEndElement();
    }

    private void writeListItem(ListItem listItem) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_LIST_ITEM);
        writer.writeCharacters(listItem.getItem());
        writer.writeEndElement();
    }

    private void writeScale(double scale) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_SCALE);
        writer.writeCharacters(Double.toString(scale));
        writer.writeEndElement();
    }

    private void writeWidth(double width) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_WIDTH);
        writer.writeCharacters(Double.toString(width));
        writer.writeEndElement();
    }

    private void writeAltitude(double altitude) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_ALTITUDE);
        writer.writeCharacters(Double.toString(altitude));
        writer.writeEndElement();
    }

    private void writeRange(double range) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_RANGE);
        writer.writeCharacters(Double.toString(range));
        writer.writeEndElement();
    }

    private void writeDrawOrder(double drawOrder) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_DRAW_ORDER);
        writer.writeCharacters(Double.toString(drawOrder));
        writer.writeEndElement();
    }

    private void writeRefreshInterval(double refreshInterval) throws XMLStreamException {
        writer.writeStartElement(URI_KML, TAG_REFRESH_INTERVAL);
        writer.writeCharacters(Double.toString(refreshInterval));
        writer.writeEndElement();
    }

    private void writeViewRefreshTime(double viewRefreshTime) throws XMLStreamException {
        writer.writeStartElement(URI_KML, TAG_VIEW_REFRESH_TIME);
        writer.writeCharacters(Double.toString(viewRefreshTime));
        writer.writeEndElement();
    }

    private void writeViewBoundScale(double viewBoundScale) throws XMLStreamException {
        writer.writeStartElement(URI_KML, TAG_VIEW_BOUND_SCALE);
        writer.writeCharacters(Double.toString(viewBoundScale));
        writer.writeEndElement();
    }

    private void writeMaxSnippetLines(int msl) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_MAX_SNIPPET_LINES);
        writer.writeCharacters(Integer.toString(msl));
        writer.writeEndElement();
    }

    private void writeHeading(Angle360 heading) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_HEADING);
        writer.writeCharacters(Double.toString(heading.getAngle()));
        writer.writeEndElement();
    }

    private void writeLongitude(Angle180 longitude) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_LONGITUDE);
        writer.writeCharacters(Double.toString(longitude.getAngle()));
        writer.writeEndElement();
    }

    private void writeLatitude(Angle90 latitude) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_LATITUDE);
        writer.writeCharacters(Double.toString(latitude.getAngle()));
        writer.writeEndElement();
    }

    private void writeTilt(Anglepos180 tilt) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_TILT);
        writer.writeCharacters(Double.toString(tilt.getAngle()));
        writer.writeEndElement();
    }

    private void writeRotation(Angle180 rotation) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_ROTATION);
        writer.writeCharacters(Double.toString(rotation.getAngle()));
        writer.writeEndElement();
    }

    private void writeNorth(Angle180 north) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_NORTH);
        writer.writeCharacters(Double.toString(north.getAngle()));
        writer.writeEndElement();
    }

    private void writeSouth(Angle180 south) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_SOUTH);
        writer.writeCharacters(Double.toString(south.getAngle()));
        writer.writeEndElement();
    }

    private void writeEast(Angle180 east) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_EAST);
        writer.writeCharacters(Double.toString(east.getAngle()));
        writer.writeEndElement();
    }

    private void writeWest(Angle180 west) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_WEST);
        writer.writeCharacters(Double.toString(west.getAngle()));
        writer.writeEndElement();
    }

    private void writeVec2(Vec2 vec2) throws XMLStreamException{
        if (isFiniteNumber(vec2.getX())){
            writer.writeAttribute(ATT_X, Double.toString(vec2.getX()));
        }
        if (isFiniteNumber(vec2.getY())){
            writer.writeAttribute(ATT_Y, Double.toString(vec2.getY()));
        }
        if (vec2.getXUnits() != null){
            writer.writeAttribute(ATT_XUNITS, vec2.getXUnits().getUnit());
        }
        if (vec2.getYUnits() != null){
            writer.writeAttribute(ATT_YUNITS, vec2.getYUnits().getUnit());
        }
    }

    private void writeHotSpot(Vec2 hotspot) throws XMLStreamException{
        writer.writeStartElement(URI_KML, TAG_HOT_SPOT);
        this.writeVec2(hotspot);
        writer.writeEndElement();
    }


    private void writeOverlayXY(Vec2 overlayXY) throws XMLStreamException {
        writer.writeStartElement(URI_KML, TAG_OVERLAY_XY);
        this.writeVec2(overlayXY);
        writer.writeEndElement();
    }

    private void writeScreenXY(Vec2 screenXY) throws XMLStreamException {
        writer.writeStartElement(URI_KML, TAG_SCREEN_XY);
        this.writeVec2(screenXY);
        writer.writeEndElement();
    }

    private void writeRotationXY(Vec2 rotationXY) throws XMLStreamException {
        writer.writeStartElement(URI_KML, TAG_ROTATION_XY);
        this.writeVec2(rotationXY);
        writer.writeEndElement();
    }

    private void writeSize(Vec2 size) throws XMLStreamException {
        writer.writeStartElement(URI_KML, TAG_SIZE);
        this.writeVec2(size);
        writer.writeEndElement();
    }

    private void writeSimpleExtensions(List<SimpleType> simpleExtensions){

    }

    private void writeObjectExtensions(List<AbstractObject> objectExtensions){

    }

    /*
     * METHODES UTILITAIRES
     */
    private static boolean isFiniteNumber(double d){
        return !(Double.isInfinite(d) && Double.isNaN(d));
    }

}
