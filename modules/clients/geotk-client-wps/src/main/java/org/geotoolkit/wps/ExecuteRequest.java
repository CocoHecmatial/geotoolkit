/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011-2016, Geomatys
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.geotoolkit.client.AbstractRequest;
import org.geotoolkit.ows.xml.v110.BoundingBoxType;
import org.geotoolkit.ows.xml.v110.CodeType;
import org.geotoolkit.security.ClientSecurity;
import org.apache.sis.util.UnconvertibleObjectException;
import org.geotoolkit.wps.converters.WPSConvertersUtils;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.geotoolkit.wps.xml.Execute;
import org.geotoolkit.wps.xml.v100.ComplexDataType;
import org.geotoolkit.wps.xml.v100.DataInputsType;
import org.geotoolkit.wps.xml.v100.DataType;
import org.geotoolkit.wps.xml.v100.DocumentOutputDefinitionType;
import org.geotoolkit.wps.xml.v100.InputReferenceType;
import org.geotoolkit.wps.xml.v100.InputType;
import org.geotoolkit.wps.xml.v100.LiteralDataType;
import org.geotoolkit.wps.xml.v100.OutputDefinitionType;
import org.geotoolkit.wps.xml.v100.ResponseDocumentType;
import org.geotoolkit.wps.xml.v100.ResponseFormType;

/**
 * WPS Execute request.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @module
 */
public class ExecuteRequest extends AbstractRequest {

    private Execute content;

    protected String outputForm;
    protected boolean storage;
    protected boolean status;
    protected List<WPSOutput> outputs;
    protected List<AbstractWPSInput> inputs;

    protected String storageDirectory;
    protected String storageURL;

    /**
     * Constructor, initialize status, lineage and storage to false and output form to "document"
     *
     * @param serverURL
     * @param security
     */
    public ExecuteRequest(final String serverURL, final ClientSecurity security) {
        super(serverURL, security, null);
        this.status = false;
        this.storage = false;
        this.outputForm = "document";
        this.outputs = new ArrayList<>();
        this.inputs = new ArrayList<>();
    }

    public Execute getContent() {
        return content;
    }

    public void setContent(Execute content) {
        this.content = content;
    }

    /**
     * Returns OutputForm "document" or "raw", can be {@code null}.
     * @return
     */
    public String getOutputForm() {
        return outputForm;
    }

    /**
     * Sets OutputForm to use.
     * @param outForm
     */
    public void setOutputForm(String outForm) {
        this.outputForm = outForm;
    }

    /**
     * Returns OutputStorage state, can be {@code null}.
     * @return
     */
    public boolean getOutputStorage() {
        return storage;
    }

    /**
     * Sets OutputStorage state.
     * @param outStorage
     */
    public void setOutputStorage(boolean outStorage) {
        this.storage = outStorage;
    }

    /**
     * Returns OutputStatus state, can be {@code null}.
     * @return
     */
    public boolean getOutputStatus() {
        return status;
    }

    /**
     * Sets OutputStatus state.
     * @param outStatus
     */
    public void setOutputStatus(boolean outStatus) {
        this.status = outStatus;
    }

    /**
     * Returns Outputs wanted from a process, can be {@code null}.
     * @return
     */
    public List<WPSOutput> getOutputs() {
        return outputs;
    }

    /**
     * Sets Outputs wanted from a process.
     * @param outForm
     */
    public void setOutputs(List<WPSOutput> outForm) {
        this.outputs = outForm;
        if (content instanceof org.geotoolkit.wps.xml.v100.Execute) {
            ((org.geotoolkit.wps.xml.v100.Execute)content).setResponseForm(getRespForm());
        }
    }

    /**
     * Returns Inputs, can be {@code null}.
     * @return
     */
    public List<AbstractWPSInput> getInputs() {
        return inputs;
    }

    /**
     * Sets Input to a process.
     * @param inputs
     */
    public void setInputs(List<AbstractWPSInput> inputs) {
        this.inputs = inputs;
        if (content instanceof org.geotoolkit.wps.xml.v100.Execute) {
            ((org.geotoolkit.wps.xml.v100.Execute)content).setDataInputs(getDataInputs());
        }
    }

    /**
     * Return the storage directory path used to store input/output data when it's needed.
     * @return path
     */
    public String getStorageDirectory() {
        return storageDirectory;
    }

    /**
     * Set the storage directory path used to store input/output data when it's needed.
     * @param path
     */
    public void setStorageDirectory(String path) {
        this.storageDirectory = path;
    }

    /**
     * Return the storage URL path used to acces stored data.
     * @return url
     */
    public String getStorageURL() {
        return storageURL;
    }

    /**
     * Set the storage URL path used to acces stored data.
     * @param url
     * @return path
     */
    public void setStorageURL(String url) {
        this.storageURL = url;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public InputStream getResponseStream() throws IOException {

        final Execute content = getContent();

        final URLConnection conec = openConnection();
        conec.setDoOutput(true);
        conec.setRequestProperty("Content-Type", "text/xml");

        try (OutputStream stream = security.encrypt(conec.getOutputStream())) {
            final Marshaller marshaller = WPSMarshallerPool.getInstance().acquireMarshaller();
            marshaller.marshal(content, stream);
            WPSMarshallerPool.getInstance().recycle(marshaller);
            stream.flush();
        } catch (JAXBException ex) {
            throw new IOException(ex);
        }
        return security.decrypt(conec.getInputStream());
    }

    /**
     * Send the request to the server URL in POST mode and return the unmarshalled response.
     *
     * @return Response of this request
     * @throws IOException if can't reach the server
     * @throws JAXBException if there is an error during Marshalling/Unmarshalling request or response.
     */
    public Object getResponse() throws JAXBException, IOException {

        // Parse the response
        Object response;
        try (final InputStream in = getResponseStream()) {
            final Unmarshaller unmarshaller = WPSMarshallerPool.getInstance().acquireUnmarshaller();
            response = unmarshaller.unmarshal(in);
            if (response instanceof JAXBElement) {
                return ((JAXBElement) response).getValue();
            }
            WPSMarshallerPool.getInstance().recycle(unmarshaller);
        }

        return response;
    }

    /**
     * Get all outputs
     *
     * @param in
     * @return
     */
    ResponseFormType getRespForm() {
        final ResponseFormType responseForm = new ResponseFormType();

        if (outputForm.equalsIgnoreCase("document")) {
            responseForm.setResponseDocument(getRespDocument());
        } else if (outputForm.equalsIgnoreCase("raw")) {
            responseForm.setRawDataOutput(getRespRaw());
        } else {
            throw new IllegalArgumentException("Respons form musb be \"raw\" or \"document\"");
        }

        return responseForm;
    }

    /**
     * Get an Output response for a document type
     *
     * @param in
     * @return
     */
    private ResponseDocumentType getRespDocument() {
        final ResponseDocumentType docu = new ResponseDocumentType();
        docu.setLineage(content.isLineage());
        docu.setStatus(status);
        docu.setStoreExecuteResponse(storage);

        for (WPSOutput output : outputs) {
            docu.getOutput().add(getOutputDef(output));
        }
        return docu;
    }

    /**
     * Get an Output response for a raw type
     *
     * @param in
     * @return
     */
    private OutputDefinitionType getRespRaw() {

        final WPSOutput output = outputs.get(0);
        final OutputDefinitionType raw = new OutputDefinitionType();
        raw.setIdentifier(new CodeType(output.getIdentifier()));
        raw.setSchema(output.getSchema());
        raw.setMimeType(output.getMime());
        raw.setUom(output.getUom());

        return raw;
    }

    /**
     * Get an Output definition
     *
     * @param in
     * @return
     */
    private DocumentOutputDefinitionType getOutputDef(final WPSOutput output) {

        final DocumentOutputDefinitionType outDef = new DocumentOutputDefinitionType();
        outDef.setIdentifier(new CodeType(output.getIdentifier()));
        outDef.setAsReference(output.getAsReference());
        outDef.setEncoding(output.getEncoding());
        outDef.setSchema(output.getSchema());
        outDef.setMimeType(output.getMime());
        outDef.setUom(output.getUom());

        return outDef;
    }

    /**
     * Get all intputs
     *
     * @param in
     * @return
     */
    DataInputsType getDataInputs() throws UnconvertibleObjectException {

        final DataInputsType input = new DataInputsType();

        for (AbstractWPSInput in : inputs) {
            if (in instanceof WPSInputBoundingBox) {
                input.getInput().add(getInputBbox((WPSInputBoundingBox) in));
            } else if (in instanceof WPSInputComplex) {
                input.getInput().add(getInputComplex((WPSInputComplex) in));
            } else if (in instanceof WPSInputLiteral) {
                input.getInput().add(getInputLiteral((WPSInputLiteral) in));
            } else if (in instanceof WPSInputReference) {
                input.getInput().add(getInputReference((WPSInputReference) in));
            }
        }
        return input;
    }

    /**
     * Get an Input of type bounding box
     *
     * @param in
     * @return
     */
    private InputType getInputBbox(final WPSInputBoundingBox in) {
        final InputType inputType = new InputType();

        final DataType data = new DataType();
        final BoundingBoxType bbox = new BoundingBoxType(in.getCrs(),
                in.getLowerCorner().get(0), in.getLowerCorner().get(1),
                in.getUpperCorner().get(0), in.getUpperCorner().get(1));

        data.setBoundingBoxData(bbox);
        inputType.setData(data);
        inputType.setIdentifier(new CodeType(in.getIdentifier()));

        return inputType;
    }

    /**
     * Get an Input of type complex
     *
     * @param in
     * @return
     */
    private InputType getInputComplex(final WPSInputComplex in) throws UnconvertibleObjectException {

        final InputType inputType = new InputType();

        final DataType datatype = new DataType();
        final String echoding  = in.getEncoding();
        final String mime      = in.getMime();
        final String schema    = in.getSchema();
        final Object inputData = in.getData();

        final Map<String,Object> parameters = new HashMap<>();
        parameters.put(WPSConvertersUtils.OUT_STORAGE_URL, storageURL);
        parameters.put(WPSConvertersUtils.OUT_STORAGE_DIR, storageDirectory);
        //Try to convert the complex input.
        final ComplexDataType complex = (ComplexDataType) WPSConvertersUtils.convertToComplex("1.0.0", inputData, mime, echoding, schema, parameters);

        datatype.setComplexData(complex);
        inputType.setData(datatype);
        inputType.setIdentifier(new CodeType(in.getIdentifier()));

        return inputType;
    }

    /**
     * Get an Input of type literal
     *
     * @param in
     * @return
     */
    private InputType getInputLiteral(final WPSInputLiteral in) {
        final InputType inputType = new InputType();

        final DataType datatype = new DataType();
        final LiteralDataType literal = new LiteralDataType();
        literal.setDataType(in.getDataType());
        literal.setUom(in.getUom());
        literal.setValue(in.getData());

        datatype.setLiteralData(literal);
        inputType.setData(datatype);
        inputType.setIdentifier(new CodeType(in.getIdentifier()));

        return inputType;
    }

    /**
     * Get an Input of type reference
     *
     * @param in
     * @return
     */
    private InputType getInputReference(final WPSInputReference in) {
        final InputType inputType = new InputType();

        final InputReferenceType ref = new InputReferenceType();
        ref.setHref(in.getHref());
        ref.setEncoding(in.getEncoding());
        ref.setMethod(in.getMethod());
        ref.setMimeType(in.getMime());
        ref.setSchema(in.getSchema());

        inputType.setReference(ref);
        inputType.setIdentifier(new CodeType(in.getIdentifier()));

        return inputType;
    }
}
