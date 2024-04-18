package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.regex.Pattern;


public class EnsureDpopNonceContainsAllowedCharactersOnly extends AbstractCondition {
	// Allowed NQCHAR = %x21 / %x23-5B / %x5D-7E
	private static final String NQCHAR_PATTERN = "[\\x21-\\x7E&&[^\\x22\\x5C]]+";

	@Override
	public Environment evaluate(Environment env)
	{

		String dpopNonce = env.getString("authorization_server_dpop_nonce");
		if(dpopNonce==null) {
			log("No DPOP nonce required");
		} else {
			Pattern validPattern = Pattern.compile(NQCHAR_PATTERN);
			if (!validPattern.matcher(dpopNonce).matches()) {
				throw error("DPOP nonce contains illegal characters. As per RFC-6749, only NQCHAR characters %x21 / %x23-5B / %x5D-7E are allowed.",
					args("DPOP nonce", dpopNonce));
			}
			logSuccess("DPOP nonce does not contain any illegal characters", args("DPOP nonce", dpopNonce));
		}
		return env;
	}
}
