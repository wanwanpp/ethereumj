package org.ethereum.sharding.processing.state;

import org.ethereum.datasource.Serializer;
import org.ethereum.sharding.processing.db.ValidatorSet;
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
 * Flattened {@link BeaconState}.
 *
 * <p>
 *     It's used in serialization purposes.
 *
 * @author Mikhail Kalinin
 * @since 23.11.2018
 */
public class FlattenedState {
    /* Hash of the validator set */
    private final byte[] validatorSetHash;
    /* Slot number that the state was calculated at */
    private final long lastStateRecalc;
    /**
     * Committee members and their assigned shard, per slot
     */
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
    /* Most recent 2 * CYCLE_LENGTH block hashes, older to newer */
    private final List<byte[]> recentBlockHashes;
    /* RANDAO state */
    private final byte[] randaoMix;
    /* Genesis time */
    private final long genesisTime;

    public FlattenedState(byte[] validatorSetHash, long lastStateRecalc, Committee[][] committees, long lastJustifiedSlot,
                          long justifiedStreak, long lastFinalizedSlot, Crosslink[] crosslinks,
                          byte[] nextShufflingSeed, long validatorSetChangeSlot, byte[] randaoMix,
                          List<byte[]> recentBlockHashes, List<AttestationRecord> pendingAttestations,
                          long genesisTime) {
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
        this.pendingAttestations = pendingAttestations;
        this.genesisTime = genesisTime;
    }

    public FlattenedState(byte[] encoded) {
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

        this.recentBlockHashes = new ArrayList<>();
        if (!isSingleZero(list.get(10).getRLPData())) {
            RLPList hashesList = RLP.unwrapList(list.get(10).getRLPData());
            for (RLPElement hashRlp : hashesList)
                recentBlockHashes.add(hashRlp.getRLPData());
        }

        this.randaoMix = list.get(11).getRLPData();

        this.genesisTime = byteArrayToLong(list.get(12).getRLPData());
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
                recentBlockHashes.size() > 0 ? RLP.wrapList(encodedHashes) : ZERO_BYTE_ARRAY,
                randaoMix, longToBytesNoLeadZeroes(genesisTime));
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

    public List<byte[]> getRecentBlockHashes() {
        return recentBlockHashes;
    }

    public byte[] getRandaoMix() {
        return randaoMix;
    }

    public long getGenesisTime() {
        return genesisTime;
    }

    public static org.ethereum.datasource.Serializer<FlattenedState, byte[]> getSerializer() {
        return Serializer;
    }

    public static FlattenedState empty() {
        List<byte[]> recentBlockHashes = new ArrayList<>();
        for (int i = 0; i < (CYCLE_LENGTH * 2); ++i) {
            recentBlockHashes.add(new byte[32]);
        }
        return new FlattenedState(ValidatorSet.EMPTY_HASH, 0, new Committee[0][], 0L, 0L, 0L,
                new Crosslink[0], new byte[32], 0L, new byte[32], recentBlockHashes, emptyList(), 0L);
    }

    public static final org.ethereum.datasource.Serializer<FlattenedState, byte[]> Serializer = new Serializer<FlattenedState, byte[]>() {
        @Override
        public byte[] serialize(FlattenedState state) {
            return state == null ? null : state.encode();
        }

        @Override
        public FlattenedState deserialize(byte[] stream) {
            return stream == null ? null : new FlattenedState(stream);
        }
    };
}
