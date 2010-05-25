package org.geotoolkit.data.model.kml;

import java.util.List;
import org.geotoolkit.data.model.xsd.SimpleType;

/**
 *
 * @author Samuel Andrés
 */
public class SchemaDataDefault extends AbstractObjectDefault implements SchemaData {

    public List<SimpleData> simpleDatas;
    public List<Object> schemaDataExtensions;

    public SchemaDataDefault(List<SimpleType> objectSimpleExtensions, IdAttributes idAttributes,
            List<SimpleData> simpleDatas, List<Object> schemaDataExtensions){
        super(objectSimpleExtensions, idAttributes);
        this.simpleDatas = simpleDatas;
        this.schemaDataExtensions = schemaDataExtensions;
    }

    @Override
    public List<SimpleData> getSimpleDatas() {return this.simpleDatas;}

    @Override
    public List<Object> getSchemaDataExtensions() {return this.schemaDataExtensions;}

}
