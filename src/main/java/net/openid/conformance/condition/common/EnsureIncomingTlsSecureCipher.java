package net.openid.conformance.condition.common;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
			throw error("The incoming connection from the client used a TLS cipher that is not permitted by FAPI. The client should be reconfigured so it will only use permitted TLS ciphers.", args("expected", ALLOWED_CIPHERS, "actual", cipher));
		}

	}

}
