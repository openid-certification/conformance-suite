package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.util.UUID;

/**
 * Generates a JWKS for credential request encryption on the emulated issuer (wallet test) side.
 *
 * Per OID4VCI 1.0 Final Section 10, the Credential Issuer publishes an encryption JWKS via the
 * credential_request_encryption metadata; the wallet encrypts credential requests to one of those
 * keys. This condition generates an EC P-256 key with alg=ECDH-ES and use=enc, stores the private
 * JWKS under vci.credential_request_encryption_jwks (for decryption), and the public JWKS under
 * vci.credential_request_encryption_public_jwks (for inclusion in issuer metadata).
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-10">OID4VCI Section 10 - Encrypted Credential Requests and Responses</a>
 */
public class VCIGenerateCredentialRequestEncryptionJwks extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	@PostEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JWK jwk;
		try {
			jwk = new ECKeyGenerator(Curve.P_256)
				.provider(BouncyCastleProviderSingleton.getInstance())
				.algorithm(JWEAlgorithm.ECDH_ES)
				.keyUse(KeyUse.ENCRYPTION)
				.keyID(UUID.randomUUID().toString())
				.generate();
		} catch (JOSEException e) {
			throw error("Failed to generate credential request encryption key", e);
		}

		JWKSet jwkSet = new JWKSet(jwk);
		JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(jwkSet);
		JsonObject publicJwks = JWKUtil.getPublicJwksAsJsonObject(jwkSet);

		env.putObject("vci", "credential_request_encryption_jwks", privateJwks);
		env.putObject("vci", "credential_request_encryption_public_jwks", publicJwks);

		log("Generated credential request encryption JWK set",
			args("credential_request_encryption_public_jwks", publicJwks));

		return env;
	}
}
