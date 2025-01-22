package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;
import java.util.Objects;

public class OIDSSFCheckExpectedJsonResponseContents extends AbstractCondition {

	private final Map<String, Object> expectedContent;

	public OIDSSFCheckExpectedJsonResponseContents(Map<String, Object> expectedContent) {
		this.expectedContent = expectedContent;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonElement responseBodyJson = env.getElementFromObject("resource_endpoint_response_full", "body_json");
		if (responseBodyJson == null) {
			throw error("Response did not contain expected JSON object");
		}

		JsonObject responseBody = responseBodyJson.getAsJsonObject();
		for (var entry : expectedContent.entrySet()) {
			String key = entry.getKey();
			Object expectedValue = entry.getValue();
			JsonElement rawValue = responseBody.get(key);
			if (rawValue == null) {
				throw error("Could not find expected attribute '" + key + "'in JSON response", args("expected_content", expectedContent, "response_body", responseBodyJson));
			}
			String value = OIDFJSON.getString(rawValue);
			if (!Objects.equals(expectedValue, value)) {
				throw error("Attribute '" + key + "' did not match expected content in JSON response", args("expected_content", expectedContent, "response_body", responseBodyJson));
			}
		}

		logSuccess("Found expected content in JSON response", args("expected_content", expectedContent, "response_body", responseBodyJson));

		return env;
	}
}
