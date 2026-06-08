package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Adds two unusable encryption keys to the {@code client_metadata.jwks} that the suite (acting as the
 * verifier) advertises to the wallet, alongside the usable key. Neither can be selected by any wallet,
 * so a conformant wallet must ignore them and encrypt its response to the usable key:
 *
 * <ul>
 *   <li>A post-quantum-shaped key ({@code kty=AKP}, an ML-KEM parameter set that does not exist). This
 *       models the realistic crypto-agility scenario: a verifier advertising a post-quantum key before
 *       all wallets support it. We deliberately use a non-existent parameter set rather than a real one
 *       (e.g. ML-KEM-768) so that the key can never become usable - if it were a real algorithm, a
 *       wallet that supports it could encrypt to it and the suite (which holds no private key for it)
 *       could not decrypt, producing a false failure.</li>
 *   <li>A key with a made-up {@code kty} that no implementation will ever support.</li>
 * </ul>
 *
 * <p>Per RFC 7517 section 5, a recipient SHOULD ignore keys in a JWK Set whose values are out of the
 * supported ranges, rather than rejecting the whole set. Run this condition immediately after
 * {@code AddVP1FinalEncryptionParametersToClientMetadata} (or its VPID2 equivalent), which places the
 * usable key in the set. If the wallet behaves correctly, the suite's later {@code DecryptResponse}
 * succeeds using the usable key.
 */
public class AddUnusableEncryptionKeyToClientMetadata extends AbstractCondition {

	// A post-quantum (ML-KEM) shaped key with a non-existent parameter set, so it is realistic but can
	// never become usable. 'pub' is an arbitrary placeholder - the key type is unsupported, so it is
	// never parsed beyond kty.
	private static final String UNUSABLE_PQ_ENC_KEY = """
		{
		  "kty": "AKP",
		  "alg": "ML-KEM-9999",
		  "kid": "unusable-pq-enc-key",
		  "use": "enc",
		  "pub": "Z0FOY29uZm9ybWFuY2UtdGVzdC1wbGFjZWhvbGRlci1wdWJsaWMta2V5"
		}""";

	// A made-up key type that no JOSE implementation supports. It carries a (made-up) alg so it is
	// well-formed enough to pass structural client_metadata checks, but is skipped because its kty is
	// unparseable.
	private static final String UNUSABLE_UNKNOWN_ENC_KEY = """
		{
		  "kty": "OIDF-CONFORMANCE-UNSUPPORTED",
		  "alg": "OIDF-CONFORMANCE-UNSUPPORTED",
		  "kid": "unusable-unknown-enc-key",
		  "use": "enc"
		}""";

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject jwks = (JsonObject) env.getElementFromObject("authorization_endpoint_request", "client_metadata.jwks");
		if (jwks == null || !jwks.has("keys") || !jwks.get("keys").isJsonArray()) {
			throw error("client_metadata.jwks with a 'keys' array was not found in the authorization endpoint request. "
				+ "This condition must run after the encryption parameters have been added to client_metadata.");
		}

		JsonArray keys = jwks.getAsJsonArray("keys");
		keys.add(JsonParser.parseString(UNUSABLE_PQ_ENC_KEY).getAsJsonObject());
		keys.add(JsonParser.parseString(UNUSABLE_UNKNOWN_ENC_KEY).getAsJsonObject());

		log("Added two unusable encryption keys (a post-quantum-shaped key with a non-existent parameter "
			+ "set, and a made-up key type) to client_metadata.jwks. A conformant wallet must ignore them "
			+ "and encrypt to the usable key (RFC 7517 section 5).",
			args("client_metadata_jwks", jwks));

		return env;
	}
}
