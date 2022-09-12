package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Base64;
import java.util.Objects;
import java.util.Set;

public class CheckPaymentAccepted extends AbstractCondition {

	private final Set<String> STATUS = Sets.newHashSet("ACSP", "ACSC", "ACCC");

	@Override
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("resource_endpoint_response_full");
		String jwtBody = OIDFJSON.getString(response.get("body"));
		JsonObject body = new Gson().fromJson(new String(Base64.getUrlDecoder().decode(jwtBody.split("\\.")[1].getBytes())), JsonObject.class);
		String status = OIDFJSON.getString(body.getAsJsonObject("data").get("status"));
		if (STATUS.contains(status)) {
			logSuccess("Payment status is accepted");
		} else {
			logFailure("Invalid payment status", args("status", status));
		}
		return env;
	}
}
