package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * SSF 1.0 8.1.1.2: without a {@code stream_id} "the Transmitter MUST return a list of the
 * stream configurations available to this Receiver. In the event that there are no Event
 * Streams configured, the Transmitter MUST return an empty list." Checks the list response of
 * {@link OIDSSFListStreamConfigsCall} ({@code ssf.stream_list}): it must be a JSON array of
 * stream configurations, and the module's stream ({@code ssf.stream.stream_id}) must be in
 * it, or must be absent from it after it was deleted.
 */
public class OIDSSFEnsureStreamListContainsStream extends AbstractCondition {

	private final boolean expectPresent;

	/**
	 * @param expectPresent {@code true} when the module's stream exists and must be listed,
	 *                      {@code false} after it was deleted
	 */
	public OIDSSFEnsureStreamListContainsStream(boolean expectPresent) {
		this.expectPresent = expectPresent;
	}

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement listEl = env.getElementFromObject("ssf", "stream_list");
		if (listEl == null || !listEl.isJsonArray()) {
			throw error("A read of the configuration endpoint without stream_id must return a JSON array of stream configurations",
				args("response_body", env.getElementFromObject("resource_endpoint_response_full", "body_json")));
		}
		JsonArray streams = listEl.getAsJsonArray();

		String streamId = env.getString("ssf", "stream.stream_id");
		if (streamId == null) {
			throw error("Could not find the stream_id of the module's stream to look for in the list");
		}

		List<String> listedStreamIds = new ArrayList<>();
		for (JsonElement streamEl : streams) {
			if (!streamEl.isJsonObject()) {
				throw error("The stream list contains an entry that is not a stream configuration object", args("entry", streamEl, "streams", streams));
			}
			listedStreamIds.add(OIDFJSON.tryGetString(streamEl.getAsJsonObject().get("stream_id")));
		}

		boolean listed = listedStreamIds.contains(streamId);
		if (expectPresent && !listed) {
			throw error("The stream list does not contain the stream created by this test",
				args("stream_id", streamId, "listed_stream_ids", listedStreamIds));
		}
		if (!expectPresent && listed) {
			throw error("The stream list still contains the stream this test deleted",
				args("stream_id", streamId, "listed_stream_ids", listedStreamIds));
		}

		if (expectPresent) {
			logSuccess("The stream list contains the stream created by this test", args("stream_id", streamId, "listed_stream_ids", listedStreamIds));
		} else if (streams.isEmpty()) {
			logSuccess("The stream list is empty after the stream was deleted", args("stream_id", streamId));
		} else {
			logSuccess("The stream list no longer contains the deleted stream; the remaining entries are other streams of this receiver",
				args("stream_id", streamId, "listed_stream_ids", listedStreamIds));
		}

		return env;
	}
}
