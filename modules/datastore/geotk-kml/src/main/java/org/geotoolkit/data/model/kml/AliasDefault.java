package org.geotoolkit.data.model.kml;

import java.util.List;
import org.geotoolkit.data.model.xsd.SimpleType;

/**
 *
 * @author Samuel Andrés
 */
public class AliasDefault extends AbstractObjectDefault implements Alias {

    private String targetHref;
    private String sourceHref;
    private List<SimpleType> aliasSimpleExtensions;
    private List<AbstractObject> aliasObjectExtensions;

    public AliasDefault(List<SimpleType> objectSimpleExtensions,
            IdAttributes idAttributes,
            String targetHref, String sourceHref,
            List<SimpleType> aliasSimpleExtensions, List<AbstractObject> aliasObjectExtensions){
        super(objectSimpleExtensions, idAttributes);
        this.targetHref = targetHref;
        this.sourceHref = sourceHref;
        this.aliasSimpleExtensions = aliasSimpleExtensions;
        this.aliasObjectExtensions = aliasObjectExtensions;
    }

    @Override
    public String getTargetHref() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getSourceHref() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<SimpleType> getAliasSimpleExtensions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<AbstractObject> getAliasObjectExtensions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
