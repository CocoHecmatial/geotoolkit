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
package org.geotoolkit.s52.lookuptable.instruction;

import com.vividsolutions.jts.geom.Coordinate;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedObject;
import org.geotoolkit.referencing.operation.matrix.XAffineTransform;
import org.geotoolkit.s52.S52Context;
import org.geotoolkit.s52.S52Palette;
import org.geotoolkit.s52.render.SymbolStyle;
import org.geotoolkit.util.Converters;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.referencing.operation.TransformException;

/**
 * S-52 Annex A Part I p.52  7.2
 *
 * @author Johann Sorel (Geomatys)
 */
public class Symbol extends Instruction{

    public Symbol() {
        super("SY");
    }

    /**
     * The symbol name is an 8 letter‑code that is composed of a class code
     * (6 letters) and a serial number (2 letters).
     */
    public String symbolName;

    /**
     * .2.1 Symbols with no rotation should always be drawn upright with respect to the screen.
     * .2.2 Symbols with a rotation instruction should be rotated with respect to the
     *      top of the screen (-y axis in figure 2 of section 5.1). (See example below).
     * .2.3 Symbols rotated by means of the six-character code of an S-57 attribute
     *      such as ORIENT should be rotated with respect to true north.
     * .2.4 The symbol should be rotated about its pivot point. Rotation angle is
     *      in degrees clockwise from 0 to 360. The default value is 0 degrees."
     */
    public String rotation;

    @Override
    protected void readParameters(String str) throws IOException {
        final String[] parts = str.split(",");
        symbolName = parts[0];
        if(parts.length>1){
            rotation = parts[1];
        }else{
            rotation = null;
        }
    }

    @Override
    public void render(RenderingContext2D ctx, S52Context context, S52Palette colorTable, ProjectedObject graphic, S52Context.GeoType geoType) throws PortrayalException {
        final Graphics2D g2d = ctx.getGraphics();
        final Feature feature = (Feature) graphic.getCandidate();

        final Coordinate center;
        try {
            center = getPivotPoint(graphic.getGeometry(null).getDisplayGeometryJTS());
        } catch (TransformException ex) {
            throw new PortrayalException(ex);
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        //find rotation
        float rotation = 0f;
        if(this.rotation == null || this.rotation.isEmpty()){
            rotation = 0f;
        }else{
            try{
                rotation = (float)Math.toRadians(Integer.valueOf(this.rotation));
            }catch(NumberFormatException ex){
                //it's a field
                final Property prop = feature.getProperty(this.rotation);
                if(prop!=null){
                    Float val = Converters.convert(prop.getValue(),Float.class);
                    if(val!=null){
                        //combine with map rotation
                        rotation = -(float)XAffineTransform.getRotation(ctx.getObjectiveToDisplay());
                        rotation += Math.toRadians(val);
                    }
                }
            }
        }

        final SymbolStyle ss = context.getSyle(this.symbolName);
        ss.render(g2d, context, colorTable, center, rotation);
    }

}
