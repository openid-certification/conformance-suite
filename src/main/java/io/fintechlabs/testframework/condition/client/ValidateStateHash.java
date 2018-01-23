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

package io.fintechlabs.testframework.condition.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nimbusds.jose.util.Base64URL;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateStateHash extends AbstractCondition {

	public ValidateStateHash(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(strings = "state", required = "state_hash")
	public Environment evaluate(Environment env) {

		String s_hash = env.getString("state_hash", "s_hash");
		if (s_hash == null) {
			return error("Couldn't find s_hash");
		}

		String alg = env.getString("state_hash", "alg");
		if (alg == null) {
			return error("Couldn't find algorithm");
		}

		String state = env.getString("state");
		if (state == null) {
			return error("Couldn't find state");
		}

		MessageDigest digester;

		try {
			Matcher matcher = Pattern.compile("^(HS|RS|ES|PS)(256|384|512)$").matcher(alg);
			if (!matcher.matches()) {
				return error("Invalid algorithm", args("alg", alg));
			}

			String digestAlgorithm = "SHA-" + matcher.group(2);
			digester = MessageDigest.getInstance(digestAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			return error("Unsupported digest for algorithm", e, args("alg", alg));
		}

		byte[] stateDigest = digester.digest(state.getBytes(StandardCharsets.US_ASCII));

		byte[] halfDigest = new byte[stateDigest.length / 2];
		System.arraycopy(stateDigest, 0, halfDigest, 0, halfDigest.length);

		String expectedHash = Base64URL.encode(halfDigest).toString();
		if (!s_hash.equals(expectedHash)) {
			return error("Invalid s_hash in token", args("expected", expectedHash, "actual", s_hash));
		}

		logSuccess("State hash validated successfully", args("s_hash", s_hash));

		return env;
	}

}
