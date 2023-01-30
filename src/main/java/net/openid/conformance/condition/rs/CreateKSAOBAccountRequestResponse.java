package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateKSAOBAccountRequestResponse extends AbstractOpenBankingApiResponse {

	@Override
	@PreEnvironment(strings = {"account_request_id", "fapi_interaction_id"})
	@PostEnvironment(required = {"account_request_response", "account_request_response_headers"})
	public Environment evaluate(Environment env) {

		String accountRequestId = env.getString("account_request_id");

		JsonObject data = new JsonObject();
		data.addProperty("Status", "AwaitingAuthorization");
		JsonArray permissions = new JsonArray();
		permissions.add("ReadAccountsBasic");
		data.add("Permissions", permissions );
		data.addProperty("ConsentId", accountRequestId);
		data.addProperty("TransactionFromDateTime", "2016-01-01T10:40:00+02:00");
		data.addProperty("TransactionToDateTime", "2025-12-31T10:40:00+02:00");
		data.addProperty("ExpirationDateTime", "2025-12-31T10:40:00+02:00");
		data.addProperty("CreationDateTime", "2023-01-29T03:35:56.745Z");
		data.addProperty("StatusUpdateDateTime", "2023-01-29T03:35:56.745Z");

		JsonObject response = createResponse(data);

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);

		logSuccess("Created account request response", args("account_request_response", response, "account_request_response_headers", headers));

		env.putObject("account_request_response", response);
		env.putObject("account_request_response_headers", headers);

		return env;

	}

}
