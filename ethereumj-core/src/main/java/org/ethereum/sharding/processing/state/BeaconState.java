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

import org.ethereum.sharding.domain.Beacon;
import org.ethereum.sharding.processing.db.ValidatorSet;
import org.ethereum.sharding.util.BeaconUtils;
import org.ethereum.util.FastByteComparisons;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.max;
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

    /* Justification source */
    private final long justificationSource;
    /* Justification source of previous cycle */
    private final long prevCycleJustificationSource;
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
    private final long genesisTime;

    /* PoW chain reference */
    private final byte[] processedPoWReceiptRoot = ZERO_BYTE_ARRAY;
    private final List<CandidatePoWReceiptRootRecord> candidatePoWReceiptRoots = emptyList();

    /* Parameters relevant to hard forks / versioning. Should be updated only by hard forks. */
    private final long preForkVersion = 0;
    private final long postForkVersion = 0;
    private final long forkSlotNumber = 0;

    /* Attestations not yet processed */
    private final List<ProcessedAttestation> pendingAttestations;
    /* Recent beacon block hashes needed to process attestations, older to newer */
    private final List<byte[]> recentBlockHashes;

    /* RANDAO seed used for next shuffling */
    private final byte[] nextShufflingSeed;
    /* RANDAO state */
    private final byte[] randaoMix;

    public BeaconState(long lastStateRecalculationSlot, ValidatorSet validatorSet, Committee[][] committees,
                       byte[] nextShufflingSeed, long validatorSetChangeSlot, long justificationSource,
                       long prevCycleJustificationSource, long lastFinalizedSlot, Crosslink[] crosslinks,
                       List<ProcessedAttestation> pendingAttestations, List<byte[]> recentBlockHashes, byte[] randaoMix,
                       long genesisTime) {
        this.lastStateRecalculationSlot = lastStateRecalculationSlot;
        this.validatorSet = validatorSet;
        this.committees = committees;
        this.nextShufflingSeed = nextShufflingSeed;
        this.validatorSetChangeSlot = validatorSetChangeSlot;
        this.justificationSource = justificationSource;
        this.prevCycleJustificationSource = prevCycleJustificationSource;
        this.lastFinalizedSlot = lastFinalizedSlot;
        this.crosslinks = crosslinks;
        this.pendingAttestations = pendingAttestations;
        this.recentBlockHashes = recentBlockHashes;
        this.randaoMix = randaoMix;
        this.genesisTime = genesisTime;
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

    public long getJustificationSource() {
        return justificationSource;
    }

    public long getPrevCycleJustificationSource() {
        return prevCycleJustificationSource;
    }

    public long getLastFinalizedSlot() {
        return lastFinalizedSlot;
    }

    public Crosslink[] getCrosslinks() {
        return crosslinks;
    }

    public List<ProcessedAttestation> getPendingAttestations() {
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

    public long getGenesisTime() {
        return genesisTime;
    }

    public BeaconState withGenesisTime(long genesisTime) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, justificationSource, prevCycleJustificationSource, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix, genesisTime);
    }

    public BeaconState withValidatorSet(ValidatorSet validatorSet) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, justificationSource, prevCycleJustificationSource, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix, genesisTime);
    }

    public BeaconState withCommittees(Committee[][] committees) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, justificationSource, prevCycleJustificationSource, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix, genesisTime);
    }

    public BeaconState withValidatorSetChangeSlot(long validatorSetChangeSlot) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, justificationSource, prevCycleJustificationSource, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix, genesisTime);
    }

    public BeaconState withLastStateRecalc(long lastStateRecalc) {
        return new BeaconState(lastStateRecalc, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, justificationSource, prevCycleJustificationSource, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix, genesisTime);
    }

    public BeaconState withCrosslinks(Crosslink[] crosslinks) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, justificationSource, prevCycleJustificationSource, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix, genesisTime);
    }

    public BeaconState withPendingAttestations(List<ProcessedAttestation> pendingAttestations) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, justificationSource, prevCycleJustificationSource, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix, genesisTime);
    }

    public BeaconState withRecentBlockHashes(List<byte[]> recentBlockHashes) {
        return new BeaconState(lastStateRecalculationSlot, validatorSet, committees, nextShufflingSeed,
                validatorSetChangeSlot, justificationSource, prevCycleJustificationSource, lastFinalizedSlot,
                crosslinks, pendingAttestations, recentBlockHashes, randaoMix, genesisTime);
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

    public long getJustificationSourceForSlot(long slot) {
        return slot >= lastStateRecalculationSlot ? justificationSource : prevCycleJustificationSource;
    }

    public byte[] getRecentBlockHashForSlot(long slot, long currentBlockSlot) {
        assert slot < currentBlockSlot;

        long baseSlot = currentBlockSlot - recentBlockHashes.size();
        int idx = (int) (slot - baseSlot);
        assert idx >= 0;
        return recentBlockHashes.get(idx);
    }

    public byte[] getCycleBoundaryHash(long currentBlockSlot) {
        long cycleSlot = BeaconUtils.cycleStartSlot(currentBlockSlot);
        return getRecentBlockHashForSlot(cycleSlot, currentBlockSlot);
    }

    /**
     * Adds new attestations to the end of the list
     * Produces new instance keeping immutability
     *
     * @param block block with attestations to add
     * @return updated ActiveState
     */
    public BeaconState addPendingAttestationsFromBlock(Beacon block) {
        List<ProcessedAttestation> updatedAttestations = new ArrayList<>(pendingAttestations);
        block.getAttestations().forEach(record ->
                updatedAttestations.add(new ProcessedAttestation(record, block.getSlot())));
        return withPendingAttestations(updatedAttestations);
    }

    public BeaconState removeOutdatedAttestations() {
        List<ProcessedAttestation> uptodateAttestations = new ArrayList<>();
        for (ProcessedAttestation record : pendingAttestations) {
            if (record.getData().getSlot() >= lastStateRecalculationSlot) {
                uptodateAttestations.add(record);
            }
        }
        return withPendingAttestations(uptodateAttestations);
    }

    public BeaconState appendRecentBlockHashes(Beacon block, long parentSlot) {
        // no updates if parent is genesis
        if (parentSlot == 0)
            return this;

        List<byte[]> recentBlockHashes = new ArrayList<>(this.recentBlockHashes);
        for (long i = parentSlot; i < block.getSlot(); i++) {
            recentBlockHashes.add(block.getHash());
        }
        return withRecentBlockHashes(recentBlockHashes);
    }

    public BeaconState trimRecentBlockHashes(int startIdx) {
        List<byte[]> recentBlockHashes = new ArrayList<>();
        for (int i = startIdx; i < this.recentBlockHashes.size(); i++) {
            recentBlockHashes.add(this.recentBlockHashes.get(i));
        }
        return withRecentBlockHashes(recentBlockHashes);
    }

    public byte[] getHash() {
        return flatten().getHash();
    }

    public FlattenedState flatten() {
        return new FlattenedState(validatorSet.getHash(), lastStateRecalculationSlot, committees,
                justificationSource, prevCycleJustificationSource, lastFinalizedSlot, crosslinks, nextShufflingSeed,
                validatorSetChangeSlot, randaoMix, recentBlockHashes, pendingAttestations, genesisTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return FastByteComparisons.equal(((BeaconState) o).getHash(), this.getHash());
    }
}
