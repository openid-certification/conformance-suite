package net.openid.conformance.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JWAUtil
{
	public static String getDigestAlgorithmForSigAlg(String signatureAlgorithm) throws InvalidAlgorithmException {
		if("EdDSA".equals(signatureAlgorithm)) {
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
