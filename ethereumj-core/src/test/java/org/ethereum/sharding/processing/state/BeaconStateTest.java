package org.ethereum.sharding.processing.state;

import org.ethereum.crypto.HashUtil;
import org.ethereum.datasource.inmem.HashMapDB;
import org.ethereum.sharding.processing.consensus.BeaconConstants;
import org.ethereum.sharding.processing.db.TrieValidatorSet;
import org.ethereum.sharding.processing.db.ValidatorSet;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static java.lang.Math.abs;
import static org.ethereum.sharding.processing.consensus.BeaconConstants.CYCLE_LENGTH;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Mikhail Kalinin
 * @since 21.09.2018
 */
public class BeaconStateTest {

    @Test
    public void testSerialize() {
        BeaconState expected = getRandomState();

        BeaconState actual = fromEncoded(expected.flatten().encode());
        assertStateEquals(expected, actual);

        // with empty crosslinks and committees
        expected = getRandomState();
        expected.withValidatorSet(expected.getValidatorSet())
                .withCommittees(new Committee[0][])
                .withCrosslinks(new Crosslink[0]);
        actual = fromEncoded(expected.flatten().encode());

        assertStateEquals(expected, actual);

        // with empty slots in committee
        expected = getRandomState();
        Committee[][] committees = expected.getCommittees();
        committees[0] = committees[committees.length / 2] = committees[committees.length - 1] = new Committee[0];
        expected = expected.withCommittees(committees);

        actual = fromEncoded(expected.flatten().encode());
        assertStateEquals(expected, actual);
    }

    BeaconState fromEncoded(byte[] encoded) {
        FlattenedState flattened = new FlattenedState(encoded);
        ValidatorSet validatorSet = new TrieValidatorSet(new HashMapDB<>(), new HashMapDB<>(),
                flattened.getValidatorSetHash());

        return new BeaconState(flattened.getLastStateRecalc(), validatorSet, flattened.getCommittees(),
                flattened.getNextShufflingSeed(), flattened.getValidatorSetChangeSlot(),
                flattened.getLastJustifiedSlot(), flattened.getJustifiedStreak(), flattened.getLastFinalizedSlot(),
                flattened.getCrosslinks(), flattened.getPendingAttestations(),
                flattened.getRecentBlockHashes(), flattened.getRandaoMix());
    }

    BeaconState getRandomState() {
        Random rnd = new Random();

        ValidatorSet validatorSet = new TrieValidatorSet(new HashMapDB<>(), new HashMapDB<>());
        Committee[][] committees = new Committee[abs(rnd.nextInt()) % 1000 + 1][];
        for (int i = 0; i < committees.length; i++) {
            Committee[] slot = new Committee[abs(rnd.nextInt()) % 18 + 1];
            committees[i] = slot;
            for (int j = 0; j < slot.length; j++) {
                int[] validators = IntStream.range(1, abs(rnd.nextInt()) % (2 * BeaconConstants.MIN_COMMITTEE_SIZE - 2) + 2)
                        .toArray();
                slot[j] = new Committee((short) (abs(rnd.nextInt()) % BeaconConstants.SHARD_COUNT), validators);
            }
        }

        Crosslink[] links = new Crosslink[BeaconConstants.SHARD_COUNT];
        for (int i = 0; i < links.length; i++) {
            links[i] = new Crosslink(abs(rnd.nextInt()) % 1000 + 1, HashUtil.randomHash());
        }

        List<byte[]> recentBlockHashes = new ArrayList<>();
        for (int i = 0; i < (CYCLE_LENGTH * 2); ++i) {
            recentBlockHashes.add(HashUtil.randomHash());
        }

        return new BeaconState(abs(rnd.nextInt()), validatorSet, committees,
                HashUtil.randomHash(), abs(rnd.nextInt()) % 100 + 1,
                abs(rnd.nextInt()) % 100 + 1, abs(rnd.nextInt()) % 100 + 1, abs(rnd.nextInt()) % 100 + 1, links,
                Collections.emptyList(), recentBlockHashes, HashUtil.randomHash());
    }
    
    void assertStateEquals(BeaconState expected, BeaconState actual) {
        assertEquals(expected.getLastStateRecalculationSlot(), actual.getLastStateRecalculationSlot());
        assertEquals(expected.getCrosslinks().length, actual.getCrosslinks().length);
        for (int i = 0; i < expected.getCrosslinks().length; i++) {
            Crosslink e = expected.getCrosslinks()[i]; Crosslink a = actual.getCrosslinks()[i];
            assertEquals(e.getSlot(), a.getSlot());
            assertArrayEquals(e.getHash(), a.getHash());
        }

        assertEquals(expected.getJustifiedStreak(), actual.getJustifiedStreak());
        assertEquals(expected.getLastJustifiedSlot(), actual.getLastJustifiedSlot());
        assertEquals(expected.getLastFinalizedSlot(), actual.getLastFinalizedSlot());

        assertArrayEquals(expected.getValidatorSet().getHash(),
                actual.getValidatorSet().getHash());
        assertArrayEquals(expected.getNextShufflingSeed(), actual.getNextShufflingSeed());
        assertEquals(expected.getValidatorSetChangeSlot(), actual.getValidatorSetChangeSlot());

        assertEquals(expected.getCommittees().length, actual.getCommittees().length);
        for (int i = 0; i < expected.getCommittees().length; i++) {
            for (int j = 0; j < expected.getCommittees()[i].length; j++) {
                assertEquals(expected.getCommittees()[i][j].getShardId(),
                        actual.getCommittees()[i][j].getShardId());
                assertArrayEquals(expected.getCommittees()[i][j].getValidators(),
                        actual.getCommittees()[i][j].getValidators());
            }
        }

        assertArrayEquals(expected.getRandaoMix(), actual.getRandaoMix());

        assertEquals(expected.getRecentBlockHashes().size(), actual.getRecentBlockHashes().size());
        for (int i = 0; i < expected.getRecentBlockHashes().size(); i++) {
            assertArrayEquals(expected.getRecentBlockHashes().get(i), actual.getRecentBlockHashes().get(i));
        }

        for (int i = 0; i < expected.getPendingAttestations().size(); i++) {
            assertArrayEquals(expected.getPendingAttestations().get(i).getEncoded(),
                    actual.getPendingAttestations().get(i).getEncoded());
        }
    }
}
