/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geotoolkit.process.coverage.isoline2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.Collections;
import java.util.LinkedList;

/**
 *
 * @author jsorel
 */
public final class Construction {
    
    private static final GeometryFactory GF = new GeometryFactory();
    
    private LinkedList<Coordinate> lst = new LinkedList<Coordinate>();
    private final Edge edge1;
    private final Edge edge2;
    private final double level;
    private boolean locked = false;

    public Construction(double level) {
        edge1 = new Edge(true);
        edge2 = new Edge(false);
        this.level = level;
    }

    public Edge getEdge1() {
        return edge1;
    }

    public Edge getEdge2() {
        return edge2;
    }
    
    public double getLevel() {
        return level;
    }
    
    public Geometry toGeometry(){
        if(locked) throw new IllegalStateException("Construction has been merged, should not be used anymore.");
        final Coordinate[] coords = lst.toArray(new Coordinate[lst.size()]);
        return GF.createLineString(coords);
    }

    public void merge(Construction other){
        if(this.equals(other)){
            //closing a construction
            return;
        }
        
        if(this.lst.getFirst().equals2D(other.lst.getLast())){
            //add at the beginning of this segment
            this.lst.removeFirst();
            this.lst.addAll(0, other.lst);
        }else if(this.lst.getLast().equals2D(other.lst.getFirst())){
            //add at the end of this segment
            this.lst.removeLast();
            this.lst.addAll(other.lst);
        }else if(this.lst.getFirst().equals2D(other.lst.getFirst())){
            //flip this list and add at the end of this segment
            //flip = true;
            Collections.reverse(lst);
            this.flipEdges();
            this.lst.removeLast();
            this.lst.addAll(other.lst);
        }else if(this.lst.getLast().equals2D(other.lst.getLast())){
            //flip other list and add at the end of this segment
            //flip = true;
            Collections.reverse(lst);
            this.flipEdges();
            this.lst.removeFirst();
            this.lst.addAll(0,other.lst);
        }else{
            throw new IllegalArgumentException("Strings can not be merged, no common point");
        }
        
        other.locked = true;
        other.lst = this.lst;
        return;
    }
    
    private void flipEdges(){
        edge1.atEnd = !edge1.atEnd;
        edge2.atEnd = !edge2.atEnd;
    }
    
    @Override
    public boolean equals(Object obj) {
        return ((Construction)obj).lst == this.lst;
    }

    @Override
    public int hashCode() {
        return 17;
    }
    
    public void update(Boundary bnd){
        if(bnd==null) return;
        if(bnd.HMiddle!=null && bnd.HMiddle.getConstruction().equals(this)){
            if(bnd.HMiddle.atEnd){
                bnd.HMiddle = (edge1.atEnd) ? edge1 : edge2;
            }else{
                bnd.HMiddle = (!edge1.atEnd) ? edge1 : edge2;
            }
        }
        if(bnd.VMiddle!=null && bnd.VMiddle.getConstruction().equals(this)){
            if(bnd.VMiddle.atEnd){
                bnd.VMiddle = (edge1.atEnd) ? edge1 : edge2;
            }else{
                bnd.VMiddle = (!edge1.atEnd) ? edge1 : edge2;
            }
        }
    }
    
    public final class Edge{
        
        private boolean atEnd;

        public Edge(boolean atEnd) {
            this.atEnd = atEnd;
        }
        
        public Construction getConstruction(){
            return Construction.this;
        }
        
        public void add(Coordinate coord){
            if(locked) throw new IllegalStateException("Construction has been merged, should not be used anymore.");
            if(atEnd){
                lst.addLast(new Coordinate(coord));
            }else{
                lst.addFirst(new Coordinate(coord));
            }
        }

        @Override
        public boolean equals(Object obj) {
            return getConstruction().equals( ((Edge)obj).getConstruction() );
        }
        
        @Override
        public int hashCode() {
            return 3;
        }
        
    }
    
}
