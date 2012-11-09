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
package org.geotoolkit.wps.converters;

import com.vividsolutions.jts.geom.Geometry;
import java.util.List;
import java.util.*;
import javax.xml.bind.JAXBElement;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.type.DefaultFeatureType;
import org.geotoolkit.feature.type.DefaultGeometryType;
import org.geotoolkit.feature.type.DefaultPropertyDescriptor;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.mathml.xml.*;
import org.geotoolkit.ows.xml.v110.DomainMetadataType;
import org.geotoolkit.referencing.IdentifiedObjects;
import org.geotoolkit.util.ArgumentChecks;
import org.geotoolkit.util.converter.ConverterRegistry;
import org.geotoolkit.util.converter.NonconvertibleObjectException;
import org.geotoolkit.wps.io.WPSIO;
import org.geotoolkit.wps.xml.v100.ComplexDataType;
import org.geotoolkit.wps.xml.v100.InputReferenceType;
import org.geotoolkit.wps.xml.v100.OutputReferenceType;
import org.geotoolkit.wps.xml.v100.ReferenceType;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public class WPSConvertersUtils {

    /**
     * Fix the CRS problem for a Feature or a FeatureCollection
     *
     * @param dataValue a Feature or a FeatureCollection
     * @return the sale Feature/FeatureCollection fixed
     * @throws CstlServiceException
     */
    public static Object fixFeature(final Object dataValue) throws FactoryException {

        if (dataValue instanceof Feature) {

            final Feature featureIN = (Feature) dataValue;
            DefaultFeatureType ft = (DefaultFeatureType) featureIN.getType();
            fixFeatureType(featureIN, ft);

            return featureIN;
        }

        if (dataValue instanceof FeatureCollection) {
            final FeatureCollection featureColl = (FeatureCollection) dataValue;

            DefaultFeatureType ft = (DefaultFeatureType) featureColl.getFeatureType();
            final FeatureIterator featureIter = featureColl.iterator();
            if (featureIter.hasNext()) {
                final Feature feature = featureIter.next();
                fixFeatureType(feature, ft);
            }
            featureIter.close();
            return featureColl;
        }

        throw new IllegalArgumentException("Invalid Feature");
    }

    /**
     * Fix a FeatureType in spread the geometry CRS from a feature to the geometry descriptor CRS
     *
     * @param featureIN feature with geometry used to fix the geometry descriptor
     * @param type the featureType to fix
     * @throws CstlServiceException
     */
    private static void fixFeatureType(final Feature featureIN, DefaultFeatureType type) throws FactoryException {

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.copy(type);

        //Fetch each geometry, get his CRS and 
        for (Property property : featureIN.getProperties()) {
            if (property.getDescriptor() instanceof GeometryDescriptor) {
                final String propertyName = property.getName().getLocalPart();
                final Geometry propertyGeom = (Geometry) property.getValue();
                final CoordinateReferenceSystem extractCRS = JTS.findCoordinateReferenceSystem(propertyGeom);

                final Iterator<PropertyDescriptor> ite = type.getDescriptors().iterator();

                while (ite.hasNext()) {
                    final DefaultPropertyDescriptor propertyDesc = (DefaultPropertyDescriptor) ite.next();

                    if (propertyDesc.getName().getLocalPart().equals(propertyName)) {
                        final DefaultGeometryType geomType = (DefaultGeometryType) propertyDesc.getType();
                        geomType.setCoordinateReferenceSystem(extractCRS);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Convert a string to a binding class. If the binding class isn't a primitive like Integer, Double, .. we search
     * into the converter list if found a match.
     *
     * @param data string to convert
     * @param binding wanted class
     * @return converted object
     * @throws CstlServiceException if there is no match found
     */
    public static <T> Object convertFromString(final String data, final Class binding) throws NonconvertibleObjectException {

        Object convertedData = null; //resulting Object

        WPSObjectConverter<String, T> converter;//converter
        try {
            //try to convert into a primitive type
            converter = new WPSObjectConverterAdapter(ConverterRegistry.system().converter(String.class, binding));
        } catch (NonconvertibleObjectException ex) {
            //try to convert with some specified converter
            converter = WPSIO.getConverter(binding, WPSIO.IOType.INPUT, WPSIO.FormChoice.LITERAL);

            if (converter == null) {
                throw new NonconvertibleObjectException("Converter can't be found.");
            }
        }
        convertedData = converter.convert(data, null);
        return convertedData;
    }

    /**
     * Convert an object into a String.
     *
     * @param data
     * @return toString object.
     */
    public static String convertToString(final Object data) {
        String out = null;

        if (data != null) {
            try {
                WPSObjectConverter converter = WPSConverterRegistry.getInstance().getConverter(data.getClass(), String.class);
                out = (String) converter.convert(data, null);
            } catch (NonconvertibleObjectException ex) {
                if (data instanceof CoordinateReferenceSystem) {
                    out = IdentifiedObjects.getIdentifier((CoordinateReferenceSystem) data);
                } else {
                    out = String.valueOf(data);
                }
            }
        }
        return out;
    }

    /**
     * Get an convert data from a reference for an expected binding
     *
     * @param expectedClass
     * @param complex
     * @return
     * @throws CstlServiceException
     */
    public static Object convertFromComplex(final ComplexDataType complex, final Class expectedClass) throws NonconvertibleObjectException {

        final String mime = complex.getMimeType();
        final String encoding = complex.getEncoding();
        final String schema = complex.getSchema();
        
        WPSIO.checkSupportedFormat(expectedClass, WPSIO.IOType.INPUT, mime, encoding, schema);
           
        final List<Object> content = complex.getContent();
        
        //remove white spaces
        if (content != null) {
            final Iterator<Object> ite = content.iterator();
            while (ite.hasNext()) {
                final Object obj = ite.next();
                if (obj == null || (obj instanceof String && ((String) obj).trim().isEmpty())) {
                    ite.remove();
                }
            }
        }
       
        final WPSObjectConverter converter = WPSIO.getConverter(expectedClass, WPSIO.IOType.INPUT, WPSIO.FormChoice.COMPLEX);

        if (converter == null) {
            throw new NonconvertibleObjectException("Input complex not supported, no converter found.");
        }

        return converter.convert(complex, null);
    }

    /**
     * Get an convert an object int a {@link ComplexDataType complex}.
     * 
     * @param object
     * @param mime
     * @param encoding
     * @param schema
     * @param storageDirectory
     * @param storageURL
     * @return
     * @throws NonconvertibleObjectException 
     */
    public static ComplexDataType convertToComplex(final Object object, final String mime, final String encoding, final String schema,
            final String storageDirectory, final String storageURL)
            throws NonconvertibleObjectException {
        
        ArgumentChecks.ensureNonNull("Object", object);
        
        WPSIO.checkSupportedFormat(object.getClass(), WPSIO.IOType.INPUT, mime, encoding, schema);
        
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(WPSObjectConverter.TMP_DIR_PATH, storageDirectory);
        parameters.put(WPSObjectConverter.TMP_DIR_URL, storageURL);
        parameters.put(WPSObjectConverter.ENCODING, encoding);
        parameters.put(WPSObjectConverter.MIME, mime);
        parameters.put(WPSObjectConverter.SCHEMA, schema);

        
        final WPSObjectConverter converter = WPSIO.getConverter(object.getClass(), WPSIO.IOType.OUTPUT, WPSIO.FormChoice.COMPLEX);
        if (converter == null) {
            throw new NonconvertibleObjectException("Output complex not supported, no converter found.");
        }

        return (ComplexDataType) converter.convert(object, parameters);
    }
    
    /**
     * Get an convert data from a reference for an expected binding
     *
     * @param reference
     * @param expectedClass
     * @return an object
     * @throws NonconvertibleObjectException if something went wrong
     */
    public static Object convertFromReference(final ReferenceType reference, final Class expectedClass) throws NonconvertibleObjectException {

        final String mime = reference.getMimeType();
        final String encoding = reference.getEncoding();
        final String schema = reference.getSchema();
        WPSIO.checkSupportedFormat(expectedClass, WPSIO.IOType.INPUT, mime, encoding, schema);
        
        final WPSObjectConverter converter = WPSIO.getConverter(expectedClass, WPSIO.IOType.INPUT, WPSIO.FormChoice.REFERENCE);

        if (converter == null) {
            throw new NonconvertibleObjectException("Input reference not supported, no converter found.");
        }

        return converter.convert(reference, null);
    }

    /**
     * Get an convert an object int a {@link ReferenceType reference}.
     * 
     * @param object
     * @param mime
     * @param encoding
     * @param schema
     * @param storageDirectory
     * @param storageURL
     * @param iotype the io type requested (INPUT/OUTPUT)
     * @return an {@link InputReferenceType input reference} if ioType is set to INPUT, or an {@link OutputReferenceType output reference} otherwise.
     * @throws NonconvertibleObjectException 
     */
    public static ReferenceType convertToReference(final Object object, final String mime, final String encoding, final String schema,
            final String storageDirectory, final String storageURL, final WPSIO.IOType iotype) throws NonconvertibleObjectException {
        
        ArgumentChecks.ensureNonNull("Object", object);
        
        WPSIO.checkSupportedFormat(object.getClass(), WPSIO.IOType.INPUT, mime, encoding, schema);
        
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(WPSObjectConverter.TMP_DIR_PATH, storageDirectory);
        parameters.put(WPSObjectConverter.TMP_DIR_URL, storageURL);
        parameters.put(WPSObjectConverter.ENCODING, encoding);
        parameters.put(WPSObjectConverter.MIME, mime);
        parameters.put(WPSObjectConverter.SCHEMA, schema);
        parameters.put(WPSObjectConverter.IOTYPE, iotype.toString());

        final WPSObjectConverter converter = WPSIO.getConverter(object.getClass(), WPSIO.IOType.OUTPUT, WPSIO.FormChoice.REFERENCE);
        if (converter == null) {
            throw new NonconvertibleObjectException("Output complex not supported, no converter found.");
        }

        return (ReferenceType) converter.convert(object, parameters);
    }
    
    /**
     * Create the DomaineMetaData object for a literal
     *
     * @param clazz
     * @return
     * @throws CstlServiceException
     */
    public static DomainMetadataType createDataType(final Class clazz) {


        if (clazz.equals(Double.class)) {
            return new DomainMetadataType("Double", "http://www.w3.org/TR/xmlschema-2/#double");

        } else if (clazz.equals(Float.class)) {
            return new DomainMetadataType("Float", "http://www.w3.org/TR/xmlschema-2/#float");

        } else if (clazz.equals(Boolean.class)) {
            return new DomainMetadataType("Boolean", "http://www.w3.org/TR/xmlschema-2/#boolean");

        } else if (clazz.equals(Integer.class)) {
            return new DomainMetadataType("Integer", "http://www.w3.org/TR/xmlschema-2/#integer");

        } else if (clazz.equals(Long.class)) {
            return new DomainMetadataType("Long", "http://www.w3.org/TR/xmlschema-2/#long");

        } else if (clazz.equals(String.class) || WPSIO.isSupportedInputClass(clazz) || WPSIO.isSupportedOutputClass(clazz)) {
            return new DomainMetadataType("String", "http://www.w3.org/TR/xmlschema-2/#string");

        } else {
           return null;
        }
    }
    
    public static String getDataTypeString(final Class clazz) {
        String ref = createDataType(clazz).getReference();;
        
        if (ref == null) {
            ref = "http://www.w3.org/TR/xmlschema-2/#string";
        }
        return ref;
    }
    
    /**
     * Format an INPUT/OUTPUT format for errors messages.
     * @param mime
     * @param encoding
     * @param schema
     * @return 
     */
    public static String dataFormatToString(final String mime, final String encoding, final String schema) {
         final StringBuilder builder = new StringBuilder();
        final String begin = "[";
        final String end = "]";
        final String separator = ", ";

        builder.append(begin);
        
        builder.append("mimeType=");
        builder.append(mime);
        builder.append(separator);
        
        builder.append("encoding=");
        builder.append(encoding);
        builder.append(separator);
        
        builder.append("schema=");
        builder.append(schema);
       
        builder.append(end);
        return builder.toString();
    }
    
    /**
     * Extract the fist MathML MTable object.
     * @param mathExp
     * @return 
     */
    public static Mtable findMtable(List<Object> mathExp){
        
        for (Object object : mathExp) {
            if (object instanceof JAXBElement) {
                final JAXBElement element = (JAXBElement) object;
                if (element.getValue() instanceof Mtable) {
                    return (Mtable) element.getValue();
                } else if(element.getValue() instanceof Mrow) {
                    final Mrow mrow = (Mrow) element.getValue();
                    return findMtable(mrow.getMathExpression());
                }
            }
        }
        return null;
    }

    /**
     * Extact rows of an {@link Mtable table}.
     * @param table
     * @return 
     */
    public static List<Mtr> getRows(final Mtable table) {
        final List<Mtr> rows = new ArrayList<Mtr>();

        final List<JAXBElement<?>> jaxbRows = table.getTableRowExpression();

        for (JAXBElement<?> jaxbRow : jaxbRows) {
            if (jaxbRow.getValue() instanceof Mtr && jaxbRow.getValue() != null) {
                rows.add((Mtr) jaxbRow.getValue());
            }
        }
        return rows;
    }
    
    /**
     * Extact double value of cell of a {@link Mtr row}.
     * @param row
     * @return 
     */
    public static double[] getCells (final Mtr row) {
        final List<Double> cells = new ArrayList<Double>();
        final List<JAXBElement<TableCellExpression>> tableCellExpressionList = row.getTableCellExpression();
        
        for (JAXBElement<TableCellExpression> jAXBElement : tableCellExpressionList) {
            final TableCellExpression tableCellExpression = jAXBElement.getValue();
            final List<Object> objects = tableCellExpression.getMathExpression();
            
            for (Object object : objects) {
                final JAXBElement element = (JAXBElement) object;
                if (element.getValue() instanceof Mn && element.getValue() != null) {
                    final Mn mn = (Mn) element.getValue();
                    final String value = (String) mn.getContent().get(0);
                    cells.add(Double.valueOf(value));
                }
            }
        }
        final double[] cellsArray = new double[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            cellsArray[i] = cells.get(i).doubleValue();
        }
        
        return cellsArray;
    }
}
