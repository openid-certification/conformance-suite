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

import java.text.ParseException;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureMinimumKeyLength extends AbstractCondition {

	public EnsureMinimumKeyLength(String testId, EventLog log, boolean optional) {
		super(testId, log, optional, "FAPI-1-5.2.2-5", "FAPI-1-5.2.2-6");
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
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
				minimumLength = 2048;
			} else if (keyType.equals(KeyType.EC)) {
				minimumLength = 160;
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