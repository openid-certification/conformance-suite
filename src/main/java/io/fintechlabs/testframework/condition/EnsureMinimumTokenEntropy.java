/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.condition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class EnsureMinimumTokenEntropy extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public EnsureMinimumTokenEntropy(String testId, EventLog log) {
		super(testId, log, ImmutableSet.of("FAPI-1-5.2.2-16"));
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {
		String accessToken = env.getString("token_endpoint_response", "access_token");
		
		double bitsPerCharacter = getShannonEntropy(accessToken);
		
		double entropy = bitsPerCharacter * (double) accessToken.length();
		
		log("Calculated entropy", ImmutableMap.of("entropy", entropy));
		
		if (entropy > 128) {
			logSuccess();
			return env;
		} else {
			return error("Minimum entropy not met, got " + entropy);
		}
		
	}

	private double getShannonEntropy(String s) {
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
			char cx = entry.getKey();
			double p = (double) entry.getValue() / n;
			e += p * log2(p);
		}
		return -e;
	}

	private static double log2(double a) {
		return Math.log(a) / Math.log(2);
	}

}
