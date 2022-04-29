package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractExtractJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;
import java.util.List;

public class ExtractSignedUserInfoFromUserInfoEndpointResponse extends AbstractExtractJWT {

	private static final String USERINFO_ENDPOINT_RESPONSE = "userinfo_endpoint_response_full";

	@Override
	@PreEnvironment(required = USERINFO_ENDPOINT_RESPONSE)
	@PostEnvironment(required = { "userinfo", "userinfo_object" } )
	public Environment evaluate(Environment env) {

		// Remove any old token
		env.removeObject("userinfo");

		String userInfoJws = env.getString(USERINFO_ENDPOINT_RESPONSE, "body");

		try {
			JsonObject jwtAsJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(userInfoJws);

			// save the parsed token
			env.putObject("userinfo_object", jwtAsJsonObject);

			// deepcopy to avoid modifying userinfo_object
			var userinfo = jwtAsJsonObject.getAsJsonObject("claims").deepCopy();

			// this list doesn't contain 'sub' as sub is also a standard claim in userinfo
			List<String> jwtClaims = ImmutableList.of("iss", "aud", "exp", "nbf", "iat", "jti");

			// the JWT standard claims aren't part of the userinfo response (apart from 'sub'), so remove them
			for (String claim : jwtClaims) {
				userinfo.remove(claim);
			}

			env.putObject("userinfo", userinfo);

			logSuccess("Found and parsed the userinfo from " + USERINFO_ENDPOINT_RESPONSE, jwtAsJsonObject);

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse the " + USERINFO_ENDPOINT_RESPONSE + " as a JWT", e,
				args(USERINFO_ENDPOINT_RESPONSE, userInfoJws));
		}

	}

}
