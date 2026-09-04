package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.fapiciba.OpenBankingBrazilCibaMaximumExpiry;
import net.openid.conformance.testmodule.Environment;

public class AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "authorization_endpoint_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		OpenBankingBrazilCibaMaximumExpiry.MaximumExpiry maximumExpiry;
		try {
			maximumExpiry = OpenBankingBrazilCibaMaximumExpiry.resolve(env);
		} catch (IllegalArgumentException e) {
			throw error(e.getMessage());
		}

		int requestedExpiry = maximumExpiry.seconds() + 1;
		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");
		authorizationEndpointRequest.addProperty("requested_expiry", requestedExpiry);

		logSuccess("Added requested expiry above the configured maximum to authorization endpoint request", args(
			"requested_expiry", requestedExpiry,
			"maximum_expiry", maximumExpiry.seconds(),
			"maximum_expiry_explicitly_configured", maximumExpiry.explicitlyConfigured()));
		return env;
	}
}
