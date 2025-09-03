package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

public class OIDSSFHandleStreamLookup extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject queryParams = env.getElementFromObject("incoming_request", "query_string_params").getAsJsonObject();

		String streamId = OIDFJSON.tryGetString(queryParams.get("stream_id"));

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		// if stream_id parameter is present use lookup stream directly
		if (streamId != null) {
			// explicit stream lookup

			JsonElement streamConfigEl = env.getElementFromObject("ssf", "streams." + streamId);
			if (streamConfigEl == null) {
				log("Could not find stream by stream_id", args("stream_id", streamId));
				resultObj.addProperty("status_code", 404);
				return env;
			}

			JsonObject streamConfig = streamConfigEl.getAsJsonObject();
			JsonObject streamConfigResult = copyStreamConfigWithoutInternalFields(streamConfig);

			resultObj.add("result", streamConfigResult);
			resultObj.addProperty("status_code", 200);
			log("Found stream by stream_id", args("stream_id", streamId, "stream", streamConfigResult));
			return env;
		}

		// Find all configured streams
		JsonElement streamConfigEl = env.getElementFromObject("ssf", "streams");
		if (streamConfigEl == null) {

			// no streams are configured
			log("Could not find any streams");
			resultObj.add("result", new JsonArray());
			resultObj.addProperty("status_code", 200);
			return env;
		}

		JsonObject streamsObj = streamConfigEl.getAsJsonObject();
		List<JsonObject> streams = new ArrayList<>();
		for(String streamIdKey : streamsObj.keySet()) {
			JsonObject streamConfig = streamsObj.getAsJsonObject(streamIdKey);

			JsonObject streamConfigResult = copyStreamConfigWithoutInternalFields(streamConfig);

			streams.add(streamConfigResult);
		}

		log("Found streams",  args("streams", streams));
		resultObj.add("result", OIDFJSON.convertJsonObjectListToJsonArray(streams));
		resultObj.addProperty("status_code", 200);
		return env;
	}
}
