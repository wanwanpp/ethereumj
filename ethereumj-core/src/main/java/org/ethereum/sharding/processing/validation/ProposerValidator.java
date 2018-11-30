package org.ethereum.sharding.processing.validation;

import org.ethereum.crypto.HashUtil;
import org.ethereum.sharding.crypto.Sign;
import org.ethereum.sharding.domain.Beacon;
import org.ethereum.sharding.domain.Validator;
import org.ethereum.sharding.processing.db.BeaconStore;
import org.ethereum.sharding.processing.state.BeaconState;
import org.ethereum.sharding.processing.state.ProposalSignedData;
import org.ethereum.sharding.processing.state.StateRepository;
import org.ethereum.sharding.util.BeaconUtils;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.FastByteComparisons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.ethereum.sharding.processing.consensus.BeaconConstants.BEACON_CHAIN_SHARD_ID;
import static org.ethereum.sharding.processing.validation.ValidationResult.InvalidProposerIndex;
import static org.ethereum.sharding.processing.validation.ValidationResult.InvalidProposerSignature;
import static org.ethereum.sharding.processing.validation.ValidationResult.InvalidRandaoReveal;
import static org.ethereum.sharding.processing.validation.ValidationResult.Success;

/**
 * @author Mikhail Kalinin
 * @since 30.11.2018
 */
public class ProposerValidator implements BeaconValidator {

    private static final Logger logger = LoggerFactory.getLogger("beacon");

    BeaconStore store;
    StateRepository repository;
    Sign sign;
    List<ValidationRule<Context>> rules;

    public ProposerValidator(BeaconStore store, StateRepository repository, Sign sign) {
        this.store = store;
        this.repository = repository;
        this.sign = sign;
        this.rules = Arrays.asList(
                ProposerIndexRule,
                ProposerSignatureRule,
                RandaoRevealRule
        );
    }

    /**
     * Checks whether block's slot has a proposal assignment
     */
    static final ValidationRule<Context> ProposerIndexRule = (block, ctx) -> {
        int proposerIdx = BeaconUtils.getProposerIndex(ctx.state.getCommittees(), block.getSlot());
        return proposerIdx >= 0 ? Success : InvalidProposerIndex;
    };

    /**
     * Basic proposer attestation validation:
     *
     * Attestation from the proposer of the block should be included
     * along with the block in the network message object
     */
    static final ValidationRule<Context> ProposerSignatureRule = (block, ctx) -> {
        Validator proposer = ctx.state.getProposerForSlot(block.getSlot());
        assert proposer != null;

        ProposalSignedData proposalData = new ProposalSignedData(block.getSlot(), BEACON_CHAIN_SHARD_ID,
                block.getHashWithoutSignature());

        if (!ctx.sign.verify(block.getProposerSignature(), proposalData.getHash(),
                ByteUtil.bytesToBigInteger(proposer.getPubKey()))) {
            return InvalidProposerSignature;
        }

        return Success;
    };

    /**
     * Checks validity of revealed RANDAO image
     */
    static final ValidationRule<Context> RandaoRevealRule = (block, ctx) -> {
        Validator proposer = ctx.state.getProposerForSlot(block.getSlot());
        assert proposer != null;

        int randaoSkips = ctx.parent.isGenesis() ? 1 : (int) (block.getSlot() - ctx.parent.getSlot());
        byte[] preImage = block.getRandaoReveal();
        for (int i = 0; i < randaoSkips; i++) {
            preImage = HashUtil.blake2b(preImage);
        }

        if (FastByteComparisons.equal(preImage, proposer.getRandao())) {
            return Success;
        } else {
            return InvalidRandaoReveal;
        }
    };

    @Override
    public ValidationResult validateAndLog(Beacon block) {
        Beacon parent = store.getByHash(block.getParentHash());
        assert parent != null;
        BeaconState state = repository.get(parent.getStateRoot());
        assert state != null;

        for (ValidationRule<Context> rule : rules) {
            ValidationResult res = rule.apply(block, new Context(parent, state, sign));
            if (res != Success) {
                logger.info("Process block {}, status: {}", block.toString(), res);
                return res;
            }
        }

        return Success;
    }

    class Context {
        Beacon parent;
        BeaconState state;
        Sign sign;

        public Context(Beacon parent, BeaconState state, Sign sign) {
            this.parent = parent;
            this.state = state;
            this.sign = sign;
        }
    }
}
