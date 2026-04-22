package net.openid.conformance.condition.as;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.testmodule.Environment;

/**
 * Creates an SD-JWT credential with a minimal cnf.jwk containing only the
 * required fields for the key type (kty, crv, x, y for EC). Optional JWK
 * metadata (kid, use, alg, key_ops, x5c, x5t, x5t#S256) is stripped.
 *
 * This tests that verifiers and wallets correctly handle JWKs without
 * optional metadata in the credential's cnf claim.
 */
public class CreateSdJwtKbCredentialWithMinimalCnf extends CreateSdJwtKbCredential {

	@Override
	protected String createSdJwt(Environment env, JWK publicJWK, ECKey privateKey, String credentialType) {
		JWK minimalPublicJWK = publicJWK;
		if (publicJWK instanceof ECKey ecKey) {
			try {
				minimalPublicJWK = new ECKey.Builder(ecKey.getCurve(), ecKey.toECPublicKey()).build();
			} catch (JOSEException e) {
				throw error("Failed to create minimal EC public JWK", e);
			}
		}
		return super.createSdJwt(env, minimalPublicJWK, privateKey, credentialType);
	}
}
