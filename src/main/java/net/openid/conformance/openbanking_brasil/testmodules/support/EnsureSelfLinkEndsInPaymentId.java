package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class EnsureSelfLinkEndsInPaymentId extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		String paymentId = OIDFJSON.getString(env.getElementFromObject("resource_endpoint_response", "data.paymentId"));
		String selfLink = OIDFJSON.getString(env.getElementFromObject("resource_endpoint_response", "links.self"));
		Map<String, Object> data = Map.of("paymentId", paymentId,
			"self", selfLink);
		if(!selfLink.endsWith(paymentId)) {
			throw error("The self link from the payment response did not have a payment ID on it",
				data);
		}
		logSuccess("Self link ended with payment ID as expected", data);
		return env;
	}

}
