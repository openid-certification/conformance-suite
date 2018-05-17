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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ExtractJWKsFromResourceConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public ExtractJWKsFromResourceConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "resource")
	@PostEnvironment(required = "resource_jwks")
	public Environment evaluate(Environment env) {

		if (!env.containsObj("resource")) {
			throw error("Couldn't find resource configuration");
		}

		// bump the client's internal JWK up to the root
		JsonElement jwks = env.findElement("resource", "jwks");

		if (jwks == null) {
			throw error("Couldn't find JWKs in resource configuration");
		} else if (!(jwks instanceof JsonObject)) {
			throw error("Invalid JWKs in resource configuration - JSON decode failed");
		}

		try {
			JWKSet parsed = JWKSet.parse(jwks.toString());
			JWKSet pub = parsed.toPublicJWKSet();
			
			JsonObject pubObj = (new JsonParser().parse(pub.toString())).getAsJsonObject();
			
			logSuccess("Extracted resource JWK", args("resource_jwks", jwks, "public_resource_jwks", pubObj));

			env.put("resource_jwks", jwks.getAsJsonObject());
			env.put("resource_public_jwks", pubObj.getAsJsonObject());

			return env;

			
		} catch (ParseException e) {
			throw error("Invalid JWKs in resource configuration, JWKS parsing failed", e, args("resource_jwks", jwks));
		}
	}

}
