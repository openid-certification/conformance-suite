package net.openid.conformance.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility for validating COSE algorithm identifiers against the IANA COSE Algorithms registry.
 *
 * @see <a href="https://www.iana.org/assignments/cose/cose.xhtml#algorithms">IANA COSE Algorithms</a>
 */
public class CoseAlgorithmUtil {

	private static final Map<Integer, String> SIGNATURE_ALGORITHMS;

	static {
		Map<Integer, String> algs = new HashMap<>();
		// RFC 9053 / RFC 9864 signature algorithms
		algs.put(-7, "ES256");
		algs.put(-8, "EdDSA");
		algs.put(-9, "ESP256");
		algs.put(-19, "Ed25519");
		algs.put(-35, "ES384");
		algs.put(-36, "ES512");
		algs.put(-37, "PS256");
		algs.put(-38, "PS384");
		algs.put(-39, "PS512");
		algs.put(-46, "HSS-LMS");
		algs.put(-47, "ES256K");
		algs.put(-48, "ML-DSA-44");
		algs.put(-49, "ML-DSA-65");
		algs.put(-50, "ML-DSA-87");
		algs.put(-51, "ESP384");
		algs.put(-52, "ESP512");
		algs.put(-53, "Ed448");
		algs.put(-257, "RS256");
		algs.put(-258, "RS384");
		algs.put(-259, "RS512");
		algs.put(-260, "WalnutDSA");
		algs.put(-265, "ESB256");
		algs.put(-266, "ESB320");
		algs.put(-267, "ESB384");
		algs.put(-268, "ESB512");
		algs.put(-65535, "RS1");
		SIGNATURE_ALGORITHMS = Collections.unmodifiableMap(algs);
	}

	private static final Map<Integer, String> MAC_ALGORITHMS;

	static {
		Map<Integer, String> algs = new HashMap<>();
		// RFC 9053 MAC algorithms
		algs.put(4, "HMAC 256/64");
		algs.put(5, "HMAC 256/256");
		algs.put(6, "HMAC 384/384");
		algs.put(7, "HMAC 512/512");
		algs.put(14, "AES-MAC 128/64");
		algs.put(15, "AES-MAC 256/64");
		algs.put(25, "AES-MAC 128/128");
		algs.put(26, "AES-MAC 256/128");
		MAC_ALGORITHMS = Collections.unmodifiableMap(algs);
	}

	public static boolean isValidCoseSignatureAlgorithm(int id) {
		return SIGNATURE_ALGORITHMS.containsKey(id);
	}

	public static boolean isValidCoseMacAlgorithm(int id) {
		return MAC_ALGORITHMS.containsKey(id);
	}

	public static boolean isValidCoseSignatureOrMacAlgorithm(int id) {
		return SIGNATURE_ALGORITHMS.containsKey(id) || MAC_ALGORITHMS.containsKey(id);
	}

	public static Set<Integer> validCoseSignatureAlgorithmIds() {
		return SIGNATURE_ALGORITHMS.keySet();
	}

	public static Set<Integer> validCoseSignatureOrMacAlgorithmIds() {
		HashMap<Integer, String> combined = new HashMap<>(SIGNATURE_ALGORITHMS);
		combined.putAll(MAC_ALGORITHMS);
		return Collections.unmodifiableSet(combined.keySet());
	}

}
