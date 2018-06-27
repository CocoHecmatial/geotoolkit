/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2015, Geomatys
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
package org.geotoolkit.wps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.geotoolkit.data.geojson.binding.GeoJSONObject;
import org.geotoolkit.data.geojson.utils.GeoJSONParser;
import static org.geotoolkit.wps.converters.ConvertersTestUtils.getTestResource;
import org.geotoolkit.wps.converters.WPSConvertersUtils;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.geotoolkit.wps.xml.v200.ComplexData;
import org.geotoolkit.wps.xml.v200.Data;
import org.geotoolkit.wps.xml.v200.DataInput;
import org.geotoolkit.wps.xml.v200.Execute;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Theo Zozime
 */
public class CDATATest extends org.geotoolkit.test.TestBase {

    /**
     * Test that the marshall and unmarshall operations works when
     * we marshall/unmarshall objects with CDATA tags
     */
    @Test
    public void testCDATAMarshallingOperations() throws JAXBException, IOException {
        // Get the content of a GeoJSON file
        String geoJsonData = getTestResource(this.getClass(), "/inputs/feature.json");
        assertNotNull(geoJsonData);

        // Create an execute request to marshall
        Execute request = new Execute();
        ComplexData complexToMarshal = new ComplexData();
        complexToMarshal.getContent().add(geoJsonData);
        request.getInput().add(new DataInput("toto", new Data(complexToMarshal)));

        // Marshall request into a StringWriter
        StringWriter sw = new StringWriter();
        Marshaller marshaller = WPSMarshallerPool.getInstance().acquireMarshaller();
        marshaller.marshal(request,sw);

        // Try to check that there is a CDATA tag set
        assertTrue(sw.toString().contains("<![CDATA["));
        assertTrue(sw.toString().contains("]]>"));

        // Unmarshall the marshalled datas
        Unmarshaller unmarshaller = WPSMarshallerPool.getInstance().acquireUnmarshaller();
        Execute executeRequest = (Execute) unmarshaller.unmarshal(new StringReader(sw.toString()));
        List<DataInput> dataInputs = executeRequest.getInput();

        // Assert that the resulting data is valid
        assertNotNull(dataInputs);
        assertEquals(1, dataInputs.size());
        final DataInput in = dataInputs.get(0);
        assertNotNull(in);
        assertNotNull(in.getData());
        ComplexData complex = in.getData().getComplexData();
        assertNotNull(complex);

        WPSConvertersUtils.removeWhiteSpaceFromList(complex.getContent());
        assertEquals(1, complex.getContent().size());

        String geoJsonContent = WPSConvertersUtils.geojsonContentAsString(complex.getContent().get(0));

        // Check that the json is still readable by the GeoJSONParser
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(geoJsonContent.getBytes());
        GeoJSONObject geoJsonObject = GeoJSONParser.parse(byteArrayInputStream);
        assertNotNull(geoJsonObject);
    }
}
