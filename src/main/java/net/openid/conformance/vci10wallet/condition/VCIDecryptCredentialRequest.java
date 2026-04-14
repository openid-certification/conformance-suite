package net.openid.conformance.vci10wallet.condition;

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
import net.openid.conformance.util.JWEUtil;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.vci10issuer.condition.VciErrorCode;
import net.openid.conformance.vci10issuer.util.VCICredentialErrorResponseUtil;

import java.text.ParseException;

/**
 * Decrypts an encrypted credential request received at the (emulated) credential endpoint.
 *
 * Per OID4VCI 1.0 Final Section 10, when credential request encryption is in use, the wallet
 * POSTs the credential request as a JWE with Content-Type: application/jwt. This condition
 * detects an encrypted request (by the request Content-Type), decrypts the JWE with the private
 * key from vci.credential_request_encryption_jwks, and replaces incoming_request.body /
 * body_json with the decrypted JSON so that the rest of the request validation can proceed
 * unchanged.
 *
 * If the request is not encrypted (Content-Type is not application/jwt), this condition is a
 * no-op — downstream validation will then fail if encryption was actually required.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-10">OID4VCI Section 10 - Encrypted Credential Requests and Responses</a>
 */
public class VCIDecryptCredentialRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"incoming_request", "vci"})
	@PostEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String contentType = env.getString("incoming_request", "headers.content-type");
		if (contentType == null) {
			logSuccess("No Content-Type header on credential request; leaving body unchanged");
			return env;
		}

		String normalizedContentType = contentType.toLowerCase().trim();
		if (!normalizedContentType.startsWith("application/jwt")) {
			logSuccess("Credential request Content-Type is not application/jwt; leaving body unchanged",
				args("content_type", contentType));
			return env;
		}

		String body = env.getString("incoming_request", "body");
		if (body == null || body.isBlank()) {
			String errorDescription = "Encrypted credential request (Content-Type: application/jwt) has an empty body";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
			throw error(errorDescription);
		}

		JsonObject privateJwks = (JsonObject) env.getElementFromObject("vci", "credential_request_encryption_jwks");
		if (privateJwks == null) {
			throw error("vci.credential_request_encryption_jwks is missing; "
				+ "VCIGenerateCredentialRequestEncryptionJwks must run during configuration when encryption is enabled");
		}

		JWEObject jweObject;
		try {
			jweObject = JWEObject.parse(body);
		} catch (ParseException e) {
			String errorDescription = "Failed to parse encrypted credential request as a JWE";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
			throw error(errorDescription, e, args("body", body));
		}

		JWEAlgorithm algorithm = jweObject.getHeader().getAlgorithm();

		JWKSet jwkSet;
		try {
			jwkSet = JWKUtil.parseJWKSet(privateJwks.toString());
		} catch (ParseException e) {
			throw error("Failed to parse vci.credential_request_encryption_jwks", e);
		}

		JWK decryptionKey = JWEUtil.selectAsymmetricKeyForEncryption(jwkSet, algorithm);
		if (decryptionKey == null) {
			String errorDescription = "No suitable key for decrypting the credential request was found";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
			throw error(errorDescription, args("algorithm", algorithm.getName()));
		}

		String decryptedPayload;
		try {
			JWEDecrypter decrypter = JWEUtil.createDecrypter(algorithm, decryptionKey);
			jweObject.decrypt(decrypter);
			decryptedPayload = jweObject.getPayload().toString();
		} catch (JOSEException e) {
			String errorDescription = "Failed to decrypt credential request JWE";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
			throw error(errorDescription, e, args("algorithm", algorithm.getName()));
		}

		JsonObject decryptedJson;
		try {
			decryptedJson = JsonParser.parseString(decryptedPayload).getAsJsonObject();
		} catch (JsonParseException | IllegalStateException e) {
			String errorDescription = "Decrypted credential request payload is not a JSON object";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
			throw error(errorDescription, e, args("decrypted_payload", decryptedPayload));
		}

		// Replace the request body and body_json with the decrypted JSON so that subsequent
		// validation conditions can inspect the plaintext credential request.
		env.putString("incoming_request", "body", decryptedPayload);
		env.putObject("incoming_request", "body_json", decryptedJson);

		logSuccess("Decrypted credential request JWE",
			args("alg", algorithm.getName(),
				"kid", decryptionKey.getKeyID(),
				"credential_request", decryptedJson));

		return env;
	}
}
