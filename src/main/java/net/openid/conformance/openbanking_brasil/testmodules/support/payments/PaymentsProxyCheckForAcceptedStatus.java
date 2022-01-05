package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;
import java.util.Map;

public class PaymentsProxyCheckForAcceptedStatus extends AbstractJsonAssertingCondition {

	private static final List<String> ACCEPTED_STATUSES = List.of(
		"ACCC",
		"ACSC",
		"ACSP"
	);

	@Override
	public Environment evaluate(Environment env) {
		JsonObject responseBody = env.getObject("resource_endpoint_response");
		JsonObject data = responseBody.getAsJsonObject("data");
		String status = OIDFJSON.getString(data.get("status"));

		boolean checkStatus = env.getBoolean("payment_proxy_check_for_reject");

		log(checkStatus ? "Configured to check status" : "Not configured to check status", Map.of("status", status));
		if (checkStatus) {
			if (ACCEPTED_STATUSES.contains(status)) {
				env.putBoolean("consent_rejected", true);
			}
		}
		return env;
	}

}