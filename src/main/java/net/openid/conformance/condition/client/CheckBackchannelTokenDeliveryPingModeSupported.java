package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class CheckBackchannelTokenDeliveryPingModeSupported extends AbstractValidateJsonArray {

	private final String environmentVariable = "backchannel_token_delivery_modes_supported";

	private final String pingMode = "ping";

	private final String errorMessageNotEnough = "No matching value from server";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(pingMode), 1, errorMessageNotEnough);

	}
}
