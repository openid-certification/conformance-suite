package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class SetPaymentCurrency extends AbstractCondition {
	@Override
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("consent_endpoint_request");
		JsonObject payment = request.getAsJsonObject("data").getAsJsonObject("payment");
		env.putString("old_currency", OIDFJSON.getString(payment.get("currency")));
		payment.addProperty("currency", "ZZZ");
		request.getAsJsonObject("data").add("payment", payment);
		env.putObject("consent_endpoint_request", request);

		return env;
	}}
