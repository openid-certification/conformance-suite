package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class OIDSSFParseVerificationEventToken extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String tokenString = env.getString("ssf", "verification.jwt");

		JsonObject tokenJsonObject;
		try {
			tokenJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(tokenString);
		} catch (ParseException e) {
			logFailure("Could not parse verification token", args("token_string", tokenString, "error_message", e.getMessage()));
			return env;
		}

		env.putObject("ssf", "verification.token", tokenJsonObject);

		logSuccess("Parsed verification event token", args("token", tokenJsonObject));

		return env;
	}

}
