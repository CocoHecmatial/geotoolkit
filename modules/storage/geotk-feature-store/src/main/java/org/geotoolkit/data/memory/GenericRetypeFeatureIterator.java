/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2003-2008, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2010, Geomatys
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

package org.geotoolkit.data.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.FeatureReader;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.factory.Hints;
import org.apache.sis.util.Classes;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.FeatureFactory;
import org.geotoolkit.feature.Property;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.PropertyDescriptor;

/**
 * Supports on the fly retyping of  FeatureIterator contents.
 * This handle limiting visible attributs.
 *
 * @author Jody Garnett (Refractions Research)
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public abstract class GenericRetypeFeatureIterator<R extends FeatureIterator> implements FeatureIterator {

    protected static final FeatureFactory FF = FeatureFactory.LENIENT;

    protected final R iterator;

    /**
     * Creates a new instance of GenericRetypeFeatureIterator
     *
     * @param iterator FeatureReader to limit
     */
    private GenericRetypeFeatureIterator(final R iterator) {
        this.iterator = iterator;
    }

    /**
     * Supplies mapping from original to target FeatureType.
     *
     * <p>
     * Will also ensure that mapping results in a valid selection of values
     * from the original. Only the xpath expression and binding are checked.
     * </p>
     *
     * @param target Desired FeatureType
     * @param original Original FeatureType
     *
     * @return Mapping from originoal to target FeatureType
     *
     * @throws IllegalArgumentException if unable to provide a mapping
     */
    protected static PropertyDescriptor[] typeAttributes(final FeatureType original, final FeatureType target) {

        if (target.equals(original)) {
            throw new IllegalArgumentException("FeatureReader already produces contents with the correct schema");
        }

        final Collection<PropertyDescriptor> targetDesc = target.getDescriptors();

        if (targetDesc.size() > original.getDescriptors().size()) {
            throw new IllegalArgumentException("Unable to retype  FeatureReader (original does not cover requested type)");
        }

        final PropertyDescriptor[] types = targetDesc.toArray(new PropertyDescriptor[targetDesc.size()]);

        //verify types binding
        for (PropertyDescriptor attrib : types) {
            final String xpath = attrib.getName().getLocalPart();
            final PropertyDescriptor check = original.getDescriptor(xpath);
            final Class<?> targetBinding = attrib.getType().getBinding();
            final Class<?> checkBinding = check.getType().getBinding();
            if (!targetBinding.isAssignableFrom(checkBinding)) {
                throw new IllegalArgumentException(
                        "Unable to retype FeatureReader for " + xpath +
                        " as " + Classes.getShortName(checkBinding) +
                        " cannot be assigned to " + Classes.getShortName(targetBinding));
            }
        }

        return types;
    }

    /**
     * Generate a table of indexes betwwen two simplefeaturetypes.
     * Allow acces by index rather then by names.
     *
     * @param target Desired FeatureType
     * @param original Original FeatureType
     *
     * @return Mapping from originoal to target FeatureType
     *
     * @throws IllegalArgumentException if unable to provide a mapping
     */
    protected static int[] typeIndexes(final FeatureType original, final FeatureType target) {

        if (target.equals(original)) {
            throw new IllegalArgumentException("FeatureReader already produces contents with the correct schema");
        }

        final List<PropertyDescriptor> originalDesc = new ArrayList<PropertyDescriptor>(original.getDescriptors());
        final Collection<PropertyDescriptor> targetDesc = target.getDescriptors();

        if (targetDesc.size() > originalDesc.size()) {
            throw new IllegalArgumentException("Unable to retype  FeatureReader (original does not cover requested type)");
        }


        final int[] types = new int[targetDesc.size()];

        int i=0;
        for (PropertyDescriptor attrib : targetDesc) {
            final String xpath = attrib.getName().getLocalPart();
            final PropertyDescriptor check = original.getDescriptor(xpath);
            types[i] = originalDesc.indexOf(check);

            final Class<?> targetBinding = attrib.getType().getBinding();
            final Class<?> checkBinding = check.getType().getBinding();
            if (!targetBinding.isAssignableFrom(checkBinding)) {
                throw new IllegalArgumentException(
                        "Unable to retype FeatureReader for " + xpath +
                        " as " + Classes.getShortName(checkBinding) +
                        " cannot be assigned to " + Classes.getShortName(targetBinding));
            }
            i++;
        }

        return types;
    }


    /**
     * {@inheritDoc }
     */
    @Override
    public void close() throws FeatureStoreRuntimeException {
        iterator.close();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean hasNext() throws FeatureStoreRuntimeException {
        return iterator.hasNext();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(Classes.getShortClassName(this));
        sb.append('\n');
        String subIterator = "\u2514\u2500\u2500" + iterator.toString(); //move text to the right
        subIterator = subIterator.replaceAll("\n", "\n\u00A0\u00A0\u00A0"); //move text to the right
        sb.append(subIterator);
        return sb.toString();
    }

    /**
     * Wrap a FeatureReader with a new featuretype.
     *
     * @param <T> extends FeatureType
     * @param <F> extends Feature
     * @param <R> extends FeatureReader<T,F>
     */
    private static final class GenericSeparateRetypeFeatureReader extends GenericRetypeFeatureIterator<FeatureReader> implements FeatureReader{

        /**
         * The descriptors we are going to from the original reader
         */
        private final PropertyDescriptor[] types;

        protected final FeatureType mask;

        private GenericSeparateRetypeFeatureReader(final FeatureReader reader, final FeatureType mask){
            super(reader);
            this.mask = mask;
            types = typeAttributes(reader.getFeatureType(), mask);
        }

        @Override
        public Feature next() throws FeatureStoreRuntimeException {
            final Feature next = iterator.next();

            final Collection<Property> properties = new ArrayList<Property>();
            for(final PropertyDescriptor prop : types){
                properties.addAll(next.getProperties(prop.getName()));
            }

            final Feature cp = FF.createFeature(properties, mask, next.getIdentifier().getID());
            //copy user datas
            cp.getUserData().putAll(next.getUserData());
            return cp;
        }

        @Override
        public FeatureType getFeatureType() {
            return mask;
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

    private static final class GenericRetypeFeatureCollection extends WrapFeatureCollection{

        private final FeatureType mask;

        private GenericRetypeFeatureCollection(final FeatureCollection original, final FeatureType mask){
            super(original);
            this.mask = mask;
        }

        @Override
        public FeatureType getFeatureType() {
            return mask;
        }

        @Override
        public FeatureIterator iterator(final Hints hints) throws FeatureStoreRuntimeException {
            FeatureIterator ite = getOriginalFeatureCollection().iterator(hints);
            if(!(ite instanceof FeatureReader)){
                ite = GenericWrapFeatureIterator.wrapToReader(ite, getOriginalFeatureCollection().getFeatureType());
            }
            return wrap((FeatureReader)ite, mask, hints);
        }

        @Override
        protected Feature modify(Feature original) throws FeatureStoreRuntimeException {
            throw new UnsupportedOperationException("should not have been called.");
        }

    }


    /**
     * Wrap a FeatureReader with a new featuretype.
     */
    public static FeatureReader wrap(final FeatureReader reader, final FeatureType mask, final Hints hints){
        final FeatureType original = reader.getFeatureType();
        if(mask.equals(original)){
            //same type mapping, no need to wrap it
            return reader;
        } else {
            return new GenericSeparateRetypeFeatureReader(reader,mask);
        }
    }

    /**
     * Create a retyped FeatureCollection wrapping the given collection.
     */
    public static FeatureCollection wrap(final FeatureCollection original, final FeatureType mask){
        return new GenericRetypeFeatureCollection(original, mask);
    }

}
