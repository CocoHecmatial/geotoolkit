/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.image.io;

import java.awt.Point;
import java.util.List;
import java.util.Arrays;

import org.junit.*;

import static org.junit.Assert.*;
import static org.geotoolkit.test.Commons.*;
import static org.geotoolkit.image.io.DimensionSlice.API.*;


/**
 * Tests {@link SpatialImageReadParam}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.08
 *
 * @since 3.08
 */
public final class SpatialImageReadParamTest {
    /**
     * Tests the API enumeration.
     */
    @Test
    public void testAPI() {
        int index = 0;
        for (final DimensionSlice.API api : DimensionSlice.API.VALIDS) {
            assertEquals("Array of valid enum is not consistent with ordinal values.", index++, api.ordinal());
        }
    }

    /**
     * Ensures that setting an API to BANDS has the side-effect of setting
     * the source bands to {0}.
     */
    @Test
    public void testAssignBands() {
        final SpatialImageReadParam param = new SpatialImageReadParam(null);
        assertNull(param.getDimensionSliceForAPI(BANDS));
        assertNull(param.getSourceBands());

        final DimensionSlice bandsSlice = param.newDimensionSlice();
        bandsSlice.setAPI(BANDS);
        assertSame(bandsSlice, param.getDimensionSliceForAPI(BANDS));
        assertArrayEquals(new int[1], param.getSourceBands());

        param.setSourceBands(new int[] {8, 4, 2});
        assertEquals(8, bandsSlice.getSliceIndex());
    }

    /**
     * Tests setting indices and API in dimension slices.
     */
    @Test
    public void testDimensionSlice() {
        final SpatialImageReadParam param = new SpatialImageReadParam(null);
        final DimensionSlice timeSlice = param.newDimensionSlice();
        timeSlice.addDimensionId("time");
        timeSlice.setSliceIndex(20);

        final DimensionSlice depthSlice = param.newDimensionSlice();
        depthSlice.addDimensionId("depth");
        depthSlice.setSliceIndex(25);

        assertSame(timeSlice,  param.getDimensionSlice("time"));
        assertSame(depthSlice, param.getDimensionSlice("depth"));
        assertNull(            param.getDimensionSlice("dummy"));

        assertEquals(20, param.getSliceIndex("time"));
        assertEquals(25, param.getSliceIndex("depth"));
        assertEquals( 0, param.getSliceIndex("dummy"));
        /*
         * Simulates NetCDF file having longitude, latitude, depth and time dimensions.
         * The longitude and latitude are unknown for now, but the depth and time should
         * be recognized.
         */
        final List<String> AXES = Arrays.asList("longitude", "latitude", "depth", "time");
        assertNull(param.getDimensionSliceForAPI(COLUMNS));
        assertNull(param.getDimensionSliceForAPI(ROWS));
        assertNull(param.getDimensionSliceForAPI(BANDS));
        assertNull(param.getDimensionSliceForAPI(IMAGES));
        assertNull(param.getDimensionSliceForAPI(NONE));
        assertEquals(NONE, timeSlice .getAPI());
        assertEquals(NONE, depthSlice.getAPI());
        assertNull(param.getSourceBands());

        timeSlice.setAPI(BANDS);
        assertNull  (   param.getDimensionSliceForAPI(COLUMNS));
        assertNull  (   param.getDimensionSliceForAPI(ROWS));
        assertEquals(3, param.getDimensionSliceForAPI(BANDS).findDimensionIndex(AXES));
        assertNull  (   param.getDimensionSliceForAPI(IMAGES));
        assertNull  (   param.getDimensionSliceForAPI(NONE));
        assertEquals(BANDS, timeSlice .getAPI());
        assertEquals(NONE,  depthSlice.getAPI());
        assertArrayEquals(new int[] {20}, param.getSourceBands());

        depthSlice.setAPI(BANDS);
        assertNull  (   param.getDimensionSliceForAPI(COLUMNS));
        assertNull  (   param.getDimensionSliceForAPI(ROWS));
        assertEquals(2, param.getDimensionSliceForAPI(BANDS).findDimensionIndex(AXES));
        assertNull  (   param.getDimensionSliceForAPI(IMAGES));
        assertNull  (   param.getDimensionSliceForAPI(NONE));
        assertEquals(NONE,  timeSlice .getAPI());
        assertEquals(BANDS, depthSlice.getAPI());
        assertArrayEquals(new int[] {25}, param.getSourceBands());

        assertNull("Should not have been set by any of the above.", param.getSourceRegion());
        assertEquals("Should not have been set by any of the above.", new Point(), param.getDestinationOffset());

        assertMultilinesEquals(decodeQuotes(
            "SpatialImageReadParam[sourceBands={25}]\n" +
            "  ├─ DimensionSlice[id={“time”}, sliceIndex=20]\n" +
            "  └─ DimensionSlice[id={“depth”}, sliceIndex=25, API=BANDS]"), param.toString());

        depthSlice.removeDimensionId("depth");
        assertMultilinesEquals(decodeQuotes(
            "SpatialImageReadParam[sourceBands={25}]\n" +
            "  └─ DimensionSlice[id={“time”}, sliceIndex=20]"), param.toString());
        /*
         * Now adds name to the (latitude, longitude) dimensions.
         */
        param.newDimensionSlice().addDimensionId("longitude");
        param.newDimensionSlice().addDimensionId("latitude");
        param.getDimensionSlice("longitude").setAPI(COLUMNS);
        param.getDimensionSlice("latitude" ).setAPI(ROWS);
        assertMultilinesEquals(decodeQuotes(
            "SpatialImageReadParam[sourceBands={25}]\n" +
            "  ├─ DimensionSlice[id={“time”}, sliceIndex=20]\n" +
            "  ├─ DimensionSlice[id={“longitude”}, sliceIndex=0, API=COLUMNS]\n" +
            "  └─ DimensionSlice[id={“latitude”}, sliceIndex=0, API=ROWS]"), param.toString());

        param.getDimensionSlice("longitude").setSliceIndex(100);
        param.getDimensionSlice("latitude" ).setSliceIndex(200);
        param.setDestinationOffset(new Point(2,3));
        assertMultilinesEquals(decodeQuotes(
            "SpatialImageReadParam[sourceRegion=(100,200 : 1,1), sourceBands={25}, destinationOffset=(2,3)]\n" +
            "  ├─ DimensionSlice[id={“time”}, sliceIndex=20]\n" +
            "  ├─ DimensionSlice[id={“longitude”}, sliceIndex=100, API=COLUMNS]\n" +
            "  └─ DimensionSlice[id={“latitude”}, sliceIndex=200, API=ROWS]"), param.toString());
    }
}
