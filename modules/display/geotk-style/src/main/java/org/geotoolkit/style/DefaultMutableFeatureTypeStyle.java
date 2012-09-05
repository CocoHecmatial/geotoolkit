/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008 - 2009, Geomatys
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
package org.geotoolkit.style;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.EventObject;
import java.util.List;
import java.util.Set;
import javax.swing.event.EventListenerList;

import org.geotoolkit.gui.swing.tree.Trees;
import org.geotoolkit.util.NumberRange;
import org.geotoolkit.util.Utilities;
import org.geotoolkit.util.collection.CollectionChangeEvent;
import org.geotoolkit.util.collection.NotifiedCheckedList;
import org.geotoolkit.util.collection.NotifiedCheckedSet;
import org.geotoolkit.util.converter.Classes;

import org.opengis.feature.type.Name;
import org.opengis.filter.Id;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.style.Description;
import org.opengis.style.SemanticType;
import org.opengis.style.StyleVisitor;
import org.opengis.style.Symbolizer;

import static org.geotoolkit.util.ArgumentChecks.*;

/**
 * Mutable implementation of GeoAPI FeatureTypeStyle.
 * 
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public class DefaultMutableFeatureTypeStyle implements MutableFeatureTypeStyle, RuleListener{
    
    private final List<MutableRule> rules = new NotifiedCheckedList<MutableRule>(MutableRule.class) {

            @Override
            protected Object getLock() {
                return DefaultMutableFeatureTypeStyle.this;
            }

            @Override
            protected void notifyAdd(final MutableRule item, final int index) {
                ruleListener.registerSource(item);
                fireRuleChange(CollectionChangeEvent.ITEM_ADDED, item, NumberRange.create(index, index) );
            }

            @Override
            protected void notifyAdd(final Collection<? extends MutableRule> items, final NumberRange<Integer> range) {
                for(final MutableRule item : items){
                    ruleListener.registerSource(item);
                }
                fireRuleChange(CollectionChangeEvent.ITEM_ADDED, items, range);
            }

            @Override
            protected void notifyRemove(final MutableRule item, int index) {
                ruleListener.unregisterSource(item);
                fireRuleChange(CollectionChangeEvent.ITEM_REMOVED, item, NumberRange.create(index, index) );
            }

            @Override
            protected void notifyRemove(final Collection<? extends MutableRule> items, final NumberRange<Integer> range) {
                for(final MutableRule rule : items){
                    ruleListener.unregisterSource(rule);
                }
                fireRuleChange(CollectionChangeEvent.ITEM_REMOVED, items, range );
            }

            @Override
            protected void notifyChange(MutableRule oldItem, MutableRule newItem, int index) {
                if(oldItem != null){
                    ruleListener.unregisterSource(oldItem);
                }
                if(newItem != null){
                    ruleListener.registerSource(newItem);
                }
                fireRuleChange(CollectionChangeEvent.ITEM_CHANGED, oldItem, NumberRange.create(index, index) );
            }
            
        };
        
    private final Set<Name> names = new NotifiedCheckedSet<Name>(Name.class){

        @Override
        protected Object getLock() {
            return DefaultMutableFeatureTypeStyle.this;
        }

        @Override
        protected void notifyAdd(Name item, NumberRange<Integer> range) {
            fireNameChange(CollectionChangeEvent.ITEM_ADDED, item, range);
        }

        @Override
        protected void notifyAdd(Collection<? extends Name> items, NumberRange<Integer> range) {
            fireNameChange(CollectionChangeEvent.ITEM_ADDED, items, range);
        }

        @Override
        protected void notifyRemove(Name item, NumberRange<Integer> range) {
            fireNameChange(CollectionChangeEvent.ITEM_REMOVED, item, range);
        }
    };
    
    private final Set<SemanticType> semantics = new NotifiedCheckedSet<SemanticType>(SemanticType.class){

        @Override
        protected Object getLock() {
            return DefaultMutableFeatureTypeStyle.this;
        }

        @Override
        protected void notifyAdd(SemanticType item, NumberRange<Integer> range) {
            fireSemanticChange(CollectionChangeEvent.ITEM_ADDED, item, range);
        }

        @Override
        protected void notifyAdd(Collection<? extends SemanticType> items, NumberRange<Integer> range) {
            fireSemanticChange(CollectionChangeEvent.ITEM_ADDED, items, range);
        }

        @Override
        protected void notifyRemove(SemanticType item, NumberRange<Integer> range) {
            fireSemanticChange(CollectionChangeEvent.ITEM_REMOVED, item, range);
        }
    };
    
    private final RuleListener.Weak ruleListener = new RuleListener.Weak(this);
        
    private final EventListenerList listeners = new EventListenerList();
    
    private String name = null;
    
    private Description desc = StyleConstants.DEFAULT_DESCRIPTION;
    
    private Id ids = null;
    
    private OnlineResource online = null;
    
    /**
     * Create a default mutable feature type style.
     */
    public DefaultMutableFeatureTypeStyle(){}
        
    /**
     * {@inheritDoc }
     * This method is thread safe.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set the name of the feature type style.
     * This method is thread safe.
     */
    @Override
    public void setName(final String name) {
        final String oldName;
        synchronized (this) {
            oldName = this.name;
            if (Utilities.equals(oldName,name)) {
                return;
            }
            this.name = name;
        }
        firePropertyChange(NAME_PROPERTY, oldName, this.name);
    }
    
    /**
     * {@inheritDoc }
     * This method is thread safe.
     */
    @Override
    public Description getDescription() {
        return desc;
    }

    /**
     * Set the Description of the feature type style.
     * @param desc : Description can't be null
     */
    @Override
    public void setDescription(final Description desc){
        ensureNonNull("description", desc);
        
        final Description oldDesc;
        synchronized (this) {
            oldDesc = this.desc;
            if(oldDesc.equals(desc)){
                return;
            }
            this.desc = desc;
        }
        firePropertyChange(DESCRIPTION_PROPERTY, oldDesc, this.desc);
    }

    /**
     * {@inheritDoc }
     * @return Id : This is the "living" Id collection.
     */
    @Override
    public Id getFeatureInstanceIDs() {
        return ids;
    }

    /**
     * {@inheritDoc }
     * This method is thread safe.
     */
    @Override
    public void setFeatureInstanceIDs(final Id id){
        final Id oldIds;
        synchronized (this) {
            oldIds = this.ids;
            if(Utilities.equals(oldIds, id)){
                return;
            }
            this.ids = id;
        }
        firePropertyChange(IDS_PROPERTY, oldIds, this.ids);
    }
    
    /**
     * {@inheritDoc }
     * @return Set<Name> : This is the "living" Set.
     */
    @Override
    public Set<Name> featureTypeNames() {
        return names;
    }

    /**
     * {@inheritDoc }
     * @return Set<SemanticType> : This is the "living" Set.
     */
    @Override
    public Set<SemanticType> semanticTypeIdentifiers() {
        return semantics;
    }

    /**
     * {@inheritDoc }
     * @return List<Rule> : This is the "living" List.
     */
    @Override
    public List<MutableRule> rules() {
        return rules;
    }

    /**
     * {@inheritDoc }
     * This method is thread safe.
     */
    @Override
    public OnlineResource getOnlineResource() {
        return online;
    }
    
    /**
     * {@inheritDoc }
     * This method is thread safe.
     */
    @Override
    public void setOnlineResource(final OnlineResource online) {
        final OnlineResource oldOnline;
        synchronized (this) {
            oldOnline = this.online;
            if(Utilities.equals(oldOnline, online)){
                return;
            }
            this.online = online;
        }
        firePropertyChange(ONLINE_PROPERTY, oldOnline, this.online);
    }
    
    @Override
    public Object accept(final StyleVisitor visitor, final Object extraData) {
        return visitor.visit(this,extraData);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("FeatureTypeStyle : ");
        builder.append(Classes.getShortClassName(this));
        builder.append(" [");
        builder.append(desc);
        builder.append(']');

        if(!rules.isEmpty()){
            builder.append(Trees.toString("", rules));
        }

        return builder.toString();
    }
    
    //--------------------------------------------------------------------------
    // listeners management ----------------------------------------------------
    //--------------------------------------------------------------------------
    
    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue){
        //TODO make fire property change thread safe, preserve fire order
        
        final PropertyChangeEvent event = new PropertyChangeEvent(this,propertyName,oldValue,newValue);
        final PropertyChangeListener[] lists = listeners.getListeners(PropertyChangeListener.class);
        
        for(PropertyChangeListener listener : lists){
            listener.propertyChange(event);
        }
        
    }
    
    protected void fireRuleChange(final int type, final MutableRule symbol, final NumberRange<Integer> range) {
        //TODO make fire property change thread safe, preserve fire order

        final CollectionChangeEvent<MutableRule> event = new CollectionChangeEvent<MutableRule>(this, symbol, type, range,null);
        final FeatureTypeStyleListener[] lists = listeners.getListeners(FeatureTypeStyleListener.class);

        for (FeatureTypeStyleListener listener : lists) {
            listener.ruleChange(event);
        }

    }
    
    protected void fireRuleChange(final int type, final MutableRule symbol, final NumberRange<Integer> range, final EventObject subEvent) {
        //TODO make fire property change thread safe, preserve fire order

        final CollectionChangeEvent<MutableRule> event = new CollectionChangeEvent<MutableRule>(this, symbol, type, range,subEvent);
        final FeatureTypeStyleListener[] lists = listeners.getListeners(FeatureTypeStyleListener.class);

        for (FeatureTypeStyleListener listener : lists) {
            listener.ruleChange(event);
        }

    }
    
    protected void fireRuleChange(final int type, final Collection<? extends MutableRule> symbol, final NumberRange<Integer> range){
        //TODO make fire property change thread safe, preserve fire order
        
        final CollectionChangeEvent<MutableRule> event = new CollectionChangeEvent<MutableRule>(this,symbol,type,range,null);
        final FeatureTypeStyleListener[] lists = listeners.getListeners(FeatureTypeStyleListener.class);
        
        for(FeatureTypeStyleListener listener : lists){
            listener.ruleChange(event);
        }
        
    }
    
    protected void fireNameChange(final int type, final Name ftsName, final NumberRange<Integer> range) {
        //TODO make fire property change thread safe, preserve fire order

        final CollectionChangeEvent<Name> event = new CollectionChangeEvent<Name>(this, ftsName, type, range, null);
        final FeatureTypeStyleListener[] lists = listeners.getListeners(FeatureTypeStyleListener.class);

        for (FeatureTypeStyleListener listener : lists) {
            listener.featureTypeNameChange(event);
        }

    }
    
    protected void fireNameChange(final int type, final Collection<? extends Name> ftsNames, final NumberRange<Integer> range){
        //TODO make fire property change thread safe, preserve fire order
        
        final CollectionChangeEvent<Name> event = new CollectionChangeEvent<Name>(this,ftsNames,type,range, null);
        final FeatureTypeStyleListener[] lists = listeners.getListeners(FeatureTypeStyleListener.class);
        
        for(FeatureTypeStyleListener listener : lists){
            listener.featureTypeNameChange(event);
        }
        
    }
    
    protected void fireSemanticChange(final int type, final SemanticType semantic, final NumberRange<Integer> range) {
        //TODO make fire property change thread safe, preserve fire order

        final CollectionChangeEvent<SemanticType> event = new CollectionChangeEvent<SemanticType>(this, semantic, type, range, null);
        final FeatureTypeStyleListener[] lists = listeners.getListeners(FeatureTypeStyleListener.class);

        for (FeatureTypeStyleListener listener : lists) {
            listener.semanticTypeChange(event);
        }

    }
    
    protected void fireSemanticChange(final int type, final Collection<? extends SemanticType> semantics, final NumberRange<Integer> range){
        //TODO make fire property change thread safe, preserve fire order
        
        final CollectionChangeEvent<SemanticType> event = new CollectionChangeEvent<SemanticType>(this,semantics,type,range,null);
        final FeatureTypeStyleListener[] lists = listeners.getListeners(FeatureTypeStyleListener.class);
        
        for(FeatureTypeStyleListener listener : lists){
            listener.semanticTypeChange(event);
        }
        
    }

    //--------------------------------------------------------------------------
    // rule listener -----------------------------------------------------------
    //--------------------------------------------------------------------------

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        final int index = rules.indexOf(event.getSource());
        fireRuleChange(CollectionChangeEvent.ITEM_CHANGED, (MutableRule)event.getSource(),
                NumberRange.create(index, index), event);
    }

    @Override
    public void symbolizerChange(final CollectionChangeEvent<Symbolizer> event) {
        final int index = rules.indexOf(event.getSource());
        fireRuleChange(CollectionChangeEvent.ITEM_CHANGED, (MutableRule)event.getSource(),
                NumberRange.create(index, index), event);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void addListener(final FeatureTypeStyleListener listener){
        addListener((PropertyChangeListener)listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void addListener(final PropertyChangeListener listener){
        listeners.add(PropertyChangeListener.class, listener);
        if(listener instanceof FeatureTypeStyleListener){
            listeners.add(FeatureTypeStyleListener.class, (FeatureTypeStyleListener)listener);
        }
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public void removeListener(final PropertyChangeListener listener){
        listeners.remove(PropertyChangeListener.class, listener);
        if(listener instanceof FeatureTypeStyleListener){
            listeners.remove(FeatureTypeStyleListener.class, (FeatureTypeStyleListener)listener);
        }
    }
    
}
