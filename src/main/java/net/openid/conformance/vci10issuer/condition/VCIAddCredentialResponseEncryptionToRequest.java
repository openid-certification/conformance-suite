package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;

/**
 * Adds credential_response_encryption parameters to the credential request.
 *
 * Per OID4VCI Section 8.2, the wallet can request the credential response to be
 * encrypted by including credential_response_encryption with alg, enc, and optionally jwk.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.2">OID4VCI Section 8.2 - Credential Request</a>
 */
public class VCIAddCredentialResponseEncryptionToRequest extends AbstractCondition {

	private static final String DEFAULT_ALG = "ECDH-ES+A256KW";
	private static final String DEFAULT_ENC = "A256GCM";

	@Override
	@PreEnvironment(required = "vci_credential_request_object")
	@PostEnvironment(required = "vci_credential_request_object")
	public Environment evaluate(Environment env) {

		JsonObject credentialRequest = env.getObject("vci_credential_request_object");
		JsonObject encryptionJwks = env.getObject("credential_encryption_jwks");

		if (encryptionJwks == null) {
			throw error("credential_encryption_jwks must be configured when credential_encryption=encrypted");
		}

		// Parse the JWKS and select an encryption key
		JWKSet jwkSet;
		try {
			jwkSet = JWKUtil.parseJWKSet(encryptionJwks.toString());
		} catch (ParseException e) {
			throw error("Failed to parse credential_encryption_jwks", e,
				args("credential_encryption_jwks", encryptionJwks));
		}

		if (jwkSet.getKeys().isEmpty()) {
			throw error("credential_encryption_jwks must contain at least one key",
				args("credential_encryption_jwks", encryptionJwks));
		}

		// Use the first key for encryption
		JWK encryptionKey = jwkSet.getKeys().get(0);

		// Determine alg and enc values
		// Check if the issuer metadata specifies supported values
		String alg = DEFAULT_ALG;
		String enc = DEFAULT_ENC;

		JsonElement algValuesEl = env.getElementFromObject("vci", "credential_issuer_metadata.credential_response_encryption_alg_values_supported");
		if (algValuesEl != null && algValuesEl.isJsonArray() && algValuesEl.getAsJsonArray().size() > 0) {
			alg = OIDFJSON.getString(algValuesEl.getAsJsonArray().get(0));
		}

		JsonElement encValuesEl = env.getElementFromObject("vci", "credential_issuer_metadata.credential_response_encryption_enc_values_supported");
		if (encValuesEl != null && encValuesEl.isJsonArray() && encValuesEl.getAsJsonArray().size() > 0) {
			enc = OIDFJSON.getString(encValuesEl.getAsJsonArray().get(0));
		}

		// Build credential_response_encryption object
		JsonObject credentialResponseEncryption = new JsonObject();
		credentialResponseEncryption.addProperty("alg", alg);
		credentialResponseEncryption.addProperty("enc", enc);

		// Include the public key in the request
		try {
			JsonObject publicKeyJson = JsonParser.parseString(encryptionKey.toPublicJWK().toJSONString()).getAsJsonObject();
			credentialResponseEncryption.add("jwk", publicKeyJson);
		} catch (Exception e) {
			throw error("Failed to extract public key from encryption JWKS", e);
		}

		credentialRequest.add("credential_response_encryption", credentialResponseEncryption);

		logSuccess("Added credential_response_encryption to credential request",
			args("credential_response_encryption", credentialResponseEncryption));

		return env;
	}
}
