package net.openid.conformance.util;

import com.google.common.base.Strings;
import com.nimbusds.jose.JWSAlgorithm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JWAUtil
{
	/**
	 * Is this the name of a JWS algorithm whose signature can be verified?
	 *
	 * <p>True only for the algorithms registered for JWS use in the IANA "JSON Web Signature and
	 * Encryption Algorithms" registry that have a key type: the digital signature algorithms
	 * (RS*, PS*, ES*, EdDSA, Ed25519, Ed448) and the MAC algorithms (HS*), both of which AuthZEN
	 * and the JOSE specs allow for signing.
	 *
	 * <p>False for {@code none}, for unregistered names, and — importantly — for the JWE key
	 * management algorithms ({@code dir}, {@code A128KW}, {@code RSA-OAEP-256}, {@code ECDH-ES},
	 * {@code PBES2-*}). Those last ones matter because {@link com.nimbusds.jose.jwk.KeyType#forAlgorithm}
	 * happily maps them onto a key type, so a plain "does this algorithm have a key type" test would
	 * accept a JWE algorithm in a JWS header.
	 *
	 * @param algorithm the 'alg' header value (may be null or empty)
	 * @return true if a JWS signature made with this algorithm could be verified
	 */
	public static boolean isJwsAlgorithm(String algorithm) {
		if (Strings.isNullOrEmpty(algorithm)) {
			return false;
		}
		JWSAlgorithm parsed = JWSAlgorithm.parse(algorithm);
		return JWSAlgorithm.Family.SIGNATURE.contains(parsed) || JWSAlgorithm.Family.HMAC_SHA.contains(parsed);
	}

	public static String getDigestAlgorithmForSigAlg(String signatureAlgorithm) throws InvalidAlgorithmException {
		if("EdDSA".equals(signatureAlgorithm) || "Ed25519".equals(signatureAlgorithm)) {
			return "SHA-512";
		} else if(signatureAlgorithm.startsWith("ES") && signatureAlgorithm.endsWith("K")) {
			Matcher matcher = Pattern.compile("^(ES)(256|384|512)K$").matcher(signatureAlgorithm);
			if (!matcher.matches()) {
				throw new InvalidAlgorithmException(signatureAlgorithm);
			}
			return "SHA-" + matcher.group(2);
		} else {
			Matcher matcher = Pattern.compile("^(HS|RS|ES|PS)(256|384|512)$").matcher(signatureAlgorithm);
			if (!matcher.matches()) {
				throw new InvalidAlgorithmException(signatureAlgorithm);
			}
			return "SHA-" + matcher.group(2);
		}
	}

	@SuppressWarnings("serial")
	public static class InvalidAlgorithmException extends Exception {
		public InvalidAlgorithmException(String algorithm) {
			super("Invalid algorithm:" + algorithm);
		}
	}
}
