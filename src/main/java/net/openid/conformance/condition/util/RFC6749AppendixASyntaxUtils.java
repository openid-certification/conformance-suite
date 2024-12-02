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
