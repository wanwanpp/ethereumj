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

import org.ethereum.sharding.processing.db.ValidatorSet;
import org.ethereum.util.FastByteComparisons;

import java.util.ArrayList;
import java.util.List;

import static org.ethereum.util.ByteUtil.ZERO_BYTE_ARRAY;

/**
 * Beacon state data structure.
 *
 * @author Mikhail Kalinin
 * @since 14.08.2018
 */
public class BeaconState {

    /* Last cycle-boundary state recalculation */
    private final long lastStateRecalculationSlot;

    /* Set of validators */
    private final ValidatorSet validatorSet;
    /** Committee members and their assigned shard, per slot */
    private final Committee[][] committees;
    /* Slot of last validator set change */
    private final long validatorSetChangeSlot;
    /* Hash chain of validator set changes (for light clients to easily track deltas) */
    private final byte[] validatorSetDeltaHashChain = ZERO_BYTE_ARRAY;

    /* Last justified slot */
    private final long lastJustifiedSlot;
    /* Number of consecutive justified slots */
    private final long justifiedStreak;
    /* Last finalized slot */
    private final long lastFinalizedSlot;

    /* Most recent crosslink for each shard */
    private final Crosslink[] crosslinks;
    /* Persistent shard committees */
    private final int[][] persistentCommittees = new int[0][];
    private final List<ShardReassignmentRecord> persistentCommitteeReassignments = new ArrayList<>();

    /* Total deposits penalized in the given withdrawal period */
    private final long[] depositsPenalizedInPeriod = new long[0];
    /* Current sequence number for withdrawals */
    private final long currentExitSeq = 0;

    /* Genesis time */
    private final long genesisTime = 0;

    /* PoW chain reference */
    private final byte[] knownPowReceiptRoot = ZERO_BYTE_ARRAY;
    private final byte[] candidatePowReceiptRoot = ZERO_BYTE_ARRAY;
    private final long candidatePowReceiptRootVotes = 0;

    /* Parameters relevant to hard forks / versioning. Should be updated only by hard forks. */
    private final long preForkVersion = 0;
    private final long postForkVersion = 0;
    private final long forkSlotNumber = 0;

    /* Attestations not yet processed */
    private final List<AttestationRecord> pendingAttestations;
    /* Recent beacon block hashes needed to process attestations, older to newer */
    private final List<byte[]> recentBlockHashes;

    /* RANDAO seed used for next shuffling */
    private final byte[] nextShufflingSeed;
    /* RANDAO state */
    private final byte[] randaoMix;

    public BeaconState(long lastStateRecalculationSlot, ValidatorSet validatorSet, Committee[][] committees,
                       byte[] nextShufflingSeed, long validatorSetChangeSlot, long lastJustifiedSlot,
                       long justifiedStreak, long lastFinalizedSlot, Crosslink[] crosslinks,
                       List<AttestationRecord> pendingAttestations, List<byte[]> recentBlockHashes, byte[] randaoMix) {
        this.lastStateRecalculationSlot = lastStateRecalculationSlot;
        this.validatorSet = validatorSet;
        this.committees = committees;
        this.nextShufflingSeed = nextShufflingSeed;
        this.validatorSetChangeSlot = validatorSetChangeSlot;
        this.lastJustifiedSlot = lastJustifiedSlot;
        this.justifiedStreak = justifiedStreak;
        this.lastFinalizedSlot = lastFinalizedSlot;
        this.crosslinks = crosslinks;
        this.pendingAttestations = pendingAttestations;
        this.recentBlockHashes = recentBlockHashes;
        this.randaoMix = randaoMix;
    }

    public long getLastStateRecalculationSlot() {
        return lastStateRecalculationSlot;
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
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix);
    }

    public BeaconState withCommittees(Committee[][] committees) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix);
    }

    public BeaconState withValidatorSetChangeSlot(long validatorSetChangeSlot) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix);
    }

    public BeaconState withLastStateRecalc(long lastStateRecalc) {
        return new BeaconState(lastStateRecalc, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix);
    }

    public BeaconState withCrosslinks(Crosslink[] crosslinks) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix);
    }

    public BeaconState withPendingAttestations(List<AttestationRecord> pendingAttestations) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix);
    }

    public BeaconState withFinality(long lastJustifiedSlot, long justifiedStreak, long lastFinalizedSlot) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, lastJustifiedSlot, justifiedStreak, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix);
    }

    public BeaconState increaseLastStateRecalc(long addition) {
        return withLastStateRecalc(lastStateRecalculationSlot + addition);
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
            if (record.getSlot() >= lastStateRecalculationSlot) {
                uptodateAttestations.add(record);
            }
        }
        return withPendingAttestations(uptodateAttestations);
    }

    public byte[] getHash() {
        return flatten().getHash();
    }

    public FlattenedState flatten() {
        return new FlattenedState(validatorSet.getHash(), lastStateRecalculationSlot, committees,
                lastJustifiedSlot, justifiedStreak, lastFinalizedSlot, crosslinks, nextShufflingSeed,
                validatorSetChangeSlot, randaoMix, recentBlockHashes, pendingAttestations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof BeaconState)) return false;

        return FastByteComparisons.equal(((BeaconState) o).getHash(), this.getHash());
    }
}
