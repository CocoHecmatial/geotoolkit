/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2002-2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.referencing.operation;

import java.io.LineNumberReader;
import org.geotoolkit.test.TestData;

import org.junit.*;


/**
 * Run the test scripts. Each script contains a list of source and target coordinates
 * reference systems (in WKT), source coordinate points and expected coordinate points
 * after the transformation from source CRS to target CRS.
 *
 * @author Yann Cézard (IRD)
 * @author Rémi Eve (IRD)
 * @author Martin Desruisseaux (IRD)
 * @version 3.0
 *
 * @since 2.1
 *
 * @todo Not yet enabled because the hook to {@code DefaultCoordinateOperationFactory}
 *       are not yet there.
 */
public final class Script3DTest {
    /**
     * Runs the specified test script.
     *
     * @throws Exception If a test failed.
     */
    private void runScript(final String filename) throws Exception {
        final LineNumberReader in = TestData.openReader(this, filename);
        final ScriptRunner3D test = new ScriptRunner3D(in);
        test.run();
        in.close();
        if (test.firstError != null) {
            throw test.firstError;
        }
    }

    /**
     * Run {@code "OpenGIS.txt"}.
     *
     * @throws Exception If a test failed.
     */
    @Test
    @Ignore
    public void testOpenGIS() throws Exception {
        runScript("OpenGIS.txt");
    }
}
