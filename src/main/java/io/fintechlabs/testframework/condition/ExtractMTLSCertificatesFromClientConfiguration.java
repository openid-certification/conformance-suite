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
public class ExtractMTLSCertificatesFromClientConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public ExtractMTLSCertificatesFromClientConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "mutual_tls_authentication")
	public Environment evaluate(Environment env) {
		// mutual_tls_authentication
		
		String certString = env.getString("client", "mtls.cert");
		String keyString = env.getString("client", "mtls.key");
		
		if (Strings.isNullOrEmpty(certString) || Strings.isNullOrEmpty(keyString)) {
			return error("Couldn't find client certificate or key for MTLS");
		}
		
		try {
			Base64.getDecoder().decode(certString);
			Base64.getDecoder().decode(keyString);
		} catch (IllegalArgumentException e) {
			return error("Couldn't decode certificate or key from Base64", e, args("cert", certString, "key", keyString));
		}

		JsonObject mtls = new JsonObject();
		mtls.addProperty("cert", certString);
		mtls.addProperty("key", keyString);
		
		env.put("mutual_tls_authentication", mtls);
		
		logSuccess("Mutual TLS authentication credentials loaded", mtls);
		
		return env;

	}

}
