package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFHandleStreamStatusUpdateRequestParsing extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		JsonObject streamStatusInput;
		try {
			streamStatusInput = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		} catch (Exception e) {
			throw error("Failed to handle stream status update request: Failed to parse stream status input");
		}

		env.putObject("ssf", "stream_status_input", streamStatusInput);

		logSuccess("Parsed stream status update");

		return env;
	}
}
