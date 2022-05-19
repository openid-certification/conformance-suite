package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedList;
import java.util.List;

public class IdmvpAddClaimsToAuthorizationEndpointRequestIdTokenClaims extends AbstractAddClaimToAuthorizationEndpointRequest {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "server"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		String locationStr = "id_token";
		JsonObject claimsObject = getClaimsForLocation(authorizationEndpointRequest, locationStr);
		List<String> claimsToAdd = List.of(IdmvpCheckClaimsSupported.idmvpMandatoryToSupportClaims);

		addRequestsForClaims(claimsObject, claimsToAdd);

		logSuccess("Added IDMVP required claims to authorization_endpoint_request using various different forms of request",
			args("authorization_endpoint_request", authorizationEndpointRequest,
				"oidc_claims", claimsToAdd));

		return env;
	}

}
