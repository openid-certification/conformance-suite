package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class OIDSSFParseSecurityEventToken extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String tokenString = env.getString("ssf", "verification.jwt");

		JsonObject tokenJsonObject;
		try {
			tokenJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(tokenString);
		} catch (ParseException e) {
			throw error("Could not parse Security Event Token", args("token_string", tokenString, "error_message", e.getMessage()));
		}

		env.putObject("ssf", "verification.token", tokenJsonObject);
		env.putObject("set_token", tokenJsonObject);

		logSuccess("Parsed Security Event Token", args("token", tokenJsonObject));

		return env;
	}

}
