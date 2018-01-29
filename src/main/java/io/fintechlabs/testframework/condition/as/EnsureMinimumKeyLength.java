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

package io.fintechlabs.testframework.condition.as;

import java.text.ParseException;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureMinimumKeyLength extends AbstractCondition {

	private static final int MINIMUM_KEY_LENGTH_RSA = 2048;

	private static final int MINIMUM_KEY_LENGTH_EC = 160;

	public EnsureMinimumKeyLength(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "jwks")
	public Environment evaluate(Environment env) {

		JsonObject jwks = env.get("jwks");
		if (jwks == null) {
			return error("Couldn't find JWKs in environment");
		}

		JWKSet jwkset;
		try {
			jwkset = JWKSet.parse(jwks.toString());
		} catch (ParseException e) {
			return error("Failure parsing JWK Set", e);
		}

		for (JWK jwk : jwkset.getKeys()) {
			KeyType keyType = jwk.getKeyType();
			int keyLength = jwk.size();
			int minimumLength;

			if (keyType.equals(KeyType.RSA)) {
				minimumLength = MINIMUM_KEY_LENGTH_RSA;
			} else if (keyType.equals(KeyType.EC)) {
				minimumLength = MINIMUM_KEY_LENGTH_EC;
			} else {
				// No requirement for other key types
				continue;
			}

			if (keyLength < minimumLength) {
				return error("Key length too short", args("minimum", minimumLength, "actual", keyLength, "key", jwk));
			}
		}

		logSuccess("Validated minimum key lengths", args("jwks", jwks));

		return env;
	}

}