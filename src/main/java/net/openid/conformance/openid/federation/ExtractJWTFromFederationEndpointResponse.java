package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractJWTFromFederationEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "federation_endpoint_response")
	@PostEnvironment(required = { "federation_response_jwt" })
	public Environment evaluate(Environment env) {

		JsonElement federationEndpointResponseBody = env.getElementFromObject("federation_endpoint_response", "body");
		if (federationEndpointResponseBody == null ||
			!federationEndpointResponseBody.isJsonPrimitive() ||
			!federationEndpointResponseBody.getAsJsonPrimitive().isString()) {
			throw error("The response body is expected to contain a JWT string", args("jwt", federationEndpointResponseBody));
		}

		String jwt = OIDFJSON.getString(federationEndpointResponseBody);
		try {
			JsonObject jwtAsJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(jwt.trim());
			env.putObject("federation_response_jwt", jwtAsJsonObject);
			logSuccess("Extracted JWT from federation endpoint response", args("federation_response_jwt", jwtAsJsonObject));
			return env;
		} catch (ParseException | OIDFJSON.UnexpectedJsonTypeException e) {
			env.removeObject("federation_response_jwt");
			throw error("Failed to parse JWT from federation endpoint response", e, args("jwt", jwt));
		}
	}

}
