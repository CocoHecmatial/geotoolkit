package org.geotoolkit.data.mapinfo.mif.geometry;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.geotoolkit.data.mapinfo.ProjectionUtils;
import org.geotoolkit.data.mapinfo.mif.style.Symbol;
import org.geotoolkit.feature.DefaultName;
import org.geotoolkit.feature.type.DefaultAttributeDescriptor;
import org.geotoolkit.feature.type.DefaultAttributeType;
import org.geotoolkit.storage.DataStoreException;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.*;
import org.opengis.referencing.operation.MathTransform;

import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

/**
 * Create collection of points from MIF MultiPoint
 *
 * @author Alexis Manin (Geomatys)
 *         Date : 27/02/13
 */
public class MIFMultiPointBuilder extends MIFGeometryBuilder {

    public static final Name NAME = new DefaultName("MULTIPOINT");

    public static final AttributeDescriptor SYMBOL_DESCRIPTOR;

    static {
        SYMBOL_DESCRIPTOR = new DefaultAttributeDescriptor(STRING_TYPE, Symbol.NAME, 0, 1, true, null);
    }

    @Override
    public void buildGeometry(Scanner scanner, Feature toFill, MathTransform toApply) throws DataStoreException {
        try {
            final int numCoords = scanner.nextInt()*2;
            double[] coords = new double[numCoords];
            for(int ptCount = 0 ; ptCount < numCoords ; ptCount++) {
                coords[ptCount] = Double.parseDouble(scanner.next(ProjectionUtils.DOUBLE_PATTERN));
            }

            final CoordinateSequence seq;
            if(toApply != null) {
                try {
                    double[] afterT = new double[numCoords];
                    toApply.transform(coords, 0, afterT, 0, numCoords/2);
                    seq = new PackedCoordinateSequence.Double(afterT, 2);
                } catch (Exception e) {
                    throw new DataStoreException("Unable to transform geometry", e);
                }
            } else {
                seq = new PackedCoordinateSequence.Double(coords, 2);
            }

            toFill.getProperty(NAME).setValue(GEOMETRY_FACTORY.createMultiPoint(seq));

            if(scanner.hasNext(Symbol.SYMBOL_PATTERN) && toFill.getType().getDescriptors().contains(SYMBOL_DESCRIPTOR)) {
                String args = scanner.next(Symbol.SYMBOL_PATTERN);
                String[] argsTab = args.substring(args.indexOf('(')+1, args.length()-1).trim().split(",");
                if (argsTab.length < 3) {
                    LOGGER.log(Level.WARNING, "A SYMBOL tag have been found, but can't be read (bad syntax ?). Ignore style.");
                }
                else {
                    final int width = Integer.decode(argsTab[0]);
                    final int pattern = Integer.decode(argsTab[1]);
                    final int color = Integer.decode(argsTab[2]);
                    Symbol symbol = new Symbol(width, pattern, color, null);
                    toFill.getProperty(Symbol.NAME).setValue(symbol);
                }
            }

        } catch (Exception e) {
            throw new DataStoreException("MultiPoint instance can't be read.", e);
        }
    }

    @Override
    public String toMIFSyntax(Feature geometry) throws DataStoreException {
        super.toMIFSyntax(geometry);

        final StringBuilder builder = new StringBuilder(NAME.getLocalPart());
        final Object value = geometry.getDefaultGeometryProperty().getValue();
        final MultiPoint multiPt;
        if(value instanceof CoordinateSequence) {
            multiPt = GEOMETRY_FACTORY.createMultiPoint(((CoordinateSequence)value).toCoordinateArray());
        } else {
            multiPt = (MultiPoint) geometry.getDefaultGeometryProperty().getValue();
        }
        builder.append(multiPt.getNumGeometries()).append('\n');

        for(int i =0 ; i < multiPt.getNumGeometries(); i++) {
            Point pt = (Point) multiPt.getGeometryN(i);
            builder.append(pt.getX()).append(pt.getY()).append('\n');
        }

        if(geometry.getProperty(Symbol.NAME) != null) {
            Object sValue = geometry.getProperty(Symbol.NAME).getValue();
            if(sValue != null && sValue instanceof Symbol) {
                builder.append(sValue).append('\n');
            }
        }

        return builder.toString();
    }

    @Override
    public Class getGeometryBinding() {
        return MultiPoint.class;
    }

    @Override
    public Class[] getPossibleBindings() {
        return new Class[]{MultiPoint.class, CoordinateSequence.class};
    }

    @Override
    public Name getName() {
        return NAME;
    }

    @Override
    protected List<AttributeDescriptor> getAttributes() {
        return Collections.singletonList(SYMBOL_DESCRIPTOR);
    }
}
