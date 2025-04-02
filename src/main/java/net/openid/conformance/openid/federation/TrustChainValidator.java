package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@SuppressWarnings("UnusedVariable")
public class TrustChainValidator {

	/**
	 * Validates a trust chain represented as a List of JWT strings. The chain is assumed to be ordered as:
	 * <p>
	 * 1. Entity Configuration for the RP (self-signed),
	 * 2. Subordinate Statement about the RP (issued by Organization A),
	 * 3. Subordinate Statement about Organization A (issued by the Trust Anchor),
	 * 4. Entity Configuration for the Trust Anchor (self-signed).
	 *
	 * @param trustChain   A list of JWT strings representing the trust chain.
	 * @param relyingParty The expected identifier for the RP.
	 * @return ValidationResult object containing the result of the trust chain validation.
	 */
	public static ValidationResult validateTrustChain(List<String> trustChain, String relyingParty) {

		if (trustChain == null || trustChain.isEmpty()) {
			return new ValidationResult("Trust chain is null or empty.");
		}

		// There should be at least two tokens (RP and Trust Anchor) even though a real chain usually has more.
		if (trustChain.size() < 3) {
			return new ValidationResult("Trust chain must contain at least two tokens.");
		}

		// Parse all JWT payloads and validate each token’s signature/claims.
		JsonObject[] payloads = new JsonObject[trustChain.size()];
		for (int i = 0; i < trustChain.size(); i++) {
			JsonObject payload = extractPayload(trustChain.get(i));
			if (payload == null) {
				return new ValidationResult("Failed to extract payload from JWT at index " + i);
			}
			if (!verifySignature(trustChain.get(i))) {
				return new ValidationResult("Signature verification failed for token at index " + i);
			}
			if (!verifyClaims(payload)) {
				return new ValidationResult("Claim validation failed for token at index " + i);
			}
			payloads[i] = payload;
		}

		// Validate token 0: RP’s self-signed entity configuration.
		JsonObject rpConfig = payloads[0];
		String rpIssuer = rpConfig.has("iss") ? rpConfig.get("iss").getAsString() : null;
		String rpSubject = rpConfig.has("sub") ? rpConfig.get("sub").getAsString() : null;
		if (rpIssuer == null || rpSubject == null) {
			return new ValidationResult("RP configuration is missing 'iss' or 'sub'.");
		}
		if (!rpIssuer.equals(rpSubject)) {
			return new ValidationResult("RP configuration is not self-signed.");
		}
		if (!rpSubject.equals(relyingParty)) {
			return new ValidationResult("RP identifier does not match the relying party.");
		}

		// Token 1: Subordinate statement about the RP (issued by Organization A).
		if (trustChain.size() >= 2) {
			JsonObject subordinateRP = payloads[1];
			String subordinateRPSubject = subordinateRP.has("sub") ? subordinateRP.get("sub").getAsString() : null;
			if (subordinateRPSubject == null) {
				return new ValidationResult("Subordinate statement about RP is missing 'sub'.");
			}
			// This subordinate statement should be about the RP.
			if (!subordinateRPSubject.equals(rpSubject)) {
				return new ValidationResult("Subordinate statement about RP does not match the RP identifier.");
			}
		}

		// For tokens 2 to n-2: each subordinate statement's "sub" must match the previous token's "iss".
		for (int i = 2; i < trustChain.size() - 1; i++) {
			JsonObject current = payloads[i];
			JsonObject previous = payloads[i - 1];
			String currentSubject = current.has("sub") ? current.get("sub").getAsString() : null;
			String previousIssuer = previous.has("iss") ? previous.get("iss").getAsString() : null;
			if (currentSubject == null || previousIssuer == null) {
				return new ValidationResult("Token at index " + i + " is missing required fields.");
			}
			if (!currentSubject.equals(previousIssuer)) {
				return new ValidationResult("Token at index " + i + " subject does not match previous token's issuer.");
			}
		}

		// Final token: Trust Anchor's self-signed entity configuration.
		int lastIndex = trustChain.size() - 1;
		JsonObject trustAnchorConfig = payloads[lastIndex];
		String taIssuer = trustAnchorConfig.has("iss") ? trustAnchorConfig.get("iss").getAsString() : null;
		String taSubject = trustAnchorConfig.has("sub") ? trustAnchorConfig.get("sub").getAsString() : null;
		if (taIssuer == null || taSubject == null) {
			return new ValidationResult("Trust Anchor configuration is missing 'iss' or 'sub'.");
		}
		if (!taIssuer.equals(taSubject)) {
			return new ValidationResult("Trust Anchor configuration is not self-signed.");
		}
		// The trust anchor's identifier should match the issuer of the previous token.
		JsonObject previousToken = payloads[lastIndex - 1];
		String previousIssuer = previousToken.has("iss") ? previousToken.get("iss").getAsString() : null;
		if (!taSubject.equals(previousIssuer)) {
			return new ValidationResult("Trust Anchor identifier does not match the issuer of the previous token.");
		}

		return new ValidationResult();
	}

	/**
	 * Extracts the JSON payload from a JWT string.
	 *
	 * @param jwt A JWT in compact string format ("header.payload.signature").
	 * @return A JsonObject representing the payload, or null if extraction fails.
	 */
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

	/**
	 * Verifies the signature of the JWT.
	 * <p>
	 * In a real implementation, you would use a JWT library (such as Nimbus JOSE+JWT)
	 * to verify the token’s signature.
	 *
	 * @param jwt The JWT string.
	 * @return true if the signature is valid; false otherwise.
	 */
	private static boolean verifySignature(String jwt) {
		// TODO: Implement JWT signature verification logic.
		return true; // Placeholder.
	}

	/**
	 * Verifies token claims such as expiration and not-before.
	 *
	 * @param payload The JSON payload extracted from the JWT.
	 * @return true if the token's claims are valid; false otherwise.
	 */
	private static boolean verifyClaims(JsonObject payload) {
		// TODO: Implement claim validations.
		return true; // Placeholder.
	}

	public static class ValidationResult {

		private final boolean isValid;
		private final String error;

		public ValidationResult() {
			this(true, null);
		}

		public ValidationResult(String error) {
			this(false, error);
		}

		public ValidationResult(boolean isValid, String error) {
			this.isValid = isValid;
			this.error = error;
		}

		public boolean isValid() {
			return isValid;
		}

		public String getErrors() {
			return error;
		}
	}
}
