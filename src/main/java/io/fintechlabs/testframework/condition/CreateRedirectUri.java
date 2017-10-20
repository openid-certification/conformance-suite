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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * Creates a callback URL based on the base_url environment value
 * 
 * 
 * @author jricher
 *
 */
public class CreateRedirectUri extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CreateRedirectUri(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#assertTrue(io.fintechlabs.testframework.testmodule.Environment, io.fintechlabs.testframework.logging.EventLog)
	 */
	@Override
	public Environment evaluate(Environment in) {
		String baseUrl = in.getString("base_url");
		
		if (Strings.isNullOrEmpty(baseUrl)) {
			return error("Base URL was null or empty");
		}
		
		// calculate the redirect URI based on our given base URL
		String redirectUri = baseUrl + "/callback";
		in.putString("redirect_uri", redirectUri);
		
		logSuccess("Created redirect URI", 
				args("redirect_uri", redirectUri));
		
		return in;
	}

}
