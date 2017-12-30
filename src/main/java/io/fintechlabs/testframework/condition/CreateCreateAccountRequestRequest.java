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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateCreateAccountRequestRequest extends AbstractCondition {

	public CreateCreateAccountRequestRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment()
	@PostEnvironment(required = "account_requests_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonArray permissions = new JsonArray();
		permissions.add("ReadAccountsBasic");

		JsonObject data = new JsonObject();
		data.add("Permissions", permissions);

		JsonObject o = new JsonObject();
		o.add("Data", data);
		o.add("Risk", new JsonObject());

		env.put("account_requests_endpoint_request", o);

		logSuccess(args("account_requests_endpoint_request", o));

		return env;
	}

}
