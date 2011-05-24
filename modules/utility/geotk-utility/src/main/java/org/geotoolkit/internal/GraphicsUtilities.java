/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2001-2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2011, Geomatys
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
package org.geotoolkit.internal;

import java.awt.Graphics2D;
import javax.swing.UIManager;

import org.geotoolkit.lang.Static;
import org.geotoolkit.util.logging.Logging;


/**
 * A set of utilities methods for painting in a {@link Graphics2D} handle.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.16
 *
 * @since 2.0
 * @module
 */
public final class GraphicsUtilities extends Static {
    /**
     * The creation of {@code GraphicsUtilities} class objects is forbidden.
     */
    private GraphicsUtilities() {
    }

    /**
     * Sets the Swing Look and Feel to the default value used in Geotk. This method exists
     * in order to have a central place where this setting can be performed, so we can change
     * the setting in a consistent fashion for the whole library.
     *
     * @param caller The class calling this method. Used only for logging purpose.
     * @param method The method invoking this one.  Used only for logging purpose.
     */
    public static void setLookAndFeel(final Class<?> caller, final String method) {
        String laf = System.getProperty("swing.defaultlaf"); // Documented in UIManager.
        if (laf != null) {
            if (laf.equalsIgnoreCase("Nimbus")) {
                laf = getNimbusLAF();
            } else {
                // Do not change the user-supplied setting.
                return;
            }
        } else if (OS.current() == OS.MAC_OS) {
            // MacOS come with a default L&F which is different than in standard JDK.
            return;
        } else {
            laf = UIManager.getSystemLookAndFeelClassName();
        }
        if (laf.equals(UIManager.getCrossPlatformLookAndFeelClassName())) {
            laf = getNimbusLAF(); // Replace Metal L&F by Nimbus L&F.
        }
        if (laf != null) try {
            UIManager.setLookAndFeel(laf);
        } catch (Exception e) {
            Logging.recoverableException(caller, method, e);
        }
    }

    /**
     * Returns the Nimbus L&F, or {@code null} if not found.
     */
    private static String getNimbusLAF() {
        for (final UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if (info.getName().equalsIgnoreCase("Nimbus")) {
                return info.getClassName();
            }
        }
        return null;
    }
}
