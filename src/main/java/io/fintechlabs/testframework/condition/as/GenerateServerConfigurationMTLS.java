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

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.runner.TestDispatcher;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author leo
 *
 */
public class GenerateServerConfigurationMTLS extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public GenerateServerConfigurationMTLS(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "server", strings = { "issuer", "discoveryUrl" })
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");

		if (Strings.isNullOrEmpty(baseUrl)) {
			throw error("Couldn't find a base URL");
		}

		// set off the URLs below with a slash, if needed
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		// FIXME: we should inject a base_url_mtls as well instead of having to do this hack
		String baseUrlMtls = baseUrl.replaceFirst(TestDispatcher.TEST_PATH, TestDispatcher.TEST_MTLS_PATH);

		// create a base server configuration object based on the base URL
		JsonObject server = new JsonObject();

		server.addProperty("issuer", baseUrl);
		server.addProperty("authorization_endpoint", baseUrlMtls + "authorize");
		server.addProperty("token_endpoint", baseUrlMtls + "token");
		server.addProperty("jwks_uri", baseUrl + "jwks");

		server.addProperty("registration_endpoint", baseUrl + "register"); // TODO: should this be pulled into an optional mix-in?
		server.addProperty("userinfo_endpoint", baseUrl + "userinfo"); // TODO: should this be pulled into an optional mix-in?

		// add this as the server configuration
		env.putObject("server", server);

		env.putString("issuer", baseUrl);
		env.putString("discoveryUrl", baseUrl + ".well-known/openid-configuration");

		return env;

	}

}
