package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.UUID;


public class AddClaimWithRandomValueToAuthorizationEndpointRequest extends AbstractAddVerifiedClaimToAuthorizationEndpointRequest {


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
		JsonObject claimInfoObject = new JsonObject();
		claimInfoObject.addProperty("value", UUID.randomUUID().toString());

		String claimNameString = OIDFJSON.getString(claimName);

		claims.add(claimNameString, claimInfoObject);

		verifiedClaims.add("claims", claims);
		addVerifiedClaims(env, verifiedClaims, true, true);
		logSuccess("Added verified claims to authorization request, with a random requested claim value",
			args("authorization_endpoint_request", env.getObject("authorization_endpoint_request"),
			"verified_claims", verifiedClaims));
		return env;
	}

}
