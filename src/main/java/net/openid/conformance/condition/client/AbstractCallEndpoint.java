package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * General utility class for calling endpoints
 *
 */
public abstract class AbstractCallEndpoint extends AbstractCondition {

	protected boolean jsonObjectError = false;
	protected boolean jsonParseError = false;
	protected JsonParseException jsonParseException = null;
	protected String endpointName;
	protected String responseEnvironmentKey;

	protected void addFullResponse(Environment env, ResponseEntity<String> response) {
		JsonObject fullResponse = convertJsonResponseForEnvironment(endpointName, response, true);
		env.putObject(responseEnvironmentKey, fullResponse);
	}

	@Override
	protected JsonObject convertJsonResponseForEnvironment(String endpointName, ResponseEntity<String> response, boolean allowParseFailure) {
		jsonParseError = false;
		jsonObjectError = false;
		jsonParseException = null;

		JsonObject responseInfo = convertResponseForEnvironment(endpointName, response);

		String jsonString = response.getBody();
		if (Strings.isNullOrEmpty(jsonString)) {
			if (allowParseFailure) {
				return responseInfo;
			}
			throw error("Empty response from the " + endpointName + " endpoint");
		}

		try {
			JsonElement jsonRoot = JsonParser.parseString(jsonString);
			if (jsonRoot == null || (!jsonRoot.isJsonObject() && !jsonRoot.isJsonArray())) {
				if (allowParseFailure) {
					jsonObjectError = true;
					return responseInfo;
				}

				throw error(endpointName + " endpoint did not return a JSON object.",
					args("response", jsonString));
			}

			responseInfo.add("body_json", jsonRoot);

		} catch (JsonParseException e) {
			if (allowParseFailure) {
				jsonParseError = true;
				jsonParseException = e; // save exception for later
				return responseInfo;
			}
			throw error("Response from " + endpointName + " endpoint does not appear to be JSON.", e,
				args("response", jsonString));
		}

		return responseInfo;
	}

	protected Environment handleJsonParseException(Environment env, JsonParseException e) {
		throw error("Error parsing "+endpointName+" response body as JSON", e);
	}

	protected Environment handleRestClientResponseException(Environment env, RestClientResponseException e) {
		throw error("RestClientResponseException occurred whilst calling "+endpointName,
			args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
	}

	protected Environment handleClientException(Environment env, RestClientException e) {
		String msg = "Call to "+endpointName+" failed";
		if (e.getCause() != null) {
			msg += " - " + e.getCause().getMessage();
		}
		throw error(msg, e);
	}
}
