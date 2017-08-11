/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012-2014, Geomatys
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

package org.geotoolkit.storage.coverage;

import java.awt.Point;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.sis.internal.metadata.NameToIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.util.Classes;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.coverage.grid.GeneralGridGeometry;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.geotoolkit.internal.data.GenericNameIndex;
import org.geotoolkit.storage.DataStore;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.StorageEvent;
import org.geotoolkit.storage.StorageListener;
import org.geotoolkit.version.Version;
import org.geotoolkit.version.VersionControl;
import org.geotoolkit.version.VersioningException;
import org.opengis.util.GenericName;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Abstract implementation of a coverage store.
 *
 * @author Johann Sorel (Geomatys)
 * @module
 */
public abstract class AbstractCoverageStore extends DataStore implements CoverageStore {

    private static final Logger LOGGER = Logging.getLogger("org.geotoolkit.storage.coverage");
    protected final Parameters parameters;
    protected final Set<StorageListener> storeListeners = new HashSet<>();

    private GenericNameIndex<CoverageResource> cachedRefs = null;

    protected AbstractCoverageStore(final ParameterValueGroup params) {
        this.parameters = Parameters.castOrWrap(params);

        //redirect warning listener events to default logger
        listeners.getLogger().setUseParentHandlers(false);
        listeners.getLogger().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                getLogger().log(record);
            }
            @Override
            public void flush() {}
            @Override
            public void close() throws SecurityException {}
        });
    }

    /**
     * Create a new metadata containing information about this datastore and the
     * coverages it contains.
     *
     * Note : Analysis should be restricted to report only information currently
     * available in this dataset. Further computing should be performed externally.
     *
     * Note 2 : You can decide how extents are stored in the metadata by overriding
     * solely {@link #setSpatialInfo(java.util.Map) } only.
     *
     * @return Created metadata, Can be null if no data is available at the
     * moment.
     *
     * @throws DataStoreException If an error occurs while analyzing underlying
     * data.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final Resource root = getRootResource();
        if (root == null) {
            return null;
        }

        final DefaultMetadata rootMd = new DefaultMetadata();

        // Queries data specific information
        final Map<GenericName, GeneralGridGeometry> geometries = new HashMap<>();
        final List<CoverageResource> refs = DataStores.flatten(root)
                .filter(node -> node instanceof CoverageResource)
                .map(node -> ((CoverageResource) node))
                .collect(Collectors.toList());

        for (final CoverageResource ref : refs) {
            final GridCoverageReader reader = ref.acquireReader();
            final SpatialMetadata md;
            final GeneralGridGeometry gg;
            try {
                md = reader.getCoverageMetadata(ref.getImageIndex());
                gg = reader.getGridGeometry(ref.getImageIndex());
                ref.recycle(reader);
            } catch (Exception e) {
                // If something turned wrong, we definitively get rid of the reader.
                reader.dispose();
                throw e;
            }

            if (gg != null) {
                geometries.put(ref.getName(), gg);
            }

            if (md != null) {
                final CoverageDescription cd = md.getInstanceForType(CoverageDescription.class); // ImageDescription
                if (cd != null)
                    rootMd.getContentInfo().add(cd);
            }
        }

        setSpatialInfo(rootMd, geometries);

        return rootMd;
    }

    /**
     * Compute extents to set in store's metadata. This analysis is separated in
 a method so inheriting stores will be able to customize it easily.
 This method is needed because geographic information could be features differently
 according to its structure. Example :
 - If the metadata represents two distinct data, we should have two distinct
 extents
 - If the metadata describes an non-continuous data cube, we should have a
 single extent which contains multiple disjoint geographic/temporal/elevation
 extents.

 Note : Default algorithm is really simple. We put all envelopes in a simple
 extent, which will directly contain the list of geographic, temporal and
 vertical extents for each reference.

 We'll also add all reference systems found in the input grid geometries if
 they're not here already.
     *
     * @param md The metadata to update
     * @param geometries The grid geometries of each store's reference, grouped
     * by reference name.
     */
    protected void setSpatialInfo(final Metadata md, final Map<GenericName, GeneralGridGeometry> geometries) {
        if (geometries == null || geometries.isEmpty())
            return;

        // HACk : create temporary sets to automatically remove doublon extents.
        final DefaultExtent extent = new DefaultExtent() {
            @Override
            protected <E> Class<? extends Collection<E>> collectionType(Class<E> elementType) {
                return (Class) Set.class;
            }
        };

        final Set<CoordinateReferenceSystem> crss = new HashSet<>();
        geometries.forEach((name, gg) -> {
            try {
                extent.addElements(gg.getEnvelope());
            } catch (TransformException ex) {
                LOGGER.log(Level.WARNING, "Extent cannot be computed for reference " + name, ex);
            }

            crss.add(gg.getCoordinateReferenceSystem());
        });

        /* Hack : copy original extents, so allocated sets are transformed into
         * lists. It is necessary, so if someone modifies an inner extent, the set
         * uniquenes won't be messed.
         */
        final DefaultDataIdentification ddi = new DefaultDataIdentification();
        ddi.getExtents().add(new DefaultExtent(extent));
        ((Collection)md.getIdentificationInfo()).add(ddi);

        // Ensure we'll have no doublon
        crss.removeAll(md.getReferenceSystemInfo());
        md.getReferenceSystemInfo().addAll((Collection)crss);
    }

    @Override
    public ParameterValueGroup getConfiguration() {
        return parameters;
    }

    protected Logger getLogger(){
        return LOGGER;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(Classes.getShortClassName(this));
        try {
            final Resource node = getRootResource();
            sb.append(' ');
            sb.append(node.toString());
        } catch (DataStoreException ex) {
            Logging.getLogger("org.geotoolkit.storage").log(Level.WARNING, null, ex);
        }

        return sb.toString();
    }

    @Override
    public CoverageResource create(GenericName name) throws DataStoreException {
        throw new DataStoreException("Creation of new coverage not supported.");
    }

    @Override
    public void delete(GenericName name) throws DataStoreException {
        throw new DataStoreException("Deletion of coverage not supported.");
    }

    ////////////////////////////////////////////////////////////////////////////
    // Convinient methods, fallback on getRootResource                            //
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public final Set<GenericName> getNames() throws DataStoreException {
        final GenericNameIndex<CoverageResource> map = listReferences();
        return map.getNames();
    }

    @Override
    public final CoverageResource findResource(GenericName name) throws DataStoreException {
        final GenericNameIndex<CoverageResource> map = listReferences();
        return map.get(name.toString());
    }

    @Override
    public Resource findResource(String name) throws DataStoreException {
        Resource res = findResource(getRootResource(), name);
        if (res==null) {
            throw new IllegalNameException("No resource for name : "+name);
        }
        return (org.geotoolkit.storage.Resource) res;
    }

    private Resource findResource(final org.apache.sis.storage.Resource candidate, String name) throws DataStoreException {

        final boolean match = NameToIdentifier.isHeuristicMatchForIdentifier(Collections.singleton(((org.geotoolkit.storage.Resource)candidate).getIdentifier()), name);
        Resource result = match ? (Resource)candidate : null;

        if (candidate instanceof Aggregate) {
            final Aggregate ds = (Aggregate) candidate;
            for (Resource rs : ds.components()) {
                Object rr = findResource(rs,name);
                if (rr instanceof Resource) {
                    if (result!=null) {
                        throw new DataStoreException("Multiple resources match the name : "+name);
                    }
                    result = (Resource) rr;
                }
            }
        }
        return result;
    }

    protected synchronized GenericNameIndex<CoverageResource> listReferences() throws DataStoreException {
        if (cachedRefs==null) {
            cachedRefs = new GenericNameIndex<>();
            listReferences(getRootResource(), cachedRefs);
        }
        return cachedRefs;
    }

    private GenericNameIndex<CoverageResource> listReferences(org.apache.sis.storage.Resource candidate, GenericNameIndex<CoverageResource> map)
            throws IllegalNameException, DataStoreException{

        if(candidate instanceof CoverageResource){
            final CoverageResource cr = (CoverageResource) candidate;
            map.add(cr.getName(), cr);
        }

        if (candidate instanceof Aggregate) {
            for(org.apache.sis.storage.Resource child : ((Aggregate)candidate).components()){
                listReferences(child, map);
            }
        }

        return map;
    }

    ////////////////////////////////////////////////////////////////////////////
    // versioning methods : handle nothing by default                         //
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean handleVersioning() {
        return false;
    }

    @Override
    public VersionControl getVersioning(GenericName typeName) throws VersioningException {
        throw new VersioningException("Versioning not supported");
    }

    @Override
    public CoverageResource findResource(GenericName name, Version version) throws DataStoreException {
        throw new DataStoreException("Versioning not supported");
    }

    ////////////////////////////////////////////////////////////////////////////
    // convinient methods                                                     //
    ////////////////////////////////////////////////////////////////////////////

    protected CoverageStoreManagementEvent fireCoverageAdded(final GenericName name){
        final CoverageStoreManagementEvent event = CoverageStoreManagementEvent.createCoverageAddEvent(this, name);
        sendStructureEvent(event);
        return event;
    }

    protected CoverageStoreManagementEvent fireCoverageUpdated(final GenericName name){
        final CoverageStoreManagementEvent event = CoverageStoreManagementEvent.createCoverageUpdateEvent(this, name);
        sendStructureEvent(event);
        return event;
    }

    protected CoverageStoreManagementEvent fireCoverageDeleted(final GenericName name){
        final CoverageStoreManagementEvent event = CoverageStoreManagementEvent.createCoverageDeleteEvent(this, name);
        sendStructureEvent(event);
        return event;
    }

    protected CoverageStoreManagementEvent firePyramidAdded(final GenericName name, final String pyramidId){
        final CoverageStoreManagementEvent event = CoverageStoreManagementEvent.createPyramidAddEvent(this, name, pyramidId);
        sendStructureEvent(event);
        return event;
    }

    protected CoverageStoreManagementEvent firePyramidUpdated(final GenericName name, final String pyramidId){
        final CoverageStoreManagementEvent event = CoverageStoreManagementEvent.createPyramidUpdateEvent(this, name, pyramidId);
        sendStructureEvent(event);
        return event;
    }

    protected CoverageStoreManagementEvent firePyramidDeleted(final GenericName name, final String pyramidId){
        final CoverageStoreManagementEvent event = CoverageStoreManagementEvent.createPyramidDeleteEvent(this, name, pyramidId);
        sendStructureEvent(event);
        return event;
    }

    protected CoverageStoreManagementEvent fireMosaicAdded(final GenericName name, final String pyramidId, final String mosaicId){
        final CoverageStoreManagementEvent event = CoverageStoreManagementEvent.createMosaicAddEvent(this, name, pyramidId, mosaicId);
        sendStructureEvent(event);
        return event;
    }

    protected CoverageStoreManagementEvent fireMosaicUpdated(final GenericName name, final String pyramidId, final String mosaicId){
        final CoverageStoreManagementEvent event = CoverageStoreManagementEvent.createMosaicUpdateEvent(this, name, pyramidId, mosaicId);
        sendStructureEvent(event);
        return event;
    }

    protected CoverageStoreManagementEvent fireMosaicDeleted(final GenericName name, final String pyramidId, final String mosaicId){
        final CoverageStoreManagementEvent event = CoverageStoreManagementEvent.createMosaicDeleteEvent(this, name, pyramidId, mosaicId);
        sendStructureEvent(event);
        return event;
    }

    protected CoverageStoreContentEvent fireDataUpdated(final GenericName name){
        final CoverageStoreContentEvent event = CoverageStoreContentEvent.createDataUpdateEvent(this, name);
        sendContentEvent(event);
        return event;
    }

    protected CoverageStoreContentEvent fireTileAdded(final GenericName name,
            final String pyramidId, final String mosaicId, final List<Point> tiles){
        final CoverageStoreContentEvent event = CoverageStoreContentEvent.createTileAddEvent(this, name, pyramidId, mosaicId, tiles);
        sendContentEvent(event);
        return event;
    }

    protected CoverageStoreContentEvent fireTileUpdated(final GenericName name,
            final String pyramidId, final String mosaicId, final List<Point> tiles){
        final CoverageStoreContentEvent event = CoverageStoreContentEvent.createTileUpdateEvent(this, name, pyramidId, mosaicId, tiles);
        sendContentEvent(event);
        return event;
    }

    protected CoverageStoreContentEvent fireTileDeleted(final GenericName name,
            final String pyramidId, final String mosaicId, final List<Point> tiles){
        final CoverageStoreContentEvent event = CoverageStoreContentEvent.createTileDeleteEvent(this, name, pyramidId, mosaicId, tiles);
        sendContentEvent(event);
        return event;
    }

    /**
     * Convinient method to check that the given type name exist.
     * Will raise a datastore exception if the name do not exist in this datastore.
     * @param candidate Name to test.
     * @throws DataStoreException if name do not exist.
     */
    protected void typeCheck(final GenericName candidate) throws DataStoreException{

        final Collection<GenericName> names = getNames();
        if(!names.contains(candidate)){
            final StringBuilder sb = new StringBuilder("Type name : ");
            sb.append(candidate);
            sb.append(" do not exist in this datastore, available names are : ");
            for(final GenericName n : names){
                sb.append(n).append(", ");
            }
            throw new DataStoreException(sb.toString());
        }
    }

    @Override
    public void addStorageListener(final StorageListener listener) {
        synchronized (storeListeners) {
            storeListeners.add(listener);
        }
    }

    @Override
    public void removeStorageListener(final StorageListener listener) {
        synchronized (storeListeners) {
            storeListeners.remove(listener);
        }
    }

    /**
     * Forward a structure event to all listeners.
     * @param event , event to send to listeners.
     */
    protected synchronized void sendStructureEvent(final StorageEvent event){
        cachedRefs = null;
        final StorageListener[] lst;
        synchronized (storeListeners) {
            lst = storeListeners.toArray(new StorageListener[storeListeners.size()]);
        }
        for(final StorageListener listener : lst){
            listener.structureChanged(event);
        }
    }

    /**
     * Forward a data event to all listeners.
     * @param event , event to send to listeners.
     */
    protected void sendContentEvent(final StorageEvent event){
        final StorageListener[] lst;
        synchronized (storeListeners) {
            lst = storeListeners.toArray(new StorageListener[storeListeners.size()]);
        }
        for(final StorageListener listener : lst){
            listener.contentChanged(event);
        }
    }

    /**
     * Forward given event, changing the source by this object.
     * For implementation use only.
     * @param event
     */
    public void forwardStructureEvent(StorageEvent event){
        sendStructureEvent(event.copy(this));
    }

    /**
     * Forward given event, changing the source by this object.
     * For implementation use only.
     * @param event
     */
    public void forwardContentEvent(StorageEvent event){
        sendContentEvent(event.copy(this));
    }

}
