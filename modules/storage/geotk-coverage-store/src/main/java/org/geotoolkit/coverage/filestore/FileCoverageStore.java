/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
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
package org.geotoolkit.coverage.filestore;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageReaderSpi;

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.storage.coverage.AbstractCoverageStore;
import org.geotoolkit.storage.coverage.CoverageStoreFactory;
import org.geotoolkit.storage.coverage.CoverageStoreFinder;
import org.geotoolkit.storage.coverage.CoverageType;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.image.io.NamedImageStore;
import org.geotoolkit.image.io.UnsupportedImageFormatException;
import org.geotoolkit.image.io.XImageIO;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.utility.parameter.ParametersExt;
import org.geotoolkit.storage.DataFileStore;
import org.geotoolkit.storage.DataNode;
import org.geotoolkit.storage.DefaultDataNode;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Coverage Store which rely on standard java readers and writers.
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class FileCoverageStore extends AbstractCoverageStore implements DataFileStore {

    private static final Logger LOGGER = Logging.getLogger("org.geotoolkit.coverage.filestore");

    private static final String REGEX_SEPARATOR;
    static {
        if (File.separatorChar == '\\') {
            REGEX_SEPARATOR = "\\\\";
        } else {
            REGEX_SEPARATOR = File.separator;
        }
    }

    private final Path root;
    private final String format;
    private final URI rootPath;

    private final String separator;

    private final DataNode rootNode = new DefaultDataNode();

    //default spi
    final ImageReaderSpi spi;

    public FileCoverageStore(URL url, String format) throws URISyntaxException, IOException {
        this(toParameters(url.toURI(), format));
    }

    public FileCoverageStore(Path path, String format) throws URISyntaxException, IOException {
        this(toParameters(path.toUri(), format));
    }

    public FileCoverageStore(URI uri, String format) throws URISyntaxException, IOException {
        this(toParameters(uri, format));
    }

    public FileCoverageStore(ParameterValueGroup params) throws URISyntaxException, IOException {
        super(params);
        rootPath = (URI) params.parameter(FileCoverageStoreFactory.PATH.getName().getCode()).getValue();
        root = Paths.get(rootPath);
        format = (String) params.parameter(FileCoverageStoreFactory.TYPE.getName().getCode()).getValue();

        if("AUTO".equalsIgnoreCase(format)){
            spi = null;
        }else{
            spi = XImageIO.getReaderSpiByFormatName(format);
        }

        separator = Parameters.value(FileCoverageStoreFactory.PATH_SEPARATOR, params);

        visit(root);
    }

    private static ParameterValueGroup toParameters(URI uri, String format){
        final ParameterValueGroup params = FileCoverageStoreFactory.PARAMETERS_DESCRIPTOR.createValue();
        ParametersExt.getOrCreateValue(params,"path").setValue(uri);
        if(format!=null){
            ParametersExt.getOrCreateValue(params,"type").setValue(format);
        }
        return params;
    }

    @Override
    public CoverageStoreFactory getFactory() {
        return CoverageStoreFinder.getFactoryById(FileCoverageStoreFactory.NAME);
    }

    @Override
    public DataNode getRootNode() {
        return rootNode;
    }

    /**
     * Visit all files and directories contained in the directory specified.
     *
     * @param file
     */
    private void visit(final Path file) throws IOException {

        Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                test(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String createLayerName(final Path candidate) {
        if (separator != null) {
            //TODO use relativize()
            final Path absRoot = root.toAbsolutePath();
            final Path abscandidate = candidate.toAbsolutePath();
            String fullName = abscandidate.toString().replace(absRoot.toString(), "");
            if (fullName.startsWith(File.separator)) {
                fullName = fullName.substring(1);
            }
            fullName = fullName.replaceAll(REGEX_SEPARATOR, separator);
            final int idx = fullName.lastIndexOf('.');
            return fullName.substring(0, idx);
        } else {
            return IOUtilities.filenameWithoutExtension(candidate);
        }
    }

    /**
     *
     * @param candidate Candidate to be a image file.
     */
    private void test(final Path candidate) {
        if(!Files.isRegularFile(candidate)){
            return;
        }
        final String candidateName = IOUtilities.filename(candidate);
        ImageReader reader = null;
        try {
            //don't comment this block, This raise an error if no reader for the file can be found
            //this way we are sure that the file is an image.
            reader = createReader(candidate, spi);

            final String nmsp = getDefaultNamespace();
            final String filename = createLayerName(candidate);

            final int nbImage = reader.getNumImages(true);

//            final Name baseName = new DefaultName(nmsp,filename);
//            final FileCoverageReference baseNode = new FileCoverageReference(this,baseName,candidate,-1);
//            rootNode.getChildren().add(baseNode);

            if (reader instanceof NamedImageStore) {
                //try to find a proper name for each image
                final NamedImageStore nis = (NamedImageStore) reader;

                final List<String> imageNames = nis.getImageNames();
                for (int i = 0, n = imageNames.size(); i < n; i++) {
                    final String in = imageNames.get(i);
                    final GenericName name = NamesExt.create(nmsp, filename + "." + in);
                    final FileCoverageReference fcr = new FileCoverageReference(this, name, candidate, i);
                    rootNode.getChildren().add(fcr);
                }

            } else {
                for (int i = 0; i < nbImage; i++) {
                    final GenericName name;
                    if (nbImage == 1) {
                        //don't number it if there is only one
                        name = NamesExt.create(nmsp, filename);
                    } else {
                        name = NamesExt.create(nmsp, filename + "." + i);
                    }

                    final FileCoverageReference fcr = new FileCoverageReference(this, name, candidate, i);
                    rootNode.getChildren().add(fcr);
                }
            }
            // Tried to parse a incompatible file, not really an error.
        } catch (UnsupportedImageFormatException ex) {
            LOGGER.log(Level.FINE, "Error for file {0} : {1}", new Object[]{candidateName, ex.getMessage()});

        } catch (Exception ex) {
            //Exception type is not specified cause we can get IOException as IllegalArgumentException.
            LOGGER.log(Level.WARNING, String.format("Error for file %s : %s", candidateName, ex.getMessage()), ex);
        } finally {
            XImageIO.disposeSilently(reader);
        }
    }

    @Override
    public void close() {
    }

    /**
     * Create a reader for the given file.
     * Detect automatically the spi if type is set to 'AUTO'.
     *
     * @param candidate file to read
     * @param spi used to create ImageReader. If null, detect automatically from candidate file.
     * @return ImageReader, never null
     * @throws IOException if fail to create a reader.
     * @throws UnsupportedImageFormatException if spi is defined but can't decode candidate file
     */
    ImageReader createReader(final Path candidate, ImageReaderSpi spi) throws IOException{
        final ImageReader reader;
        if(spi == null){
            if (!IOUtilities.extension(candidate).isEmpty()) {
                reader = XImageIO.getReaderBySuffix(candidate, Boolean.FALSE, Boolean.FALSE);
            } else {
                reader = XImageIO.getReader(candidate,Boolean.FALSE,Boolean.FALSE);
            }
        }else{
            if (spi.canDecodeInput(candidate)) {
                reader = spi.createReaderInstance();
                Object in = XImageIO.toSupportedInput(spi, candidate);
                reader.setInput(in);
            } else {
                throw new UnsupportedImageFormatException("Unsupported file input for spi "+spi.getPluginClassName());
            }
        }

        return reader;
    }

    /**
     * Create a writer for the given file.
     * Detect automatically the spi if type is set to 'AUTO'.
     *
     * @param candidate
     * @return ImageWriter, never null
     * @throws IOException if fail to create a writer.
     */
    ImageWriter createWriter(final Path candidate) throws IOException{
        final ImageReaderSpi readerSpi = createReader(candidate,spi).getOriginatingProvider();
        final String[] writerSpiNames = readerSpi.getImageWriterSpiNames();
        if(writerSpiNames == null || writerSpiNames.length == 0){
            throw new IOException("No writer for this format.");
        }

        return XImageIO.getWriterByFormatName(readerSpi.getFormatNames()[0], candidate, null);
    }

    @Override
    public CoverageType getType() {
        return CoverageType.GRID;
    }

    @Override
    public Path[] getDataFiles() throws DataStoreException {
        return new Path[] {root};
    }
}
