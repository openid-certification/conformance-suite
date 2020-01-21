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
				withinRange(pairs).build().generate(punctuationCount);
		}
		String numbers = "";
		if(numberCount>0) {
			numbers = new RandomStringGenerator.Builder().
				withinRange(48, 57).    //0 to 9
				build().generate(numberCount);
		}
		String alphas = "";
		if(alphaCount>0) {
			char[][] pairs = {
				{'a', 'z'},
				{'A', 'Z'}
			};
			alphas = new RandomStringGenerator.Builder().
				withinRange(pairs).build().generate(alphaCount);
		}
		return alphas + numbers + puncts;
	}
}
