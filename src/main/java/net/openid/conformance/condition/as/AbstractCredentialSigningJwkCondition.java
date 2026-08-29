package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

/**
 * Base for the conditions that sign with the 'Signing JWK' of the 'Credential Issuer' section of
 * the test configuration: the key that signs the SD-JWT VCs the suite presents, and the Token
 * Status List that goes with them, so that both verify against the same issuer.
 */
public abstract class AbstractCredentialSigningJwkCondition extends AbstractCondition {

	protected JWK credentialSigningJwk(Environment env) {
		JsonElement signingJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		if (signingJwkEl == null) {
			throw error("'Signing JWK' field is missing from the 'Credential Issuer' section in the test configuration");
		}
		try {
			return JWK.parse(signingJwkEl.toString());
		} catch (ParseException e) {
			throw error("Failed to parse the 'Signing JWK' field in the 'Credential Issuer' section of the test configuration", e, args("signing_jwk", signingJwkEl));
		}
	}

	/** The algorithm the key signs with: its alg, or ES256 for an EC key that names none. */
	protected JWSAlgorithm signingAlgorithm(JWK signingJwk) {
		if (signingJwk.getAlgorithm() != null) {
			return JWSAlgorithm.parse(signingJwk.getAlgorithm().getName());
		}
		if (signingJwk instanceof ECKey) {
			return JWSAlgorithm.ES256;
		}
		throw error("'Signing JWK' field in the 'Credential Issuer' section of the test configuration must include an 'alg' claim specifying the signing algorithm, as there is no default for this key type",
			args("kty", signingJwk.getKeyType().getValue(), "kid", signingJwk.getKeyID()));
	}
}
