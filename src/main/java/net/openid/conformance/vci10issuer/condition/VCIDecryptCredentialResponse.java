package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.util.JSONObjectUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWEUtil;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;

/**
 * Decrypts an encrypted credential response (JWE).
 *
 * When the credential issuer returns an encrypted response, this condition
 * decrypts it using the wallet's private key.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-11.2.3">OID4VCI Section 11.2.3 - Credential Response Encryption</a>
 */
public class VCIDecryptCredentialResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"endpoint_response", "credential_encryption_jwks"})
	@PostEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject endpointResponse = env.getObject("endpoint_response");
		String responseBody = OIDFJSON.getString(endpointResponse.get("body"));

		// Check if the response is a JWE (starts with eyJ and has 5 parts separated by dots)
		if (!isJWE(responseBody)) {
			logSuccess("Response does not appear to be encrypted, skipping decryption",
				args("body_preview", responseBody != null ? responseBody.substring(0, Math.min(100, responseBody.length())) : null));
			return env;
		}

		JsonObject encryptionJwks = env.getObject("credential_encryption_jwks");

		try {
			// Parse the JWE
			JWEObject jweObject = JWEObject.parse(responseBody);
			JWEAlgorithm algorithm = jweObject.getHeader().getAlgorithm();

			// Get the decryption key from the configured JWKS
			JWKSet jwkSet = JWKUtil.parseJWKSet(encryptionJwks.toString());
			JWK decryptionKey = JWEUtil.selectAsymmetricKeyForEncryption(jwkSet, algorithm);

			if (decryptionKey == null) {
				throw error("No suitable key for decrypting the credential response was found in credential_encryption_jwks",
					args("algorithm", algorithm.getName(), "credential_encryption_jwks", encryptionJwks));
			}

			// Decrypt the JWE
			JWEDecrypter decrypter = JWEUtil.createDecrypter(algorithm, decryptionKey);
			jweObject.decrypt(decrypter);

			// Get the decrypted payload
			String decryptedPayload = jweObject.getPayload().toString();

			// Parse the decrypted JSON
			JsonObject decryptedResponse = JsonParser.parseString(decryptedPayload).getAsJsonObject();

			// Update the endpoint response with the decrypted body
			endpointResponse.addProperty("body", decryptedPayload);
			endpointResponse.add("body_json", decryptedResponse);
			endpointResponse.addProperty("encrypted", true);

			// Store the JWE header for validation (using same approach as JWTUtil.jwtHeaderAsJsonObject)
			JsonObject jweHeader = JsonParser.parseString(
				JSONObjectUtils.toJSONString(jweObject.getHeader().toJSONObject())).getAsJsonObject();
			env.putObject("credential_response_jwe_header", jweHeader);

			logSuccess("Decrypted credential response",
				args("jwe_header", jweHeader, "decrypted_response", decryptedResponse));

			return env;

		} catch (ParseException | JsonParseException e) {
			// Nimbus JWE parsing uses ParseException
			// GSON JSON parsing uses JsonParseException
			throw error("Failed to parse encrypted credential response as JWE", e,
				args("response_body", responseBody));
		} catch (JOSEException e) {
			throw error("Failed to decrypt credential response", e,
				args("response_body", responseBody));
		}
	}

	/**
	 * Checks if the string appears to be a JWE (compact serialization).
	 * A JWE has 5 parts separated by dots.
	 */
	private boolean isJWE(String str) {
		if (str == null || str.isEmpty()) {
			return false;
		}
		// JWE compact serialization has exactly 5 parts
		String[] parts = str.split("\\.");
		return parts.length == 5;
	}
}
