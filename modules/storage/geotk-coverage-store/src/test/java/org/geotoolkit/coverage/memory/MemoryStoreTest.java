package org.geotoolkit.coverage.memory;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.Names;
import org.geotoolkit.coverage.grid.GeneralGridGeometry;
import org.geotoolkit.coverage.grid.GridCoverageBuilder;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.storage.DataNode;
import org.geotoolkit.storage.coverage.AbstractCoverageStore;
import org.geotoolkit.storage.coverage.CoverageReference;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.TemporalExtent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class MemoryStoreTest {

    private static MemoryCoverageStore create() throws DataStoreException {
        final MemoryCoverageStore mcs = new MemoryCoverageStore();
        final CoverageReference ref = mcs.create(Names.createLocalName("test", ":", "mock"));

        final GridCoverageBuilder gcb = new GridCoverageBuilder();
        gcb.setRenderedImage(new BufferedImage(13, 13, BufferedImage.TYPE_BYTE_GRAY));
        gcb.setCoordinateReferenceSystem(CommonCRS.defaultGeographic());
        gcb.setEnvelope(new GeneralEnvelope(new DefaultGeographicBoundingBox(-7, 7, -7, 7)));

        ref.acquireWriter().write(gcb.build(), null);
        return mcs;
    }

    @Test
    public void testMetadata() throws Exception {
        final MemoryCoverageStore store = create();
        final DataNode root = store.getRootNode();
        Assume.assumeNotNull(root);

        final Metadata md = store.getMetadata();

        final CoverageReference[] refs = AbstractCoverageStore.flattenSubTree(root)
                .filter(node -> node instanceof CoverageReference)
                .map(node -> (CoverageReference) node)
                .toArray(size -> new CoverageReference[size]);

        final DefaultExtent expectedExtent = new DefaultExtent();
        final Set<CoordinateReferenceSystem> crss = new HashSet<>();
        for (final CoverageReference ref : refs) {
            final GridCoverageReader reader = ref.acquireReader();
            try {
                final GeneralGridGeometry gg = reader.getGridGeometry(ref.getImageIndex());
                expectedExtent.addElements(gg.getEnvelope());
                crss.add(gg.getCoordinateReferenceSystem());
                ref.recycle(reader);
            } catch (Exception e) {
                reader.dispose();
                throw e;
            }
        }

        final Set<GeographicExtent> geoBoxes = new HashSet<>();
        final Set<TemporalExtent> timeBoxes = new HashSet<>();
        final Set<VerticalExtent> vertBoxes = new HashSet<>();
        md.getIdentificationInfo().stream()
                .filter(info -> info instanceof DataIdentification)
                .flatMap(info -> ((DataIdentification)info).getExtents().stream())
                .sequential()
                .forEach(extent -> {
                    geoBoxes.addAll(extent.getGeographicElements());
                    timeBoxes.addAll(extent.getTemporalElements());
                    vertBoxes.addAll(extent.getVerticalElements());
                });

        Assert.assertTrue("Metadata should contain data reference system.", md.getReferenceSystemInfo().containsAll(crss));
        Assert.assertTrue("Metadata geographic extents should be aligned with data store envelopes", sameElements(geoBoxes, expectedExtent.getGeographicElements()));
        Assert.assertTrue("Metadata temporal extents should be aligned with data store envelopes", sameElements(timeBoxes, expectedExtent.getTemporalElements()));
        Assert.assertTrue("Metadata vertical extents should be aligned with data store envelopes", sameElements(vertBoxes, expectedExtent.getVerticalElements()));
    }
    
    private static boolean sameElements(Collection first, Collection second) {
        return first.size() == second.size() && first.containsAll(second);
    }
}
