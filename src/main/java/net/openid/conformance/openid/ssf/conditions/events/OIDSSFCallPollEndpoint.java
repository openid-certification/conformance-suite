package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.AbstractOIDSSFTransmitterEndpointCall;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import org.springframework.http.HttpHeaders;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class OIDSSFCallPollEndpoint extends AbstractOIDSSFTransmitterEndpointCall {

	public static final int DEFAULT_MAX_EVENTS = 10;

	public enum PollMode {
		POLL_ONLY,
		ACKNOWLEDGE_ONLY,

		POLL_AND_ACKNOWLEDGE,

		/**
		 * Reports the SETs in {@code ssf.poll.sets} as erroneous via {@code setErrs} (RFC 8936
		 * 2.4.4) without polling; the request carries {@code Content-Language} as 2.6 requires.
		 */
		REPORT_ERRORS
	}

	/** The error reported for every SET in a {@link PollMode#REPORT_ERRORS} request. */
	public static final String REPORTED_ERROR_CODE = "invalid_request";

	public static final String REPORTED_ERROR_DESCRIPTION = "Reported by the conformance suite to exercise the transmitter's handling of setErrs";

	@Override
	protected String getEndpointName() {
		return "Poll Endpoint";
	}

	@Override
	protected String getResourceEndpointUrl(Environment env) {
		String pollEndpoint = OIDFJSON.getString(env.getElementFromObject("ssf", "stream.delivery.endpoint_url"));
		return pollEndpoint;
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		Environment environment = super.handleClientResponse(env, responseCode, responseBody, responseHeaders, fullResponse);
		return environment;
	}

	@Override
	protected HttpHeaders getHeaders(Environment env) {
		HttpHeaders headers = super.getHeaders(env);
		if (PollMode.REPORT_ERRORS.name().equals(env.getString("ssf", "poll.mode"))) {
			// RFC 8936 2.6: a request carrying error descriptions "must also include ... a
			// Content-Language header field whose value indicates the language of the error descriptions"
			headers.set(HttpHeaders.CONTENT_LANGUAGE, "en");
		}
		return headers;
	}

	@Override
	protected void prepareRequest(Environment env) {
		env.putString("resource", "resourceMethod", "POST");

		// See: https://www.rfc-editor.org/rfc/rfc8936.html#section-2.4
		// Poll requests have three variations:
		String pollModeName = env.getString("ssf", "poll.mode");
		PollMode pollMode = PollMode.valueOf(pollModeName);

		// RFC 8936 2.2: returnImmediately defaults to false (long poll); the suite short-polls
		// unless a module asks for a long poll via ssf.poll.return_immediately=false.
		String returnImmediatelyOverride = env.getString("ssf", "poll.return_immediately");
		boolean returnImmediately = returnImmediatelyOverride == null || Boolean.parseBoolean(returnImmediatelyOverride);
		int maxEvents = DEFAULT_MAX_EVENTS;
		Integer maxEventsOverride = env.getInteger("ssf", "poll.max_events");
		if (maxEventsOverride != null) {
			maxEvents = maxEventsOverride;
		}

		// See: https://www.rfc-editor.org/rfc/rfc8936.html#section-2.2
		Map<Object, Object> pollRequest;
		switch (pollMode) {
			// Poll-Only
			case POLL_ONLY:
				pollRequest = Map.of( //
					"maxEvents", maxEvents,
					"returnImmediately", returnImmediately
				);
				break;
			// Acknowledge-Only
			case ACKNOWLEDGE_ONLY: {
				Set<String> sets = env.getElementFromObject("ssf","poll.sets").getAsJsonObject().keySet();
				pollRequest = Map.of( //
					"maxEvents", 0,
					"returnImmediately", returnImmediately,
					"ack", sets
				);
			}
			break;
			// Combined Acknowledge and Poll
			case POLL_AND_ACKNOWLEDGE: {
				Set<String> sets = env.getElementFromObject("ssf","poll.sets").getAsJsonObject().keySet();
				pollRequest = Map.of( //
					"maxEvents", maxEvents,
					"returnImmediately", returnImmediately,
					"ack", sets
				);
			}
			break;
			// Error report only (RFC 8936 2.4.4)
			case REPORT_ERRORS: {
				Set<String> sets = env.getElementFromObject("ssf","poll.sets").getAsJsonObject().keySet();
				Map<String, Object> setErrs = new LinkedHashMap<>();
				for (String jti : sets) {
					setErrs.put(jti, Map.of("err", REPORTED_ERROR_CODE, "description", REPORTED_ERROR_DESCRIPTION));
				}
				pollRequest = Map.of( //
					"maxEvents", 0,
					"returnImmediately", true,
					"setErrs", setErrs
				);
			}
			break;
			default:
				throw error("Unsupported poll mode", args("pollMode", pollMode));
		}

		String json = new Gson().toJson(pollRequest);
		env.putString("resource_request_entity", json);
		// the sent parameters let the response validation check that maxEvents was honoured
		env.putObjectFromJsonString("ssf", "poll.request", json);

		log("Configuring poll mode", args("pollMode", pollMode, "poll_request", pollRequest));
	}
}
