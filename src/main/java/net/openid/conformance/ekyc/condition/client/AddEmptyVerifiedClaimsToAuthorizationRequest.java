package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddEmptyVerifiedClaimsToAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("authorization_endpoint_request");
		if(!request.has("claims")) {
			request.add("claims", new JsonObject());
		}
		JsonObject claims = request.get("claims").getAsJsonObject();
		JsonObject verifiedClaims = new JsonObject();
		claims.add("verified_claims", verifiedClaims);
		JsonObject verification = new JsonObject();
		verifiedClaims.add("verification", verification);
		JsonObject claimsUnderVerifiedClaims = new JsonObject();
		verifiedClaims.add("claims", claimsUnderVerifiedClaims);
		logSuccess("Added empty verified claims element to authorization request", args("actual", request));
		return env;
	}
}
