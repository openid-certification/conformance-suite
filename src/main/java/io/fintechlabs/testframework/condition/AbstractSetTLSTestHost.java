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

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public abstract class AbstractSetTLSTestHost extends AbstractCondition {

	private static final int HTTPS_DEFAULT_PORT = 443;

	/**
	 * @param testId
	 * @param log
	 */
	public AbstractSetTLSTestHost(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	protected Environment setTLSTestHost(Environment env, String host, int port) {

		JsonObject o = new JsonObject();
		o.addProperty("testHost", host);
		o.addProperty("testPort", port);

		env.removeObject("tls");
		env.putObject("tls", o);

		logSuccess("Configured TLS test host", o);

		return env;
	}

	protected Environment setTLSTestHost(Environment env, String url) {

		UriComponents components = UriComponentsBuilder.fromUriString(url).build();

		String host = components.getHost();
		int port = components.getPort();

		if (port < 0) {
			port = HTTPS_DEFAULT_PORT;
		}

		return setTLSTestHost(env, host, port);
	}

}
