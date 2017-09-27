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

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import jersey.repackaged.com.google.common.collect.ImmutableMap;

/**
 * @author jricher
 *
 */
public class ExtractAuthorizationCodeFromAuthorizationResponse extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public ExtractAuthorizationCodeFromAuthorizationResponse(String testId, EventLog log) {
		super(testId, log);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment in) {
		if (!in.containsObj("callback_params")) {
			return error("Couldn't find callback parameters");
		}

		if (Strings.isNullOrEmpty(in.getString("callback_params", "code"))) {
			return error("Couldn't find authorizaiton code in callback");
		} else {
			in.putString("code", in.getString("callback_params", "code"));
			log(ImmutableMap.of("msg", "Found authorization code", "code", in.getString("callback_params", "code")));
			logSuccess();
			return in;
		}
		
	}

}
