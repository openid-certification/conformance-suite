package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractExtractJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractSignedJwtFromPaymentConsentResponse extends AbstractExtractJWT {

	public static final String CONSENT_ENDPOINT_RESPONSE_JWT = "consent_endpoint_response_jwt";

	@Override
	@PreEnvironment(strings = CONSENT_ENDPOINT_RESPONSE_JWT)
	@PostEnvironment(required = { "consent_endpoint_response" } )
	public Environment evaluate(Environment env) {

		env.removeObject("consent_endpoint_response");

		String consentEndpointJws = env.getString(CONSENT_ENDPOINT_RESPONSE_JWT);

		try {
			JsonObject jwtAsJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(consentEndpointJws);

			// save the parsed token
			env.putObject("consent_endpoint_response_jwt", jwtAsJsonObject);

			env.putObject("consent_endpoint_response", jwtAsJsonObject.getAsJsonObject("claims"));

			logSuccess("Found and parsed the JWT from " + CONSENT_ENDPOINT_RESPONSE_JWT, jwtAsJsonObject);

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse the " + CONSENT_ENDPOINT_RESPONSE_JWT + " as a JWT", e,
				args(CONSENT_ENDPOINT_RESPONSE_JWT, consentEndpointJws));
		}

	}

}
