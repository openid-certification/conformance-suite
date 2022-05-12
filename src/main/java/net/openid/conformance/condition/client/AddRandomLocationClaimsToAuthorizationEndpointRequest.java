package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.LinkedList;
import java.util.List;

public class AddRandomLocationClaimsToAuthorizationEndpointRequest extends AbstractAddClaimToAuthorizationEndpointRequest {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "server"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String locationStr = RandomStringUtils.randomAlphanumeric(10);
		JsonObject claimsObject = getClaimsForLocation(authorizationEndpointRequest, locationStr);
		List<String> claimsToAdd = new LinkedList<>();
		claimsToAdd.add(RandomStringUtils.randomAlphanumeric(10));
		addRequestsForClaims(claimsObject, claimsToAdd);

		logSuccess("Added a request for claims to be returned in a random location to the authorization_endpoint_request. As per spec, 'Any members used that are not understood MUST be ignored.'.",
			args("authorization_endpoint_request", authorizationEndpointRequest));

		return env;
	}

}
