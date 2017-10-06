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

import java.util.Date;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ValidateIdToken extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public ValidateIdToken(String testId, EventLog log) {
		super(testId, log, "FAPI-1-5.2.2-24");
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {
		
		if (!env.containsObj("id_token")) {
			return error("Couldn't find parsed ID token");
		}
		
		String clientId = env.getString("client_id"); // to check the audience
		String issuer = env.getString("server", "issuer"); // to validate the issuer
		Date now = new Date(); // to check timestamps
		
		// check all our testable values
		if (Strings.isNullOrEmpty(clientId) 
				|| Strings.isNullOrEmpty(issuer)) {
			return error("Couldn't find values to test ID token against");
		}
		
		if (!issuer.equals(env.getString("id_token", "claims.iss"))) {
			log("Issuer mismatch", ImmutableMap.of("expected", issuer, "actual", env.getString("id_token", "claims.iss")));
			return error("Issuer mismatch");
		}
		
		JsonElement aud = env.findElement("id_token", "claims.aud");
		if (aud == null) {
			return error("Missing audience");
		}
		
		if (aud.isJsonArray()) {
			if (!aud.getAsJsonArray().contains(new JsonPrimitive(clientId))) {
				log("Audience not found", ImmutableMap.of("expected", clientId, "actual", aud));
				return error("Couldn't find audience value");
			}
		} else {
			if (!clientId.equals(aud.getAsString())) {
				log("Audience mismatch", ImmutableMap.of("expected", clientId, "actual", aud));
				return error("Couldn't find audience value");
			}
		}
		
		Long exp = env.getLong("id_token", "claims.exp");
		if (exp == null) {
			return error("Missing expiration");
		} else {
			if (now.after(new Date(exp * 1000L))) {
				log("Token expired", ImmutableMap.of("expiration", new Date(exp * 1000L), "now", now));
				return error("Token expired");
			}
		}
		
		Long iat = env.getLong("id_token", "claims.iat");
		if (iat == null) {
			return error("Missing issuace time");
		} else {
			if (now.before(new Date(iat * 1000L))) {
				log("Token issued in the future", ImmutableMap.of("issued-at", new Date(iat * 1000L), "now", now));
				return error("Token issued in the future");
			}
		}
		
		Long nbf = env.getLong("id_token", "claims.nbf");
		if (nbf != null) {
			if (now.before(new Date(nbf * 1000L))) {
				// this is just something to log, it doesn't make the token invalid
				log("Token has future not-before", ImmutableMap.of("not-before", new Date(nbf * 1000L), "now", now));
			}
		}
		
		logSuccess();
		return env;
		
	}

}
