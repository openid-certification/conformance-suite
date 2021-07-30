package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddPaymentRequestEntity extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	@PostEnvironment(strings = "resource_request_entity")
	public Environment evaluate(Environment env) {
		JsonElement pixPayment = env.getElementFromObject("resource", "brazilPixPayment");
		if(pixPayment == null) {
			throw error("As 'payments' is included in the 'scope' within the test configuration, a payment initiation request JSON object must also be provided in the test configuration.");
		}
		String paymentRequest = pixPayment.toString();
		env.putString("resource_request_entity", paymentRequest);

		logSuccess(args("resource_request_entity", paymentRequest));
		return env;
	}

}
