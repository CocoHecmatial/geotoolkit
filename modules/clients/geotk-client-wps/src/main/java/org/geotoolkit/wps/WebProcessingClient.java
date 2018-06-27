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

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import org.apache.sis.parameter.Parameters;
import org.geotoolkit.client.AbstractClient;
import org.geotoolkit.client.CapabilitiesException;
import org.geotoolkit.security.ClientSecurity;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.ows.xml.ExceptionResponse;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.geotoolkit.storage.DataStores;
import org.opengis.parameter.ParameterValueGroup;
import org.geotoolkit.ows.xml.v110.AcceptVersionsType;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.wps.xml.ProcessOfferings;
import org.geotoolkit.wps.xml.WPSCapabilities;

/**
 * WPS server, used to aquiere capabilites and requests process.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @module
 */
public class WebProcessingClient extends AbstractClient {

    private static final Logger LOGGER = Logging.getLogger("org.geotoolkit.wps");
    private static final int TIMEOUT_CAPS = 10000;

    private WPSCapabilities capabilities;
    private boolean forceGET = true;
    private WPSProcessingRegistry registry = null;


    /**
     * Constructor
     *
     * @param serverURL
     * @param version
     */
    public WebProcessingClient(final URL serverURL, final String version) {
        this(serverURL, null, version, true, null, false);
    }

    /**
     * Costructor with forceGET tunning.
     *
     * @param serverURL
     * @param version
     * @param forceGET if true, GetCapabilities and DescribeProcess will be request in GET, otherwise POST is used.
     */
    public WebProcessingClient(final URL serverURL, final String version, final boolean forceGET) {
        this(serverURL, null, version, forceGET, null, false);
    }

    /**
     * Constructor
     * Auto detect version.
     *
     * @param serverURL
     */
    public WebProcessingClient(final URL serverURL) {
        this(serverURL,null,null,true, null, false);
    }

    /**
     * Constructor
     * Auto detect version.
     *
     * @param serverURL
     * @param security
     */
    public WebProcessingClient(final URL serverURL, final ClientSecurity security) {
        this(serverURL,security,null,true, null, false);
    }

    /**
     * Constructor
     *
     * @param serverURL
     * @param security
     * @param version
     */
    public WebProcessingClient(final URL serverURL, final ClientSecurity security, final String version) {
        this(serverURL,security,version,true, null, false);
    }

    /**
     * Constructor
     *
     * @param serverURL
     * @param security
     * @param version
     */
    public WebProcessingClient(final URL serverURL, final ClientSecurity security, final WPSVersion version) {
        this(serverURL, security, version==null?null:version.getCode(), true, null, false);
    }

    /**
     * Constructor
     *
     * @param serverURL
     * @param security
     * @param version
     * @param timeout
     */
    public WebProcessingClient(final URL serverURL, final ClientSecurity security, final WPSVersion version, final Integer timeout) {
        this(serverURL, security, version==null?null:version.getCode(), true, timeout, false);
    }

    /**
     * Constructor
     *
     * @param serverURL
     * @param security
     * @param version
     * @param timeout
     */
    public WebProcessingClient(final URL serverURL, final ClientSecurity security, final WPSVersion version, final Integer timeout, final Boolean dynamicLoading) {
        this(serverURL, security, version==null?null:version.getCode(), true, timeout, dynamicLoading);
    }

    /**
     * Constructor
     *
     * @param serverURL
     * @param security
     * @param version
     * @param forceGET if true, GetCapabilities and DescribeProcess will be request in GET, otherwise POST is used.
     * @param timeout
     * @param dynamicLoading
     */
    public WebProcessingClient(final URL serverURL, final ClientSecurity security, String version, final boolean forceGET, final Integer timeout, final Boolean dynamicLoading) {
        super(create(WPSClientFactory.PARAMETERS, serverURL, security, timeout));

        if(version==null || "auto".equalsIgnoreCase(version)){
            //if version is null, call getCapabilities to found service version
            if(LOGGER.isLoggable(Level.FINE)){
                LOGGER.log(Level.FINE, "No version define : search it on getCapabilities");
            }
            try {
                this.capabilities = getServiceCapabilities();
                //set version
                version = WPSVersion.getVersion(this.capabilities.getVersion()).getCode();
            } catch (CapabilitiesException e) {
                LOGGER.log(Level.WARNING,  e.getLocalizedMessage(), e);
                version = WPSVersion.v200.getCode();
            }
        }
        if (dynamicLoading != null) {
            Parameters.castOrWrap(parameters).getOrCreate(WPSClientFactory.DYNAMIC_LOADING).setValue(dynamicLoading);
        }
        if (version.equals("1.0.0")) {
            Parameters.castOrWrap(parameters).getOrCreate(WPSClientFactory.VERSION).setValue(WPSVersion.v100.getCode());
        } else if (version.equals("2.0.0")) {
            Parameters.castOrWrap(parameters).getOrCreate(WPSClientFactory.VERSION).setValue(WPSVersion.v200.getCode());
        } else {
            throw new IllegalArgumentException("Unknown version : " + version);
        }
        this.forceGET = forceGET;

        LOGGER.log(Level.INFO, "Web processing client initialization complete.");
    }


    public WebProcessingClient(ParameterValueGroup params) {
        super(params);
        LOGGER.log(Level.INFO, "Web processing client initialization complete.");
    }

    public synchronized WPSProcessingRegistry asRegistry() throws CapabilitiesException {
        if (registry==null) {
            registry = new WPSProcessingRegistry(this, parameters.getOrCreate(WPSClientFactory.DYNAMIC_LOADING).booleanValue());
        }
        return registry;
    }

    @Override
    public DataStoreFactory getProvider() {
        return DataStores.getFactoryById(WPSClientFactory.NAME);
    }

    /**
     * @return WPSVersion : currently used version for this server
     */
    public WPSVersion getVersion() {
        return WPSVersion.getVersion(Parameters.castOrWrap(parameters).getValue(WPSClientFactory.VERSION));
    }

    @Override
    public Logger getLogger() {
        return super.getLogger();
    }

    /**
     * @return WPSCapabilitiesType : WPS server capabilities
     * @throws org.geotoolkit.client.CapabilitiesException
     */
    public WPSCapabilities getServiceCapabilities() throws CapabilitiesException {
        return getServiceCapabilities(false);
    }

    /**
     * @param refresh if set to true, the cached capabilities document will be renewed.
     *
     * @return WPSCapabilitiesType : WPS server capabilities
     * @throws org.geotoolkit.client.CapabilitiesException
     */
    public WPSCapabilities getServiceCapabilities(boolean refresh) throws CapabilitiesException {

        if (capabilities != null && !refresh) {
            return capabilities;
        }

        final GetCapabilitiesRequest capaReq = createGetCapabilities();
        capaReq.setTimeout(TIMEOUT_CAPS);
        try (final InputStream is = capaReq.getResponseStream()) {
            final Unmarshaller unmarshaller = WPSMarshallerPool.getInstance().acquireUnmarshaller();
            Object obj = unmarshaller.unmarshal(is);
            if(obj instanceof JAXBElement) {
                obj = ((JAXBElement)obj).getValue();
            }
            if(obj instanceof ExceptionResponse) {
                final ExceptionResponse er = (ExceptionResponse) obj;
                throw new CapabilitiesException(er.toException().getMessage(), er.toException());
            }
            capabilities = (WPSCapabilities) obj;
            WPSMarshallerPool.getInstance().recycle(unmarshaller);
        } catch (Exception ex) {
            capabilities = null;
            throw new CapabilitiesException(ex.getMessage(), ex);
        }

        if (capabilities == null) {
            throw new CapabilitiesException("A problem occured while getting Service capabilities.");
        }
        LOGGER.log(Level.INFO, "GetCapabilities request succeed.");
        return capabilities;
    }

    /**
     * Create a getCapabilities request.
     *
     * @return GetCapabilitiesRequest : getCapabilities request.
     */
    public GetCapabilitiesRequest createGetCapabilities() {

        final GetCapabilitiesRequest request = new GetCapabilitiesRequest(serverURL.toString(), getClientSecurity(), forceGET, getTimeOutValue());
        switch (getVersion()) {
            case v100: {
                final org.geotoolkit.wps.xml.v100.TOREMOVE.GetCapabilities cap = new org.geotoolkit.wps.xml.v100.TOREMOVE.GetCapabilities();
                cap.setService("WPS");
                cap.setAcceptVersions(new AcceptVersionsType("1.0.0"));
                request.setContent(cap);
                } break;
            case v200: {
                final org.geotoolkit.wps.xml.v200.GetCapabilities cap = new org.geotoolkit.wps.xml.v200.GetCapabilities();
                cap.setService("WPS");
                cap.setAcceptVersions(new org.geotoolkit.ows.xml.v200.AcceptVersionsType("2.0.0"));
                request.setContent(cap);
                } break;
            default:{
                if(LOGGER.isLoggable(Level.FINE)){
                    LOGGER.log(Level.FINE, "Version was not defined");
                }
                final org.geotoolkit.wps.xml.v200.GetCapabilities cap = new org.geotoolkit.wps.xml.v200.GetCapabilities();
                cap.setService("WPS");
                cap.setAcceptVersions(new org.geotoolkit.ows.xml.v200.AcceptVersionsType("1.0.0","2.0.0"));
                request.setContent(cap);
                } break;
        }

        return request;
    }

    /**
     * Create a describe process request
     *
     * @return DescribeProcessRequest : describe process request.
     */
    public DescribeProcessRequest createDescribeProcess() {

        final DescribeProcessRequest request = new DescribeProcessRequest(serverURL.toString(), getClientSecurity(), forceGET, getTimeOutValue());
        switch (getVersion()) {
            case v100: {
                final org.geotoolkit.wps.xml.v100.TOREMOVE.DescribeProcess content = new org.geotoolkit.wps.xml.v100.TOREMOVE.DescribeProcess();
                content.setService("WPS");
                content.setVersion("1.0.0");
                request.setContent(content);
                } break;
            case v200: {
                final org.geotoolkit.wps.xml.v200.DescribeProcess content = new org.geotoolkit.wps.xml.v200.DescribeProcess();
                content.setService("WPS");
                content.setVersion("2.0.0");
                request.setContent(content);
                } break;
            default:
                throw new IllegalArgumentException("Version not defined or unsupported.");
        }

        return request;
    }

    /**
     * Perform a DescribeProcess request on the specified identifiers.
     *
     * @param processIDs List of process Identifiers
     * @return ProcessDescriptions : WPS process description
     *
     * @throws java.io.IOException
     */
    public ProcessOfferings getDescribeProcess(final List<String> processIDs) throws Exception {

        ProcessOfferings description = null;

        //Thread to prevent infinite request on a server
        final DescribeProcessRequest describe = registry.getClient().createDescribeProcess();
        describe.setTimeout(getTimeOutValue());
        describe.getContent().setIdentifier(processIDs);
        try (final InputStream request = describe.getResponseStream()) {
            final Unmarshaller unmarshaller = WPSMarshallerPool.getInstance().acquireUnmarshaller();
            Object response = unmarshaller.unmarshal(request);
            WPSMarshallerPool.getInstance().recycle(unmarshaller);
            if (response instanceof ProcessOfferings) {
                description = (ProcessOfferings) response;
            } else if (response instanceof ExceptionResponse) {
                ExceptionResponse report = (ExceptionResponse) response;
                throw report.toException();
            } else {
                throw new Exception("Unexpected response type from the WPS server.");
            }
        }
        return description;
    }

    /**
     * Create an execute request
     *
     * @return Execute : execute request.
     */
    public ExecuteRequest createExecute() {

        final ExecuteRequest request = new ExecuteRequest(serverURL.toString(), getClientSecurity(), getTimeOutValue());

        switch (getVersion()) {
            case v100: {
                final org.geotoolkit.wps.xml.v100.TOREMOVE.Execute content = new org.geotoolkit.wps.xml.v100.TOREMOVE.Execute();
                content.setService("WPS");
                content.setVersion("1.0.0");
                request.setContent(content);
                } break;
            case v200: {
                final org.geotoolkit.wps.xml.v200.Execute content = new org.geotoolkit.wps.xml.v200.Execute();
                content.setService("WPS");
                content.setVersion("2.0.0");
                request.setContent(content);
                } break;
            default:
                throw new IllegalArgumentException("Version was not defined or unsupported.");
        }
        return request;
    }

    /**
     * Create a GetStatus request
     *
     * @param jobId The job identifier.
     * @return GetStatusRequest
     */
    public GetStatusRequest createGetStatus(final String jobId) {

        final GetStatusRequest request = new GetStatusRequest(serverURL.toString(), getClientSecurity(), true, getTimeOutValue());

        switch (getVersion()) {
            case v100:
                throw new IllegalArgumentException("GetStatus requests are not available in WPS 1.0.0");
            case v200: {
                final org.geotoolkit.wps.xml.v200.GetStatus content = new org.geotoolkit.wps.xml.v200.GetStatus();
                content.setService("WPS");
                content.setVersion("2.0.0");
                content.setJobID(jobId);
                request.setContent(content);
                } break;
            default:
                throw new IllegalArgumentException("Version was not defined or unsupported.");
        }
        return request;
    }

    /**
     * Create a GetResult request
     *
     * @param jobId The job identifier.
     * @return GetResultRequest.
     */
    public GetResultRequest createGetResult(final String jobId) {

        final GetResultRequest request = new GetResultRequest(serverURL.toString(), getClientSecurity(), true, getTimeOutValue());

        switch (getVersion()) {
            case v100:
                throw new IllegalArgumentException("GetResult requests are not available in WPS 1.0.0");
            case v200: {
                final org.geotoolkit.wps.xml.v200.GetResult content = new org.geotoolkit.wps.xml.v200.GetResult();
                content.setService("WPS");
                content.setVersion("2.0.0");
                content.setJobID(jobId);
                request.setContent(content);
                } break;
            default:
                throw new IllegalArgumentException("Version was not defined or unsupported.");
        }
        return request;
    }

    /**
     * Create a Dismiss request
     *
     * @param jobId The job identifier.
     * @return GetResultRequest.
     */
    public DismissRequest createDismiss(final String jobId) {

        final DismissRequest request = new DismissRequest(serverURL.toString(), getClientSecurity(), true, getTimeOutValue());

        switch (getVersion()) {
            case v100:
                throw new IllegalArgumentException("Dismiss requests are not available in WPS 1.0.0");
            case v200: {
                final org.geotoolkit.wps.xml.v200.Dismiss content = new org.geotoolkit.wps.xml.v200.Dismiss();
                content.setService("WPS");
                content.setVersion("2.0.0");
                content.setJobID(jobId);
                request.setContent(content);
                } break;
            default:
                throw new IllegalArgumentException("Version was not defined or unsupported.");
        }
        return request;
    }

}
