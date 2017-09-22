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
import io.fintechlabs.testframework.testmodule.AbstractCondition;
import io.fintechlabs.testframework.testmodule.ConditionError;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CreateRedirectUri extends AbstractCondition {

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#assertTrue(io.fintechlabs.testframework.testmodule.Environment, io.fintechlabs.testframework.logging.EventLog)
	 */
	@Override
	public Environment evaluate(Environment in, String src, EventLog log) {
		String baseUrl = in.getString("base_url");
		
		if (Strings.isNullOrEmpty(baseUrl)) {
			throw new ConditionError(this, "Base URL was null or empty");
		}
		
		// calculate the redirect URI based on our given base URL
		String redirectUri = baseUrl + "/callback";
		in.put("redirect_uri", redirectUri);
		
		log.log(src, ImmutableMap.of("msg", "Created redirect URI", 
				"redirec_uri", redirectUri));
		
		return in;
	}

}
