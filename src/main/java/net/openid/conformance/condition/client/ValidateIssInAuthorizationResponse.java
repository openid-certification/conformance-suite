package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;
import java.util.Date;

import static java.time.temporal.ChronoUnit.DAYS;

public class ValidateIssInAuthorizationResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_response", "server" } )
	public Environment evaluate(Environment env) {

		String issuer = env.getString("server", "issuer");

		String authResponseIssuer = env.getString("authorization_endpoint_response", "iss");

		if (authResponseIssuer == null) {
			log("No 'iss' value in authorization response.");
			return env;
		}

		if (!issuer.equals(authResponseIssuer)) {
			throw error("'iss' parameter in authorization response does not match server's issuer value.",
				args("expected", issuer, "actual", authResponseIssuer));
		}

		logSuccess("'iss' parameter in authorization response matches server's issuer value.");

		return env;

	}

}
