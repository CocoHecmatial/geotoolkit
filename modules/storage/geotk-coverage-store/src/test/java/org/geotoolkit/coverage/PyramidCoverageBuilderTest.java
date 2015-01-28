package org.geotoolkit.coverage;

import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.grid.GridCoverageBuilder;
import org.geotoolkit.coverage.memory.MPCoverageStore;
import org.geotoolkit.coverage.memory.MemoryCoverageStore;
import org.geotoolkit.feature.type.DefaultName;
import org.geotoolkit.feature.type.Name;
import org.geotoolkit.image.interpolation.InterpolationCase;
import org.geotoolkit.referencing.CRS;
import org.junit.Test;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test pyramid generation with PyramidCoverageBuilder.
 * TODO add more tests
 *
 * @author Quentin Boileau (Geomatys)
 */
public class PyramidCoverageBuilderTest {

    private static final Name NAME = new DefaultName("test");
    private static final CoordinateReferenceSystem CRS84 = CommonCRS.WGS84.normalizedGeographic();
    private static final CoordinateReferenceSystem EPSG4326;
    static {
        try {
            EPSG4326 = CRS.decode("EPSG:4326");
        } catch (NoSuchAuthorityCodeException ex) {
            throw new RuntimeException(ex);
        } catch (FactoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testPyramid () throws DataStoreException, TransformException, IOException, FactoryException {
        GeneralEnvelope env1 = new GeneralEnvelope(EPSG4326);
        env1.setRange(0, 0, +20);
        env1.setRange(1, 0, +20);
        AffineTransform af = new AffineTransform(0.1, 0, 0, -0.1, 0, 20);
        CoverageReference ref1 = createCoverage("cov1", env1, af, createImage(200, 200, Color.RED));

        final MPCoverageStore mpCovStore = new MPCoverageStore();
        final PyramidCoverageBuilder pcb = new PyramidCoverageBuilder(new Dimension(100, 100), InterpolationCase.NEIGHBOR, 2);
        final double[] fillValue = new double[4];

        final double[] scales = new double[]{0.1};
        final Map<Envelope, double[]> map = new HashMap<>();
        map.put(env1, scales);

        final Name name = new DefaultName("memory_store_test");
        pcb.create(ref1, mpCovStore, name, map, fillValue, null, null);

        //test reference
        CoverageReference outRef = mpCovStore.getCoverageReference(name);
        assertNotNull(outRef);
        assertTrue(outRef instanceof AbstractPyramidalCoverageReference);
        AbstractPyramidalCoverageReference outRefPy = (AbstractPyramidalCoverageReference) outRef;

        //test pyramids
        PyramidSet pyramidSet = outRefPy.getPyramidSet();
        assertNotNull(pyramidSet);
        Collection<Pyramid> pyramids = pyramidSet.getPyramids();
        assertEquals(1, pyramids.size());

        //test pyramid
        Pyramid pyramid = pyramids.iterator().next();
        assertTrue(CRS.equalsIgnoreMetadata(EPSG4326, pyramid.getCoordinateReferenceSystem()));
        assertArrayEquals(scales, pyramid.getScales(), 0.0000001);

        //test mosaics
        List<GridMosaic> mosaics = pyramid.getMosaics();
        assertEquals(1, mosaics.size());
        GridMosaic gridMosaic = mosaics.get(0);

        assertEquals(new Dimension(2,2), gridMosaic.getGridSize());

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                TileReference tile = gridMosaic.getTile(x, y, null);
                RenderedImage img = (RenderedImage) tile.getInput();
                testImage(img, 100, 100, Color.RED);
            }
        }

    }

    /**
     * Test overlaps coverages.
     * Try to write two overlapping coverage in the same pyramid using PyramidCoverageBuilder with
     * reuseTile parameter at true.
     * The result tiles should contain both data from the first coverage (Red) and th second coverage (Blue).
     */
    @Test
    public void testOverlaps () throws DataStoreException, TransformException, IOException, FactoryException {
        int tileSize = 100;

        GeneralEnvelope env1 = new GeneralEnvelope(EPSG4326);
        env1.setRange(0, 0, +20);
        env1.setRange(1, 0, +20);
        AffineTransform af1 = new AffineTransform(0.1, 0, 0, -0.1, 0, 20);
        CoverageReference ref1 = createCoverage("cov1", env1, af1, createImage(200, 200, Color.RED));

        GeneralEnvelope env2 = new GeneralEnvelope(EPSG4326);
        env2.setRange(0, +10, +30);
        env2.setRange(1, +10, +30);
        AffineTransform af2 = new AffineTransform(0.1, 0, 0, -0.1, +10, +30);
        CoverageReference ref2 = createCoverage("cov2", env2, af2, createImage(200, 200, Color.BLUE));

        /*
                 +-------+
                 |       |
            +----+ Blue  |
            |    |       |
            |    +---+---+
            | Red    |
            +--------+
         */

        final MPCoverageStore mpCovStore = new MPCoverageStore();
        final PyramidCoverageBuilder pcb = new PyramidCoverageBuilder(new Dimension(tileSize, tileSize), InterpolationCase.NEIGHBOR, 2, true);

        final GeneralEnvelope env = new GeneralEnvelope(EPSG4326);
        env.setRange(0, 0, +30);
        env.setRange(1, 0, +30);
        final double[] fillValue = new double[4];
        final double[] scales = new double[]{0.15};
        final Map<Envelope, double[]> map = new HashMap<>();
        map.put(env, scales);

        final Name name = new DefaultName("memory_store_test");
        //pyramid 1st coverage
        pcb.create(ref1, mpCovStore, name, map, fillValue, null, null);
        //append 2nd coverage
        pcb.create(ref2, mpCovStore, name, map, fillValue, null, null);

        //test reference
        CoverageReference outRef = mpCovStore.getCoverageReference(name);
        assertNotNull(outRef);
        assertTrue(outRef instanceof AbstractPyramidalCoverageReference);
        AbstractPyramidalCoverageReference outRefPy = (AbstractPyramidalCoverageReference) outRef;

        //test pyramids
        PyramidSet pyramidSet = outRefPy.getPyramidSet();
        assertNotNull(pyramidSet);
        Collection<Pyramid> pyramids = pyramidSet.getPyramids();
        assertEquals(1, pyramids.size());

        //test pyramid
        Pyramid pyramid = pyramids.iterator().next();
        assertTrue(CRS.equalsIgnoreMetadata(EPSG4326, pyramid.getCoordinateReferenceSystem()));
        assertArrayEquals(scales, pyramid.getScales(), 0.0000001);

        //test mosaics
        List<GridMosaic> mosaics = pyramid.getMosaics();
        assertEquals(1, mosaics.size());
        GridMosaic gridMosaic = mosaics.get(0);
        System.out.println(gridMosaic.getEnvelope());

        assertEquals(new Dimension(2,2), gridMosaic.getGridSize());

        //test tile 0x0
        /*
                  +---+
                  |   |
            +-----+ B |
            |  R  |   |
            +-----+---+
         */
        TileReference tile = gridMosaic.getTile(0, 0, null);
        RenderedImage tileImg = (RenderedImage) tile.getInput();
        BufferedImage expectedImage = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = expectedImage.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 66, tileSize, tileSize);
        g.setColor(Color.BLUE);
        g.fillRect(66, 0, tileSize, tileSize);
        testImage(tileImg, expectedImage, 0);

        //test tile 0x1
        /*
            +-----+---+
            |     | B |
            |  R  +---|
            |         |
            +---------+
         */
        tile = gridMosaic.getTile(0, 1, null);
        tileImg = (RenderedImage) tile.getInput();
        expectedImage = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        g = expectedImage.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, tileSize, tileSize);
        g.setColor(Color.BLUE);
        g.fillRect(66, 0, tileSize, 33);
        testImage(tileImg, expectedImage, 1);

        //test tile 1x0
         /*
            +---------+
            |         |
            |    B    |
            |         |
            +---------+
         */
        tile = gridMosaic.getTile(1, 0, null);
        tileImg = (RenderedImage) tile.getInput();
        testImage(tileImg, tileSize, tileSize, Color.BLUE);

        //test tile 1x1
        /*
            +---------+
            |    B    |
            +---+-----|
            | R |     |
            +---------+
         */
        tile = gridMosaic.getTile(1, 1, null);
        tileImg = (RenderedImage) tile.getInput();
        expectedImage = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        g = expectedImage.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 33, tileSize);
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, tileSize, 33);
        testImage(tileImg, expectedImage, 3);
    }

    private CoverageReference createCoverage(String name, GeneralEnvelope env, AffineTransform gridToCRS, RenderedImage image) throws DataStoreException {
        final GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setName(name);
        gcb.setEnvelope(env);
        gcb.setGridToCRS(gridToCRS);
        gcb.setRenderedImage(image);
        final GridCoverage2D coverage = gcb.getGridCoverage2D();
        final MemoryCoverageStore store = new MemoryCoverageStore(coverage);
        return store.getCoverageReference(store.getNames().iterator().next());
    }

    private static void testImage(RenderedImage img, int width, int height, Color fill){
        assertNotNull(img);
        assertEquals(img.getWidth(), width);
        assertEquals(img.getHeight(), height);
        final int[] color = new int[]{fill.getRed(),fill.getGreen(),fill.getBlue(),fill.getAlpha()};
        final int[] buffer = new int[4];
        final Raster data = img.getData();
        for(int y=0;y<height;y++){
            for(int x=0;x<width;x++){
                data.getPixel(x, y, buffer);
                assertArrayEquals(color, buffer);
            }
        }
    }

    private static void testImage(RenderedImage candidateImg, RenderedImage expectedImage, int nb)  {
        assertNotNull(candidateImg);
        assertEquals(expectedImage.getWidth(), candidateImg.getWidth());
        assertEquals(expectedImage.getHeight(), candidateImg.getHeight());
        final int[] expectedBuf = new int[4];
        final int[] candidateBuf = new int[4];
        final Raster expectedData = expectedImage.getData();
        final Raster candidateData = candidateImg.getData();
        for(int y=0;y<expectedImage.getHeight();y++){
            for(int x=0;x<expectedImage.getWidth();x++){
                expectedData.getPixel(x, y, expectedBuf);
                candidateData.getPixel(x, y, candidateBuf);
                assertArrayEquals(expectedBuf, candidateBuf);
            }
        }
    }

    private static BufferedImage createImage(int width, int height, Color color){
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        return image;
    }
}
