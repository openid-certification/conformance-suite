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

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CheckClientRedirectUri extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public CheckClientRedirectUri(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@PreEnvironment(required =  "client")
	@Override
	public Environment evaluate(Environment env) {

		JsonElement ru = env.getElementFromObject("client", "redirect_uris");

		if (ru == null) {
			throw error("Redirect URIs list was null");
		}

		JsonArray redirectUris = ru.getAsJsonArray();

		if (redirectUris.size() == 0) {
			throw error("Redirect URIs list was empty");
		}

		Map<String, String> schemes = new HashMap<>();
		Map<String, String> otherSchemes = new HashMap<>();

		for (JsonElement el : redirectUris) {
			String redirectUri = el.getAsString();

			try {

				URI uri = new URI(redirectUri);

				if (uri.getScheme().equals("http")) {
					// make sure that it's a "localhost" URL
					InetAddress addr = InetAddress.getByName(uri.getHost());

					if (!addr.isLoopbackAddress()) {
						throw error("Address given was not a loopback (localhost) address", args("scheme", uri.getScheme(), "host", uri.getHost()));
					}

					schemes.put(uri.getScheme(), uri.getHost());

				} else if (uri.getScheme().equals("https")) {
					// any remote host URL is fine
					schemes.put(uri.getScheme(), uri.getHost());
				} else {
					// a non-HTTP URL is assumed to be app-specific
					otherSchemes.put(uri.getScheme(), uri.getSchemeSpecificPart());
				}
			} catch (URISyntaxException | UnknownHostException e) {
				throw error("Couldn't parse key as URI", e, args("uri", redirectUri));
			}
		}

		logSuccess("All redirect URIs have appropriate scheme and host combinations", args("http", schemes, "non-http", otherSchemes));

		return env;
	}


}
