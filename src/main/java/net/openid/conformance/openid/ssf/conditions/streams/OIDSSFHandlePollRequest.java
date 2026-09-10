package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEventAckConsumer;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEventErrorConsumer;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore.EventsBatch;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamStatusValue;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestLockManager;

import java.util.List;
import java.util.Map;

public class OIDSSFHandlePollRequest extends AbstractOIDSSFHandleReceiverRequest {

	/**
	 * Key under {@code ssf} of the object counting, per variation name, the poll requests the
	 * receiver made (RFC 8936 2.4): {@link #VARIATION_POLL_ONLY}, {@link #VARIATION_ACKNOWLEDGE_ONLY},
	 * {@link #VARIATION_POLL_WITH_ACKNOWLEDGEMENT}, and independently {@link #VARIATION_LONG_POLL}
	 * or {@link #VARIATION_SHORT_POLL} for the {@code returnImmediately} choice.
	 */
	public static final String POLL_REQUEST_VARIATIONS_KEY = "poll_request_variations";

	/** No acknowledgement, events requested (RFC 8936 2.4.1). */
	public static final String VARIATION_POLL_ONLY = "poll-only";

	/** Acknowledgement with {@code maxEvents} 0 (RFC 8936 2.4.2). */
	public static final String VARIATION_ACKNOWLEDGE_ONLY = "acknowledge-only";

	/** Acknowledgement and events requested in one request (RFC 8936 2.4.3). */
	public static final String VARIATION_POLL_WITH_ACKNOWLEDGEMENT = "poll-with-acknowledgement";

	/** {@code returnImmediately} false or absent (RFC 8936 2.2). */
	public static final String VARIATION_LONG_POLL = "long poll";

	/** {@code returnImmediately} true. */
	public static final String VARIATION_SHORT_POLL = "short poll";

	protected final OIDSSFEventStore eventStore;

	protected OIDSSFEventAckConsumer onStreamEventAcknowledged;

	protected OIDSSFEventErrorConsumer onStreamEventErrorReported;

	public OIDSSFHandlePollRequest(OIDSSFEventStore eventStore, OIDSSFEventAckConsumer onStreamEventAcknowledged, OIDSSFEventErrorConsumer onStreamEventErrorReported) {
		this.eventStore = eventStore;
		this.onStreamEventAcknowledged = onStreamEventAcknowledged;
		this.onStreamEventErrorReported = onStreamEventErrorReported;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "poll_result", resultObj);

		JsonObject queryParams = env.getElementFromObject("incoming_request", "query_string_params").getAsJsonObject();

		String streamId = OIDFJSON.tryGetString(queryParams.get("stream_id"));

		if (streamId == null) {
			resultObj.add("error", createErrorObj("invalid_request", "Missing stream_id in request parameter"));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream poll request: Missing stream_id in request parameter", args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			log("Failed to handle stream poll request: No streams configured", args("error", resultObj.get("error")));
			return env;
		}

		JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfigEl == null) {
			log("Failed to handle stream poll request: Stream not found", args("stream_id", streamId));
			resultObj.addProperty("status_code", 404);
			return env;
		}

		JsonObject pollRequestInput;
		try {
			pollRequestInput = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("parsing_error", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream poll request: Failed to parse request body", args("error", resultObj.get("error")));
			return env;
		}

		JsonElement returnImmediatelyEl = pollRequestInput.get("returnImmediately");
		JsonElement maxEventsEl = pollRequestInput.get("maxEvents");
		JsonArray ackArrayEl;
		JsonElement setErrsEl;
		boolean returnImmediately;
		int maxCount;
		try {
			// RFC 8936 2.2 defines the request parameter types; malformed values are a
			// client error (400), never a transmitter crash.
			ackArrayEl = pollRequestInput.getAsJsonArray("ack");
			setErrsEl = pollRequestInput.getAsJsonObject("setErrs");
			returnImmediately = returnImmediatelyEl != null && OIDFJSON.getBoolean(returnImmediatelyEl);
			maxCount = maxEventsEl != null ? OIDFJSON.getInt(maxEventsEl) : 16;
		} catch (RuntimeException e) {
			resultObj.add("error", createErrorObj("invalid_request", "Malformed poll request parameters: 'maxEvents' must be an integer, 'returnImmediately' a boolean, 'ack' a JSON array and 'setErrs' a JSON object (RFC 8936 2.2)"));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream poll request: malformed request parameters", args("error", resultObj.get("error"), "poll_request", pollRequestInput));
			return env;
		}

		recordPollRequestVariation(env, ackArrayEl, setErrsEl, maxCount, returnImmediately);

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		JsonObject setsObject = new JsonObject();

		JsonObject pollResultObj = new JsonObject();
		pollResultObj.add("sets", setsObject);

		// Acknowledgements and error reports concern SETs the receiver already holds
		// (RFC 8936 2.4), so they are processed whatever the stream status; only the
		// delivery of further events depends on it (SSF 1.0 8.1.2.1).

		// process acknowledgements if necessary
		if (ackArrayEl != null && !ackArrayEl.isEmpty()) {
			List<String> acks = OIDFJSON.convertJsonArrayToList(ackArrayEl);
			log("Process acknowledgements for stream events for stream_id=" + streamId, args("ack", acks));
			for (String jti : acks) {
				OIDSSFSecurityEvent ackedEvent = eventStore.registerAckForStreamEvent(streamId, jti);
				if (ackedEvent == null) {
					log("Receiver acknowledged unknown SET jti=" + jti + " for stream_id=" + streamId,
						args("jti", jti, "stream_id", streamId));
					continue;
				}
				onStreamEventAcknowledged.accept(streamId, jti, ackedEvent);
			}
		}

		// process errors if necessary
		if (setErrsEl != null) {
			JsonObject setErrsObj = setErrsEl.getAsJsonObject();
			log("Process errors for stream events for stream_id=" + streamId, args("setErrs", setErrsObj));
			for (Map.Entry<String, JsonElement> entry : setErrsObj.entrySet()) {
				String jti = entry.getKey();
				JsonObject errorObj = entry.getValue().getAsJsonObject();
				eventStore.registerErrorForStreamEvent(streamId, jti, errorObj);
				onStreamEventErrorReported.accept(streamId, jti, errorObj);
			}
		}

		// retrieve events if necessary
		StreamStatusValue streamStatus = OIDSSFStreamUtils.getStreamStatusValue(streamConfig);
		if (!streamStatus.isEventDeliveryEnabled()) {
			// SSF 1.0 8.1.2.1: a paused or disabled stream "MUST NOT transmit events"; queued
			// events stay held until the receiver enables the stream again.
			log("Stream is " + streamStatus + ": acknowledgements were processed, no events are delivered", args("stream_id", streamId, "status", streamStatus.name()));
		} else if (maxCount > 0) {

			log("Deliver stream events for stream_id=" + streamId, args("maxCount", maxCount, "returnImmediately", returnImmediately));
			int maxWaitTimeSeconds = 10;
			boolean waitForEvents = !returnImmediately;

			EventsBatch eventsBatch = pollEventsReleasingLockWhileWaiting(streamId, maxCount, waitForEvents, maxWaitTimeSeconds);

			// merge jti-SET pairs into the poll response
			// see: https://www.rfc-editor.org/rfc/rfc8936.html#section-2.5
			for (OIDSSFSecurityEvent setEvent : eventsBatch.events()) {
				setsObject.addProperty(setEvent.jti(), setEvent.securityEventToken());
			}

			if (eventsBatch.moreAvailable()) {
				pollResultObj.addProperty("moreAvailable", true);
			}
		}

		logSuccess("Handled stream events polling request for stream_id=" + streamId, args("stream_id", streamId, "polling_request", pollRequestInput));

		resultObj.addProperty("stream_id", streamId);
		resultObj.add("result", pollResultObj);
		resultObj.addProperty("status_code", 200);
		return env;
	}

	/**
	 * Counts the request under its RFC 8936 2.4 variation in {@code ssf.poll_request_variations}.
	 * A request acknowledges when {@code ack} or {@code setErrs} names at least one SET; an
	 * empty {@code ack} array acknowledges nothing and counts as poll-only, as does a request
	 * that neither acknowledges nor asks for events.
	 */
	protected void recordPollRequestVariation(Environment env, JsonArray ackArrayEl, JsonElement setErrsEl, int maxCount, boolean returnImmediately) {
		boolean acknowledges = (ackArrayEl != null && !ackArrayEl.isEmpty())
			|| (setErrsEl != null && setErrsEl.isJsonObject() && !setErrsEl.getAsJsonObject().isEmpty());
		String variation;
		if (!acknowledges) {
			variation = VARIATION_POLL_ONLY;
		} else if (maxCount == 0) {
			variation = VARIATION_ACKNOWLEDGE_ONLY;
		} else {
			variation = VARIATION_POLL_WITH_ACKNOWLEDGEMENT;
		}
		String pollKind = returnImmediately ? VARIATION_SHORT_POLL : VARIATION_LONG_POLL;

		JsonElement variationsEl = env.getElementFromObject("ssf", POLL_REQUEST_VARIATIONS_KEY);
		JsonObject variations;
		if (variationsEl != null && variationsEl.isJsonObject()) {
			variations = variationsEl.getAsJsonObject();
		} else {
			variations = new JsonObject();
			env.putObject("ssf", POLL_REQUEST_VARIATIONS_KEY, variations);
		}
		for (String name : List.of(variation, pollKind)) {
			int count = variations.has(name) ? OIDFJSON.getInt(variations.get(name)) : 0;
			variations.addProperty(name, count + 1);
		}
		log("Poll request uses the " + variation + " variation as a " + pollKind,
			args("variation", variation, "poll_kind", pollKind, "poll_request_variations", variations.deepCopy()));
	}

	/**
	 * A long poll (RFC 8936 2.2: {@code returnImmediately} defaults to false) may wait seconds
	 * for an event. This condition runs on a request thread that holds the test lock, so the
	 * lock is released for the wait - as the framework does around outbound HTTP calls -
	 * otherwise every other request the receiver makes meanwhile (an acknowledgement, a jwks
	 * fetch, a delete) queues behind this one.
	 */
	protected EventsBatch pollEventsReleasingLockWhileWaiting(String streamId, int maxCount, boolean waitForEvents, int maxWaitTimeSeconds) {
		TestLockManager lockManager = waitForEvents ? getLockManager() : null;
		if (lockManager != null) {
			lockManager.releaseLock();
		}
		try {
			return eventStore.pollEvents(streamId, maxCount, waitForEvents, maxWaitTimeSeconds);
		} finally {
			if (lockManager != null) {
				lockManager.reacquireLock();
			}
		}
	}
}
