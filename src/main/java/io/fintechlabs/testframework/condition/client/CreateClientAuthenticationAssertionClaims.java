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

import org.apache.commons.lang3.RandomStringUtils;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CreateClientAuthenticationAssertionClaims extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public CreateClientAuthenticationAssertionClaims(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = { "client", "server" })
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {

		String issuer = env.getString("client", "client_id");
		String audience = env.getString("server", "token_endpoint");

		if (Strings.isNullOrEmpty(issuer) || Strings.isNullOrEmpty(audience)) {
			return error("Couldn't find required configuration element", args("issuer", issuer, "audience", audience));
		}

		JsonObject claims = new JsonObject();
		claims.addProperty("iss", issuer);
		claims.addProperty("sub", issuer);
		claims.addProperty("aud", audience);
		claims.addProperty("jti", RandomStringUtils.randomAlphanumeric(20));

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(60);

		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());

		logSuccess("Created client assertion claims", claims);

		env.put("client_assertion_claims", claims);

		return env;

	}

}
