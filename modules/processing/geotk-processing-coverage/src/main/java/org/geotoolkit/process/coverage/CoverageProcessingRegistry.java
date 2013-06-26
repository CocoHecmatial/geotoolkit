/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009 - 2012, Geomatys
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

package org.geotoolkit.process.coverage;

import java.util.Collections;
import org.geotoolkit.metadata.iso.DefaultIdentifier;
import org.geotoolkit.metadata.iso.citation.DefaultCitation;
import org.geotoolkit.metadata.iso.identification.DefaultServiceIdentification;
import org.geotoolkit.process.AbstractProcessingRegistry;
import org.geotoolkit.process.coverage.bandcombine.BandCombineDescriptor;
import org.geotoolkit.process.coverage.bandselect.BandSelectDescriptor;
import org.geotoolkit.process.coverage.copy.CopyCoverageStoreDescriptor;
import org.geotoolkit.process.coverage.coveragetofeatures.CoverageToFeaturesDescriptor;
import org.geotoolkit.process.coverage.coveragetovector.CoverageToVectorDescriptor;
import org.geotoolkit.process.coverage.isoline.IsolineDescriptor;
import org.geotoolkit.process.coverage.kriging.KrigingDescriptor;
import org.geotoolkit.process.coverage.metadataextractor.ExtractionDescriptor;
import org.geotoolkit.process.coverage.pgpyramid.PyramidDescriptor;
import org.geotoolkit.process.coverage.reducetodomain.ReduceToDomainDescriptor;
import org.geotoolkit.process.coverage.reformat.ReformatDescriptor;
import org.geotoolkit.process.coverage.resample.ResampleDescriptor;
import org.geotoolkit.process.coverage.straighten.StraightenDescriptor;
import org.geotoolkit.process.coverage.tiling.TilingDescriptor;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.identification.Identification;

/**
 * Declare loading of coverage processes.
 *
 * @author Johann sorel (Geomatys)
 * @author Cédric Briançon (Geomatys)
 * @module pending
 */
public class CoverageProcessingRegistry extends AbstractProcessingRegistry{

    public static final String NAME = "coverage";
    public static final DefaultServiceIdentification IDENTIFICATION;

    static {
        IDENTIFICATION = new DefaultServiceIdentification();
        Identifier id = new DefaultIdentifier(NAME);
        DefaultCitation citation = new DefaultCitation(NAME);
        citation.setIdentifiers(Collections.singleton(id));
        IDENTIFICATION.setCitation(citation);
    }

    public CoverageProcessingRegistry(){
        super(CoverageToVectorDescriptor.INSTANCE,
              CoverageToFeaturesDescriptor.INSTANCE,
              TilingDescriptor.INSTANCE,
              KrigingDescriptor.INSTANCE,
              ExtractionDescriptor.INSTANCE,
              IsolineDescriptor.INSTANCE,
              ResampleDescriptor.INSTANCE,
              CopyCoverageStoreDescriptor.INSTANCE,
              StraightenDescriptor.INSTANCE,
              ReduceToDomainDescriptor.INSTANCE,
              BandSelectDescriptor.INSTANCE,
              BandCombineDescriptor.INSTANCE,
              ReformatDescriptor.INSTANCE,
              PyramidDescriptor.INSTANCE);
    }

    @Override
    public Identification getIdentification() {
        return IDENTIFICATION;
    }

}
