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
import org.ethereum.sharding.processing.db.BeaconStore;
import org.ethereum.sharding.processing.db.ValidatorSet;
import org.ethereum.sharding.processing.state.BeaconState;
import org.ethereum.sharding.processing.state.Committee;
import org.ethereum.sharding.pubsub.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ethereum.sharding.processing.consensus.BeaconConstants.CYCLE_LENGTH;
import static org.ethereum.sharding.processing.consensus.BeaconConstants.MIN_VALIDATOR_SET_CHANGE_INTERVAL;
import static org.ethereum.sharding.pubsub.Events.onBeaconAttestationIncluded;
import static org.ethereum.sharding.pubsub.Events.onStateRecalc;
import static org.ethereum.sharding.util.BeaconUtils.cycleStartSlot;

/**
 * @author Mikhail Kalinin
 * @since 12.09.2018
 */
public class BeaconStateTransition implements StateTransition<BeaconState> {

    private static final Logger logger = LoggerFactory.getLogger("beacon");

    Publisher publisher;
    BeaconStore store;
    CommitteeFactory committeeFactory = new ShufflingCommitteeFactory();

    public BeaconStateTransition(Publisher publisher) {
        this.publisher = publisher;
    }

    public BeaconStateTransition() {
    }

    @Override
    public BeaconState applyBlock(Beacon block, BeaconState to) {
        BeaconState ret = to;
        Beacon parent = store.getByHash(block.getParentHash());

        // update recent block hashes
        ret = ret.appendRecentBlockHashes(block, parent.getSlotNumber());

        ret = ret.addPendingAttestationsFromBlock(block);
        block.getAttestations().forEach(at -> publisher.publish(onBeaconAttestationIncluded(at)));

        if (block.getSlotNumber() - ret.getLastStateRecalculationSlot() >= CYCLE_LENGTH) {
            logger.info("Process cycle transition, slot: {}, prev slot: {}",
                    block.getSlotNumber(), to.getLastStateRecalculationSlot());

            // FIXME add finality transition

            if (block.getSlotNumber() - to.getValidatorSetChangeSlot() >= MIN_VALIDATOR_SET_CHANGE_INTERVAL) {

                logger.info("Change validator set, slot: {}, prev slot: {}",
                        block.getSlotNumber(), to.getValidatorSetChangeSlot());

                // FIXME onboard new validators from PoW chain
                ValidatorSet validatorSet = to.getValidatorSet();

                // committee transition
                int startShard = to.getCommitteesEndShard() + 1;
                int[] validators = validatorSet.getActiveIndices();

                // using parent hash to seed committees instead of hash of processing block,
                // the reason is that proposer does not know hash of newly created block before it applies that block to a state
                Committee[][] committees = committeeFactory.create(block.getParentHash(), validators, startShard);

                ret = ret.withValidatorSetChangeSlot(cycleStartSlot(block))
                        .withValidatorSet(validatorSet)
                        .withCommittees(committees);
            }

            ret = ret.withLastStateRecalc(cycleStartSlot(block));

            if (publisher != null) {
                publisher.publish(onStateRecalc(ret.getLastStateRecalculationSlot()));
            }

            // remove attestations older than last_state_recalc
            ret = ret.removeOutdatedAttestations();

            // trim recent block hashes
            ret = ret.trimRecentBlockHashes(CYCLE_LENGTH);
        }

        return ret;
    }
}
