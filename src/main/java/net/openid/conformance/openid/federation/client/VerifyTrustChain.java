package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.federation.TrustChainVerifier;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VerifyTrustChain extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"trust_chain"})
	public Environment evaluate(Environment env) {
		String trustChainSubject = env.getString("trust_chain", "subject");
		String trustChainTrustAnchor = env.getString("trust_chain", "trust_anchor");
		JsonArray trustChain = env.getElementFromObject("trust_chain", "trust_chain").getAsJsonArray();

		TrustChainVerifier.VerificationResult result =
			TrustChainVerifier.verifyTrustChain(trustChainSubject, trustChainTrustAnchor, OIDFJSON.convertJsonArrayToList(trustChain));

		if (!result.isVerified()) {
			throw error("Could not verify the trust chain from the sub %s to trust anchor %s".formatted(trustChainSubject, trustChainTrustAnchor),
				args("error", result.getError(), "trust_chain", trustChain));
		}

		logSuccess("Trust chain verified successfully", args("trust_chain", trustChain));

		return env;
	}
}
