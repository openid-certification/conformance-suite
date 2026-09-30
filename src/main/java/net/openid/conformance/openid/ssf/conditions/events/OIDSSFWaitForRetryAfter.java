package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.common.AbstractWaitForSpecifiedSeconds;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;

/**
 * Waits before repeating a verification request the transmitter answered with 429. SSF 1.0
 * 8.1.1 lets a transmitter answer 429 when verification requests come more often than its
 * {@code min_verification_interval}. The wait is the {@code Retry-After} of the 429 response
 * (delta-seconds) when present, else the advertised {@code min_verification_interval}, else a
 * short default; capped so a transmitter cannot stall the test.
 * <p>
 * Reads the 429 response from {@code resource_endpoint_response_full} and the stream from
 * {@code ssf.stream}.
 */
public class OIDSSFWaitForRetryAfter extends AbstractWaitForSpecifiedSeconds {

	protected static final long DEFAULT_WAIT_SECONDS = 10;

	protected static final long MAX_WAIT_SECONDS = 600;

	@Override
	protected long getExpectedWaitSeconds(Environment env) {
		Long wait = retryAfterSeconds(env);
		String source = "Retry-After header";
		if (wait == null) {
			JsonElement intervalEl = env.getElementFromObject("ssf", "stream.min_verification_interval");
			if (intervalEl != null && intervalEl.isJsonPrimitive() && intervalEl.getAsJsonPrimitive().isNumber()) {
				wait = OIDFJSON.getLong(intervalEl);
				source = "min_verification_interval";
			}
		}
		if (wait == null || wait <= 0) {
			wait = DEFAULT_WAIT_SECONDS;
			source = "default";
		}
		if (wait > MAX_WAIT_SECONDS) {
			log("The wait before repeating the verification request exceeds the cap; waiting only the capped duration",
				args("wait_seconds", wait, "cap_seconds", MAX_WAIT_SECONDS, "source", source));
			wait = MAX_WAIT_SECONDS;
		}
		log("Waiting before repeating the verification request the transmitter answered with 429",
			args("wait_seconds", wait, "source", source));
		return wait;
	}

	private Long retryAfterSeconds(Environment env) {
		JsonElement headersEl = env.getElementFromObject("resource_endpoint_response_full", "headers");
		if (headersEl == null || !headersEl.isJsonObject()) {
			return null;
		}
		for (Map.Entry<String, JsonElement> header : headersEl.getAsJsonObject().entrySet()) {
			if (!"retry-after".equalsIgnoreCase(header.getKey())) {
				continue;
			}
			JsonElement value = header.getValue();
			String text = value.isJsonArray() && !value.getAsJsonArray().isEmpty()
				? OIDFJSON.tryGetString(value.getAsJsonArray().get(0))
				: OIDFJSON.tryGetString(value);
			if (text == null) {
				return null;
			}
			try {
				return Long.parseLong(text.trim());
			} catch (NumberFormatException e) {
				// an HTTP-date form is not interpreted; the advertised interval applies instead
				log("Retry-After is not in delta-seconds form; the advertised min_verification_interval applies", args("retry_after", text));
				return null;
			}
		}
		return null;
	}
}
