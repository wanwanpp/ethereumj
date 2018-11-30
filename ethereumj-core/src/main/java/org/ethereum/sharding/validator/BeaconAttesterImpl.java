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

import org.ethereum.sharding.config.ValidatorConfig;
import org.ethereum.sharding.processing.db.BeaconStore;
import org.ethereum.sharding.processing.state.AttestationRecord;
import org.ethereum.sharding.processing.state.AttestationData;
import org.ethereum.sharding.processing.state.StateRepository;
import org.ethereum.sharding.util.Bitfield;
import org.ethereum.sharding.crypto.Sign;
import org.ethereum.sharding.util.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collections;

/**
 * Default implementation of {@link BeaconAttester}.
 */
public class BeaconAttesterImpl implements BeaconAttester {

    private static final Logger logger = LoggerFactory.getLogger("attester");

    StateRepository repository;
    BeaconStore store;
    ValidatorConfig config;
    Sign sign;

    public BeaconAttesterImpl(StateRepository repository, BeaconStore store, ValidatorConfig config, Sign sign) {
        this.repository = repository;
        this.store = store;
        this.config = config;
        this.sign = sign;
    }

    @Override
    public AttestationRecord attestBlock(Input in, byte[] pubKey) {
        long justifiedSlot = in.state.getJustificationSource();
        AttestationData data = new AttestationData(
                in.slotNumber,
                in.index.getShardId(),
                in.block.getHash(),
                in.state.getCycleBoundaryHash(in.block.getSlotNumber()),
                HashUtils.ZERO_HASH32,
                HashUtils.ZERO_HASH32,
                justifiedSlot,
                in.state.getRecentBlockHashForSlot(justifiedSlot, in.block.getSlotNumber())
        );

        Sign.Signature aggSignature = sign.aggSigns(Collections.singletonList(
                sign.sign(data.getHash(), new BigInteger(pubKey))));

        AttestationRecord attestationRecord = new AttestationRecord(
                data,
                Bitfield.createEmpty(in.index.getCommitteeSize()).markVote(in.index.getValidatorIdx()),
                Bitfield.createEmpty(in.index.getCommitteeSize()),
                aggSignature
        );

        logger.info("Block {} attested by #{} in slot {} ", in.block, in.index.getValidatorIdx(), in.slotNumber);
        return attestationRecord;
    }
}
