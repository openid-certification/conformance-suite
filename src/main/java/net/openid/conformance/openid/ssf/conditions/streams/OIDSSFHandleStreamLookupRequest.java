package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

public class OIDSSFHandleStreamLookupRequest extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		/*
		 * 8.1.1.2. Reading a Stream's Configuration
		 * The GET request MAY include the "stream_id" as a query parameter in order to identify the correct Event Stream.
		 */
		JsonObject queryParams = env.getElementFromObject("incoming_request", "query_string_params").getAsJsonObject();
		if (queryParams.has("stream_id")) {

			JsonElement streamIdEl = queryParams.get("stream_id");
			if (streamIdEl.isJsonNull()) {
				resultObj.add("error", createErrorObj("not_found", "Stream not found"));
				resultObj.addProperty("status_code", 404);
				throw error("Failed to handle stream lookup request: Could not find stream by stream_id", args("stream_id", streamIdEl));
			}

			String streamId = OIDFJSON.tryGetString(streamIdEl);

			JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
			if (streamConfigEl == null) {
				resultObj.add("error", createErrorObj("not_found", "Stream not found"));
				resultObj.addProperty("status_code", 404);
				throw error("Failed to handle stream lookup request: Could not find stream by stream_id", args("stream_id", streamId));
			}

			JsonObject streamConfig = streamConfigEl.getAsJsonObject();
			JsonObject streamConfigResult = copyConfigObjectWithoutInternalFields(streamConfig);

			resultObj.addProperty("stream_id", streamId);
			resultObj.add("result", streamConfigResult);
			resultObj.addProperty("status_code", 200);
			logSuccess("Handled stream lookup request: Found stream for stream_id=" + streamId, args("stream_id", streamId, "stream", streamConfigResult));
			return env;
		}

		/*
		 * If the "stream_id" parameter is missing, then the Transmitter MUST return a list of the stream configurations available
		 * to this Receiver. In the event that there are no Event Streams configured, the Transmitter MUST return an empty list.
		 */
		// Find all configured streams
		JsonElement streamConfigEl = env.getElementFromObject("ssf", "streams");
		if (streamConfigEl == null) {

			// no streams are configured
			log("Handled stream lookup request: Could not find any streams");
			resultObj.add("result", new JsonArray());
			resultObj.addProperty("status_code", 200);
			return env;
		}

		JsonObject streamsObj = streamConfigEl.getAsJsonObject();
		List<JsonObject> streams = new ArrayList<>();
		for(String streamIdKey : streamsObj.keySet()) {
			JsonObject streamConfig = streamsObj.getAsJsonObject(streamIdKey);

			JsonObject streamConfigResult = copyConfigObjectWithoutInternalFields(streamConfig);

			streams.add(streamConfigResult);
		}

		logSuccess("Handled stream lookup request: Found " + streams.size() + " streams",  args("streams", streams));
		resultObj.add("result", OIDFJSON.convertJsonObjectListToJsonArray(streams));
		resultObj.addProperty("status_code", 200);
		return env;
	}
}
