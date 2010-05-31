package org.geotoolkit.data.model.kml;

import java.util.List;
import org.geotoolkit.data.model.xsd.SimpleType;

/**
 *
 * @author Samuel Andrés
 */
public class ImagePyramidDefault extends AbstractObjectDefault implements ImagePyramid {

    private int titleSize;
    private int maxWidth;
    private int maxHeight;
    private GridOrigin gridOrigin;
    private List<SimpleType> imagePyramidSimpleExtensions;
    private List<AbstractObject> imagePyramidObjectExtensions;

    /**
     *
     * @param objectSimpleExtensions
     * @param idAttributes
     * @param titleSize
     * @param maxWidth
     * @param maxHeight
     * @param gridOrigin
     * @param imagePyramidSimpleExtensions
     * @param imagePyramidObjectExtensions
     */
    public ImagePyramidDefault(List<SimpleType> objectSimpleExtensions, IdAttributes idAttributes,
            int titleSize, int maxWidth, int maxHeight, GridOrigin gridOrigin,
            List<SimpleType> imagePyramidSimpleExtensions, List<AbstractObject> imagePyramidObjectExtensions){
        super(objectSimpleExtensions, idAttributes);
        this.titleSize = titleSize;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.gridOrigin = gridOrigin;
        this.imagePyramidSimpleExtensions = imagePyramidSimpleExtensions;
        this.imagePyramidObjectExtensions = imagePyramidObjectExtensions;
    }

    /**
     *
     * @{@inheritDoc }
     */
    @Override
    public int getTitleSize() {return this.titleSize;}

    /**
     *
     * @{@inheritDoc }
     */
    @Override
    public int getMaxWidth() {return this.maxWidth;}

    /**
     *
     * @{@inheritDoc }
     */
    @Override
    public int getMaxHeight() {return this.maxHeight;}

    /**
     *
     * @{@inheritDoc }
     */
    @Override
    public GridOrigin getGridOrigin() {return this.gridOrigin;}

    /**
     *
     * @{@inheritDoc }
     */
    @Override
    public List<SimpleType> getImagePyramidSimpleExtensions() {return this.imagePyramidSimpleExtensions;}

    /**
     *
     * @{@inheritDoc }
     */
    @Override
    public List<AbstractObject> getImagePyramidObjectExtensions() {return this.imagePyramidObjectExtensions;}

}
