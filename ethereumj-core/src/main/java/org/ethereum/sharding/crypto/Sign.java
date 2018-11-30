package org.ethereum.sharding.crypto;

import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import static org.ethereum.util.ByteUtil.bigIntegerToBytes;
import static org.ethereum.util.ByteUtil.bytesToBigInteger;

/**
 * Signature utilities
 * Signature should be implemented using BLS
 */
public interface Sign {

    /**
     * Sign the message
     */
    Signature sign(byte[] msg, BigInteger privateKey);

    /**
     * Verifies whether signature is made by signer with publicKey
     */
    boolean verify(Signature signature, byte[] msg, BigInteger publicKey);

    /**
     * Calculates public key from private
     */
    BigInteger privToPub(BigInteger privKey);

    /**
     * Aggregates several signatures in one
     */
    Signature aggSigns(List<Signature> signatures);

    /**
     * Aggregates public keys
     */
    BigInteger aggPubs(List<BigInteger> pubKeys);

    class Signature {
        public BigInteger r;
        public BigInteger s;

        public byte[] getEncoded() {
            return RLP.wrapList(
                    bigIntegerToBytes(r),
                    bigIntegerToBytes(s)
            );
        }

        public Signature() {
        }

        public Signature(byte[] encoded) {
            RLPList list = RLP.unwrapList(encoded);
            this.r = bytesToBigInteger(list.get(0).getRLPData());
            this.s = bytesToBigInteger(list.get(1).getRLPData());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Signature signature = (Signature) o;
            return Objects.equals(r, signature.r) &&
                    Objects.equals(s, signature.s);
        }

        @Override
        public String toString() {
            return "Signature{" +
                    "r=" + r +
                    ", s=" + s +
                    '}';
        }
    }
}
