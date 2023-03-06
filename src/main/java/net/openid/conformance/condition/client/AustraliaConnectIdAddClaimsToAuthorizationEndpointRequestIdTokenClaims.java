package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class AustraliaConnectIdAddClaimsToAuthorizationEndpointRequestIdTokenClaims extends AbstractAddClaimToAuthorizationEndpointRequest {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "server"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		String locationStr = "id_token";
		JsonObject claimsObject = getClaimsForLocation(authorizationEndpointRequest, locationStr);
		List<String> claimsToAdd = List.of(AustraliaConnectIdCheckClaimsSupported.ConnectIdMandatoryToSupportClaims);

		addRequestsForClaims(claimsObject, claimsToAdd);

		logSuccess("Added ConnectID required claims to authorization_endpoint_request using various different forms of request",
			args("authorization_endpoint_request", authorizationEndpointRequest,
				"oidc_claims", claimsToAdd));

		return env;
	}

}
