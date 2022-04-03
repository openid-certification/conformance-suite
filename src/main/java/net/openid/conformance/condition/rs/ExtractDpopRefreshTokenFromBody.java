package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractDpopRefreshTokenFromBody extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "incoming_dpop_refresh_token")
	public Environment evaluate(Environment env) {
		env.removeObject("incoming_dpop_refresh_token");

		String refreshToken = env.getString("token_endpoint_request", "body_form_params.refresh_token");
		if(!Strings.isNullOrEmpty(refreshToken)) {
			logSuccess("Found DPoP refresh token", args("refresh_token", refreshToken));

			try {
				JsonObject jwtAsJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(refreshToken);
				if (jwtAsJsonObject == null) {
					throw error("Couldn't parse DPoP refresh token as a JWT", args("incoming_dpop_refresh_token", refreshToken));
				}
				// save the parsed token
				env.putObject("incoming_dpop_refresh_token", jwtAsJsonObject);
				logSuccess("Found and parsed the DPoP refresh token", jwtAsJsonObject);
				return env;
			} catch (ParseException e) {
				throw error("Couldn't parse Couldn't parse DPoP refresh token as a JWT", e, args("DPoP Token", refreshToken));
			}
		}
		throw error("Couldn't find DPoP refresh token", args("body_form_params", env.getObject("body_form_params")));
	}

}
