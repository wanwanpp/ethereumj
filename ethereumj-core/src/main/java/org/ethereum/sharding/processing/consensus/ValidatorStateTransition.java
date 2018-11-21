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
package org.ethereum.sharding.processing.consensus;

import org.ethereum.sharding.domain.Beacon;
import org.ethereum.sharding.processing.db.ValidatorSet;
import org.ethereum.sharding.processing.state.Committee;
import org.ethereum.sharding.processing.state.ValidatorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ethereum.sharding.processing.consensus.BeaconConstants.MIN_VALIDATOR_SET_CHANGE_INTERVAL;
import static org.ethereum.sharding.util.BeaconUtils.cycleStartSlot;

/**
 * @author Mikhail Kalinin
 * @since 12.09.2018
 */
public class ValidatorStateTransition implements StateTransition<ValidatorState> {

    private static final Logger logger = LoggerFactory.getLogger("beacon");

    StateTransition<ValidatorSet> validatorSetTransition;
    CommitteeFactory committeeFactory = new ShufflingCommitteeFactory();

    public ValidatorStateTransition(StateTransition<ValidatorSet> validatorSetTransition) {
        this.validatorSetTransition = validatorSetTransition;
    }

    @Override
    public ValidatorState applyBlock(Beacon block, ValidatorState to) {
        if (block.getSlotNumber() - to.getValidatorSetChangeSlot() < MIN_VALIDATOR_SET_CHANGE_INTERVAL)
            return to;

        logger.info("Calculate new validator state, slot: {}, prev slot: {}",
                block.getSlotNumber(), to.getValidatorSetChangeSlot());

        // validator set transition
        ValidatorSet validatorSet = validatorSetTransition.applyBlock(block, to.getValidatorSet());

        // committee transition
        int startShard = to.getCommitteesEndShard() + 1;
        int[] validators = validatorSet.getActiveIndices();

        // using parent hash to seed committees instead of hash of processing block,
        // the reason is that proposer does not know hash of newly created block before it applies that block to a state
        Committee[][] committees = committeeFactory.create(block.getParentHash(), validators, startShard);

        return to.withStartSlot(cycleStartSlot(block))
                .withValidatorSet(validatorSet)
                .withCommittees(committees);
    }
}
