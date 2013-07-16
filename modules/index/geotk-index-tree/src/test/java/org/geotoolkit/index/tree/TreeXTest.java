///*
// *    Geotoolkit.org - An Open Source Java GIS Toolkit
// *    http://www.geotoolkit.org
// *
// *    (C) 2009-2012, Geomatys
// *
// *    This library is free software; you can redistribute it and/or
// *    modify it under the terms of the GNU Lesser General Public
// *    License as published by the Free Software Foundation;
// *    version 2.1 of the License.
// *
// *    This library is distributed in the hope that it will be useful,
// *    but WITHOUT ANY WARRANTY; without even the implied warranty of
// *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// *    Lesser General Public License for more details.
// */
//package org.geotoolkit.index.tree;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import org.apache.sis.geometry.GeneralEnvelope;
//import org.geotoolkit.filter.SpatialFilterType;
//import org.geotoolkit.index.tree.io.DefaultTreeVisitor;
//import org.geotoolkit.index.tree.io.StoreIndexException;
//import org.geotoolkit.index.tree.io.TreeElementMapper;
//import org.geotoolkit.index.tree.io.TreeVisitor;
//import org.geotoolkit.index.tree.io.TreeX;
//import org.geotoolkit.index.tree.star.StarRTree;
//import org.geotoolkit.referencing.crs.DefaultEngineeringCRS;
//import static org.junit.Assert.assertTrue;
//import org.junit.Test;
//import org.opengis.geometry.Envelope;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//
///**
// * Test static TreeX methods.
// * Intersect test is already effectuate by tree test suite.
// *
// * @author Rémi Maréchal (Geomatys).
// */
//public class TreeXTest extends TreeTest {
//
//    List listSearch = new ArrayList();
//    TreeVisitor defVisit = new DefaultTreeVisitor(listSearch);
//    Tree tree ;
//    List<Envelope> lData = new ArrayList<>();
//    private TreeXElementMapperTest tXEM ;
//
//    public TreeXTest() throws StoreIndexException, IOException {
//        final GeneralEnvelope geTemp = new GeneralEnvelope(DefaultEngineeringCRS.CARTESIAN_3D);
//        for(int z = 0; z<=200; z+=20) {
//            for(int y = 0; y<=200; y+=20) {
//                for(int x = 0; x<=200; x+=20) {
//                    geTemp.setEnvelope(x-5, y-5, z-5, x+5, y+5, z+5);
////                    tree.insert(new GeneralEnvelope(geTemp));
//                    lData.add(new GeneralEnvelope(geTemp));
//                }
//            }
//        }
//        tXEM = new TreeXElementMapperTest(DefaultEngineeringCRS.CARTESIAN_3D);
//        tree = new StarRTree(4, DefaultEngineeringCRS.CARTESIAN_3D, tXEM);
//        for (Envelope env: lData) {
//            tree.insert(env);
//        }
//    }
//    
//    private List getresult(List listSearch) {
//        List<Envelope> lResult = new ArrayList<Envelope>();
//        for (int i = 0, s = listSearch.size(); i < s; i++) {
//            lResult.add(tXEM.getObjectFromTreeIdentifier((Integer)listSearch.get(i)));
//        }
//        return lResult;
//    }
//
//    @Test
//    public void testContains() throws StoreIndexException, IOException {
//        final List<Envelope> listRef = new ArrayList<Envelope>();
//        final GeneralEnvelope geTemp = new GeneralEnvelope(DefaultEngineeringCRS.CARTESIAN_3D);
//        geTemp.setEnvelope(115, 135, 35, 125, 145, 45);
//        listRef.add(new GeneralEnvelope(geTemp));
//        geTemp.setEnvelope(116, 136, 36, 124, 144, 44);
//        TreeX.search(tree, geTemp, SpatialFilterType.CONTAINS, defVisit);
//        assertTrue(compareList(listRef, getresult(listSearch)));
//        listSearch.clear();
//        geTemp.setEnvelope(tree.getRoot().getBoundary());
//        TreeX.search(tree, geTemp, SpatialFilterType.CONTAINS, defVisit);
//        assertTrue(listSearch.isEmpty());
//    }
//
//    @Test
//    public void testDisjoint() throws StoreIndexException, IOException {
//        final List<Envelope> listRef = new ArrayList<Envelope>();
//        final GeneralEnvelope geTemp = new GeneralEnvelope(DefaultEngineeringCRS.CARTESIAN_3D);
//        for(int z = 0; z<=100; z+=20) {
//            for(int y = 0; y<=200; y+=20) {
//                for(int x = 0; x<=200; x+=20) {
//                    geTemp.setEnvelope(x-5, y-5, z-5, x+5, y+5, z+5);
//                    listRef.add(new GeneralEnvelope(geTemp));
//                }
//            }
//        }
//        geTemp.setEnvelope(-10, -10, 110, 210, 210, 210);
//        TreeX.search(tree, geTemp, SpatialFilterType.DISJOINT, defVisit);
//        assertTrue(compareList(listRef, getresult(listSearch)));
//        listSearch.clear();
//        geTemp.setEnvelope(tree.getRoot().getBoundary());
//        TreeX.search(tree, geTemp, SpatialFilterType.DISJOINT, defVisit);
//        assertTrue(listSearch.isEmpty());
//    }
//
//    @Test
//    public void testWithin() throws StoreIndexException, IOException {
//        final List<Envelope> listRef = new ArrayList<Envelope>();
//        final GeneralEnvelope geTemp = new GeneralEnvelope(DefaultEngineeringCRS.CARTESIAN_3D);
//        for(int z = 0; z<=200; z+=20) {
//            for(int y = 0; y<=200; y+=20) {
//                    geTemp.setEnvelope(195, y-5, z-5, 205, y+5, z+5);
//                    listRef.add(new GeneralEnvelope(geTemp));
//            }
//        }
//        geTemp.setEnvelope(180, -10, -10, 210, 210, 210);
//        TreeX.search(tree, geTemp, SpatialFilterType.WITHIN, defVisit);
//        assertTrue(compareList(listRef, getresult(listSearch)));
//        listSearch.clear();
//        geTemp.setEnvelope(-10, 97, -10, 210, 104, 210);
//        TreeX.search(tree, geTemp, SpatialFilterType.WITHIN, defVisit);
//        assertTrue(listSearch.isEmpty());
//    }
//
//    @Test
//    public void testTouches() throws StoreIndexException, IOException {
//        final List<Envelope> listRef = new ArrayList<Envelope>();
//        final GeneralEnvelope geTemp = new GeneralEnvelope(DefaultEngineeringCRS.CARTESIAN_3D);
//        for(int z = 0; z<=200; z+=20) {
//            for(int y = 0; y<=200; y+=20) {
//                for(int x = 140; x<=160; x+=20) {
//                    geTemp.setEnvelope(x-5, y-5, z-5, x+5, y+5, z+5);
//                    listRef.add(new GeneralEnvelope(geTemp));
//                }
//            }
//        }
//        geTemp.setEnvelope(145, -10, -10, 155, 210, 210);
//        TreeX.search(tree, geTemp, SpatialFilterType.TOUCHES, defVisit);
//        assertTrue(compareList(listRef, getresult(listSearch)));
//        listSearch.clear();
//        geTemp.setEnvelope(144, -10, -10, 156, 210, 210);
//        TreeX.search(tree, geTemp, SpatialFilterType.TOUCHES, defVisit);
//        assertTrue(listSearch.isEmpty());
//    }
//
//    @Test
//    public void testEquals() throws StoreIndexException, IOException {
//        final List<Envelope> listRef = new ArrayList<Envelope>();
//        final GeneralEnvelope geTemp = new GeneralEnvelope(DefaultEngineeringCRS.CARTESIAN_3D);
//        geTemp.setEnvelope(115, 135, 35, 125, 145, 45);
//        listRef.add(new GeneralEnvelope(geTemp));
//        geTemp.setEnvelope(115, 135, 35, 125, 145, 45);
//        TreeX.search(tree, geTemp, SpatialFilterType.EQUALS, defVisit);
//        assertTrue(compareList(listRef, getresult(listSearch)));
//        listSearch.clear();
//        geTemp.setEnvelope(tree.getRoot().getBoundary());
//        TreeX.search(tree, geTemp, SpatialFilterType.EQUALS, defVisit);
//        assertTrue(listSearch.isEmpty());
//    }
//
//    @Test
//    public void testOverlaps() throws StoreIndexException, IOException {
//        final List<Envelope> listRef = new ArrayList<Envelope>();
//        final GeneralEnvelope geTemp = new GeneralEnvelope(DefaultEngineeringCRS.CARTESIAN_3D);
//        for(int z = 0; z<=200; z+=20){
//            for(int y = 0; y<=200; y+=20){
//                for(int x = 140; x<=160; x+=20){
//                    geTemp.setEnvelope(x-5, y-5, z-5, x+5, y+5, z+5);
//                    listRef.add(new GeneralEnvelope(geTemp));
//                }
//            }
//        }
//        geTemp.setEnvelope(144, -10, -10, 156, 210, 210);
//        TreeX.search(tree, geTemp, SpatialFilterType.OVERLAPS, defVisit);
//        assertTrue(compareList(listRef, getresult(listSearch)));
//        listSearch.clear();
//        geTemp.setEnvelope(145, -10, -10, 155, 210, 210);
//        TreeX.search(tree, geTemp, SpatialFilterType.OVERLAPS, defVisit);
//        assertTrue(listSearch.isEmpty());
//        listSearch.clear();
//        geTemp.setEnvelope(145, -10, -10, 165, 210, 210);
//        TreeX.search(tree, geTemp, SpatialFilterType.OVERLAPS, defVisit);
//        assertTrue(listSearch.isEmpty());
//    }
//
//}
//
//class TreeXElementMapperTest implements TreeElementMapper<Envelope> {
//
//    private final CoordinateReferenceSystem crs;
//    private final List<Envelope> lData;
//    private final List<Integer> lID;
//
//    public TreeXElementMapperTest(CoordinateReferenceSystem crs) {
//        this.crs = crs;
//        this.lData = new ArrayList<Envelope>();
//        this.lID = new ArrayList<Integer>();
//    }
//    
//    @Override
//    public int getTreeIdentifier(Envelope object) {
//        for (int i = 0, s = lData.size(); i < s; i++) {
//            if (lData.get(i).equals(object)) {
//                return lID.get(i);
//            }
//        }
//        throw new IllegalStateException("impossible to found treeIdentifier.");
//    }
//
//    @Override
//    public Envelope getEnvelope(Envelope object) {
//        return new GeneralEnvelope(object);
//    }
//
//    @Override
//    public void setTreeIdentifier(Envelope object, int treeIdentifier) {
//        lData.add(object);
//        lID.add(treeIdentifier);
//    }
//
//    @Override
//    public Envelope getObjectFromTreeIdentifier(int treeIdentifier) {
//        for (int i = 0, l = lID.size(); i < l; i++) {
//            if (lID.get(i) == treeIdentifier) {
//                return lData.get(i);
//            }
//        }
//        throw new IllegalStateException("impossible to found Data.");
//    }
//
//    @Override
//    public void clear() {
//        lData.clear();
//        lID.clear();
//    }
//    
//}