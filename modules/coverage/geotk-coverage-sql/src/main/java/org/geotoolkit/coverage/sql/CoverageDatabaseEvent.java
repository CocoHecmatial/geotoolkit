/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2010, Geomatys
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
package org.geotoolkit.coverage.sql;

import java.util.EventObject;


/**
 * The event delivered when the content of a {@linkplain CoverageDatabase Coverage Database}
 * is about to change, or after the content has been changed.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.12
 *
 * @see CoverageDatabaseListener
 *
 * @since 3.12 (derived from Seagis)
 * @module
 */
public class CoverageDatabaseEvent extends EventObject {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5238430708385410876L;

    /**
     * {@code true} if the event occurs before the change, or
     * {@code false} if the event occurs after the change.
     */
    private final boolean isBefore;

    /**
     * Number of entries added, or a negative number if entries are being removed.
     */
    private final int numEntryChange;

    /**
     * Creates a new event having the given database as its source.
     *
     * @param source The source of this event.
     * @param isBefore {@code true} if the event is invoked before the change,
     *        or {@code false} if the event occurs after the change.
     * @param numEntryChange Number of entries added, or a negative number if entries are being removed.
     */
    public CoverageDatabaseEvent(final CoverageDatabase source, final boolean isBefore, final int numEntryChange) {
        super(source);
        this.isBefore = isBefore;
        this.numEntryChange = numEntryChange;
    }

    /**
     * Returns the database in which entries are added or removed.
     */
    @Override
    public CoverageDatabase getSource() {
        return (CoverageDatabase) super.getSource();
    }

    /**
     * Returns whetever this event is occuring before the change is actually applied in the
     * database. {@linkplain DatabaseCoverageListener Listeners} can veto the change before
     * it is applied, but can not veto the change after it has been applied.
     *
     * @return {@code true} if the event occurs before the change, or
     *         {@code false} if the event occurs after the change.
     */
    public boolean isBefore() {
        return isBefore;
    }

    /**
     * Returns whatever new entries are added or existing entries removed, and how many of them.
     * This method typically returns +1 or -1, but is not restricted to those values.
     *
     * @return Number of entries added, or a negative number if entries are being removed.
     */
    public int getNumEntryChange() {
        return numEntryChange;
    }
}
