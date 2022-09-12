package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Base64;
import java.util.Set;

public class CheckPaymentPending extends AbstractCondition {

	private final Set<String> STATUS = Sets.newHashSet("PDNG", "PART");

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("resource_endpoint_response_full");
		String jwtBody = OIDFJSON.getString(response.get("body"));
		JsonObject body = new Gson().fromJson(new String(Base64.getUrlDecoder().decode(jwtBody.split("\\.")[1].getBytes())), JsonObject.class);
		String status = OIDFJSON.getString(body.getAsJsonObject("data").get("status"));
		log("Payment status extracted", args("status", status));
		env.putBoolean("payment_not_pending", !STATUS.contains(status));
		logSuccess(STATUS.contains(status) ? "Payment still pending" : "Payment not pending anymore");
		return env;
	}
}
