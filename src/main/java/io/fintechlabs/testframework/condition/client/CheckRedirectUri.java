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

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CheckRedirectUri extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public CheckRedirectUri(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@PreEnvironment(strings = "redirect_uri")
	@Override
	public Environment evaluate(Environment env) {

		String redirectUri = env.getString("redirect_uri");
		
		try {
			
			URI uri = new URI(redirectUri);
			
			if (uri.getScheme().equals("http")) {
				// make sure that it's a "localhost" URL
				InetAddress addr = InetAddress.getByName(uri.getHost());
				
				if (!addr.isLoopbackAddress()) {
					throw error("Address given was not a loopback (localhost) address", args("scheme", uri.getScheme(), "host", uri.getHost()));
				}
				
				logSuccess("Plain http on localhost allowed", args("scheme", uri.getScheme(), "host", uri.getHost()));
				return env;
				
			} else if (uri.getScheme().equals("https")) {
				// any remote host URL is fine
				logSuccess("Encrypted http on any host allowed", args("scheme", uri.getScheme(), "host", uri.getHost()));
				return env;
				
			} else {
				// a non-HTTP URL is assumed to be app-specific
				logSuccess("Non-http URL allowed, assuming app-specific", args("scheme", uri.getScheme(), "path", uri.getSchemeSpecificPart()));
				return env;
			}
		} catch (URISyntaxException | UnknownHostException e) {
			throw error("Couldn't parse key as URI", e, args("uri", redirectUri));
		}
		
	}

}
