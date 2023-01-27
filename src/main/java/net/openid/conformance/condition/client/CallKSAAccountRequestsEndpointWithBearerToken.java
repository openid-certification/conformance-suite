package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;


public class CallKSAAccountRequestsEndpointWithBearerToken extends CallProtectedResource {

	@Override
	protected String getUri(Environment env) {

		String resourceUri = env.getString("resource","resourceUrlAccountRequests");
		if (Strings.isNullOrEmpty(resourceUri)){
			throw error("resourceUrlAccountRequests has not been provided in the test configuration");
		}
		return resourceUri;
	}

	@Override
	protected Object getBody(Environment env) {
		JsonObject requestObject = env.getObject("account_requests_endpoint_request");
		return requestObject.toString();
	}

	@Override
	protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
		String jsonString = responseBody;

		if (Strings.isNullOrEmpty(jsonString)) {
			throw error("Empty/missing response from the account requests endpoint");
		} else {
			log("Account requests endpoint response", args("account_requests_endpoint_response", jsonString));

			try {
				JsonElement jsonRoot = JsonParser.parseString(jsonString);
				if (jsonRoot == null || !jsonRoot.isJsonObject()) {
					throw error("Account requests endpoint did not return a JSON object");
				}


				env.putObject("account_requests_endpoint_response", jsonRoot.getAsJsonObject());
				env.putObject("resource_endpoint_response_full", jsonRoot.getAsJsonObject());
				env.putObject("resource_endpoint_response_headers", responseHeaders);


				logSuccess("Parsed account requests endpoint response", args("body", jsonString, "headers", responseHeaders));

				return env;
			} catch (JsonParseException e) {
				throw error(e);
			}
		}
	}

	@Override
	protected HttpMethod getMethod(Environment env) {
		return HttpMethod.POST;
	}

	@Override
	protected MediaType getContentType(Environment env) {
		return MediaType.APPLICATION_JSON;
	}
}
