/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2007-2012, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2012, Geomatys
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
package org.geotoolkit.image.io.plugin;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.IIOException;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.metadata.IIOMetadataFormat;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import ucar.ma2.Array;
import ucar.nc2.Group;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.VariableIF;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordSysBuilderIF;
import ucar.nc2.dataset.EnhanceScaleMissing;
import ucar.nc2.dataset.Enhancements;
import ucar.ma2.InvalidRangeException;

import org.opengis.coverage.grid.GridGeometry;
import org.opengis.metadata.content.TransferFunctionType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.geotoolkit.image.io.metadata.MetadataNodeAccessor;
import org.geotoolkit.image.io.metadata.ReferencingBuilder;
import org.geotoolkit.internal.image.io.NetcdfVariable;
import org.geotoolkit.internal.image.io.SampleMetadataFormat;
import org.geotoolkit.internal.image.io.DiscoveryAccessor;
import org.geotoolkit.internal.image.io.DimensionAccessor;
import org.geotoolkit.internal.image.io.GridDomainAccessor;
import org.geotoolkit.internal.image.io.IrregularGridConverter;
import org.apache.sis.internal.referencing.AxisDirections;
import org.geotoolkit.referencing.adapters.NetcdfAxis;
import org.geotoolkit.referencing.adapters.NetcdfCRS;
import org.geotoolkit.referencing.adapters.NetcdfCRSBuilder;
import org.geotoolkit.metadata.netcdf.NetcdfMetadataReader;
import org.geotoolkit.resources.Errors;
import org.apache.sis.util.collection.BackingStoreException;

import org.apache.sis.internal.util.UnmodifiableArrayList;
import static org.geotoolkit.image.io.metadata.SpatialMetadataFormat.ISO_FORMAT_NAME;
import static org.geotoolkit.image.io.plugin.NetcdfImageReader.Spi.NATIVE_FORMAT_NAME;
import static ucar.nc2.constants.CF.GRID_MAPPING;


/**
 * Metadata from NetCDF file. This implementation assumes that the NetCDF file follows the
 * <a href="http://www.cfconventions.org">CF Metadata conventions</a>.
 *
 * {@section Limitation}
 * Current implementation retains only the first {@linkplain CoordinateSystem coordinate system}
 * found in the NetCDF file or for a given variable. The {@link org.geotoolkit.coverage.io} package
 * would not know what to do with the extra coordinate systems anyway.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.20
 *
 * @since 3.08 (derived from 2.4)
 * @module
 */
final class NetcdfMetadata extends SpatialMetadata {
    /**
     * Forces usage of UCAR libraries in some places where we use our own code instead.
     * This may result in rounding errors and absence of information regarding fill values,
     * but is useful for checking if we are doing the right thing compared to the UCAR way.
     */
    private static final boolean USE_UCAR_LIB = false;

    /**
     * The mapping from COARD attribute names to ISO 19115-2 attribute names.
     */
    private static final String[] BBOX = {
        "west_longitude", "westBoundLongitude",
        "east_longitude", "eastBoundLongitude",
        "south_latitude", "southBoundLatitude",
        "north_latitude", "northBoundLatitude"
    };

    /**
     * The NetCDF file, or {@code null} if none. This field is set at construction time
     * and will be reset to {@code null} when not needed anymore, in order to let GC do
     * its work.
     */
    private NetcdfFile file;

    /**
     * The NetCDF variables, or {@code null} if none. Only one of {@link #file} or
     * {@code variables} can be non-null. This field is reset to {@code null} when
     * not needed anymore, in order to let GC do its work.
     */
    private VariableIF[] variables;

    /**
     * The format inferred from the content of the NetCDF file.
     * Will be created only when first needed.
     */
    private IIOMetadataFormat nativeFormat;

    /**
     * The native NetCDF metadata, created when first needed.
     */
    private Node netcdfMetadata;

    /**
     * {@code true} if we need to check for ISO metadata.
     * This will be done only if needed.
     */
    private boolean checkISO;

    /**
     * {@code true} if the sample dimensions declare at least one range of value. This is
     * always {@code true} for CF-conformant variables. However it may be {@code false}
     * for some non-standard variable which store their range in other variables rather
     * than attributes (case of IFREMER <cite>Caraïbes</cite> data).
     */
    private boolean hasValueRange;

    /**
     * Creates <cite>stream metadata</cite> from the specified file. Note that
     * {@link CoordSysBuilderIF#buildCoordinateSystems(NetcdfDataset)}
     * should have been invoked (if needed) before this constructor.
     *
     * @param reader The reader for which to assign the metadata.
     * @param file The file for which to read metadata.
     */
    public NetcdfMetadata(final ImageReader reader, final NetcdfFile file) {
        super(true, reader, null);
        Attribute attr = file.findGlobalAttribute("project_name");
        if (attr != null) {
            final MetadataNodeAccessor ac = new MetadataNodeAccessor(this, DiscoveryAccessor.ROOT);
            ac.setAttribute("citation", attr.getStringValue());
        }
        MetadataNodeAccessor ac = null;
        for (int i=0; i<BBOX.length; i+=2) {
            attr = file.findGlobalAttribute(BBOX[i]);
            if (attr != null) {
                if (ac == null) {
                    ac = new MetadataNodeAccessor(this, DiscoveryAccessor.GEOGRAPHIC_ELEMENT);
                    ac.setAttribute("inclusion", true);
                }
                ac.setAttribute(BBOX[i+1], attr.getStringValue());
            }
        }
        this.file = file;
        checkISO = true;
    }

    /**
     * Creates <cite>image metadata</cite> from the specified variables. Note that
     * {@link CoordSysBuilderIF#buildCoordinateSystems(NetcdfDataset)}
     * should have been invoked (if needed) before this constructor.
     * <p>
     * This constructor is usually invoked for exactly one variable, unless the user
     * assigned many variables to the same image index using the bands API.
     *
     * @param reader The reader for which to assign the metadata.
     * @param file The originating dataset file, or {@code null} if none.
     * @param variables The variables for which to read metadata.
     * @throws IOException If an I/O operation was needed and failed.
     */
    public NetcdfMetadata(final NetcdfImageReader reader, final NetcdfDataset file,
            final VariableIF... variables) throws IOException
    {
        super(false, reader, null);
        GDALGridMapping  gdal     = null;
        CoordinateSystem netcdfCS = null;
        List<Dimension>  domain   = null;
        for (final VariableIF variable : variables) {
            if (gdal == null && file != null) {
                /*
                 * Before to rely on CF convention, check for GDAL convention. GDAL declares
                 * the CRS in WKT format, together with the "grid to CRS" affine transform.
                 * Note that even if we find a CRS from GDAL conventions, we will still try
                 * to create CRS from CF convention in the next block below. This allows us
                 * to emmit a warning in case of mismatch.
                 */
                final String name = getStringValue(variable, GRID_MAPPING);
                if (name != null) {
                    final Map<String,GDALGridMapping> gridMapping = reader.getGridMapping();
                    gdal = gridMapping.get(name);
                    if (gdal == null) {
                        final Variable mapping = file.findVariable(name);
                        final String wkt = getStringValue(mapping, "spatial_ref");
                        final String gtr = getStringValue(mapping, "GeoTransform");
                        if (wkt != null || gtr != null) {
                            gdal = new GDALGridMapping(this, wkt, gtr);
                            gridMapping.put(name, gdal);
                        }
                    }
                }
            }
            /*
             * Before to rely on CF convention, check for ESRI convention. This is the same
             * principle than the above check for GDAL convention, but simpler. If both ESRI
             * and GDAL attributes are defined, then the GDAL attributes will have precedence.
             */
            if (gdal == null) {
                final String wkt = getStringValue(variable, "ESRI_pe_string");
                if (wkt != null) {
                    gdal = new GDALGridMapping(this, wkt, null);
                }
            }
            /*
             * Now check for CF-convention. If a CRS is found from CF convention, we will check
             * for consistency but the CRS found above (if any) will have precedence. We prefer
             * WKT definition rather than CF conventions because CF convention does not declare
             * (at the time of writing) datum or axis order.
             */
            if (variable instanceof Enhancements) {
                for (final CoordinateSystem cs : ((Enhancements) variable).getCoordinateSystems()) {
                    if (netcdfCS == null || priority(cs) > priority(netcdfCS)) {
                        netcdfCS = cs;
                    }
                }
            }
            /*
             * Get the domain of the first variable, and ensure that the domain of all other
             * variables is the same. We will need to domain later for sorting axes in an order
             * consistent with the data.
             */
            final List<Dimension> vd = variable.getDimensions();
            if (domain == null) {
                domain = vd;
            } else if (!domain.equals(vd)) {
                throw new IIOException(Errors.format(Errors.Keys.INCONSISTENT_DOMAIN_2,
                        variable.getShortName(), variables[0].getShortName()));
            }
        }
        setCoordinateSystem(reader, file, domain, netcdfCS,
                (gdal != null) ? gdal.crs : null,
                (gdal != null) ? gdal.gridToCRS : null);
        addSampleDimension(variables);
        this.variables = variables;
    }

    /**
     * Returns the string value of the given variable, or {@code null} if none.
     *
     * @param  variable The variable to look, or {@code null} if none.
     * @param  attributeName The attribute to look for.
     * @return The string value, or {@code null} if the variable or attribute was not found.
     */
    private static String getStringValue(final VariableIF variable, final String attributeName) {
        if (variable != null) {
            final Attribute attribute = variable.findAttributeIgnoreCase(attributeName);
            if (attribute != null) {
                return attribute.getStringValue();
            }
        }
        return null;
    }

    /**
     * Returns a "measurement" of the given coordinate system fitness for the purpose of grid
     * coverages. Higher numbers are better.
     */
    private static int priority(final CoordinateSystem cs) {
        int p = cs.isRegular() ? 2 : cs.isProductSet() ? 1 : 0;
        if (cs.isGeoReferencing()) {
            p |= 4;
        }
        return p;
    }

    /**
     * Sets the Coordinate Reference System to a value inferred from the specified
     * NetCDF object. This method wraps the given NetCDF coordinate system in to a
     * GeoAPI {@linkplain org.opengis.referencing.crs.CoordinateReferenceSystem
     * Coordinate Reference System} implementation.
     *
     * @param  file The originating dataset file, or {@code null} if none.
     * @param  domain The domain in NetCDF order (reverse of "natural" order).
     * @param  cs The NetCDF coordinate system to define in metadata, or {@code null}.
     * @param  crs Always {@code null}, unless an alternative CRS should be formatted
     *         in replacement of the CRS built from the given NetCDF coordinate system.
     * @param  gridToCRS The transform from pixel coordinates to CRS coordinates, or
     *         {@code null} if unknown.
     * @throws IOException If an I/O operation was needed and failed.
     */
    private void setCoordinateSystem(final NetcdfImageReader reader, final NetcdfDataset file,
            final List<Dimension> domain, final CoordinateSystem cs, CoordinateReferenceSystem crs,
            AffineTransform gridToCRS) throws IOException
    {
        /*
         * If a NetCDF coordinate system is available, wraps it as a GeoAPI implementation.
         * Use a cache of previously created objects in the current file, since the same CS
         * is typically used for many variables.
         */
        if (cs != null) {
            NetcdfCRSBuilder builder = reader.crsBuilder;
            if (builder == null) {
                reader.crsBuilder = builder = new NetcdfCRSBuilder(file, reader);
            }
            final List<Dimension> rd = new ArrayList<>(domain);
            Collections.reverse(rd);
            builder.setDomain(rd);
            builder.setCoordinateSystem(cs);
            builder.sortAxesAccordingDomain();
            final NetcdfCRS netcdfCRS = builder.getNetcdfCRS();
            /*
             * The following code is only a validity check. It may produce warnings,
             * but does not write any metadata at this stage.
             */
            final int dim = netcdfCRS.getDimension();
            for (int i=0; i<dim; i++) {
                final NetcdfAxis axis = netcdfCRS.getAxis(i);
                final String units = axis.delegate().getUnitsString();
                final int offset = units.lastIndexOf('_');
                if (offset >= 0) {
                    final String direction = units.substring(offset + 1).trim();
                    final String opposite = AxisDirections.opposite(axis.getDirection()).name();
                    if (direction.equalsIgnoreCase(opposite)) {
                        warning("setCoordinateSystem", Errors.Keys.INCONSISTENT_AXIS_ORIENTATION_2,
                                new String[] {axis.getCode(), direction});
                    }
                }
            }
            /*
             * The above was only a check. Now perform the metadata writing.
             */
            GridGeometry gridGeometry = null;
            CoordinateReferenceSystem regularCRS = netcdfCRS.regularize();
            if (regularCRS instanceof GridGeometry) {
                gridGeometry = (GridGeometry) regularCRS;
            }
            if (regularCRS == netcdfCRS) {
                /*
                 * ======== HACK !!!! ================================
                 * Try a few special cases. This is a hack which will
                 * need to be replaced by more generic approach later.
                 * ===================================================
                 */
                final IrregularGridConverter c = new IrregularGridConverter(file, domain, netcdfCRS, null);
                try {
                    final CoordinateReferenceSystem candidate = c.forROM();
                    if (candidate != null) {
                        regularCRS = candidate;
                        gridGeometry = c.getGridToCRS(regularCRS);
                    }
                } catch (Exception e) {
                    // We haven been unable to create a CRS. Do not write CRS metadata
                    // and continue reading. TODO: we should log a warning here.
                }
                // End of hack.
            }
            if (gridGeometry != null) {
                final GridDomainAccessor accessor = new GridDomainAccessor(this);
                accessor.setGridGeometry(gridGeometry, null, null);
                gridToCRS = null;
            }
            if (crs == null) {
                crs = regularCRS;
            }
        }
        if (gridToCRS != null) {
            new GridDomainAccessor(this).setGridToCRS(gridToCRS);
        }
        if (crs != null) {
            new ReferencingBuilder(this).setCoordinateReferenceSystem(crs);
        }
    }

    /**
     * Adds sample dimension information for the specified variables.
     *
     * @param  variables The variables to add as sample dimensions.
     */
    private void addSampleDimension(final VariableIF... variables) {
        final DimensionAccessor accessor = new DimensionAccessor(this);
        for (final VariableIF variable : variables) {
            final NetcdfVariable m;
            if (USE_UCAR_LIB && variable instanceof EnhanceScaleMissing) {
                m = new NetcdfVariable((EnhanceScaleMissing) variable);
            } else {
                m = new NetcdfVariable(variable);
            }
            accessor.selectChild(accessor.appendChild());
            accessor.setDescriptor(variable.getShortName());
            accessor.setUnits(m.units);
            if (variable instanceof EnhanceScaleMissing) {
                final EnhanceScaleMissing ev = (EnhanceScaleMissing) variable;
                if (!m.hasCollisions(ev)) {
                    accessor.setValueRange(ev.getValidMin(), ev.getValidMax());
                    if (Double.isNaN(m.scale) && Double.isNaN(m.offset)) {
                        m.setTransferFunction(ev);
                    }
                }
            }
            accessor.setValidSampleValue(m.minimum, m.maximum);
            accessor.setFillSampleValues(m.fillValues);
            if (!m.isGeophysics()) {
                accessor.setTransfertFunction(m.scale, m.offset, TransferFunctionType.LINEAR);
            }
            hasValueRange |= !(Double.isInfinite(m.minimum) && Double.isInfinite(m.maximum));
        }
    }

    /**
     * Workaround for non-standard NetCDF data. For CF-compliant data, the
     * {@link #hasValueRange} flag is {@code true} and this method does nothing.
     * <p>
     * Current implementation handles the following special cases:
     * <p>
     * <ul>
     *   <li>IFREMER <cite>Caraïbes</cite> data</li>
     * </ul>
     *
     * @param file The NetCDF file which contains the variables.
     * @throws IOException If an error occurred while reading the variable.
     *
     * @since 3.14
     */
    final void workaroundNonStandard(final NetcdfFile file) throws IOException {
        if (!hasValueRange) {
            final DimensionAccessor accessor = new DimensionAccessor(this);
            final int n = accessor.childCount();
            final double[] min = readVariable(file, "Minimum_value", n);
            final double[] max = readVariable(file, "Maximum_value", n);
            if (min != null && max != null) {
                for (int i=0; i<n; i++) {
                    accessor.selectChild(i);
                    accessor.setValidSampleValue(min[i], max[i]);
                }
            }
        }
    }

    /**
     * Returns the data of the given variable, provided that its length is equals or greater than
     * the given value. This method is invoked by {@link #workaroundNonStandard(NetcdfFile)} when
     * there is a chance that the minimum and maximum values are stored in separated variables
     * instead than attributes of the variable of interest.
     *
     * @param file The NetCDF file which contains the variables.
     * @param name The name of the variable which contains the minimum or maximum values.
     * @param n    The expected number of values.
     * @return An array of length <var>n</var> containing the values, or {@code null} if none.
     * @throws IOException If an error occurred while reading the variable.
     *
     * @since 3.14
     */
    private static double[] readVariable(final NetcdfFile file, final String name, final int n)
            throws IOException
    {
        Variable variable = file.findVariable(name);
        if (variable != null && variable.getRank() == 1 && variable.getShape(0) >= n) {
            final Array array;
            try {
                array = variable.read(new int[] {0}, new int[] {n});
            } catch (InvalidRangeException e) { // Should never happen.
                throw new IOException(e);
            }
            final double[] data = new double[n];
            for (int i=0; i<n; i++) {
                data[i] = array.getDouble(i);
            }
            return data;
        }
        return null;
    }

    /**
     * The metadata format for the enclosing {@link NetcdfMetadata} instance.
     */
    private final class Format extends SampleMetadataFormat {
        /**
         * Creates a new instance.
         */
        Format() {
            super(NATIVE_FORMAT_NAME);
        }

        /**
         * Returns the metadata to use for inferring the format.
         * This is invoked when first needed.
         */
        @Override
        protected Node getDataRootNode() {
            return getAsTree(NATIVE_FORMAT_NAME);
        }

        /**
         * Returns the data type associated to the given attribute.
         */
        @Override
        protected int getDataType(final Node attribute, final int index) {
            final Node element = ((Attr) attribute).getOwnerElement();
            if (element instanceof IIOMetadataNode) { // Paranoiac check (should always be true)
                final List<?> attributes = (List<?>) ((IIOMetadataNode) element).getUserObject();
                /*
                 * The list may be shorter than 'index' because of the hard-coded
                 * attributes added by the getAsTree(...) method.
                 */
                if (index < attributes.size()) {
                    final DataType type = ((Attribute) attributes.get(index)).getDataType();
                    switch (type) {
                        case BOOLEAN: return DATATYPE_BOOLEAN;
                        case BYTE:    // Fall through
                        case SHORT:   // Fall through
                        case INT:     return DATATYPE_INTEGER;
                        case FLOAT:   return DATATYPE_FLOAT;
                        case LONG:    // Fall through
                        case DOUBLE:  return DATATYPE_DOUBLE;
                    }
                } else {
                    final String name = attribute.getNodeName();
                    if (NetcdfVariable.VALID_MIN.equals(name) || NetcdfVariable.VALID_MAX.equals(name)) {
                        return DATATYPE_DOUBLE;
                    }
                }
            }
            return DATATYPE_STRING;
        }
    }

    /**
     * A unmodifiable list of attributes, together with the variable name.
     * The name is returned by the {@link #toString()} method in order to
     * get is displayed in the tree table in place of the enumeration of
     * all attributes contained in this list.
     */
    @SuppressWarnings("serial")
    private static final class AttributeList extends UnmodifiableArrayList<Attribute> {
        /**
         * The variable name.
         */
        private final String name;

        /**
         * Creates a new list for the given attributes and variable name.
         */
        AttributeList(final List<Attribute> attributes, final String name) {
            super(attributes.toArray(new Attribute[attributes.size()]));
            this.name = name;
        }

        /**
         * Returns the value to be displayed in the "value" column of the tree table.
         */
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * If the given format name is {@code "NetCDF"}, returns a "dynamic" metadata format
     * inferred from the actual content of the NetCDF file. Otherwise delegates to the super-class.
     */
    @Override
    public IIOMetadataFormat getMetadataFormat(final String formatName) {
        if (formatName != null) { // If null, let the super-class produce a better error message.
            if (formatName.equalsIgnoreCase(NATIVE_FORMAT_NAME)) {
                if (nativeFormat == null) {
                    nativeFormat = new Format();
                }
                return nativeFormat;
            }
        }
        return super.getMetadataFormat(formatName);
    }

    /**
     * If the given format name is {@code "NetCDF"}, returns the native metadata.
     * If the given format name is {@code "ISO-19115"}, returns the ISO metadata.
     * Otherwise returns the usual metadata as defined in the super-class.
     */
    @Override
    public Node getAsTree(final String formatName) {
        if (formatName != null) {
            /*
             * "NetCDF" metadata case (both stream and image metadata)
             */
            if (formatName.equalsIgnoreCase(NATIVE_FORMAT_NAME)) {
                if (netcdfMetadata != null) {
                    return netcdfMetadata;
                }
                final IIOMetadataNode root = new IIOMetadataNode(NATIVE_FORMAT_NAME);
                if (variables != null) {
                    for (final VariableSimpleIF var : variables) {
                        final IIOMetadataNode node = new IIOMetadataNode("Variable");
                        node.setNodeValue(var.getShortName());
                        appendAttributes(new AttributeList(var.getAttributes(), var.getShortName()), node);
                        node.setAttribute("data_type", String.valueOf(var.getDataType()));
                        if (var instanceof EnhanceScaleMissing) {
                            final EnhanceScaleMissing eh = (EnhanceScaleMissing) var;
                            node.setAttribute(NetcdfVariable.VALID_MIN, String.valueOf(eh.getValidMin()));
                            node.setAttribute(NetcdfVariable.VALID_MAX, String.valueOf(eh.getValidMax()));
                        }
                        root.appendChild(node);
                    }
                    variables = null; // Not needed anymore; let GC do its work.
                } else {
                    buildTree(file.getRootGroup(), root); // 'file' should never be null at this point.
                    if (!checkISO) {
                        file = null; // Not needed anymore; let GC do its work.
                    }
                }
                netcdfMetadata = root;
                return root;
            }
            /*
             * "ISO-19115" metadata case (stream metadata only).
             *
             * TODO: the tree is not yet built.
             */
            if (checkISO) {
                final boolean isReadOnly = isReadOnly();
                final IIOMetadataNode root = new IIOMetadataNode(ISO_FORMAT_NAME);
                try {
                    root.setUserObject(new NetcdfMetadataReader(file, this).read());
                    setReadOnly(false);
                    mergeTree(ISO_FORMAT_NAME, root);
                } catch (IOException e) {
                    throw new BackingStoreException(e); // Will be handled by GridCoverageReader.
                } finally {
                    setReadOnly(isReadOnly);
                }
                checkISO = false;
                if (netcdfMetadata != null) {
                    file = null; // Not needed anymore; let GC do its work.
                }
                return root;
            }
        }
        return super.getAsTree(formatName);
    }

    /**
     * Appends attributes to the given node. The node user object is set to the list of attributes.
     */
    private static void appendAttributes(final List<Attribute> attributes, final IIOMetadataNode node) {
        for (final Attribute attribute : attributes) {
            /*
             * NetCDF attributes can get string, numeric or array values. As
             * DOM node expects String object, we have to convert those types
             * to a single string.
             */
            final int length = attribute.getLength();
            final StringBuilder buffer = new StringBuilder();
            for (int i=0; i<length; i++) {
                Object value = attribute.getStringValue(i);
                if (value == null) {
                    value = attribute.getNumericValue(i);
                    if (value == null) {
                        continue;
                    }
                }
                if (i != 0) {
                    buffer.append(", ");
                }
                buffer.append(value);
            }
            node.setAttribute(attribute.getName(), buffer.toString());
        }
        node.setUserObject(attributes); // Required by Format.getDataType(Node, int).
    }

    /**
     * Invoked recursively for building the tree.
     */
    private static void buildTree(final Group group, final IIOMetadataNode parent) {
        if (group != null) {
            appendAttributes(group.getAttributes(), parent);
            for (final Group subgroup : group.getGroups()) {
                final IIOMetadataNode child = new IIOMetadataNode(subgroup.getShortName());
                buildTree(subgroup, child);
                parent.appendChild(child);
            }
        }
    }

    /**
     * Convenience method for logging a warning.
     */
    private void warning(final String method, final short key, final Object value) {
        LogRecord record = Errors.getResources(getLocale()).getLogRecord(Level.WARNING, key, value);
        record.setSourceClassName(NetcdfMetadata.class.getName());
        record.setSourceMethodName(method);
        warningOccurred(record);
    }
}
