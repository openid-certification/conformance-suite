package net.openid.conformance.condition;

import net.openid.conformance.testmodule.Environment;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractEnsureMinimumEntropy extends AbstractCondition {

	protected Environment ensureMinimumEntropy(Environment env, String s, double requiredEntropy) {

		double bitsPerCharacter = getShannonEntropy(s);

		double entropy = bitsPerCharacter * (double) s.length();

		if (entropy > requiredEntropy) {
			logSuccess("Calculated shannon entropy seems sufficient", args("value", s, "expected", requiredEntropy, "actual", entropy));
			return env;
		} else {
			throw error("Calculated shannon entropy does not seem to meet minimum required entropy (i.e. item is too short, or not random enough)", args("value", s, "expected", requiredEntropy, "actual", entropy));
		}

	}

	// entropy calculation from https://rosettacode.org/wiki/Entropy#Java
	protected double getShannonEntropy(String s) {
		int n = 0;
		Map<Character, Integer> occ = new HashMap<>();

		for (int c_ = 0; c_ < s.length(); ++c_) {
			char cx = s.charAt(c_);
			if (occ.containsKey(cx)) {
				occ.put(cx, occ.get(cx) + 1);
			} else {
				occ.put(cx, 1);
			}
			++n;
		}

		double e = 0.0;
		for (Map.Entry<Character, Integer> entry : occ.entrySet()) {
			double p = (double) entry.getValue() / n;
			e += p * log2(p);
		}
		return -e;
	}

	private static double log2(double a) {
		return Math.log(a) / Math.log(2);
	}

}
