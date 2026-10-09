package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * OID4VCI 1.0 section 12.2.4: only the presence of batch_credential_issuance "means that the issuer supports
 * more than one key proof in the proofs parameter". Without it the wallet should send one proof. Callers run
 * this as a WARNING: the specification states the issuer's capability, not a wallet MUST.
 */
public class VCIEnsureSingleProofWhenBatchIssuanceNotAdvertised extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"proof_jwts", "credential_issuer_metadata"})
	public Environment evaluate(Environment env) {
		if (env.getElementFromObject("credential_issuer_metadata", "batch_credential_issuance") != null) {
			log("The emulated issuer advertises batch_credential_issuance; several proofs are allowed");
			return env;
		}
		JsonArray items = env.getObject("proof_jwts").getAsJsonArray("items");
		if (items.size() > 1) {
			throw error("The wallet sent more than one proof although the credential issuer metadata does not advertise batch_credential_issuance",
				args("proofs_sent", items.size()));
		}
		logSuccess("The wallet sent a single proof", args("proofs_sent", items.size()));
		return env;
	}
}
