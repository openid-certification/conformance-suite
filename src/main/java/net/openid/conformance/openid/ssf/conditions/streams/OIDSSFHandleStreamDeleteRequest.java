package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;
import java.util.function.BiConsumer;

public class OIDSSFHandleStreamDeleteRequest extends AbstractOIDSSFHandleReceiverRequest {

	private final OIDSSFEventStore eventStore;

	private final BiConsumer<String, List<OIDSSFSecurityEvent>> onEventsUndeliverable;

	public OIDSSFHandleStreamDeleteRequest(OIDSSFEventStore eventStore) {
		this(eventStore, (streamId, events) -> {
		});
	}

	public OIDSSFHandleStreamDeleteRequest(OIDSSFEventStore eventStore, BiConsumer<String, List<OIDSSFSecurityEvent>> onEventsUndeliverable) {
		this.eventStore = eventStore;
		this.onEventsUndeliverable = onEventsUndeliverable;
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

		boolean pushDelivery = OIDSSFStreamUtils.isPushDelivery(streamObj);

		streamsObj.remove(streamId);

		resultObj.addProperty("stream_id", streamId);
		resultObj.addProperty("status_code", 204);
		logSuccess("Handled stream deletion request for stream_id=" + streamId, args("stream_id", streamId));

		// Deleting the stream purges its event store; anything not delivered (and, for poll,
		// not acknowledged) by now can never be acknowledged. Record those events before the
		// purge so waiting test modules stop expecting acks for them instead of timing out.
		// - push: still-queued events were never handed to the push task. (Events of the
		//   currently-executing push batch are already drained from the queue; the push task
		//   records those itself when it detects the deletion mid-batch.) Push acks are
		//   tracked by the modules per delivery response, not in the event store.
		// - poll: the receiver acknowledges via the event store, so everything the store has
		//   not seen an ack for - still queued or retrieved-but-unacknowledged - is stranded.
		List<OIDSSFSecurityEvent> undeliverableEvents = pushDelivery
			? eventStore.getQueuedEvents(streamId)
			: eventStore.getUnacknowledgedEvents(streamId);
		if (!undeliverableEvents.isEmpty()) {
			onEventsUndeliverable.accept(streamId, undeliverableEvents);
		}

		eventStore.purgeStreamEvents(streamId);

		return env;
	}
}
