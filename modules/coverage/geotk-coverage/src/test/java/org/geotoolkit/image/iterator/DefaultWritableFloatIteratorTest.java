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
package org.geotoolkit.image.iterator;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;

/**
 * Test DefaultWritableFloatIterator class.
 *
 * @author Rémi Maréchal (Geomatys).
 */
public class DefaultWritableFloatIteratorTest extends DefaultWritableTest{

    float[] tabRef, tabTest;

    /**
     * {@inheritDoc }
     */
    @Override
    protected int getDataBufferType() {
        return DataBuffer.TYPE_FLOAT;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected void setRasterTest(int minx, int miny, int width, int height, int numBand, Rectangle subArea) {
        int comp = 0;
        rasterTest = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width, height, numBand, new Point(minx, miny));
        for (int y = miny; y<miny + height; y++) {
            for (int x = minx; x<minx + width; x++) {
                for (int b = 0; b<numBand; b++) {
                    rasterTest.setSample(x, y, b, comp++ -32000.5);
                }
            }
        }
        int mx, my, w,h;
        if (subArea == null) {
            mx = minx;
            my = miny;
            w = width;
            h = height;

        } else {
            mx = Math.max(minx, subArea.x);
            my = Math.max(miny, subArea.y);
            w  = Math.min(minx + width, subArea.x + subArea.width) - mx;
            h  = Math.min(miny + height, subArea.y + subArea.height) - my;
        }

        final int length = w * h * numBand;
        tabRef  = new float[length];
        tabTest = new float[length];
        comp = 0;
        for (int y = my; y<my + h; y++) {
            for (int x = mx; x<mx + w; x++) {
                for (int b = 0; b<numBand; b++) {
                    tabRef[comp++] =  (float) (b + ((x-minx) + (y-miny) * width) * numBand-32000.5);
                }
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected void setTabTestValue(int index, double value) {
        tabTest[index] = (float) value;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected boolean compareTab() {
        return compareTab(tabRef, tabTest);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected void setRenderedImgTest(int minx, int miny, int width, int height, int tilesWidth, int tilesHeight, int numBand, Rectangle areaIterate) {
        final BandedSampleModel sampleM = new BandedSampleModel(DataBuffer.TYPE_FLOAT, tilesWidth, tilesHeight, numBand);
        renderedImage = new TiledImage(minx, miny, width, height, minx+tilesWidth, miny+tilesHeight, sampleM, null);

        int comp;
        int nbrTX = width/tilesWidth;
        int nbrTY = height/tilesHeight;
        int val;
        for(int j = 0;j<nbrTY;j++){
            for(int i = 0; i<nbrTX;i++){
                val = 0;
                for (int y = miny+j*tilesHeight, ly = y+tilesHeight; y<ly; y++) {
                    for (int x = minx+i*tilesWidth, lx = x + tilesWidth; x<lx; x++) {
                        for (int b = 0; b<numBand; b++) {
                            renderedImage.setSample(x, y, b, val++ - 32000.5);
                        }
                    }
                }
            }
        }

        int cULX, cULY, cBRX, cBRY, minIX = 0, minIY = 0, maxIX = 0, maxIY = 0;
        int tileMinX, tileMinY, tileMaxX, tileMaxY;
        int rastminY, rastminX, rastmaxY, rastmaxX, depX, depY, endX, endY, tabLenght;

        if (areaIterate != null) {
            cULX = areaIterate.x;
            cULY = areaIterate.y;
            cBRX = cULX + areaIterate.width;
            cBRY = cULY + areaIterate.height;
            minIX = Math.max(cULX, minx);
            minIY = Math.max(cULY, miny);
            maxIX = Math.min(cBRX, minx + width);
            maxIY = Math.min(cBRY, miny + height);
            tabLenght = Math.abs((maxIX-minIX)*(maxIY-minIY)) * numBand;
            tileMinX = (minIX - minx) / tilesWidth;
            tileMinY = (minIY - miny) / tilesHeight;
            tileMaxX = (maxIX - minx) / tilesWidth;
            tileMaxY = (maxIY - miny) / tilesHeight;
        } else {
            tileMinX = tileMinY = 0;
            tileMaxX = width/tilesWidth;
            tileMaxY = height/tilesHeight;
            tabLenght = width*height*numBand;
        }

        tabRef  = new float[tabLenght];
        tabTest = new float[tabLenght];
        comp = 0;
        for (int tileY = tileMinY; tileY<tileMaxY; tileY++) {
            rastminY = tileY * tilesHeight;
            rastmaxY = rastminY + tilesHeight;
            for (int tileX = tileMinX; tileX<tileMaxX; tileX++) {
                //tile by tile
                rastminX = tileX * tilesWidth;
                rastmaxX = rastminX + tilesWidth;
                if (areaIterate == null) {
                    depX = rastminX;
                    depY = rastminY;
                    endX = rastmaxX;
                    endY = rastmaxY;
                } else {
                    depX = Math.max(rastminX, minIX);
                    depY = Math.max(rastminY, minIY);
                    endX = Math.min(rastmaxX, maxIX);
                    endY = Math.min(rastmaxY, maxIY);
                }

                for (int y = depY; y<endY; y++) {
                    for (int x = depX; x<endX; x++) {
                        for (int b = 0; b<numBand; b++) {
                            tabRef[comp++] =  (float) (b + ((x-depX) + (y-depY) * tilesWidth) * numBand -32000.5);
                        }
                    }
                }
            }
        }
    }

     /**
     * {@inheritDoc }
     */
    @Override
    protected void setTabRefValue(int index, double value) {
        tabRef[index] = (float) value;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected void createTable(int length) {
        tabRef = new float[length];
        tabTest = new float[length];
    }
}
