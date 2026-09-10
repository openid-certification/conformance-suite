package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfConstants.StreamStatus;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks that the stream status object in the last status endpoint response (SSF 1.0 8.1.2.1
 * read, 8.1.2.2 update) reports the expected {@code status} value.
 */
public class OIDSSFEnsureStreamStatusIs extends AbstractCondition {

	private final StreamStatus expectedStatus;

	public OIDSSFEnsureStreamStatusIs(StreamStatus expectedStatus) {
		this.expectedStatus = expectedStatus;
	}

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {

		JsonElement bodyJsonEl = env.getElementFromObject("resource_endpoint_response_full", "body_json");
		if (bodyJsonEl == null || !bodyJsonEl.isJsonObject()) {
			throw error("The status endpoint response does not contain a JSON object",
				args("response", env.getObject("resource_endpoint_response_full")));
		}

		JsonObject streamStatus = bodyJsonEl.getAsJsonObject();
		JsonElement statusEl = streamStatus.get("status");
		String status = OIDFJSON.isString(statusEl) ? OIDFJSON.getString(statusEl) : null;
		if (status == null) {
			throw error("The stream status object does not contain a 'status' string",
				args("stream_status", streamStatus, "expected_status", expectedStatus.name()));
		}

		if (!expectedStatus.name().equals(status)) {
			throw error("The stream status is not '" + expectedStatus.name() + "'",
				args("expected_status", expectedStatus.name(), "actual_status", status, "stream_status", streamStatus));
		}

		logSuccess("The stream status is '" + expectedStatus.name() + "'", args("stream_status", streamStatus));

		return env;
	}
}
