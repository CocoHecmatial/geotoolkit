/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
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

package org.geotoolkit.gui.javafx.util;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javafx.beans.property.Property;
import javafx.beans.property.adapter.JavaBeanBooleanPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanDoublePropertyBuilder;
import javafx.beans.property.adapter.JavaBeanFloatPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanIntegerPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanLongPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.geotoolkit.image.io.PaletteFactory;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.internal.Loggers;
import org.geotoolkit.style.interval.DefaultIntervalPalette;
import org.geotoolkit.style.interval.DefaultRandomPalette;
import org.geotoolkit.style.interval.Palette;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class FXUtilities {

    
    public static final PaletteFactory PF = PaletteFactory.getDefault();
    public static final List<Object> PALETTES = new ArrayList<Object>();
    public static final List<Object> PALETTES_NAMED = new ArrayList<Object>();

    static{
        PALETTES.add(new DefaultRandomPalette());
        final Set<String> paletteNames = PF.getAvailableNames();

        for (String palName : paletteNames) {
            PALETTES.add(palName);
            PALETTES_NAMED.add(palName);
        }

        double[] fractions = new double[]{
                -3000,
                -1500,
                -0.1,
                +0,
                556,
                1100,
                1600,
                2200,
                3000};
        java.awt.Color[] colors = new java.awt.Color[]{
                new java.awt.Color(9, 9, 145, 255),
                new java.awt.Color(31, 131, 224, 255),
                new java.awt.Color(182, 240, 240, 255),
                new java.awt.Color(5, 90, 5, 255),
                new java.awt.Color(150, 200, 150, 255),
                new java.awt.Color(190, 150, 20, 255),
                new java.awt.Color(100, 100, 50, 255),
                new java.awt.Color(200, 210, 220, 255),
                new java.awt.Color(255, 255, 255, 255),
        };
        PALETTES.add(new DefaultIntervalPalette(fractions,colors));
    }
    
    public static final Font FONTAWESOME;
    
    static {
        FONTAWESOME = Font.loadFont(FXUtilities.class.getResource("/org/geotoolkit/font/fontawesome-webfont.ttf").toExternalForm(), 16);
    }
    
    private FXUtilities() {}
    
    public static <T> Property<T> beanProperty(Object candidate, String propertyName, Class<T> dataType){
        try {
            if(Boolean.class.equals(dataType)){
                return (Property<T>)JavaBeanBooleanPropertyBuilder.create().bean(candidate).name(propertyName).build();
            }else if(Integer.class.equals(dataType)){
                return (Property<T>)JavaBeanIntegerPropertyBuilder.create().bean(candidate).name(propertyName).build();
            }else if(Long.class.equals(dataType)){
                return (Property<T>)JavaBeanLongPropertyBuilder.create().bean(candidate).name(propertyName).build();
            }else if(Float.class.equals(dataType)){
                return (Property<T>)JavaBeanFloatPropertyBuilder.create().bean(candidate).name(propertyName).build();
            }else if(Double.class.equals(dataType)){
                return (Property<T>)JavaBeanDoublePropertyBuilder.create().bean(candidate).name(propertyName).build();
            }else{
                return JavaBeanObjectPropertyBuilder.create().bean(candidate).name(propertyName).build();
            }
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex.getMessage(),ex);
        }
    }
        
//    public ObservableValue create(){
//        return new ObservableValueBase() {
//
//            @Override
//            public Object getValue() {
//                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//            }
//        };
//    }
    
    
    public static java.awt.Color toSwingColor(Color fxColor){
        final int r = (int) (fxColor.getRed() * 255);
        final int g = (int) (fxColor.getGreen() * 255);
        final int b = (int) (fxColor.getBlue() * 255);
        final int rgb = (r << 16) + (g << 8) + b;
        return new java.awt.Color(rgb);
    }
    
    public static Color toFxColor(java.awt.Color swingColor){
        final double r = (double)swingColor.getRed() / 255.0;
        final double g = (double)swingColor.getGreen() / 255.0;
        final double b = (double)swingColor.getBlue() / 255.0;
        final double a = (double)swingColor.getBlue() / 255.0;
        return new Color(r, g, b, a);
    }
        
    /**
     * Expand all nodes from root to given node
     * @param candidate 
     */
    public static void expandRootToItem(TreeItem candidate) {
        if (candidate != null) {
            expandRootToItem(candidate.getParent());
            if (!candidate.isLeaf()) {
                candidate.setExpanded(true);
            }
        }
    }
    
    /**
     * Expand all nodes child node recursively
     * @param candidate 
     */
    public static void expandAll(TreeItem candidate) {
        if (candidate != null) {
            candidate.setExpanded(true);
            for(Object ti : candidate.getChildren()){
                expandAll((TreeItem)ti);
            }
        }
    }
    
    public static Image createPalettePreview(Object paletteValue, Dimension size){
        final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        
        if (paletteValue != null) {
            final Rectangle rect = new Rectangle(size);
            rect.grow(-2, -2);
            
            if (paletteValue instanceof String) {
                try {
                    final String paletteName = String.valueOf(paletteValue);
                    final org.geotoolkit.image.io.Palette palette = PF.getPalette(paletteName, 10);
                    final RenderedImage img = palette.getLegend(rect.getSize());
                    ((Graphics2D) g).drawRenderedImage(img, new AffineTransform(1,0,0,1,2,2));
                } catch (IOException ex) {
                    Loggers.JAVAFX.log(Level.WARNING, ex.getMessage(), ex);
                }
            } else if (paletteValue instanceof Palette) {
                final Palette palette = (Palette) paletteValue;
                palette.render((Graphics2D) g, rect);
            }
        }
        
        return SwingFXUtils.toFXImage(image, null);
    }
    

}
