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
package org.geotoolkit.process.coverage.pgpyramid;

import java.awt.Dimension;
import java.util.Map;
import java.util.concurrent.CancellationException;
import javax.swing.JLabel;
import javax.swing.ProgressMonitor;
import org.geotoolkit.coverage.CoverageStore;
import org.geotoolkit.feature.DefaultName;
import org.geotoolkit.image.interpolation.InterpolationCase;
import org.geotoolkit.coverage.PyramidCoverageBuilder;
import static org.geotoolkit.parameter.Parameters.*;
import org.geotoolkit.process.AbstractProcess;
import org.geotoolkit.process.ProcessException;
import static org.geotoolkit.process.coverage.pgpyramid.PyramidDescriptor.*;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.process.ProcessEvent;
import org.geotoolkit.process.ProcessListener;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;

/**
 * <p>Build and store a image pyramid.<br/><br/>
 *
 * For more explanations about input output objects see {@link PGCoverageBuilder} javadoc.
 *
 * @author Remi Marechal (Geomatys).
 */
public class PyramidProcess extends AbstractProcess implements ProcessListener {

    PyramidProcess(final ParameterValueGroup input) {
        super(INSTANCE, input);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    protected void execute() throws ProcessException {

        ArgumentChecks.ensureNonNull("inputParameters", inputParameters);

        final GridCoverage coverage               = value(IN_COVERAGE         , inputParameters);
        final CoverageStore coverageStore         = value(IN_COVERAGESTORE    , inputParameters);
        final InterpolationCase interpolationcase = value(IN_INTERPOLATIONCASE, inputParameters);
        final Map resolution_per_envelope         = value(IN_RES_PER_ENVELOPE , inputParameters);
        final String pyramid_name                 = value(IN_PYRAMID_NAME     , inputParameters);
        final Dimension tilesize                  = value(IN_TILE_SIZE        , inputParameters);
        final double[] fillvalue                  = value(IN_FILLVALUES       , inputParameters);

        //check map values
        for(Object obj : resolution_per_envelope.keySet()) {
            if (!(obj instanceof Envelope))
                throw new ProcessException("Map key must be instance of Envelope", this, null);
            if (!(resolution_per_envelope.get(obj) instanceof double[]))
                throw new ProcessException("Map store objects must be instance of double[]", this, null);
        }

        final PyramidCoverageBuilder pgcb = new PyramidCoverageBuilder(tilesize, interpolationcase, 2);
        if (isCanceled()) {
            throw new CancellationException();
        }
        try {
            pgcb.create(coverage, coverageStore, new DefaultName(pyramid_name), resolution_per_envelope, fillvalue,
                    this, new PyramidMonitor(this));
        } catch (Exception ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }

        if (isCanceled()) {
            throw new CancellationException();
        }
        getOrCreate(OUT_COVERAGESTORE, outputParameters).setValue(coverageStore);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void started(ProcessEvent event) {
        // do nothing volontary
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void progressing(ProcessEvent event) {
        fireProgressing(event.getTask(), event.getProgress(), false);
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void paused(ProcessEvent event) {
        fireProcessPaused(event.getTask(), event.getProgress());
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void resumed(ProcessEvent event) {
        fireProcessResumed(event.getTask(), event.getProgress());
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void completed(ProcessEvent event) {
        // do nothing volontary
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void failed(ProcessEvent event) {
        fireProcessFailed(event.getTask(), event.getException());
    }

    /**
     * Allow to cancel the tiles creation process.
     */
    private class PyramidMonitor extends ProgressMonitor {
        private final PyramidProcess process;

        public PyramidMonitor(final PyramidProcess process) {
            super(new JLabel(), "", "", 0, 100);
            this.process = process;
        }

        @Override
        public boolean isCanceled() {
            return process.isCanceled();
        }
    }
}
