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
package org.geotoolkit.s52.symbolizer;

import java.util.Map;
import javax.measure.unit.NonSI;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.geotoolkit.style.AbstractExtensionSymbolizer;
import org.geotoolkit.style.DefaultDescription;
import org.opengis.filter.expression.Expression;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class S52Symbolizer extends AbstractExtensionSymbolizer{

    public static final String NAME = "S-52";

    public S52Symbolizer() {
        super(NonSI.PIXEL, "", "S-52",
                new DefaultDescription(
                new SimpleInternationalString(""),
                new SimpleInternationalString("")));
    }

    @Override
    public String getExtensionName() {
        return NAME;
    }

    @Override
    public Map<String, Expression> getParameters() {
        //add all properties here
        return super.getParameters();
    }

}
