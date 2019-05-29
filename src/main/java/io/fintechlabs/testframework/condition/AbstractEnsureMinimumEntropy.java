package io.fintechlabs.testframework.condition;

import java.util.HashMap;
import java.util.Map;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public abstract class AbstractEnsureMinimumEntropy extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public AbstractEnsureMinimumEntropy(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	protected Environment ensureMinimumEntropy(Environment env, String s, double requiredEntropy) {

		double bitsPerCharacter = getShannonEntropy(s);

		double entropy = bitsPerCharacter * (double) s.length();

		if (entropy > requiredEntropy) {
			logSuccess("Calculated entropy", args("expected", requiredEntropy, "actual", entropy));
			return env;
		} else {
			throw error("Minimum entropy not met", args("expected", requiredEntropy, "actual", entropy));
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
