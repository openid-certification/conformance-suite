package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFHandleStreamRequestBodyParsing extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		String rawStreamInput = env.getString("incoming_request", "body");
		env.putString("ssf", "stream_input_raw", rawStreamInput);

		String parseError = env.getString("incoming_request", "body_json_parse_error");

		JsonObject streamConfigInput = OIDSSFStreamUtils.getStreamFromRequestBody(env);
		if (streamConfigInput == null || parseError != null) {
			env.removeElement("ssf", "stream_input");
			throw error("Failed to parse JSON in stream request: Stream config missing or invalid", args("error", "Could not find stream config in request body", "unparsed_stream_input", rawStreamInput, "parse_error", parseError));
		}

		env.putObject("ssf", "stream_input", streamConfigInput);

		logSuccess("Found stream configuration in request body", args("stream_config", streamConfigInput));

		return env;
	}
}
