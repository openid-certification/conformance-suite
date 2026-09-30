package net.openid.conformance.openid.ssf.conditions.events;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.AbstractLenientJwksCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.util.List;

/**
 * CAEP Interop Profile 2.6: "All events MUST be signed using the RS256
 * algorithm with a minimum of 2048-bit keys."
 * <p>
 * Resolves the SET's signing key (via the {@code kid} header) from the
 * transmitter's JWKS and asserts it is an RSA key of at least 2048 bits.
 * The RS256 algorithm itself is asserted separately by
 * {@link OIDSSFEnsureEventSignedWithRsa256}. The JWKS is parsed leniently,
 * as the signature check does: a key the JOSE library cannot parse is logged
 * and skipped, so it cannot fail a SET signed with a usable key.
 */
public class OIDSSFEnsureEventSignerRsaKeySizeAtLeast2048Bits extends AbstractLenientJwksCondition {

	protected static final int MIN_RSA_KEY_SIZE_BITS = 2048;

	@Override
	@PreEnvironment(required = {"set_token", "server_jwks"})
	public Environment evaluate(Environment env) {

		JWKSet jwkSet;
		try {
			jwkSet = parseJwksLenientlyLoggingSkips(env.getObject("server_jwks").toString(), "transmitter");
		} catch (ParseException e) {
			throw error("Could not parse the transmitter JWKS", e);
		}

		String kid = env.getString("set_token", "header.kid");

		// Candidate signing keys, mirroring how AbstractVerifyJwsSignature selects keys:
		// match the kid when present (skipping keys of other types that share the kid),
		// and consider every signature-capable RSA key when no kid is given.
		List<RSAKey> candidateKeys = jwkSet.getKeys().stream()
			.filter(key -> kid == null || kid.equals(key.getKeyID()))
			.filter(key -> KeyType.RSA.equals(key.getKeyType()))
			.filter(key -> key.getKeyUse() == null || KeyUse.SIGNATURE.equals(key.getKeyUse()))
			.map(RSAKey.class::cast)
			.toList();

		if (candidateKeys.isEmpty()) {
			throw error(kid != null
					? "Could not find an RSA signing key with the SET's kid in the transmitter JWKS; under the CAEP Interop Profile SETs must be signed with RS256, so the key must be an RSA key."
					: "Could not find any RSA signing key in the transmitter JWKS; under the CAEP Interop Profile SETs must be signed with RS256, so the key must be an RSA key.",
				args("kid", kid, "jwks_kids", jwkSet.getKeys().stream().map(JWK::getKeyID).toList()));
		}

		// Without a kid the exact signer may be ambiguous - the check passes only when
		// every candidate RSA signing key meets the minimum size.
		List<String> keysBelowMinimum = candidateKeys.stream()
			.filter(key -> key.size() < MIN_RSA_KEY_SIZE_BITS)
			.map(key -> (key.getKeyID() == null ? "<no kid>" : key.getKeyID()) + " (" + key.size() + " bits)")
			.toList();
		if (!keysBelowMinimum.isEmpty()) {
			throw error("The transmitter JWKS contains candidate RSA signing key(s) below the minimum of 2048 bits the CAEP Interop Profile requires",
				args("kid", kid, "keys_below_minimum", keysBelowMinimum, "min_key_size_bits", MIN_RSA_KEY_SIZE_BITS));
		}

		logSuccess("SET signing key(s) are RSA keys with a sufficient key size",
			args("kid", kid, "candidate_key_count", candidateKeys.size()));

		return env;
	}
}
