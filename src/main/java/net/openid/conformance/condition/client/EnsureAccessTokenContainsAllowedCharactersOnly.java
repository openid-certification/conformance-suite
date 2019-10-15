package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.regex.Pattern;

public class EnsureAccessTokenContainsAllowedCharactersOnly extends AbstractCondition {
	private static final String VSCHAR_PATTERN = "[\\x20-\\x7E]+";

	@Override
	@PreEnvironment(required = {"token_endpoint_response"})
	public Environment evaluate(Environment env)
	{

		String accessToken = env.getString("token_endpoint_response", "access_token");
		if(accessToken==null) {
			throw error("Token endpoint response does not contain an access_token");
		}

		Pattern validPattern = Pattern.compile(VSCHAR_PATTERN);
		if (!validPattern.matcher(accessToken).matches()) {
			throw error("Access token contains illegal characters. As per RFC-6749, only characters between %x20 and %x7E are allowed.",
						args("access_token", accessToken));
		}
		logSuccess("Access token does not contain any illegal characters");
		return env;
	}
}
