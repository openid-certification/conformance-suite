package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckPollStatus extends AbstractJsonAssertingCondition {

	@Override
	public Environment evaluate(Environment env) {
		JsonObject responseBody = env.getObject("resource_endpoint_response");

		JsonObject data = responseBody.getAsJsonObject("data");
		String status = OIDFJSON.getString(data.get("status"));

		if (status.equals("PDNG") || status.equals("PART")) {
			env.putBoolean("payment_proxy_check_for_reject", false);
			logSuccess("Status still in a PDNG or PART state");
		} else {
			env.putBoolean("payment_proxy_check_for_reject", true);
			logSuccess("Status no longer in a PDNG or PART state");
		}
		return env;
	}

}
