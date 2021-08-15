package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreatePaymentRequestEntityClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	@PostEnvironment(required = "resource_request_entity_claims")
	public Environment evaluate(Environment env) {
		JsonElement pixPayment = env.getElementFromObject("resource", "brazilPixPayment");
		if(pixPayment == null || !pixPayment.isJsonObject()) {
			throw error("As 'payments' is included in the 'scope' within the test configuration, a payment initiation request JSON object must also be provided in the test configuration.");
		}
		env.putObject("resource_request_entity_claims", (JsonObject)pixPayment);

		logSuccess(args("resource_request_entity_claims", pixPayment));
		return env;
	}

}
