package net.openid.conformance.openid.federation;

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

		String jwt = OIDFJSON.getString(env.getElementFromObject("federation_endpoint_response", "body"));
		try {
			JsonObject jwtAsJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(jwt);
			env.putObject("federation_response_jwt", jwtAsJsonObject);
			logSuccess("Extracted JWT from federation endpoint response", args("fereation_response_jwt", jwtAsJsonObject));
			return env;
		} catch (ParseException e) {
			throw error("Failed to parse JWT from federation endpoint response", e, args("jwt", jwt));
		}
	}

}
