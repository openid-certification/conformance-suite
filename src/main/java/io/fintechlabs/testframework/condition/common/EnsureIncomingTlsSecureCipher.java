package io.fintechlabs.testframework.condition.common;

import java.util.List;

import com.google.common.collect.ImmutableList;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureIncomingTlsSecureCipher extends AbstractCondition {


	// list of allowed cyphers from BCP195; note that apache strips off the "TLS_" from these names and formats them with dashes, unlike
	// the constants found in the CipherSuite enum used by DisallowInsecureCipher

	private static final List<String> ALLOWED_CIPHERS = ImmutableList.of(
		"DHE-RSA-AES128-GCM-SHA256",
		"ECDHE-RSA-AES128-GCM-SHA256",
		"DHE-RSA-AES256-GCM-SHA384",
		"ECDHE-RSA-AES256-GCM-SHA384");

	@PreEnvironment(required = "client_request")
	@Override
	public Environment evaluate(Environment env) {

		String cipher = env.getString("client_request", "headers.x-ssl-cipher");

		if (ALLOWED_CIPHERS.contains(cipher)) {
			logSuccess("TLS cipher is allowed", args("expected", ALLOWED_CIPHERS, "actual", cipher));
			return env;
		} else {
			throw error("TLS cipher is not allowed", args("expected", ALLOWED_CIPHERS, "actual", cipher));
		}

	}

}
