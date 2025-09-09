package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFHandleStreamStatusLookup extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject queryParams = env.getElementFromObject("incoming_request", "query_string_params").getAsJsonObject();

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		String streamId = OIDFJSON.tryGetString(queryParams.get("stream_id"));

		if (streamId == null) {
			resultObj.add("error", createErrorObj("bad_request", "missing stream_id parameter"));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream status request: missing stream_id parameter",
				args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			log("Failed to handle stream status request: No streams configured",
				args("stream_id", streamId, "error", resultObj.get("error")));
			return env;
		}

		JsonObject streamObj = streamsObj.getAsJsonObject(streamId);
		if (streamObj == null) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			log("Failed to handle stream status request: Stream not found",
				args("stream_id", streamId, "error", resultObj.get("error")));
			return env;
		}

		JsonObject streamStatus = streamObj.getAsJsonObject("_status");

		resultObj.addProperty("stream_id", streamId);
		resultObj.add("result", streamStatus);
		resultObj.addProperty("status_code", 200);
		logSuccess("Handled stream status request for stream_id=" + streamId, args("stream_id", streamId, "status", streamStatus));

		return env;
	}
}
