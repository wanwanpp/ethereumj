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

import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import static org.ethereum.util.ByteUtil.byteArrayToLong;
import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

/**
 * @author Mikhail Kalinin
 * @since 12.09.2018
 */
public class Crosslink {

    /* Slot during which crosslink was added */
    private final long slot;
    /* The block hash */
    private byte[] hash;

    public Crosslink(long slot, byte[] hash) {
        this.slot = slot;
        this.hash = hash;
    }

    public Crosslink(byte[] encoded) {
        RLPList list = RLP.unwrapList(encoded);

        this.slot = byteArrayToLong(list.get(0).getRLPData());
        this.hash = list.get(1).getRLPData();
    }

    public byte[] getEncoded() {
        return RLP.wrapList(longToBytesNoLeadZeroes(slot), hash);
    }

    public long getSlot() {
        return slot;
    }

    public byte[] getHash() {
        return hash;
    }

    public static Crosslink[] empty(int shardCount) {
        Crosslink[] crosslinks = new Crosslink[shardCount];
        for (int i = 0; i < shardCount; i++)
            crosslinks[i] = empty();

        return crosslinks;
    }

    public static Crosslink empty() {
        return new Crosslink(0L, new byte[32]);
    }
}
