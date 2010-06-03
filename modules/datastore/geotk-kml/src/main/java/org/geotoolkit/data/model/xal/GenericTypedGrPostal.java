package org.geotoolkit.data.model.xal;

/**
 * <p>This interface maps the most current fields used by elements.</p>
 *
 * @author Samuel Andrés
 */
public interface GenericTypedGrPostal {

    /**
     * 
     * @return
     */
    public String getContent();

    /**
     *
     * @return
     */
    public String getType();

    /**
     *
     * @return
     */
    public GrPostal getGrPostal();
}
