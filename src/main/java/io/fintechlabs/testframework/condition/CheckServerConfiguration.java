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
 * @author jricher
 *
 */
public class CheckServerConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CheckServerConfiguration(String testId, EventLog log) {
		super(testId, log);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment in) {

		// first make sure we've got a "server" object at all
		if (!in.containsObj("server")) {
			return error("Couldn't find a server configuration at all");
		}

		ensureString(in, "authorization_endpoint");
		ensureString(in, "token_endpoint");
		ensureString(in, "issuer");

		return in;
	}
	
	private void ensureString(Environment in, String path) {
		String string = in.getString("server", path);
		if (Strings.isNullOrEmpty(string)) {
			error("Couldn't find required component " + path);
		} else {
			log(ImmutableMap.of(path, string));
		}
		
	}

}
