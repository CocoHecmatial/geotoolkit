/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Geomatys
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
package org.geotoolkit.client;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import org.geotoolkit.security.ClientSecurity;


/**
 * Default interface for all server-side classes.
 *
 * @author Cédric Briançon (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public interface Server {
    
    /**
     * @return the server url as an {@link URI}, or {@code null} il the uri syntax
     * is not respected.
     */
    URI getURI();
    
    /**
     * @return the server url as an {@link URL}.
     */
    URL getURL();
    
    /**
     * @return ClientSecurity used by this server. never null.
     */
    ClientSecurity getClientSecurity();
    
    
    /**
     * Store a value for this server in a hashmap using the given key.
     * @param key
     * @param value  
     */
    void setUserProperty(String key,Object value);

    /**
     * Get a stored value knowing the key.
     * @param key
     * @return user property object , can be null
     */
    Object getUserProperty(String key);

    /**
     * @return map of all user properties.
     *          This is the live map.
     */
    Map<String,Object> getUserProperties();
    
}
