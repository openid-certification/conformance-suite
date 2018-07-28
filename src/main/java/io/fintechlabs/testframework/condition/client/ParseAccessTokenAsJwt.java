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

import java.text.ParseException;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ParseAccessTokenAsJwt extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ParseAccessTokenAsJwt(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@PreEnvironment(required = "access_token")
	@PostEnvironment(required = "access_token_jwt")
	@Override
	public Environment evaluate(Environment env) {
		String accessToken = env.getString("access_token", "value");

		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Access token is missing");
		}

		try {
			JWT jwt = JWTParser.parse(accessToken);

			// Note: we need to round-trip this to get to GSON objects because the JWT library uses a different parser
			JsonObject header = new JsonParser().parse(jwt.getHeader().toJSONObject().toJSONString()).getAsJsonObject();
			JsonObject claims = new JsonParser().parse(jwt.getJWTClaimsSet().toJSONObject().toJSONString()).getAsJsonObject();

			JsonObject o = new JsonObject();
			o.addProperty("value", accessToken); // save the original string to allow for crypto operations
			o.add("header", header);
			o.add("claims", claims);

			env.put("access_token_jwt", o);

			logSuccess("Extracted access token as a JWT", o);

			return env;

		} catch (ParseException e) {
			throw error("Couldn't parse access token as a JWT", e, args("access_token", accessToken));
		}
	}

}
