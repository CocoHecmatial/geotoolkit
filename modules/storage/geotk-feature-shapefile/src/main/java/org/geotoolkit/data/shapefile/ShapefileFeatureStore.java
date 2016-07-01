/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009, Geomatys
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

package org.geotoolkit.data.shapefile;

import org.geotoolkit.data.FeatureStoreFactory;
import org.geotoolkit.data.shapefile.lock.ShpFileType;
import org.geotoolkit.data.shapefile.lock.StorageFile;
import org.geotoolkit.data.shapefile.lock.ShpFiles;
import org.geotoolkit.data.shapefile.lock.AccessManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.metadata.iso.citation.Citations;

import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.AbstractFeatureStore;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.data.FeatureReader;
import org.geotoolkit.data.FeatureWriter;
import org.geotoolkit.data.memory.GenericEmptyFeatureIterator;
import org.geotoolkit.data.query.DefaultQueryCapabilities;
import org.geotoolkit.data.query.Query;
import org.geotoolkit.data.query.QueryCapabilities;
import org.geotoolkit.data.query.QueryUtilities;
import org.geotoolkit.data.dbf.DbaseFileHeader;
import org.geotoolkit.data.dbf.DbaseFileReader;
import org.geotoolkit.data.shapefile.shp.ShapeType;
import org.geotoolkit.data.shapefile.shp.ShapefileHeader;
import org.geotoolkit.data.shapefile.shp.ShapefileReader;
import org.geotoolkit.data.shapefile.shp.ShapefileWriter;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.feature.FeatureTypeUtilities;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.filter.visitor.FilterAttributeExtractor;
import org.geotoolkit.geometry.jts.JTSEnvelope2D;
import org.geotoolkit.io.wkt.PrjFiles;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.parameter.Parameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.geotoolkit.data.shapefile.cpg.CpgFiles;

import org.geotoolkit.storage.DataFileStore;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.AttributeDescriptor;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.GeometryDescriptor;
import org.opengis.util.GenericName;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import static org.geotoolkit.data.shapefile.lock.ShpFileType.*;
import org.geotoolkit.storage.DataStores;
import org.opengis.feature.MismatchedFeatureException;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class ShapefileFeatureStore extends AbstractFeatureStore implements DataFileStore {

    // This is the default character as specified by the DBF specification
    public static final Charset DEFAULT_STRING_CHARSET = DbaseFileReader.DEFAULT_STRING_CHARSET;

    private final QueryCapabilities capabilities = new DefaultQueryCapabilities(false);
    protected final ShpFiles shpFiles;
    protected final boolean useMemoryMappedBuffer;
    protected final Charset dbfCharset;
    private GenericName name;
    private FeatureType schema;


    /**
     * Creates a new instance of ShapefileDataStore.
     *
     * @param uri The URL of the shp file to use for this DataSource.
     *
     * @throws NullPointerException DOCUMENT ME!
     * @throws DataStoreException If computation of related URLs (dbf,shx) fails.
     */
    public ShapefileFeatureStore(final URI uri) throws DataStoreException,MalformedURLException {
        this(uri, null);
    }

    /**
     * this sets the datastore's namespace during construction (so the schema -
     * FeatureType - will have the correct value) You can call this with
     * namespace = null, but I suggest you give it an actual namespace.
     *
     * @param uri
     * @param namespace
     */
    public ShapefileFeatureStore(final URI uri, final String namespace)
            throws DataStoreException,MalformedURLException {
        this(uri, namespace, false, null);
    }

    /**
     * This sets the datastore's namespace during construction (so the schema -
     * FeatureType - will have the correct value) You can call this with
     * namespace = null, but I suggest you give it an actual namespace.
     *
     * @param uri
     * @param namespace
     * @param useMemoryMapped : default is true
     * @param dbfCharset : if null default will be ShapefileDataStore.DEFAULT_STRING_CHARSET
     */
    public ShapefileFeatureStore(final URI uri, final String namespace, final boolean useMemoryMapped,
            Charset dbfCharset) throws MalformedURLException, DataStoreException {
        this(toParameter(uri, namespace, useMemoryMapped, dbfCharset));
    }

    public ShapefileFeatureStore(final ParameterValueGroup params) throws MalformedURLException, DataStoreException {
        super(params);

        final URI uri = (URI) params.parameter(
                ShapefileFeatureStoreFactory.PATH.getName().toString()).getValue();
        final Boolean useMemoryMapped = (Boolean) params.parameter(
                ShapefileFeatureStoreFactory.MEMORY_MAPPED.getName().toString()).getValue();
        Charset dbfCharset = (Charset) params.parameter(
                ShapefileFeatureStoreFactory.DBFCHARSET.getName().toString()).getValue();

        shpFiles = new ShpFiles(uri);

        //search for a .cpg file which contains the character encoding
        if(dbfCharset == null && shpFiles.exists(CPG)){
            try (ReadableByteChannel channel = shpFiles.getReadChannel(CPG)) {
                dbfCharset = CpgFiles.read(channel);
            } catch (IOException ex) {
                throw new DataStoreException(ex.getMessage(), ex);
            }
        }

        if(dbfCharset == null){
            dbfCharset = DEFAULT_STRING_CHARSET;
        }

        if (!shpFiles.isLocal() || !shpFiles.exists(SHP)) {
            this.useMemoryMappedBuffer = false;
        } else {
            this.useMemoryMappedBuffer = useMemoryMapped;
        }

        this.dbfCharset = dbfCharset;
    }

    private static ParameterValueGroup toParameter(final URI uri, final String namespace,
            final boolean useMemoryMapped, Charset dbfCharset){
        final ParameterValueGroup params = ShapefileFeatureStoreFactory.PARAMETERS_DESCRIPTOR.createValue();
        Parameters.getOrCreate(ShapefileFeatureStoreFactory.PATH, params).setValue(uri);
        Parameters.getOrCreate(ShapefileFeatureStoreFactory.NAMESPACE, params).setValue(namespace);
        Parameters.getOrCreate(ShapefileFeatureStoreFactory.MEMORY_MAPPED, params).setValue(useMemoryMapped);
        if(dbfCharset!=null){
            Parameters.getOrCreate(ShapefileFeatureStoreFactory.DBFCHARSET, params).setValue(dbfCharset);
        }
        return params;
    }

    @Override
    public FeatureStoreFactory getFactory() {
        return (FeatureStoreFactory) DataStores.getFactoryById(ShapefileFeatureStoreFactory.NAME);
    }

    @Override
    public boolean isWritable(final GenericName typeName) throws DataStoreException {
        return shpFiles.isLocal();
    }

    public GenericName getName() throws DataStoreException{
        checkTypeExist();
        return name;
    }

    public FeatureType getFeatureType() throws DataStoreException{
        checkTypeExist();
        return schema;
    }

    private void checkTypeExist() throws DataStoreException {
        if (name != null && schema != null) {
            return;
        }
        this.schema = buildSchema(getDefaultNamespace());
        this.name = schema.getName();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Set<GenericName> getNames() throws DataStoreException {
        return Collections.singleton(getName());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureType getFeatureType(final GenericName typeName) throws DataStoreException {
        typeCheck(typeName);
        return schema;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public QueryCapabilities getQueryCapabilities() {
        return capabilities;
    }

    /**
     * Gets the bounding box of the file represented by this data store as a
     * whole (that is, off all of the features in the shapefile)
     *
     * @return The bounding box of the datasource or null if unknown and too
     *         expensive for the method to calculate.
     * @throws DataSourceException DOCUMENT ME!
     */
    @Override
    public Envelope getEnvelope(final Query query) throws DataStoreException, FeatureStoreRuntimeException {
        typeCheck(query.getTypeName());

        if(QueryUtilities.queryAll(query)){

            // This is way quick!!!
            ReadableByteChannel in = null;

            try {
                final ByteBuffer buffer = ByteBuffer.allocate(100);

                in = shpFiles.getReadChannel(SHP);
                try {
                    in.read(buffer);
                    buffer.flip();
                    final ShapefileHeader header = ShapefileHeader.read(buffer, true);

                    final com.vividsolutions.jts.geom.Envelope env =
                            new com.vividsolutions.jts.geom.Envelope(
                            header.minX(), header.maxX(), header.minY(), header.maxY());

                    if (schema != null) {
                        return new JTSEnvelope2D(env, schema.getCoordinateReferenceSystem());
                    }else{
                        return new JTSEnvelope2D(env, null);
                    }
                } finally {
                    in.close();
                }

            } catch (IOException ioe) {
                // What now? This seems arbitrarily appropriate !
                throw new DataStoreException("Problem getting Bbox", ioe);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ioe) {
                    // do nothing
                }
            }
        }else{
            return super.getEnvelope(query);
        }

    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureReader getFeatureReader(final Query query) throws DataStoreException {
        typeCheck(query.getTypeName());

        final Hints hints = query.getHints();
        final String typeName = query.getTypeName().tip().toString();
        final GenericName[] propertyNames = query.getPropertyNames();
        final GenericName defaultGeomName = schema.getGeometryDescriptor().getName();
        final double[] resample = query.getResolution();

        //check if we must read the 3d values
        final CoordinateReferenceSystem reproject = query.getCoordinateSystemReproject();
        final boolean read3D = (reproject == null || CRS.getVerticalComponent(reproject, true) != null);

        // gather attributes needed by the query tool, they will be used by the
        // query filter
        final FilterAttributeExtractor extractor = new FilterAttributeExtractor();
        final Filter filter = query.getFilter();
        filter.accept(extractor, null);
        final GenericName[] filterAttnames = extractor.getAttributeNames();

        // check if the geometry is the one and only attribute needed
        // to return attribute _and_ to run the query filter
        if (   propertyNames != null
            && propertyNames.length == 1
            && propertyNames[0].tip().toString().equals(defaultGeomName.tip().toString())
            && (filterAttnames.length == 0 || (filterAttnames.length == 1 && filterAttnames[0].tip().toString()
                        .equals(defaultGeomName.tip().toString())))) {
            try {
                final FeatureType newSchema = FeatureTypeUtilities.createSubType(schema, propertyNames);

                final ShapefileAttributeReader attReader = getAttributesReader(false,read3D,resample);
                final FeatureIDReader idReader = new DefaultFeatureIDReader(typeName);
                FeatureReader reader = ShapefileFeatureReader.create(attReader, idReader, newSchema, hints);
                final QueryBuilder remaining = new QueryBuilder(query.getTypeName());
                remaining.setProperties(query.getPropertyNames());
                remaining.setFilter(query.getFilter());
                remaining.setHints(query.getHints());
                remaining.setCRS(query.getCoordinateSystemReproject());
                remaining.setSortBy(query.getSortBy());
                remaining.setStartIndex(query.getStartIndex());
                remaining.setMaxFeatures(query.getMaxFeatures());
                reader = handleRemaining(reader, remaining.buildQuery());

                return reader;
            } catch (MismatchedFeatureException se) {
                throw new DataStoreException("Error creating schema", se);
            }
        }else{
             try {
                final FeatureType newSchema;
                if (propertyNames != null) {
                    newSchema = FeatureTypeUtilities.createSubType(schema, propertyNames);
                } else {
                    newSchema = schema;
                }

                final ShapefileAttributeReader attReader = getAttributesReader(true,read3D,resample);
                final FeatureIDReader idReader = new DefaultFeatureIDReader(typeName);
                FeatureReader reader = ShapefileFeatureReader.create(attReader,idReader, newSchema, hints);
                QueryBuilder query2 = new QueryBuilder(query.getTypeName());
                query2.setProperties(query.getPropertyNames());
                query2.setFilter(query.getFilter());
                query2.setHints(query.getHints());
                query2.setCRS(query.getCoordinateSystemReproject());
                query2.setSortBy(query.getSortBy());
                query2.setStartIndex(query.getStartIndex());
                query2.setMaxFeatures(query.getMaxFeatures());
                reader = handleRemaining(reader, query2.buildQuery());

                return reader;
            } catch (MismatchedFeatureException se) {
                throw new DataStoreException("Error creating schema", se);
            }
        }


    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureWriter getFeatureWriter(final GenericName typeName, final Filter filter, final Hints hints) throws DataStoreException {
        typeCheck(typeName);

        final ShapefileAttributeReader attReader = getAttributesReader(true,true,null);
        final FeatureIDReader idReader = new DefaultFeatureIDReader(typeName.tip().toString());
        FeatureReader featureReader;
        try {
            featureReader = ShapefileFeatureReader.create(attReader,idReader, schema, hints);
        } catch (Exception e) {
            featureReader = GenericEmptyFeatureIterator.createReader(schema);
        }
        try {
            return handleRemaining(new ShapefileFeatureWriter(this,typeName.tip().toString(), shpFiles, attReader, featureReader, dbfCharset),filter);
        } catch (IOException ex) {
            throw new DataStoreException(ex);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // schema manipulation /////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Set the FeatureType of this DataStore. This method will delete any
     * existing local resources or throw an IOException if the featurestore is
     * remote.
     *
     * @param featureType The desired FeatureType.
     * @throws DataStoreException If the featurestore is remote.
     *
     * @todo must synchronize this properly
     */
    @Override
    public void createFeatureType(final GenericName typeName, final FeatureType featureType) throws DataStoreException {
        if (!shpFiles.isLocal()) {
            throw new DataStoreException("Cannot create FeatureType on remote shapefile");
        }

        if(typeName == null){
            throw new DataStoreException("Type name can not be null.");
        }

        if(!featureType.isSimple()){
            throw new DataStoreException("Feature type must not be null and must be a simple feature type.");
        }

        if(!featureType.getName().equals(typeName)){
            throw new DataStoreException("Shapefile featurestore can only hold typename same as feature type name.");
        }


        //delete the files
        shpFiles.delete();

        final AccessManager locker = shpFiles.createLocker();

        //update schema and name
        name = typeName;
        schema = featureType;

        final GeometryDescriptor desc = featureType.getGeometryDescriptor();
        CoordinateReferenceSystem crs = null;
        final Class<?> geomType;
        final ShapeType shapeType;
        if(desc != null){
            crs = desc.getCoordinateReferenceSystem();
            geomType = desc.getType().getBinding();
            shapeType =ShapeType.findBestGeometryType(geomType);
        }else{
            geomType = null;
            shapeType = ShapeType.NULL;
        }

        if(shapeType == ShapeType.UNDEFINED){
            throw new DataStoreException("Cannot create a shapefile whose geometry type is "+ geomType);
        }

        try{
            final StorageFile shpStoragefile = locker.getStorageFile(SHP);
            final StorageFile shxStoragefile = locker.getStorageFile(SHX);
            final StorageFile dbfStoragefile = locker.getStorageFile(DBF);
            final StorageFile prjStoragefile = locker.getStorageFile(PRJ);
            final StorageFile cpgStoragefile = locker.getStorageFile(CPG);

            try (FileChannel shpChannel = shpStoragefile.getWriteChannel();
                 FileChannel shxChannel = shxStoragefile.getWriteChannel()) {

                try (ShapefileWriter writer = new ShapefileWriter(shpChannel, shxChannel)) {
                    // try to get the domain first
                    final Envelope domain = org.geotoolkit.referencing.CRS.getEnvelope(crs);
                    if (domain != null) {
                        writer.writeHeaders(new JTSEnvelope2D(domain), shapeType, 0, 100);
                    } else {
                        // try to reproject the single overall envelope keeping poles out of the way
                        final JTSEnvelope2D env = new JTSEnvelope2D(-179, 179, -89, 89, CommonCRS.WGS84.normalizedGeographic());
                        JTSEnvelope2D transformedBounds;
                        if (crs != null) {
                            try {
                                transformedBounds = env.transform(crs, true);
                            } catch (Throwable t) {
                                if (getLogger().isLoggable(Level.WARNING)) {
                                    getLogger().log(Level.WARNING, t.getLocalizedMessage(), t);
                                }
                                transformedBounds = env;
                                crs = null;
                            }
                        } else {
                            transformedBounds = env;
                        }

                        writer.writeHeaders(transformedBounds, shapeType, 0, 100);
                    }
                } finally {
                    assert !shpChannel.isOpen();
                    assert !shxChannel.isOpen();
                }
            }

            final DbaseFileHeader dbfheader = DbaseFileHeader.createDbaseHeader(schema);
            dbfheader.setNumRecords(0);

            try (WritableByteChannel dbfChannel = dbfStoragefile.getWriteChannel()) {
                dbfheader.writeHeader(dbfChannel);
            }

            if (crs != null) {
                // .prj files should have no carriage returns in them, this messes up
                // ESRI's ArcXXX software, so we'll be compatible
                final WKTFormat format = new WKTFormat(Locale.ENGLISH, null);
                format.setConvention(Convention.WKT1_COMMON_UNITS);
                format.setNameAuthority(Citations.ESRI);
                format.setIndentation(WKTFormat.SINGLE_LINE);
                final String s = format.format(crs);
                IOUtilities.writeString(s, prjStoragefile.getFile(), Charset.forName("ISO-8859-1"));
            } else {
                getLogger().warning("PRJ file not generated for null CoordinateReferenceSystem");
                Path prjFile = prjStoragefile.getFile();
                Files.deleteIfExists(prjFile);
            }

            //write dbf encoding .cpg
            CpgFiles.write(dbfCharset, cpgStoragefile.getFile());

            locker.disposeReaderAndWriters();
            locker.replaceStorageFiles();
        }catch(IOException ex){
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void updateFeatureType(final GenericName typeName, final FeatureType featureType) throws DataStoreException {
        throw new DataStoreException("Can not update shapefile schema.");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void deleteFeatureType(final GenericName typeName) throws DataStoreException {
        throw new DataStoreException("Can not delete shapefile schema.");
    }

    ////////////////////////////////////////////////////////////////////////////
    // utils ///////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Obtain the FeatureType of the given name. ShapefileFeatureStore contains
     * only one FeatureType.
     *
     * @return The FeatureType that this featurestore contains.
     * @throws IOException If a type by the requested name is not present.
     */
    private synchronized FeatureType buildSchema(final String namespace) throws DataStoreException {

        //read all attributes///////////////////////////////////////////////////
        final AccessManager locker = shpFiles.createLocker();

        final ShapefileReader shp;
        final DbaseFileReader dbf;
        try {
            shp = locker.getSHPReader(true, useMemoryMappedBuffer, true, null);
            dbf = locker.getDBFReader(useMemoryMappedBuffer, dbfCharset);
        } catch (IOException ex) {
            throw new DataStoreException(ex);
        }

        CoordinateReferenceSystem crs = null;

        //read the projection
        final boolean qpjExists = shpFiles.exists(QPJ);
        final boolean prjExists = shpFiles.exists(PRJ);
        if (qpjExists || prjExists) {
            try (final ReadableByteChannel channel = qpjExists ? shpFiles.getReadChannel(QPJ) : shpFiles.getReadChannel(PRJ)) {
                crs = PrjFiles.read(channel, true);
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, ex.getMessage(), ex);
                crs = null;
            }
        }

        final GeometryDescriptor geomDescriptor;
        final List<AttributeDescriptor> attributes = new ArrayList<>();

        try {
            //get the descriptor from shp
            geomDescriptor = shp.getHeader().createDescriptor(namespace, crs);
            attributes.add(geomDescriptor);

            //get dbf attributes if exist
            if (dbf != null) {
                final DbaseFileHeader header = dbf.getHeader();
                attributes.addAll(header.createDescriptors(namespace));
            }
        } finally {
            //we have finish readring what we want, dispose everything
            locker.disposeReaderAndWriters();
        }

        //create the feature type //////////////////////////////////////////////
        final FeatureTypeBuilder builder = new FeatureTypeBuilder(null,false);
        builder.setName(namespace,shpFiles.getTypeName());
        builder.setAbstract(false);
        builder.addAll(attributes);
        builder.setDefaultGeometry(geomDescriptor.getLocalName());

        return builder.buildSimpleFeatureType();
    }

    /**
     * Returns the attribute reader, allowing for a pure shapefile reader, or a
     * combined dbf/shp reader.
     *
     * @param readDbf - if true, the dbf fill will be opened and read
     * @throws IOException
     */
    protected ShapefileAttributeReader getAttributesReader(final boolean readDbf,
            final boolean read3D, final double[] resample) throws DataStoreException {

        final AccessManager locker = shpFiles.createLocker();
        final FeatureType schema = getFeatureType();

        final PropertyDescriptor[] descs;
        if(readDbf){
            descs =  schema.getDescriptors().toArray(new PropertyDescriptor[0]);
        }else{
            getLogger().fine("The DBF file won't be opened since no attributes will be read from it");
            descs = new PropertyDescriptor[]{schema.getGeometryDescriptor()};
        }
        try {
            return new ShapefileAttributeReader(locker, descs, read3D,
                    useMemoryMappedBuffer,resample, readDbf, dbfCharset,null);
        } catch (IOException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public Path[] getDataFiles() throws DataStoreException {
        final List<Path> files = new ArrayList<>();
        for (final ShpFileType type : ShpFileType.values()) {
            final Path f = shpFiles.getPath(type);
            if (f != null && Files.exists(f)) {
                files.add(f);
            }
        }
        return files.toArray(new Path[files.size()]);
    }

    ////////////////////////////////////////////////////////////////////////////
    //Fallback on iterative reader and writer //////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    public List<FeatureId> addFeatures(final GenericName groupName, final Collection<? extends Feature> newFeatures,
            final Hints hints) throws DataStoreException {
        final List<FeatureId> ids = handleAddWithFeatureWriter(groupName, newFeatures, hints);
        return ids;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void updateFeatures(final GenericName groupName, final Filter filter, final Map<? extends PropertyDescriptor, ? extends Object> values) throws DataStoreException {
        handleUpdateWithFeatureWriter(groupName, filter, values);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeFeatures(final GenericName groupName, final Filter filter) throws DataStoreException {
        handleRemoveWithFeatureWriter(groupName, filter);
    }

    @Override
    public void refreshMetaModel() {
		name = null;
		schema = null;

	}

}
