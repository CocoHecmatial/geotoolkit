package org.geotoolkit.data.geojson;

import com.fasterxml.jackson.core.JsonEncoding;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.feature.FeatureUtilities;
import org.geotoolkit.feature.IllegalAttributeException;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Quentin Boileau (Geomatys)
 */
public class GeoJSONStreamWriter implements FeatureWriter<FeatureType, Feature> {

    private final GeoJSONWriter writer;
    private final FeatureType featureType;

    private Feature edited = null;
    private Feature lastWritten = null;
    private int currentFeatureIdx = 0;

    /**
     *
     * @param outputStream stream were GeoJSON will be written
     * @param featureType {@link FeatureType} of features to write.
     * @param doubleAccuracy number of coordinates fraction digits
     * @throws DataStoreException
     */
    public GeoJSONStreamWriter(OutputStream outputStream, FeatureType featureType, final int doubleAccuracy)
            throws DataStoreException {
        this(outputStream, featureType, JsonEncoding.UTF8, doubleAccuracy);
    }

    /**
     *
     * @param outputStream stream were GeoJSON will be written
     * @param featureType {@link FeatureType} of features to write.
     * @param encoding
     * @param doubleAccuracy number of coordinates fraction digits
     * @throws DataStoreException
     */
    public GeoJSONStreamWriter(OutputStream outputStream, FeatureType featureType, final JsonEncoding encoding, final int doubleAccuracy)
            throws DataStoreException {
        this.featureType= featureType;
        try {
            writer = new GeoJSONWriter(outputStream, encoding, doubleAccuracy, false);
            //start write feature collection.
            writer.writeStartFeatureCollection(featureType.getCoordinateReferenceSystem(), null);
            writer.flush();
        } catch (IOException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    @Override
    public FeatureType getFeatureType() {
        return featureType;
    }

    @Override
    public Feature next() throws FeatureStoreRuntimeException {
        edited = FeatureUtilities.defaultFeature(featureType, "id-" + currentFeatureIdx++);
        return edited;
    }

    @Override
    public void remove() throws FeatureStoreRuntimeException {
        throw new FeatureStoreRuntimeException("Not supported on reader.");
    }

    @Override
    public void write() throws FeatureStoreRuntimeException {
        if(edited == null || edited.equals(lastWritten)) return;

        lastWritten = edited;
        try {
            writer.writeFeature(edited);
            writer.flush();
        } catch (IOException e) {
            throw new FeatureStoreRuntimeException(e.getMessage(), e);
        } catch (IllegalAttributeException e) {
            throw new FeatureStoreRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public boolean hasNext() throws FeatureStoreRuntimeException {
        return true;
    }

    @Override
    public void close() {
        try {
            writer.writeEndFeatureCollection();
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            throw new FeatureStoreRuntimeException(ex);
        }
    }
}
