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
public class ExtractAssertionFromIntrospectionRequest extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ExtractAssertionFromIntrospectionRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "introspection_request")
	@PostEnvironment(required = "resource_assertion")
	public Environment evaluate(Environment env) {

		String assertion = env.getString("introspection_request", "params.client_assertion");
		String assertionType = env.getString("introspection_request", "params.client_assertion_type");

		if (Strings.isNullOrEmpty(assertion) || Strings.isNullOrEmpty(assertionType)) {
			throw error("Couldn't find assertion or assertion type in request");
		}

		try {
			JWT parsed = JWTParser.parse(assertion);

			JsonParser parser = new JsonParser();

			JsonObject o = new JsonObject();

			o.addProperty("assertion", assertion);
			o.addProperty("assertion_type", assertionType);
			o.add("assertion_header", parser.parse(parsed.getHeader().toString()));
			o.add("assertion_payload", parser.parse(parsed.getJWTClaimsSet().toString()));

			env.put("resource_assertion", o);

			logSuccess("Extracted assertion from resource server", o);

			return env;
		} catch (ParseException e) {
			throw error("Couldn't parse client assertion", e);
		}

	}

}
