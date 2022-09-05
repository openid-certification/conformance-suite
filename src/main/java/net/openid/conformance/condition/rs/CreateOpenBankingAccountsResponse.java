package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateOpenBankingAccountsResponse extends AbstractOpenBankingApiResponse {

	@Override
	@PreEnvironment(strings = {"account_id", "fapi_interaction_id"})
	@PostEnvironment(required = {"accounts_endpoint_response", "accounts_endpoint_response_headers"})
	public Environment evaluate(Environment env) {

		String accountId = env.getString("account_id");

		JsonObject accountRoot = JsonParser.parseString(
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

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);
		headers.addProperty("content-type", "application/json; charset=UTF-8");

		logSuccess("Created account response object", args("accounts_endpoint_response", response, "accounts_endpoint_response_headers", headers));

		env.putObject("accounts_endpoint_response", response);
		env.putObject("accounts_endpoint_response_headers", headers);

		return env;

	}

}
