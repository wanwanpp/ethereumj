package org.ethereum.sharding.processing.consensus;

import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.sharding.domain.Beacon;
import org.ethereum.sharding.domain.Validator;
import org.ethereum.sharding.processing.db.TrieValidatorSet;
import org.ethereum.sharding.processing.db.ValidatorSet;
import org.ethereum.sharding.processing.state.ActiveState;
import org.ethereum.sharding.processing.state.BeaconState;
import org.ethereum.sharding.processing.state.Committee;
import org.ethereum.sharding.processing.state.Crosslink;
import org.ethereum.sharding.processing.state.CrystallizedState;
import org.ethereum.sharding.processing.state.ValidatorState;
import org.ethereum.sharding.processing.state.Finality;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.ethereum.crypto.HashUtil.randomHash;
import static org.junit.Assert.assertEquals;

/**
 * @author Mikhail Kalinin
 * @since 14.09.2018
 */
public class StateTransitionTest {

    @Test
    public void testBasics() {
        Validator validator = getRandomValidator();

        StateTransition<BeaconState> transitionFunction = new BeaconStateTransition(
                validatorStateTransition(validatorTransition(validator)), finalityTransition());

        Beacon b1 = new Beacon(new byte[32], new byte[32], new byte[32], new byte[32], 1L, new ArrayList<>());
        Beacon b2 = new Beacon(new byte[32], new byte[32], new byte[32], new byte[32], 63L, new ArrayList<>());
        Beacon b3 = new Beacon(new byte[32], new byte[32], new byte[32], new byte[32], 64L, new ArrayList<>());
        Beacon b4 = new Beacon(new byte[32], new byte[32], new byte[32], new byte[32], 72L, new ArrayList<>());

        assertEquals(getOrigin(), transitionFunction.applyBlock(b1, getOrigin()));
        assertEquals(getOrigin(), transitionFunction.applyBlock(b2, getOrigin()));
        assertEquals(getExpected(b3, validator), transitionFunction.applyBlock(b3, getOrigin()));
        assertEquals(getExpected(b4, validator), transitionFunction.applyBlock(b4, getOrigin()));
    }

    StateTransition<ValidatorState> validatorStateTransition(StateTransition<ValidatorSet> validatorTransition) {
        return (block, to) -> to.withValidatorSet(validatorTransition.applyBlock(block, to.getValidatorSet()));
    }

    StateTransition<Finality> finalityTransition() {
        return (block, to) -> new Finality(10L, 10L, 10L);
    }

    StateTransition<ValidatorSet> validatorTransition(Validator validator) {
        return (block, to) -> {
            to.add(validator);
            return to;
        };
    }

    BeaconState getExpected(Beacon block, Validator validator) {
        ValidatorSet validatorSet = validatorTransition(validator).applyBlock(block,
                new TrieValidatorSet(new HashMapDB<>(), new HashMapDB<byte[]>()));
        ValidatorState validatorState = validatorStateTransition(validatorTransition(validator))
                .applyBlock(block, getOrigin().getCrystallizedState().getValidatorState())
                .withValidatorSet(validatorSet);
        Finality finality = finalityTransition().applyBlock(block, getOrigin().getCrystallizedState().getFinality());

        CrystallizedState crystallized = new CrystallizedState(
                getOrigin().getCrystallizedState().getLastStateRecalc() + BeaconConstants.CYCLE_LENGTH,
                validatorState, finality, new Crosslink[0]
        );

        return new BeaconState(crystallized, getOrigin().getActiveState());
    }

    BeaconState getOrigin() {
        ValidatorSet validatorSet = new TrieValidatorSet(new HashMapDB<>(), new HashMapDB<>());
        ValidatorState validatorState = new ValidatorState(validatorSet, new Committee[0][], new byte[32], 0L);
        Finality finality = Finality.empty();

        CrystallizedState crystallized = new CrystallizedState(0L, validatorState, finality, new Crosslink[0]);
        return new BeaconState(crystallized, ActiveState.createEmpty());
    }

    Validator getRandomValidator() {
        long shardId = new Random().nextInt();
        shardId = (shardId < 0 ? (-shardId) : shardId) % 1024;
        return new Validator(randomHash(), shardId,
                HashUtil.sha3omit12(randomHash()), randomHash());
    }
}
