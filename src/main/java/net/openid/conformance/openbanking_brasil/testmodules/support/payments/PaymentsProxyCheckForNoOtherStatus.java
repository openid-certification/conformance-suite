package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;
import java.util.Map;

public class PaymentsProxyCheckForNoOtherStatus extends AbstractJsonAssertingCondition {

	private static final List<String> ACCEPTED_STATUSES = List.of(
		"SASC",
		"PNDG",
		"PART",
		"SASP"
	);

	@Override
	public Environment evaluate(Environment env) {
		JsonObject responseBody = env.getObject("resource_endpoint_response");
		JsonObject data = responseBody.getAsJsonObject("data");
		String status = OIDFJSON.getString(data.get("status"));

		if (!ACCEPTED_STATUSES.contains(status)) {
			throw error("Unaccaptable status returned", Map.of("status", status));
		}
		return env;
	}

}
