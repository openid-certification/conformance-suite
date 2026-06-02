package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Adds two unusable signing keys to the published {@code server_public_jwks} (served at the suite's
 * jwks_uri), leaving the real signing key in {@code server_jwks} untouched. Neither can be used by any
 * relying party, so a conformant RP must ignore them and verify the id_token using the real key:
 *
 * <ul>
 *   <li>A post-quantum-shaped key ({@code kty=AKP}, an ML-DSA parameter set that does not exist). This
 *       models the realistic crypto-agility scenario: an Issuer advertising a post-quantum key before
 *       all relying parties support it. We deliberately use a non-existent parameter set rather than a
 *       real one (e.g. ML-DSA-65) so the key can never become usable.</li>
 *   <li>A key with a made-up {@code kty} that no implementation will ever support.</li>
 * </ul>
 *
 * <p>Both are given distinct {@code kid}s so they are unambiguously not the key used to sign the
 * id_token. Per RFC 7517 section 5, a relying party SHOULD ignore keys in a JWK Set whose values are
 * out of the supported ranges, rather than rejecting the whole set.
 */
public class AddUnparseableKeyToServerPublicJwks extends AbstractCondition {

	// A post-quantum (ML-DSA) shaped key with a non-existent parameter set, so it is realistic but can
	// never become usable. 'pub' is an arbitrary placeholder - the key type is unsupported.
	private static final String UNUSABLE_PQ_SIG_KEY = """
		{
		  "kty": "AKP",
		  "alg": "ML-DSA-9999",
		  "kid": "unusable-pq-sig-key",
		  "use": "sig",
		  "pub": "Z0FOY29uZm9ybWFuY2UtdGVzdC1wbGFjZWhvbGRlci1wdWJsaWMta2V5"
		}""";

	// A made-up key type that no JOSE implementation supports. It carries a (made-up) alg so it is
	// well-formed enough to pass structural checks, but is skipped because its kty is unparseable.
	private static final String UNUSABLE_UNKNOWN_SIG_KEY = """
		{
		  "kty": "OIDF-CONFORMANCE-UNSUPPORTED",
		  "alg": "OIDF-CONFORMANCE-UNSUPPORTED",
		  "kid": "unusable-unknown-sig-key",
		  "use": "sig"
		}""";

	@Override
	@PreEnvironment(required = "server_public_jwks")
	@PostEnvironment(required = "server_public_jwks")
	public Environment evaluate(Environment env) {

		JsonObject publicJwks = env.getObject("server_public_jwks");
		if (publicJwks == null || !publicJwks.has("keys") || !publicJwks.get("keys").isJsonArray()) {
			throw error("server_public_jwks with a 'keys' array was not found. This condition must run "
				+ "after the server JWKS have been generated.");
		}

		JsonArray keys = publicJwks.getAsJsonArray("keys");
		keys.add(JsonParser.parseString(UNUSABLE_PQ_SIG_KEY).getAsJsonObject());
		keys.add(JsonParser.parseString(UNUSABLE_UNKNOWN_SIG_KEY).getAsJsonObject());

		env.putObject("server_public_jwks", publicJwks);

		log("Added two unusable signing keys (a post-quantum-shaped key with a non-existent parameter "
			+ "set, and a made-up key type) to the published server_public_jwks. A conformant relying "
			+ "party must ignore them and verify the id_token using the real key (RFC 7517 section 5).",
			args("server_public_jwks", publicJwks));

		return env;
	}
}
