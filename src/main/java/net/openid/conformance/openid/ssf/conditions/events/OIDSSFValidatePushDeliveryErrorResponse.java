package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates the error response a receiver returns when it rejects a pushed SET
 * (RFC 8935 2.3): HTTP status 400, an {@code application/json} Content-Type, a
 * {@code Content-Language} header naming the language of the description, and a JSON
 * object body carrying the string members {@code err} (a Security Event Token Error Code)
 * and {@code description}. Whether {@code err} is a registered code, or the code the
 * scenario expects, is checked separately by
 * {@link OIDSSFWarnPushDeliveryErrorCodeNotRegistered} and
 * {@link OIDSSFWarnPushDeliveryErrorCodeMismatch}.
 */
public class OIDSSFValidatePushDeliveryErrorResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		Integer status = env.getInteger("endpoint_response", "status");
		if (status == null || status == 0) {
			throw error("No HTTP response was received for the push delivery; a receiver that rejects a SET must answer with an error response",
				args("http_status", status));
		}
		if (status != 400) {
			throw error("A receiver that rejects a pushed SET because it failed to parse, validate or authenticate it must answer with HTTP status 400",
				args("http_status", status, "expected", 400));
		}

		String contentType = env.getString("endpoint_response", "headers.content-type");
		String mimeType = AbstractCheckEndpointContentTypeReturned.getMimeTypeFromContentType(contentType);
		if (!"application/json".equals(mimeType)) {
			throw error("The error response to a rejected push delivery must carry the Content-Type application/json",
				args("content_type", contentType, "expected", "application/json"));
		}

		String contentLanguage = env.getString("endpoint_response", "headers.content-language");
		if (contentLanguage == null || contentLanguage.isBlank()) {
			throw error("The error response to a rejected push delivery must carry a Content-Language header naming the language of the error description",
				args("headers", env.getElementFromObject("endpoint_response", "headers")));
		}

		JsonElement bodyJson = env.getElementFromObject("endpoint_response", "body_json");
		if (bodyJson == null || !bodyJson.isJsonObject()) {
			throw error("The body of the error response to a rejected push delivery must be a JSON object",
				args("body", env.getString("endpoint_response", "body")));
		}
		JsonObject body = bodyJson.getAsJsonObject();

		JsonElement err = body.get("err");
		if (err == null || !err.isJsonPrimitive() || !err.getAsJsonPrimitive().isString()) {
			throw error("The error response to a rejected push delivery must contain the string member 'err' holding a Security Event Token Error Code",
				args("body", body));
		}
		JsonElement description = body.get("description");
		if (description == null || !description.isJsonPrimitive() || !description.getAsJsonPrimitive().isString()) {
			throw error("The error response to a rejected push delivery must contain the string member 'description' with a human-readable description of the error",
				args("body", body));
		}

		logSuccess("The receiver answered the rejected push delivery with a well-formed error response",
			args("http_status", status, "err", OIDFJSON.getString(err), "description", OIDFJSON.getString(description)));
		return env;
	}
}
