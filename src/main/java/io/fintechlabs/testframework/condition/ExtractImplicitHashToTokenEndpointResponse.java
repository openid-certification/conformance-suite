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

import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ExtractImplicitHashToTokenEndpointResponse extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public ExtractImplicitHashToTokenEndpointResponse(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "implicit_hash")
	@PostEnvironment(required = {"callback_params", "token_endpoint_response"})
	public Environment evaluate(Environment env) {

		if (!Strings.isNullOrEmpty(env.getString("implicit_hash"))) {
			
			String hash = env.getString("implicit_hash").substring(1); // strip off the leading # character
			
			List<NameValuePair> parameters = URLEncodedUtils.parse(hash, Charset.defaultCharset());
			
			log("Extracted response from hash", args("parameters", parameters));
			
			JsonObject o = new JsonObject();
			for (NameValuePair pair : parameters) {
				o.addProperty(pair.getName(), pair.getValue());
			}
			
			// these count as both the authorization and token responses
			env.put("callback_params", o);
			env.put("token_endpoint_response", o);
			
			logSuccess("Extracted the hash values", o);
			
			return env;
			
		} else {
			return error("Couldn't find the response in hash for implicit flow");
		}
		
	}

}
