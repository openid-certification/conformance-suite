package net.openid.conformance.condition.util;

import org.apache.commons.text.RandomStringGenerator;

public class RFC6749AppendixASyntaxUtils {

	/**
	 * VSCHAR     = %x20-7E
	 * @return random VSCHAR String
	 */
	public static String generateVSChar(int alphaCount, int numberCount, int punctuationCount) {
		String puncts = "";
		if(punctuationCount>0) {
			char[][] pairs = {
				{' ', '/'},
				{':', '@'},
				{'[', '`'},
				{'{', '~'},
			};
			puncts = new RandomStringGenerator.Builder().
				withinRange(pairs).get().generate(punctuationCount);
		}
		String numbers = generateNumberChar(numberCount);
		String alphas = generateAlphaChar(alphaCount);
		return alphas + numbers + puncts;
	}

	/**
	 * NQCHAR     = %x21 / %x23-5B / %x5D-7E
	 * @return random NQCHAR String
	 */
	public static String generateNQChar(int alphaCount, int numberCount, int punctuationCount) {
		String puncts = "";
		if(punctuationCount>0) {
			char[][] pairs = {
				{'!', '!'},
				{'#', '/'},
				{':', '@'},
				{'[', '['},
				{']', '`'},
				{'{', '~'},
			};
			puncts = new RandomStringGenerator.Builder().
				withinRange(pairs).get().generate(punctuationCount);
		}
		String numbers = generateNumberChar(numberCount);
		String alphas = generateAlphaChar(alphaCount);
		return alphas + numbers + puncts;
	}

	/**
	 * NQCHAR     = %x21 / %x23-5B / %x5D-7E
	 * i.e. visible ASCII other than space, double-quote and backslash.
	 */
	public static boolean isNQChar(char c) {
		return c == 0x21 || (c >= 0x23 && c <= 0x5B) || (c >= 0x5D && c <= 0x7E);
	}

	/**
	 * Whether the value matches {@code 1*NQCHAR}, i.e. is non-empty and made up entirely of
	 * {@link #isNQChar(char) NQCHAR}. This is the syntax RFC 6749 Appendix A gives for a scope-token, and
	 * that RFC 9449 section 8.1 reuses for a DPoP nonce.
	 */
	public static boolean isNQCharSequence(String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		for (int i = 0; i < value.length(); i++) {
			if (!isNQChar(value.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static String generateAlphaChar(int alphaCount) {
		String alphas = "";
		if(alphaCount>0) {
			char[][] pairs = {
				{'a', 'z'},
				{'A', 'Z'}
			};
			alphas = new RandomStringGenerator.Builder().
				withinRange(pairs).get().generate(alphaCount);
		}
		return alphas ;
	}

	private static String generateNumberChar(int numberCount) {
		String numbers = "";
		if(numberCount>0) {
			numbers = new RandomStringGenerator.Builder().
				withinRange(48, 57).    //0 to 9
				get().generate(numberCount);
		}
		return numbers;
	}
}
