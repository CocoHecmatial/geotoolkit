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
package org.geotoolkit.wps.converters.inputs.references;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import org.geotoolkit.feature.xml.XmlFeatureReader;
import org.geotoolkit.util.converter.NonconvertibleObjectException;
import org.geotoolkit.wps.converters.WPSConvertersUtils;
import org.geotoolkit.wps.converters.inputs.AbstractInputConverter;
import org.geotoolkit.wps.io.WPSMimeType;
import org.opengis.feature.Feature;
import org.opengis.util.FactoryException;

/**
 * Implementation of ObjectConverter to convert a reference into a Feature.
 *
 * @author Quentin Boileau (Geomatys).
 */
public final class ReferenceToFeatureConverter extends AbstractInputConverter {

    private static ReferenceToFeatureConverter INSTANCE;

    private ReferenceToFeatureConverter() {
    }

    public static synchronized ReferenceToFeatureConverter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ReferenceToFeatureConverter();
        }
        return INSTANCE;
    }

    @Override
    public Class<? extends Object> getTargetClass() {
        return Feature.class;
    }
    
    /**
     * {@inheritDoc}
     * @return Feature.
     */
    @Override
    public Feature convert(final Map<String, Object> source) throws NonconvertibleObjectException {

        final String mime = (String) source.get(IN_MIME) != null ? (String) source.get(IN_MIME) : WPSMimeType.TEXT_XML.val();
        final InputStream stream = (InputStream) source.get(IN_STREAM);

        //XML
        if (mime.equalsIgnoreCase(WPSMimeType.TEXT_XML.val()) || mime.equalsIgnoreCase(WPSMimeType.APP_GML.val())
                || mime.equalsIgnoreCase(WPSMimeType.TEXT_GML.val())) {

            XmlFeatureReader fcollReader = null;
            try {
                fcollReader = getFeatureReader(source);
                final Feature fcoll = (Feature) fcollReader.read(stream);
                return (Feature) WPSConvertersUtils.fixFeature(fcoll);

            } catch (FactoryException ex) {
                throw new NonconvertibleObjectException("Invalid reference input : can't spread CRS.", ex);
            } catch (IllegalArgumentException ex) {
                throw new NonconvertibleObjectException("Unable to read the feature with the specified schema.", ex);
            } catch (JAXBException ex) {
                throw new NonconvertibleObjectException("Invalid reference input : can't read reference schema.", ex);
            } catch (MalformedURLException ex) {
                throw new NonconvertibleObjectException("Invalid reference input : Malformed schema or resource.", ex);
            } catch (IOException ex) {
                throw new NonconvertibleObjectException("Invalid reference input : IO.", ex);
            } catch (XMLStreamException ex) {
                throw new NonconvertibleObjectException("Invalid reference input.", ex);
            } finally {
                if (fcollReader != null) {
                    fcollReader.dispose();
                }
            }

        } else {
            throw new NonconvertibleObjectException("Reference data mime is not supported");
        }
    }
}