package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * Adds five unusable signing keys to the published {@code server_public_jwks} (served at the suite's
 * jwks_uri), leaving the real signing key in {@code server_jwks} untouched. None can be used by any
 * relying party, so a conformant RP must ignore them and verify the id_token using the real key, per
 * RFC 7517 section 5 (a recipient SHOULD ignore keys within a JWK Set that use unknown key types or
 * parameter values that are not understood, rather than rejecting the whole set).
 *
 * <p>Between them the keys cover each point at which a relying party may have to skip a key:
 *
 * <ul>
 *   <li>A post-quantum-shaped key ({@code kty=AKP}, an ML-DSA parameter set that does not exist). This
 *       models the realistic crypto-agility scenario: an Issuer advertising a post-quantum key before
 *       all relying parties support it. We deliberately use a non-existent parameter set rather than a
 *       real one (e.g. ML-DSA-65) so the key can never become usable.</li>
 *   <li>A key with a made-up {@code kty} that no implementation will ever support. Both this and the
 *       AKP key must be skipped when the JWK Set is parsed.</li>
 *   <li>An RSA key (valid key material) with an {@code alg} that does not exist — parseable, but must
 *       be skipped when a verification key is selected.</li>
 *   <li>An EC key (valid P-256 key material) with an {@code alg} that does not exist — as above, for
 *       the EC key hierarchy.</li>
 *   <li>An EC key with a {@code crv} that does not exist — parseable as JSON but the key material
 *       cannot be imported, so it must be skipped at key construction time.</li>
 * </ul>
 *
 * <p>All are given distinct {@code kid}s so they are unambiguously not the key used to sign the
 * id_token. The RSA and EC key material is taken from the well-known example keys in RFC 7517
 * Appendix A.1, making clear the values are test vectors and not real keys.
 */
public class AddUnusableKeysToServerPublicJwks extends AbstractCondition {

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
	private static final String UNUSABLE_UNKNOWN_KTY_KEY = """
		{
		  "kty": "OIDF-CONFORMANCE-UNSUPPORTED",
		  "alg": "OIDF-CONFORMANCE-UNSUPPORTED",
		  "kid": "unusable-unknown-kty-key",
		  "use": "sig"
		}""";

	// A parseable RSA key (modulus/exponent from RFC 7517 Appendix A.1) whose alg does not exist, so it
	// can never be selected to verify a signature.
	private static final String UNUSABLE_RSA_UNKNOWN_ALG_KEY = """
		{
		  "kty": "RSA",
		  "alg": "RS9999",
		  "kid": "unusable-rsa-unknown-alg-key",
		  "use": "sig",
		  "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw",
		  "e": "AQAB"
		}""";

	// A parseable EC key (P-256 coordinates from RFC 7517 Appendix A.1) whose alg does not exist.
	private static final String UNUSABLE_EC_UNKNOWN_ALG_KEY = """
		{
		  "kty": "EC",
		  "crv": "P-256",
		  "alg": "ES9999",
		  "kid": "unusable-ec-unknown-alg-key",
		  "use": "sig",
		  "x": "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
		  "y": "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM"
		}""";

	// An EC key with a non-existent crv; parseable as JSON but the key material cannot be imported. The
	// coordinates are placeholders (the curve is unknown, so no coordinates could be valid). No alg, so
	// implementations that filter on alg before importing still exercise the unknown-curve path.
	private static final String UNUSABLE_EC_UNKNOWN_CRV_KEY = """
		{
		  "kty": "EC",
		  "crv": "P-9999",
		  "kid": "unusable-ec-unknown-crv-key",
		  "use": "sig",
		  "x": "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
		  "y": "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM"
		}""";

	private static final List<String> UNUSABLE_KEYS = List.of(
		UNUSABLE_PQ_SIG_KEY,
		UNUSABLE_UNKNOWN_KTY_KEY,
		UNUSABLE_RSA_UNKNOWN_ALG_KEY,
		UNUSABLE_EC_UNKNOWN_ALG_KEY,
		UNUSABLE_EC_UNKNOWN_CRV_KEY);

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
		for (String unusableKey : UNUSABLE_KEYS) {
			keys.add(JsonParser.parseString(unusableKey).getAsJsonObject());
		}

		env.putObject("server_public_jwks", publicJwks);

		log("Added five unusable signing keys to the published server_public_jwks: a post-quantum-shaped "
			+ "key with a non-existent parameter set, a made-up key type, an RSA key with an unknown alg, "
			+ "an EC key with an unknown alg, and an EC key with an unknown crv. A conformant relying "
			+ "party must ignore them and verify the id_token using the real key (RFC 7517 section 5).",
			args("server_public_jwks", publicJwks));

		return env;
	}
}
