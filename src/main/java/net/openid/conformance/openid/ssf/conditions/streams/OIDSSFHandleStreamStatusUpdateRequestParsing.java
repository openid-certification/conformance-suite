package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFHandleStreamStatusUpdateRequestParsing extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String rawStreamStatusInput = env.getString("incoming_request", "body");
		env.putString("ssf", "stream_status_input_raw", rawStreamStatusInput);

		JsonElement streamStatusUpdateInputEl = env.getElementFromObject("incoming_request", "body_json");
		String parseError = env.getString("incoming_request", "body_json_parse_error");

		if (streamStatusUpdateInputEl == null || parseError != null) {
			throw error("Failed to handle stream status update request: Failed to parse stream status input", args("stream_status_input_raw", rawStreamStatusInput, "parse_error", parseError));
		}

		JsonObject streamStatusInput = streamStatusUpdateInputEl.getAsJsonObject();
		env.putObject("ssf", "stream_status_input", streamStatusInput);

		logSuccess("Parsed stream status update");

		return env;
	}
}
