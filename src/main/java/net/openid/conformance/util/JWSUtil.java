package net.openid.conformance.util;

import com.nimbusds.jose.JWSAlgorithm;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JWSUtil {

	/**
	 * Asymmetric JWS algorithms registered in the IANA JOSE Algorithms registry that are
	 * not yet exposed by Nimbus families. The Nimbus EC/ED/RSA families miss these recently
	 * registered post-quantum and revised algorithm names; until the dependency catches up
	 * we treat membership in this set as equivalent to membership in the Nimbus families.
	 *
	 * @see <a href="https://www.iana.org/assignments/jose/jose.xhtml#web-signature-encryption-algorithms">IANA JOSE Algorithms registry</a>
	 */
	private static final Set<String> EXTRA_ASYMMETRIC_JWS_ALGORITHMS = Set.of(
		"ML-DSA-44",
		"ML-DSA-65",
		"ML-DSA-87"
	);

	/**
	 * Checks if alg is one of the algorithms supported by Nimbusds
	 * @param alg
	 * @return
	 */
	public static boolean isValidJWSAlgorithm(String alg) {
		JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(alg);
		if(JWSAlgorithm.Family.EC.contains(jwsAlgorithm)
			||JWSAlgorithm.Family.ED.contains(jwsAlgorithm)
			||JWSAlgorithm.Family.HMAC_SHA.contains(jwsAlgorithm)
			||JWSAlgorithm.Family.RSA.contains(jwsAlgorithm)
		) {
			return true;
		}
		return EXTRA_ASYMMETRIC_JWS_ALGORITHMS.contains(alg);
	}

	public static List<String> validJWSAlgorithms() {
		return familyNamesWithExtras(
			JWSAlgorithm.Family.EC,
			JWSAlgorithm.Family.ED,
			JWSAlgorithm.Family.HMAC_SHA,
			JWSAlgorithm.Family.RSA);
	}

	/**
	 * Checks if alg is an asymmetric algorithm
	 * @param alg
	 * @return
	 */
	public static boolean isAsymmetricJWSAlgorithm(String alg) {
		JWSAlgorithm jwsAlgorithm = JWSAlgorithm.parse(alg);
		if(JWSAlgorithm.Family.EC.contains(jwsAlgorithm)
			||JWSAlgorithm.Family.ED.contains(jwsAlgorithm)
			||JWSAlgorithm.Family.RSA.contains(jwsAlgorithm)
		) {
			return true;
		}
		return EXTRA_ASYMMETRIC_JWS_ALGORITHMS.contains(alg);
	}

	public static List<String> validAsymmetricJWSAlgorithms() {
		return familyNamesWithExtras(
			JWSAlgorithm.Family.EC,
			JWSAlgorithm.Family.ED,
			JWSAlgorithm.Family.RSA);
	}

	/**
	 * Collects the algorithm names of the given Nimbus families plus the
	 * {@link #EXTRA_ASYMMETRIC_JWS_ALGORITHMS} not yet exposed by Nimbus.
	 */
	private static List<String> familyNamesWithExtras(JWSAlgorithm.Family... families) {
		return Stream.concat(
				Stream.of(families)
					.flatMap(family -> family.stream())
					.map(JWSAlgorithm::getName),
				EXTRA_ASYMMETRIC_JWS_ALGORITHMS.stream())
			.collect(Collectors.toList());
	}

}
