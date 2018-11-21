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

/**
 * @author Mikhail Kalinin
 * @since 12.09.2018
 */
public class ValidatorState {

    /* Set of validators */
    private final ValidatorSet validatorSet;
    /** Committee members and their assigned shard, per slot */
    private final Committee[][] committees;
    /* Randao seed that will be used for next shuffling */
    private final byte[] nextShufflingSeed;
    /* Slot of last validator set change */
    private final long validatorSetChangeSlot;

    public ValidatorState(ValidatorSet validatorSet, Committee[][] committees, byte[] nextShufflingSeed,
                          long validatorSetChangeSlot) {
        this.validatorSet = validatorSet;
        this.committees = committees;
        this.nextShufflingSeed = nextShufflingSeed;
        this.validatorSetChangeSlot = validatorSetChangeSlot;
    }

    public ValidatorSet getValidatorSet() {
        return validatorSet;
    }

    public Committee[][] getCommittees() {
        return committees;
    }

    public int getCommitteesEndShard() {
        if (committees.length == 0)
            return 0;

        Committee[] endSlot = committees[committees.length - 1];
        if (endSlot.length == 0)
            return 0;

        return endSlot[endSlot.length - 1].getShardId();
    }

    public byte[] getNextShufflingSeed() {
        return nextShufflingSeed;
    }

    public long getValidatorSetChangeSlot() {
        return validatorSetChangeSlot;
    }

    public ValidatorState withValidatorSet(ValidatorSet validatorSet) {
        return new ValidatorState(validatorSet, committees, nextShufflingSeed, validatorSetChangeSlot);
    }

    public ValidatorState withCommittees(Committee[][] committees) {
        return new ValidatorState(validatorSet, committees, nextShufflingSeed, validatorSetChangeSlot);
    }

    public ValidatorState withStartSlot(long startSlot) {
        return new ValidatorState(validatorSet, committees, nextShufflingSeed, startSlot);
    }
}
