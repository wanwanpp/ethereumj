package org.ethereum.sharding.processing.consensus;

import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.sharding.domain.Beacon;
import org.ethereum.sharding.domain.Validator;
import org.ethereum.sharding.processing.db.TrieValidatorSet;
import org.ethereum.sharding.processing.db.ValidatorSet;
import org.ethereum.sharding.processing.state.BeaconState;
import org.ethereum.sharding.processing.state.Committee;
import org.ethereum.sharding.processing.state.Crosslink;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;

import static java.util.Collections.emptyList;
import static org.ethereum.crypto.HashUtil.randomHash;
import static org.ethereum.sharding.processing.consensus.BeaconConstants.CYCLE_LENGTH;
import static org.junit.Assert.assertEquals;

/**
 * @author Mikhail Kalinin
 * @since 14.09.2018
 */
public class StateTransitionTest {

    @Test
    public void testBasics() {
        Validator validator = getRandomValidator();

        StateTransition<BeaconState> transitionFunction = new BeaconStateTransition() {
            @Override
            public BeaconState applyBlock(Beacon block, BeaconState to) {
                BeaconState ret = to;
                if (block.getSlotNumber() - ret.getLastStateRecalc() >= CYCLE_LENGTH) {
                    ret = finalityTransition().applyBlock(block, ret);
                    ValidatorSet validators = validatorTransition(validator).applyBlock(block, ret.getValidatorSet());
                    ret = ret.withValidatorSet(validators);
                }
                return super.applyBlock(block, ret);
            }
        };

        Beacon b1 = new Beacon(new byte[32], new byte[32], new byte[32], new byte[32], 1L, new ArrayList<>());
        Beacon b2 = new Beacon(new byte[32], new byte[32], new byte[32], new byte[32], 63L, new ArrayList<>());
        Beacon b3 = new Beacon(new byte[32], new byte[32], new byte[32], new byte[32], 64L, new ArrayList<>());
        Beacon b4 = new Beacon(new byte[32], new byte[32], new byte[32], new byte[32], 72L, new ArrayList<>());

        assertEquals(getOrigin(), transitionFunction.applyBlock(b1, getOrigin()));
        assertEquals(getOrigin(), transitionFunction.applyBlock(b2, getOrigin()));
        BeaconState expected = getExpected(b3, validator), actual = transitionFunction.applyBlock(b3, getOrigin());
        assertEquals(getExpected(b3, validator), transitionFunction.applyBlock(b3, getOrigin()));
        assertEquals(getExpected(b4, validator), transitionFunction.applyBlock(b4, getOrigin()));
    }

    StateTransition<BeaconState> finalityTransition() {
        return (block, to) -> to.withFinality(10L, 10L, 10L);
    }

    StateTransition<ValidatorSet> validatorTransition(Validator validator) {
        return (block, to) -> {
            to.add(validator);
            return to;
        };
    }

    BeaconState getExpected(Beacon block, Validator validator) {
        ValidatorSet validatorSet = validatorTransition(validator).applyBlock(block,
                new TrieValidatorSet(new HashMapDB<>(), new HashMapDB<>()));
        BeaconState ret = getOrigin().withValidatorSet(validatorSet);
        ret = finalityTransition().applyBlock(block, ret);
        return ret.increaseLastStateRecalc(BeaconConstants.CYCLE_LENGTH);
    }

    BeaconState getOrigin() {
        ValidatorSet validatorSet = new TrieValidatorSet(new HashMapDB<>(), new HashMapDB<>());
        return new BeaconState(0L, validatorSet, new Committee[0][], new byte[32], 0L, 0L, 0L, 0L,
                new Crosslink[0], emptyList(), emptyList(), emptyList(), new byte[32]);
    }

    Validator getRandomValidator() {
        long shardId = new Random().nextInt();
        shardId = (shardId < 0 ? (-shardId) : shardId) % 1024;
        return new Validator(randomHash(), shardId,
                HashUtil.sha3omit12(randomHash()), randomHash());
    }
}
