package net.openid.conformance.fapiciba;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.AbstractValidateJsonArray;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CheckDiscEndpointTokenDeliveryModeSupportedIsPollOnly extends AbstractValidateJsonArray {

	private final String environmentVariable = "backchannel_token_delivery_modes_supported";

	private final String pollMode = "poll";

	@Override
	public Environment evaluate(Environment env) {
		List<String> setValues = List.of(pollMode);
		JsonElement serverValues = env.getElementFromObject("server", environmentVariable);
		if(serverValues == null || !serverValues.isJsonArray() || serverValues.getAsJsonArray().isEmpty()) {
			throw error(environmentVariable + " is null, empty or not an array", args("expected", setValues, "actual", serverValues));
		}

		long matchingElements = countMatchingElements(setValues, serverValues.getAsJsonArray());
		if(matchingElements != 1 || serverValues.getAsJsonArray().size() > 1) {
			throw error(environmentVariable + " must contain poll only", args("expected", setValues, "actual", serverValues));
		}

		logSuccess("Token delivery mode supported is poll only", args(environmentVariable, serverValues));
		return env;
	}

}
