package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWEUtil;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;

/**
 * Encrypts the credential request body as a JWE per OID4VCI Section 10.
 *
 * Per OID4VCI 1.0 Final Section 8.2, Credential Request encryption MUST be used when
 * credential_response_encryption is included. The wallet selects an encryption key from
 * the issuer's credential_request_encryption.jwks metadata and serializes the credential
 * request as a JWE using media type application/jwt.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-10">OID4VCI Section 10 - Encrypted Credential Requests and Responses</a>
 */
public class VCIEncryptCredentialRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"vci", "resource_endpoint_request_headers"}, strings = "resource_request_entity")
	@PostEnvironment(strings = "resource_request_entity")
	public Environment evaluate(Environment env) {

		String requestBody = env.getString("resource_request_entity");

		JsonElement jwksEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_request_encryption.jwks");
		if (jwksEl == null || !jwksEl.isJsonObject()) {
			throw error("credential_issuer_metadata.credential_request_encryption.jwks is missing or not an object; "
				+ "credential request encryption requires the issuer to publish an encryption JWKS");
		}

		JWKSet jwkSet;
		try {
			jwkSet = JWKUtil.parseJWKSet(jwksEl.toString());
		} catch (ParseException e) {
			throw error("Failed to parse credential_issuer_metadata.credential_request_encryption.jwks", e,
				args("jwks", jwksEl));
		}

		if (jwkSet.getKeys().isEmpty()) {
			throw error("credential_issuer_metadata.credential_request_encryption.jwks must contain at least one key",
				args("jwks", jwksEl));
		}

		// Per OID4VCI 1.0 Final Section 10, the alg parameter MUST be present on the selected key,
		// the JWE alg used MUST equal the JWK alg, and the key is used for JWE key agreement.
		// Pick the first key whose alg parses to an asymmetric JWE algorithm (RSA / ECDH-ES family)
		// and whose use is "enc" or absent — this skips signing keys or keys with unrelated algs.
		JWK encryptionKey = jwkSet.getKeys().stream()
			.filter(this::isUsableForJweKeyAgreement)
			.findFirst()
			.orElse(null);
		if (encryptionKey == null) {
			throw error("No usable encryption key found in credential_issuer_metadata.credential_request_encryption.jwks; "
				+ "expected a key with use=enc (or absent), an asymmetric JWE 'alg' (RSA or ECDH-ES family), "
				+ "per OID4VCI 1.0 Section 10",
				args("jwks", jwksEl));
		}

		// Per OID4VCI 1.0 Final Section 12.2.4, every JWK in credential_request_encryption.jwks
		// MUST have a kid that uniquely identifies the key; per Section 10, the JWE MUST then carry
		// that kid in its header. Fail loudly if the issuer's metadata is missing the kid rather
		// than silently producing a JWE without one.
		if (encryptionKey.getKeyID() == null || encryptionKey.getKeyID().isEmpty()) {
			throw error("Selected key in credential_issuer_metadata.credential_request_encryption.jwks "
				+ "is missing a 'kid'; per OID4VCI 1.0 Section 12.2.4 each JWK in the credential request "
				+ "encryption JWKS MUST have a kid that uniquely identifies the key",
				args("jwks", jwksEl, "selected_key", encryptionKey.toJSONString()));
		}

		String alg = encryptionKey.getAlgorithm().getName();

		// enc is obtained from context: use the first value from enc_values_supported. The
		// presence and non-emptiness of this array is already guaranteed by the encrypted-variant
		// skip gate in AbstractVCIIssuerTestModule.onConfigure; throw here if it's not so the
		// precondition is enforced in one place rather than papered over with a default.
		JsonElement encValuesEl = env.getElementFromObject("vci",
			"credential_issuer_metadata.credential_request_encryption.enc_values_supported");
		if (encValuesEl == null || !encValuesEl.isJsonArray() || encValuesEl.getAsJsonArray().isEmpty()) {
			throw error("credential_issuer_metadata.credential_request_encryption.enc_values_supported "
				+ "is missing, not an array, or empty; per OID4VCI 1.0 Section 12.2.4 it is REQUIRED",
				args("enc_values_supported", encValuesEl));
		}
		String enc = OIDFJSON.getString(encValuesEl.getAsJsonArray().get(0));

		JWEAlgorithm jweAlgorithm = JWEAlgorithm.parse(alg);
		EncryptionMethod encryptionMethod = EncryptionMethod.parse(enc);

		String encryptedRequest;
		try {
			JWEEncrypter encrypter = JWEUtil.createEncrypter(encryptionKey);

			// Per OID4VCI 1.0 Section 10, when the selected public key has a kid (which
			// Section 12.2.4 requires), the JWE MUST carry the same kid in its header.
			JWEHeader.Builder headerBuilder = new JWEHeader.Builder(jweAlgorithm, encryptionMethod)
				.contentType("json")
				.keyID(encryptionKey.getKeyID());

			JWEObject jweObject = new JWEObject(
				headerBuilder.build(),
				new Payload(requestBody)
			);

			jweObject.encrypt(encrypter);
			encryptedRequest = jweObject.serialize();

		} catch (JOSEException e) {
			throw error("Failed to encrypt credential request", e,
				args("alg", alg, "enc", enc));
		}

		env.putString("resource_request_entity", encryptedRequest);

		// Per OID4VCI 1.0 Section 10, the media type MUST be set to application/jwt.
		env.putString("resource_endpoint_request_headers", "Content-Type", "application/jwt");

		logSuccess("Encrypted credential request as JWE",
			args("alg", alg, "enc", enc, "kid", encryptionKey.getKeyID(),
				"credential_request_body", requestBody,
				"encrypted_credential_request", encryptedRequest));

		return env;
	}

	protected boolean isUsableForJweKeyAgreement(JWK candidate) {
		if (candidate.getAlgorithm() == null) {
			return false;
		}
		if (candidate.getKeyUse() != null && !KeyUse.ENCRYPTION.equals(candidate.getKeyUse())) {
			return false;
		}
		JWEAlgorithm candidateAlg = JWEAlgorithm.parse(candidate.getAlgorithm().getName());
		return JWEAlgorithm.Family.ASYMMETRIC.contains(candidateAlg);
	}
}
