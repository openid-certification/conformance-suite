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

import java.net.MalformedURLException;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.util.TLSTestValueExtractor;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ExtractTLSTestValuesFromResourceConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ExtractTLSTestValuesFromResourceConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "resource")
	@PostEnvironment(required = "resource_endpoint_tls")
	public Environment evaluate(Environment env) {

		try {
			String resourceEndpoint = env.getString("resource", "resourceUrl");
			if (Strings.isNullOrEmpty(resourceEndpoint)) {
				throw error("Resource endpoint not found");
			}

			JsonObject resourceEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(resourceEndpoint);

			env.put("resource_endpoint_tls", resourceEndpointTls);

			logSuccess("Extracted TLS information from resource endpoint", args(
					"resource_endpoint", resourceEndpointTls
				));

			return env;
		} catch (MalformedURLException e) {
			throw error("URL not properly formed", e);
		}
	}

}
