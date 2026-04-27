package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.ExtractDCQLQueryFromAuthorizationRequest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddUnknownTypeTransactionDataToAuthorizationEndpointRequest extends AbstractCondition {

	static final String UNKNOWN_TYPE = "conformance-test-unknown-type-must-be-rejected";

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		JsonObject dcqlQuery = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);

		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		if (credentials == null || credentials.isEmpty()) {
			throw error("dcql_query.credentials must be a non-empty array to bind transaction_data to",
				args("dcql_query", dcqlQuery));
		}
		String credentialId = OIDFJSON.getString(credentials.get(0).getAsJsonObject().get("id"));

		JsonObject transactionDataEntry = new JsonObject();
		transactionDataEntry.addProperty("type", UNKNOWN_TYPE);
		JsonArray credentialIds = new JsonArray();
		credentialIds.add(credentialId);
		transactionDataEntry.add("credential_ids", credentialIds);

		String encoded = Base64URL.encode(transactionDataEntry.toString()).toString();

		JsonArray transactionData = new JsonArray();
		transactionData.add(encoded);
		authorizationEndpointRequest.add("transaction_data", transactionData);

		logSuccess("Added transaction_data parameter with an unknown type",
			args("transaction_data_decoded", transactionDataEntry,
				"transaction_data_encoded", encoded,
				"authorization_endpoint_request", authorizationEndpointRequest));

		return env;
	}

}
