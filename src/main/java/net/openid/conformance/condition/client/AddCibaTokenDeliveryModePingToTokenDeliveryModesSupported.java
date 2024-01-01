package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddCibaTokenDeliveryModePingToTokenDeliveryModesSupported extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray cibaModes = env.getElementFromObject("server", "backchannel_token_delivery_modes_supported").getAsJsonArray();

		cibaModes.add("ping");

		log("Added CIBA token delivery mode ping to backchannel_token_delivery_modes_supported", args("backchannel_token_delivery_modes_supported", cibaModes));

		return env;
	}
}
