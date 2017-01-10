package org.geotoolkit.data.geojson.utils;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.data.geojson.binding.GeoJSONObject;
import org.geotoolkit.io.wkt.WKTFormat;
import org.geotoolkit.lang.Static;
import org.geotoolkit.nio.IOUtilities;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Collection;
import java.util.logging.Level;

import org.apache.sis.util.Utilities;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import org.apache.sis.referencing.IdentifiedObjects;
import static org.geotoolkit.data.geojson.utils.GeoJSONMembres.*;
import static org.geotoolkit.data.geojson.utils.GeoJSONTypes.*;

/**
 * @author Quentin Boileau (Geomatys)
 */
public final class GeoJSONUtils extends Static {

    /**
     * Fallback CRS
     */
    private static final CoordinateReferenceSystem DEFAULT_CRS = CommonCRS.WGS84.normalizedGeographic();

    /**
     * Parse LinkedCRS (href + type).
     * @param href
     * @param crsType
     * @return CoordinateReferenceSystem or null.
     */
    public static CoordinateReferenceSystem parseCRS(String href, String crsType) {
        String wkt = null;
        try(InputStream stream = new URL(href).openStream()) {
            wkt = IOUtilities.toString(stream);
        } catch (IOException e) {
            GeoJSONParser.LOGGER.log(Level.WARNING, "Can't access to linked CRS "+href, e);
        }

        if (wkt != null) {
            WKTFormat format = new WKTFormat();
            if (crsType.equals(CRS_TYPE_OGCWKT)) {
                format.setConvention(Convention.WKT1);
            } else if (crsType.equals(CRS_TYPE_ESRIWKT)) {
                format.setConvention(Convention.WKT1_COMMON_UNITS);
            }
            try {
                Object obj = format.parseObject(wkt);
                if (obj instanceof CoordinateReferenceSystem) {
                    return (CoordinateReferenceSystem) obj;
                } else {
                    GeoJSONParser.LOGGER.log(Level.WARNING, "Parsed WKT is not a CRS "+wkt);
                }
            } catch (ParseException e) {
                GeoJSONParser.LOGGER.log(Level.WARNING, "Can't parse CRS WKT " + crsType+ " : "+wkt, e);
            }
        }

        return null;
    }

    /**
     * Convert a CoordinateReferenceSystem to a identifier string like
     * urn:ogc:def:crs:EPSG::4326
     * @param crs
     * @return
     */
    public static String toURN(CoordinateReferenceSystem crs) {
        ArgumentChecks.ensureNonNull("crs", crs);

        try {
            if (Utilities.equalsIgnoreMetadata(crs, CommonCRS.WGS84.normalizedGeographic())) {
                return "urn:ogc:def:crs:OGC:1.3:CRS84";
            }

//            int code = IdentifiedObjects.lookupEpsgCode(crs, true);
            final Integer code = IdentifiedObjects.lookupEPSG(crs);
            if (code != null)
                return "urn:ogc:def:crs:EPSG::"+code;
        } catch (FactoryException e) {
            GeoJSONParser.LOGGER.log(Level.WARNING, "Unable to extract epsg code from given CRS "+crs, e);
        }
        GeoJSONParser.LOGGER.log(Level.WARNING, "Unable to extract epsg code from given CRS "+crs);
        return null;
    }

    /**
     * Try to extract/parse the CoordinateReferenceSystem from a GeoJSONObject.
     * Use WGS_84 as fallback CRS.
     * @param obj GeoJSONObject
     * @return GeoJSONObject CoordinateReferenceSystem or fallback CRS (WGS84).
     * @throws MalformedURLException
     * @throws DataStoreException
     */
    public static CoordinateReferenceSystem getCRS(GeoJSONObject obj) throws MalformedURLException, DataStoreException {
        CoordinateReferenceSystem crs = null;
        try {
            if (obj.getCrs() != null) {
                crs = obj.getCrs().getCRS();
            }
        } catch (FactoryException e) {
            throw new DataStoreException(e.getMessage(), e);
        }

        if (crs == null) {
            crs = DEFAULT_CRS;
        }
        return crs;
    }

    /**
     * Utility method Create geotk Envelope if bbox array is filled.
     * @return Envelope or null.
     */
    public static Envelope getEnvelope(GeoJSONObject obj, CoordinateReferenceSystem crs) {

        double[] bbox = obj.getBbox();
        if (bbox != null) {
            GeneralEnvelope env = new GeneralEnvelope(crs);
            int dim = bbox.length/2;
            if (dim == 2) {
                env.setRange(0, bbox[0], bbox[2]);
                env.setRange(1, bbox[1], bbox[3]);
            } else if (dim == 3) {
                env.setRange(0, bbox[0], bbox[3]);
                env.setRange(1, bbox[1], bbox[4]);
            }
            return env;
        }
        return null;
    }

    /**
     * Return file name without extension
     * @param file candidate
     * @return String
     */
    public static String getNameWithoutExt(Path file) {
        return IOUtilities.filenameWithoutExtension(file);
    }

    /**
     * Returns the filename extension from a {@link String}, {@link File}, {@link URL} or
     * {@link URI}. If no extension is found, returns an empty string.
     *
     * @param  path The path as a {@link String}, {@link File}, {@link URL} or {@link URI}.
     * @return The filename extension in the given path, or an empty string if none.
     */
    public static String extension(final Object path) {
        return IOUtilities.extension(path);
    }

    /**
     * Write an empty FeatureCollection in a file
     * @param f output file
     * @throws IOException
     */
    @Deprecated
    public static void writeEmptyFeatureCollection(File f) throws IOException {
        writeEmptyFeatureCollection(f.toPath());
    }

    /**
     * Write an empty FeatureCollection in a file
     * @param f output file
     * @throws IOException
     */
    public static void writeEmptyFeatureCollection(Path f) throws IOException {

        try (OutputStream outStream = Files.newOutputStream(f, CREATE, WRITE, TRUNCATE_EXISTING);
             JsonGenerator writer = GeoJSONParser.FACTORY.createGenerator(outStream, JsonEncoding.UTF8)) {

            //start write feature collection.
            writer.writeStartObject();
            writer.writeStringField(TYPE, FEATURE_COLLECTION);
            writer.writeArrayFieldStart(FEATURES);
            writer.writeEndArray();
            writer.writeEndObject();
            writer.flush();
        }
    }

    /**
     * Useful method to help write an object into a JsonGenerator.
     * This method can handle :
     * <ul>
     *     <li>Arrays</li>
     *     <li>Collection</li>
     *     <li>Numbers (Double, Float, Short, BigInteger, BigDecimal, integer, Long, Byte)</li>
     *     <li>Boolean</li>
     *     <li>String</li>
     * </ul>
     * @param value
     * @param writer
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public static void writeValue(Object value, JsonGenerator writer) throws IOException, IllegalArgumentException {

        if (value == null) {
            writer.writeNull();
            return;
        }

        Class binding = value.getClass();

        if (binding.isArray()) {
            if (byte.class.isAssignableFrom(binding.getComponentType())) {
                writer.writeBinary((byte[])value);
            } else {
                writer.writeStartArray();
                final int size = Array.getLength(value);
                for (int i = 0; i < size; i++) {
                    writeValue(Array.get(value, i), writer);
                }
                writer.writeEndArray();
            }

        } else if (Collection.class.isAssignableFrom(binding)) {
            writer.writeStartArray();
            Collection coll = (Collection) value;
            for (Object obj : coll) {
                writeValue(obj, writer);
            }
            writer.writeEndArray();

        } else if (Double.class.isAssignableFrom(binding)) {
            writer.writeNumber((Double) value);
        } else if (Float.class.isAssignableFrom(binding)) {
            writer.writeNumber((Float) value);
        } else if (Short.class.isAssignableFrom(binding)) {
            writer.writeNumber((Short) value);
        } else if (Byte.class.isAssignableFrom(binding)) {
            writer.writeNumber((Byte) value);
        } else if (BigInteger.class.isAssignableFrom(binding)) {
            writer.writeNumber((BigInteger) value);
        } else if (BigDecimal.class.isAssignableFrom(binding)) {
            writer.writeNumber((BigDecimal) value);
        } else if (Integer.class.isAssignableFrom(binding)) {
            writer.writeNumber((Integer) value);
        } else if (Long.class.isAssignableFrom(binding)) {
            writer.writeNumber((Long) value);

        } else if (Boolean.class.isAssignableFrom(binding)) {
            writer.writeBoolean((Boolean) value);
        } else if (String.class.isAssignableFrom(binding)) {
            writer.writeString(String.valueOf(value));
        } else {
            //fallback
            writer.writeString(String.valueOf(value));
        }
    }

    /**
     * Compare {@link JsonLocation} equality without sourceRef test.
     * @param loc1
     * @param loc2
     * @return
     */
    public static boolean equals(JsonLocation loc1, JsonLocation loc2) {
        if (loc1 == null) {
            return (loc2 == null);
        }

        return loc2 != null && (loc1.getLineNr() == loc2.getLineNr() &&
                loc1.getColumnNr() == loc2.getColumnNr() &&
                loc1.getByteOffset() == loc2.getByteOffset() &&
                loc1.getCharOffset() == loc2.getCharOffset());


    }
}
