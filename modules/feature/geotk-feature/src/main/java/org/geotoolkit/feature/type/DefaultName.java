/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.feature.type;

import java.util.Collections;
import java.util.Objects;
import javax.xml.namespace.QName;
import org.apache.sis.util.iso.DefaultTypeName;
import org.apache.sis.internal.system.DefaultFactories;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
import org.opengis.util.ScopedName;


/**
 * Simple implementation of Name.
 * <p>
 * This class emulates QName, and is used as the implementation of both AttributeName and
 * TypeName (so when the API settles down we should have a quick fix.
 * <p>
 * Its is advantageous to us to be able to:
 * <ul>
 * <li>Have a API in agreement with QName - considering our target audience
 * <li>Strongly type AttributeName and TypeName separately
 * </ul>
 * The ISO interface move towards combining the AttributeName and Attribute classes,
 * and TypeName and Type classes, while we understand the attractiveness of this on a
 * UML diagram it is very helpful to keep these concepts separate when playing with
 * a strongly typed language like java.
 * </p>
 * <p>
 * It case it is not obvious this is a value object and equality is based on
 * namespace and name.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @author Johann Sorel, Geomatys
 * @module pending
 *
 * @deprecated The GeoAPI {@code Name} interface is expected to be replaced by {@link org.opengis.util.GenericName}.
 */
@Deprecated
public class DefaultName extends DefaultTypeName implements GenericName {

    public static GenericName create(final QName qname) {
        return create(qname.getNamespaceURI(), qname.getLocalPart());
    }

    public static GenericName create(final String local) {
        return create(null,local);
    }
    
    public static GenericName create(final String namespace, final String local) {

        // WARNING: DefaultFactories.NAMES is not a public API and may change in any future SIS version.
        if(namespace==null){
            return DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, local);
        }else{
            return DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, namespace, local);
        }
    }


    /**
     * Namespace / scope
     */
    private final String namespace;

    /**
     * Local part
     */
    private final String local;

    /**
     * Constructs an instance with the local part set. Namespace / scope is
     * set to null.
     *
     * @param local The local part of the name.
     */
    private DefaultName(final String local) {
        this(null, local);
    }

    private DefaultName(final QName qname) {
        this(qname.getNamespaceURI(), qname.getLocalPart());
    }

    /**
     * Constructs an instance with the local part and namespace set.
     *
     * @param namespace The namespace or scope of the name.
     * @param local The local part of the name.
     *
     */
    private DefaultName(final String namespace, final String local) {

        // WARNING: DefaultFactories.NAMES is not a public API and may change in any future SIS version.

        super(namespace == null ? null : DefaultFactories.forBuildin(NameFactory.class).createNameSpace(
                DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, namespace),
                Collections.singletonMap("separator.head", ":")), local != null ? local : "unnamed");
        this.namespace = namespace;
        this.local = local;
        //return DefaultFactories.forBuildin(NameFactory.class).createGenericName(null, namespace, local);
    }

    /**
     * Returns a hash code value for this operand.
     */
    @Override
    public int hashCode() {
        return (namespace == null ? 0 : namespace.hashCode()) +
                37 * (local == null ? 0 : local.hashCode());
    }

    /**
     * value object with equality based on name and namespace.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof GenericName) {
            final GenericName other = (GenericName) obj;
            final String ns1 = DefaultName.getNamespace(this);
            final String ns2 = DefaultName.getNamespace(other);
            if( (ns2 != null && !ns2.isEmpty()) &&
                (ns1 != null  && !ns1.isEmpty()) ){
                if (!Objects.equals(ns1, ns2)) {
                    return false;
                }
            }
            if (!Objects.equals(this.local, other.tip().toString())) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Parse a string value that can be expressed in 2 different forms :
     * JSR-283 extended form : {uri}localpart
     * Separator form : uri:localpart
     *
     * if the given string do not match any, then a Name with no namespace will be
     * created and the localpart will be the given string.
     *
     * @param candidate
     * @return Name
     */
    public static GenericName valueOf(final String candidate){

        if(candidate.startsWith("{")){
            //name is in extended form
            return toSessionNamespaceFromExtended(candidate);
        }

        int index = candidate.lastIndexOf(':');

        if(index <= 0){
            return DefaultName.create(null, candidate);
        }else{
            final String uri = candidate.substring(0,index);
            final String name = candidate.substring(index+1,candidate.length());
            return DefaultName.create(uri, name);
        }

    }

    private static GenericName toSessionNamespaceFromExtended(final String candidate) {
        final int index = candidate.indexOf('}');

        if(index == -1) throw new IllegalArgumentException("Invalide extended form : "+ candidate);

        final String uri = candidate.substring(1, index);
        final String name = candidate.substring(index+1, candidate.length());

        return DefaultName.create(uri, name);
    }

    public static String toExtendedForm(final GenericName name){
        final String ns = DefaultName.getNamespace(name);
        if(ns==null || ns.isEmpty()){
            return name.toString();
        }else{
            return new StringBuilder(ns).append(':').append(name.tip().toString()).toString();
        }
    }

    public static String toExpandedString(final GenericName name){
        String ns = getNamespace(name);
        if(ns==null){
            return name.tip().toString();
        }else{
            return new StringBuilder("{").append(ns).append('}').append(name.tip().toString()).toString();
        }
    }

    /**
     * Tests that the given string representation matches the given name.
     * String can be written with only the local part or in extendedform or JCR
     * extended form.
     *
     * @param name
     * @param candidate
     * @return true if the string match the name
     */
    public static boolean match(final GenericName name, final String candidate){
        if(candidate.startsWith("{")){
            //candidate is in extended form
            return candidate.equals(toExpandedString(name));
        }

        final int index = candidate.lastIndexOf(':');

        if(index <= 0){
            return candidate.equals(name.tip().toString());
        }else{
            final String uri = candidate.substring(0,index);
            final String local = candidate.substring(index+1,candidate.length());
            return uri.equals(getNamespace(name)) && local.equals(name.tip().toString());
        }
    }

    public static boolean match(final GenericName name, final GenericName candidate){
        final String ns1 = getNamespace(name);
        final String ns2 = getNamespace(candidate);
        if(ns1==null || ns2==null){
            //compare only localpart
            return name.tip().toString().equals(candidate.tip().toString());
        }else{
            return name.toString().equals(candidate.toString());
        }
    }

    public static String getNamespace(GenericName name){
        return (name instanceof ScopedName) ? ((ScopedName)name).path().toString() : null;
    }

}
