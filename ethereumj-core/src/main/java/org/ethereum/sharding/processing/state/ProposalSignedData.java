package org.ethereum.sharding.processing.state;

import org.ethereum.sharding.processing.consensus.BeaconConstants;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import static org.ethereum.crypto.HashUtil.blake2b;
import static org.ethereum.util.ByteUtil.byteArrayToInt;
import static org.ethereum.util.ByteUtil.byteArrayToLong;
import static org.ethereum.util.ByteUtil.intToBytesNoLeadZeroes;
import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

/**
 * @author Mikhail Kalinin
 * @since 30.11.2018
 */
public class ProposalSignedData {

    /* Slot number */
    private final long slot;
    /** Shard number (or {@link BeaconConstants#BEACON_CHAIN_SHARD_ID} for beacon chain) */
    private final int shard;
    /* Hash of the block with empty proposerSignature */
    private final byte[] blockHash;

    public ProposalSignedData(long slot, int shard, byte[] blockHash) {
        this.slot = slot;
        this.shard = shard;
        this.blockHash = blockHash;
    }

    public ProposalSignedData(byte[] encoded) {
        RLPList items = RLP.unwrapList(encoded);
        this.slot = byteArrayToLong(items.get(0).getRLPData());
        this.shard = byteArrayToInt(items.get(1).getRLPData());
        this.blockHash = items.get(2).getRLPData();
    }

    public byte[] getEncoded() {
        return RLP.wrapList(
                longToBytesNoLeadZeroes(slot),
                intToBytesNoLeadZeroes(shard),
                blockHash
        );
    }

    public byte[] getHash() {
        return blake2b(getEncoded());
    }

    public long getSlot() {
        return slot;
    }

    public int getShard() {
        return shard;
    }

    public byte[] getBlockHash() {
        return blockHash;
    }
}
