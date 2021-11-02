package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

public class EnsureSelfLinkEndsInConsentId extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		String consentId = OIDFJSON.getString(env.getElementFromObject("resource_endpoint_response", "data.consentId"));
		String selfLink = OIDFJSON.getString(env.getElementFromObject("resource_endpoint_response", "links.self"));
		Map<String, Object> data = Map.of("consentId", consentId,
			"self", selfLink);
		if(!selfLink.endsWith(consentId)) {
			throw error("The self link from the payment response did not have a consent ID on it",
				data);
		}
		logSuccess("Self link ended with consent ID as expected", data);
		return env;
	}

}
