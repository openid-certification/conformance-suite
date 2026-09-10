package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * SSF 1.0 8.1.2.1, status {@code paused}: "The Transmitter MUST NOT transmit events over the
 * stream."
 * <p>
 * Runs against the push request most recently taken from the push queue
 * ({@code ssf.push_request}, received at {@code ssf.push_request_received_at}). A push that
 * reached the receiver before the transmitter acknowledged the pause
 * ({@code ssf.stream_paused_at}) was sent while the stream was still enabled and is accepted;
 * one received after the pause was acknowledged is a violation.
 */
public class OIDSSFEnsureNoSetPushedWhileStreamPaused extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement pushRequestEl = env.getElementFromObject("ssf", "push_request");
		if (pushRequestEl == null) {
			logSuccess("No push request was received while the stream was paused");
			return env;
		}

		Instant pausedAt = parseInstant(env.getString("ssf", "stream_paused_at"), "ssf.stream_paused_at");
		Instant receivedAt = parseInstant(env.getString("ssf", "push_request_received_at"), "ssf.push_request_received_at");

		if (receivedAt.isBefore(pausedAt)) {
			logSuccess("The push request was received before the transmitter acknowledged the pause; it was sent while the stream was still enabled",
				args("push_request_received_at", receivedAt.toString(), "stream_paused_at", pausedAt.toString()));
			return env;
		}

		throw error("The transmitter pushed a SET while the stream status was 'paused'. "
				+ "No events may be transmitted over a paused stream.",
			args("push_request_received_at", receivedAt.toString(), "stream_paused_at", pausedAt.toString(),
				"push_request", pushRequestEl));
	}

	private Instant parseInstant(String value, String key) {
		if (value == null) {
			throw error("Missing timestamp in environment", args("missing", key));
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException e) {
			throw error("Invalid timestamp in environment", args("key", key, "value", value, "error", e.getMessage()));
		}
	}
}
