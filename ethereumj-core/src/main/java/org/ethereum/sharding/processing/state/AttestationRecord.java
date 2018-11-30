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

import org.ethereum.sharding.crypto.Sign;
import org.ethereum.sharding.util.Bitfield;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import java.util.Arrays;

/**
 * Slot attestation data
 */
public class AttestationRecord {

    private final AttestationData data;
    /* Attester participation bitfield */
    private final Bitfield attesterBitfield;
    /* Proof of custody bitfield */
    private final Bitfield pocBitfield;
    /* BLS aggregate signature */
    private final Sign.Signature aggregateSig;

    public AttestationRecord(AttestationData data, Bitfield attesterBitfield,
                             Bitfield pocBitfield, Sign.Signature aggregateSig) {
        this.data = data;
        this.attesterBitfield = attesterBitfield;
        this.pocBitfield = pocBitfield;
        this.aggregateSig = aggregateSig;
    }

    public AttestationData getData() {
        return data;
    }

    public Bitfield getAttesterBitfield() {
        return attesterBitfield;
    }

    public Bitfield getPocBitfield() {
        return pocBitfield;
    }

    public Sign.Signature getAggregateSig() {
        return aggregateSig;
    }

    public byte[] getDataHash() {
        return data.getHash();
    }

    public AttestationRecord(byte[] encoded) {
        RLPList list = RLP.unwrapList(encoded);
        this.data = new AttestationData(list.get(0).getRLPData());
        this.attesterBitfield = new Bitfield(list.get(1).getRLPData());
        this.pocBitfield = new Bitfield(list.get(2).getRLPData());
        this.aggregateSig = new Sign.Signature(list.get(3).getRLPData());
    }

    public byte[] getEncoded() {
        return RLP.wrapList(
                data.getEncoded(),
                attesterBitfield.getData(),
                pocBitfield.getData(),
                aggregateSig.getEncoded());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttestationRecord that = (AttestationRecord) o;
        return Arrays.equals(this.getEncoded(), that.getEncoded());
    }
}
