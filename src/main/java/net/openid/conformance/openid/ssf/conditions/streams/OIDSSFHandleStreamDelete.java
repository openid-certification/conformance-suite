package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFHandleStreamDelete extends AbstractOIDSSFHandleReceiverRequest {

	private final OIDSSFEventStore eventStore;

	public OIDSSFHandleStreamDelete(OIDSSFEventStore eventStore) {
		this.eventStore = eventStore;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject queryParams = env.getElementFromObject("incoming_request", "query_string_params").getAsJsonObject();

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		String streamId = OIDFJSON.tryGetString(queryParams.get("stream_id"));

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
