/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2013, Geomatys
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
package org.geotoolkit.data.mapinfo.mif.geometry;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.mapinfo.ProjectionUtils;
import org.geotoolkit.data.mapinfo.mif.style.Pen;
import org.geotoolkit.feature.type.NamesExt;
import org.geotoolkit.feature.type.DefaultAttributeDescriptor;
import org.geotoolkit.feature.type.DefaultAttributeType;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.AttributeDescriptor;
import org.geotoolkit.feature.type.AttributeType;
import org.opengis.util.GenericName;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.util.ArgumentChecks;

/**
 * Class Description
 *
 * @author Alexis Manin (Geomatys)
 *         Date : 26/02/13
 */
public class MIFArcBuilder extends MIFGeometryBuilder {

    public static final GenericName NAME = NamesExt.create("ARC");
    public static final GenericName BEGIN_ANGLE_NAME = NamesExt.create("BEGIN_ANGLE");
    public static final GenericName END_ANGLE_NAME = NamesExt.create("END_ANGLE");

    public static final AttributeDescriptor BEGIN_ANGLE;
    public static final AttributeDescriptor END_ANGLE;
    private static final AttributeDescriptor PEN;

    static {
        final AttributeType angleType = new DefaultAttributeType(NamesExt.create("ANGLE"), Double.class,false, false, null, null, null);

        BEGIN_ANGLE = new DefaultAttributeDescriptor(angleType, BEGIN_ANGLE_NAME, 1, 1, false, null);
        END_ANGLE = new DefaultAttributeDescriptor(angleType, END_ANGLE_NAME, 1, 1, false, null);

        PEN = new DefaultAttributeDescriptor(STRING_TYPE, Pen.NAME, 1, 1, true, null);
    }

    @Override
    public void buildGeometry(Scanner scanner, Feature toFill, MathTransform toApply) throws DataStoreException {

        final double[] linePts = new double[4];
        try {
            for (int i = 0; i < linePts.length; i++) {
                linePts[i] = Double.parseDouble(scanner.next(ProjectionUtils.DOUBLE_PATTERN));
            }

            final CoordinateSequence seq;
            if(toApply != null) {
                try {
                double[] afterT = new double[4];
                toApply.transform(linePts, 0, afterT, 0, 2);
                seq = new PackedCoordinateSequence.Double(afterT, 2);
                } catch (Exception e) {
                    throw new DataStoreException("Unable to transform geometry", e);
                }
            } else {
                seq = new PackedCoordinateSequence.Double(linePts, 2);
            }
            final Envelope line = new Envelope(seq.getCoordinate(0), seq.getCoordinate(1));
            toFill.getDefaultGeometryProperty().setValue(line);

            // Get arc angles
            Double beginAngle = Double.parseDouble(scanner.next(ProjectionUtils.DOUBLE_PATTERN));
            Double endAngle   = Double.parseDouble(scanner.next(ProjectionUtils.DOUBLE_PATTERN));
            toFill.getProperty(BEGIN_ANGLE_NAME).setValue(beginAngle);
            toFill.getProperty(END_ANGLE_NAME).setValue(endAngle);
        } catch (InputMismatchException ex) {
            throw new DataStoreException("Arc is not properly defined : not enough points found.", ex);
        }

        if(scanner.hasNext(Pen.PEN_PATTERN) && toFill.getType().getDescriptors().contains(PEN)) {
            String args = scanner.nextLine();
            String[] argsTab = args.substring(args.indexOf('(')+1, args.length()-1)
                    .replaceAll("[^\\d^,]+", "")
                    .split(",");
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
    }

    @Override
    public String toMIFSyntax(Feature source) throws DataStoreException {
        ArgumentChecks.ensureNonNull("Source feature", source);
        if(source.getDefaultGeometryProperty() == null) {
            throw new DataStoreException("Input feature does not contain any geometry.");
        }

        if (source.getProperty(BEGIN_ANGLE_NAME) == null || source.getProperty(END_ANGLE_NAME) == null) {
            throw new DataStoreException("Not enough information to build an arc (missing angle).");
        }

        StringBuilder builder = new StringBuilder(NAME.tip().toString()).append(' ');
        Object value = source.getDefaultGeometryProperty().getValue();
        if(value instanceof Envelope) {
            Envelope env = (Envelope) value;
            builder.append(env.getMinX()).append(' ')
                    .append(env.getMinY()).append(' ')
                    .append(env.getMaxX()).append(' ')
                    .append(env.getMaxY());
        } else if (value instanceof Rectangle2D) {
            Rectangle2D rect = (Rectangle2D) value;
            builder.append(rect.getMinX()).append(' ')
                    .append(rect.getMinY()).append(' ')
                    .append(rect.getMaxX()).append(' ')
                    .append(rect.getMaxY());
        } else if(value instanceof Envelope2D) {
            Envelope2D env = (Envelope2D) value;
            builder.append(env.getMinX()).append(' ')
                    .append(env.getMinY()).append(' ')
                    .append(env.getMaxX()).append(' ')
                    .append(env.getMaxY());
        } else {
            throw new DataStoreException("Unable to build an arc with the current geometry (Non compatible type"+value.getClass()+").");
        }
        builder.append('\n');

        builder.append(source.getProperty(BEGIN_ANGLE_NAME).getValue()).append(' ')
                .append(source.getProperty(BEGIN_ANGLE_NAME).getValue()).append('\n');

        if(source.getProperty(Pen.NAME) != null) {
            Object penValue = source.getProperty(Pen.NAME).getValue();
            if(penValue != null && penValue instanceof Pen) {
                builder.append(penValue).append('\n');
            }
        }

        return builder.toString();
    }

    @Override
    public Class getGeometryBinding() {
        return Envelope.class;
    }

    @Override
    public Class[] getPossibleBindings() {
        return new Class[]{Envelope.class, Envelope2D.class, Rectangle2D.class};
    }

    public GenericName getName() {
        return NAME;
    }

    @Override
    protected List<AttributeDescriptor> getAttributes() {
        List<AttributeDescriptor> descList = new ArrayList<AttributeDescriptor>(3);
        descList.add(BEGIN_ANGLE);
        descList.add(END_ANGLE);
        descList.add(PEN);

        return descList;
    }
}
