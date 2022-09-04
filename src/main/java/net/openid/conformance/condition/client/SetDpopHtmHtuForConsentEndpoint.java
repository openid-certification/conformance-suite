package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetDpopHtmHtuForConsentEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "dpop_proof_claims"})
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("dpop_proof_claims");

		String consentUrl = env.getString("config", "resource.consentUrl");

		String resourceMethod = "POST";

		claims.addProperty("htm", resourceMethod);
		claims.addProperty("htu", consentUrl);

		logSuccess("Added htm/htu to DPoP proof claims", claims);

		return env;

	}
}
