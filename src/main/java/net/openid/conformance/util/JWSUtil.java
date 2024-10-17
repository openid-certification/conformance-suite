package net.openid.conformance.util;

import com.nimbusds.jose.JWSAlgorithm;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JWSUtil {

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
		return false;
	}

	public static List<String> validJWSAlgorithms() {
		return Stream.of(
				JWSAlgorithm.Family.EC,
				JWSAlgorithm.Family.ED,
				JWSAlgorithm.Family.HMAC_SHA,
				JWSAlgorithm.Family.RSA
			)
			.flatMap(family -> family.stream())
			.map(JWSAlgorithm::getName)
			.collect(Collectors.toList());
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
		return false;
	}

}
