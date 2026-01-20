package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWEUtil;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

import java.text.ParseException;
import java.util.Set;

/**
 * Encrypts the credential endpoint response as a JWE per OID4VCI Section 11.2.3.
 *
 * This condition takes the credential response JSON and encrypts it using the
 * encryption parameters provided in the credential request (credential_response_encryption).
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.2.3">OID4VCI Section 11.2.3 - Credential Response Encryption</a>
 */
public class VCIEncryptCredentialResponse extends AbstractCondition {

	// Supported JWE algorithms for credential response encryption
	private static final Set<String> SUPPORTED_ALG_VALUES = Set.of(
		"ECDH-ES", "ECDH-ES+A128KW", "ECDH-ES+A192KW", "ECDH-ES+A256KW"
	);

	// Supported JWE encryption methods
	private static final Set<String> SUPPORTED_ENC_VALUES = Set.of(
		"A128GCM", "A192GCM", "A256GCM", "A128CBC-HS256", "A192CBC-HS384", "A256CBC-HS512"
	);

	@Override
	@PreEnvironment(required = {"credential_endpoint_response", "incoming_request"})
	@PostEnvironment(required = "credential_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject credentialResponse = env.getObject("credential_endpoint_response");
		JsonObject requestBodyJson = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();

		// Check if credential_response_encryption is present in the request
		JsonElement encryptionEl = requestBodyJson.get("credential_response_encryption");
		if (encryptionEl == null || !encryptionEl.isJsonObject()) {
			logSuccess("No credential_response_encryption in request, returning unencrypted response");
			return env;
		}

		JsonObject encryptionParams = encryptionEl.getAsJsonObject();

		// Get required encryption parameters
		String alg = OIDFJSON.getString(encryptionParams.get("alg"));
		String enc = OIDFJSON.getString(encryptionParams.get("enc"));

		if (alg == null || enc == null) {
			String errorDescription = "credential_response_encryption must contain 'alg' and 'enc' parameters";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_ENCRYPTION_PARAMETERS, errorDescription);
			throw error(errorDescription,
				args("credential_response_encryption", encryptionParams));
		}

		// Validate that the requested algorithm is supported
		if (!SUPPORTED_ALG_VALUES.contains(alg)) {
			String errorDescription = "Unsupported encryption algorithm: " + alg;
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_ENCRYPTION_PARAMETERS, errorDescription);
			throw error(errorDescription,
				args("alg", alg, "supported_alg_values", SUPPORTED_ALG_VALUES));
		}

		// Validate that the requested encryption method is supported
		if (!SUPPORTED_ENC_VALUES.contains(enc)) {
			String errorDescription = "Unsupported encryption method: " + enc;
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_ENCRYPTION_PARAMETERS, errorDescription);
			throw error(errorDescription,
				args("enc", enc, "supported_enc_values", SUPPORTED_ENC_VALUES));
		}

		// Get the JWK for encryption (either from request or from configured wallet JWKS)
		JWK encryptionKey = null;
		JsonElement jwkEl = encryptionParams.get("jwk");

		if (jwkEl != null && jwkEl.isJsonObject()) {
			// Use the JWK provided in the request
			try {
				encryptionKey = JWK.parse(jwkEl.getAsJsonObject().toString());
			} catch (ParseException e) {
				throw error("Failed to parse JWK from credential_response_encryption",
					e, args("jwk", jwkEl));
			}
		} else {
			// Try to use the configured wallet encryption JWKS
			JsonObject walletEncryptionJwks = env.getObject("credential_encryption_jwks");
			if (walletEncryptionJwks != null) {
				try {
					JWKSet jwkSet = JWKUtil.parseJWKSet(walletEncryptionJwks.toString());
					JWEAlgorithm jweAlgorithm = JWEAlgorithm.parse(alg);
					encryptionKey = JWEUtil.selectAsymmetricKeyForEncryption(jwkSet, jweAlgorithm);
				} catch (ParseException e) {
					throw error("Failed to parse credential_encryption_jwks", e,
						args("credential_encryption_jwks", walletEncryptionJwks));
				}
			}
		}

		if (encryptionKey == null) {
			throw error("No suitable encryption key found. Either provide 'jwk' in credential_response_encryption or configure credential_encryption_jwks",
				args("credential_response_encryption", encryptionParams));
		}

		// Encrypt the credential response
		JWEAlgorithm jweAlgorithm = JWEAlgorithm.parse(alg);
		EncryptionMethod encryptionMethod = EncryptionMethod.parse(enc);

		try {
			JWEEncrypter encrypter = JWEUtil.createEncrypter(encryptionKey);

			JWEHeader.Builder headerBuilder = new JWEHeader.Builder(jweAlgorithm, encryptionMethod)
				.contentType("json");

			if (encryptionKey.getKeyID() != null) {
				headerBuilder.keyID(encryptionKey.getKeyID());
			}

			JWEObject jweObject = new JWEObject(
				headerBuilder.build(),
				new Payload(credentialResponse.toString())
			);

			jweObject.encrypt(encrypter);
			String encryptedResponse = jweObject.serialize();

			// Replace the response with the encrypted JWE string
			// Per spec, the encrypted response is returned as application/jwt
			JsonObject encryptedResponseJson = new JsonObject();
			encryptedResponseJson.addProperty("credential_response", encryptedResponse);

			// Actually, per the spec, when encrypted the entire response body is the JWE
			// We need to update the response handling to return the JWE directly
			env.putString("encrypted_credential_response", encryptedResponse);

			logSuccess("Encrypted credential response as JWE",
				args("alg", alg, "enc", enc, "kid", encryptionKey.getKeyID(),
					"credential_response_json", credentialResponse,
					"encrypted_credential_response", encryptedResponse));

			return env;

		} catch (JOSEException e) {
			throw error("Failed to encrypt credential response", e,
				args("alg", alg, "enc", enc));
		}
	}
}
