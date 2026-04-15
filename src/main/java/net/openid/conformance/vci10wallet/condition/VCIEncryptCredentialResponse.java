package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.CompressionAlgorithm;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWEUtil;
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

		if (!encryptionParams.has("enc")) {
			String errorDescription = "credential_response_encryption must contain 'enc' parameter";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_ENCRYPTION_PARAMETERS, errorDescription);
			throw error(errorDescription,
				args("credential_response_encryption", encryptionParams));
		}

		String enc = OIDFJSON.getString(encryptionParams.get("enc"));

		if (!encryptionParams.has("jwk")) {
			String errorDescription = "credential_response_encryption must contain 'jwk' parameter";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_ENCRYPTION_PARAMETERS, errorDescription);
			throw error(errorDescription,
				args("credential_response_encryption", encryptionParams));
		}

		JsonElement jwkEl = encryptionParams.get("jwk");
		if (!jwkEl.isJsonObject()) {
			String errorDescription = "credential_response_encryption 'jwk' parameter must be a JSON object";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_ENCRYPTION_PARAMETERS, errorDescription);
			throw error(errorDescription,
				args("credential_response_encryption", encryptionParams));
		}

		// Use the JWK provided in the request
		JWK encryptionKey;
		try {
			encryptionKey = JWK.parse(jwkEl.getAsJsonObject().toString());
		} catch (ParseException e) {
			throw error("Failed to parse JWK from credential_response_encryption",
				e, args("jwk", jwkEl));
		}

		String alg = null;
		if (encryptionKey.getAlgorithm() != null) {
			alg = encryptionKey.getAlgorithm().getName();
		}

		if (alg == null || alg.isBlank()) {
			String errorDescription = "credential_response_encryption must identify the encryption algorithm via 'jwk.alg'";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_ENCRYPTION_PARAMETERS, errorDescription);
			throw error(errorDescription, args("credential_response_encryption", encryptionParams));
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

			// Apply compression if requested
			JsonElement zipEl = encryptionParams.get("zip");
			if (zipEl != null) {
				headerBuilder.compressionAlgorithm(new CompressionAlgorithm(OIDFJSON.getString(zipEl)));
			}

			JWEObject jweObject = new JWEObject(
				headerBuilder.build(),
				new Payload(credentialResponse.toString())
			);

			jweObject.encrypt(encrypter);
			String encryptedResponse = jweObject.serialize();

			// Per the spec, when encrypted the entire response body is the JWE
			env.putString("encrypted_credential_response", encryptedResponse);

			JsonObject credentialResponseJwe = JWEUtil.jweStringToJsonObjectForEnvironment(encryptedResponse, credentialResponse);

			logSuccess("Encrypted credential response as JWE",
				args("credential_response_jwe", credentialResponseJwe));

			return env;

		} catch (JOSEException e) {
			throw error("Failed to encrypt credential response", e,
				args("alg", alg, "enc", enc));
		} catch (ParseException e) {
			throw error("Failed to parse serialized credential response JWE for logging", e);
		}
	}
}
