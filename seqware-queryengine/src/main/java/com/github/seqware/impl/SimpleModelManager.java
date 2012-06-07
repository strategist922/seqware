/*
 * Copyright (C) 2012 SeqWare
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.seqware.impl;

import com.github.seqware.factory.Factory;
import com.github.seqware.factory.ModelManager;
import com.github.seqware.model.*;
import com.github.seqware.model.Analysis.Builder;
import com.github.seqware.model.impl.inMemory.*;
import com.github.seqware.util.SGID;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * A Simple implementation of the ModelManager interface. We can make this more
 * efficient later.
 *
 * The current idea is that we try to minimize the interaction with the user by
 * using Hibernate/JPA-like semantics
 *
 * @author dyuen
 */
public class SimpleModelManager implements ModelManager {

    private Map<SGID, ParticleStatePair> dirtySet = new HashMap<SGID, ParticleStatePair>();

    @Override
    public void persist(Particle p) {
        this.dirtySet.put(p.getSGID(), new ParticleStatePair(p, State.MANAGED));
    }

    @Override
    public void clear() {
        dirtySet.clear();
    }

    @Override
    public void close() {
        this.flush();
        this.clear();
    }

    @Override
    public void flush() {
        // update dirty objects
        for (Entry<SGID, ParticleStatePair> p : dirtySet.entrySet()) {
            if (p.getValue().getState() == State.NEW_CREATION || p.getValue().getState() == State.NEW_VERSION){
                if (p.getValue().getState() == State.NEW_VERSION){
                    // if they have preceding versions do an update, otherwise store
                    // only molecules should have preceding states
                    Factory.getBackEnd().update((Atom)dirtySet.get(p.getKey()).getP());
                } else {
                    Factory.getBackEnd().store(dirtySet.get(p.getKey()).getP());
                }
            }
            p.getValue().setState(State.MANAGED);
        }
        
    }

    @Override
    public FeatureSet.Builder buildFeatureSet() {
        FeatureSet.Builder fSet = null;
        if (Factory.getBackEnd() instanceof SimplePersistentBackEnd) {
            fSet = InMemoryFeatureSet.newBuilder().setManager(this);
        }
        return fSet;
    }

    @Override
    public Reference.Builder buildReference() {
        Reference.Builder ref = null;
        if (Factory.getBackEnd() instanceof SimplePersistentBackEnd) {
            ref = InMemoryReference.newBuilder().setManager(this);
        }
        assert (ref != null);
        return ref;
    }

    @Override
    public ReferenceSet.Builder buildReferenceSet() {
        ReferenceSet.Builder rSet = null;
        if (Factory.getBackEnd() instanceof SimplePersistentBackEnd) {
            rSet = InMemoryReferenceSet.newBuilder().setManager(this);
        }
        assert (rSet != null);
        return rSet;
    }

    @Override
    public TagSet.Builder buildTagSet() {
        TagSet.Builder tSet = null;
        if (Factory.getBackEnd() instanceof SimplePersistentBackEnd) {
            tSet = InMemoryTagSet.newBuilder().setManager(this);
        }
        assert (tSet != null);
        return tSet;
    }

    @Override
    public AnalysisSet.Builder buildAnalysisSet() {
        AnalysisSet.Builder aSet = null;
        if (Factory.getBackEnd() instanceof SimplePersistentBackEnd) {
            return InMemoryAnalysisSet.newBuilder().setManager(this);
        }
        assert (aSet != null);
        return aSet;
    }

    @Override
    public User.Builder buildUser() {
        User.Builder aSet = null;
        if (Factory.getBackEnd() instanceof SimplePersistentBackEnd) {
            return User.newBuilder().setManager(this);
        }
        assert (aSet != null);
        return aSet;
    }

    @Override
    public Group.Builder buildGroup() {
        Group.Builder aSet = null;
        if (Factory.getBackEnd() instanceof SimplePersistentBackEnd) {
            return Group.newBuilder().setManager(this);
        }
        assert (aSet != null);
        return aSet;
    }

    @Override
    public Tag.Builder buildTag() {
        Tag.Builder aSet = null;
        if (Factory.getBackEnd() instanceof SimplePersistentBackEnd) {
            return Tag.newBuilder().setManager(this);
        }
        assert (aSet != null);
        return aSet;
    }

    @Override
    public Feature.Builder buildFeature() {
        Feature.Builder aSet = null;
        if (Factory.getBackEnd() instanceof SimplePersistentBackEnd) {
            return Feature.newBuilder().setManager(this);
        }
        assert (aSet != null);
        return aSet;
    }
  
    
    @Override
    public void objectCreated(Particle source) {
        particleStateChange(source, State.NEW_CREATION);
    }

    @Override
    public void particleStateChange(Particle source, State state) {
        // check for valid state transitions
        boolean validTransition = false;
        if (this.dirtySet.containsKey(source.getSGID())){
            State current = this.dirtySet.get(source.getSGID()).getState();
            if (current == State.MANAGED && state == State.NEW_VERSION){
                validTransition = true;
            } else if (current == State.MANAGED && state == State.NEW_CREATION){
                validTransition = true;
            } else if (current == State.UNMANAGED && state == State.MANAGED){
                validTransition = true;
            } else if (current == State.NEW_CREATION && state == State.MANAGED){
                validTransition = true;
            } else if (current == State.NEW_VERSION && state == State.MANAGED){
                validTransition = true;
            }
        } else{
            // assume all other transitions are valid for now
            validTransition = true;
        }
        if (validTransition){
            this.dirtySet.put(source.getSGID(), new ParticleStatePair(source, state));
        }
    }

    @Override
    public Builder buildAnalysis() {
        Analysis.Builder aSet = null;
        if (Factory.getBackEnd() instanceof SimplePersistentBackEnd) {
            return InMemoryQueryFutureImpl.newBuilder().setManager(this);
        }
        assert (aSet != null);
        return aSet;
    }
    
    protected class ParticleStatePair{
        protected ParticleStatePair(Particle p, State state){
            this.p = p;
            this.state = state;
        }
        private Particle p;
        private State state;

        /**
         * @return the p
         */
        protected Particle getP() {
            return p;
        }

        /**
         * @param p the p to set
         */
        protected void setP(Particle p) {
            this.p = p;
        }

        /**
         * @return the state
         */
        protected State getState() {
            return state;
        }

        /**
         * @param state the state to set
         */
        protected void setState(State state) {
            this.state = state;
        }
    }
}