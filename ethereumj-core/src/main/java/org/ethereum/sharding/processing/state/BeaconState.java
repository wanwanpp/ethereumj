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
package org.ethereum.sharding.processing.state;

import org.ethereum.datasource.Serializer;
import org.ethereum.sharding.processing.db.ValidatorSet;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPElement;
import org.ethereum.util.RLPList;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.ethereum.crypto.HashUtil.blake2b;
import static org.ethereum.sharding.processing.consensus.BeaconConstants.CYCLE_LENGTH;
import static org.ethereum.util.ByteUtil.ZERO_BYTE_ARRAY;
import static org.ethereum.util.ByteUtil.byteArrayToLong;
import static org.ethereum.util.ByteUtil.isSingleZero;
import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

/**
 * Beacon state data structure.
 *
 * @author Mikhail Kalinin
 * @since 14.08.2018
 */
public class BeaconState {

    /* Slot number that the state was calculated at */
    private final long lastStateRecalc;
    /* Set of validators */
    private final ValidatorSet validatorSet;
    /** Committee members and their assigned shard, per slot */
    private final Committee[][] committees;
    /* Randao seed that will be used for next shuffling */
    private final byte[] nextShufflingSeed;
    /* Slot of last validator set change */
    private final long validatorSetChangeSlot;
    /* The last justified slot */
    private final long lastJustifiedSlot;
    /* Number of consecutive justified slots ending at this one */
    private final long justifiedStreak;
    /* The last finalized slot */
    private final long lastFinalizedSlot;
    /* The most recent crosslinks for each shard */
    private final Crosslink[] crosslinks;
    /* Attestations that have not yet been processed */
    private final List<AttestationRecord> pendingAttestations;
    /* Special objects that have not yet been processed */
    private final List<SpecialRecord> pendingSpecials;
    /* Most recent 2 * CYCLE_LENGTH block hashes, older to newer */
    private final List<byte[]> recentBlockHashes;
    /* RANDAO state */
    private final byte[] randaoMix;

    public BeaconState(long lastStateRecalc, ValidatorSet validatorSet, Committee[][] committees,
                       byte[] nextShufflingSeed, long validatorSetChangeSlot, long lastJustifiedSlot,
                       long justifiedStreak, long lastFinalizedSlot, Crosslink[] crosslinks,
                       List<AttestationRecord> pendingAttestations, List<SpecialRecord> pendingSpecials,
                       List<byte[]> recentBlockHashes, byte[] randaoMix) {
        this.lastStateRecalc = lastStateRecalc;
        this.validatorSet = validatorSet;
        this.committees = committees;
        this.nextShufflingSeed = nextShufflingSeed;
        this.validatorSetChangeSlot = validatorSetChangeSlot;
        this.lastJustifiedSlot = lastJustifiedSlot;
        this.justifiedStreak = justifiedStreak;
        this.lastFinalizedSlot = lastFinalizedSlot;
        this.crosslinks = crosslinks;
        this.pendingAttestations = pendingAttestations;
        this.pendingSpecials = pendingSpecials;
        this.recentBlockHashes = recentBlockHashes;
        this.randaoMix = randaoMix;
    }

    public long getLastStateRecalc() {
        return lastStateRecalc;
    }

    public byte[] getNextShufflingSeed() {
        return nextShufflingSeed;
    }

    public long getValidatorSetChangeSlot() {
        return validatorSetChangeSlot;
    }

    public long getLastJustifiedSlot() {
        return lastJustifiedSlot;
    }

    public long getJustifiedStreak() {
        return justifiedStreak;
    }

    public long getLastFinalizedSlot() {
        return lastFinalizedSlot;
    }

    public Crosslink[] getCrosslinks() {
        return crosslinks;
    }

    public List<AttestationRecord> getPendingAttestations() {
        return pendingAttestations;
    }

    public List<SpecialRecord> getPendingSpecials() {
        return pendingSpecials;
    }

    public List<byte[]> getRecentBlockHashes() {
        return recentBlockHashes;
    }

    public byte[] getRandaoMix() {
        return randaoMix;
    }

    public Committee[][] getCommittees() {
        return committees;
    }

    public ValidatorSet getValidatorSet() {
        return validatorSet;
    }

    public BeaconState withValidatorSet(ValidatorSet validatorSet) {
        return new BeaconState(lastStateRecalc, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, pendingSpecials, recentBlockHashes, randaoMix);
    }

    public BeaconState withCommittees(Committee[][] committees) {
        return new BeaconState(lastStateRecalc, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, pendingSpecials, recentBlockHashes, randaoMix);
    }

    public BeaconState withValidatorSetChangeSlot(long validatorSetChangeSlot) {
        return new BeaconState(lastStateRecalc, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, pendingSpecials, recentBlockHashes, randaoMix);
    }

    public BeaconState withLastStateRecalc(long lastStateRecalc) {
        return new BeaconState(lastStateRecalc, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, pendingSpecials, recentBlockHashes, randaoMix);
    }

    public BeaconState withCrosslinks(Crosslink[] crosslinks) {
        return new BeaconState(lastStateRecalc, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, pendingSpecials, recentBlockHashes, randaoMix);
    }

    public BeaconState withPendingAttestations(List<AttestationRecord> pendingAttestations) {
        return new BeaconState(lastStateRecalc, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, pendingSpecials, recentBlockHashes, randaoMix);
    }

    public BeaconState withFinality(long lastJustifiedSlot, long justifiedStreak, long lastFinalizedSlot) {
        return new BeaconState(lastStateRecalc, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, pendingSpecials, recentBlockHashes, randaoMix);
    }

    public BeaconState increaseLastStateRecalc(long addition) {
        return withLastStateRecalc(lastStateRecalc + addition);
    }

    public int getCommitteesEndShard() {
        if (committees.length == 0)
            return 0;

        Committee[] endSlot = committees[committees.length - 1];
        if (endSlot.length == 0)
            return 0;

        return endSlot[endSlot.length - 1].getShardId();
    }

    /**
     * Adds new attestations to the end of the list
     * Produces new instance keeping immutability
     * @param pendingAttestations   Attestations to add
     * @return updated ActiveState
     */
    public BeaconState addPendingAttestations(List<AttestationRecord> pendingAttestations) {
        List<AttestationRecord> mergedAttestations = new ArrayList<>();
        mergedAttestations.addAll(this.getPendingAttestations());
        mergedAttestations.addAll(pendingAttestations);

        return withPendingAttestations(mergedAttestations);
    }

    public BeaconState removeOutdatedAttestations() {
        List<AttestationRecord> uptodateAttestations = new ArrayList<>();
        for (AttestationRecord record : pendingAttestations) {
            if (record.getSlot() >= lastStateRecalc) {
                uptodateAttestations.add(record);
            }
        }
        return withPendingAttestations(uptodateAttestations);
    }

    public byte[] getHash() {
        return flatten().getHash();
    }

    public Flattened flatten() {
        return new Flattened(validatorSet.getHash(), lastStateRecalc, committees,
                lastJustifiedSlot, justifiedStreak, lastFinalizedSlot, crosslinks, nextShufflingSeed,
                validatorSetChangeSlot, randaoMix, recentBlockHashes, pendingSpecials, pendingAttestations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof BeaconState)) return false;

        return FastByteComparisons.equal(((BeaconState) o).getHash(), this.getHash());
    }

    public static class Flattened {
        /* Hash of the validator set */
        private final byte[] validatorSetHash;
        /* Slot number that the state was calculated at */
        private final long lastStateRecalc;
        /** Committee members and their assigned shard, per slot */
        private final Committee[][] committees;
        /* The last justified slot */
        private final long lastJustifiedSlot;
        /* Number of consecutive justified slots ending at this one */
        private final long justifiedStreak;
        /* The last finalized slot */
        private final long lastFinalizedSlot;
        /* The most recent crosslinks for each shard */
        private final Crosslink[] crosslinks;
        /* Randao seed used that will be used for next shuffling */
        private final byte[] nextShufflingSeed;
        /* Slot of last validator set change */
        private final long validatorSetChangeSlot;
        /* Attestations that have not yet been processed */
        private final List<AttestationRecord> pendingAttestations;
        /* Special objects that have not yet been processed */
        private final List<SpecialRecord> pendingSpecials;
        /* Most recent 2 * CYCLE_LENGTH block hashes, older to newer */
        private final List<byte[]> recentBlockHashes;
        /* RANDAO state */
        private final byte[] randaoMix;

        public Flattened(byte[] validatorSetHash, long lastStateRecalc, Committee[][] committees, long lastJustifiedSlot,
                         long justifiedStreak, long lastFinalizedSlot, Crosslink[] crosslinks,
                         byte[] nextShufflingSeed, long validatorSetChangeSlot, byte[] randaoMix,
                         List<byte[]> recentBlockHashes, List<SpecialRecord> pendingSpecials,
                         List<AttestationRecord> pendingAttestations) {
            this.validatorSetHash = validatorSetHash;
            this.lastStateRecalc = lastStateRecalc;
            this.committees = committees;
            this.lastJustifiedSlot = lastJustifiedSlot;
            this.justifiedStreak = justifiedStreak;
            this.lastFinalizedSlot = lastFinalizedSlot;
            this.crosslinks = crosslinks;
            this.nextShufflingSeed = nextShufflingSeed;
            this.validatorSetChangeSlot = validatorSetChangeSlot;
            this.randaoMix = randaoMix;
            this.recentBlockHashes = recentBlockHashes;
            this.pendingSpecials = pendingSpecials;
            this.pendingAttestations = pendingAttestations;
        }

        public Flattened(byte[] encoded) {
            RLPList list = RLP.unwrapList(encoded);

            this.validatorSetHash = list.get(0).getRLPData();
            this.lastStateRecalc = byteArrayToLong(list.get(1).getRLPData());
            this.lastJustifiedSlot = byteArrayToLong(list.get(2).getRLPData());
            this.justifiedStreak = byteArrayToLong(list.get(3).getRLPData());
            this.lastFinalizedSlot = byteArrayToLong(list.get(4).getRLPData());
            this.validatorSetChangeSlot = byteArrayToLong(list.get(5).getRLPData());
            this.nextShufflingSeed = list.get(6).getRLPData();

            if (!isSingleZero(list.get(7).getRLPData())) {
                RLPList committeeList = RLP.unwrapList(list.get(7).getRLPData());
                this.committees = new Committee[committeeList.size()][];
                for (int i = 0; i < committeeList.size(); i++) {
                    if (!isSingleZero(committeeList.get(i).getRLPData())) {
                        RLPList slotList = RLP.unwrapList(committeeList.get(i).getRLPData());
                        Committee[] slotCommittees = new Committee[slotList.size()];
                        for (int j = 0; j < slotList.size(); j++) {
                            slotCommittees[j] = new Committee(slotList.get(j).getRLPData());
                        }
                        this.committees[i] = slotCommittees;
                    } else {
                        this.committees[i] = new Committee[0];
                    }
                }
            } else {
                this.committees = new Committee[0][];
            }

            if (!isSingleZero(list.get(8).getRLPData())) {
                RLPList crosslinkList = RLP.unwrapList(list.get(8).getRLPData());
                this.crosslinks = new Crosslink[crosslinkList.size()];
                for (int i = 0; i < crosslinkList.size(); i++)
                    this.crosslinks[i] = new Crosslink(crosslinkList.get(i).getRLPData());
            } else {
                this.crosslinks = new Crosslink[0];
            }

            this.pendingAttestations = new ArrayList<>();
            if (!isSingleZero(list.get(9).getRLPData())) {
                RLPList attestationList = RLP.unwrapList(list.get(9).getRLPData());
                for (RLPElement attestationRlp : attestationList)
                    pendingAttestations.add(new AttestationRecord(attestationRlp.getRLPData()));
            }

            this.pendingSpecials = new ArrayList<>();
            if (!isSingleZero(list.get(10).getRLPData())) {
                RLPList specialsList = RLP.unwrapList(list.get(10).getRLPData());
                for (RLPElement specialRlp : specialsList)
                    pendingSpecials.add(new SpecialRecord(specialRlp.getRLPData()));
            }

            this.recentBlockHashes = new ArrayList<>();
            if (!isSingleZero(list.get(11).getRLPData())) {
                RLPList hashesList = RLP.unwrapList(list.get(11).getRLPData());
                for (RLPElement hashRlp : hashesList)
                    recentBlockHashes.add(hashRlp.getRLPData());
            }

            this.randaoMix = list.get(12).getRLPData();
        }

        public byte[] encode() {
            byte[][] encodedCommittees = new byte[committees.length][];
            byte[][] encodedCrosslinks = new byte[crosslinks.length][];

            if (committees.length > 0) {
                for (int i = 0; i < committees.length; i++) {
                    Committee[] slotCommittees = committees[i];
                    byte[][] encodedSlot = new byte[slotCommittees.length][];
                    for (int j = 0; j < slotCommittees.length; j++) {
                        encodedSlot[j] = slotCommittees[j].getEncoded();
                    }
                    encodedCommittees[i] = slotCommittees.length > 0 ? RLP.wrapList(encodedSlot) : ZERO_BYTE_ARRAY;
                }
            }

            if (crosslinks.length > 0) {
                for (int i = 0; i < crosslinks.length; i++)
                    encodedCrosslinks[i] = crosslinks[i].getEncoded();
            }

            byte[][] encodedAttestations = new byte[pendingAttestations.size()][];
            for (int i = 0; i < pendingAttestations.size(); i++)
                encodedAttestations[i] = pendingAttestations.get(i).getEncoded();

            byte[][] encodedSpecials = new byte[pendingSpecials.size()][];
            for (int i = 0; i < pendingSpecials.size(); i++)
                encodedSpecials[i] = pendingSpecials.get(i).getEncoded();

            byte[][] encodedHashes = new byte[recentBlockHashes.size()][];
            for (int i = 0; i < recentBlockHashes.size(); i++)
                encodedHashes[i] = recentBlockHashes.get(i);

            return RLP.wrapList(validatorSetHash, longToBytesNoLeadZeroes(lastStateRecalc),
                    longToBytesNoLeadZeroes(lastJustifiedSlot), longToBytesNoLeadZeroes(justifiedStreak),
                    longToBytesNoLeadZeroes(lastFinalizedSlot),
                    longToBytesNoLeadZeroes(validatorSetChangeSlot), nextShufflingSeed,
                    encodedCommittees.length > 0 ? RLP.wrapList(encodedCommittees) : ZERO_BYTE_ARRAY,
                    encodedCrosslinks.length > 0 ? RLP.wrapList(encodedCrosslinks) : ZERO_BYTE_ARRAY,
                    pendingAttestations.size() > 0 ? RLP.wrapList(encodedAttestations) : ZERO_BYTE_ARRAY,
                    pendingSpecials.size() > 0 ? RLP.wrapList(encodedSpecials) : ZERO_BYTE_ARRAY,
                    recentBlockHashes.size() > 0 ? RLP.wrapList(encodedHashes) : ZERO_BYTE_ARRAY,
                    randaoMix);
        }

        public byte[] getHash() {
            return blake2b(encode());
        }

        public byte[] getValidatorSetHash() {
            return validatorSetHash;
        }

        public long getLastStateRecalc() {
            return lastStateRecalc;
        }

        public Committee[][] getCommittees() {
            return committees;
        }

        public long getLastJustifiedSlot() {
            return lastJustifiedSlot;
        }

        public long getJustifiedStreak() {
            return justifiedStreak;
        }

        public long getLastFinalizedSlot() {
            return lastFinalizedSlot;
        }

        public Crosslink[] getCrosslinks() {
            return crosslinks;
        }

        public byte[] getNextShufflingSeed() {
            return nextShufflingSeed;
        }

        public long getValidatorSetChangeSlot() {
            return validatorSetChangeSlot;
        }

        public List<AttestationRecord> getPendingAttestations() {
            return pendingAttestations;
        }

        public List<SpecialRecord> getPendingSpecials() {
            return pendingSpecials;
        }

        public List<byte[]> getRecentBlockHashes() {
            return recentBlockHashes;
        }

        public byte[] getRandaoMix() {
            return randaoMix;
        }

        public static org.ethereum.datasource.Serializer<Flattened, byte[]> getSerializer() {
            return Serializer;
        }

        public static Flattened empty() {
            List<byte[]> recentBlockHashes = new ArrayList<>();
            for (int i = 0; i < (CYCLE_LENGTH * 2); ++i) {
                recentBlockHashes.add(new byte[32]);
            }
            return new Flattened(ValidatorSet.EMPTY_HASH, 0, new Committee[0][], 0L, 0L, 0L,
                    new Crosslink[0], new byte[32], 0L, new byte[32], recentBlockHashes, emptyList(), emptyList());
        }

        public static final org.ethereum.datasource.Serializer<Flattened, byte[]> Serializer = new Serializer<Flattened, byte[]>() {
            @Override
            public byte[] serialize(Flattened state) {
                return state == null ? null : state.encode();
            }

            @Override
            public Flattened deserialize(byte[] stream) {
                return stream == null ? null : new Flattened(stream);
            }
        };
    }
}
