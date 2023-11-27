package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetDpopProofNonceForResourceEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dpop_proof_claims"})
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("dpop_proof_claims");

		String nonce = env.getString("resource_server_dpop_nonce");

		if (!Strings.isNullOrEmpty(nonce)) {
			claims.addProperty("nonce", nonce);
			logSuccess("Added nonce to DPoP proof claims", args("DPoP nonce", nonce));
		} else {
			throw error("resource_server_dpop_nonce not found");
		}
		return env;
	}
}
