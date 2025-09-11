package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFHandleStreamDeleteRequest extends AbstractOIDSSFHandleReceiverRequest {

	private final OIDSSFEventStore eventStore;

	public OIDSSFHandleStreamDeleteRequest(OIDSSFEventStore eventStore) {
		this.eventStore = eventStore;
	}

	@Override
	public Environment evaluate(Environment env) {

		String streamId = env.getString("ssf", "current_stream_id");

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		if (streamId == null) {
			resultObj.add("error", createErrorObj("bad_request", "missing stream_id parameter"));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream deletion request", args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "No streams found"));
			resultObj.addProperty("status_code", 404);
			log("Failed to handle stream deletion request", args("stream_id", streamId, "error", resultObj.get("error")));
			return env;
		}

		JsonObject streamObj = streamsObj.getAsJsonObject(streamId);
		if (streamObj == null) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			log("Failed to handle stream deletion request", args("stream_id", streamId, "error", resultObj.get("error")));
			return env;
		}

		streamsObj.remove(streamId);

		resultObj.addProperty("stream_id", streamId);
		resultObj.addProperty("status_code", 204);
		logSuccess("Handled stream deletion request for stream_id=" + streamId, args("stream_id", streamId));

		eventStore.purgeStreamEvents(streamId);

		return env;
	}
}
