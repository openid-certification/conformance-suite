package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.AbstractOIDSSFTransmitterEndpointCall;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;
import java.util.Set;

public class OIDSSFCallPollEndpoint extends AbstractOIDSSFTransmitterEndpointCall {

	public enum PollMode {
		POLL_ONLY,
		ACKNOWLEDGE_ONLY,

		POLL_AND_ACKNOWLEDGE
	}

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
	protected void prepareRequest(Environment env) {
		env.putString("resource", "resourceMethod", "POST");

		// See: https://www.rfc-editor.org/rfc/rfc8936.html#section-2.4
		// Poll requests have three variations:
		String pollModeName = env.getString("ssf", "poll.mode");
		PollMode pollMode = PollMode.valueOf(pollModeName);

		boolean returnImmediately = true;

		// See: https://www.rfc-editor.org/rfc/rfc8936.html#section-2.2
		Map<Object, Object> pollRequest;
		switch (pollMode) {
			// Poll-Only
			case POLL_ONLY:
				pollRequest = Map.of( //
					"maxEvents", 10,  // retrieve 10 events at max
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
				pollRequest = Map.of( //
					"maxEvents", 10,  // retrieve 10 events at max
					"returnImmediately", returnImmediately,
					"ack", Set.of()
				);
			}
			break;
			default:
				throw error("Unsupported poll mode", args("pollMode", pollMode));
		}

		String json = new Gson().toJson(pollRequest);
		env.putString("resource_request_entity", json);

		log("Configuring poll mode", args("pollMode", pollMode));
	}
}
