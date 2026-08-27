package net.openid.conformance.condition.as;

import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.util.Set;

public class CdrValidateAuthorizationSignedResponseAlg extends AbstractCondition {

	private static final Set<String> PERMITTED = Set.of("PS256", "ES256");

	@Override
	@PreEnvironment(required = "server_jwks")
	public Environment evaluate(Environment env) {

		JWK signingKey;
		try {
			signingKey = JWKUtil.getSigningKey(env.getObject("server_jwks"));
		} catch (ParseException e) {
			throw error("Couldn't parse the server JWKS in the test configuration", e);
		}
		String keyAlg = signingKey.getAlgorithm() == null ? null : signingKey.getAlgorithm().getName();

		if (keyAlg == null || !PERMITTED.contains(keyAlg)) {
			throw error("The suite signs JARM responses with the signing key from the test configuration, but CDR requires authorisation responses to be signed using PS256 or ES256. Please use a signing key with an 'alg' of PS256 or ES256 in the test configuration.",
				args("signing_key_alg", keyAlg, "permitted", PERMITTED));
		}

		logSuccess("The suite's JARM signing key uses an algorithm CDR permits",
			args("signing_key_alg", keyAlg, "permitted", PERMITTED));

		return env;
	}

}
