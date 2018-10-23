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
package org.ethereum.sharding.validator;

import org.ethereum.crypto.HashUtil;
import org.ethereum.sharding.config.ValidatorConfig;
import org.ethereum.sharding.domain.Beacon;
import org.ethereum.sharding.domain.Validator;
import org.ethereum.sharding.processing.db.BeaconStore;
import org.ethereum.sharding.pubsub.BeaconChainSynced;
import org.ethereum.sharding.processing.consensus.StateTransition;
import org.ethereum.sharding.processing.state.BeaconState;
import org.ethereum.sharding.processing.state.StateRepository;
import org.ethereum.sharding.util.Randao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * Default implementation of {@link BeaconProposer}.
 *
 * <p>
 *     <b>Note:</b> {@link #createNewBlock(Input, byte[])} must not be called prior to {@link BeaconChainSynced} event,
 *     handler of this event is used to finish proposer initialization
 *
 * @author Mikhail Kalinin
 * @since 28.08.2018
 */
public class BeaconProposerImpl implements BeaconProposer {

    private static final Logger logger = LoggerFactory.getLogger("proposer");

    Randao randao;
    StateTransition<BeaconState> stateTransition;
    StateRepository repository;
    ValidatorConfig config;
    BeaconStore store;
    BeaconAttester beaconAttester;

    public BeaconProposerImpl(Randao randao, StateRepository repository, BeaconStore store,
                              StateTransition<BeaconState> stateTransition, ValidatorConfig config,
                              BeaconAttester beaconAttester) {
        this.randao = randao;
        this.repository = repository;
        this.store = store;
        this.stateTransition = stateTransition;
        this.config = config;
        this.beaconAttester = beaconAttester;
    }

    byte[] randaoReveal(BeaconState state, byte[] pubKey) {
        if (!config.isEnabled()) {
            logger.error("Failed to reveal Randao: validator is disabled in the config");
            return new byte[] {};
        }

        Validator validator = state.getValidatorSet().getByPubKey(pubKey);
        if (validator == null) {
            logger.error("Failed to reveal Randao: validator does not exist in the set");
            return new byte[] {};
        }

        return randao.reveal(validator.getRandao());
    }

    @Override
    public Beacon createNewBlock(Input in, byte[] pubKey) {
        Beacon lastJustified = store.getCanonicalByNumber(in.state.getCrystallizedState().getFinality().getLastJustifiedSlot());
        Beacon block = new Beacon(in.parent.getHash(), randaoReveal(in.state, pubKey), in.mainChainRef,
                HashUtil.EMPTY_DATA_HASH, in.slotNumber, beaconAttester.getAttestations(in.slotNumber, lastJustified));
        BeaconState newState = stateTransition.applyBlock(block, in.state);
        block.setStateHash(newState.getHash());
        logger.info("New block created {}", block);
        return block;
    }
}