package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Base for conditions comparing the number of credentials in the credential response with
 * the number of key proofs that were sent in the batch credential request.
 */
public abstract class AbstractVCIBatchCredentialCountCheck extends AbstractCondition {

	protected int getCredentialCount(Environment env) {
		JsonArray list = env.getObject("extracted_credentials").getAsJsonArray("list");
		return list.size();
	}

	protected int getRequestedProofCount(Environment env) {
		Integer requested = env.getInteger("vci_batch_requested_proof_count");
		if (requested == null) {
			throw error("'vci_batch_requested_proof_count' is missing - VCIPrepareBatchProofKeys must run first");
		}
		return requested;
	}
}
