package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

/**
 * Verifies the Token Status List JWT signature outside HAIP with the key that
 * {@link net.openid.conformance.sequence.client.ResolveStatusListTokenVerificationKey} resolved,
 * taken from the first of these that is present:
 * <ol>
 *   <li>{@code status_list_token_verification_key}: a key from the token's JOSE header or the
 *       referenced credential ({@link ResolveStatusListTokenKeyFromJwtOrCredential})</li>
 *   <li>{@code jwt_vc_issuer_metadata_jwks}: the keys of the issuer's JWT VC Issuer Metadata</li>
 *   <li>{@code status_list_token_server_jwks}: the keys the authorization server publishes at
 *       its {@code jwks_uri}</li>
 * </ol>
 *
 * This is a signature check only. No trust anchor is mandated outside HAIP, so a passing check
 * shows that the token was signed with the resolved key, not that the key belongs to the Status
 * Issuer: a key from the token's own header is self-asserted, and an authorization server key
 * identifies the authorization server rather than the Status Issuer.
 *
 * Skips when no status list fetch was performed (credential had no status claim).
 */
public class VerifyStatusListTokenSignature extends AbstractVerifyJwsSignature {

	private static final String TOKEN_NAME = "status list token";

	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObject("status_list_token")) {
			log("No parsed status list token in environment, skipping signature verification");
			return env;
		}

		String statusListTokenJwtString = env.getString("status_list_token", "value");
		if (statusListTokenJwtString == null) {
			throw error("status_list_token has no raw JWT value");
		}

		JsonObject verificationKey = env.getObject("status_list_token_verification_key");
		if (verificationKey != null && verificationKey.has("jwks") && verificationKey.get("jwks").isJsonObject()) {
			verifyWith(statusListTokenJwtString, verificationKey.getAsJsonObject("jwks"),
				env.getString("status_list_token_verification_key", "source"));
			return env;
		}

		if (env.containsObject("jwt_vc_issuer_metadata_jwks")) {
			verifyWith(statusListTokenJwtString, env.getObject("jwt_vc_issuer_metadata_jwks"),
				"JWT VC Issuer Metadata");
			return env;
		}

		if (env.containsObject("status_list_token_server_jwks")) {
			log("The status list token is verified with the authorization server's keys. These identify the authorization server, which is not necessarily the Status Issuer.");
			verifyWith(statusListTokenJwtString, env.getObject("status_list_token_server_jwks"),
				"authorization server jwks_uri");
			return env;
		}

		throw error("Unable to verify the status list token signature because no verification key could be resolved. "
				+ "The JOSE header carries neither 'jwk' nor 'x5c', 'kid' is not a did:jwk, the referenced credential "
				+ "does not embed a key with the same 'kid', no usable JWT VC Issuer Metadata was found for the issuer, "
				+ "and no keys could be fetched from a 'jwks_uri' in the authorization server metadata",
			args("header", env.getElementFromObject("status_list_token", "header"),
				"iss", env.getElementFromObject("status_list_token", "claims.iss")));
	}

	private void verifyWith(String token, JsonObject jwks, String source) {
		log("Verifying the status list token signature using the " + source
				+ ". This checks the signature only, not whether the key is trusted for the Status Issuer.",
			args("jwks", jwks));
		verifyJwsSignature(token, jwks, TOKEN_NAME, false, source);
	}
}
