/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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
package org.geotoolkit.process.converters;

import javax.measure.unit.Unit;

import org.geotoolkit.util.converter.NonconvertibleObjectException;
import org.geotoolkit.util.converter.SimpleConverter;

/**
 * Implementation of ObjectConverter to convert a String into an Unit
 * @author Quentin Boileau
 * @module pending
 */
public class StringToUnitConverter extends SimpleConverter<String, Unit> {
    /*
     * Public constructor in order to regiser converter in Geotk ConverterRegisry by ServiceLoader system.
     */
    public StringToUnitConverter(){
    }

    @Override
    public Class<? super String> getSourceClass() {
        return String.class;
    }

    @Override
    public Class<? extends Unit> getTargetClass() {
        return Unit.class ;
    }

    @Override
    public Unit convert(final String s) throws NonconvertibleObjectException {
        try {
            if(s == null) throw new NonconvertibleObjectException("Empty Unit");

            final Unit unit = Unit.valueOf(s);

            if(unit == null){
                throw new NonconvertibleObjectException("Invalid Unit");
            }
            return unit;
        } catch(IllegalArgumentException ex){
            throw new NonconvertibleObjectException(ex);
        }
    }
}


