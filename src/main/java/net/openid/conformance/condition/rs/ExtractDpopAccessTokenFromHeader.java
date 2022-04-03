package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractDpopAccessTokenFromHeader extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(required = "incoming_dpop_access_token")
	public Environment evaluate(Environment env) {
		env.removeObject("incoming_dpop_access_token");

		String authHeader = env.getString("incoming_request", "headers.authorization");
		if (!Strings.isNullOrEmpty(authHeader)) {
			if (authHeader.toLowerCase().startsWith("dpop ")) {
				String tokenFromHeader = authHeader.substring("dpop ".length());
				if(!Strings.isNullOrEmpty(tokenFromHeader)) {
					logSuccess("Found DPoP access token on incoming request Authorization header", args("access_token", tokenFromHeader));

					try {
						JsonObject jwtAsJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(tokenFromHeader);
						if (jwtAsJsonObject == null) {
							throw error("Couldn't extract DPoP access token from Authorization header", args("header", tokenFromHeader));
						}
						// save the parsed token
						env.putObject("incoming_dpop_access_token", jwtAsJsonObject);
						logSuccess("Found and parsed the DPoP access token", jwtAsJsonObject);
						return env;
					} catch (ParseException e) {
						throw error("Couldn't parse Couldn't parse DPoP access token as a JWT", e, args("DPoP Token", tokenFromHeader));
					}
				}
			}
		}
		throw error("Couldn't parse DPoP access token as a JWT", args("DPoP Token", authHeader));
	}

}
