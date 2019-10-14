package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.regex.Pattern;

public class EnsureRefreshTokenContainsAllowedCharactersOnly extends AbstractCondition {
	private static final String VSCHAR_PATTERN = "[\\x20-\\x7E]+";

	@Override
	@PreEnvironment(required = {"token_endpoint_response"})
	public Environment evaluate(Environment env) {
		String refreshToken = env.getString("token_endpoint_response", "refresh_token");
		if(refreshToken==null) {
			logSuccess("Token endpoint response does not contain a refresh_token");
			return env;
		}

		Pattern validPattern = Pattern.compile(VSCHAR_PATTERN);
		if (!validPattern.matcher(refreshToken).matches()) {
			throw error("Refresh token contains illegal characters. As per RFC-6749, only characters between %x20 and %x7E are allowed.",
				args("refresh_token", refreshToken));
		}
		logSuccess("Refresh token does not contain any illegal characters");
		return env;
	}
}
