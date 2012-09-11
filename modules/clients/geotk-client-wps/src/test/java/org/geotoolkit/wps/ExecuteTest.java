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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.util.StringUtilities;
import org.geotoolkit.util.converter.NonconvertibleObjectException;
import org.geotoolkit.wps.v100.Execute100;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.geotoolkit.wps.xml.v100.Execute;
import org.junit.Test;
import static org.junit.Assert.*;
import org.opengis.util.FactoryException;

/**
 * Testing class for GetCapabilities requests of WPS client, in version 1.0.0.
 *
 * @author Quentin Boileau
 */
public class ExecuteTest {
    
    private static String EPSG_VERSION;

    public ExecuteTest() {
        EPSG_VERSION = CRS.getVersion("EPSG").toString();
    }

    @Test
    public void testRequestAndMarshall() {
        try {
            final List<Double> corner = new ArrayList<Double>();
            corner.add(10.0);
            corner.add(10.0);

            final GeometryFactory gf = new GeometryFactory();
            final Point point = gf.createPoint(new Coordinate(0.0, 0.0));
            JTS.setCRS(point, CRS.decode("EPSG:4326"));

            final List<AbstractWPSInput> inputs = new ArrayList<AbstractWPSInput>();
            inputs.add(new WPSInputLiteral("literal", "10"));
            inputs.add(new WPSInputBoundingBox("bbox", corner, corner, "EPSG:4326", 2));
            inputs.add(new WPSInputComplex("complex", point, Geometry.class));
            inputs.add(new WPSInputReference("reference", "http://link.to/reference/"));

            final List<WPSOutput> outputs = new ArrayList<WPSOutput>();
            outputs.add(new WPSOutput("output"));

            final Execute100 exec100 = new Execute100("http://test.com", null);
            exec100.setIdentifier("identifier");
            exec100.setInputs(inputs);
            exec100.setOutputs(outputs);

            final Execute request = exec100.makeRequest();
            assertEquals("WPS", request.getService());
            assertEquals("1.0.0", request.getVersion());
            assertEquals(request.getIdentifier().getValue(), "identifier");

            final StringWriter stringWriter = new StringWriter();
            final Marshaller marshaller = WPSMarshallerPool.getInstance().acquireMarshaller();
            marshaller.marshal(request, stringWriter);

            String result = StringUtilities.removeXmlns(stringWriter.toString());
            String expected = expectedRequest();
            assertEquals(expected, result);

        } catch (FactoryException ex) {
            fail(ex.getLocalizedMessage());
            return;
        } catch (NonconvertibleObjectException ex) {
            fail(ex.getLocalizedMessage());
            return;
        } catch (JAXBException ex) {
            fail(ex.getLocalizedMessage());
            return;
        }
    }

    private static String expectedRequest() {

        String str = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                +"<wps:Execute version=\"1.0.0\" service=\"WPS\" >\n"
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
                +"                    <gml:Point srsName=\"urn:ogc:def:crs:epsg:" + EPSG_VERSION + ":4326\">\n"
                +"                        <gml:pos srsName=\"EPSG:4326\" srsDimension=\"2\">0.0 0.0</gml:pos>\n"
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
