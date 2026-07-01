package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddTransactionIdToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "transaction_id", required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String transactionId = env.getString("transaction_id");
		if (Strings.isNullOrEmpty(transactionId)) {
			throw error("Couldn't find transaction_id value");
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("transaction_id", transactionId);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added transaction_id parameter to request", authorizationEndpointRequest);

		return env;

	}

}
