/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2012, Geomatys
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
package org.geotoolkit.build.project.report;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.io.File;
import java.io.Writer;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.geotoolkit.internal.io.IOUtilities;


/**
 * Base classes of tools that automatically generate javadoc comments.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.20
 *
 * @since 3.20
 */
public abstract class JavadocUpdater {
    /**
     * The encoding of source files.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * The signature that indicates where to insert the comments.
     */
    private static final String SIGNATURE = "<!-- GENERATED PARAMETERS";

    /**
     * The lines in HTML formats, without carriage returns. All {@code createFoo(...)} methods
     * defined in this class will append lines in HTML format to this list. After the list has
     * been completed, its content can be printed directly (for example by {@link #toString()},
     * or can be prefixed by the {@code " * "} characters of the lines are to be inserted in a
     * class Javadoc.
     */
    final List<String> lines;

    /**
     * The project root.
     */
    private final File root;

    /**
     * For subclass constructors only.
     */
    JavadocUpdater() throws IOException {
        lines = new ArrayList<String>();
        File file = IOUtilities.toFile(JavadocUpdater.class.getResource("JavadocUpdater.class"), null);
        while (file != null) {
            if (new File(file, "pom.xml").isFile() &&
                new File(file, "modules").isDirectory() &&
                new File(file, "demos")  .isDirectory() &&
                new File(file, "build")  .isDirectory())
            {
                root = file;
                return;
            }
            file = file.getParentFile();
        }
        throw new IOException("Project root not found.");
    }

    /**
     * Returns the outer class.
     */
    private static Class<?> getOuterClass(Class<?> classe) {
        while (classe != null) {
            final Class<?> enclosing = classe.getEnclosingClass();
            if (enclosing == null) break;
            classe = enclosing;
        }
        return classe;
    }

    /**
     * Updates the given class with the current content of {@link #lines}.
     */
    final void update(final String module, final Class<?> classe) throws IOException {
        // Where to put the updated code.
        final StringBuilder buffer = new StringBuilder();

        // What to search as an indication of the begining of the section to modify.
        final Pattern classSignature = Pattern.compile(".*\\bclass\\s+" + classe.getSimpleName() + "\\b.*");
        boolean foundClassSignature = false;

        final File file = new File(root, "modules/" + module + "/src/main/java/" +
                getOuterClass(classe).getCanonicalName().replace('.', '/') + ".java");
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), ENCODING));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                buffer.append(line).append('\n');
                if (!foundClassSignature) {
                    foundClassSignature = classSignature.matcher(line).matches();
                } else {
                    if (line.contains(SIGNATURE)) {
                        final String margin = line.substring(0, line.indexOf('*') + 2);
                        for (final String gen : lines) {
                            buffer.append(margin).append(gen).append('\n');
                        }
                    }
                }
            }
        } finally {
            in.close();
        }
        // Write the result.
        final Writer out = new OutputStreamWriter(new FileOutputStream(file), ENCODING);
        try {
            out.write(buffer.toString());
        } finally {
            out.close();
        }
    }

    /**
     * Returns the HTML code for debugging purpose
     */
    @Override
    public String toString() {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final StringBuilder buffer = new StringBuilder();
        for (final String line : lines) {
            buffer.append(line).append(lineSeparator);
        }
        return buffer.toString();
    }
}
