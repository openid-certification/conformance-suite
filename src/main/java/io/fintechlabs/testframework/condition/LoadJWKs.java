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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class LoadJWKs extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public LoadJWKs(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = {"public_jwks", "jwks"})
	public Environment evaluate(Environment env) {

		JsonElement configured = env.findElement("config", "server.jwks");
		
		if (configured == null) {
			return error("Couldn't find a JWK set in configuration");
		}
		
		// parse the JWKS to make sure it's valid
		try {
			JWKSet jwks = JWKSet.parse(configured.toString());
			
			JsonObject publicJwks = new JsonParser().parse(jwks.toJSONObject(true).toJSONString()).getAsJsonObject();
			JsonObject privateJwks = new JsonParser().parse(jwks.toJSONObject(false).toJSONString()).getAsJsonObject();
			
			env.put("public_jwks", publicJwks);
			env.put("jwks", privateJwks);
			
			logSuccess("Parsed public and private JWK sets", args("public_jwks", publicJwks, "jwks", jwks));
			
			return env;
			
		} catch (ParseException e) {
			return error("Failure parsing JWK Set", e, args("jwk_string", configured));
		}
		

		
	}

}
