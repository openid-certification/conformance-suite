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

		JsonObject encryptionJwks = env.getObject("credential_encryption_jwks");

		try {
			// Parse the JWE
			JWEObject jweObject = JWEObject.parse(responseBody);
			JWEAlgorithm algorithm = jweObject.getHeader().getAlgorithm();
			String kid = jweObject.getHeader().getKeyID();

			// Get the decryption key from the configured JWKS
			JWKSet jwkSet = JWKUtil.parseJWKSet(encryptionJwks.toString());
			JWK decryptionKey = JWEUtil.selectAsymmetricKeyForEncryption(jwkSet, algorithm, kid);

			if (decryptionKey == null) {
				throw error("The credential response was encrypted with an alg/kid that does not match any key the test suite offered"
					+ " in the credential request's credential_response_encryption.jwks."
					+ " The issuer must encrypt to one of the keys the client advertised.",
					args("algorithm", algorithm.getName(), "kid", kid, "offered_jwks", encryptionJwks));
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

			// Store the JWE in the common environment format (value/claims/jwe_header) so
			// downstream conditions can read it like any other JWE in the suite.
			JsonObject credentialResponseJwe = JWEUtil.jweStringToJsonObjectForEnvironment(responseBody, decryptedResponse);
			env.putObject("credential_response_jwe", credentialResponseJwe);

			logSuccess("Decrypted credential response",
				args("credential_response_jwe", credentialResponseJwe));

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
}
