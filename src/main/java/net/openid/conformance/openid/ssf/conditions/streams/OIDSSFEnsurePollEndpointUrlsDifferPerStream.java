package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * SSF 1.0 6.1.2: poll delivery {@code endpoint_url} values "MAY be reused across Receivers,
 * but MUST be unique per stream for a given Receiver."
 * <p>
 * Compares the poll {@code endpoint_url} of two streams the same receiver created, stored
 * at {@code ssf.first_stream} and {@code ssf.second_stream}.
 */
public class OIDSSFEnsurePollEndpointUrlsDifferPerStream extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonObject firstStream = getStream(env, "first_stream");
		JsonObject secondStream = getStream(env, "second_stream");

		String firstStreamId = stringOrNull(firstStream.get("stream_id"));
		String secondStreamId = stringOrNull(secondStream.get("stream_id"));

		String firstEndpointUrl = getPollEndpointUrl(firstStream, "first");
		String secondEndpointUrl = getPollEndpointUrl(secondStream, "second");

		if (firstEndpointUrl.equals(secondEndpointUrl)) {
			if (firstStreamId != null && firstStreamId.equals(secondStreamId)) {
				throw error("The transmitter answered the second create request with 201 but returned the same stream again, "
						+ "so both streams share one poll endpoint_url. A transmitter that supports only one stream per receiver "
						+ "must reject the second create request with 409.",
					args("stream_id", firstStreamId, "endpoint_url", firstEndpointUrl));
			}
			throw error("Both streams created by this receiver share the same poll endpoint_url. "
					+ "The poll endpoint_url must be unique per stream for a given receiver.",
				args("first_stream_id", firstStreamId, "second_stream_id", secondStreamId, "endpoint_url", firstEndpointUrl));
		}

		logSuccess("The two streams created by this receiver have distinct poll endpoint_url values",
			args("first_stream_id", firstStreamId, "first_endpoint_url", firstEndpointUrl,
				"second_stream_id", secondStreamId, "second_endpoint_url", secondEndpointUrl));

		return env;
	}

	private JsonObject getStream(Environment env, String key) {
		JsonElement streamEl = env.getElementFromObject("ssf", key);
		if (streamEl == null || !streamEl.isJsonObject()) {
			throw error("Missing stream configuration to compare", args("missing", "ssf." + key));
		}
		return streamEl.getAsJsonObject();
	}

	private String getPollEndpointUrl(JsonObject stream, String which) {
		JsonElement deliveryEl = stream.get("delivery");
		if (deliveryEl == null || !deliveryEl.isJsonObject()) {
			throw error("The " + which + " stream configuration does not contain a 'delivery' object",
				args("stream_configuration", stream));
		}
		String endpointUrl = stringOrNull(deliveryEl.getAsJsonObject().get("endpoint_url"));
		if (endpointUrl == null || endpointUrl.isBlank()) {
			throw error("The " + which + " stream configuration does not contain a poll 'endpoint_url'. "
					+ "For poll delivery the transmitter supplies the endpoint_url.",
				args("stream_configuration", stream));
		}
		return endpointUrl;
	}

	private static String stringOrNull(JsonElement element) {
		return OIDFJSON.isString(element) ? OIDFJSON.getString(element) : null;
	}
}
