package org.ethereum.sharding.processing.state;

import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import java.util.Arrays;

import static org.ethereum.crypto.HashUtil.blake2b;
import static org.ethereum.sharding.processing.consensus.BeaconConstants.CYCLE_LENGTH;
import static org.ethereum.sharding.processing.consensus.BeaconConstants.MIN_ATTESTATION_INCLUSION_DELAY;
import static org.ethereum.util.ByteUtil.byteArrayToInt;
import static org.ethereum.util.ByteUtil.byteArrayToLong;
import static org.ethereum.util.ByteUtil.intToBytesNoLeadZeroes;
import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

/**
 * @author Mikhail Kalinin
 * @since 29.11.2018
 */
public class AttestationData {

    /* Slot number */
    private final long slot;
    /* Shard number */
    private final int shardId;
    /* Hash of the block we're signing */
    private final byte[] blockHash;
    /* Hash of the ancestor at the cycle boundary */
    private final byte[] cycleBoundaryHash;
    /* Shard block hash being attested to */
    private final byte[] shardBlockHash;
    /* Last crosslink hash */
    private final byte[] lastCrosslinkHash;
    /* Slot of last justified beacon block */
    private final long justifiedSlot;
    /* Hash of last justified beacon block */
    private final byte[] justifiedBlockHash;

    public AttestationData(long slot, int shardId, byte[] blockHash, byte[] cycleBoundaryHash,
                           byte[] shardBlockHash, byte[] lastCrosslinkHash, long justifiedSlot,
                           byte[] justifiedBlockHash) {
        this.slot = slot;
        this.shardId = shardId;
        this.blockHash = blockHash;
        this.cycleBoundaryHash = cycleBoundaryHash;
        this.shardBlockHash = shardBlockHash;
        this.lastCrosslinkHash = lastCrosslinkHash;
        this.justifiedSlot = justifiedSlot;
        this.justifiedBlockHash = justifiedBlockHash;
    }

    public AttestationData(byte[] encoded) {
        RLPList list = RLP.unwrapList(encoded);
        this.slot = byteArrayToLong(list.get(0).getRLPData());
        this.shardId = byteArrayToInt(list.get(1).getRLPData());
        this.blockHash = list.get(2).getRLPData();
        this.cycleBoundaryHash = list.get(3).getRLPData();
        this.shardBlockHash = list.get(4).getRLPData();
        this.lastCrosslinkHash = list.get(5).getRLPData();
        this.justifiedSlot = byteArrayToLong(list.get(6).getRLPData());
        this.justifiedBlockHash = list.get(7).getRLPData();
    }

    public long getSlot() {
        return slot;
    }

    public int getShardId() {
        return shardId;
    }

    public byte[] getBlockHash() {
        return blockHash;
    }

    public byte[] getCycleBoundaryHash() {
        return cycleBoundaryHash;
    }

    public byte[] getShardBlockHash() {
        return shardBlockHash;
    }

    public byte[] getLastCrosslinkHash() {
        return lastCrosslinkHash;
    }

    public long getJustifiedSlot() {
        return justifiedSlot;
    }

    public byte[] getJustifiedBlockHash() {
        return justifiedBlockHash;
    }

    public byte[] getEncoded() {
        return RLP.wrapList(longToBytesNoLeadZeroes(slot),
                intToBytesNoLeadZeroes(shardId),
                blockHash,
                cycleBoundaryHash,
                shardBlockHash,
                lastCrosslinkHash,
                longToBytesNoLeadZeroes(justifiedSlot),
                justifiedBlockHash);
    }

    public byte[] getHash() {
        return blake2b(getEncoded());
    }

    public boolean isAcceptableIn(long slot) {
        return this.slot >= Math.max(0L, slot - CYCLE_LENGTH + 1) &&
                this.slot <= Math.max(0L, slot - MIN_ATTESTATION_INCLUSION_DELAY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttestationRecord that = (AttestationRecord) o;
        return Arrays.equals(this.getEncoded(), that.getEncoded());
    }

    @Override
    public int hashCode() {
        return ByteUtil.byteArrayToInt(Arrays.copyOf(getHash(), Integer.BYTES));
    }
}
