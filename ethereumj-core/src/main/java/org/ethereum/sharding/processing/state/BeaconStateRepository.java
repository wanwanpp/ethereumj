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

import org.ethereum.datasource.ObjectDataSource;
import org.ethereum.datasource.Source;
import org.ethereum.sharding.processing.db.BeaconStore;
import org.ethereum.sharding.processing.db.TrieValidatorSet;
import org.ethereum.sharding.processing.db.ValidatorSet;

/**
 * Default implementation of {@link StateRepository}.
 *
 * @author Mikhail Kalinin
 * @since 16.08.2018
 */
public class BeaconStateRepository implements StateRepository {

    Source<byte[], byte[]> src;
    ObjectDataSource<FlattenedState> stateDS;
    Source<byte[], byte[]> validatorSrc;
    Source<byte[], byte[]> validatorIndexSrc;

    public BeaconStateRepository(Source<byte[], byte[]> src, Source<byte[], byte[]> validatorSrc,
                                 Source<byte[], byte[]> validatorIndexSrc) {
        this.src = src;
        this.validatorSrc = validatorSrc;
        this.validatorIndexSrc = validatorIndexSrc;
        this.stateDS = new ObjectDataSource<>(src, FlattenedState.Serializer, BeaconStore.BLOCKS_IN_MEM);
    }

    @Override
    public void insert(BeaconState state) {
        stateDS.put(state.getHash(), state.flatten());
    }

    @Override
    public BeaconState get(byte[] hash) {
        FlattenedState flattened = stateDS.get(hash);
        if (flattened == null)
            return null;

        return fromFlattened(flattened);
    }

    @Override
    public BeaconState getEmpty() {
        return fromFlattened(FlattenedState.empty());
    }

    BeaconState fromFlattened(FlattenedState flattened) {
        ValidatorSet validatorSet = new TrieValidatorSet(validatorSrc, validatorIndexSrc,
                flattened.getValidatorSetHash());
        return new BeaconState(flattened.getLastStateRecalc(), validatorSet, flattened.getCommittees(),
                flattened.getNextShufflingSeed(), flattened.getValidatorSetChangeSlot(),
                flattened.getLastJustifiedSlot(), flattened.getJustifiedStreak(), flattened.getLastFinalizedSlot(),
                flattened.getCrosslinks(), flattened.getPendingAttestations(),
                flattened.getRecentBlockHashes(), flattened.getRandaoMix());
    }

    @Override
    public void commit() {
        stateDS.flush();
    }
}
