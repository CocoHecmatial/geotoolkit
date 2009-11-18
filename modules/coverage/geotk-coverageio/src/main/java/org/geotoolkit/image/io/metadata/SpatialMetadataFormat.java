/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.image.io.metadata;

import java.util.Date;
import java.util.Locale;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;

import org.opengis.util.CodeList;
import org.opengis.util.RecordType;
import org.opengis.annotation.Obligation;

// We use a lot of different metadata interfaces in this class.
// It is a bit too tedious to declare all of them.
import org.opengis.metadata.*;
import org.opengis.metadata.extent.*;
import org.opengis.metadata.spatial.*;
import org.opengis.metadata.quality.*;
import org.opengis.metadata.lineage.*;
import org.opengis.metadata.content.*;
import org.opengis.metadata.citation.*;
import org.opengis.metadata.constraint.*;
import org.opengis.metadata.acquisition.*;
import org.opengis.metadata.maintenance.*;
import org.opengis.metadata.distribution.*;
import org.opengis.metadata.identification.*;
import org.opengis.metadata.identification.Identification; // Override the package class.
import org.opengis.metadata.content.Band; // Override the package class.

import org.opengis.coverage.grid.GridCell;
import org.opengis.coverage.grid.GridPoint;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.RectifiedGrid;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.primitive.Point;
import org.opengis.util.InternationalString;

import org.geotoolkit.internal.CodeLists;
import org.geotoolkit.util.NumberRange;
import org.geotoolkit.util.converter.Classes;
import org.geotoolkit.util.NullArgumentException;
import org.geotoolkit.metadata.KeyNamePolicy;
import org.geotoolkit.metadata.NullValuePolicy;
import org.geotoolkit.metadata.TypeValuePolicy;
import org.geotoolkit.metadata.ValueRestriction;
import org.geotoolkit.metadata.MetadataStandard;
import org.geotoolkit.gui.swing.tree.TreeTableNode;
import org.geotoolkit.gui.swing.tree.Trees;
import org.geotoolkit.resources.Errors;


/**
 * Describes the structure of {@linkplain SpatialMetadata spatial metadata}.
 * This class infers the tree structure from metadata objects defined by some
 * {@linkplain MetadataStandard metadata standard}, typically ISO 19115-2. New
 * metadata elements are declared by calls to the {@link #addTree addTree} method.
 * Default formats are defined <a href="#default-formats">below</a>.
 *
 * {@section String formatting in attributes}
 * The following formatting rules apply:
 * <p>
 * <ul>
 *   <li>Numbers are formatted as in the {@linkplain Locale#US US locale}, i.e.
 *       as {@link Integer#toString(int)} or {@link Double#toString(double)}.</li>
 *   <li>Dates are formatted with the {@code "yyyy-MM-dd HH:mm:ss"}
 *       {@linkplain java.text.SimpleDateFormat pattern} in UTC
 *       {@linkplain java.util.TimeZone timezone}.</li>
 * </ul>
 *
 * <a name="default-formats">{@section Default formats}</a>
 * The default {@link #STREAM} and {@link #IMAGE} formats are inferred from a subset of the
 * {@link Metadata} and {@link ImageDescription} interfaces, respectively. Consequently those
 * instances can be considered as profiles of ISO 19115-2, with a few minor departures:
 * <p>
 * <ul>
 *   <li>The {@link Band} interface defined by ISO 19115-2 is used only when the values are
 *       measurements of wavelengths in the electromagnetic spectrum, as specified in the ISO
 *       specification. Otherwise the {@link SampleDimension} interface (which is very similar)
 *       is used.</li>
 * </ul>
 * <p>
 * The tree structures are show below. As a general rule, the name of <em>elements</em> start with
 * a upper case letter while the name of <em>attributes</em> start with a lower case letter.
 * For browsing these trees in an applet together with additional information, see the
 * <a href="http://www.geotoolkit.org/demos/geotk-simples/applet/IIOMetadataPanel.html">IIOMetadataPanel applet</a>.
 *
<blockquote><table border="1" cellpadding="12">
<tr bgcolor="lightblue"><th>Stream metadata</th><th>Image metadata</th></tr>
<tr><td nowrap valign="top" width="50%">
<pre>geotk-coverageio_3.06
├───<b>DiscoveryMetadata</b> : {@linkplain DataIdentification}
│   ├───citation
│   ├───abstract
│   ├───purpose
│   ├───credits
│   ├───status
│   ├───<b>DescriptiveKeywords</b> : {@linkplain Keywords}[]
│   │   └───DescriptiveKeywordsEntry
│   │       ├───keywords
│   │       ├───thesaurusName
│   │       └───type
│   ├───<b>SpatialResolution</b> : {@linkplain Resolution}
│   │   ├───distance
│   │   └───EquivalentScale
│   │       └───denominator
│   ├───topicCategories
│   ├───environmentDescription
│   ├───<b>Extent</b> : {@linkplain Extent}
│   │   ├───description
│   │   ├───<b>GeographicElement</b> : {@linkplain GeographicBoundingBox}
│   │   │   ├───inclusion
│   │   │   ├───westBoundLongitude
│   │   │   ├───eastBoundLongitude
│   │   │   ├───southBoundLatitude
│   │   │   └───northBoundLatitude
│   │   └───<b>VerticalElement</b> : {@linkplain VerticalExtent}
│   │       ├───minimumValue
│   │       ├───maximumValue
│   │       └───verticalCRS
│   └───supplementalInformation
├───<b>AcquisitionMetadata</b> : {@linkplain AcquisitionInformation}
│   ├───<b>EnvironmentalConditions</b> : {@linkplain EnvironmentalRecord}
│   │   ├───averageAirTemperature
│   │   ├───maxRelativeHumidity
│   │   ├───maxAltitude
│   │   └───meteorologicalConditions
│   └───<b>Platform</b> : {@linkplain Platform}
│       ├───citation
│       ├───identifier
│       ├───description
│       └───Instruments
│           └───<b>Instrument</b> : {@linkplain Instrument}
│               ├───citation
│               ├───Identifier : {@linkplain Identifier}
│               │   ├───code
│               │   └───authority
│               ├───type
│               └───description
└───<b>QualityMetadata</b> : {@linkplain DataQuality}
    └───<b>Report</b> : {@linkplain Element}
        ├───namesOfMeasure
        ├───measureIdentification
        ├───measureDescription
        ├───evaluationMethodType
        ├───evaluationMethodDescription
        ├───evaluationProcedure
        └───date</pre>
</td><td nowrap valign="top" width="50%">
<pre>geotk-coverageio_3.06
├───<b>ImageDescription</b> : {@linkplain ImageDescription}
│   ├───contentType
│   ├───illuminationElevationAngle
│   ├───illuminationAzimuthAngle
│   ├───imagingCondition
│   ├───ImageQualityCode : {@linkplain Identifier}
│   │   ├───code
│   │   └───authority
│   ├───cloudCoverPercentage
│   ├───ProcessingLevelCode : {@linkplain Identifier}
│   │   ├───code
│   │   └───authority
│   ├───compressionGenerationQuantity
│   ├───triangulationIndicator
│   ├───radiometricCalibrationDataAvailable
│   ├───cameraCalibrationInformationAvailable
│   ├───filmDistortionInformationAvailable
│   ├───lensDistortionInformationAvailable
│   ├───<b>Dimensions</b> : {@linkplain SampleDimension}[]
│   │   └───Dimension
│   │       ├───descriptor
│   │       ├───sequenceIdentifier
│   │       ├───validSampleValues
│   │       ├───fillSampleValues
│   │       ├───minValue
│   │       ├───maxValue
│   │       ├───units
│   │       ├───peakResponse
│   │       ├───bitsPerValue
│   │       ├───toneGradation
│   │       ├───scaleFactor
│   │       ├───offset
│   │       ├───bandBoundaryDefinition
│   │       ├───nominalSpatialResolution
│   │       ├───transferFunctionType
│   │       ├───transmittedPolarization
│   │       └───detectedPolarization
│   └───<b>RangeElementDescriptions</b> : {@linkplain RangeElementDescription}
│       └───RangeElementDescription
│           ├───name
│           ├───definition
│           └───rangeElements
├───<b>SpatialRepresentation</b> : {@linkplain Georectified}
│   ├───numberOfDimensions
│   ├───cellGeometry
│   ├───centerPoint
│   └───pointInPixel
└───<b>RectifiedGridDomain</b> : {@linkplain RectifiedGrid}
    ├───<b>Limits</b> : {@linkplain GridEnvelope}
    │   ├───low
    │   └───high
    ├───origin
    └───OffsetVectors
        └───OffsetVector
            └───values</pre>
</tr></table></blockquote>
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.06
 *
 * @see SpatialMetadata
 *
 * @since 3.04 (derived from 2.4)
 * @module
 */
public class SpatialMetadataFormat extends IIOMetadataFormatImpl {
    /**
     * The metadata format name, which is {@value}. The {@link javax.imageio.metadata} package
     * description requires that we provide a version number as part of the format name. The
     * version number provided in this constant is set to the last Geotk version when this
     * format has been modified, and may change in any future version.
     */
    public static final String FORMAT_NAME = "geotk-coverageio_3.06";

    /**
     * The policy for the names of the nodes to be inferred from the ISO objects.
     * We use JavaBeans names instead of UML identifiers in order to get the plural
     * form for collections.
     */
    static KeyNamePolicy NAME_POLICY = KeyNamePolicy.JAVABEANS_PROPERTY;

    /**
     * The default instance for <cite>stream</cite> metadata format. This is the metadata
     * format that apply to file as a whole, which may contain more than one image. The
     * tree structure is documented in the <a href="#default-formats">class javadoc</a>.
     *
     * @see #addTreeForStream()
     *
     * @since 3.05
     */
    public static final SpatialMetadataFormat STREAM;
    static {
        STREAM = new SpatialMetadataFormat(FORMAT_NAME);
        STREAM.addTreeForStream();
    }

    /**
     * The default instance for <cite>image</cite> metadata format. This
     * is the metadata format that apply to a particular image in a file.
     * The tree structure is documented in the <a href="#default-formats">class javadoc</a>.
     *
     * @see #addTreeForImage()
     *
     * @since 3.05
     */
    public static final SpatialMetadataFormat IMAGE;
    static {
        IMAGE = new SpatialMetadataFormat(FORMAT_NAME);
        IMAGE.addTreeForImage();
    }

    /**
     * The metadata standards represented by this format. The most
     * common standard is {@link MetadataStandard#ISO_19115 ISO_19115}.
     */
    private final Map<String,MetadataStandard> standards = new HashMap<String,MetadataStandard>();

    /**
     * The last value returned by {@link #getDescriptions}, cached on the assumption
     * that the description of different attributes of the same element are likely
     * to be asked a few consecutive time.
     */
    private volatile transient MetadataDescriptions descriptions;

    /**
     * Creates an initially empty format. Subclasses shall invoke the various
     * {@code addFoo(...)} methods defined in this class or parent class for
     * adding new elements and attributes.
     *
     * @param rootName the name of the root element.
     */
    protected SpatialMetadataFormat(final String rootName) {
        super(rootName, CHILD_POLICY_SOME);
    }

    /**
     * Makes sure an argument is non-null.
     *
     * @param  name   Argument name.
     * @param  object User argument.
     * @throws NullArgumentException if {@code object} is null.
     */
    private static void ensureNonNull(String name, Object object) throws NullArgumentException {
        if (object == null) {
            throw new NullArgumentException(Errors.format(Errors.Keys.NULL_ARGUMENT_$1, name));
        }
    }

    /**
     * Adds the tree structure for <cite>stream</cite> metadata. The default implementation
     * adds the tree structure documented in the "<cite>Stream metadata</cite>" column of the
     * <a href="#default-formats">class javadoc</a>.
     *
     * @see #STREAM
     *
     * @since 3.05
     */
    protected void addTreeForStream() {
        final Map<Class<?>,Class<?>> substitution = new HashMap<Class<?>,Class<?>>(20);
        /*
         * Metadata excluded because they are redundant with standard API.
         */
        substitution.put(Format.class,                    null);  // Redundant with ImageReaderWriterSpi.
        substitution.put(Locale.class,                    null);  // Specified in ImageReader.getLocale().
        substitution.put(CharacterSet.class,              null);  // Fixed to Unicode in java.lang.String.
        substitution.put(BrowseGraphic.class,             null);  // Redundant with Image I/O Thumbnails.
        substitution.put(SpatialRepresentationType.class, null);  // Fixed to "grid" for Image I/O.
        /*
         * Metadata excluded because we are not interrested in (at this time). Their
         * inclusion introduce large sub-trees that would need to be simplified.  We
         * may revisit some of those exclusion in a future version, when we get more
         * experience about what are needed.
         */
        substitution.put(Usage.class,                  null);  // MD_DataIdentification.resourceSpecificUsage
        substitution.put(ResponsibleParty.class,       null);  // MD_DataIdentification.pointOfContact
        substitution.put(Constraints.class,            null);  // MD_DataIdentification.resourceConstraints
        substitution.put(MaintenanceInformation.class, null);  // MD_DataIdentification.resourceMaintenance
        substitution.put(AggregateInformation.class,   null);  // MD_DataIdentification.aggregationInfo
        substitution.put(Plan.class,                   null);  // MI_AcquisitionInformation.acquisitionPlan
        substitution.put(Objective.class,              null);  // MI_AcquisitionInformation.objective
        substitution.put(Operation.class,              null);  // MI_AcquisitionInformation.operation
        substitution.put(Requirement.class,            null);  // MI_AcquisitionInformation.acquisitionRequirement
        substitution.put(Scope.class,                  null);  // DQ_DataQuality.scope
        substitution.put(Lineage.class,                null);  // DQ_DataQuality.lineage
        substitution.put(Result.class,                 null);  // DQ_DataQuality.report.result
        /*
         * Metadata excluded because not yet implemented.
         */
        substitution.put(TemporalExtent.class, null);
        /*
         * Metadata simplification, where elements are replaced by attributes. The simplification
         * is especially important for Citation because they appear in many different places with
         * the same name ("citation"),  while Image I/O does not allow many element nodes to have
         * the same name (this is not strictly forbidden, but the getter methods return information
         * only about the first occurence of the given name. Note however that having the same name
         * under different element node is not an issue for attributes). In addition, the Citation
         * sub-tree is very large and we don't want to allow the tree to growth that big.
         */
        substitution.put(Citation.class,   String.class);
        substitution.put(Citation[].class, String.class);
        substitution.put(Identifier.class, String.class);
        /*
         * Metadata excluded because they introduce circularity or because
         * they appear more than once (we shall not declare two nodes with
         * the same name in Image I/O). Some will be added by hand later.
         */
        substitution.put(Instrument.class, null);  // MI_AcquisitionInformation.instrument
        /*
         * Collections replaced by singletons, because only one
         * instance is enough for the purpose of stream metadata.
         */
        substitution.put(Extent[].class,           Extent.class);            // MD_DataIdentification.extent
        substitution.put(GeographicExtent[].class, GeographicExtent.class);  // MD_DataIdentification.extent.geographicElement
        substitution.put(VerticalExtent[].class,   VerticalExtent.class);    // MD_DataIdentification.extent.verticalElement
        substitution.put(Resolution[].class,       Resolution.class);        // MD_DataIdentification.spatialResolution
        substitution.put(Platform[].class,         Platform.class);          // MI_AcquisitionInformation.platform
        substitution.put(Element[].class,          Element.class);           // DQ_DataQuality.report
        substitution.put(Date[].class,             Date.class);              // DQ_DataQuality.report.dateTime
        /*
         * Since this set of metadata is about gridded data,
         * replace the generic interfaces by specialized ones.
         */
        substitution.put(Identification.class,        DataIdentification.class);
        substitution.put(SpatialRepresentation.class, GridSpatialRepresentation.class);
        substitution.put(GeographicExtent.class,      GeographicBoundingBox.class);
        /*
         * Build the tree.
         */
        final String root = getRootName();
        final MetadataStandard standard = MetadataStandard.ISO_19115;
        addTree(standard, DataIdentification.class,     "DiscoveryMetadata",   root, substitution);
        addTree(standard, AcquisitionInformation.class, "AcquisitionMetadata", root, substitution);
        addTree(standard, DataQuality.class,            "QualityMetadata",     root, substitution);
        removeAttribute("EquivalentScale", "doubleValue");
        /*
         * Add by hand a node in the place where it would have been added if we didn't
         * excluded it. We do this addition because Instruments appear in two places,
         * while we want only the occurence that appear under the "Platform" node.
         */
        substitution.put(Platform.class, null);
        substitution.remove(Identifier.class); // Allow full expansion.
        addTree(standard, Instrument[].class, "Instruments", "Platform", substitution);
    }

    /**
     * Adds the tree structure for <cite>image</cite> metadata. The default implementation
     * adds the tree structure documented in the "<cite>Image metadata</cite>" column of the
     * <a href="#default-formats">class javadoc</a>.
     *
     * @see #IMAGE
     *
     * @since 3.05
     */
    protected void addTreeForImage() {
        final Map<Class<?>,Class<?>> substitution = new HashMap<Class<?>,Class<?>>(4);
        substitution.put(Citation.class,       String.class);   // MD_ImageDescription.xxxCode
        substitution.put(RecordType.class,     null);           // MD_CoverageDescription.attributeDescription
        substitution.put(RangeDimension.class, Band.class);     // MD_CoverageDescription.dimension
        /*
         * Adds the "ImageDescription" node derived from ISO 19115.
         * The 'fillSampleValues' attribute is a Geotk extension.
         */
        final String root = getRootName();
        MetadataStandard standard = MetadataStandard.ISO_19115;
        addTree(standard, ImageDescription.class, "ImageDescription", root, substitution);
        addAttribute("Dimension", "validSampleValues", DATATYPE_STRING, false, null);
        addAttribute("Dimension", "fillSampleValues",  DATATYPE_DOUBLE, false, 0, Integer.MAX_VALUE);
        addObjectValue("Dimension", SampleDimension.class, true, null); // Replace Band.class.
        /*
         * Adds the "SpatialRepresentation" node derived from ISO 19115.
         * We ommit the information about spatial-temporal axis properties (the Dimension object)
         * because it is redundant with the information provided in the CRS and offset vectors.
         */
        substitution.put(Dimension.class,           null);  // GridSpatialRepresentation.axisDimensionProperties
        substitution.put(Point.class,     double[].class);  // MD_Georectified.centerPoint
        substitution.put(GCP.class,                 null);  // MD_Georectified.checkPoint
        substitution.put(Boolean.TYPE,              null);  // MD_Georectified.checkPointAvailability
        substitution.put(InternationalString.class, null);  // MD_Georectified.various descriptions...
        addTree(standard, Georectified.class, "SpatialRepresentation", root, substitution);
        removeAttribute("SpatialRepresentation", "cornerPoints");
        /*
         * Adds the "RectifiedGridDomain" node derived from ISO 19123.
         */
        substitution.put(String.class,          null); // CV_Grid.axisNames
        substitution.put(GridCell.class,        null); // CV_Grid.cell
        substitution.put(GridPoint.class,       null); // CV_Grid.intersection
        substitution.put(GridEnvelope.class,    null); // CV_Grid.extent (will be added later)
        substitution.put(GridCoordinates.class, int[].class);    // CV_GridEnvelope.low/high
        substitution.put(DirectPosition.class,  double[].class); // CV_RectifiedGrid.origin
        standard = MetadataStandard.ISO_19123;
        addTree(standard, RectifiedGrid.class, "RectifiedGridDomain", root, substitution);
        /*
         * Following is part of ISO 19123 and "GML in JPEG 2000" specifications,
         * but under different names. We use the "GML in JPEG 2000" names.
         */
        addTree(standard, GridEnvelope.class, "Limits", "RectifiedGridDomain", substitution);
        removeAttribute("Limits",              "dimension"); // Redundant with the one in RectifiedGridDomain.
        removeAttribute("RectifiedGridDomain", "dimension"); // Redundant with the one in SpatialRepresentation.
    }

    /**
     * Adds a new element or attribute of the given type as a child of the root. This method
     * performs the same work than {@link #addTree(MetadataStandard, Class, String, String, Map)},
     * except that the element is added at the root and the name is inferred from the given type
     * for convenience.
     *
     * @param standard     The metadata standard of the element or attribute to be added.
     * @param type         The type of the element or attribute to be added.
     * @param substitution The map of children types to substitute by other types, or {@code null}.
     */
    protected void addTree(final MetadataStandard standard, final Class<?> type,
            final Map<Class<?>,Class<?>> substitution)
    {
        ensureNonNull("standard", standard);
        ensureNonNull("type",     type);
        addTree(standard, type, type.getSimpleName(), getRootName(), substitution);
    }

    /**
     * Adds a new element or attribute of the given type and name as a child of the given node. If
     * the given type is a metadata, then that child is {@linkplain #addElement(String,String,int)
     * added as an element} and all its children are added recursively. Otherwise the type is
     * {@linkplain #addAttribute(String,String,int,boolean,String) added as an attribute}.
     * <p>
     * This method can be given an optional <cite>substitution map</cite>. If this map is non
     * null, then every occurence of a class in the set of keys is replaced by the associated
     * class in the collection of values. The purpose of this map is to:
     *
     * <ul>
     *   <li><p>Replace a base class by some specialized subclass. Since {@code IIOMetadata} are
     *   about grided data (not generic {@code Feature}s), the exact subtype is often known and
     *   we want the additional attributes to be declared inconditionnaly. Example:</p>
     *
     * <blockquote><pre>substitution.put({@linkplain RangeDimension}.class, {@linkplain Band}.class);</pre></blockquote></li>
     *
     *   <li><p>Exclude a particular class. This is conceptually equivalent to setting the target
     *   type to {@code null}. This is used for excluding metadata type which bring a large tree
     *   of dependencies that we may not be interrested in. Example:</p>
     *
     * <blockquote><pre>substitution.put({@linkplain Objective}.class, null);</pre></blockquote></li>
     *
     *   <li><p>Replace an element class (including the whole tree behind it) by a single attribute.
     *   This simplification is especially useful for {@code Citation} because they typically appear
     *   in many different places with the same name ("<cite>citation</cite>"), while Image I/O does
     *   not allow many elements to have the same name (actually this is not strictly forbidden, but
     *   the getter methods return information only about the first occurence of a given name).
     *   Converting an element to an attribute allow it to appear with the same name under different
     *   nodes, and can make the tree considerably simplier (at the cost of losing all the sub-tree
     *   below the converted element). Example:</p>
     *
     * <blockquote><pre>substitution.put({@linkplain Citation}.class, String.class);</pre></blockquote></li>
     *
     *   <li><p>Replace a collection by a singleton. This is conceptually equivalent to setting the
     *   source type to an array, and the target type to the element of that array. This is useful
     *   when a collection seems an overkill for the specific case of stream or image metadata.
     *   Example:</p>
     *
     * <blockquote><pre>substitution.put({@linkplain Identification}[].class, {@linkplain Identification}.class);</pre></blockquote></li>
     * </ul>
     *
     * The substitution map applies only to childs (if any), not to the type given directly to this
     * method.
     *
     * @param standard      The metadata standard of the element or attribute to be added.
     * @param type          The type of the element or attribute to be added.
     * @param elementName   The name of the element or attribute node to be added.
     * @param parentName    The name of the parent node to where to add the child.
     * @param substitution  The map of children types to substitute by other types, or {@code null}.
     */
    protected void addTree(final MetadataStandard standard, Class<?> type,
            final String elementName, final String parentName,
            final Map<Class<?>,Class<?>> substitution)
    {
        ensureNonNull("standard",    standard);
        ensureNonNull("type",        type);
        ensureNonNull("elementName", elementName);
        ensureNonNull("parentName",  parentName);
        final Set<Class<?>> exclude = new HashSet<Class<?>>();
        if (substitution != null) {
            for (final Map.Entry<Class<?>,Class<?>> entry : substitution.entrySet()) {
                if (entry.getValue() == null) {
                    exclude.add(entry.getKey());
                }
            }
        }
        /*
         * If the given type is an arrray, handle as a collection  (i.e. we will add a
         * "Elements" parent node, and declare in that parent a single "Element" child
         * which can be repeated many time).
         */
        int max = 1;
        String identifier = null;
        if (type.isArray()) {
            type = type.getComponentType();
            max = Integer.MAX_VALUE;
            try {
                identifier = standard.getInterface(type).getSimpleName();
            } catch (ClassCastException e) {
                // Not an implementation of the expected standard.
                // It may be an "ordinary" object from the JDK.
                identifier = elementName;
            }
        }
        addTree(standard, type, identifier, elementName, parentName, 0, max, null, exclude, substitution);
    }

    /**
     * Implementation of {@link #addTree(Class,String,String, Map)} with a set of attribute type
     * to exclude. This method invokes itself recursively. The given set will be modified in order
     * to avoid infinite recursivity (e.g. {@code Identifier.getAuthority().getIdentifiers()}).
     *
     * @param standard     The metadata standard of the element or attribute to be added.
     * @param type         The type of the element or attribute to be added.
     * @param identifier   The UML identifier, or {@code null} if unknown.
     * @param elementName  The name of the element or attribute node to be added.
     * @param parentName   The name of the parent node to where to add the child.
     * @param minOccurence Minimal occurence of the element or attribute in the parent node.
     * @param maxOccurence Maximal occurence of the element or attribute in the parent node.
     * @param restriction  The restriction on the valid values, or {@code null} if none.
     * @param exclude      The attribute types to exclude. This set will be modified.
     * @param substitution The classes to substitute by other classes.
     *        This user-supplied map applies only on childs and is not modified.
     */
    private void addTree(final MetadataStandard standard, final Class<?> type,
            String identifier, String elementName, String parentName,
            final int minOccurence, final int maxOccurence, final ValueRestriction restriction,
            final Set<Class<?>> exclude, final Map<Class<?>,Class<?>> substitution)
    {
        if (maxOccurence == 0) return;
        final boolean mandatory = (minOccurence != 0);
         /*
         * CodeList    ⇒    Attribute VALUE_ENUMERATION
         *
         * The enums are the code list elements. There is no default value.
         */
        if (CodeList.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            final Class<CodeList<?>> codeType = (Class<CodeList<?>>) type;
            final List<String> codes = Arrays.asList(CodeLists.identifiers(codeType));
            addAttribute(parentName, elementName, DATATYPE_STRING, mandatory, null, codes);
            return;
        }
        /*
         * JSE type    ⇒    Attribute VALUE_ARBITRARY | VALUE_LIST | VALUE_ENUMERATION
         *
         * If the element is not an other object from the same metadata standard, handles it as
         * an attribute. Everything which can not be handled by one of the DATATYPE_* constants
         * is handled as a String.
         */
        if (!standard.isMetadata(type)) {
            int dataType = typeOf(type);
            if (maxOccurence != 1) {
                /*
                 * Collection  ⇒  Attribute VALUE_LIST
                 *
                 * In most case, we are adding a list of String or double values. But in a
                 * few cases we add a list of double[] arrays (e.g. "offsetVectors"), in
                 * which cases we need to insert a compound element in the tree.
                 */
                final Class<?> component = type.getComponentType();
                if (component != null) {
                    // The container for the repeated elements (CHILD_POLICY_REPEAT)
                    elementName = toElementName(elementName);
                    addElement(elementName, parentName, minOccurence, maxOccurence);
                    standards.put(elementName, standard);

                    // The repeated element with no child, only a single attribute.
                    parentName  = elementName;
                    elementName = toComponentName(elementName, identifier, true);
                    addElement(elementName, parentName, CHILD_POLICY_EMPTY);
                    standards.put(elementName, standard);

                    // The attribute of kind VALUE_LIST.
                    parentName  = elementName;
                    elementName = "values";
                    dataType    = typeOf(component);
                }
                addAttribute(parentName, elementName, dataType, mandatory, minOccurence, maxOccurence);
            } else if (dataType == IIOMetadataFormat.DATATYPE_BOOLEAN) {
                /*
                 * Boolean  ⇒  Attribute VALUE_ENUMERATION
                 *
                 * A default value (false) is provided only if the attribute is
                 * not mandatory, otherwise we will require the user to specify
                 * a value explicitly.
                 */
                addBooleanAttribute(parentName, elementName, !mandatory, false);
            } else if (restriction != null && restriction.range != null) {
                /*
                 * Number  ⇒  Attribute VALUE_RANGE[_?_INCLUSIVE]
                 */
                final NumberRange<?> range = restriction.range;
                addAttribute(parentName, elementName, dataType, mandatory, null,
                        toString(range.getMinValue()), toString(range.getMaxValue()),
                        range.isMinIncluded(), range.isMaxIncluded());
            } else {
                /*
                 * Object  ⇒  Attribute VALUE_ARBITRARY
                 */
                addAttribute(parentName, elementName, dataType, mandatory, null);
            }
            return;
        }
        /*
         * Collection of Metadata    ⇒    Element CHILD_POLICY_REPEAT
         *
         * The 'elementName' is inferred from the method name and is typically in plural
         * form (at least in GeoAPI interfaces).  We add a node for 'elementName', which
         * can contain many occurences of the actual metadata structure. The new node is
         * set as the parent of the actual metadata structure. The name of that metadata
         * structure is set to the UML identifier, which is typically the same name than
         * 'elementName' except that it is in singular form.
         */
        elementName = toElementName(elementName);
        if (maxOccurence != 1) {
            addElement(elementName, parentName, minOccurence, maxOccurence);
            standards.put(elementName, standard);
            parentName  = elementName;
            identifier  = toElementName(identifier);
            elementName = toComponentName(elementName, identifier, false);
        }
        /*
         * Metadata singleton    ⇒    Element CHILD_POLICY_SOME|ALL|CHOICE|EMPTY
         *
         * If every childs have the same obligation,
         * then we will apply the following mapping:
         *
         *   MANDATORY   ⇒ CHILD_POLICY_ALL
         *   CONDITIONAL ⇒ CHILD_POLICY_CHOICE  (this is assuming that XOR is the condition)
         *   FORBIDDEN   ⇒ CHILD_POLICY_EMPTY
         *
         * Otherwise the policy is CHILD_POLICY_SOME.
         */
        boolean hasChilds = false;
        Obligation obligation = Obligation.FORBIDDEN; // If there is no child.
        final Map<String,String> identifiers;
        final Map<String,ValueRestriction> restrictions;
        final Map<String,Class<?>> propertyTypes, elementTypes;
        identifiers   = standard.asNameMap       (type, KeyNamePolicy.  UML_IDENTIFIER, NAME_POLICY);
        propertyTypes = standard.asTypeMap       (type, TypeValuePolicy.PROPERTY_TYPE,  NAME_POLICY);
        elementTypes  = standard.asTypeMap       (type, TypeValuePolicy.ELEMENT_TYPE,   NAME_POLICY);
        restrictions  = standard.asRestrictionMap(type, NullValuePolicy.NON_NULL,       NAME_POLICY);
        for (final Map.Entry<String,Class<?>> entry : elementTypes.entrySet()) {
            final Class<?> candidate = entry.getValue();
            if (standard.isMetadata(candidate) && !CodeList.class.isAssignableFrom(candidate)) {
                final ValueRestriction vr = restrictions.get(entry.getKey());
                if (vr != null) {
                    final Obligation c = vr.obligation;
                    if (c != null) {
                        if (!hasChilds) {
                            hasChilds = true;
                            obligation = c;
                            continue;
                        }
                        if (c.equals(obligation)) {
                            continue;
                        }
                    }
                }
                // Found an obligation which is unknown or different than the previous ones.
                obligation = null;
                hasChilds = true;
                break;
            }
        }
        if (obligation == null) {
            // The obligation is not the same for every child.
            obligation = Obligation.OPTIONAL;
        }
        final int childPolicy;
        switch (obligation) {
            case MANDATORY:   childPolicy = CHILD_POLICY_ALL;    break;
            case CONDITIONAL: childPolicy = CHILD_POLICY_CHOICE; break;
            case FORBIDDEN:   childPolicy = CHILD_POLICY_EMPTY;  break;
            default:          childPolicy = CHILD_POLICY_SOME;   break;
        }
        /*
         * At this point we have determined the child policy to apply to the new node.
         * Now add the child elements. The loop below invokes this method recursively
         * for each attribute of the metadata object that we are adding.
         */
        addElement(elementName, parentName, childPolicy);
        addObjectValue(elementName, type, false, null);
        standards.put(elementName, standard);
        for (final Map.Entry<String,Class<?>> entry : propertyTypes.entrySet()) {
            String childName = entry.getKey();
            final ValueRestriction vr = restrictions.get(childName);
            int min = 0, max = 1;
            if (vr != null && vr.obligation != null) {
                switch (vr.obligation) {
                    case MANDATORY: min = 1; break;
                    case FORBIDDEN: max = 0; break;
                }
            }
            Class<?> childType = entry.getValue();
            if (Collection.class.isAssignableFrom(childType)) {
                // Replace the collection type by the type of elements in that collection.
                childType = elementTypes.get(childName);
                if (childType == null) {
                    /*
                     * We have been unable to find the element type.
                     * Silently ignore.
                     */
                    continue;
                }
                max = Integer.MAX_VALUE;
            }
            /*
             * If the caller specified a substitution map, then we perform two checks:
             *
             * 1) If we have a collection (max > 1), then check if the caller wants to
             *    replace the collection (identified by an array type) by a singleton.
             *
             * 2) Then check if we want to replace the element type by an other element
             *    type. It could be a new array type.
             */
            if (substitution != null) {
                Class<?> replacement = null;
                if (max > 1) { // Collection case.
                    replacement = substitution.get(Classes.changeArrayDimension(childType, 1));
                    if (replacement != null) {
                        childType = replacement;    // Typically, the replacement type is the same.
                        childName = identifiers.get(childName); // Replace plural by singular form.
                        max = 1;
                    }
                }
                replacement = substitution.get(childType);
                if (replacement != null) {
                    childType = replacement;
                    replacement = childType.getComponentType();
                    if (childType.isArray()) {
                        max = Integer.MAX_VALUE;
                        childType = replacement;
                    }
                }
            }
            /*
             * We now have all the properties for the child that we want to add. Invoke this method
             * recursively for proceding to the addition, with guard against infinite recursivity.
             */
            if (exclude.add(childType)) {
                addTree(standard, childType, identifiers.get(childName), childName, elementName,
                        min, max, vr, exclude, substitution);
                if (!exclude.remove(childType)) {
                    throw new AssertionError(childType);
                }
            }
        }
    }

    /**
     * Returns one of the {@code DATATYPE_*} constant for the given class. If no constant
     * matches, then returns {@code DATATYPE_STRING} on the assumption that all attributes
     * have a sensible {@link Object#toString()} implementation.
     *
     * @param  type The class for which the {@code DATATYPE_*} constant is desired.
     * @return The {@code DATATYPE_*} constant for the given class.
     */
    private static int typeOf(Class<?> type) {
        type = Classes.primitiveToWrapper(type);
        if (Number.class.isAssignableFrom(type)) {
            if (Classes.isInteger(type)) {
                return IIOMetadataFormat.DATATYPE_INTEGER;
            }
            if (Float.class.isAssignableFrom(type)) {
                return IIOMetadataFormat.DATATYPE_FLOAT;
            }
            return IIOMetadataFormat.DATATYPE_DOUBLE;
        }
        if (Boolean.class.isAssignableFrom(type)) {
            return IIOMetadataFormat.DATATYPE_BOOLEAN;
        }
        return IIOMetadataFormat.DATATYPE_STRING;
    }

    /**
     * Makes the first character an upper-case letter. This is used for element names,
     * which typically starts with an upper-case letter in Image I/O metadata.
     *
     * @param  elementName The element name.
     * @return The given name with the first character converted to an upper-case letter.
     *
     * @since 3.06
     */
    static String toElementName(String elementName) {
        if (elementName != null && (elementName = elementName.trim()).length() != 0) {
            final char c = elementName.charAt(0);
            final char u = Character.toUpperCase(c);
            if (c != u) {
                final StringBuilder buffer = new StringBuilder(elementName);
                buffer.setCharAt(0, u);
                elementName = buffer.toString();
            }
        }
        return elementName;
    }

    /**
     * Returns the name of an entry in a collection.
     *
     * @param  elementName The Java-Beans name of the collection. This is usually plural.
     * @param  identifier  The UML identifier of the same element than above.
     *                     This is usually singular. It may be {@code null}
     * @param  attribute   {@code true} if the {@code elementName} is actually for an attribute.
     * @return The name of an entry in the collection.
     */
    static String toComponentName(final String elementName, final String identifier, final boolean attribute) {
        if (identifier != null && !identifier.equalsIgnoreCase(elementName)) {
            return identifier;
        }
        if (attribute && elementName.endsWith("s")) {
            /*
             * Try to make singular assuming an English speeling (we are already making the same
             * assumption when adding the "Entry" suffix below). We do that only for attributes,
             * not for elements, because elements may be complex structures in which the plural
             * form is intentional.
             *
             * Examples:
             *  - "DescriptiveKeywords" is an element with "Keywords" (and others) attributes.
             *    We don't want to make it singular, because it can contains many keywords.
             *  - "offsetVectors" is an attribute of type List<double[]>, which is converted
             *    by this class as an "offserVectors" element with "offsetVector" childs.
             */
            return elementName.substring(0, elementName.length() - 1);
        }
        // This is used only as a fallback.
        return (identifier != null ? identifier : elementName) + "Entry";
    }

    /**
     * Returns a string representation of the given value, or
     * {@code null} if that value is null (unbounded range).
     */
    private static String toString(final Comparable<?> value) {
        return (value != null) ? value.toString() : null;
    }

    /**
     * Removes an element from the format. If no element with the given
     * name was present, nothing happens and no exception is thrown.
     *
     * @param elementName the name of the element to be removed.
     */
    @Override
    protected void removeElement(final String elementName) {
        super.removeElement(elementName);
        standards.remove(elementName);
    }

    /**
     * Returns {@code true} if the element (and the subtree below it) is allowed to appear
     * in a metadata document for an image of the given type. The default implementation
     * always returns {@code true}.
     */
    @Override
    public boolean canNodeAppear(final String elementName, final ImageTypeSpecifier imageType) {
        return true;
    }

    /**
     * Returns the element which is the parent of the named element, or {@code null} if none.
     * For example if this metadata format is the {@link #STREAM} instance, then:
     * <p>
     * <ul>
     *   <li>The path to {@code "GeographicElement"} is {@code "DiscoveryMetadata/Extent/GeographicElement"}.</li>
     *   <li>The parent of {@code "GeographicElement"} returned by this method is {@code "Extent"}.</li>
     * </ul>
     *
     * {@note An element may have more than one parent, since the same element can be copied under
     *        many nodes using <code>addChildElement(...)</code>. In such case, this method returns
     *        only the first parent. Note that this case does not occur with the <code>STREAM</code>
     *        and <code>IMAGE</code> formats defined in <code>SpatialMetadataFormat</code>.}
     *
     * @param  elementName The element for which the parent is desired.
     * @return The parent of the given element, or {@code null}.
     *
     * @see #getElementPath(String)
     *
     * @since 3.06
     */
    public String getElementParent(final String elementName) {
        ensureNonNull("elementName", elementName);
        return getElementParent(getRootName(), elementName, null);
    }

    /**
     * Returns the element which is the parent of the named element, or {@code null} if none.
     * <p>
     * <b>Note:</b> Current implementation is somewhat inefficient.  We could maintain a map of
     * parents when new elements are added, but {@link IIOMetadataFormatImpl} already maintains
     * such map - I'm not sure why they do no provide API for getting that info. This API could
     * have been implemented as:
     *
     * {@preformat java
     *     public String[] getElementParents(String elementName) {
     *         List<String> parents = getElement(elementName).parentList;
     *         return parents.toString(new String[parents.size()]);
     *     }
     * }
     *
     * @param  root The root element from which to starts the scan.
     * @param  elementName The element for which the parent is desired.
     * @param  path If non-null, a buffer where to append the path before the node.
     * @return The parent of the given element, or {@code null}.
     */
    private String getElementParent(final String root, final String elementName, final StringBuilder path) {
        final String[] childs = getChildNames(root);
        if (childs != null) {
            for (final String child : childs) {
                if (child.equals(elementName)) {
                    return root;
                }
            }
            // Do recursive call only after we checked every childs at the root. If a name
            // appears twice (it should not), we will favor the one at the lowest depth.
            for (final String child : childs) {
                final String candidate = getElementParent(child, elementName, path);
                if (candidate != null) {
                    if (path != null) {
                        path.insert(0, '/').insert(0, child);
                    }
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Returns the path to the named element, or {@code null} if none. For example if this
     * metadata format is the {@link #STREAM} instance, then the path to the
     * {@code "GeographicElement"} is {@code "DiscoveryMetadata/Extent/GeographicElement"}.
     *
     * {@note An element may have more than one path, since the same element can be copied under
     *        many nodes using <code>addChildElement(...)</code>. In such case, this method returns
     *        only the first path. Note that this case does not occur with the <code>STREAM</code>
     *        and <code>IMAGE</code> formats defined in <code>SpatialMetadataFormat</code>.}
     *
     * @param  elementName The element for which the path is desired.
     * @return The path to the given element, or {@code null}.
     *
     * @see #getElementParent(String)
     *
     * @since 3.06
     */
    public String getElementPath(final String elementName) {
        ensureNonNull("elementName", elementName);
        final StringBuilder path = new StringBuilder();
        final String parent = getElementParent(getRootName(), elementName, path);
        if (parent != null) {
            // The parent is already in the path at this point.
            return path.append(elementName).toString();
        }
        return null;
    }

    /**
     * Returns the metadata standard implemented by the element of the given name.
     * If the given element does not implement a standard (which may happen if the
     * element was not added by an {@link #addTree addTree(...)} method), then this
     * method returns {@code null}.
     *
     * @param  elementName The element for which the standard is desired.
     * @return The standard implemented by the given element, or {@code null}.
     *
     * @since 3.06
     */
    public MetadataStandard getElementStandard(final String elementName) {
        return standards.get(elementName);
    }

    /**
     * Returns a description of the named element, or {@code null}. The desciption will be
     * localized for the supplied locale if possible.
     * <p>
     * The default implementation first queries the
     * {@linkplain MetadataStandard#asDescriptionMap description map} associated with the
     * {@linkplain #standard metadata standard}. If no description is found, then the
     * {@linkplain IIOMetadataFormatImpl#getElementDescription super-class implementation}
     * is used.
     *
     * @param  elementName The name of the element.
     * @param  locale The Locale for which localization will be attempted, or null.
     * @return The attribute description.
     *
     * @since 3.05
     */
    @Override
    public String getElementDescription(final String elementName, final Locale locale) {
        ensureNonNull("elementName", elementName);
        String description = getDescription(elementName, null, locale);
        if (description == null) {
            description = super.getElementDescription(elementName, locale);
        }
        return description;
    }

    /**
     * Returns a description of the named attribute, or {@code null}. The desciption will be
     * localized for the supplied locale if possible.
     * <p>
     * The default implementation first queries the
     * {@linkplain MetadataStandard#asDescriptionMap description map} associated with the
     * {@linkplain #standard metadata standard}. If no description is found, then the
     * {@linkplain IIOMetadataFormatImpl#getAttributeDescription super-class implementation}
     * is used.
     *
     * @param  elementName The name of the element.
     * @param  attrName    The name of the attribute.
     * @param  locale      The Locale for which localization will be attempted, or null.
     * @return The attribute description.
     *
     * @since 3.05
     */
    @Override
    public String getAttributeDescription(final String elementName, final String attrName, final Locale locale) {
        ensureNonNull("elementName", elementName);
        ensureNonNull("attrName",    attrName);
        String description = getDescription(elementName, attrName, locale);
        if (description == null) {
            description = super.getAttributeDescription(elementName, attrName, locale);
        }
        return description;
    }

    /**
     * Returns the description of the given attribute of the given element, in the given locale.
     * If the attribute is null, then this method assumes that the caller want the description
     * of the element itself. If there is no description available, returns {@code null}.
     *
     * @param  elementName The name of the element in which to search for attributes.
     * @param  attrName The name of the attribute for which the descriptions is desired, or {@code null}.
     * @param  locale The locale of the descriptions, or {@code null} for the default.
     * @return The requested description, or {@code null} if none.
     */
    private String getDescription(String elementName, String attrName, Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        if (attrName == null) {
            attrName = elementName;
            elementName = getElementParent(elementName);
            if (elementName == null) {
                return null;
            }
        }
        MetadataDescriptions candidate = descriptions;
        if (candidate == null || !locale.equals(candidate.locale) || !elementName.equals(candidate.elementName)) {
            Class<?> type = null;
            try {
                type = getObjectClass(elementName);
            } catch (IllegalArgumentException e) {
                // The given element does not allow the storage of objects.
                // We will set the description map to an empty map.
            }
            Map<String,String> desc = Collections.emptyMap();
            if (type != null) {
                final MetadataStandard standard = standards.get(elementName);
                if (standard != null) try {
                    desc = standard.asDescriptionMap(type, locale, NAME_POLICY);
                } catch (ClassCastException e) {
                    // The element type is not an instance of the expected standard.
                    // We will set the description map to an empty map.
                }
            }
            candidate = new MetadataDescriptions(desc, elementName, locale);
            descriptions = candidate;
        }
        return candidate.descriptions.get(attrName);
    }

    /**
     * Returns a <cite>tree table</cite> representation of this metadata standard.
     * This convenience method delegates the work to {@link MetadataTreeTable}.
     *
     * @param  locale The locale for which localization will be attempted, or {@code null}.
     * @return A tree representation of this metadata standard.
     */
    public TreeTableNode toTreeTable(final Locale locale) {
        final MetadataTreeTable tree = new MetadataTreeTable(this);
        if (locale != null) {
            tree.setLocale(locale);
        }
        return tree.getRootNode();
    }

    /**
     * Returns a string representation of this format.
     * The default implementation formats this object as a tree.
     */
    @Override
    public String toString() {
        return Trees.toString(toTreeTable(null));
    }
}
