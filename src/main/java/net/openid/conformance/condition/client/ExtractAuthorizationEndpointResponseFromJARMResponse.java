package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class ExtractAuthorizationEndpointResponseFromJARMResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jarm_response")
	public Environment evaluate(Environment env) {
		// list of standard jwt claims as per RFC 7519-4.1
		List<String> jwtClaims = ImmutableList.of(/*"iss", */ "sub", "aud", "exp", "nbf", "iat", "jti");

		JsonObject jarmClaims = (JsonObject) env.getElementFromObject("jarm_response", "claims");

		// the JWT standard claims aren't part of the authorization response, so remove them - except for iss
		// which is also part of the response as per https://datatracker.ietf.org/doc/html/draft-ietf-oauth-iss-auth-resp
		// and https://bitbucket.org/openid/fapi/issues/478/fapi2-baseline-jarm-iss-draft
		JsonObject authResponse = jarmClaims.deepCopy();
		for (String claim : jwtClaims) {
			authResponse.remove(claim);
		}

		env.putObject("authorization_endpoint_response", authResponse);

		logSuccess("Extracted the authorization response", authResponse);

		return env;
	}

}
