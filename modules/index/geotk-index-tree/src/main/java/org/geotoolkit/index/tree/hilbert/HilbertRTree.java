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
package org.geotoolkit.index.tree.hilbert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geotoolkit.geometry.GeneralDirectPosition;
import org.geotoolkit.geometry.GeneralEnvelope;
import static org.geotoolkit.index.tree.DefaultTreeUtils.countElements;
import static org.geotoolkit.index.tree.DefaultTreeUtils.getEnveloppeMin;
import org.geotoolkit.index.tree.*;
import static org.geotoolkit.index.tree.Node.*;
import org.geotoolkit.index.tree.calculator.Calculator;
import org.geotoolkit.index.tree.io.DefaultTreeVisitor;
import static org.geotoolkit.index.tree.io.TVR.*;
import org.geotoolkit.index.tree.io.TreeVisitor;
import org.geotoolkit.index.tree.io.TreeVisitorResult;
import org.geotoolkit.index.tree.NodeFactory;
import org.geotoolkit.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.UnmodifiableArrayList;
import org.apache.sis.util.Classes;
import static org.geotoolkit.index.tree.DefaultTreeUtils.getMedian;
import org.geotoolkit.index.tree.calculator.CalculatorND;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.geometry.MismatchedReferenceSystemException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Create Hilbert RTree.
 *
 * @author Rémi Maréchal (Geomatys).
 */
public class HilbertRTree extends AbstractTree {

    int hilbertOrder;
    private static final double LN2 = 0.6931471805599453;
    
    /**
     * Create Hilbert R-Tree using default node factory.
     *
     * <blockquote><font size=-1> <strong>
     * NOTE: In HilbertRTree each leaf contains some sub-{@code Node} called cells.
     * {@code Envelope} entries are contains in their cells.
     * Cells number per leaf = 2 ^ (dim*hilbertOrder).
     * Moreother there are maxElements_per_cells 2 ^(dim*hilbertOrder) elements per leaf.
     * </strong> </font></blockquote>
     *
     * @param nbMaxElement          : max elements number within each tree leaf cells.
     * @param hilbertOrder          : max order value.
     * @param crs                   : associate coordinate system.
     * @return Hilbert R-Tree.
     * @throws IllegalArgumentException if maxElements <= 0.
     * @throws IllegalArgumentException if hilbertOrder <= 0.
     */
    public HilbertRTree(int nbMaxElement, int hilbertOrder, CoordinateReferenceSystem crs) {
        this(nbMaxElement, hilbertOrder, crs, DefaultNodeFactory.INSTANCE);
    }
    
    /**
     * Create Hilbert R-Tree.
     *
     * <blockquote><font size=-1> <strong>
     * NOTE: In HilbertRTree each leaf contains some sub-{@code Node} called cells.
     * {@code Envelope} entries are contains in their cells.
     * Cells number per leaf = 2 ^ (dim*hilbertOrder).
     * Moreother there are maxElements_per_cells 2 ^(dim*hilbertOrder) elements per leaf.
     * </strong> </font></blockquote>
     *
     * @param nbMaxElement          : max elements number within each tree leaf cells.
     * @param hilbertOrder          : max order value.
     * @param crs                   : associate coordinate system.
     * @param nodefactory           : made to create tree {@code Node}.
     * @return Hilbert R-Tree.
     * @throws IllegalArgumentException if maxElements <= 0.
     * @throws IllegalArgumentException if hilbertOrder <= 0.
     */
    public HilbertRTree(int nbMaxElement, int hilbertOrder, CoordinateReferenceSystem crs, NodeFactory nodefactory) {
        super(nbMaxElement, crs, nodefactory);
        ArgumentChecks.ensureStrictlyPositive("impossible to create Hilbert Rtree with order <= 0", hilbertOrder);
        this.hilbertOrder = hilbertOrder;
    }

    /**
     * @return Max Hilbert order value.
     */
    public int getHilbertOrder() {
        return hilbertOrder;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        return Classes.getShortClassName(this) + "\n" + getRoot();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void search(final Envelope regionSearch, final TreeVisitor visitor) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("search : region search", regionSearch);
        ArgumentChecks.ensureNonNull("search : visitor", visitor);
        if (!CRS.equalsIgnoreMetadata(crs, regionSearch.getCoordinateReferenceSystem())) throw new MismatchedReferenceSystemException();
        final Node root = getRoot();
        if (!root.isEmpty() && root != null) searchHilbertNode(root, regionSearch, visitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(final Envelope entry) throws IllegalArgumentException{
        super.insert(entry);
        if(!CRS.equalsIgnoreMetadata(crs, entry.getCoordinateReferenceSystem())) throw new MismatchedReferenceSystemException();
        super.eltCompteur++;
        final Node root = getRoot();
        final int dim = entry.getDimension();
        final double[] coords = new double[2 * dim];
        System.arraycopy(entry.getLowerCorner().getCoordinate(), 0, coords, 0, dim);
        System.arraycopy(entry.getUpperCorner().getCoordinate(), 0, coords, dim, dim);
        if (root == null || root.isEmpty()) {
            setRoot(createNode(this, null, null, UnmodifiableArrayList.wrap(entry), coords));
        } else {
            insertNode(root, entry);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(final Envelope entry) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("delete : entry", entry);
        if (!CRS.equalsIgnoreMetadata(crs, entry.getCoordinateReferenceSystem())) throw new MismatchedReferenceSystemException();
        return deleteHilbertNode(getRoot(), entry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final Envelope entry) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("remove : entry", entry);
        if (!CRS.equalsIgnoreMetadata(crs, entry.getCoordinateReferenceSystem())) throw new MismatchedReferenceSystemException();
        return removeHilbertNode(getRoot(), entry);
    }

    /**
     * Find all {@code Envelope} (entries) which intersect regionSearch
     * parameter.
     *
     * @param regionSearch area of search.
     * @param result {@code List} where is add search resulting.
     */
    public static TreeVisitorResult searchHilbertNode(final Node candidate, final Envelope regionSearch, final TreeVisitor visitor) {
        final TreeVisitorResult tvr = visitor.filter(candidate);
        if (isTerminate(tvr)) return tvr;
        final Envelope bound = candidate.getBoundary();
        if (bound != null) {
            if (regionSearch == null) {
                if (candidate.isLeaf()) {
                    final List<Node> lN = candidate.getChildren();
                    for (Node n2d : lN.toArray(new Node[lN.size()])) {
                        if (!n2d.isEmpty()) {
                            for (Envelope env : n2d.getEntries().toArray(new Envelope[n2d.getEntries().size()])) {
                                final TreeVisitorResult tvrTemp = visitor.visit(env);
                                if (isTerminate(tvrTemp))   return tvrTemp;
                                if (isSkipSibling(tvrTemp)) break;
                            }
                        }
                    }
                } else {
                    if (!isSkipSubTree(tvr)) {
                        for (Node nod : candidate.getChildren()) {
                            final TreeVisitorResult tvrTemp = searchHilbertNode(nod, null, visitor);
                            if (isTerminate(tvrTemp))   return tvrTemp;
                            if (isSkipSibling(tvrTemp)) break;
                        }
                    }
                }
            }else{
                final GeneralEnvelope rS = new GeneralEnvelope(regionSearch);
                if (rS.contains(bound, true)) {
                    searchHilbertNode(candidate, null, visitor);
                } else if(rS.intersects(bound, true)) {
                    if (candidate.isLeaf()) {
                        final List<Node> lN = candidate.getChildren();
                        for (Node n2d : lN.toArray(new Node[lN.size()])) {
                            TreeVisitorResult tvrTemp = null;
                            if (!n2d.isEmpty()) {
                                if (rS.intersects(n2d.getBoundary(), true)) {
                                    for (Envelope sh : n2d.getEntries().toArray(new Envelope[n2d.getEntries().size()])) {
                                        if (rS.intersects(sh, true)) {
                                            tvrTemp = visitor.visit(sh);
                                        }
                                        if (isTerminate(tvrTemp) && tvrTemp != null)   return tvrTemp;
                                        if (isSkipSibling(tvrTemp) && tvrTemp != null) break;
                                    }
                                }
                            }
                            if (isSkipSibling(tvrTemp)) break;
                        }
                    }else{
                        if (!isSkipSubTree(tvr)) {
                            for (Node child : candidate.getChildren()) {
                                final TreeVisitorResult tvrTemp = searchHilbertNode(child, regionSearch, visitor);
                                if (isTerminate(tvrTemp))   return tvrTemp;
                                if (isSkipSibling(tvrTemp)) break;
                            }
                        }
                    }
                }
            }
            return tvr;
        }
        return TreeVisitorResult.TERMINATE;
    }

    /**
     * Insert entry in {@code Node} in accordance with R-Tree properties.
     *
     * @param candidate {@code Node} where user want insert data.
     * @param entry to insert.
     * @throws IllegalArgumentException if candidate or entry are null.
     */
    public static void insertNode(final Node candidate, final Envelope entry) throws IllegalArgumentException{
        ArgumentChecks.ensureNonNull("impossible to insert a null entry", entry);
        if (candidate.isFull()) {
            List<Node> lSp = splitNode(candidate);
            //List is null if internal Node hilbert order should be increase by 1. 
            if (lSp != null) {
                
                final Node lsp0      = lSp.get(0);
                final Node lsp1      = lSp.get(1);
                Node parentCandidate = candidate.getParent();
                
                if (parentCandidate != null) {
                    final List<Node> parentChildren = parentCandidate.getChildren();
                    parentChildren.remove(candidate);
                    lsp0.setParent(parentCandidate);
                    lsp1.setParent(parentCandidate);
                    parentChildren.add(lSp.get(0));
                    parentChildren.add(lSp.get(1));
                    insertNode(parentCandidate, entry);
                } else {
                    candidate.getChildren().clear();
                    candidate.setUserProperty(PROP_ISLEAF, false);
                    candidate.setUserProperty(PROP_HILBERT_ORDER, 0);
                    lsp0.setParent(candidate);
                    lsp1.setParent(candidate);
                    candidate.getChildren().add(lSp.get(0));
                    candidate.getChildren().add(lSp.get(1));
                    insertNode(candidate, entry);
                }
            }else {
                insertNode(candidate, entry);
            }
        } else {
            if (candidate.isLeaf()) {
                final GeneralEnvelope cB = new GeneralEnvelope(candidate.getBoundary());
                if ((!cB.contains(entry, true))) {
                    final List<Envelope> lS = new ArrayList<Envelope>();
                    searchHilbertNode(candidate, cB, new DefaultTreeVisitor(lS));
                    lS.add(entry);
                    Envelope envelope = getEnveloppeMin(lS);
                    createBasicHL(candidate, (Integer) candidate.getUserProperty(PROP_HILBERT_ORDER), envelope);
                    for (Envelope sh : lS) {
                        candidate.setBound(envelope);//to avoid recomputing of candidate boundary.
                        chooseSubtree(candidate, entry).getEntries().add(sh);
                    }
                } else {
                    chooseSubtree(candidate, entry).getEntries().add(entry);
                }
            } else {
                insertNode(chooseSubtree(candidate, entry), entry);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if this {@code Node} contains lesser two
     * subnode.
     * @throws IllegalArgumentException if this {@code Node} doesn't contains {@code Entry}.
     */
    public static List<Node> splitNode(final Node candidate) throws IllegalArgumentException{
        boolean cleaf = candidate.isLeaf();
        int cHO = (Integer) candidate.getUserProperty(PROP_HILBERT_ORDER);
        if (candidate.getChildren().size() < 2 && !cleaf) throw new IllegalStateException("impossible to split node with lesser two subnode");
        if (cleaf && (cHO < ((HilbertRTree) candidate.getTree()).getHilbertOrder())) {
            final List<Envelope> lS = new ArrayList<Envelope>();
            searchHilbertNode(candidate, candidate.getBoundary(), new DefaultTreeVisitor(lS));
            if (lS.isEmpty()) throw new IllegalStateException("impossible to increase Hilbert order of a empty Node");
            final Envelope boundT = getEnveloppeMin(lS);
            createBasicHL(candidate, cHO + 1, boundT);
            for (Envelope sh : lS) {
                candidate.setBound(boundT);//to avoid candidate boundary re-computing.
                chooseSubtree(candidate, sh).getEntries().add(sh);
            }
            return null;
        } else {
            final List<Node> lS = hilbertNodeSplit(candidate);
            return lS;
        }
    }

    /**
     * Compute and define which axis to split {@code Node} candidate.
     *
     * <blockquote><font size=-1> <strong>NOTE: Define split axis method decides
     * a split axis among all dimensions. The choosen axis is the one with
     * smallest overall perimeter or area (in fonction with dimension size). It
     * work by sorting all entry or {@code Node}, from their left boundary
     * coordinates. Then it considers every divisions of the sorted list that
     * ensure each node is at least 40% full. The algorithm compute perimeters
     * or area of two result {@code Node} from every division. A second pass
     * repeat this process with respect their right boundary coordinates.
     * Finally the overall perimeter or area on one axis is the som of all
     * perimeter or area obtained from the two pass.</strong>
     * </font></blockquote>
     *
     * @throws IllegalArgumentException if candidate is null.
     * @return prefered ordinate index to split.
     */
    private static int defineSplitAxis(final Node candidate) {
        ArgumentChecks.ensureNonNull("defineSplitAxis : ", candidate);
        final boolean isLeaf = candidate.isLeaf();
        List eltList;
        if (isLeaf) {
            final List<Node> ldf =  candidate.getChildren();
            eltList = new ArrayList();
            for (Node dn : ldf) {
                eltList.addAll(dn.getEntries());
            }
        } else {
            eltList = candidate.getChildren();
        }
        final AbstractTree tree = (AbstractTree)candidate.getTree();
        final Calculator calc   = tree.getCalculator();
        final int size          = eltList.size();
        final double size04     = size * 0.4;
        final int demiSize      = (int) ((size04 >= 1) ? size04 : 1);
        final List splitListA   = new ArrayList();
        final List splitListB   = new ArrayList();
        double bulkRef          = Double.POSITIVE_INFINITY;
        int index               = 0;
        GeneralEnvelope gESPLA, gESPLB;
        double bulkTemp; 
        
        final GeneralEnvelope globalEltsArea = getEnveloppeMin(eltList);
        final int dim           = globalEltsArea.getDimension();
        
        // if glogaleArea.span(currentDim) == 0 || if all elements have same span
        // value as global area on current ordinate, impossible to split on this axis.
        unappropriateOrdinate : 
        for (int indOrg = 0; indOrg < dim; indOrg++) {
            final double globalSpan = globalEltsArea.getSpan(indOrg);
            boolean isSameSpan = true;
            //check if its possible to split on this currently ordinate.
            for (Object elt : eltList) {
                final Envelope envElt = (isLeaf) ? (Envelope) elt : ((Node)elt).getBoundary();
                if (!(Math.abs(envElt.getSpan(indOrg) - globalSpan) <= 1E-9)) {
                    isSameSpan = false;
                    break;
                }
            }
            if (globalSpan <= 1E-9 || isSameSpan) continue unappropriateOrdinate; 
            bulkTemp = 0;
            
            for (int left_or_right = 0; left_or_right < 2; left_or_right++) {
                eltList = (left_or_right == 0) ? calc.sortList(indOrg, true, eltList):calc.sortList(indOrg, false, eltList);
                for (int cut = demiSize, sdem = size - demiSize; cut <= sdem; cut++) {
                    splitListA.clear();
                    splitListB.clear();
                    for (int i = 0; i < cut; i++) {
                        splitListA.add(eltList.get(i));
                    }
                    for (int j = cut; j < size; j++) {
                        splitListB.add(eltList.get(j));
                    }
                    gESPLA = getEnveloppeMin(splitListA);
                    gESPLB = getEnveloppeMin(splitListB);
                    bulkTemp += calc.getEdge(gESPLA);
                    bulkTemp += calc.getEdge(gESPLB);
                }
            }
            if (bulkTemp < bulkRef) {
                bulkRef = bulkTemp;
                index = indOrg;
            }
        }
        return index;
    }

    /**
     * Compute and define how to split {@code Node} candidate.
     *
     * <blockquote><font size=-1> <strong>NOTE: To choose which {@code Node}
     * couple, split algorithm sorts the entries (for tree leaf), or {@code Node}
     * (for tree branch) in accordance to their lower or upper boundaries on the
     * selected dimension (see defineSplitAxis method) and examines all possible
     * divisions. Two {@code Node} resulting, is the final division which has
     * the minimum overlaps between them.</strong> </font></blockquote>
     *
     * @throws IllegalArgumentException if candidate is null.
     * @return Two appropriate {@code Node} in List in accordance with
     * R*Tree split properties.
     */
    private static List<Node> hilbertNodeSplit(final Node candidate) throws IllegalArgumentException{

        final int splitIndex  = defineSplitAxis(candidate);
        final boolean isLeaf  = candidate.isLeaf();
        final Tree tree       = candidate.getTree();
        final Calculator calc = tree.getCalculator();
        List eltList;
        if (isLeaf) {
            final List<Node> ldf = candidate.getChildren();
            eltList = new ArrayList();
            for (Node dn : ldf) {
                eltList.addAll(dn.getEntries());
            }
        } else {
            eltList = candidate.getChildren();
        }
        final int size        = eltList.size();
        //to find best split combinaison follow list elements from 1/3 th elts to 2/3th elts.
        final double size033  = size * 0.333;
        final int tierSize    = (int) ((size033 >= 1) ? size033 : 1);
        double bulkRef        = Double.POSITIVE_INFINITY;
        final List splitListA = new ArrayList();
        final List splitListB = new ArrayList();
        int index             = 0;
        int lower_or_upper    = 0;
        final List<CoupleGE> listCGE = new ArrayList<CoupleGE>();
        GeneralEnvelope gESPLA, gESPLB;
        CoupleGE coupleGE;
        double bulkTemp;
        
        for (int lu = 0; lu < 2; lu++) {
            eltList = (lu == 0) ? calc.sortList(splitIndex, true, eltList):calc.sortList(splitIndex, false, eltList);
            for (int cut = tierSize; cut <= size - tierSize; cut++) {
                for (int i = 0; i < cut; i++) {
                    splitListA.add(eltList.get(i));
                }
                for (int j = cut; j < size; j++) {
                    splitListB.add(eltList.get(j));
                }
                gESPLA = getEnveloppeMin(splitListA);
                gESPLB = getEnveloppeMin(splitListB);
                coupleGE = new CoupleGE(gESPLA, gESPLB, calc);
                bulkTemp = coupleGE.getOverlaps();
                
                if (Double.isNaN(bulkTemp) || bulkTemp == 0) {
                    coupleGE.setUserProperty("cut", cut);
                    coupleGE.setUserProperty("lower_or_upper", lu);
                    listCGE.add(coupleGE);
                } else if (bulkTemp < bulkRef && listCGE.isEmpty()) {
                    bulkRef = bulkTemp;
                    index = cut;
                    lower_or_upper = lu;
                }
                splitListA.clear();
                splitListB.clear();
            }
        }

        if (!listCGE.isEmpty()) {
            double areaRef = Double.POSITIVE_INFINITY;
            double areaTemp;
            for (CoupleGE cge : listCGE) {
                areaTemp = cge.getEdge();
                if (areaTemp < areaRef) {
                    areaRef = areaTemp;
                    index = (Integer) cge.getUserProperty("cut");
                    lower_or_upper = (Integer) cge.getUserProperty("lower_or_upper");
                }
            }
        }
        eltList = (lower_or_upper == 0) ? calc.sortList(splitIndex, true, eltList):calc.sortList(splitIndex, false, eltList);
        for (int i = 0; i < index; i++) {
            splitListA.add(eltList.get(i));
        }
        for (int i = index; i < size; i++) {
            splitListB.add(eltList.get(i));
        }
        
        //paranoiac assertion.
        assert (!splitListA.isEmpty()) :"split list A should not be empty";
        assert (!splitListB.isEmpty()) :"split list B should not be empty";
        
        if (isLeaf) return UnmodifiableArrayList.wrap(tree.createNode(tree, null, null, splitListA), tree.createNode(tree, null, null, splitListB));
        final Node resultA = (Node) ((splitListA.size() == 1) ? splitListA.get(0) : tree.createNode(tree, null, splitListA, null));
        final Node resultB = (Node) ((splitListB.size() == 1) ? splitListB.get(0) : tree.createNode(tree, null, splitListB, null));
        return UnmodifiableArrayList.wrap(resultA, resultB);
    }

    /**
     * Find appropriate subnode to insert new entry. Appropriate subnode is
     * chosen to answer HilbertRtree criterion.
     *
     * @param entry to insert.
     * @throws IllegalArgumentException if this subnodes list is empty.
     * @throws IllegalArgumentException if entry is null.
     * @return subnode chosen.
     */
    public static Node chooseSubtree(final Node candidate, final Envelope entry) {
        ArgumentChecks.ensureNonNull("impossible to choose subtree with entry null", entry);
        if (candidate.isLeaf() && candidate.isFull()) throw new IllegalStateException("impossible to choose subtree in overflow node");
        Calculator calc = candidate.getTree().getCalculator();
        if (candidate.isLeaf()) {
            if ((Integer) candidate.getUserProperty(PROP_HILBERT_ORDER) < 1) return candidate.getChildren().get(0);
            int index;
            index = getHVOfEntry(candidate, entry);
            for (Node nod : candidate.getChildren()) {
                if (index <= ((Integer) (nod.getUserProperty(PROP_HILBERT_VALUE))) && !nod.isFull()) return nod;
            }
            return candidate.getChildren().get(findAnotherCell(index, candidate));
        } else {
            final List<Node> childrenList = candidate.getChildren();
            final int size = childrenList.size();
            if (childrenList.get(0).isLeaf()) {
                final List<Node> listOverZero = new ArrayList<Node>();
                double overlapsRef = Double.POSITIVE_INFINITY;
                int index = -1;
                double overlapsTemp = 0;
                for (int i = 0; i < size; i++) {
                    final GeneralEnvelope gnTemp = new GeneralEnvelope(childrenList.get(i).getBoundary());
                    gnTemp.add(entry);
                    for (int j = 0; j < size; j++) {
                        if (i != j) {
                            final Envelope gET = childrenList.get(j).getBoundary();
                            overlapsTemp += calc.getOverlaps(gnTemp, gET);
                        }
                    }
                    if (overlapsTemp == 0) {
                        listOverZero.add(childrenList.get(i));
                    } else {
                        if ((overlapsTemp < overlapsRef)) {
                            overlapsRef = overlapsTemp;
                            index = i;
                        } else if (overlapsTemp == overlapsRef) {
                            if (countElements(childrenList.get(i)) < countElements(childrenList.get(index))) {
                                overlapsRef = overlapsTemp;
                                index = i;
                            }
                        }
                    }
                    overlapsTemp = 0;
                }
                if (!listOverZero.isEmpty()) {
                    double areaRef = Double.POSITIVE_INFINITY;
                    int indexZero  = -1;
                    double areaTemp;
                    for (int i = 0, s = listOverZero.size(); i < s; i++) {
                        final GeneralEnvelope gE = new GeneralEnvelope(listOverZero.get(i).getBoundary());
                        gE.add(entry);
                        areaTemp = calc.getEdge(gE);
                        if (areaTemp < areaRef) {
                            areaRef = areaTemp;
                            indexZero = i;
                        }
                    }
                    return listOverZero.get(indexZero);
                }
                if (index == -1) throw new IllegalStateException("chooseSubTree : no subLeaf find");
                return childrenList.get(index);
            }

            for (Node no : childrenList) {
                final GeneralEnvelope ge = new GeneralEnvelope(no.getBoundary());
                if (ge.contains(entry, true)) return no;
            }

            double enlargRef = Double.POSITIVE_INFINITY;
            int indexEnlarg  = -1;
            for (int i = 0, s = childrenList.size(); i < s; i++) {
                final Node n3d = childrenList.get(i);
                final Envelope gEN = n3d.getBoundary();
                final GeneralEnvelope GE = new GeneralEnvelope(gEN);
                GE.add(entry);
                double enlargTemp = calc.getEnlargement(gEN, GE);
                if (enlargTemp < enlargRef || enlargRef == -1) {
                    enlargRef = enlargTemp;
                    indexEnlarg = i;
                }
            }
            return childrenList.get(indexEnlarg);
        }
    }

    /**
     * To answer Hilbert criterion and to avoid call split method, in some case
     * we constrain tree leaf to choose another cell to insert Entry.
     *
     * @param index of subnode which is normally chosen.
     * @param ptEntryCentroid subnode chosen centroid.
     * @throws IllegalArgumentException if method call by none leaf {@code Node}.
     * @throws IllegalArgumentException if index is out of required limit.
     * @throws IllegalStateException if no another cell is find.
     * @return index of another subnode.
     */
    private static int findAnotherCell(int index, final Node candidate) {
        if (!candidate.isLeaf()) throw new IllegalArgumentException("impossible to find another leaf in Node which isn't LEAF tree");
        final List<Node> listCells = candidate.getChildren();
        final int siz   = listCells.size();
        boolean oneTime = false;
        int indexTemp1  = index;
        for (int i = index; i < siz; i++) {
            if (!listCells.get(i).isFull()) {
                indexTemp1 = i;
                break;
            }
            if (i == siz - 1) {
                if (oneTime) throw new IllegalStateException("will be able to split");
                oneTime = true;
                i = -1;
            }
        }
        return indexTemp1;
    }

    /**
     * Travel down {@code Tree}, find {@code Envelope} entry if it exist
     * and delete it.
     *
     * <blockquote><font size=-1> <strong>NOTE: Moreover {@code Tree} is
     * condensate after a deletion to stay conform about R-Tree
     * properties.</strong> </font></blockquote>
     *
     * @param candidate {@code Node} where to delete.
     * @param entry {@code Envelope} to delete.
     * @throws IllegalArgumentException if candidate or entry is null.
     * @return true if entry is find and deleted else false.
     */
    private static boolean deleteHilbertNode(final Node candidate, final Envelope entry) throws IllegalArgumentException{
        ArgumentChecks.ensureNonNull("deleteHilbertNode Node candidate : ", candidate);
        ArgumentChecks.ensureNonNull("deleteHilbertNode Envelope entry : ", entry);
        if (new GeneralEnvelope(candidate.getBoundary()).intersects(entry, true)) {
            if (candidate.isLeaf()) {
                boolean removed = false;
                final List<Node> lN = candidate.getChildren();
                for (Node nod : lN) {
                    if (nod.getEntries().remove(entry)) {
                        removed = true;
                        break;
                    }
                }
                if (removed) {
                    final AbstractTree tree = ((AbstractTree)candidate.getTree());
                    tree.setElementsNumber(tree.getElementsNumber()-1);
                    candidate.setBound(null);
                    trim(candidate);
                    return true;
                }
            } else {
                for (Node nod : candidate.getChildren().toArray(new Node[candidate.getChildren().size()])) {
                    final  boolean removed = deleteHilbertNode(nod, entry);
                    if (removed) return true;
                }
            }
        }
        return false;
    }

    /**
     * Travel down {@code Tree}, find {@code Envelope} entry if it exist
     * and delete it.
     *
     * <blockquote><font size=-1> <strong>NOTE: Moreover {@code Tree} is
     * condensate after a deletion to stay conform about R-Tree
     * properties.</strong> </font></blockquote>
     *
     * @param candidate {@code Node} where to delete.
     * @param entry {@code Envelope} to delete.
     * @throws IllegalArgumentException if candidate or entry is null.
     * @return true if entry is find and deleted else false.
     */
    private static boolean removeHilbertNode(final Node candidate, final Envelope entry) throws IllegalArgumentException{
        ArgumentChecks.ensureNonNull("deleteHilbertNode Node candidate : ", candidate);
        ArgumentChecks.ensureNonNull("deleteHilbertNode Envelope entry : ", entry);
        if (new GeneralEnvelope(candidate.getBoundary()).intersects(entry, true)) {
            if (candidate.isLeaf()) {
                boolean removed = false;
                final List<Node> lN = candidate.getChildren();
                List<Envelope> l;
                for (Node nod : lN) {
                    l = nod.getEntries();
                    for(int i = l.size()-1;i>=0;i--){
                        if(l.get(i).equals(entry)){
                            removed = true;
                            l.remove(i);
                            break;
                        }
                    }
                    if (removed) break;
                }
                if (removed) {
                    final AbstractTree tree = ((AbstractTree)candidate.getTree());
                    tree.setElementsNumber(tree.getElementsNumber()-1);
                    candidate.setBound(null);
                    trim(candidate);
                    return true;
                }
            } else {
                for (Node nod : candidate.getChildren().toArray(new Node[candidate.getChildren().size()])) {
                    final boolean removed = removeHilbertNode(nod, entry);
                    if (removed) return true;
                }
            }
        }
        return false;
    }

    /**
     * Method which permit to condense R-Tree. Condense made begin by leaf and
     * travel up to tree trunk.
     *
     * @param candidate {@code Node} to begin condense.
     */
    public static void trim(final Node candidate) throws IllegalArgumentException{
        if (!candidate.isLeaf()) {
            final List<Node> children = candidate.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                final Node child = children.get(i);
                if (child.isEmpty()) {
                    children.remove(i);
                } else if (child.getChildren().size() == 1 && !child.isLeaf()) {
                    children.remove(i);
                    for (Node dn : child.getChildren()) {
                        dn.setParent(candidate);
                    }
                    children.addAll(child.getChildren());
                }
            }

            final HilbertRTree tree = (HilbertRTree) candidate.getTree();
            final List<Envelope> lS = new ArrayList<Envelope>();
            searchHilbertNode(candidate, candidate.getBoundary(), new DefaultTreeVisitor(lS));
            
            if (lS.size() <= tree.getMaxElements() * Math.pow(2, tree.getHilbertOrder() * 2) && !lS.isEmpty()) {
                final Envelope bound = getEnveloppeMin(lS);
                createBasicHL(candidate, tree.getHilbertOrder(), bound);
                candidate.setUserProperty(PROP_ISLEAF, true);
                for (Envelope entry : lS) {
                    candidate.setBound(bound);
                    chooseSubtree(candidate, entry).getEntries().add(entry);
                }
            }
        }
        if (candidate.getParent() != null) trim(candidate.getParent());
    }

    /**
     * Create appropriate sub-node to HilbertRTree leaf.
     *
     * @param tree pointer on Tree.
     * @param parent pointer on parent {@code Node}.
     * @param children sub {@code Node}.
     * @param entries entries {@code List} to add in this node.
     * @param coordinates lower upper bounding box coordinates table.
     * @return appropriate Node from tree to Hilbert RTree leaf.
     */
    public static Node createCell(final Tree tree, final Node parent, final DirectPosition centroid, final int hilbertValue, final List<Envelope> entries) {
        final Node cell = tree.getNodeFactory().createNode(tree, parent, centroid, centroid, null, entries);
        cell.setUserProperty(PROP_HILBERT_VALUE, hilbertValue);
        return cell;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public Node createNode(Tree tree, Node parent, List<Node> listChildren, List<Envelope> listEntries, double... coordinates) throws IllegalArgumentException{
        if(!(tree instanceof HilbertRTree)){
            throw new IllegalArgumentException("argument tree : "+tree.getClass().getName()+" not adapted to create an Hilbert RTree Node");
        }
        final int ddim = coordinates.length;
        assert (ddim % 2) == 0 : "coordinate dimension is not correct";
        Node result;
        if (ddim == 0) {
            result = nodefactory.createNode(tree, parent, null, null, listChildren, null);
        }else{
            final int dim = coordinates.length / 2;
            final double[] dp1Coords = new double[dim];
            final double[] dp2Coords = new double[dim];
            System.arraycopy(coordinates, 0, dp1Coords, 0, dim);
            System.arraycopy(coordinates, dim, dp2Coords, 0, dim);

            final DirectPosition dp1 = new GeneralDirectPosition(tree.getCrs());
            final DirectPosition dp2 = new GeneralDirectPosition(tree.getCrs());
            for (int i = 0; i < dim; i++) {
                dp1.setOrdinate(i, dp1Coords[i]);
                dp2.setOrdinate(i, dp2Coords[i]);
            }
            result = nodefactory.createNode(tree, parent, dp1, dp2, listChildren, null);
        }
        result.setUserProperty(PROP_ISLEAF, false);
        result.setUserProperty(PROP_HILBERT_ORDER, 0);
        if (listEntries != null && !listEntries.isEmpty()) {
            final GeneralEnvelope bound = getEnveloppeMin(listEntries);
            final int dim = bound.getDimension();
            int diment = dim;
            for (int d = 0; d < dim; d++) if (bound.getSpan(d) <= 1E-12) diment--;
            final int size = listEntries.size();
            final int maxElts = tree.getMaxElements();
            final int hOrder = (size <= maxElts) ? 0 : (int)((Math.log(size-1)-Math.log(maxElts))/(diment*LN2)) + 1;
            //case where splitting method lost a dimension and overmuch elements for n-1 dimension.
            if (hOrder > ((HilbertRTree)tree).getHilbertOrder()) {
                createBasicHL(result, 0, bound);
                for (Envelope ent : listEntries) insertNode(result, ent);
            } else {
                result.setUserProperty(PROP_ISLEAF, true);
                createBasicHL(result, hOrder, bound);
                for (Envelope ent : listEntries) {
                    result.setBound(bound);
                    chooseSubtree(result, ent).getEntries().add(ent);
                }
            }
        }
        return result;
    }
    
    /**
     * Method exclusively used by {@code HilbertRTree}.
     *
     * Create subnode(s) centroid(s). These centroids define Hilbert curve.
     * Increase the Hilbert order of {@code Node} passed in parameter by
     * one unity.
     *
     * @param candidate HilbertLeaf to increase Hilbert order.
     * @param
     * @throws IllegalArgumentException if param "candidate" is null.
     * @throws IllegalArgumentException if param hl Hilbert order is larger than
     * them Hilbert RTree order.
     */
    private static void createBasicHL(final Node candidate, final int order, final Envelope bound) throws MismatchedDimensionException {
        ArgumentChecks.ensurePositive("impossible to create Hilbert Curve with negative indice", order);
        assert order <= ((HilbertRTree)candidate.getTree()).getHilbertOrder() : 
                "impossible to build HilbertLeaf with Hilbert order higher than tree Hilbert order.";
        candidate.getChildren().clear();
        candidate.setUserProperty(PROP_ISLEAF, true);
        candidate.setUserProperty(PROP_HILBERT_ORDER, order);
        candidate.setBound(bound);
        final List<Node> listN = candidate.getChildren();
        listN.clear();
        if (order > 0) {
            int dim = bound.getDimension();
            int dim2 = dim;
            for (int d = 0; d < dim2; d++) if (bound.getSpan(d) <= 1E-9) dim--;
            final int nbCells = 2 << (dim * order - 1);
            int[] tabHV = new int[nbCells];
            for (int i = 0; i < nbCells; i++) {
                tabHV[i] = i;
                listN.add(HilbertRTree.createCell(candidate.getTree(), candidate, null, i, null));
            }
            candidate.setUserProperty(PROP_HILBERT_TABLE, tabHV);
        } else {
            listN.add(HilbertRTree.createCell(candidate.getTree(), candidate, null, 0, null));
        }
        candidate.setBound(bound);
    }
    
    /**
     * Find Hilbert order of an entry from candidate.
     *
     * @param candidate entry 's hilbert value from it.
     * @param entry which we looking for its Hilbert order.
     * @throws IllegalArgumentException if parameter "entry" is out of this node
     * boundary.
     * @throws IllegalArgumentException if entry is null.
     * @return integer the entry Hilbert order.
     */
    private static int getHVOfEntry(Node candidate, Envelope entry) {
        ArgumentChecks.ensureNonNull("impossible to define Hilbert coordinate with null entry", entry);
        final DirectPosition ptCE = getMedian(entry);
        final GeneralEnvelope bound = new GeneralEnvelope(candidate.getBoundary());
        final int order = (Integer) candidate.getUserProperty(PROP_HILBERT_ORDER);
        if (! bound.contains(ptCE)) throw new IllegalArgumentException("entry is out of this node boundary");
        
        int[] hCoord = getHilbCoord(candidate, ptCE, bound, order);
        final int spaceHDim = hCoord.length;
        
        if (spaceHDim == 1) return hCoord[0];
                
        final HilbertIterator hIt = new HilbertIterator(order, spaceHDim);
        int hilberValue = 0;
        while (hIt.hasNext()) {
            final int[] currentCoords = hIt.next();
            if (Arrays.equals(hCoord, currentCoords)) return hilberValue;
            hilberValue++;
        }
        throw new IllegalArgumentException("should never throw");
    }
    
    /**
     * Find {@code DirectPosition} Hilbert coordinate from this Node.
     *
     * @param pt {@code DirectPosition}
     * @throws IllegalArgumentException if parameter "dPt" is out of this node
     * boundary.
     * @throws IllegalArgumentException if parameter dPt is null.
     * @return int[] table of length 3 which contains 3 coordinates.
     */
    private static int[] getHilbCoord(final Node candidate, final DirectPosition dPt, final Envelope envelop, final int hilbertOrder) {
        ArgumentChecks.ensureNonNull("DirectPosition dPt : ", dPt);
        if (!new GeneralEnvelope(envelop).contains(dPt)) {
            throw new IllegalArgumentException("Point is out of this node boundary");
        }
        final Calculator calc = candidate.getTree().getCalculator();
        assert calc instanceof CalculatorND : "getHilbertCoord : calculatorND type required";
        final double div  = 2 << hilbertOrder - 1;
        
        List<Integer> lInt = new ArrayList<Integer>();
        
        for(int d = 0; d < envelop.getDimension(); d++){
            final double span = envelop.getSpan(d);
            if (span <= 1E-9) continue;
            final double currentDiv = span/div;
            int val = (int) (Math.abs(dPt.getOrdinate(d) - envelop.getMinimum(d)) / currentDiv);
            if (val == div) val--;
            lInt.add(val);
        }
        final int[] result = new int[lInt.size()];
        int i = 0;
        for (Integer val : lInt) result[i++] = val;
        return result;
    }
}
