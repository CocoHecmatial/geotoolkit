/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007 - 2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2008 - 2009, Johann Sorel
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
package org.geotoolkit.gui.swing.style;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JButton;
import org.geotoolkit.cql.CQLException;
import org.geotoolkit.gui.swing.filter.JCQLEditor;
import org.geotoolkit.gui.swing.resource.FontAwesomeIcons;
import org.geotoolkit.gui.swing.resource.IconBuilder;
import org.geotoolkit.map.MapLayer;
import org.opengis.filter.expression.Expression;

/**
 *
 * @author Johann Sorel (Puzzle-GIS)
 * @module pending
 */
public class JSpecialExpressionButton extends JButton{

    public static final String EXPRESSION_PROPERTY = "expression";

    private static final Icon ICON_EXP_NO = IconBuilder.createIcon(FontAwesomeIcons.ICON_PENCIL_SQUARE, 16, Color.LIGHT_GRAY);
    private static final Icon ICON_EXP_YES = IconBuilder.createIcon(FontAwesomeIcons.ICON_PENCIL_SQUARE, 16, Color.RED);
    private Expression exp = null;
    private MapLayer layer = null;

    public JSpecialExpressionButton(){
        setBorderPainted(false);
        setContentAreaFilled(false);
        setBorder(null);
        setIcon(ICON_EXP_NO);

        addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                try{
                    final Expression ne = JCQLEditor.showDialog(JSpecialExpressionButton.this, layer, exp);
                    if(ne != null && ne != exp){
                        parse(ne);
                        firePropertyChange(EXPRESSION_PROPERTY, exp, ne);
                    }
                }catch(CQLException ex){
                    ex.printStackTrace();
                }
            }
        });
    }

    public void setLayer(final MapLayer layer){
        this.layer = layer;
    }

    public MapLayer getLayer(){
        return layer;
    }

    public void parse(final Expression exp){
        this.exp = exp;
        setIcon( (exp == null) ? ICON_EXP_NO : ICON_EXP_YES);
    }

    public Expression get(){
        return exp;
    }

}
