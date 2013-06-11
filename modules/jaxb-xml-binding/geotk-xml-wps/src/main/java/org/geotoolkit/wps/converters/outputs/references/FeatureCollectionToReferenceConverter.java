/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 *    (C) 2011, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 3 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.wps.converters.outputs.references;

import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.feature.xml.XmlFeatureTypeWriter;
import org.geotoolkit.feature.xml.jaxb.JAXBFeatureTypeWriter;
import org.geotoolkit.feature.xml.jaxp.JAXPStreamFeatureWriter;
import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.util.converter.NonconvertibleObjectException;
import org.geotoolkit.wps.converters.outputs.complex.AbstractComplexOutputConverter;
import org.geotoolkit.wps.io.WPSIO;
import org.geotoolkit.wps.xml.v100.ComplexDataType;
import org.geotoolkit.wps.xml.v100.InputReferenceType;
import org.geotoolkit.wps.xml.v100.OutputReferenceType;
import org.geotoolkit.wps.xml.v100.ReferenceType;
import org.opengis.feature.type.FeatureType;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of ObjectConverter to convert a FeatureCollection into a {@link org.geotoolkit.wps.xml.v100.ComplexDataType}.
 *
 * @author Quentin Boileau (Geomatys).
 */
public final class FeatureCollectionToReferenceConverter extends AbstractReferenceOutputConverter<FeatureCollection> {

    private static FeatureCollectionToReferenceConverter INSTANCE;

    private FeatureCollectionToReferenceConverter() {
    }

    public static synchronized FeatureCollectionToReferenceConverter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FeatureCollectionToReferenceConverter();
        }
        return INSTANCE;
    }

    @Override
    public Class<? super FeatureCollection> getSourceClass() {
        return FeatureCollection.class;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceType convert(final FeatureCollection source, final Map<String, Object> params) throws NonconvertibleObjectException {

        if (params.get(TMP_DIR_PATH) == null) {
            throw new NonconvertibleObjectException("The output directory should be defined.");
        }
        
        if (source == null) {
            throw new NonconvertibleObjectException("The output data should be defined.");
        }
        if (!(source instanceof FeatureCollection)) {
            throw new NonconvertibleObjectException("The requested output data is not an instance of FeatureCollection.");
        }
        final WPSIO.IOType ioType = WPSIO.IOType.valueOf((String) params.get(IOTYPE));
        ReferenceType reference = null ;

        if (ioType.equals(WPSIO.IOType.INPUT)) {
            reference = new InputReferenceType();
        } else {
            reference = new OutputReferenceType();
        }
        reference.setMimeType((String) params.get(MIME));
        reference.setEncoding((String) params.get(ENCODING));

        final FeatureType ft = source.getFeatureType();
        final String namespace = ft.getName().getURI();
        final Map<String, String> schemaLocation = new HashMap<String, String>();

        final String randomFileName = UUID.randomUUID().toString();

        try {
            final String schemaFileName = randomFileName + "_schema" + ".xsd";
            //create file
            final File schemaFile = new File((String) params.get(TMP_DIR_PATH), schemaFileName);
            final OutputStream stream = new FileOutputStream(schemaFile);
            //write featureType xsd on file
            final XmlFeatureTypeWriter xmlFTWriter = new JAXBFeatureTypeWriter();
            xmlFTWriter.write(ft, stream);

            reference.setSchema((String) params.get(TMP_DIR_URL) + "/" + schemaFileName);
            schemaLocation.put(namespace, reference.getSchema());
            
        } catch (JAXBException ex) {
            throw new NonconvertibleObjectException("Can't write FeatureType into xsd schema.", ex);
        } catch (FileNotFoundException ex) {
            throw new NonconvertibleObjectException("Can't create xsd schema file.", ex);
        }

        JAXPStreamFeatureWriter featureWriter = null;
        try {
            featureWriter = new JAXPStreamFeatureWriter(schemaLocation);

            final String dataFileName = randomFileName+".xml";

            //create file
            final File dataFile = new File((String) params.get(TMP_DIR_PATH), dataFileName);
            final OutputStream dataStream = new FileOutputStream(dataFile);

            //Write feature collection in file
            featureWriter.setOutput(dataStream);
            featureWriter.writeFeatureCollection(source, false, null);

            reference.setHref((String) params.get(TMP_DIR_URL) + "/" +dataFileName);

        } catch (IOException ex) {
            throw new NonconvertibleObjectException(ex);
        } catch (XMLStreamException ex) {
            throw new NonconvertibleObjectException("Stax exception while writing the feature collection", ex);
        } catch (DataStoreException ex) {
            throw new NonconvertibleObjectException("DataStore exception while writing the feature collection", ex);
        } catch (FeatureStoreRuntimeException ex) {
            throw new NonconvertibleObjectException("DataStoreRuntimeException exception while writing the feature collection", ex);
        } finally {
            try {
                if (featureWriter != null) {
                    featureWriter.dispose();
                }
            } catch (IOException ex) {
                throw new NonconvertibleObjectException(ex);
            } catch (XMLStreamException ex) {
                throw new NonconvertibleObjectException(ex);
            }
        }

        return reference;

    }

}
