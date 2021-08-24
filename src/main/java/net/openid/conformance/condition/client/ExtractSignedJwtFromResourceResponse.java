package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractExtractJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractSignedJwtFromResourceResponse extends AbstractExtractJWT {

	@Override
	@PreEnvironment(required = "endpoint_response")
	@PostEnvironment(required = { "consent_endpoint_response" } )
	public Environment evaluate(Environment env) {

		env.removeObject("consent_endpoint_response");

		String consentEndpointJws = env.getString("endpoint_response", "body");

		try {
			JsonObject jwtAsJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(consentEndpointJws);

			// save the parsed token
			env.putObject("consent_endpoint_response_jwt", jwtAsJsonObject);

			env.putObject("consent_endpoint_response", jwtAsJsonObject.getAsJsonObject("claims"));

			logSuccess("Found and parsed the JWT from payment consent endpoint", jwtAsJsonObject);

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse the payment consent response as a JWT", e,
				args("response", consentEndpointJws));
		}

	}

}
