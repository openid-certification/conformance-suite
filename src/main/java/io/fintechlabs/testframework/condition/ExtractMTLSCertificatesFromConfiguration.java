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

import java.util.Base64;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ExtractMTLSCertificatesFromConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public ExtractMTLSCertificatesFromConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "mutual_tls_authentication")
	public Environment evaluate(Environment env) {
		// mutual_tls_authentication
		
		String certString = env.getString("config", "mtls.cert");
		String keyString = env.getString("config", "mtls.key");
		String caString = env.getString("config", "mtls.ca");
		
		if (Strings.isNullOrEmpty(certString) || Strings.isNullOrEmpty(keyString)) {
			return error("Couldn't find TLS client certificate or key for MTLS");
		}
		
		if (Strings.isNullOrEmpty(caString)) {
			// Not an error; we just won't send a CA chain
			log("No certificate authority found for MTLS");
		}

		try {
			Base64.getDecoder().decode(certString);
			Base64.getDecoder().decode(keyString);
			if (caString != null) {
				Base64.getDecoder().decode(caString);
			}
		} catch (IllegalArgumentException e) {
			return error("Couldn't decode certificate, key, or CA chain from Base64", e, args("cert", certString, "key", keyString, "ca", Strings.emptyToNull(caString)));
		}

		JsonObject mtls = new JsonObject();
		mtls.addProperty("cert", certString);
		mtls.addProperty("key", keyString);
		if (caString != null) {
			mtls.addProperty("ca", caString);
		}
		
		env.put("mutual_tls_authentication", mtls);
		
		logSuccess("Mutual TLS authentication credentials loaded", mtls);
		
		return env;

	}

}
