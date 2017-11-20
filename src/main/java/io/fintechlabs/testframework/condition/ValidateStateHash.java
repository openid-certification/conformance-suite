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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nimbusds.jose.util.Base64URL;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateStateHash extends AbstractCondition {

	public ValidateStateHash(String testId, EventLog log, boolean optional) {
		super(testId, log, optional, "FAPI-2-5.2.2-4");
	}

	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObj("id_token")) {
			return error("Couldn't find parsed ID token");
		}

		String s_hash = env.getString("id_token", "claims.s_hash");
		if (s_hash == null) {
			return error("Couldn't find s_hash in ID token");
		}

		String alg = env.getString("id_token", "header.alg");
		if (alg == null) {
			return error("Couldn't find algorithm in ID token header");
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

		logSuccess("State hash validated successfully");
		return env;
	}

}
