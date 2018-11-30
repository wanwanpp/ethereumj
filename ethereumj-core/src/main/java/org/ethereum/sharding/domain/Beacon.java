/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ethereum.sharding.domain;

import org.ethereum.datasource.Serializer;
import org.ethereum.sharding.crypto.Sign;
import org.ethereum.sharding.processing.state.AttestationRecord;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.ethereum.crypto.HashUtil.blake2b;
import static org.ethereum.sharding.util.HashUtils.ZERO_HASH32;
import static org.ethereum.util.ByteUtil.ZERO_BYTE_ARRAY;
import static org.ethereum.util.ByteUtil.isSingleZero;

/**
 * Beacon chain block structure.
 *
 * @author Mikhail Kalinin
 * @since 14.08.2018
 */
public class Beacon {

    /* Hash of the parent block */
    private byte[] parentHash;
    /* Proposer RANDAO reveal */
    private byte[] randaoReveal;
    /* Reference to main chain block */
    private byte[] mainChainRef;
    /* State root */
    private byte[] stateRoot;
    /* Slot number */
    private long slot;
    /* Attestations */
    private List<AttestationRecord> attestations;
    /* Proposer signature */
    private Sign.Signature proposerSignature;

    public Beacon(byte[] parentHash, byte[] randaoReveal, byte[] mainChainRef, byte[] stateRoot,
                  long slot, List<AttestationRecord> attestations, Sign.Signature proposerSignature) {
        this.parentHash = parentHash;
        this.randaoReveal = randaoReveal;
        this.mainChainRef = mainChainRef;
        this.stateRoot = stateRoot;
        this.slot = slot;
        this.attestations = attestations;
        this.proposerSignature = proposerSignature;
    }

    public Beacon(byte[] parentHash, byte[] randaoReveal, byte[] mainChainRef, byte[] stateRoot,
                  long slot, List<AttestationRecord> attestations) {
        this(parentHash, randaoReveal, mainChainRef, stateRoot, slot, attestations, new Sign.Signature());
    }

    public Beacon(byte[] rlp) {
        RLPList items = RLP.unwrapList(rlp);
        this.parentHash = items.get(0).getRLPData();
        this.randaoReveal = items.get(1).getRLPData();
        this.mainChainRef = items.get(2).getRLPData();
        this.stateRoot = items.get(3).getRLPData();
        this.slot = ByteUtil.bytesToBigInteger(items.get(4).getRLPData()).longValue();

        this.attestations = new ArrayList<>();
        if (!isSingleZero(items.get(5).getRLPData())) {
            RLPList attestationsRlp = RLP.unwrapList(items.get(5).getRLPData());
            for (RLPElement anAttestationsRlp : attestationsRlp) {
                attestations.add(new AttestationRecord(anAttestationsRlp.getRLPData()));
            }
        }
        this.proposerSignature = new Sign.Signature(items.get(6).getRLPData());
    }

    public byte[] getEncoded(boolean withSignature) {
        byte[][] encodedAttestations = new byte[attestations.size()][];
        for (int i = 0; i < attestations.size(); i++)
            encodedAttestations[i] = attestations.get(i).getEncoded();

        return RLP.wrapList(parentHash, randaoReveal, mainChainRef, stateRoot,
                BigInteger.valueOf(slot).toByteArray(),
                encodedAttestations.length == 0 ? ZERO_BYTE_ARRAY : RLP.wrapList(encodedAttestations),
                withSignature && proposerSignature != null ? proposerSignature.getEncoded() :
                        new Sign.Signature().getEncoded());
    }

    public byte[] getHash() {
        if (this.isGenesis()) {
            return ZERO_HASH32;
        } else {
            return blake2b(getEncoded(true));
        }
    }

    public byte[] getHashWithoutSignature() {
        return blake2b(getEncoded(false));
    }

    public byte[] getParentHash() {
        return parentHash;
    }

    public byte[] getRandaoReveal() {
        return randaoReveal;
    }

    public byte[] getMainChainRef() {
        return mainChainRef;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public long getSlot() {
        return slot;
    }

    public List<AttestationRecord> getAttestations() {
        return attestations;
    }

    public Sign.Signature getProposerSignature() {
        return proposerSignature;
    }

    public void setProposerSignature(Sign.Signature proposerSignature) {
        this.proposerSignature = proposerSignature;
    }

    public boolean isParentOf(Beacon other) {
        return FastByteComparisons.equal(this.getHash(), other.getParentHash());
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
    }

    public void setAttestations(List<AttestationRecord> attestations) {
        this.attestations = attestations;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if ((other == null) || getClass() != other.getClass()) return false;

        return FastByteComparisons.equal(this.getHash(), ((Beacon) other).getHash());
    }

    @Override
    public String toString() {
        if (this.isGenesis()) {
            return "#0 (Genesis)";
        } else {
            return "#" + getSlot() + " (" + Hex.toHexString(getHash()).substring(0, 6) + " <~ "
                    + Hex.toHexString(getParentHash()).substring(0, 6) + "; mainChainRef: " +
                    Hex.toHexString(mainChainRef).substring(0, 6) + ")";
        }
    }

    public boolean isGenesis() {
        return slot == 0L;
    }

    /**
     * Genesis is an empty block with 0 slot number and hash full of zeros.
     * Actually, there is no genesis block in beacon chain. However, it's handy to have it here and there.
     */
    public static Beacon genesis() {
        return new Beacon(ZERO_HASH32, ZERO_HASH32, ZERO_HASH32, ZERO_HASH32,
                0L, Collections.emptyList(), new Sign.Signature());
    }

    public static final Serializer<Beacon, byte[]> Serializer = new Serializer<Beacon, byte[]>() {
        @Override
        public byte[] serialize(Beacon block) {
            return block == null ? null : block.getEncoded(true);
        }

        @Override
        public Beacon deserialize(byte[] stream) {
            return stream == null ? null : new Beacon(stream);
        }
    };
}
