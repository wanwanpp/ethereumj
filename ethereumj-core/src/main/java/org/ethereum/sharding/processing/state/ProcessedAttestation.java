package org.ethereum.sharding.processing.state;

import org.ethereum.sharding.util.Bitfield;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.FastByteComparisons;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

/**
 * @author Mikhail Kalinin
 * @since 30.11.2018
 */
public class ProcessedAttestation {

    /* Signed data */
    private final AttestationData data;
    /* Attester participation bitfield */
    private final Bitfield attesterBitfield;
    /* Proof of custody bitfield */
    private final Bitfield pocBitfield;
    /* Slot in which it was included */
    private final long slotIncluded;

    public ProcessedAttestation(AttestationData data, Bitfield attesterBitfield,
                                Bitfield pocBitfield, long slotIncluded) {
        this.data = data;
        this.attesterBitfield = attesterBitfield;
        this.pocBitfield = pocBitfield;
        this.slotIncluded = slotIncluded;
    }

    public ProcessedAttestation(AttestationRecord record, long slotIncluded) {
        this.data = record.getData();
        this.attesterBitfield = record.getAttesterBitfield();
        this.pocBitfield = record.getPocBitfield();
        this.slotIncluded = slotIncluded;
    }

    public ProcessedAttestation(byte[] encoded) {
        RLPList list = RLP.unwrapList(encoded);

        this.data = new AttestationData(list.get(0).getRLPData());
        this.attesterBitfield = new Bitfield(list.get(1).getRLPData());
        this.pocBitfield = new Bitfield(list.get(2).getRLPData());
        this.slotIncluded = ByteUtil.byteArrayToLong(list.get(3).getRLPData());
    }

    public byte[] getEncoded() {
        return RLP.wrapList(
                data.getEncoded(),
                attesterBitfield.getData(),
                pocBitfield.getData(),
                ByteUtil.longToBytesNoLeadZeroes(slotIncluded));
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

    public long getSlotIncluded() {
        return slotIncluded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return FastByteComparisons.equal(this.getEncoded(), ((ProcessedAttestation) o).getEncoded());
    }
}
