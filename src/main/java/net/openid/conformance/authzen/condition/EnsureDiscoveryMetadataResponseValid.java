package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Spec section 9.2.2-1 — the well-known discovery endpoint MUST return HTTP
 * 200 with `Content-Type: application/json`. The existing
 * {@link GetPDPDynamicServerConfiguration} relies on Spring throwing on
 * non-2xx and on the body parsing as JSON; this condition asserts the two
 * surface signals explicitly so a non-200 success status or a non-JSON
 * Content-Type is caught with a clear error.
 */
public class EnsureDiscoveryMetadataResponseValid extends AbstractCondition {

	@Override
	@PreEnvironment(required = "discovery_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("discovery_endpoint_response");

		JsonElement statusElem = response.get("status");
		if (statusElem == null || !statusElem.isJsonPrimitive()) {
			throw error("Discovery response has no status", args("response", response));
		}
		int status = OIDFJSON.getInt(statusElem);
		if (status != 200) {
			throw error("Discovery endpoint did not return HTTP 200",
				args("status", status, "response", response));
		}

		String contentType = readSingleHeader(response, "content-type");
		if (contentType == null) {
			throw error("Discovery response is missing Content-Type",
				args("headers", response.get("headers")));
		}
		if (!contentType.toLowerCase().startsWith("application/json")) {
			throw error("Discovery response Content-Type is not application/json",
				args("content_type", contentType));
		}

		logSuccess("Discovery endpoint returned HTTP 200 with application/json",
			args("content_type", contentType));
		return env;
	}

	private static String readSingleHeader(JsonObject response, String lowercaseName) {
		JsonElement headersElem = response.get("headers");
		if (headersElem == null || !headersElem.isJsonObject()) {
			return null;
		}
		JsonElement headerElem = headersElem.getAsJsonObject().get(lowercaseName);
		if (headerElem == null) {
			return null;
		}
		if (headerElem.isJsonArray() && !headerElem.getAsJsonArray().isEmpty()) {
			return OIDFJSON.getString(headerElem.getAsJsonArray().get(0));
		}
		if (headerElem.isJsonPrimitive() && headerElem.getAsJsonPrimitive().isString()) {
			return OIDFJSON.getString(headerElem);
		}
		return null;
	}
}
