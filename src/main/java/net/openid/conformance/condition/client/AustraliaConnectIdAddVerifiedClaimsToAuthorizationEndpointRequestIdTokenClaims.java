package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.ekyc.condition.client.AbstractAddVerifiedClaimToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.Environment;

public class AustraliaConnectIdAddVerifiedClaimsToAuthorizationEndpointRequestIdTokenClaims extends AbstractAddVerifiedClaimToAuthorizationEndpointRequest {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "server"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject verifiedObject = new JsonObject();
		JsonObject verification = new JsonObject();

		JsonObject trustFramework = new JsonObject();
		trustFramework.addProperty("value", AustraliaConnectIdCheckTrustFrameworkSupported.ConnectIdTrustFramework);
		verification.add("trust_framework", trustFramework);
		verifiedObject.add("verification", verification);

		JsonObject verifiedClaims = new JsonObject();

		for (String claimNameString : AustraliaConnectIdCheckVerifiedClaimsSupported.ConnectIdVerifiedClaims) {
			JsonObject claimInfoObject = new JsonObject();
			if (claimNameString.startsWith("beneficiary")) {
				claimInfoObject.addProperty("essential", false);

			}
			else {
				claimInfoObject.addProperty("essential", true);
			}

			verifiedClaims.add(claimNameString, claimInfoObject);
		}

		verifiedObject.add("claims", verifiedClaims);

		addVerifiedClaims(env, verifiedObject, true, false);

		logSuccess("Added verified claims to authorization request",
			args("authorization_endpoint_request", env.getObject("authorization_endpoint_request"),
			"verified_claims", verifiedObject));

		return env;
	}
}
