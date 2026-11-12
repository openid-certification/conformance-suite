package net.openid.conformance.security;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The JWS algorithms the IdP's tokens are verified with: the configured ones for
 * its access tokens, and {@link #ID_TOKEN} for its ID tokens.
 * <p>
 * These have to be configuration rather than constants in the code: which algorithm
 * an IdP signs with is a deployment fact. Note that the IdP's discovery document is
 * NOT a usable source for this - {@code id_token_signing_alg_values_supported} lists
 * everything the IdP <em>can</em> do, and says nothing about access tokens.
 */
public final class IdpJwsAlgorithms {

	private IdpJwsAlgorithms() {
	}

	/**
	 * Used when {@code oidc.idp.access-token-signing-algs} is blank. Both are
	 * accepted because IdPs differ: Keycloak signs access tokens with RS256 by
	 * default even when its ID tokens are ES256.
	 */
	public static final List<SignatureAlgorithm> DEFAULT = List.of(SignatureAlgorithm.ES256, SignatureAlgorithm.RS256);

	/**
	 * Accepted on an ID token from a login through the IdP. Kept apart from
	 * {@link #DEFAULT}: an IdP signs the two kinds of token independently, so a
	 * deployment narrowing its access-token algorithms says nothing about these.
	 */
	public static final List<SignatureAlgorithm> ID_TOKEN = List.of(SignatureAlgorithm.ES256, SignatureAlgorithm.RS256);

	/**
	 * @param names comma-separated algorithm names, e.g. "ES256,RS256"; blank or null
	 *              yields {@link #DEFAULT}
	 * @return the named algorithms, in the order given, without duplicates
	 * @throws IllegalStateException if a name is not an asymmetric JWS algorithm
	 */
	public static List<SignatureAlgorithm> parse(String names) {
		if (names == null || names.isBlank()) {
			return DEFAULT;
		}

		Set<SignatureAlgorithm> algorithms = new LinkedHashSet<>();
		for (String name : names.split(",")) {
			String trimmed = name.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			// SignatureAlgorithm covers only RS*/ES*/PS*, so "none" and the HS* MAC
			// algorithms are rejected here rather than silently accepted - a token
			// signed with either must never authenticate a user.
			SignatureAlgorithm algorithm = SignatureAlgorithm.from(trimmed);
			if (algorithm == null) {
				throw new IllegalStateException("'" + trimmed + "' is not a supported JWS signature algorithm."
					+ " Expected one or more of " + supportedNames() + ", comma separated.");
			}
			algorithms.add(algorithm);
		}

		return algorithms.isEmpty() ? DEFAULT : List.copyOf(algorithms);
	}

	private static String supportedNames() {
		return Arrays.stream(SignatureAlgorithm.values()).map(SignatureAlgorithm::getName).toList().toString();
	}
}
