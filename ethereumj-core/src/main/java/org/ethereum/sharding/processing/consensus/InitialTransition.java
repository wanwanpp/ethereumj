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
import org.ethereum.sharding.domain.Validator;
import org.ethereum.sharding.processing.db.ValidatorSet;
import org.ethereum.sharding.processing.state.BeaconState;
import org.ethereum.sharding.processing.state.Committee;
import org.ethereum.sharding.processing.state.Crosslink;
import org.ethereum.sharding.registration.ValidatorRepository;

import java.util.List;

import static org.ethereum.sharding.processing.consensus.BeaconConstants.SHARD_COUNT;

/**
 * @author Mikhail Kalinin
 * @since 04.09.2018
 */
public class InitialTransition implements StateTransition<BeaconState> {

    ValidatorRepository validatorRepository;
    StateTransition<ValidatorSet> validatorSetTransition = new ValidatorSetInitiator();
    CommitteeFactory committeeFactory = new ShufflingCommitteeFactory();
    byte[] mainChainRef;
    long genesisTimestamp = 1535454832L;

    public InitialTransition(ValidatorRepository validatorRepository) {
        this.validatorRepository = validatorRepository;
    }

    public InitialTransition withMainChainRef(byte[] mainChainRef) {
        this.mainChainRef = mainChainRef;
        return this;
    }

    @Override
    public BeaconState applyBlock(Beacon block, BeaconState to) {
        assert block.isGenesis();

        ValidatorSet validatorSet = validatorSetTransition.applyBlock(block, to.getValidatorSet());
        Committee[][] committees = committeeFactory.create(new byte[32],
                validatorSet.getActiveIndices(), 0);

        return to.withValidatorSet(validatorSet)
                .withCommittees(committees)
                .withLastStateRecalc(0L)
                .withCrosslinks(Crosslink.empty(SHARD_COUNT))
                .withGenesisTime(genesisTimestamp);
    }

    class ValidatorSetInitiator implements StateTransition<ValidatorSet> {

        @Override
        public ValidatorSet applyBlock(Beacon block, ValidatorSet set) {
            List<Validator> registered = validatorRepository.query(mainChainRef);
            registered.forEach(set::add);
            return set;
        }
    }
}
