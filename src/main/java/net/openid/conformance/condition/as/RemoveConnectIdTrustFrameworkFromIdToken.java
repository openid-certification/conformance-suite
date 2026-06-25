package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveConnectIdTrustFrameworkFromIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");
		JsonObject verifiedClaims = claims.getAsJsonObject("verified_claims");

		if (verifiedClaims != null) {
			JsonObject verification = verifiedClaims.getAsJsonObject("verification");
			if (verification != null) {
				verification.remove("trust_framework");
			}
		}

		env.putObject("id_token_claims", claims);

		logSuccess("Removed verified_claims verification trust_framework from ID token claims",
			args("id_token_claims", claims));

		return env;
	}

}
