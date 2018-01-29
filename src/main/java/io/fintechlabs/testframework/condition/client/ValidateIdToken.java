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

import java.time.Instant;
import java.util.Date;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ValidateIdToken extends AbstractCondition {

	// TODO: make this configurable
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	/**
	 * @param testId
	 * @param log
	 */
	public ValidateIdToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"id_token", "server"}, strings = "client_id")
	public Environment evaluate(Environment env) {
		
		if (!env.containsObj("id_token")) {
			return error("Couldn't find parsed ID token");
		}
		
		String clientId = env.getString("client_id"); // to check the audience
		String issuer = env.getString("server", "issuer"); // to validate the issuer
		Instant now = Instant.now(); // to check timestamps
		
		// check all our testable values
		if (Strings.isNullOrEmpty(clientId) 
				|| Strings.isNullOrEmpty(issuer)) {
			return error("Couldn't find values to test ID token against");
		}
		
		JsonElement iss = env.findElement("id_token", "claims.iss");
		if (iss == null) {
			return error("Missing issuer");
		}
		
		if (!issuer.equals(env.getString("id_token", "claims.iss"))) {
			return error("Issuer mismatch", args("expected", issuer, "actual", env.getString("id_token", "claims.iss")));
		}
		
		JsonElement aud = env.findElement("id_token", "claims.aud");
		if (aud == null) {
			return error("Missing audience");
		}
		
		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(clientId))) {
				return error("Audience not found", args("expected", clientId, "actual", aud));
			}
		} else {
			if (!clientId.equals(aud.getAsString())) {
				return error("Audience mismatch", args("expected", clientId, "actual", aud));
			}
		}
		
		Long exp = env.getLong("id_token", "claims.exp");
		if (exp == null) {
			return error("Missing expiration");
		} else {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				return error("Token expired", args("expiration", new Date(exp * 1000L), "now", now));
			}
		}
		
		Long iat = env.getLong("id_token", "claims.iat");
		if (iat == null) {
			return error("Missing issuace time");
		} else {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
				return error("Token issued in the future", args("issued-at", new Date(iat * 1000L), "now", now));
			}
		}
		
		Long nbf = env.getLong("id_token", "claims.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				// this is just something to log, it doesn't make the token invalid
				log("Token has future not-before", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}
		
		logSuccess("ID token claims passed all validation checks");
		return env;
		
	}

}
