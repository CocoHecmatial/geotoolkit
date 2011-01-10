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

package org.geotoolkit.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import static javax.xml.stream.XMLStreamReader.*;

/**
 * An abstract class for all stax parser.<br/>
 * Readers for a given specification should extend this class and
 * provide appropriate read methods.<br/>
 * <br/>
 * Example : <br/>
 * <pre>
 * {@code
 * public class UserReader extends StaxStreamReader{
 *
 *   public User read() throws XMLStreamException{
 *      //casual stax reading operations
 *      return user;
 *   }
 * }
 * }
 * </pre>
 * And should be used like :<br/>
 * <pre>
 * {@code
 * final UserReader instance = new UserReader();
 * try{
 *     instance.setInput(stream);
 *     user = instance.read();
 * }finally{
 *     instance.dispose();
 * }
 * </pre>
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public abstract class StaxStreamReader {

    protected XMLStreamReader reader;
    
    /**
     * Store the input stream if it was generated by the parser itself.
     * It will closed on the dispose method or when a new input is set.
     */
    private InputStream sourceStream;

    public StaxStreamReader(){
    }

    /**
     * close potentiel previous stream and cache if there are some.
     * This way the reader can be reused for a different input later.
     * The underlying stax reader will be closed.
     */
    public void reset() throws IOException, XMLStreamException{
        if(sourceStream != null){
            sourceStream.close();
            sourceStream = null;
        }
        if(reader != null){
            reader.close();
            reader = null;
        }
    }

    /**
     * Release potentiel locks or opened stream.
     * Must be called when the reader is not needed anymore.
     * It should not be used after this method has been called.
     */
    public void dispose() throws IOException, XMLStreamException{
        reset();
    }

    /**
     * Set the input for this reader.<br/>
     * Handle types are :<br/>
     * - java.io.File<br/>
     * - java.io.Reader<br/>
     * - java.io.InputStream<br/>
     * - java.net.URL<br/>
     * - java.net.URI<br/>
     * - javax.xml.stream.XMLStreamReader<br/>
     * - javax.xml.transform.Source<br/>
     * 
     * @param input
     * @throws IOException
     * @throws XMLStreamException
     */
    public void setInput(Object input) throws IOException, XMLStreamException{
        reset();

        if(input instanceof XMLStreamReader){
            reader = (XMLStreamReader) input;
            return;
        }

        if(input instanceof File){
            sourceStream = new FileInputStream((File)input);
            input = sourceStream;
        }else if(input instanceof URL){
            sourceStream = ((URL)input).openStream();
            input = sourceStream;
        }else if(input instanceof URI){
            sourceStream = ((URI)input).toURL().openStream();
            input = sourceStream;
        }else if(input instanceof String){
            input = new StringReader((String) input);
        }

        reader = toReader(input);
    }


    /**
     * Iterator on the reader until it reachs the end of the given tag name.
     * @param tagName tag name to search
     * @throws XMLStreamException
     */
    protected void toTagEnd(final String tagName) throws XMLStreamException{
        while (reader.hasNext()) {
            if(END_ELEMENT == reader.next() &&
               tagName.equalsIgnoreCase(reader.getLocalName()))
               return;
        }
        throw new XMLStreamException("Error in xml file, Could not find end of tag "+tagName+" .");
    }

    /**
     * Creates a new XMLStreamReader.
     * @param input
     * @return XMLStreamReader
     * @throws XMLStreamException if the input is not handled
     */
    private static final XMLStreamReader toReader(final Object input)
            throws XMLStreamException{
        final XMLInputFactory XMLfactory = XMLInputFactory.newInstance();
        XMLfactory.setProperty("http://java.sun.com/xml/stream/properties/report-cdata-event", Boolean.TRUE);
        XMLfactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);

        if(input instanceof InputStream){
            return XMLfactory.createXMLStreamReader((InputStream)input);
        }else if(input instanceof Source){
            return XMLfactory.createXMLStreamReader((Source)input);
        }else if(input instanceof Reader){
            return XMLfactory.createXMLStreamReader((Reader)input);
        }else{
            throw new XMLStreamException("Input type is not supported : "+ input);
        }
    }

    /**
     * <p>XML language provides two notations for boolean type :
     * "true" can be written "1" and "0" significates "false".
     * This method considers all this values as Strings and return its boolean value.</p>
     *
     * @param bool The String to parse
     * @return true if bool is equal to "true" or "1".
     */
    protected static boolean parseBoolean(final String candidate) {
        if (candidate.length() == 1) {
            return !candidate.equals("0");
        }
        return Boolean.parseBoolean(candidate);
    }

    /**
     * <p>This method reads doubles with coma separated.</p>
     * 
     * @param candidate Can not be null.
     * @return
     */
    protected static double parseDouble(final String candidate) {
        return Double.parseDouble(candidate.replace(',', '.'));
    }

}
