package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractExtractJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractSignedUserInfoFromUserInfoEndpointResponse extends AbstractExtractJWT {

	public static final String USERINFO_ENDPOINT_RESPONSE = "userinfo_endpoint_response";

	@Override
	@PreEnvironment(strings = USERINFO_ENDPOINT_RESPONSE)
	@PostEnvironment(required = { "userinfo", "userinfo_object" } )
	public Environment evaluate(Environment env) {

		// Remove any old token
		env.removeObject("userinfo");

		String userInfoJws = env.getString(USERINFO_ENDPOINT_RESPONSE);

		try {
			JsonObject jwtAsJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(userInfoJws);

			// save the parsed token
			env.putObject("userinfo_object", jwtAsJsonObject);

			var claims = jwtAsJsonObject.getAsJsonObject("claims");
			env.putObject("userinfo", claims);

			logSuccess("Found and parsed the userinfo from " + USERINFO_ENDPOINT_RESPONSE, jwtAsJsonObject);

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse the " + USERINFO_ENDPOINT_RESPONSE + " as a JWT", e,
				args(USERINFO_ENDPOINT_RESPONSE, userInfoJws));
		}

	}

}
