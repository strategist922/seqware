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
package com.github.seqware.impl.protobufIO;

import com.github.seqware.dto.QESupporting.SGIDPB;
import com.github.seqware.dto.QueryEngine;
import com.github.seqware.dto.QueryEngine.TagSetPB;
import com.github.seqware.factory.Factory;
import com.github.seqware.model.Tag;
import com.github.seqware.model.TagSet;
import com.github.seqware.model.User;
import com.github.seqware.model.impl.AtomImpl;
import com.github.seqware.model.impl.MoleculeImpl;
import com.github.seqware.model.impl.inMemory.InMemoryTagSet;
import com.github.seqware.util.SGID;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dyuen
 */
public class TagSetIO implements ProtobufTransferInterface<TagSetPB, TagSet>{

    @Override
    public TagSet pb2m(TagSetPB userpb) {
        TagSet.Builder builder = InMemoryTagSet.newBuilder();
        builder = userpb.hasName() ? builder.setName(userpb.getName()) : builder;
        TagSet user = builder.build();
        UtilIO.handlePB2Atom(userpb.getAtom(), (AtomImpl)user);
        UtilIO.handlePB2ACL(userpb.getAcl(), (MoleculeImpl)user);
        if (ProtobufTransferInterface.PERSIST_VERSION_CHAINS && userpb.hasPrecedingVersion()){
           user.setPrecedingVersion(pb2m(userpb.getPrecedingVersion()));
        }
        SGID[] sgidArr = new SGID[userpb.getTagIDsCount()];
        for(int i = 0; i < sgidArr.length; i++){
            sgidArr[i] = (SGIDIO.pb2m(userpb.getTagIDs(i)));
        }
        List<Tag> atomsBySGID = Factory.getFeatureStoreInterface().getAtomsBySGID(Tag.class, sgidArr);
        if (atomsBySGID != null && atomsBySGID.size() > 0) {user.add(atomsBySGID);}
        return user;
    }
    

    @Override
    public TagSetPB m2pb(TagSet sgid) {
        QueryEngine.TagSetPB.Builder builder = QueryEngine.TagSetPB.newBuilder();
        builder = sgid.getName() != null ? builder.setName(sgid.getName()) : builder;
        builder.setAtom(UtilIO.handleAtom2PB(builder.getAtom(), (AtomImpl)sgid));
        builder.setAcl(UtilIO.handleACL2PB(builder.getAcl(), (MoleculeImpl)sgid));
        if (ProtobufTransferInterface.PERSIST_VERSION_CHAINS && sgid.getPrecedingVersion() != null){
            builder.setPrecedingVersion(m2pb(sgid.getPrecedingVersion()));
        }
        for(Tag ref : sgid){
            builder.addTagIDs(SGIDIO.m2pb(ref.getSGID()));
        }
        TagSetPB userpb = builder.build();
        return userpb;
    }

    @Override
    public TagSet byteArr2m(byte[] arr) {
        try {
            QueryEngine.TagSetPB userpb = QueryEngine.TagSetPB.parseFrom(arr);
            return pb2m(userpb);
        } catch (InvalidProtocolBufferException ex) {
            Logger.getLogger(FeatureSetIO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}