package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilCreatePaymentConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config" )
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		throw error("Payment support has been disabled as it does not support signed payment requests. It will be reenabled once support for the signed API request/response has been added.");
	}
}
