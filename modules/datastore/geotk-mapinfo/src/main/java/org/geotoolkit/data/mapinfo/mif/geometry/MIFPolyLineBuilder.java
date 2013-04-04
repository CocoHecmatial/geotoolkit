package org.geotoolkit.data.mapinfo.mif.geometry;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.geotoolkit.data.mapinfo.ProjectionUtils;
import org.geotoolkit.data.mapinfo.mif.style.Pen;
import org.geotoolkit.feature.DefaultName;
import org.geotoolkit.feature.FeatureTypeUtilities;
import org.geotoolkit.feature.FeatureUtilities;
import org.geotoolkit.feature.type.DefaultAttributeDescriptor;
import org.geotoolkit.feature.type.DefaultAttributeType;
import org.geotoolkit.storage.DataStoreException;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.*;
import org.opengis.referencing.operation.MathTransform;

import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Util class to build a feature from Multi line object of a MIF file.
 *
 * @author Alexis Manin (Geomatys)
 *         Date : 26/02/13
 */
public class MIFPolyLineBuilder extends MIFGeometryBuilder {

    public static final Name NAME = new DefaultName("PLINE");
    public static final Name SMOOTH_NAME = new DefaultName("SMOOTH");

    private static final AttributeDescriptor SMOOTH;
    private static final AttributeDescriptor PEN;

    static {
        PEN = new DefaultAttributeDescriptor(STRING_TYPE, Pen.NAME, 0, 1, true, null);

        final AttributeType smoothType =
                new DefaultAttributeType(SMOOTH_NAME, Boolean.class, false, false, null, null, null);
        SMOOTH = new DefaultAttributeDescriptor(smoothType, SMOOTH_NAME, 1, 1, false, Boolean.FALSE);
    }

    public SimpleFeatureType featureType;

    /**
     * {@inheritDoc}
     *
     *
     *
     * @param scanner the Scanner to use for geometry parsing (should be placed on the beginning of the geometry).
     * @param toFill
     * @param toApply
     * @return
     * @throws DataStoreException
     */
    @Override
    public void buildGeometry(Scanner scanner, Feature toFill, MathTransform toApply) throws DataStoreException {
        int numLines = 1;

        try {
            Pattern multiple = Pattern.compile("multiple", Pattern.CASE_INSENSITIVE);
            if(scanner.hasNext(multiple)) {
                numLines = Integer.decode(scanner.next("\\d+"));
            }

            final LineString[] lineTab = new LineString[numLines];
            for(int lineCount = 0 ; lineCount < numLines ; lineCount++) {
                // We put a x2 factor as we work in 2 dimensions.
                final int numCoord = Integer.decode(scanner.next("\\d+"))*2;
                final double[] linePts = new double[numCoord];
                for(int coordCount = 0 ; coordCount < numCoord; coordCount++) {
                    linePts[coordCount] = Double.parseDouble(scanner.next(ProjectionUtils.DOUBLE_PATTERN));
                }

                final CoordinateSequence seq;
                if(toApply != null) {
                    try {
                        double[] afterT = new double[numCoord];
                        toApply.transform(linePts, 0, afterT, 0, numCoord/2);
                        seq = new PackedCoordinateSequence.Double(afterT, 2);
                    } catch (Exception e) {
                        throw new DataStoreException("Unable to transform geometry", e);
                    }
                } else {
                    seq = new PackedCoordinateSequence.Double(linePts, 2);
                }
                lineTab[lineCount] = GEOMETRY_FACTORY.createLineString(seq);
            }

            toFill.getProperty(NAME).setValue(GEOMETRY_FACTORY.createMultiLineString(lineTab));

        } catch (InputMismatchException ex) {
            throw new DataStoreException("Line is not properly defined : not enough points found.", ex);
        }

        if(scanner.hasNext(Pen.PEN_PATTERN) && toFill.getType().getDescriptors().contains(PEN)) {
            String args = scanner.next(Pen.PEN_PATTERN);
            String[] argsTab = args.substring(args.indexOf('(')+1, args.length()-1).trim().split(",");
            if (argsTab.length < 3) {
                LOGGER.log(Level.WARNING, "A PEN tag have been found, but can't be read (bad syntax ?). Ignore style.");
            }
            else {
                final int width = Integer.decode(argsTab[0]);
                final int pattern = Integer.decode(argsTab[1]);
                final int color = Integer.decode(argsTab[2]);
                Pen pen = new Pen(width, pattern, color);
                toFill.getProperty(Pen.NAME).setValue(pen);
            }
        }

        if(scanner.hasNext(Pattern.compile(SMOOTH_NAME.getLocalPart(), Pattern.CASE_INSENSITIVE))) {
            toFill.getProperty(SMOOTH_NAME).setValue(Boolean.TRUE);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toMIFSyntax(Feature geometry) throws DataStoreException {
        super.toMIFSyntax(geometry);

        StringBuilder builder = new StringBuilder(NAME.getLocalPart());

        MultiLineString polyLine = null;
        Object value = geometry.getDefaultGeometryProperty().getValue();
        if(value instanceof LineString) {
            polyLine = GEOMETRY_FACTORY.createMultiLineString(new LineString[]{(LineString)value});
        } else {
            polyLine = (MultiLineString) value;
            if(polyLine.getNumGeometries() > 1) {
                builder.append("MULTIPLE ").append(polyLine.getNumGeometries());
            }
        }
        builder.append('\n');

        for(int i = 0 ; i < polyLine.getNumGeometries() ; i++) {
            LineString line = (LineString) polyLine.getGeometryN(i);
            builder.append(line.getNumPoints()).append('\n');
            for(Coordinate pt : line.getCoordinates()) {
                builder.append(pt.x).append(' ').append(pt.y).append('\n');
            }
        }

        if(geometry.getProperty(Pen.NAME) != null) {
            Object penValue = geometry.getProperty(Pen.NAME).getValue();
            if(penValue != null && penValue instanceof Pen) {
                builder.append(penValue).append('\n');
            }
        }

        if(geometry.getProperty(SMOOTH_NAME) != null) {
            builder.append(SMOOTH_NAME.getLocalPart()).append('n');
        }

        return builder.toString();
    }

    @Override
    public Class getGeometryBinding() {
        return MultiLineString.class;
    }

    @Override
    public Class[] getPossibleBindings() {
        return new Class[]{LineString.class, MultiLineString.class};
    }

    @Override
    public Name getName() {
        return NAME;
    }

    @Override
    protected List<AttributeDescriptor> getAttributes() {
        final List<AttributeDescriptor> descList = new ArrayList<AttributeDescriptor>(2);
        descList.add(PEN);
        descList.add(SMOOTH);

        return  descList;
    }
}
