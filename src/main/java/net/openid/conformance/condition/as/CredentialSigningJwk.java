package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The 'Signing JWK' of the 'Credential Issuer' section of the test configuration: the key that
 * signs the SD-JWT VCs the suite presents, and the Token Status List that goes with them, so
 * that both verify against the same issuer.
 */
final class CredentialSigningJwk {

	/**
	 * A problem with the configured key, for the condition that hit it to report with the message
	 * and details here.
	 */
	static final class Problem extends Exception {

		private static final long serialVersionUID = 1L;

		private final transient Map<String, Object> details;

		Problem(String message, Throwable cause, Map<String, Object> details) {
			super(message, cause);
			this.details = details;
		}

		Map<String, Object> details() {
			return details;
		}
	}

	private CredentialSigningJwk() {
		// utility class
	}

	static JWK fromConfig(Environment env) throws Problem {
		JsonElement signingJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		if (signingJwkEl == null) {
			throw new Problem("'Signing JWK' field is missing from the 'Credential Issuer' section in the test configuration",
				null, Map.of());
		}
		try {
			return JWK.parse(signingJwkEl.toString());
		} catch (ParseException e) {
			throw new Problem("Failed to parse the 'Signing JWK' field in the 'Credential Issuer' section of the test configuration",
				e, Map.of("signing_jwk", signingJwkEl));
		}
	}

	/** The algorithm the key signs with: its alg, or ES256 for an EC key that names none. */
	static JWSAlgorithm signingAlgorithm(JWK signingJwk) throws Problem {
		if (signingJwk.getAlgorithm() != null) {
			return JWSAlgorithm.parse(signingJwk.getAlgorithm().getName());
		}
		if (signingJwk instanceof ECKey) {
			return JWSAlgorithm.ES256;
		}
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("kty", signingJwk.getKeyType().getValue());
		details.put("kid", signingJwk.getKeyID());
		throw new Problem("'Signing JWK' field in the 'Credential Issuer' section of the test configuration must include an 'alg' claim specifying the signing algorithm, as there is no default for this key type",
			null, details);
	}
}
