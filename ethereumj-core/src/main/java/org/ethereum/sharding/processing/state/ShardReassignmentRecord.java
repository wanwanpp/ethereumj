package org.ethereum.sharding.processing.state;

/**
 * @author Mikhail Kalinin
 * @since 23.11.2018
 */
public class ShardReassignmentRecord {

    /* Which validator to reassign */
    private final int validatorIndex;
    /* To which shard */
    private final int shard;
    /* When */
    private final long slot;

    public ShardReassignmentRecord(int validatorIndex, int shard, long slot) {
        this.validatorIndex = validatorIndex;
        this.shard = shard;
        this.slot = slot;
    }

    public int getValidatorIndex() {
        return validatorIndex;
    }

    public int getShard() {
        return shard;
    }

    public long getSlot() {
        return slot;
    }
}
