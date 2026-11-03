package net.openid.conformance.security;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Parses the configured JWS algorithm names used to verify tokens issued by the IdP.
 * <p>
 * These have to be configuration rather than constants in the code: which algorithm
 * an IdP signs with is a deployment fact. Note that the IdP's discovery document is
 * NOT a usable source for this - {@code id_token_signing_alg_values_supported} lists
 * everything the IdP <em>can</em> do, in no meaningful order, so picking an entry
 * from it would routinely pick one the IdP does not actually use.
 */
public final class IdpJwsAlgorithms {

	private IdpJwsAlgorithms() {
	}

	/**
	 * Used when configuration is blank. This is the algorithm the shipped
	 * configuration selects (see {@code oidc.idp.id-token-signed-response-alg} and
	 * {@code oidc.idp.access-token-signing-algs} in application.properties), not
	 * OIDC Core's RS256 registration default: a blank value has to behave like the
	 * value we ship, or a deployment that clears the property silently starts
	 * verifying with an algorithm its IdP does not sign with, and every login fails
	 * on the signature.
	 */
	public static final SignatureAlgorithm DEFAULT = SignatureAlgorithm.ES256;

	/**
	 * @param names comma-separated algorithm names, e.g. "ES256,RS256"; blank or null
	 *              yields {@link #DEFAULT}
	 * @return the named algorithms, in the order given, without duplicates
	 * @throws IllegalStateException if a name is not an asymmetric JWS algorithm
	 */
	public static List<SignatureAlgorithm> parse(String names) {
		if (names == null || names.isBlank()) {
			return List.of(DEFAULT);
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

		return algorithms.isEmpty() ? List.of(DEFAULT) : List.copyOf(algorithms);
	}

	/**
	 * @return the single algorithm named by {@code names}
	 * @throws IllegalStateException if more than one is named; the ID token decoder
	 *                               accepts exactly one algorithm
	 */
	public static SignatureAlgorithm parseSingle(String names) {
		List<SignatureAlgorithm> algorithms = parse(names);
		if (algorithms.size() > 1) {
			throw new IllegalStateException("Expected a single JWS signature algorithm, but got " + algorithms
				+ ". An ID token is signed with exactly one algorithm.");
		}
		return algorithms.get(0);
	}

	/**
	 * @return true if the IdP's discovery document says it can sign with {@code algorithm},
	 * or if it advertises nothing usable (in which case there is nothing to contradict)
	 */
	public static boolean isAdvertised(Collection<?> advertised, SignatureAlgorithm algorithm) {
		if (advertised == null || advertised.isEmpty()) {
			return true;
		}
		return advertised.stream()
			.filter(Objects::nonNull)
			.map(String::valueOf)
			.anyMatch(name -> name.equals(algorithm.getName()));
	}

	private static String supportedNames() {
		return java.util.Arrays.stream(SignatureAlgorithm.values()).map(SignatureAlgorithm::getName).toList().toString();
	}
}
