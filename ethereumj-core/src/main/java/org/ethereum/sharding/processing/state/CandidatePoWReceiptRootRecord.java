package org.ethereum.sharding.processing.state;

/**
 * Stores PoW receipt root with given votes number.
 *
 * @author Mikhail Kalinin
 * @since 26.11.2018
 */
public class CandidatePoWReceiptRootRecord {

    /* Candidate PoW receipt root */
    private final byte[] candidatePoWReceiptRoot;
    /* Vote count */
    private final int votes;

    public CandidatePoWReceiptRootRecord(byte[] candidatePoWReceiptRoot, int votes) {
        this.candidatePoWReceiptRoot = candidatePoWReceiptRoot;
        this.votes = votes;
    }

    public byte[] getCandidatePoWReceiptRoot() {
        return candidatePoWReceiptRoot;
    }

    public int getVotes() {
        return votes;
    }
}
