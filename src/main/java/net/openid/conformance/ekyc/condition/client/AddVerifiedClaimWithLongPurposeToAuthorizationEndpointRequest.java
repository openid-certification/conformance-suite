package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;


public class AddVerifiedClaimWithLongPurposeToAuthorizationEndpointRequest extends AbstractAddVerifiedClaimToAuthorizationEndpointRequest {


	@Override
	@PreEnvironment(required = {"server", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonElement verifiedClaimsSupportedElement = env.getElementFromObject("server", "claims_in_verified_claims_supported");
		if(verifiedClaimsSupportedElement==null) {
			throw error("claims_in_verified_claims_supported element in server configuration is required for this test");
		}
		JsonObject verifiedClaims = new JsonObject();
		JsonObject verification = new JsonObject();
		verification.add("trust_framework", JsonNull.INSTANCE);
		verifiedClaims.add("verification", verification);
		JsonObject claims = new JsonObject();
		JsonElement claimName = verifiedClaimsSupportedElement.getAsJsonArray().get(0);
		JsonObject claimInfoObject = new JsonObject();
		claimInfoObject.addProperty("purpose", "This purpose string is longer than 300 characters and " +
			"the authentication request MUST fail and the OP must return an error invalid_request to the RP." +
			"Abcdef ghijk lmnop qrstuv wxyz. " +
			"Abcdef ghijk lmnop qrstuv wxyz. " +
			"Abcdef ghijk lmnop qrstuv wxyz. " +
			"Abcdef ghijk lmnop qrstuv wxyz. " +
			"Abcdef ghijk lmnop qrstuv wxyz. " +
			"Abcdef ghijk lmnop qrstuv wxyz. " +
			"Abcdef ghijk lmnop qrstuv wxyz. " +
			"Abcdef ghijk lmnop qrstuv wxyz. " +
			"Abcdef ghijk lmnop qrstuv wxyz. ");
		claims.add(OIDFJSON.getString(claimName), claimInfoObject);

		verifiedClaims.add("claims", claims);
		addVerifiedClaims(env, verifiedClaims, true, true);
		logSuccess("Added verified claims to authorization request, " +
				"including a claim with a very long purpose",
			args("authorization_endpoint_request", env.getObject("authorization_endpoint_request"),
			"verified_claims", verifiedClaims));
		return env;
	}

}
