package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetInvalidConnectIdTrustFrameworkInIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");
		JsonObject verifiedClaims = claims.getAsJsonObject("verified_claims");
		if (verifiedClaims == null) {
			verifiedClaims = new JsonObject();
			claims.add("verified_claims", verifiedClaims);
		}

		JsonObject verification = verifiedClaims.getAsJsonObject("verification");
		if (verification == null) {
			verification = new JsonObject();
			verifiedClaims.add("verification", verification);
		}

		String trustFramework = "unsupported_trust_framework";
		verification.addProperty("trust_framework", trustFramework);

		env.putObject("id_token_claims", claims);

		logSuccess("Set invalid verified_claims verification trust_framework in ID token claims",
			args("id_token_claims", claims, "trust_framework", trustFramework));

		return env;
	}

}
