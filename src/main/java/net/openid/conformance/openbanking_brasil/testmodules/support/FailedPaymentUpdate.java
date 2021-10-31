package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Base64;

public class FailedPaymentUpdate extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		JsonObject resource = env.getObject("resource_endpoint_response_full");
		String jwtBody = OIDFJSON.getString(resource.get("body")).split("\\.")[1];
		String decodedBody = new String(Base64.getUrlDecoder().decode(jwtBody.getBytes()));
		JsonObject body = new JsonParser().parse(decodedBody).getAsJsonObject();
		body = body.getAsJsonObject("data");
		String paymentStatus = OIDFJSON.getString(body.get("status"));

		log("Payment not accepted or rejected yet: " + paymentStatus);
		return env;
	}
}
