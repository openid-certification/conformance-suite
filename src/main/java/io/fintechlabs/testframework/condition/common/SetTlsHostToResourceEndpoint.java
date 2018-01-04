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

package io.fintechlabs.testframework.condition.common;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class SetTlsHostToResourceEndpoint extends AbstractCondition {

	private static final int HTTPS_DEFAULT_PORT = 443;

	public SetTlsHostToResourceEndpoint(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"resource", "config"})
	public Environment evaluate(Environment env) {

		String resourceEndpoint = env.getString("resource", "resourceUrl");
		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			return error("Resource endpoint not found");
		}

		UriComponents components = UriComponentsBuilder.fromUriString(resourceEndpoint).build();

		String host = components.getHost();
		int port = components.getPort();

		if (port < 0) {
			port = HTTPS_DEFAULT_PORT;
		}

		JsonObject endpoint = new JsonObject();
		endpoint.addProperty("testHost", host);
		endpoint.addProperty("testPort", port);

		env.get("config").remove("tls");
		env.get("config").add("tls", endpoint);

		return env;
	}

}
