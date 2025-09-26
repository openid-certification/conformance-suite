package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFHandleStreamDeleteRequestValidation extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonObject queryParams = env.getElementFromObject("incoming_request", "query_string_params").getAsJsonObject();

		String streamId = OIDFJSON.tryGetString(queryParams.get("stream_id"));

		if (streamId == null) {
			throw error("Missing stream_id parameter in delete stream request");
		}

		env.putString("ssf", "current_stream_id", streamId);

		logSuccess("Found valid parameters in delete stream request");

		return env;
	}
}
