/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Geomatys
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

package org.geotoolkit.display2d.canvas;

import java.util.Collections;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.Date;

import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.service.DefaultPortrayalService;
import org.apache.sis.geometry.GeneralEnvelope;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import static org.junit.Assert.*;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.CommonCRS;

/**
 * Test envelope configuration on J2DCanvas.
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class J2DCanvasTest {

    public J2DCanvasTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

     @Test
     public void test4DEnvelope() throws Exception {
         final J2DCanvas canvas = new J2DCanvasBuffered(CommonCRS.WGS84.normalizedGeographic(), new Dimension(800,600));

         //check size
         assertTrue( canvas.getDisplayBounds().getBounds().width == 800 );
         assertTrue( canvas.getDisplayBounds().getBounds().height == 600 );

         //check sended temporal and elevation
         Date[] temps = canvas.getTemporalRange();
         Double[] elev = canvas.getElevationRange();
         assertTrue(temps == null);
         assertTrue(elev == null);

         CoordinateReferenceSystem crs = new DefaultCompoundCRS(
               Collections.singletonMap(DefaultCompoundCRS.NAME_KEY, "WGS84-4D"),
               CommonCRS.WGS84.normalizedGeographic(),
               CommonCRS.Vertical.ELLIPSOIDAL.crs(),
               CommonCRS.Temporal.JAVA.crs());

         final GeneralEnvelope env = new GeneralEnvelope(crs);
         env.setRange(0, -170, 170);
         env.setRange(1, -80, 80);
         env.setRange(2, -50, 150);
         env.setRange(3, 3000, 6000);

         canvas.setObjectiveCRS(crs);
         canvas.setVisibleArea(env);

         temps = canvas.getTemporalRange();
         elev = canvas.getElevationRange();
         assertTrue(temps[0] != null);
         assertTrue(temps[1] != null);
         assertTrue(elev[0] != null);
         assertTrue(elev[1] != null);
         assertTrue(elev[0] == -50);
         assertTrue(elev[1] == 150);
         assertTrue(temps[0].getTime() == 3000);
         assertTrue(temps[1].getTime() == 6000);

     }

     @Test
     public void testCreationWith4Denvelope() throws PortrayalException{

         CoordinateReferenceSystem crs = new DefaultCompoundCRS(
               Collections.singletonMap(DefaultCompoundCRS.NAME_KEY, "WGS84-4D"),
               CommonCRS.WGS84.normalizedGeographic(),
               CommonCRS.Vertical.ELLIPSOIDAL.crs(),
               CommonCRS.Temporal.JAVA.crs());

         final GeneralEnvelope env = new GeneralEnvelope(crs);
         env.setRange(0, -170, 170);
         env.setRange(1, -80, 80);
         env.setRange(2, -50, 150);
         env.setRange(3, 3000, 6000);

         MapContext context = MapBuilder.createContext(CommonCRS.WGS84.normalizedGeographic());

         //was raising an error since we asked a 4D envelope with a 2D context
         //the canvas should change the crs to 2D to pass this test
         DefaultPortrayalService.portray(context, env, new Dimension(800, 600), true);

     }

     @Test
     public void testObjectToDisplayTrs() throws Exception{
         final J2DCanvas canvas = new J2DCanvasBuffered(CommonCRS.WGS84.normalizedGeographic(), new Dimension(360,180));
         final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
         env.setRange(0, -180, +180);
         env.setRange(1, -90, +90);
         canvas.setVisibleArea(env);

         final AffineTransform2D objtoDisp = canvas.getObjectiveToDisplay();
         assertEquals(new AffineTransform2D(1, 0, 0, -1, 180, 90), objtoDisp);
     }

     @Test
     public void testCenterTransform() throws NoninvertibleTransformException, TransformException{
         final J2DCanvas canvas = new J2DCanvasBuffered(CommonCRS.WGS84.normalizedGeographic(), new Dimension(360,180));
         final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
         env.setRange(0, -180, +180);
         env.setRange(1, -90, +90);
         canvas.setVisibleArea(env);

         final AffineTransform2D objtoDisp = canvas.getObjectiveToDisplay();
         final AffineTransform centerTrs = canvas.getCenterTransform();
         assertEquals(new AffineTransform2D(1, 0, 0, -1, 0, 0), centerTrs);

         //reset it and check
         canvas.setCenterTransform(centerTrs);
         final AffineTransform objToDisp2 = canvas.getObjectiveToDisplay();
         assertEquals(objtoDisp, objToDisp2);
     }

}
