package net.openid.conformance.condition;

import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for conditions that select a usable key from a counterparty's JWK set.
 *
 * Provides lenient parsing that skips keys the JOSE library cannot handle (e.g. unsupported curves
 * like Brainpool, or future post-quantum key types) and logs each skipped key - with the reason the
 * JOSE library rejected it - into the test log. This mirrors how a real recipient behaves (RFC 7517
 * section 5: ignore keys you cannot use) while keeping the fact that the counterparty published an
 * unusable key visible in the results, rather than silently dropping it.
 */
public abstract class AbstractLenientJwksCondition extends AbstractCondition {

	/**
	 * Parse a counterparty's JWK set leniently, logging any keys that could not be parsed.
	 *
	 * @param jwksString the JWK set as a JSON string
	 * @param jwksName a short name for the JWK set, used in the log message (e.g. "client", "server")
	 * @return a JWK set containing only the keys that parsed successfully (possibly empty)
	 * @throws ParseException if the JSON is not a valid JWK set object (missing "keys" array)
	 */
	protected JWKSet parseJwksLenientlyLoggingSkips(String jwksString, String jwksName) throws ParseException {
		List<JWKUtil.SkippedJwk> skippedKeys = new ArrayList<>();
		JWKSet jwkSet = JWKUtil.parseJWKSetLeniently(jwksString, skippedKeys);
		for (JWKUtil.SkippedJwk skipped : skippedKeys) {
			log("Ignoring a key in the " + jwksName + " JWKS that the JOSE library cannot parse "
					+ "(e.g. unsupported curve or key type)",
				args("key", skipped.keyJson(), "reason", skipped.reason()));
		}
		return jwkSet;
	}
}
