package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AustraliaConnectIdCheckClaimsSupported;
import net.openid.conformance.testmodule.Environment;

public class AustraliaConnectIdAddClaimsToIdTokenClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {
		JsonObject idTokenClaims = env.getObject("id_token_claims");

		for (String claim : AustraliaConnectIdCheckClaimsSupported.ConnectIdMandatoryToSupportClaims) {
			// In a real system these would come from the user's profile
			// For the conformance suite we just add some dummy data
			idTokenClaims.addProperty(claim, "ConnectID " + claim);
		}

		logSuccess("Added ConnectID claims to id_token_claims", args("id_token_claims", idTokenClaims));

		return env;
	}

}
