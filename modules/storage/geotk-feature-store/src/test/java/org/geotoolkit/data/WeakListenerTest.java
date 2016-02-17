/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.data;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.sis.storage.DataStoreException;

import org.geotoolkit.data.memory.MemoryFeatureStore;
import org.geotoolkit.feature.FeatureTypeBuilder;

import org.junit.Test;
import org.geotoolkit.feature.type.FeatureType;

import static org.junit.Assert.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class WeakListenerTest extends org.geotoolkit.test.TestBase {


    public WeakListenerTest() {
    }

    /**
     * Test no memory leak in weak style listener
     */
    @Test
    public void testWeakStorageListener() throws DataStoreException {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("test1");
        ftb.add("att", Integer.class);
        final FeatureType type1 = ftb.buildFeatureType();
        ftb.reset();
        ftb.setName("test2");
        ftb.add("att2", Integer.class);
        final FeatureType type2 = ftb.buildFeatureType();


        final AtomicInteger count = new AtomicInteger(0);

        final FeatureStore store = new MemoryFeatureStore();

        FeatureStoreListener listener = new FeatureStoreListener() {
            @Override
            public void structureChanged(FeatureStoreManagementEvent event) {
                count.incrementAndGet();
            }
            @Override
            public void contentChanged(FeatureStoreContentEvent event) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };

        final FeatureStoreListener.Weak ref = new FeatureStoreListener.Weak(listener);
        ref.registerSource(store);

        store.createFeatureType(type1.getName(), type1);
        assertEquals(1, count.get());
        listener = null;
        System.gc();

        store.createFeatureType(type2.getName(), type2);
        //listener should have desapear now, so the event should not have been send
        assertEquals(1, count.get());
    }
}
