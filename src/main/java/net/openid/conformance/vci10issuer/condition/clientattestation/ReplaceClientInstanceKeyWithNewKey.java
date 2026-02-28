package net.openid.conformance.vci10issuer.condition.clientattestation;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractGenerateKey;
import net.openid.conformance.testmodule.Environment;

/**
 * Replaces the client_instance_key with a freshly generated key so that the subsequent
 * client attestation PoP JWT will be signed with a key that does not match the cnf.jwk
 * already embedded in the attestation JWT.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.2">OAuth2-ATCA Section 5.2</a>
 */
public class ReplaceClientInstanceKeyWithNewKey extends AbstractGenerateKey {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		JWK newKey = createJwkForAlg("ES256");

		String originalThumbprint = null;
		String originalKeyJson = env.getString("vci", "client_instance_key");
		if (originalKeyJson != null) {
			try {
				originalThumbprint = JWK.parse(originalKeyJson).computeThumbprint().toString();
			} catch (Exception e) {
				// ignore
			}
		}

		// Replace only the private key used for PoP signing; leave client_instance_key_public
		// unchanged so the attestation JWT's cnf.jwk still references the original key.
		env.putString("vci", "client_instance_key", newKey.toJSONString());

		try {
			logSuccess("Replaced client_instance_key with a fresh key that does not match the attested cnf.jwk",
				args("original_key_thumbprint", originalThumbprint,
					"new_key_thumbprint", newKey.computeThumbprint().toString()));
		} catch (JOSEException e) {
			throw error("Failed to compute thumbprint of new key", e);
		}

		return env;
	}
}
