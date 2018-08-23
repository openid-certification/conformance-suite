package io.fintechlabs.testframework.condition.rs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CreateOpenBankingAccountsResponse extends AbstractOpenBankingApiResponse {

	public CreateOpenBankingAccountsResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String[] requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = "account_id")
	@PostEnvironment(required = "accounts_endpoint_response")
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

		env.putObject("accounts_endpoint_response", response);

		return env;

	}

}
