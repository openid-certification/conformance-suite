package net.openid.conformance.condition.client;

import java.text.ParseException;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

public class ParseAccessTokenAsJwt extends AbstractCondition {

	@PreEnvironment(required = "access_token")
	@PostEnvironment(required = "access_token_jwt")
	@Override
	public Environment evaluate(Environment env) {
		String accessToken = env.getString("access_token", "value");

		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Access token is missing");
		}

		try {
			JWT jwt = JWTUtil.parseJWT(accessToken);

			JsonObject header = JWTUtil.jwtHeaderAsJsonObject(jwt);
			JsonObject claims = JWTUtil.jwtClaimsSetAsJsonObject(jwt, false);

			JsonObject o = new JsonObject();
			o.addProperty("value", accessToken); // save the original string to allow for crypto operations
			o.add("header", header);
			o.add("claims", claims);

			env.putObject("access_token_jwt", o);

			logSuccess("Extracted access token as a JWT", o);

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse access token as a JWT", e, args("access_token", accessToken));
		}
	}

}
