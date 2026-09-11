package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

import java.util.Map;

/**
 * RFC 8936 2.2: "When making a request, the HTTP Content-Type header field is set to
 * application/json." Checks the poll request the receiver sent, recorded under
 * {@code incoming_request}. The emulated transmitter parses the body whatever the header says,
 * so a wrong or missing header does not change the outcome of the request; callers grade this
 * as a WARNING.
 */
public class OIDSSFWarnPollRequestContentTypeNotJson extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		JsonElement headersEl = env.getElementFromObject("incoming_request", "headers");
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
			throw error("The poll request carries no Content-Type header; a poll request is sent with Content-Type application/json",
				args("headers", headersEl));
		}
		try {
			MediaType mediaType = MediaType.parseMediaType(contentType);
			if (!MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
				throw error("The poll request's Content-Type is not application/json", args("content_type", contentType));
			}
		} catch (InvalidMediaTypeException e) {
			throw error("The poll request's Content-Type header could not be parsed", args("content_type", contentType));
		}

		logSuccess("The poll request is sent as application/json", args("content_type", contentType));
		return env;
	}
}
