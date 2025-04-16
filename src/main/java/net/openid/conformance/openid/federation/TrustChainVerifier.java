package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.testmodule.OIDFJSON;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@SuppressWarnings("UnusedVariable")
public class TrustChainVerifier {

	public static VerificationResult verifyTrustChain(String subjectEntity, String trustAnchor, List<String> trustChain) {
		return verifyTrustChain(subjectEntity, trustAnchor, trustChain, false);
	}

		public static VerificationResult verifyTrustChain(String subjectEntity, String trustAnchor, List<String> trustChain, boolean ignoreExpAndIat) {

		if (trustChain == null || trustChain.isEmpty()) {
			return new VerificationResult(false,"Trust chain is null or empty");
		}

		// There should be at minimum three tokens (subject, a subordinate statement about subject from TA, and TA).
		if (trustChain.size() < 3) {
			return new VerificationResult(false,"Trust chain must contain at least three tokens");
		}

		// Parse all JWT payloads and validate each token’s signature/claims.
		JsonObject[] payloads = new JsonObject[trustChain.size()];
		for (int i = 0; i < trustChain.size(); i++) {
			JsonObject payload = extractPayload(trustChain.get(i));

			if (payload == null) {
				return new VerificationResult(false,"Failed to extract payload from JWT at index " + i);
			}

			JsonElement jwks = null;
			if (i == 0 || i == trustChain.size() - 1) {
				jwks = payload.get("jwks");
			} else {
				JsonObject nextPayload = extractPayload(trustChain.get(i + 1));
				jwks = nextPayload.get("jwks");
			}

			if (jwks == null) {
				return new VerificationResult(false, "Missing 'jwks' in the payload at index " + i);
			}

			String jwt = trustChain.get(i);
			try {
				if (!verifySignature(jwks, jwt)) {
					return new VerificationResult(false, "Failed to verify JWT signature with the provided key at index " + i);
				}
			} catch (Exception e) {
				return new VerificationResult(false, e.getMessage());
			}

			if (!ignoreExpAndIat && !verifyClaims(payload)) {
				return new VerificationResult(false,"Claim (iat/exp) validation failed for token at index " + i);
			}

			payloads[i] = payload;
		}

		// Validate token 0: Subject’s self-signed entity configuration.
		JsonObject subjectEntityConfiguration = payloads[0];
		String subjectEntityIss = subjectEntityConfiguration.has("iss") ? OIDFJSON.getString(subjectEntityConfiguration.get("iss")) : null;
		String subjectEntitySub = subjectEntityConfiguration.has("sub") ? OIDFJSON.getString(subjectEntityConfiguration.get("sub")) : null;
		if (subjectEntityIss == null || subjectEntitySub == null) {
			return new VerificationResult(false,"Subject entity configuration is missing 'iss' or 'sub'");
		}
		if (!subjectEntityIss.equals(subjectEntitySub)) {
			return new VerificationResult(false,"Subject entity configuration is not self-signed");
		}
		if (!subjectEntitySub.equals(subjectEntity)) {
			return new VerificationResult(false,"Subject entity identifier in entity configuration does not match the expected subject entity identifier");
		}

		// For tokens 1 to n-2: each subordinate statement's "sub" must match the previous token's "iss".
		for (int i = 2; i < trustChain.size() - 1; i++) {
			JsonObject current = payloads[i];
			JsonObject previous = payloads[i - 1];
			String currentSubject = current.has("sub") ? OIDFJSON.getString(current.get("sub")) : null;
			String previousIssuer = previous.has("iss") ? OIDFJSON.getString(previous.get("iss")) : null;
			if (currentSubject == null || previousIssuer == null) {
				return new VerificationResult(false,"Token at index " + i + " is missing required fields");
			}
			if (!currentSubject.equals(previousIssuer)) {
				return new VerificationResult(false,"Token at index " + i + " subject does not match previous token's issuer");
			}
		}

		// Final token: Trust Anchor's self-signed entity configuration.
		int lastIndex = trustChain.size() - 1;
		JsonObject trustAnchorConfiguration = payloads[lastIndex];
		String taIssuer = trustAnchorConfiguration.has("iss") ? OIDFJSON.getString(trustAnchorConfiguration.get("iss")) : null;
		String taSubject = trustAnchorConfiguration.has("sub") ? OIDFJSON.getString(trustAnchorConfiguration.get("sub")) : null;
		if (taIssuer == null || taSubject == null) {
			return new VerificationResult(false,"Trust Anchor configuration is missing 'iss' or 'sub'");
		}
		if (!taIssuer.equals(taSubject)) {
			return new VerificationResult(false,"Trust Anchor configuration is not self-signed");
		}
		if (!taIssuer.equals(trustAnchor)) {
			return new VerificationResult(false,"Trust Anchor identifier does not match the expected trust anchor");
		}
		// The trust anchor's identifier should match the issuer of the previous token.
		JsonObject previousToken = payloads[lastIndex - 1];
		String previousIssuer = previousToken.has("iss") ? OIDFJSON.getString(previousToken.get("iss")) : null;
		if (!taSubject.equals(previousIssuer)) {
			return new VerificationResult(false,"Trust Anchor identifier does not match the issuer of the previous token");
		}

		return new VerificationResult(true);
	}

	private static boolean verifySignature(JsonElement jwks, String jwt) throws ParseException, JOSEException {
		VerifyEntityStatmentSignature signatureVerifier = new VerifyEntityStatmentSignature();
		JWKSet jwkSet = JWKSet.parse(jwks.toString());
		SignedJWT signedJWT = SignedJWT.parse(jwt);
		return signatureVerifier.verifySignature(signedJWT, jwkSet);
	}

	private static boolean verifyClaims(JsonObject payload) {
		Instant now = Instant.now();

		Instant iat = Instant.ofEpochSecond(OIDFJSON.getLong(payload.get("iat")));
		Instant exp = Instant.ofEpochSecond(OIDFJSON.getLong(payload.get("exp")));

		return !now.plusMillis(ValidateEntityStatementIat.TIME_SKEW_MILLIS).isBefore(iat)
			&& !now.minusMillis(ValidateEntityStatementExp.TIME_SKEW_MILLIS).isAfter(exp);
	}

	private static JsonObject extractPayload(String jwt) {
		String[] parts = jwt.split("\\.");
		if (parts.length != 3) {
			return null;
		}
		String payloadPart = parts[1];

		byte[] decodedBytes = Base64.getUrlDecoder().decode(payloadPart);
		String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
		return JsonParser.parseString(payloadJson).getAsJsonObject();
	}


	public static class VerificationResult {

		private final boolean isVerified;
		private final String error;

		public VerificationResult(boolean isVerified) {
			this(isVerified, null);
		}

		public VerificationResult(boolean isVerified, String error) {
			this.isVerified = isVerified;
			this.error = error;
		}

		public boolean isVerified() {
			return isVerified;
		}

		public String getError() {
			return error;
		}
	}
}
