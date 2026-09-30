package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

import java.util.Map;

/**
 * Validates the body of a poll response against RFC 8936 2.3 and 2.5: the response is
 * {@code application/json}; {@code sets} is a JSON object whose member names are {@code jti}
 * values and whose values are JSON strings (the SETs), empty when nothing is outstanding;
 * {@code moreAvailable}, when present, is a JSON boolean. Whether the response respects the
 * request's {@code maxEvents} is a SHOULD and checked separately by
 * {@link OIDSSFWarnPollResponseExceedsMaxEvents}.
 * <p>
 * Reads the response from {@code ssf_polling_response} (map {@code resource_endpoint_response_full}
 * onto it first).
 */
public class OIDSSFValidatePollResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf_polling_response")
	public Environment evaluate(Environment env) {

		JsonObject pollResponse = env.getObject("ssf_polling_response");

		checkContentType(pollResponse);

		JsonElement bodyEl = pollResponse.get("body_json");
		if (bodyEl == null || !bodyEl.isJsonObject()) {
			throw error("The poll response body is not a JSON object",
				args("body", pollResponse.get("body"), "body_json", bodyEl));
		}
		JsonObject body = bodyEl.getAsJsonObject();

		JsonElement setsEl = body.get("sets");
		if (setsEl == null) {
			throw error("The poll response does not contain the 'sets' member; it is required, "
				+ "as an empty JSON object when no SETs are outstanding", args("body", body));
		}
		if (!setsEl.isJsonObject()) {
			throw error("The 'sets' member of the poll response must be a JSON object keyed by jti",
				args("sets", setsEl));
		}
		JsonObject sets = setsEl.getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : sets.entrySet()) {
			JsonElement setEl = entry.getValue();
			if (!setEl.isJsonPrimitive() || !setEl.getAsJsonPrimitive().isString() || OIDFJSON.getString(setEl).isBlank()) {
				throw error("Each member of 'sets' must be a JSON string containing the SET",
					args("jti", entry.getKey(), "value", setEl));
			}
		}

		JsonElement moreAvailableEl = body.get("moreAvailable");
		if (moreAvailableEl != null && (!moreAvailableEl.isJsonPrimitive() || !moreAvailableEl.getAsJsonPrimitive().isBoolean())) {
			throw error("The 'moreAvailable' member of the poll response must be a JSON boolean",
				args("moreAvailable", moreAvailableEl));
		}

		logSuccess("The poll response is well-formed", args("returned_sets", sets.size(), "moreAvailable", moreAvailableEl));

		return env;
	}

	private void checkContentType(JsonObject pollResponse) {
		JsonElement headersEl = pollResponse.get("headers");
		String contentType = null;
		if (headersEl != null && headersEl.isJsonObject()) {
			for (Map.Entry<String, JsonElement> header : headersEl.getAsJsonObject().entrySet()) {
				if ("content-type".equalsIgnoreCase(header.getKey())) {
					JsonElement value = header.getValue();
					contentType = value.isJsonArray() && !value.getAsJsonArray().isEmpty()
						? OIDFJSON.tryGetString(value.getAsJsonArray().get(0))
						: OIDFJSON.tryGetString(value);
					break;
				}
			}
		}
		if (contentType == null) {
			throw error("The poll response does not carry a Content-Type header; a poll response is application/json",
				args("headers", headersEl));
		}
		try {
			MediaType mediaType = MediaType.parseMediaType(contentType);
			if (!MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
				throw error("The poll response Content-Type is not application/json",
					args("content_type", contentType));
			}
		} catch (InvalidMediaTypeException e) {
			throw error("The poll response Content-Type header could not be parsed", args("content_type", contentType));
		}
	}
}
