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
 * decrypts the JWE with the private key from vci.credential_request_encryption_jwks and
 * replaces incoming_request.body / body_json with the decrypted JSON so that the rest of the
 * request validation can proceed unchanged.
 *
 * The condition is only invoked when the test variant requires encryption (the credential
 * request also carries credential_response_encryption, so per Section 8.2 the request MUST
 * have been encrypted). It therefore fails the test if the incoming Content-Type is anything
 * other than application/jwt or if the body is not a valid JWE — silently letting an
 * unencrypted body through would let an issuer test pass without actually encrypting.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-10">OID4VCI Section 10 - Encrypted Credential Requests and Responses</a>
 */
public class VCIDecryptCredentialRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"incoming_request", "vci"})
	@PostEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String contentType = env.getString("incoming_request", "headers.content-type");
		if (contentType == null || contentType.isBlank()) {
			String errorDescription = "Credential request is missing a Content-Type header; "
				+ "an encrypted credential request MUST use Content-Type: application/jwt per OID4VCI 1.0 Section 10";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
			throw error(errorDescription);
		}

		String normalizedContentType = contentType.toLowerCase().trim();
		if (!normalizedContentType.startsWith("application/jwt")) {
			String errorDescription = "Credential request Content-Type is not application/jwt; "
				+ "credential_response_encryption was requested so per OID4VCI 1.0 Section 8.2 "
				+ "the credential request MUST be encrypted as a JWE with Content-Type: application/jwt";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
			throw error(errorDescription, args("content_type", contentType));
		}

		String body = env.getString("incoming_request", "body");
		if (body == null || body.isBlank()) {
			String errorDescription = "Encrypted credential request has an empty body";
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
		String kid = jweObject.getHeader().getKeyID();

		JWKSet jwkSet;
		try {
			jwkSet = JWKUtil.parseJWKSet(privateJwks.toString());
		} catch (ParseException e) {
			throw error("Failed to parse vci.credential_request_encryption_jwks", e);
		}

		JWK decryptionKey = JWEUtil.selectAsymmetricKeyForEncryption(jwkSet, algorithm, kid);
		if (decryptionKey == null) {
			String errorDescription = "No suitable key for decrypting the credential request was found";
			VCICredentialErrorResponseUtil.updateCredentialErrorResponseInEnv(env, VciErrorCode.INVALID_CREDENTIAL_REQUEST, errorDescription);
			throw error(errorDescription, args("algorithm", algorithm.getName(), "kid", kid));
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

		JsonObject credentialRequestJwe;
		try {
			credentialRequestJwe = JWEUtil.jweStringToJsonObjectForEnvironment(body, decryptedJson);
		} catch (ParseException e) {
			throw error("Failed to parse credential request JWE for logging", e);
		}

		logSuccess("Decrypted credential request JWE",
			args("credential_request_jwe", credentialRequestJwe));

		return env;
	}
}
