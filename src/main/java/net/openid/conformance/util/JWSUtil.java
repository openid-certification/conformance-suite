package net.openid.conformance.util;

import com.nimbusds.jose.JWSAlgorithm;

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
}
