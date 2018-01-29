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

package io.fintechlabs.testframework.openbanking;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.rs.AbstractOpenBankingApiResponse;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CreateOpenBankingAccountsResponse extends AbstractOpenBankingApiResponse {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public CreateOpenBankingAccountsResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String[] requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = "account_id")
	@PostEnvironment(required = "accounts_response")
	public Environment evaluate(Environment env) {

		String accountId = env.getString("account_id");

		JsonObject accountRoot = new JsonParser().parse(
				"      {\n" + 
				"        \"AccountId\": \"" + accountId + "\",\n" +
				"        \"Currency\": \"GBP\",\n" +
				"        \"Nickname\": \"Bills\",\n" +
				"        \"Account\": {\n" +
				"          \"SchemeName\": \"SortCodeAccountNumber\",\n" +
				"          \"Identification\": \"80200110203345\",\n" +
				"          \"Name\": \"Mr Kevin\",\n" +
				"          \"SecondaryIdentification\": \"00021\"\n" +
				"        }\n" +
				"      }")
			.getAsJsonObject();

		JsonArray accounts = new JsonArray();
		accounts.add(accountRoot);

		JsonObject data = new JsonObject();
		data.add("Account", accounts);

		JsonObject response = createResponse(data);

		logSuccess("Created account response object", args("accounts_endpoint_response", response));

		env.put("accounts_endpoint_response", response);

		return env;

	}

}
