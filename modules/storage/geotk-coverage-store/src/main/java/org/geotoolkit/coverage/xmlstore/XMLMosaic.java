/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012-2014, Geomatys
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
package org.geotoolkit.coverage.xmlstore;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ProgressMonitor;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import net.iharder.Base64;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Classes;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.coverage.AbstractGridMosaic;
import org.geotoolkit.coverage.DefaultTileReference;
import org.geotoolkit.coverage.GridMosaic;
import org.geotoolkit.coverage.TileReference;
import org.geotoolkit.image.io.XImageIO;
import org.geotoolkit.util.BufferedImageUtilities;
import org.geotoolkit.util.StringUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
@XmlAccessorType(XmlAccessType.NONE)
public class XMLMosaic implements GridMosaic{

    private static final Logger LOGGER = Logging.getLogger(XMLMosaic.class);

    /** Executor used to write images */
    private static final RejectedExecutionHandler LOCAL_REJECT_EXECUTION_HANDLER = new ThreadPoolExecutor.CallerRunsPolicy();

    private static final BlockingQueue IMAGEQUEUE = new ArrayBlockingQueue(Runtime.getRuntime().availableProcessors()*2);

    private static final ThreadPoolExecutor TILEWRITEREXECUTOR = new ThreadPoolExecutor(
            0, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.MINUTES, IMAGEQUEUE, LOCAL_REJECT_EXECUTION_HANDLER);

    //empty tile informations
    private byte[] emptyTileEncoded = null;

    //written values
    @XmlElement
    double scale;
    @XmlElement
    double[] upperLeft;
    @XmlElement
    int gridWidth;
    @XmlElement
    int gridHeight;
    @XmlElement
    int tileWidth;
    @XmlElement
    int tileHeight;
    String existMask;
    String emptyMask;

    XMLPyramid pyramid = null;
    BitSet tileExist;
    BitSet tileEmpty;


    void initialize(XMLPyramid pyramid) {
        this.pyramid = pyramid;
        if (existMask != null && !existMask.isEmpty()) {
            try {
                tileExist = (existMask != null)
                        ? BitSet.valueOf(Base64.decode(existMask))
                        : new BitSet(gridWidth * gridHeight);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                tileExist = new BitSet(gridWidth * gridHeight);
            }
        } else {
            tileExist = new BitSet(gridWidth * gridHeight);
        }

        if (emptyMask != null && !emptyMask.isEmpty()) {
            try {
                tileEmpty = (emptyMask != null)
                        ? BitSet.valueOf(Base64.decode(emptyMask))
                        : new BitSet(gridWidth * gridHeight);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                tileEmpty = new BitSet(gridWidth * gridHeight);
            }
        } else {
            tileEmpty = new BitSet(gridWidth * gridHeight);
        }
    }

    private synchronized byte[] createEmptyTile(){
        if(emptyTileEncoded==null){
            //create an empty tile
            final List<XMLSampleDimension> dims = pyramid.getPyramidSet().getRef().getXMLSampleDimensions();
            final BufferedImage emptyTile;
            if(dims!=null && !dims.isEmpty()){
                emptyTile = BufferedImageUtilities.createImage(tileWidth, tileHeight, dims.size(), dims.get(0).getDataType());
            }else{
                emptyTile = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB);
            }


            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                ImageIO.write(emptyTile, pyramid.getPyramidSet().getFormatName(), out);
                out.flush();
            } catch (IOException ex) {
                Logger.getLogger(XMLMosaic.class.getName()).log(Level.SEVERE, null, ex);
            }
            emptyTileEncoded = out.toByteArray();
        }

        return emptyTileEncoded;
    }

    private void updateCompletionString() {
        existMask = Base64.encodeBytes(tileExist.toByteArray());
        emptyMask = Base64.encodeBytes(tileEmpty.toByteArray());
    }

    /**
     * Id equals scale string value
     */
    @Override
    public String getId() {
        final StringBuilder sb = new StringBuilder();
        sb.append(scale);
        for(int i=0;i<upperLeft.length;i++){
            sb.append('x');
            sb.append(upperLeft[i]);
        }
        //avoid local system formating
        return sb.toString().replace(DecimalFormatSymbols.getInstance().getDecimalSeparator(), 'd');
    }

    public File getFolder(){
        return new File(getPyramid().getFolder(),getId());
    }

    @Override
    public XMLPyramid getPyramid() {
        return pyramid;
    }

    @Override
    public DirectPosition getUpperLeftCorner() {
        final GeneralDirectPosition ul = new GeneralDirectPosition(getPyramid().getCoordinateReferenceSystem());
        for(int i=0;i<upperLeft.length;i++) ul.setOrdinate(i, upperLeft[i]);
        return ul;
    }

    @Override
    public Dimension getGridSize() {
        return new Dimension(gridWidth, gridHeight);
    }

    @Override
    public double getScale() {
        return scale;
    }

    @Override
    public Dimension getTileSize() {
        return new Dimension(tileWidth, tileHeight);
    }

    @Override
    public Envelope getEnvelope(){
        final GeneralDirectPosition ul = new GeneralDirectPosition(getUpperLeftCorner());
        final double minX = ul.getOrdinate(0);
        final double maxY = ul.getOrdinate(1);
        final double spanX = getTileSize().width * getGridSize().width * scale;
        final double spanY = getTileSize().height* getGridSize().height* scale;

        final GeneralEnvelope envelope = new GeneralEnvelope(ul,ul);
        envelope.setRange(0, minX, minX + spanX);
        envelope.setRange(1, maxY - spanY, maxY );

        return envelope;
    }

    @Override
    public Envelope getEnvelope(int col, int row) {
        final GeneralDirectPosition ul = new GeneralDirectPosition(getUpperLeftCorner());
        final double minX = ul.getOrdinate(0);
        final double maxY = ul.getOrdinate(1);
        final double spanX = getTileSize().width * scale;
        final double spanY = getTileSize().height * scale;

        final GeneralEnvelope envelope = new GeneralEnvelope(ul,ul);
        envelope.setRange(0, minX + col*spanX, minX + (col+1)*spanX);
        envelope.setRange(1, maxY - (row+1)*spanY, maxY - row*spanY);

        return envelope;
    }

    @Override
    public boolean isMissing(int col, int row) {
        return !tileExist.get(getTileIndex(col, row));
    }

    private boolean isEmpty(int col, int row){
        return tileEmpty.get(getTileIndex(col, row));
    }

    @Override
    public TileReference getTile(int col, int row, Map hints) throws DataStoreException {

        final TileReference tile;
        if(isEmpty(col, row)){
            try {
                tile = new DefaultTileReference(getPyramid().getPyramidSet().getReaderSpi(),
                        ImageIO.createImageInputStream(new ByteArrayInputStream(createEmptyTile())), 0, new Point(col, row));
            } catch (IOException ex) {
                throw new DataStoreException(ex);
            }
        }else{
            tile = new DefaultTileReference(getPyramid().getPyramidSet().getReaderSpi(),
                    getTileFile(col, row), 0, new Point(col, row));
        }

        return tile;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(Classes.getShortClassName(this));
        sb.append("   scale = ").append(getScale());
        sb.append("   gridSize[").append(getGridSize().width).append(',').append(getGridSize().height).append(']');
        sb.append("   tileSize[").append(getTileSize().width).append(',').append(getTileSize().height).append(']');
        return sb.toString();
    }

    public File getTileFile(int col, int row) throws DataStoreException{
        checkPosition(col, row);
        final String postfix = getPyramid().getPyramidSet().getReaderSpi().getFileSuffixes()[0];
        return new File(getFolder(),row+"_"+col+"."+postfix);
    }

    ImageWriter acquireImageWriter() throws IOException {
        return XImageIO.getWriterByFormatName(getPyramid().getPyramidSet().getFormatName(), null, null);
    }

    void createTile(int col, int row, RenderedImage image) throws DataStoreException {
        ImageWriter writer = null;
        try {
            writer = acquireImageWriter();
            createTile(col, row, image, writer);
        } catch (IOException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        } finally {
            if(writer != null){
                writer.dispose();
            }
        }
    }

    void createTile(final int col, final int row, final RenderedImage image, final ImageWriter writer) throws DataStoreException {
        if (isEmpty(image.getData())) {
            tileExist.set(getTileIndex(col, row), true);
            tileEmpty.set(getTileIndex(col, row), true);
            return;
        }

        checkPosition(col, row);
        final File f = getTileFile(col, row);
        f.getParentFile().mkdirs();

        ImageOutputStream out = null;
        try {
            final Class[] outTypes = writer.getOriginatingProvider().getOutputTypes();
            if(ArraysExt.contains(outTypes, File.class)){
                //writer support files directly, let him handle it
                writer.setOutput(f);
            }else{
                out = ImageIO.createImageOutputStream(f);
                writer.setOutput(out);
            }
            writer.write(image);
            final int ti = getTileIndex(col, row);
            tileExist.set(ti, true);
            tileEmpty.set(ti, false);
        } catch (IOException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        } finally {
            if (writer != null) {
                writer.setOutput(null);
            }
            if(out!=null){
                try {
                    out.close();
                } catch (IOException ex) {
                    throw new DataStoreException(ex);
                }
            }
        }
    }

    void writeTiles(final RenderedImage image, final boolean onlyMissing, final ProgressMonitor monitor) throws DataStoreException{
        final List<Future> futurs = new ArrayList<>();
        for(int y=0,ny=image.getNumYTiles(); y<ny; y++){
            for(int x=0,nx=image.getNumXTiles(); x<nx; x++){
                if (monitor != null && monitor.isCanceled()) {
                    // Stops submitting new thread
                    return;
                }

                if(onlyMissing && !isMissing(x, y)){
                    continue;
                }

                final int tileIndex = getTileIndex(x, y);
                checkPosition(x, y);

                final File f = getTileFile(x, y);
                f.getParentFile().mkdirs();
                Future fut = TILEWRITEREXECUTOR.submit(new TileWriter(f, image, x, y, tileIndex, image.getColorModel(), getPyramid().getPyramidSet().getFormatName(), monitor));
                futurs.add(fut);
            }
        }

        //wait for all writing tobe done
        for(Future f : futurs){
            try {
                f.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }

    }

    private void checkPosition(int col, int row) throws DataStoreException{
        if(col >= getGridSize().width || row >=getGridSize().height){
            throw new DataStoreException("Tile position is outside the grid : " + col +" "+row);
        }
    }

    private int getTileIndex(int col, int row){
        final int index = row*getGridSize().width + col;
        return index;
    }

    @XmlElement
    protected String getExistMask() {
        updateCompletionString();
        return emptyMask;
    }

    protected void setExistMask(String newValue) {
        emptyMask = newValue;
    }
    
    @XmlElement
    protected String getEmptyMask() {
        updateCompletionString();
        return emptyMask;
    }

    protected void setEmptyMask(String newValue) {
        emptyMask = newValue;
    }

    /**
     * check if image is empty
     */
    private static boolean isEmpty(Raster raster){
        double[] array = null;
        searchEmpty:
        for(int x=0,width=raster.getWidth(); x<width; x++){
            for(int y=0,height=raster.getHeight(); y<height; y++){
                array = raster.getPixel(x, y, array);
                for(double d : array){
                    if(d != 0){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public BlockingQueue<Object> getTiles(Collection<? extends Point> positions, Map hints) throws DataStoreException{
        return AbstractGridMosaic.getTiles(this, positions, hints);
    }

    private class TileWriter implements Runnable{

        private final File f;
        private final RenderedImage image;
        private final int idx;
        private final int idy;
        private final int tileIndex;
        private final ColorModel cm;
        private final String formatName;
        private final ProgressMonitor monitor;

        public TileWriter(File f,RenderedImage image, int idx, int idy, int tileIndex, ColorModel cm, String formatName, ProgressMonitor monitor) {
            ArgumentChecks.ensureNonNull("file", f);
            ArgumentChecks.ensureNonNull("image", image);
            this.f = f;
            this.image = image;
            this.idx = idx;
            this.idy = idy;
            this.tileIndex = tileIndex;
            this.cm = cm;
            this.formatName = formatName;
            this.monitor = monitor;
        }

        @Override
        public void run() {
            // Stops writing tile if process cancelled
            if (monitor != null && monitor.isCanceled()) {
                return;
            }

            ImageWriter writer = null;
            ImageOutputStream out = null;
            try{
                Raster raster = image.getTile(idx, idy);

                //check if image is empty
                if(raster == null || isEmpty(raster)){
                    synchronized(tileExist){
                        tileExist.set(tileIndex, true);
                    }
                    synchronized(tileEmpty){
                        tileEmpty.set(tileIndex, true);
                    }
                    return;
                }
                
                writer = ImageIO.getImageWritersByFormatName(formatName).next();
                
                final Class[] outTypes = writer.getOriginatingProvider().getOutputTypes();
                if(ArraysExt.contains(outTypes, File.class)){
                    //writer support files directly, let him handle it
                    writer.setOutput(f);
                }else{
                    out = ImageIO.createImageOutputStream(f);
                    writer.setOutput(out);
                }                

                final boolean canWriteRaster = writer.canWriteRasters();
                //write tile
                if(canWriteRaster){
                    final IIOImage buffer = new IIOImage(raster, null, null);
                    writer.write(buffer);
                }else{
                    //encapsulate image in a buffered image with parent color model
                    final BufferedImage buffer = new BufferedImage(
                            cm, (WritableRaster)raster, true, null);
                    writer.write(buffer);
                }

                synchronized(tileExist){
                    tileExist.set(tileIndex, true);
                }
                synchronized(tileEmpty){
                    tileEmpty.set(tileIndex, false);
                }

            }catch(Exception ex){
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                throw new RuntimeException(ex.getMessage(),ex);
            }finally{
                if(writer != null){
                    writer.dispose();
                    if(out != null){
                        try {
                            out.close();
                        } catch (IOException ex) {
                            Logger.getLogger(XMLMosaic.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }

        }

    }

}
