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

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * @author srmoore
 *
 */
public class CreateS256CodeChallenge extends AbstractCondition{

	/**
	 * @param testId
	 * @param log
	 */
	public CreateS256CodeChallenge(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(strings = "code_verifier")
	@PostEnvironment(strings = {"code_challenge","code_challenge_method"})
	public Environment evaluate(Environment env) {
		String verifier = env.getString("code_verifier");

		if(Strings.isNullOrEmpty(verifier)){
			throw error("code_verifier was null or empty");
		}

		try {
			byte[] bytes = verifier.getBytes("US-ASCII");
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(bytes, 0, bytes.length);
			byte[] digest = md.digest();
			String challenge = Base64.encodeBase64URLSafeString(digest);

			env.putString("code_challenge", challenge);
			env.putString("code_challenge_method", "S256");
			log("Created code_challenge value", args("code_challenge", challenge));

		} catch (NoSuchAlgorithmException e) {
			throw error("No such Algorithm Error",e);
		} catch (UnsupportedEncodingException e) {
			throw error("Unsupported Encoding while getting code_verifier bytes", e);
		}

		return env;
	}
}
