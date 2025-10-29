package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFHandleStreamDeleteRequest extends AbstractOIDSSFHandleReceiverRequest {

	private final OIDSSFEventStore eventStore;

	public OIDSSFHandleStreamDeleteRequest(OIDSSFEventStore eventStore) {
		this.eventStore = eventStore;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		JsonObject queryParams = env.getElementFromObject("incoming_request", "query_string_params").getAsJsonObject();

		if (!queryParams.has("stream_id")) {
			resultObj.add("error", createErrorObj("bad_request", "missing stream_id parameter"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream deletion request", args("error", resultObj.get("error")));
		}

		JsonElement streamIdEl = queryParams.get("stream_id");
		if (streamIdEl.isJsonNull()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream deletion request", args("stream_id", streamIdEl, "error", resultObj.get("error")));
		}

		String streamId = OIDFJSON.tryGetString(streamIdEl);

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "No streams found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream deletion request", args("stream_id", streamId, "error", resultObj.get("error")));
		}

		JsonObject streamObj = streamsObj.getAsJsonObject(streamId);
		if (streamObj == null) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream deletion request", args("stream_id", streamId, "error", resultObj.get("error")));
		}

		streamsObj.remove(streamId);

		resultObj.addProperty("stream_id", streamId);
		resultObj.addProperty("status_code", 204);
		logSuccess("Handled stream deletion request for stream_id=" + streamId, args("stream_id", streamId));

		eventStore.purgeStreamEvents(streamId);

		return env;
	}
}
