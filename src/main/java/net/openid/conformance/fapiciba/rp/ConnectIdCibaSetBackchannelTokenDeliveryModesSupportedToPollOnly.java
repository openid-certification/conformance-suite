package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ConnectIdCibaSetBackchannelTokenDeliveryModesSupportedToPollOnly extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonArray data = new JsonArray();
		data.add("poll");

		JsonObject server = env.getObject("server");
		server.add("backchannel_token_delivery_modes_supported", data);

		logSuccess("Set backchannel_token_delivery_modes_supported to poll only", args("values", data));

		return env;
	}
}
