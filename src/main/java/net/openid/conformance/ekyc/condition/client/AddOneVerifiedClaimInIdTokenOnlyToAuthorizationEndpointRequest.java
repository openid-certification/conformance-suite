package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddOneVerifiedClaimInIdTokenOnlyToAuthorizationEndpointRequest extends AbstractAddVerifiedClaimToAuthorizationEndpointRequest {


	@Override
	@PreEnvironment(required = {"server", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject verifiedClaims = new JsonObject();
		JsonObject verification = new JsonObject();
		verification.add("trust_framework", JsonNull.INSTANCE);
		verifiedClaims.add("verification", verification);
		JsonObject claims = new JsonObject();
		JsonElement claimName = getVerifiedClaimsRequestList(env).get(0);
		claims.add(OIDFJSON.getString(claimName), JsonNull.INSTANCE);

		verifiedClaims.add("claims", claims);
		addVerifiedClaims(env, verifiedClaims, true, false);
		logSuccess("Added verified claims to authorization request",
			args("authorization_endpoint_request", env.getObject("authorization_endpoint_request"),
			"verified_claims", verifiedClaims));
		return env;
	}

}
