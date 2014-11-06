/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 * 
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009, Geomatys
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
package org.geotoolkit.temporal.reference;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.geotoolkit.metadata.Citations;
import org.apache.sis.referencing.NamedIdentifier;
import org.geotoolkit.temporal.object.DefaultCalendarDate;
import org.geotoolkit.temporal.object.DefaultInstant;
import org.geotoolkit.temporal.object.DefaultJulianDate;
import org.geotoolkit.temporal.object.DefaultPeriod;
import org.geotoolkit.temporal.object.DefaultPosition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.temporal.CalendarDate;
import org.opengis.temporal.CalendarEra;
import org.opengis.temporal.IndeterminateValue;
import org.opengis.temporal.Instant;
import org.opengis.temporal.JulianDate;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalReferenceSystem;
import org.opengis.util.InternationalString;


/**
 *
 * @author Mehdi Sidhoum (Geomatys)
 * @module pending
 */
public class DefaultCalendarEraTest {

    private CalendarEra calendarEra1;
    private CalendarEra calendarEra2;
    private Calendar cal = Calendar.getInstance();

    @Before
    public void setUp() {
        TemporalDatum tempdat = CommonCRS.Temporal.UNIX.datum();
        NamedIdentifier name1 = new NamedIdentifier(Citations.CRS, "Julian calendar");
        final Map<String, Object> properties1 = new HashMap<>();
        properties1.put(IdentifiedObject.NAME_KEY, name1);
//        TemporalReferenceSystem frame1 = new DefaultTemporalReferenceSystem(properties1, tempdat, null);
        TemporalReferenceSystem frame1 = new DefaultTemporalReferenceSystem(properties1);
        
        NamedIdentifier name2 = new NamedIdentifier(Citations.CRS, "Babylonian calendar");
        final Map<String, Object> properties2 = new HashMap<>();
        properties2.put(IdentifiedObject.NAME_KEY, name2);
//        TemporalReferenceSystem frame2 = new DefaultTemporalReferenceSystem(properties2, tempdat, null);
        TemporalReferenceSystem frame2 = new DefaultTemporalReferenceSystem(properties2);
//        TemporalReferenceSystem frame1 = new DefaultTemporalReferenceSystem(name1, null);
//        TemporalReferenceSystem frame2 = new DefaultTemporalReferenceSystem(name2, null);
        int[] calendarDate1 = {1900, 1, 1};
        int[] calendarDate2 = {400, 1, 1};
        CalendarDate referenceDate1 = new DefaultCalendarDate(frame1, IndeterminateValue.BEFORE, new SimpleInternationalString("Gregorian calendar"), calendarDate1);
        CalendarDate referenceDate2 = new DefaultCalendarDate(frame2, IndeterminateValue.NOW, new SimpleInternationalString("Babylonian calendar"), calendarDate2);
        JulianDate julianReference = new DefaultJulianDate(frame1, IndeterminateValue.NOW, 123456789);
        
        cal.set(1900, 0, 1);
        
        //-- Map instant
        NamedIdentifier nameInstant = new NamedIdentifier(Citations.CRS, "Period instant");
        final Map<String, Object> propertiesInstant = new HashMap<>();
        propertiesInstant.put(IdentifiedObject.NAME_KEY, nameInstant);
        
        Instant begining1 = new DefaultInstant(propertiesInstant, new DefaultPosition(cal.getTime()));
        cal.set(2000, 9, 17);
        Instant ending1 = new DefaultInstant(propertiesInstant, new DefaultPosition(cal.getTime()));
        cal.set(2000, 1, 1);
        Instant begining2 = new DefaultInstant(propertiesInstant, new DefaultPosition(cal.getTime()));
        cal.set(2012, 1, 1);
        Instant ending2 = new DefaultInstant(propertiesInstant, new DefaultPosition(cal.getTime()));

        //-- map period
        NamedIdentifier namePeriod = new NamedIdentifier(Citations.CRS, "Period");
        final Map<String, Object> propertiesPeriod = new HashMap<>();
        propertiesPeriod.put(IdentifiedObject.NAME_KEY, namePeriod);
        
        Period epochOfUse1 = new DefaultPeriod(propertiesPeriod, begining1, ending1);
        Period epochOfUse2 = new DefaultPeriod(propertiesPeriod, begining2, ending2);
        
        final Map<String, Object> calendarEra1Prop = new HashMap<>();
        calendarEra1Prop.put(IdentifiedObject.NAME_KEY, new SimpleInternationalString("Cenozoic"));
        calendarEra1Prop.put(IdentifiedObject.IDENTIFIERS_KEY, new SimpleInternationalString("Cenozoic"));
        calendarEra1Prop.put(org.opengis.temporal.Calendar.REFERENCE_EVENT_KEY, new SimpleInternationalString("no description"));

        calendarEra1 = new DefaultCalendarEra(calendarEra1Prop, referenceDate1, julianReference, epochOfUse1);
        
        final Map<String, Object> calendarEra2Prop = new HashMap<>();
        calendarEra2Prop.put(IdentifiedObject.NAME_KEY, new SimpleInternationalString("Mesozoic"));
        calendarEra2Prop.put(IdentifiedObject.IDENTIFIERS_KEY, new SimpleInternationalString("Mesozoic"));
        calendarEra2Prop.put(org.opengis.temporal.Calendar.REFERENCE_EVENT_KEY, new SimpleInternationalString("no description"));
        calendarEra2 = new DefaultCalendarEra(calendarEra2Prop, referenceDate2, julianReference, epochOfUse2);
    }

    @After
    public void tearDown() {
        calendarEra1 = null;
        calendarEra2 = null;
    }

    /**
     * Test of getName method, of class DefaultCalendarEra.
     */
    @Test
    public void testGetName() {
        Identifier result = calendarEra1.getName();
        assertFalse(calendarEra2.getName().equals(result));
    }

    /**
     * Test of getReferenceEvent method, of class DefaultCalendarEra.
     */
    @Test
    public void testGetReferenceEvent() {
        InternationalString result = calendarEra1.getReferenceEvent();
        assertFalse(calendarEra2.getReferenceEvent().equals(result));
    }

    /**
     * Test of getReferenceDate method, of class DefaultCalendarEra.
     */
    @Test
    public void testGetReferenceDate() {
        CalendarDate result = calendarEra1.getReferenceDate();
        assertFalse(calendarEra2.getReferenceDate().equals(result));

    }

    /**
     * Test of getJulianReference method, of class DefaultCalendarEra.
     */
    @Test
    public void testGetJulianReference() {
        JulianDate result = calendarEra1.getJulianReference();
        assertEquals(calendarEra2.getJulianReference(), result);
    }

    /**
     * Test of getEpochOfUse method, of class DefaultCalendarEra.
     */
    @Test
    public void testGetEpochOfUse() {
        Period result = calendarEra1.getEpochOfUse();
        assertFalse(calendarEra2.getEpochOfUse().equals(result));
    }

//    /**
//     * Test of setName method, of class DefaultCalendarEra.
//     */
//    @Test
//    public void testSetName() {
//        InternationalString result = calendarEra1.getName();
//        ((DefaultCalendarEra)calendarEra1).setName(new SimpleInternationalString("new Era"));
//        assertFalse(calendarEra1.getName().equals(result));
//    }

    /**
     * Test of setReferenceEvent method, of class DefaultCalendarEra.
     */
    @Test
    public void testSetReferenceEvent() {
        InternationalString result = calendarEra1.getReferenceEvent();
        ((DefaultCalendarEra)calendarEra1).setReferenceEvent(new SimpleInternationalString("new Era description"));
        assertFalse(calendarEra1.getReferenceEvent().equals(result));
    }

    /**
     * Test of setReferenceDate method, of class DefaultCalendarEra.
     */
    @Test
    public void testSetReferenceDate() {
        CalendarDate result = calendarEra1.getReferenceDate();
        int[] date = {1950,6,10};
        ((DefaultCalendarEra)calendarEra1).setReferenceDate(new DefaultCalendarDate(null, null, null, date));
        assertFalse(calendarEra1.getReferenceDate().equals(result));
    }

    /**
     * Test of setJulianReference method, of class DefaultCalendarEra.
     */
    @Test
    public void testSetJulianReference() {
        JulianDate result = calendarEra1.getJulianReference();
        ((DefaultCalendarEra)calendarEra1).setJulianReference(new DefaultJulianDate(null, null, 785410));
        assertFalse(calendarEra1.getJulianReference().equals(result));
    }

    /**
     * Test of setEpochOfUse method, of class DefaultCalendarEra.
     */
    @Test
    public void testSetEpochOfUse() {
//        Period result = calendarEra1.getEpochOfUse();
//        cal.set(1900, 10, 10);
//        ((DefaultCalendarEra)calendarEra1).setEpochOfUse(new DefaultPeriod(new DefaultInstant(new DefaultPosition(cal.getTime())), new DefaultInstant(new DefaultPosition(new Date()))));
//        assertFalse(calendarEra1.getEpochOfUse().equals(result));
    }

    /**
     * Test of getDatingSystem method, of class DefaultCalendarEra.
     */
    @Test
    public void testGetDatingSystem() {
        Collection<org.opengis.temporal.Calendar> result = ((DefaultCalendarEra)calendarEra1).getDatingSystem();
        assertEquals(((DefaultCalendarEra)calendarEra2).getDatingSystem(),result);
    }

    /**
     * Test of equals method, of class DefaultCalendarEra.
     */
    @Test
    public void testEquals() {
        assertFalse(calendarEra1.equals(null));
        assertEquals(calendarEra1, calendarEra1);
        assertFalse(calendarEra1.equals(calendarEra2));
    }

    /**
     * Test of hashCode method, of class DefaultCalendarEra.
     */
    @Test
    public void testHashCode() {
        int result = calendarEra1.hashCode();
        assertFalse(calendarEra2.hashCode() == result);
    }

    /**
     * Test of toString method, of class DefaultCalendarEra.
     */
    @Test
    public void testToString() {
        String result = calendarEra1.toString();
        assertFalse(calendarEra2.toString().equals(result));
    }
}
