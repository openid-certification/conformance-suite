package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class ConnectIdCibaCheckBackchannelTokenDeliveryModesSupportedOnlyPoll extends AbstractCondition {

	private static final String METADATA_KEY = "backchannel_token_delivery_modes_supported";
	private static final List<String> EXPECTED_VALUES = List.of("poll");

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement serverValues = env.getElementFromObject("server", METADATA_KEY);

		if (serverValues == null) {
			throw error(METADATA_KEY + ": not found", args("discovery_metadata_key", METADATA_KEY, "expected", EXPECTED_VALUES));
		}

		if (!serverValues.isJsonArray()) {
			throw error("'" + METADATA_KEY + "' should be an array",
				args("discovery_metadata_key", METADATA_KEY, "expected", EXPECTED_VALUES, "actual", serverValues));
		}

		JsonArray values = serverValues.getAsJsonArray();
		if (values.size() != EXPECTED_VALUES.size()) {
			throw error(METADATA_KEY + " must contain only poll",
				args("discovery_metadata_key", METADATA_KEY, "expected", EXPECTED_VALUES, "actual", values));
		}

		for (JsonElement value : values) {
			if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || !"poll".equals(OIDFJSON.getString(value))) {
				throw error(METADATA_KEY + " must contain only poll",
					args("discovery_metadata_key", METADATA_KEY, "expected", EXPECTED_VALUES, "actual", values));
			}
		}

		logSuccess(METADATA_KEY + " contains only poll", args("actual", values, "expected", EXPECTED_VALUES));
		return env;
	}
}
