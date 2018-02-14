package org.geotoolkit.gml.v321;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotoolkit.gml.ReadGeometryTest;
import org.junit.Test;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class ReadGeometry321Test extends ReadGeometryTest {

    @Test
    public void testMultiPolygon() throws Exception {
        testCollection(ReadGeometry321Test.class.getResource("polygons.gml"),
                MultiPolygon.class,
                71,
                new Envelope(99038, 399714, 6704170, 6879408)
        );
    }

    @Test
    public void testMultiLine() throws Exception {
        testCollection(ReadGeometry321Test.class.getResource("lines.gml"),
                MultiLineString.class,
                20,
                new Envelope(181899.675179, 197243.635526, 6850863.655278, 6869540.566522)
        );
    }

    @Test
    public void testMultiPoint() throws Exception {
        testCollection(ReadGeometry321Test.class.getResource("points.gml"),
                MultiPoint.class,
                3,
                new Envelope(223735.707508, 223735.707508, 6785396.537226, 6785396.537226)
        );
    }

    @Test
    public void testCurve() throws Exception {
        testCollection(ReadGeometry321Test.class.getResource("curve.gml"),
                MultiLineString.class,
                1,
                new Envelope(12.98241111111111, 13.462391666666665, -87.81824444444445, -87.25221944444445)
        );
    }
}
