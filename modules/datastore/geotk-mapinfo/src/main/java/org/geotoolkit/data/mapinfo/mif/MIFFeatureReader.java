package org.geotoolkit.data.mapinfo.mif;

import org.geotoolkit.data.FeatureReader;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.feature.FeatureUtilities;
import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.util.ArgumentChecks;
import org.geotoolkit.util.Converters;
import org.geotoolkit.util.Strings;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.operation.MathTransform;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * MIF reader which is designed to browse data AND ONLY data, it's to say geometry data from MIF file, and all data from
 * MID file.
 *
 * @author Alexis Manin (Geomatys)
 * @date : 22/02/13
 */
public class MIFFeatureReader implements FeatureReader<FeatureType, Feature> {

    private final static Logger LOGGER = Logger.getLogger(MIFFeatureReader.class.getName());

    private static final Pattern GEOMETRY_ID_PATTERN;
    static {
        final StringBuilder patternBuilder = new StringBuilder();
        final MIFUtils.GeometryType[] types = MIFUtils.GeometryType.values();
        patternBuilder.append(types[0].name());
        for(int i = 1 ; i < types.length; i++) {
            patternBuilder.append('|').append(types[i].name());
        }
        GEOMETRY_ID_PATTERN = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }

    /**
     * MIF/MID input connections.
     */
    private InputStream mifStream = null;
    private InputStream midStream = null;

    /**
     * MIF/MID file readers.
     */
    private Scanner mifScanner = null;
    private Scanner midScanner = null;

    /**
     * Counters : feature counter (Mid and mif lines are not equal for the same feature.
     */
    int mifCounter = 0;
    int midCounter = 0;

    /**
     * booleans to check if we just read mid file (feature type doesn't contain any geometry) or just MIF file
     * (geometries only), or both.
     */
    boolean readMid = false;
    boolean readMif = false;

    MIFManager master;
    FeatureType readType;

    public MIFFeatureReader(MIFManager parent, Name typeName) throws DataStoreException {
        ArgumentChecks.ensureNonNull("Parent reader", parent);
        master = parent;
        readType = master.getType(typeName);
        if(readType.equals(master.getBaseType()) || readType.getSuper().equals(master.getBaseType())) {
            readMid = true;
        }

        if(readType.getGeometryDescriptor() != null) {
            readMif = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureType getFeatureType() {
        return readType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Feature next() throws FeatureStoreRuntimeException {

        Feature resFeature = null;
        final SimpleFeature mifFeature;

        try {
            checkScanners();

            String name = (readMif)? "mif"+mifCounter : "mid"+midCounter;
            resFeature = FeatureUtilities.defaultFeature(readType, name);

            // We check the MIF file first, because it will define the feature count to reach the next good typed data.
            if(readMif) {
                final String geomId = readType.getGeometryDescriptor().getLocalName();
                String currentPattern;
                while(mifScanner.hasNextLine()) {
                    currentPattern = mifScanner.findInLine(GEOMETRY_ID_PATTERN);
                    if(geomId.equalsIgnoreCase(currentPattern)) {
                        parseGeometry(geomId, resFeature, master.getTransform());
                        break;
                        // We must check if we're on a Geometry naming line to increment the counter of past geometries.
                    } else if(currentPattern != null) {
                        mifCounter++;
                    }
                    mifScanner.nextLine();
                }
            }

            if(readMid) {
                final SimpleFeatureType baseType = master.getBaseType();
                //parse MID line.
                while(midCounter < mifCounter) {
                    midScanner.nextLine();
                    midCounter++;
                }
                final String line = midScanner.nextLine();
                final String[] split = Strings.split(line, master.mifDelimiter);
                for (int i = 0; i < split.length; i++) {
                    AttributeType att = baseType.getType(i);
                    Object value = null;
                    if (!split[i].isEmpty()) {
                        value = Converters.convert(split[i], att.getBinding());
                    }
                    resFeature.getProperty(att.getName()).setValue(value);
                }
                midCounter++;
            }

            if(readMif) {
                mifCounter++;
            }

        } catch (Exception ex) {
            throw new FeatureStoreRuntimeException("Can't reach next feature with type name " + readType.getName().getLocalPart(), ex);
        }

        return resFeature;
    }

    /**
     * Parse the geometry pointed by this reader scanner.
     * @param geomId The ID (REGION, POLYLINE, etc) of the geometry to parse.
     * @param toFill The feature to put built geometry into. Can't be null.
     * @param transform A math transform to apply to read geometry.
     * @throws DataStoreException If a problem occurs while parsing geometry.
     */
    private void parseGeometry(String geomId, Feature toFill, MathTransform transform) throws DataStoreException {
        final String upperId = geomId.toUpperCase();
        MIFUtils.GeometryType builder = MIFUtils.getGeometryType(geomId);
        builder.readGeometry(mifScanner, toFill, transform);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() throws FeatureStoreRuntimeException {
        boolean midNext = false;
        boolean mifNext = false;

        try {
            try {
            checkScanners();
            } catch (IOException e) {
                // If we can't access source files, maybe we're in creation mode, so we just say we can't find next.
                return false;
            }

            if (readMif) {
                // Check the MapInfo geometry typename to see if there's some next in the file.
                String geomName = readType.getGeometryDescriptor().getLocalName();
                Pattern geomPattern = Pattern.compile(geomName, Pattern.CASE_INSENSITIVE);
                while(mifScanner.hasNextLine()) {
                    if (mifScanner.hasNext(geomPattern)) {
                        mifNext = true;
                        break;
                    } else {
                        // We must check if we're on a Geometry naming line to increment the counter of past geometries.
                        if(mifScanner.hasNext(GEOMETRY_ID_PATTERN)) {
                            mifCounter++;
                        }
                    }

                    mifScanner.nextLine();
                }
            }

            // Once we know the number of the next geometry data, we can check if we can go as far in the mid file.
            if (readMid) {
                for( ; midCounter < mifCounter ; midCounter++) {
                    if(midScanner.hasNextLine()) {
                        midScanner.nextLine();
                    } else {
                        break;
                    }
                }
                midNext = midScanner.hasNextLine();
            }

        } catch (Exception ex) {
            throw new FeatureStoreRuntimeException(ex);
        }

        if (readMid && !readMif) {
            return midNext;
        } else if (readMif && !readMid) {
            return mifNext;
        } else return (midNext && mifNext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {

        mifCounter = 0;
        midCounter = 0;

        if (mifScanner != null) {
            mifScanner.close();
            mifScanner = null;

        }
        if (midScanner != null) {
            midScanner.close();
            midScanner = null;

        }

        try {
            if (mifStream != null) {
                mifStream.close();
                mifStream = null;
            }

            if (midStream != null) {
                midStream.close();
                midStream = null;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Input connections to MIF/MID files can't be closed.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException("MIF feature iterator is for reading only.");
    }


    /**
     * Check if we have an open access to mif/mid files. If not, try to get one.
     *
     * @throws DataStoreException If we're unable to access files.
     */
    private void checkScanners() throws DataStoreException, IOException {
        if(readMif) {
            if(mifStream == null) {
                mifStream = MIFUtils.openInConnection(master.getMIFPath());
            }
            if(mifScanner == null) {
                mifScanner = new Scanner(mifStream);
                repositionMIFReader();
            }
        }

        if(readMid) {
            if(midStream == null) {
                midStream = MIFUtils.openInConnection(master.getMIDPath());
            }
            if(midScanner == null) {
                midScanner = new Scanner(midStream);

                // Reposition the scanner.
                int midPosition = 0;
                while (midPosition < midCounter) {
                    midScanner.nextLine();
                    midPosition++;
                }
            }
        }
    }

    /**
     * Check the current feature counter, and move the mif scanner according to that counter. It's useful if MIF scanner
     * have been reset but not iterator position.
     *
     * WARNING : YOU <b>MUST NOT</b> USE THIS FUNCTION IF SCANNERS ARE NOT EARLY PLACED IN THE INPUT FILE.
     */
    private void repositionMIFReader() {
        int mifPosition = 0;
        // Go to the first feature.
        while (mifScanner.hasNextLine()) {
            if (mifScanner.hasNext(GEOMETRY_ID_PATTERN)) {
                break;
            }
            mifScanner.nextLine();
        }

        //Browse file until we're well placed.
        while (mifPosition < mifCounter) {
            while (mifScanner.hasNextLine()) {
                mifScanner.nextLine();
                if (mifScanner.hasNext(GEOMETRY_ID_PATTERN)) {
                    mifPosition++;
                    break;
                }
            }
        }
    }

}
