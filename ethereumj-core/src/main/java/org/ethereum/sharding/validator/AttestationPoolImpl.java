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

import org.ethereum.sharding.crypto.Sign;
import org.ethereum.sharding.domain.Beacon;
import org.ethereum.sharding.processing.db.BeaconStore;
import org.ethereum.sharding.processing.state.AttestationData;
import org.ethereum.sharding.processing.state.AttestationRecord;
import org.ethereum.sharding.pubsub.BeaconAttestationIncluded;
import org.ethereum.sharding.pubsub.BeaconBlockAttested;
import org.ethereum.sharding.pubsub.Publisher;
import org.ethereum.sharding.pubsub.StateRecalc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ethereum.sharding.processing.consensus.BeaconConstants.CYCLE_LENGTH;
import static org.ethereum.sharding.util.Bitfield.orBitfield;

/**
 * Default implementation of {@link AttestationPool}.
 */
public class AttestationPoolImpl implements AttestationPool {

    // Single attestations
    private final Map<AttestationData, Set<AttestationRecord>> attestations = new HashMap<>();

    BeaconStore store;
    Sign sign;
    Publisher publisher;

    public AttestationPoolImpl(BeaconStore store, Sign sign, Publisher publisher) {
        this.store = store;
        this.sign = sign;
        this.publisher = publisher;
        init();
    }

    private void init() {
        publisher.subscribe(BeaconBlockAttested.class, (data) ->
            addSingleAttestation(data.getAttestationRecord()));

        publisher.subscribe(BeaconAttestationIncluded.class, event ->
            purgeAttestations(event.getAttestationRecord()));

        // FIXME further replace with last finalized slot
        publisher.subscribe(StateRecalc.class, event ->
                removeOldSlots(event.getSlot() - CYCLE_LENGTH));
    }

    @Override
    public synchronized List<AttestationRecord> getAttestations(Long currentSlot, Beacon lastJustified) {
        List<AttestationRecord> res = new ArrayList<>();
        attestations.entrySet().forEach(e -> {
            AttestationData data = e.getKey();
            Set<AttestationRecord> singles = e.getValue();
            if (data.isAcceptableIn(currentSlot)) {
                res.add(new AttestationRecord(
                        e.getKey(),
                        orBitfield(singles.stream().map(AttestationRecord::getAttesterBitfield).collect(Collectors.toList())),
                        orBitfield(singles.stream().map(AttestationRecord::getPocBitfield).collect(Collectors.toList())),
                        sign.aggSigns(singles.stream().map(AttestationRecord::getAggregateSig).collect(Collectors.toList()))
                ));
            }
        });

        return res;
    }

    @Override
    public synchronized void addSingleAttestation(AttestationRecord attestationRecord) {
        if (attestationRecord.getAttesterBitfield().calcVotes() != 1) {
            throw new RuntimeException("Accepts only unmerged attestations");
        }

        Set<AttestationRecord> setByHash = attestations.getOrDefault(attestationRecord.getData(), new HashSet<>());
        setByHash.add(attestationRecord);
    }

    @Override
    public synchronized void purgeAttestations(AttestationRecord attestationRecord) {
        Set<AttestationRecord> singleAttestations = attestations.get(attestationRecord.getData());
        singleAttestations.forEach(record -> {
            if (orBitfield(attestationRecord.getAttesterBitfield(), record.getAttesterBitfield()) ==
                    attestationRecord.getAttesterBitfield()) {
                singleAttestations.remove(record);
                if (singleAttestations.isEmpty()) {
                    attestations.remove(record.getDataHash());
                }
            }
        });
    }

    @Override
    public synchronized void removeOldSlots(long beforeSlot) {
        Iterator<AttestationData> iter = attestations.keySet().iterator();
        while (iter.hasNext()) {
            if (iter.next().getSlot() < beforeSlot) {
                iter.remove();
            }
        }
    }
}
