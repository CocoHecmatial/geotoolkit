/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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
package org.geotoolkit.wps;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.geotoolkit.geometry.jts.JTS;
import org.apache.sis.util.UnconvertibleObjectException;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.apache.sis.test.Assert.*;
import org.geotoolkit.wps.io.WPSMimeType;
import org.geotoolkit.wps.io.WPSSchema;
import org.apache.sis.referencing.CommonCRS;
import org.geotoolkit.wps.xml.Execute;
import org.geotoolkit.wps.xml.v100.InputType;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.util.FactoryException;


/**
 * Testing class for GetCapabilities requests of WPS client, in version 1.0.0.
 *
 * @author Quentin Boileau
 */
public class ExecuteTest extends org.geotoolkit.test.TestBase {

    private static String EPSG_VERSION;

    public ExecuteTest() {
        EPSG_VERSION = org.geotoolkit.referencing.CRS.getVersion("EPSG").toString();
    }

    @Test
    public void testRequestAndMarshall() throws IOException, ParserConfigurationException, SAXException {
        try {
            final GeneralEnvelope env = new GeneralEnvelope(CRS.forCode("EPSG:4326"));
            env.setRange(0, 10, 10);
            env.setRange(1, 10, 10);

            final GeometryFactory gf = new GeometryFactory();
            final Point point = gf.createPoint(new Coordinate(0.0, 0.0));
            JTS.setCRS(point, CommonCRS.WGS84.geographic());

            final List<InputType> inputs = new ArrayList<>();
            inputs.add(InputType.createLiteral("literal", "10", null, null));
            inputs.add(InputType.createBoundingBox("bbox", env));
            inputs.add(InputType.createComplex("complex", "UTF-8", WPSMimeType.APP_GML.val(), WPSSchema.OGC_GML_3_1_1.getValue(), point, null, null));
            inputs.add(InputType.createReference("reference", "http://link.to/reference/", null, null, null, null));


            final List<WPSOutput> outputs = new ArrayList<>();
            outputs.add(new WPSOutput("output"));

            final WebProcessingClient client = new WebProcessingClient(new URL("http://test.com"), null, WPSVersion.v100);
            final ExecuteRequest request = client.createExecute();
            request.getContent().setIdentifier("identifier");
            request.setInputs(inputs);
            request.setOutputs(outputs);

            final Execute execute = request.getContent();
            assertEquals("WPS", execute.getService());
            assertEquals("1.0.0", execute.getVersion().toString());
            assertEquals(execute.getIdentifier().getValue(), "identifier");

            final StringWriter stringWriter = new StringWriter();
            final Marshaller marshaller = WPSMarshallerPool.getInstance().acquireMarshaller();
            marshaller.marshal(execute, stringWriter);

            String result = stringWriter.toString();
            String expected = expectedRequest();
            assertXmlEquals(expected, result, "xmlns:*");

            WPSMarshallerPool.getInstance().recycle(marshaller);
        } catch (UnconvertibleObjectException | JAXBException | FactoryException ex) {
            fail(ex.getLocalizedMessage());
        }
    }

    private static String expectedRequest() {

        String str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                +"<wps:Execute version=\"1.0.0\" service=\"WPS\""
                + " xmlns:wps=\"http://www.opengis.net/wps/1.0.0\""
                + " xmlns:ows=\"http://www.opengis.net/ows/1.1\""
                + " xmlns:xlink=\"http://www.w3.org/1999/xlink\""
                + " xmlns:gml=\"http://www.opengis.net/gml\">\n"
                +"    <ows:Identifier>identifier</ows:Identifier>\n"
                +"    <wps:DataInputs>\n"
                +"        <wps:Input>\n"
                +"            <ows:Identifier>literal</ows:Identifier>\n"
                +"            <wps:Data>\n"
                +"                <wps:LiteralData>10</wps:LiteralData>\n"
                +"            </wps:Data>\n"
                +"        </wps:Input>\n"
                +"        <wps:Input>\n"
                +"            <ows:Identifier>bbox</ows:Identifier>\n"
                +"            <wps:Data>\n"
                +"                <wps:BoundingBoxData dimensions=\"2\" crs=\"EPSG:4326\">\n"
                +"                    <ows:LowerCorner>10.0 10.0</ows:LowerCorner>\n"
                +"                    <ows:UpperCorner>10.0 10.0</ows:UpperCorner>\n"
                +"                </wps:BoundingBoxData>\n"
                +"            </wps:Data>\n"
                +"        </wps:Input>\n"
                +"        <wps:Input>\n"
                +"            <ows:Identifier>complex</ows:Identifier>\n"
                +"            <wps:Data>\n"
                +"                <wps:ComplexData>\n"
                +"                    <gml:Point srsName=\"urn:ogc:def:crs:EPSG:" + EPSG_VERSION + ":4326\">\n"
                +"                        <gml:pos srsName=\"urn:ogc:def:crs:EPSG:" + EPSG_VERSION + ":4326\" srsDimension=\"2\">0.0 0.0</gml:pos>\n"
                +"                    </gml:Point>\n"
                +"                </wps:ComplexData>\n"
                +"            </wps:Data>\n"
                +"        </wps:Input>\n"
                +"        <wps:Input>\n"
                +"            <ows:Identifier>reference</ows:Identifier>\n"
                +"            <wps:Reference xlink:href=\"http://link.to/reference/\"/>\n"
                +"        </wps:Input>\n"
                +"    </wps:DataInputs>\n"
                +"    <wps:ResponseForm>\n"
                +"        <wps:ResponseDocument status=\"false\" lineage=\"false\" storeExecuteResponse=\"false\">\n"
                +"            <wps:Output asReference=\"false\">\n"
                +"                <ows:Identifier>output</ows:Identifier>\n"
                +"            </wps:Output>\n"
                +"        </wps:ResponseDocument>\n"
                +"    </wps:ResponseForm>\n"
                +"</wps:Execute>\n";

        return str;
    }
}
