/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
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
package org.geotoolkit.index.tree;

import java.io.IOException;
import org.geotoolkit.index.tree.calculator.Calculator;
import org.geotoolkit.index.tree.io.StoreIndexException;
import org.geotoolkit.index.tree.io.TreeElementMapper;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedReferenceSystemException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Define a generic Tree.
 *
 * @author Rémi Maréchal       (Geomatys).
 * @author Johann Sorel        (Geomatys).
 * @author Martin Desruisseaux (Geomatys).
 */
public interface Tree<E> {

    /**
     * Find some {@code Entry} which intersect regionSearch parameter
     * and add them into result {@code List} parameter.
     *
     * <blockquote><font size=-1>
     * <strong>NOTE: if no result found, the list passed in parameter is unchanged.</strong>
     * </font></blockquote>
     *
     * @param regionSearch Define the region to find Shape within tree.
     * @param result List of Entr(y)(ies).
     * @throws MismatchedReferenceSystemException if entry CRS is different from tree CRS
     */
    int[] searchID(Envelope regionSearch) throws StoreIndexException;
    
//    /**
//     * Find some {@code Entry} which intersect regionSearch parameter
//     * and add them into result {@code List} parameter.
//     *
//     * <blockquote><font size=-1>
//     * <strong>NOTE: if no result found, the list passed in parameter is unchanged.</strong>
//     * </font></blockquote>
//     *
//     * @param regionSearch Define the region to find Shape within tree.
//     * @param result List of Entr(y)(ies).
//     * @throws MismatchedReferenceSystemException if entry CRS is different from tree CRS
//     */
//    Iterable<E> search(Envelope regionSearch) throws StoreIndexException;
    
    /**
     * Insert a {@code Entry} into Rtree.
     *
     * @param Entry to insert into tree.
     * @throws MismatchedReferenceSystemException if entry CRS is different from tree CRS
     */
    void insert(E object) throws StoreIndexException;

    /**
     * Find a {@code Envelope} (entry) from Iterator into the tree and delete it.
     *
     * <blockquote><font size=-1>
     * <strong>NOTE: comparison to remove entry is based from them references.</strong>
     * </font></blockquote>
     *
     * @param Entry to delete.
     * @throws MismatchedReferenceSystemException if entry CRS is different from tree CRS
     */
    boolean remove(E object) throws StoreIndexException;

    TreeElementMapper<E> getTreeElementMapper();
    
    /**
     * @return max number authorized by tree cells.
     */
    int getMaxElements();

    /**
     * @return tree trunk.
     */
    Node getRoot();

    /**
     * Affect a new root {@code Node}.
     *
     * @param root new root.
     */
    void setRoot(Node root) throws StoreIndexException;

    /**
     * @return associate crs.
     */
    CoordinateReferenceSystem getCrs();

    /**
     * @return Calculator to effectuate Tree compute.
     */
    Calculator getCalculator();

    /**
     * @return NodeFactory to create adapted Tree Node.
     */
    NodeFactory getNodeFactory();

    /**
     * Clear tree (tree root Node becomme null).
     */
    void clear() throws StoreIndexException;

    /**
     * Return elements number within tree.
     */
    int getElementsNumber();

    /**
     * <blockquote><font size=-1>
     * <strong>NOTE: return {@code null} if tree root node is null.</strong>
     * </font></blockquote>
     *
     * @return all tree data set boundary.
     */
    double[] getExtent() throws StoreIndexException;
    
    void close() throws StoreIndexException;
    
    /**Create a node in accordance with this properties.
     *
     * @param tree pointer on Tree.
     * @param parent pointer on parent {@code Node}.
     * @param children sub {@code Node}.
     * @param entries entries {@code List} to add in this node.
     * @param coordinates lower upper bounding box coordinates table.
     * @return appropriate Node from tree.
     */
    Node createNode(Tree tree, Node parent, Node[] children, Object[] objects, double[][] coordinates) throws IllegalArgumentException, IOException;
}
