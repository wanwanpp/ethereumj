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
package org.ethereum.sharding.processing.validation;

import org.ethereum.sharding.crypto.Sign;
import org.ethereum.sharding.domain.Beacon;
import org.ethereum.sharding.processing.consensus.BeaconConstants;
import org.ethereum.sharding.processing.db.BeaconStore;
import org.ethereum.sharding.processing.state.AttestationRecord;
import org.ethereum.sharding.processing.state.AttestationData;
import org.ethereum.sharding.processing.state.BeaconState;
import org.ethereum.sharding.processing.state.Committee;
import org.ethereum.sharding.processing.state.StateRepository;
import org.ethereum.sharding.util.Bitfield;
import org.ethereum.sharding.util.HashUtils;
import org.ethereum.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.ethereum.sharding.processing.consensus.BeaconConstants.MAX_ATTESTATION_COUNT;
import static org.ethereum.sharding.processing.validation.ValidationResult.InvalidAttestations;
import static org.ethereum.sharding.processing.validation.ValidationResult.Success;
import static org.ethereum.sharding.util.BeaconUtils.scanCommittees;
import static org.ethereum.util.FastByteComparisons.equal;

/**
 * Validates block attestations
 */
public class AttestationsValidator implements BeaconValidator {

    private static final Logger logger = LoggerFactory.getLogger("beacon");

    BeaconStore store;
    StateRepository repository;
    Sign sign;
    List<ValidationRule<Data>> rules;

    public AttestationsValidator(BeaconStore store, StateRepository repository, Sign sign) {
        this(store, repository, sign, new ArrayList<>());
        rules.add(MaxAttestationsRule);
        rules.add(CommonAttestationRule);
    }

    private AttestationsValidator(BeaconStore store, StateRepository repository,
                                  Sign sign, List<ValidationRule<Data>> rules) {
        this.store = store;
        this.repository = repository;
        this.sign = sign;
        this.rules = rules;
    }

    /**
     * Check against {@link BeaconConstants#MAX_ATTESTATION_COUNT}
     */
    static final ValidationRule<Data> MaxAttestationsRule = (block, data) ->
        block.getAttestations().size() > MAX_ATTESTATION_COUNT ? InvalidAttestations : Success;

    static final ValidationRule<Data> CommonAttestationRule = (block, data) -> {
        for (AttestationRecord attestation : block.getAttestations()) {
            AttestationData signedData = attestation.getData();

            // Is not acceptable in block slot
            if (!signedData.isAcceptableIn(block.getSlot())) {
                return InvalidAttestations;
            }

            // Incorrect justification
            if (signedData.getJustifiedSlot() != data.state.getJustificationSourceForSlot(signedData.getSlot())) {
                return InvalidAttestations;
            }

            // Hash of the block with justified slot does not equal to justifiedBlockHash
            byte[] justifiedBlockHash = data.state.getRecentBlockHashForSlot(
                    signedData.getJustifiedSlot(), block.getSlot());
            if (!equal(signedData.getJustifiedBlockHash(), justifiedBlockHash)) {
                return InvalidAttestations;
            }

            // FIXME remove in Phase 1
            if (!equal(signedData.getShardBlockHash(), HashUtils.ZERO_HASH32)) {
                return InvalidAttestations;
            }

            // Incorrect shard block hash
            byte[] shardBlockHash = data.state.getCrosslinks()[signedData.getShardId()].getHash();
            if (!equal(signedData.getLastCrosslinkHash(), shardBlockHash) &&
                    !equal(signedData.getShardBlockHash(), shardBlockHash)) {
                return InvalidAttestations;
            }

            int slotOffset = (int) (signedData.getSlot() - data.state.getValidatorSetChangeSlot());
            List<Committee.Index> attestationIndices = scanCommittees(
                    data.state.getCommittees(), slotOffset, signedData.getShardId());

            // Validate bitfield
            if (attestation.getAttesterBitfield().size() != Bitfield.calcLength(attestationIndices.size())) {
                return InvalidAttestations;
            }

            // Confirm that there were no votes of nonexistent attesters
            int lastBit = attestationIndices.size();
            for (int i = lastBit - 1; i < Bitfield.calcLength(attestationIndices.size()) * Byte.SIZE; ++i) {
                if (attestation.getAttesterBitfield().hasVoted(i)) {
                    return InvalidAttestations;
                }
            }

            // Validate aggregate signature
            List<BigInteger> pubKeys = new ArrayList<>();
            for (Committee.Index index : attestationIndices) {
                if (attestation.getAttesterBitfield().hasVoted(index.getValidatorIdx())) {
                    byte[] key = data.state.getValidatorSet().get(index.getValidatorIdx()).getPubKey();
                    pubKeys.add(ByteUtil.bytesToBigInteger(key));
                }
            }

            if (!data.sign.verify(attestation.getAggregateSig(), signedData.getHash(), data.sign.aggPubs(pubKeys))) {
                return InvalidAttestations;
            }
        }

        return Success;
    };

    public ValidationResult validateAndLog(Beacon block) {
        Beacon parent = store.getByHash(block.getParentHash());
        assert parent != null;
        BeaconState state = repository.get(parent.getStateRoot());
        assert state != null;

        for (ValidationRule<Data> rule : rules) {
            ValidationResult res = rule.apply(block, new Data(state, store, sign));
            if (res != Success) {
                logger.info("Process attestations validation in block {}, status: {}", block.toString(), res);
                return res;
            }
        }

        return Success;
    }

    static class Data {
        BeaconState state;
        Sign sign;
        BeaconStore store;

        public Data(BeaconState state, BeaconStore store, Sign sign) {
            this.state = state;
            this.store = store;
            this.sign = sign;
        }
    }
}
