/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2002-2009, Open Source Geospatial Foundation (OSGeo)
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

/**
 * {@linkplain org.opengis.referencing.AuthorityFactory Authority factories} for the
 * <A HREF="http://www.epsg.org">EPSG</A> database.
 *
 *
 * {@section Installation}
 *
 * See <a href="doc-files/install.html">installation instructions</a>.
 *
 *
 * {@section Fetching a connection}
 *
 * The connection to the database can be specified in a simple properties file or through JNDI.
 * The steps used for fetching the connection parameters are described there:
 * <p>
 * </ul>
 *   <li>{@link org.geotoolkit.referencing.factory.epsg.ThreadedEpsgFactory#getDataSource}</li>
 * </ul>
 *
 *
 * {@section Getting a factory instance}
 *
 * An EPSG authority factory is created using the following code:
 *
 * {@preformat java
 *     CRSAuthorityFactory factory = AuthorityFactoryFinder.getCRSAuthorityFactory("EPSG", null);
 * }
 *
 *
 * {@section How CRS are identified}
 *
 * EPSG codes are numerical identifiers. For example "4326" is the EPSG identifier for the
 * "<cite>WGS 84</cite>" geographic CRS. However, the default implementation accepts names
 * as well as numeric identifiers. For example "<cite>NTF (Paris) / France I</cite>" and
 * {@code "27581"} both fetchs the same object. Note that names may be ambiguous since the
 * same name may be used for more than one object. This is the case of "<cite>WGS 84</cite>"
 * for example. If such an ambiguity is found, an exception will be thrown.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @author Yann Cézard (IRD)
 * @author Rueben Schulz (UBC)
 * @author Matthias Basler
 * @author Andrea Aime (TOPP)
 * @author Jody Garnett (Refractions)
 * @author Didier Richard (IGN)
 * @author John Grange
 *
 * @version 3.00
 *
 * @since 2.1
 * @module
 */
package org.geotoolkit.referencing.factory.epsg;
